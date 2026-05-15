package com.chinahitech.shop.config;

import com.chinahitech.shop.utils.JwtUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("SecurityConfig authorization tests")
@SpringBootTest(classes = {
        SecurityConfigTest.TestApplication.class,
        SecurityConfigTest.TestEndpoints.class
})
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        System.setProperty("jwt.secret", "test-jwt-secret-test-jwt-secret-test-jwt-secret");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("jwt.secret");
    }

    @Test
    @DisplayName("top manager register - anonymous request is rejected")
    void testTopManagerRegister_Anonymous_Unauthorized() throws Exception {
        mockMvc.perform(post("/topManager/register"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(50008));
    }

    @Test
    @DisplayName("admin activity audit - student token is forbidden")
    void testActivityAccept_StudentToken_Forbidden() throws Exception {
        mockMvc.perform(post("/activity/accept").header("X-Token", token(JwtUtils.ROLE_STUDENT)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin group audit - student token is forbidden")
    void testGroupAccept_StudentToken_Forbidden() throws Exception {
        mockMvc.perform(post("/group/accept").header("X-Token", token(JwtUtils.ROLE_STUDENT)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("group permission update - student token is forbidden")
    void testIndividualGroupUpdatePermission_StudentToken_Forbidden() throws Exception {
        mockMvc.perform(post("/individualGroup/updatePermission").header("X-Token", token(JwtUtils.ROLE_STUDENT)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("top manager add user - manager token is forbidden")
    void testTopManagerAddUser_ManagerToken_Forbidden() throws Exception {
        mockMvc.perform(post("/topManager/addUser").header("X-Token", token(JwtUtils.ROLE_MANAGER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("manager activity detail - manager token is allowed")
    void testActivityManagerDetail_ManagerToken_Ok() throws Exception {
        mockMvc.perform(post("/activity/managerDetail").header("X-Token", token(JwtUtils.ROLE_MANAGER)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("manager group detail - manager token is allowed")
    void testGroupManagerDetail_ManagerToken_Ok() throws Exception {
        mockMvc.perform(post("/group/managerDetail").header("X-Token", token(JwtUtils.ROLE_MANAGER)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("managed group list - manager token is allowed")
    void testIndividualGroupManagedGroups_ManagerToken_Ok() throws Exception {
        mockMvc.perform(post("/individualGroup/allManagedGroups").header("X-Token", token(JwtUtils.ROLE_MANAGER)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("管理活动列表 - 管理员令牌允许访问")
    void testIndividualActivityManagedActivities_ManagerToken_Ok() throws Exception {
        mockMvc.perform(post("/individualActivity/allManagedActivities").header("X-Token", token(JwtUtils.ROLE_MANAGER)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("学生自助社团列表 - 管理员令牌禁止访问")
    void testIndividualGroupAllGroups_ManagerToken_Forbidden() throws Exception {
        mockMvc.perform(post("/individualGroup/allGroups").header("X-Token", token(JwtUtils.ROLE_MANAGER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("top manager audit - top manager token is allowed")
    void testActivityAccept_TopManagerToken_Ok() throws Exception {
        mockMvc.perform(post("/activity/accept").header("X-Token", token(JwtUtils.ROLE_TOP_MANAGER)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("top manager group audit - top manager token is allowed")
    void testGroupAccept_TopManagerToken_Ok() throws Exception {
        mockMvc.perform(post("/group/accept").header("X-Token", token(JwtUtils.ROLE_TOP_MANAGER)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("private upload file - anonymous request is rejected")
    void testPrivateUploadFile_Anonymous_Unauthorized() throws Exception {
        mockMvc.perform(get("/upload/users.xlsx"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("公开上传图片 - 匿名请求允许访问")
    void testPublicUploadImage_Anonymous_Ok() throws Exception {
        mockMvc.perform(get("/upload/logo.png"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("私有上传附件 - 管理员令牌允许访问")
    void testPrivateUploadFile_ManagerToken_Ok() throws Exception {
        mockMvc.perform(get("/upload/users.xlsx").header("X-Token", token(JwtUtils.ROLE_MANAGER)))
                .andExpect(status().isOk());
    }

    private String token(String role) {
        return JwtUtils.generateToken("1001", role);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(SecurityConfig.class)
    static class TestApplication {
    }

    @RestController
    static class TestEndpoints {

        @PostMapping(value = "/topManager/register", produces = MediaType.TEXT_PLAIN_VALUE)
        String topManagerRegister() {
            return "ok";
        }

        @PostMapping(value = "/topManager/addUser", produces = MediaType.TEXT_PLAIN_VALUE)
        String topManagerAddUser() {
            return "ok";
        }

        @PostMapping(value = "/activity/accept", produces = MediaType.TEXT_PLAIN_VALUE)
        String activityAccept() {
            return "ok";
        }

        @PostMapping(value = "/activity/managerDetail", produces = MediaType.TEXT_PLAIN_VALUE)
        String activityManagerDetail() {
            return "ok";
        }

        @PostMapping(value = "/group/accept", produces = MediaType.TEXT_PLAIN_VALUE)
        String groupAccept() {
            return "ok";
        }

        @PostMapping(value = "/group/managerDetail", produces = MediaType.TEXT_PLAIN_VALUE)
        String groupManagerDetail() {
            return "ok";
        }

        @PostMapping(value = "/individualGroup/updatePermission", produces = MediaType.TEXT_PLAIN_VALUE)
        String individualGroupUpdatePermission() {
            return "ok";
        }

        @PostMapping(value = "/individualGroup/allManagedGroups", produces = MediaType.TEXT_PLAIN_VALUE)
        String individualGroupAllManagedGroups() {
            return "ok";
        }

        @PostMapping(value = "/individualActivity/allManagedActivities", produces = MediaType.TEXT_PLAIN_VALUE)
        String individualActivityAllManagedActivities() {
            return "ok";
        }

        @PostMapping(value = "/individualGroup/allGroups", produces = MediaType.TEXT_PLAIN_VALUE)
        String individualGroupAllGroups() {
            return "ok";
        }

        @GetMapping(value = "/upload/users.xlsx", produces = MediaType.TEXT_PLAIN_VALUE)
        String privateUploadFile() {
            return "ok";
        }

        @GetMapping(value = "/upload/logo.png", produces = MediaType.TEXT_PLAIN_VALUE)
        String publicUploadImage() {
            return "ok";
        }
    }
}
