package com.chinahitech.shop.service;

import com.chinahitech.shop.config.TeamRagProperties;
import com.chinahitech.shop.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TeamRag协议与流式适配测试")
class TeamRagClientTest {
    final ObjectMapper json = new ObjectMapper();
    final TeamRagAccessService.Access access = new TeamRagAccessService.Access("club-one","baituan-STUDENT-u1","READER");
    TeamRagProperties properties;
    TeamRagClient client;
    HttpServer upstream;
    AtomicReference<String> body;
    boolean secured;
    @BeforeEach void setup() throws Exception {
        properties = new TeamRagProperties(); properties.setTokenSecret("test-secret-for-teamrag-at-least-32-bytes");
        upstream = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0); body = new AtomicReference<>(); secured = true;
        properties.setBaseUrl("http://127.0.0.1:" + upstream.getAddress().getPort());
        upstream.createContext("/api/auth/me", exchange -> {
            byte[] payload = json.writeValueAsBytes(Map.of("securityEnabled",secured,"username",access.owner()));
            exchange.sendResponseHeaders(200,payload.length); exchange.getResponseBody().write(payload); exchange.close();
        });
        upstream.createContext("/api/rag/qa", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            byte[] answer="data: 社团资料回答\n\n".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type","text/event-stream");
            exchange.sendResponseHeaders(200,answer.length); exchange.getResponseBody().write(answer); exchange.close();
        });
        upstream.start(); client = new TeamRagClient(properties,json);
    }
    @AfterEach void cleanup() { upstream.stop(0); }
    @Test @DisplayName("授权凭据 - 单项目与用户身份隔离且短期有效")
    void testToken_ScopedClaims() throws Exception {
        // Given / When
        String token=client.token(access);
        Map<?,?> claims=json.readValue(Base64.getUrlDecoder().decode(token.split("\\.")[1]),Map.class);
        // Then
        assertEquals(access.owner(),claims.get("sub")); assertEquals("READER",claims.get("role"));
        assertEquals(java.util.List.of("club-one"),claims.get("projects"));
        assertEquals(120,((Number)claims.get("exp")).longValue()-((Number)claims.get("iat")).longValue());
    }
    @Test @DisplayName("知识问答 - 透传SSE且服务端固定项目和会话作用域")
    void testAsk_StreamAndScope() throws Exception {
        // Given
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        // When
        client.ask(access,"入团要求","session-1").writeTo(output);
        // Then
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("data: 社团资料回答"));
        Map<?,?> request=json.readValue(body.get(),Map.class);
        assertEquals("club-one",request.get("project"));
        assertEquals("baituan-STUDENT-u1:club-one:session-1",request.get("conversationId"));
        assertFalse(request.containsKey("memoryId"));
    }
    @Test @DisplayName("TeamRag关闭认证 - 拒绝共享匿名会话")
    void testAsk_UnsecuredUpstream_Reject() {
        // Given
        secured=false;
        // When / Then
        assertThrows(BusinessException.class,()->client.ask(access,"入团要求","session-1")); assertNull(body.get());
    }
    @Test @DisplayName("问题或会话无效 - 不调用上游")
    void testAsk_InvalidInput_Reject() {
        // Given / When / Then
        assertThrows(BusinessException.class,()->client.ask(access," ","session-1"));
        assertThrows(BusinessException.class,()->client.ask(access,"问题","../other-user")); assertNull(body.get());
    }
    @Test @DisplayName("空文件上传 - 拒绝请求")
    void testUpload_EmptyFile_Reject() {
        // Given
        MockMultipartFile file=new MockMultipartFile("file",new byte[0]);
        // When / Then
        assertThrows(BusinessException.class,()->client.upload(access,file));
    }
}
