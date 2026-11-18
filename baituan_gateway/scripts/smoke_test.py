"""用真实网关进程和两个临时 HTTP 服务验证路由、轮询、透传及故障响应；无需 Docker。"""
import json
import os
from pathlib import Path
import socket
import subprocess
import threading
import sys
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError


def free_port():
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


calls = []


class Echo(BaseHTTPRequestHandler):
    def do_GET(self):
        calls.append((self.server.server_port, self.command, self.path))
        if getattr(self.server, "fail", False):
            self.connection.shutdown(socket.SHUT_RDWR)
            self.connection.close()
            return
        payload = json.dumps({"instance": self.server.server_port, "path": self.path,
                              "token": self.headers.get("X-Token"),
                              "authorization": self.headers.get("Authorization")}).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def do_POST(self):
        self.rfile.read(int(self.headers.get("Content-Length", "0")))
        self.do_GET()

    def log_message(self, *_):
        pass


def main():
    root = Path(__file__).resolve().parents[1]
    servers = [ThreadingHTTPServer(("127.0.0.1", 0), Echo) for _ in range(2)]
    for server in servers:
        threading.Thread(target=server.serve_forever, daemon=True).start()
    port = free_port()
    command = ["java", "-jar", str(root / "target/baituan-gateway-1.0.0.jar"),
               f"--server.port={port}"]
    registry = None
    if "--consul" in sys.argv:
        class Registry(BaseHTTPRequestHandler):
            def do_GET(self):
                if self.path.startswith("/v1/health/service/club-backend"):
                    assert "passing" in self.path, self.path
                    data = [{"Node": {"Node": "test", "Address": "127.0.0.1"},
                             "Service": {"ID": str(s.server_port), "Service": "club-backend",
                                         "Address": "127.0.0.1", "Port": s.server_port,
                                         "Tags": [], "Meta": {}}, "Checks": []}
                            for s in self.server.active_backends]
                else:
                    data = {"club-backend": []}
                payload = json.dumps(data).encode()
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(payload)))
                self.end_headers()
                self.wfile.write(payload)

            def log_message(self, *_):
                pass

        registry = ThreadingHTTPServer(("127.0.0.1", 0), Registry)
        registry.active_backends = servers[:]
        threading.Thread(target=registry.serve_forever, daemon=True).start()
        command += ["--spring.profiles.active=cluster", "--spring.cloud.consul.host=127.0.0.1",
                    f"--spring.cloud.consul.port={registry.server_port}",
                    "--spring.cloud.loadbalancer.cache.enabled=false"]
    else:
        for index, server in enumerate(servers):
            command.append(f"--spring.cloud.discovery.client.simple.instances.club-backend[{index}].uri="
                           f"http://127.0.0.1:{server.server_port}")
    log = open(root / "smoke-runtime.log", "w", encoding="utf-8")
    process = subprocess.Popen(command, stdout=log, stderr=subprocess.STDOUT,
                               creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)

    def request(path, method="GET"):
        req = Request(f"http://127.0.0.1:{port}{path}", method=method,
                      headers={"X-Token": "test-token", "Authorization": "Bearer test-bearer"},
                      data=b"{}" if method == "POST" else None)
        try:
            with urlopen(req, timeout=25) as response:
                return response.status, json.load(response)
        except HTTPError as error:
            raw = error.read()
            return error.code, json.loads(raw) if raw else {}

    try:
        deadline = time.monotonic() + 90
        while True:
            if process.poll() is not None:
                raise RuntimeError("网关启动失败，查看 smoke-runtime.log")
            try:
                if request("/actuator/health")[0] == 200:
                    break
            except (URLError, TimeoutError):
                pass
            if time.monotonic() > deadline:
                raise RuntimeError("网关启动超时")
            time.sleep(0.5)
        seen = set()
        for _ in range(6):
            status, body = request("/api/activity/all?page=1")
            assert status == 200, (status, body)
            assert body["path"] == "/activity/all?page=1", body
            assert body["token"] == "test-token", body
            assert body["authorization"] == "Bearer test-bearer", body
            seen.add(body["instance"])
        assert len(seen) == 2, seen
        before = len(calls)
        assert request("/api/activity/applyJoin", "POST")[0] == 200
        assert len(calls) == before + 1, "写请求被重复转发"
        assert request("/upload/test.png")[1]["path"] == "/upload/test.png"
        assert request("/internal/private")[0] == 404
        if registry:
            # 模拟注册中心健康查询已剔除一个实例，验证客户端不再向它发请求。
            registry.active_backends = servers[:1]
            for _ in range(4):
                assert request("/api/activity/all")[1]["instance"] == servers[0].server_port
        for server in servers:
            server.fail = True
        before = len(calls)
        assert request("/api/activity/applyJoin", "POST")[0] == 503
        assert len(calls) == before + 1, "连接重置导致写请求自动重放"
        before = len(calls)
        for _ in range(25):
            assert request("/api/activity/all")[0] == 503
        assert len(calls) - before < 25, "连续故障后熔断器未阻止后端调用"
        for server in servers:
            server.shutdown()
            server.server_close()
        status, body = request("/api/activity/all")
        assert status == 503 and body["code"] == 50300, (status, body)
        print("PASS: 双实例轮询、路径及凭据透传、POST单次转发及故障不重放、内部路径隔离、熔断及503")
    finally:
        process.terminate()
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait()
        for server in servers:
            server.shutdown()
            server.server_close()
        log.close()
        if registry:
            registry.shutdown()
            registry.server_close()


if __name__ == "__main__":
    main()
