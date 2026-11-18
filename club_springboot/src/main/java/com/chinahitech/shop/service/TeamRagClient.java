package com.chinahitech.shop.service;

import com.chinahitech.shop.config.TeamRagProperties;
import com.chinahitech.shop.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.io.*;

/** 唯一的 TeamRag HTTP 适配入口：短期、单项目、按用户授权的凭据永不返回浏览器。 */
@Service
@RequiredArgsConstructor
public class TeamRagClient {
    private final TeamRagProperties properties;
    private final ObjectMapper json;

    public String token(TeamRagAccessService.Access access) {
        String secret = properties.getTokenSecret();
        if (secret == null || secret.length() < 32)
            throw new BusinessException("SERVICE_UNAVAILABLE", "知识库认证配置不完整");
        try {
            long now = Instant.now().getEpochSecond();
            String unsigned = encode(json.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT"))) + "."
                    + encode(json.writeValueAsBytes(Map.of("sub", access.owner(), "tenant", properties.getTenantId(),
                    "role", access.role(), "projects", List.of(access.project()), "iat", now, "exp", now + 120)));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return unsigned + "." + encode(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) { throw unavailable(); }
    }

    private String encode(byte[] bytes) { return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }

    private void verifyIdentity(TeamRagAccessService.Access access) {
        HttpURLConnection connection = null;
        try {
            connection = open("/api/auth/me", "GET", access);
            if (connection.getResponseCode() != 200) throw unavailable();
            try (InputStream input = connection.getInputStream()) {
                Map<?, ?> identity = json.readValue(input, Map.class);
                if (!Boolean.TRUE.equals(identity.get("securityEnabled"))
                        || !access.owner().equals(identity.get("username"))) throw unavailable();
            }
        } catch (IOException ex) { throw unavailable(); }
        finally { if (connection != null) connection.disconnect(); }
    }

    private HttpURLConnection open(String path, String method, TeamRagAccessService.Access access) throws IOException {
        URI base = URI.create(properties.getBaseUrl());
        if (!List.of("http", "https").contains(base.getScheme()) || base.getHost() == null)
            throw unavailable();
        HttpURLConnection connection = (HttpURLConnection) base.resolve(path).toURL().openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(properties.getConnectTimeoutMs());
        connection.setReadTimeout(properties.getReadTimeoutMs());
        connection.setRequestMethod(method);
        connection.setRequestProperty("Authorization", "Bearer " + token(access));
        return connection;
    }

    public StreamingResponseBody ask(TeamRagAccessService.Access access, String query, String conversationId) {
        if (query == null || query.isBlank() || query.length() > 4000)
            throw new BusinessException("PARAM_ERROR", "问题长度应为1至4000字");
        if (conversationId == null || !conversationId.matches("[A-Za-z0-9-]{1,64}"))
            throw new BusinessException("PARAM_ERROR", "会话编号无效");
        // 上游关闭认证时会共享匿名记忆，必须拒绝接入，不能仅凭已配置密钥认为隔离生效。
        verifyIdentity(access);
        // 网络连接在异步回调内创建；请求取消后写出失败会立即关闭上游，避免泄漏连接。
        return output -> {
            HttpURLConnection connection = null;
            try {
                connection = open("/api/rag/qa", "POST", access);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "text/event-stream");
                try (OutputStream request = connection.getOutputStream()) {
                    json.writeValue(request, Map.of("query", query, "project", access.project(),
                            "conversationId", access.owner() + ":" + access.project() + ":" + conversationId));
                }
                if (connection.getResponseCode() != 200) throw new IOException("TeamRag unavailable");
                try (InputStream input = connection.getInputStream()) {
                    byte[] buffer = new byte[4096];
                    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(180);
                    int size;
                    while ((size = input.read(buffer)) != -1) {
                        if (System.nanoTime() > deadline) throw new IOException("TeamRag stream timeout");
                        output.write(buffer, 0, size);
                        output.flush();
                    }
                }
            } catch (IOException ex) {
                // 已开始流式响应后不能改发 Result JSON；使用 SSE error 事件让前端明确显示失败。
                output.write("event: error\ndata: 知识库服务暂不可用，请稍后重试\n\n".getBytes(StandardCharsets.UTF_8));
                output.flush();
            } finally { if (connection != null) connection.disconnect(); }
        };
    }

    public Object documents(TeamRagAccessService.Access access) {
        verifyIdentity(access);
        HttpURLConnection connection = null;
        try {
            connection = open("/api/document", "GET", access);
            if (connection.getResponseCode() != 200) throw unavailable();
            try (InputStream input = connection.getInputStream()) { return json.readValue(input, Object.class); }
        } catch (IOException ex) { throw unavailable(); }
        finally { if (connection != null) connection.disconnect(); }
    }

    public Object upload(TeamRagAccessService.Access access, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 10 * 1024 * 1024)
            throw new BusinessException("PARAM_ERROR", "请选择不超过10MB的文档");
        verifyIdentity(access);
        HttpURLConnection connection = null;
        try {
            connection = open("/api/document/upload", "POST", access);
            connection.setDoOutput(true);
            connection.setChunkedStreamingMode(8192);
            String boundary = "baituan-" + UUID.randomUUID();
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            String filename = String.valueOf(file.getOriginalFilename()).replaceAll("[\\r\\n\\\"\\\\/]", "_");
            try (OutputStream output = connection.getOutputStream(); InputStream input = file.getInputStream()) {
                output.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"project\"\r\n\r\n"
                        + access.project() + "\r\n--" + boundary
                        + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + filename
                        + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                input.transferTo(output);
                output.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            }
            if (connection.getResponseCode() != 200) throw unavailable();
            try (InputStream input = connection.getInputStream()) { return json.readValue(input, Object.class); }
        } catch (IOException ex) { throw unavailable(); }
        finally { if (connection != null) connection.disconnect(); }
    }

    private BusinessException unavailable() { return new BusinessException("SERVICE_UNAVAILABLE", "知识库服务暂不可用"); }
}
