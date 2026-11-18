package com.chinahitech.shop.controller;

import com.chinahitech.shop.config.SecurityConfig;
import com.chinahitech.shop.config.StreamingConfig;
import com.chinahitech.shop.service.TeamRagAccessService;
import com.chinahitech.shop.service.TeamRagClient;
import com.chinahitech.shop.utils.JwtUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.charset.StandardCharsets;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TeamRagControllerTest.Application.class)
@AutoConfigureMockMvc
@DisplayName("TeamRag认证与异步流接口测试")
class TeamRagControllerTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({SecurityConfig.class, StreamingConfig.class, TeamRagController.class})
    static class Application { }
    @Autowired MockMvc mvc;
    @MockitoBean TeamRagAccessService access;
    @MockitoBean TeamRagClient client;

    @Test @DisplayName("知识问答 - 匿名请求不能启动上游调用")
    void testChat_Anonymous_Unauthorized() throws Exception {
        // Given / When / Then
        mvc.perform(post("/knowledge/groups/1/chat").contentType("application/json")
                .content("{\"query\":\"问题\",\"conversationId\":\"c1\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(access,client);
    }
    @Test @DisplayName("知识问答 - 已认证请求保留异步响应和SSE内容")
    void testChat_Authenticated_Stream() throws Exception {
        // Given
        String previous=System.getProperty("jwt.secret");
        System.setProperty("jwt.secret","test-secret-at-least-32-bytes-for-controller");
        try {
            var scope=new TeamRagAccessService.Access("club-one","baituan-STUDENT-u1","READER");
            when(access.authorize(eq(1),any(),eq(false))).thenReturn(scope);
            when(client.ask(scope,"问题","c1")).thenReturn(output -> output.write("data: 回答\n\n".getBytes(StandardCharsets.UTF_8)));
            // When
            var result=mvc.perform(post("/knowledge/groups/1/chat")
                    .header("X-Token",JwtUtils.generateToken("u1",JwtUtils.ROLE_STUDENT))
                    .contentType("application/json").content("{\"query\":\"问题\",\"conversationId\":\"c1\"}"))
                    .andExpect(request().asyncStarted()).andReturn();
            // Then
            mvc.perform(asyncDispatch(result)).andExpect(status().isOk())
                    .andExpect(content().string("data: 回答\n\n"));
            verify(client).ask(scope,"问题","c1");
        } finally {
            if(previous==null) System.clearProperty("jwt.secret"); else System.setProperty("jwt.secret",previous);
        }
    }
}
