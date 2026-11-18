"""真实 HTTP 冒烟验证：只创建 codex_search_http_* 隔离数据库及独立索引，自动停止测试后端。
前置：MySQL 测试容器 codex-baituan-bench-mysql:13306、Redis:16379、ES:19200；
先 mvn package。运行结果写入 target/search-http/result.json；不更改业务库。
"""
import base64, hashlib, hmac, json, os, re, subprocess, time, urllib.request, urllib.error, uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "target/search-http"
DOCKER = r"C:\Program Files\Docker\Docker\resources\bin\docker.exe"
JAVA = r"C:\Users\30983\.jdks\ms-17.0.18\bin\java.exe"
SECRET = base64.b64encode(b"isolated-search-http-key-" * 4).decode()
DATABASE = "codex_search_http_" + uuid.uuid4().hex[:10]
INDEX = DATABASE
OUT.mkdir(parents=True, exist_ok=True)

def sql(statement, database=None):
    args = [DOCKER, "exec", "-i", "codex-baituan-bench-mysql", "mysql", "-uroot",
            "-plocal-benchmark-only", "--default-character-set=utf8mb4"]
    if database: args.append(database)
    subprocess.run(args, input=statement.encode(), capture_output=True, check=True)

def token(role):
    enc = lambda v: base64.urlsafe_b64encode(v).rstrip(b"=").decode()
    now = int(time.time())
    value = enc(b'{"alg":"HS512","typ":"JWT"}') + "." + enc(json.dumps(
        {"sub":"search-test-user","role":role,"iat":now,"exp":now+1800}).encode())
    return value + "." + enc(hmac.new(base64.b64decode(SECRET), value.encode(), hashlib.sha512).digest())

def request(path, role=None, method="GET", extra=None):
    headers = {"Authorization":"Bearer " + token(role)} if role else {}
    headers.update(extra or {})
    req = urllib.request.Request("http://127.0.0.1:18082" + path, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as r: status, raw = r.status, r.read()
    except urllib.error.HTTPError as e: status, raw = e.code, e.read()
    try: body = json.loads(raw)
    except ValueError: body = raw.decode(errors="replace")
    return status, body

def run():
    assert DATABASE.startswith("codex_search_http_")
    sql("CREATE DATABASE " + DATABASE + " CHARACTER SET utf8mb4")
    source = (ROOT.parent / "club_finalversion.sql").read_text(encoding="utf-8")
    sql("SET FOREIGN_KEY_CHECKS=0;\n" + "\n".join(re.findall(r"CREATE TABLE .*?;\s*", source, re.S)), DATABASE)
    sql((ROOT / "src/main/resources/db/migration/activity-search-outbox.sql").read_text(encoding="utf-8"), DATABASE)
    values = [f"({i},'人工智能分享会{i}','计算机协会','算法实践',0,100,{1 if i < 125 else 0})" for i in range(1,126)]
    sql("INSERT INTO activity(id,name,group_name,description,hot,number,is_accepted) VALUES " + ",".join(values), DATABASE)
    env = os.environ.copy()
    env.update(DB_URL=f"jdbc:mysql://127.0.0.1:13306/{DATABASE}?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8",
               DB_USERNAME="root", DB_PASSWORD="local-benchmark-only", SERVER_PORT="18082",
               REDIS_HOST="127.0.0.1", REDIS_PORT="16379", REDIS_DATABASE="13",
               JWT_SECRET=SECRET, ELASTICSEARCH_ENABLED="true", ELASTICSEARCH_HOST="127.0.0.1",
               ELASTICSEARCH_PORT="19200", ELASTICSEARCH_INDEX=INDEX,
               KAFKA_ENABLED="false", KAFKA_LISTENER_AUTO_STARTUP="false", REQUEST_LOG_ENABLED="false",
               BUSINESS_CACHE_ENABLED="false", RATE_LIMIT_FAIL_OPEN="false")
    facts = {"database":DATABASE, "index":INDEX, "fixtures":125, "approved":124, "checks":[]}
    with (OUT / "server.log").open("wb") as log:
        proc = subprocess.Popen([JAVA, "-Xms256m", "-Xmx768m", "-jar",
            str(ROOT / "target/shop-0.0.1-SNAPSHOT.jar"), "--app.activity.async-write-enabled=false",
            "--logging.level.com.chinahitech.shop.mapper=INFO"], env=env, cwd=OUT,
            stdout=log, stderr=subprocess.STDOUT, creationflags=0x08000000)
        try:
            for _ in range(90):
                if proc.poll() is not None: raise RuntimeError("后端启动失败，请查 server.log")
                try:
                    if request("/actuator/health")[0] == 200: break
                except OSError: pass
                time.sleep(1)
            else: raise TimeoutError("启动超时")
            assert request("/activity/search")[0] == 401
            facts["checks"].append("anonymous_401")
            for role in ["STUDENT","MANAGER","TOP_MANAGER"]:
                status, body = request("/activity/search?keyword=AI", role)
                assert status == 200 and body["code"] == 20000, (status, body)
            facts["checks"].append("three_roles_search_200")
            for role in ["STUDENT","MANAGER"]:
                assert request("/search/index/rebuildActivities", role, "POST")[0] == 403
            facts["checks"].append("non_admin_rebuild_403")
            status, body = request("/search/index/rebuildActivities", "TOP_MANAGER", "POST")
            assert status == 200 and body["data"]["queued"] == 125
            assert "indexed" not in body["data"]
            facts["checks"].append("admin_rebuild_queued_125")
            for _ in range(60):
                status, body = request("/search/index/status", "TOP_MANAGER")
                if status == 200 and body["data"]["queue"]["pending"] == 0: break
                time.sleep(1)
            else: raise TimeoutError("同步队列未完成")
            time.sleep(1.2)  # 等待 ES 默认 refresh_interval，队列完成不等于刷新可见。
            path = "/activity/search?keyword=" + urllib.parse.quote("人工智能") + "&pageNum=7&pageSize=20"
            status, body = request(path, "STUDENT")
            assert status == 200 and body["code"] == 20000, body
            data = body["data"]
            assert data["searchSource"] == "elasticsearch" and data["degraded"] is False, data
            assert data["page"]["total"] == 124 and data["page"]["pages"] == 7 and len(data["items"]) == 4, data
            facts["checks"].append("page7_total124_items4")
            status, body = request("/activity/search?pageNum=2147483647&pageSize=100", "STUDENT")
            assert body["success"] is False, body
            facts["checks"].append("overflow_page_rejected")
            status, _ = request("/activity/search", method="OPTIONS", extra={
                "Origin":"http://localhost:9533","Access-Control-Request-Method":"GET",
                "Access-Control-Request-Headers":"authorization"})
            assert status == 200, status
            facts["checks"].append("cors_preflight_200")
            facts["success"] = True
            (OUT / "result.json").write_text(json.dumps(facts, ensure_ascii=False, indent=2), encoding="utf-8")
            print(json.dumps(facts, ensure_ascii=False))
        finally:
            proc.terminate()
            try: proc.wait(timeout=20)
            except subprocess.TimeoutExpired: proc.kill()

if __name__ == "__main__":
    import urllib.parse
    run()

