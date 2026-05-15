package com.chinahitech.shop.controller;

import com.chinahitech.shop.aop.RepeatLimit;
import com.chinahitech.shop.bean.User;
import com.chinahitech.shop.bean.notAddedToDatabase.RegisterUser;
import com.chinahitech.shop.bean.notAddedToDatabase.StoredFile;
import com.chinahitech.shop.service.FileStorageService;
import com.chinahitech.shop.service.TopManagerService;
import com.chinahitech.shop.utils.JwtUtils;
import com.chinahitech.shop.utils.PageUtils;
import com.chinahitech.shop.utils.Result;
import com.chinahitech.shop.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/topManager")
public class TopManagerController {
    @Autowired
    private TopManagerService topManagerService;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 超级管理员登录，校验账号密码并返回 JWT token。
     */
    @RepeatLimit
    @PostMapping("/login")
    public Result login(String userId, String password) {
        String token = topManagerService.loginToken(userId, password);
        if (token == null) {
            return Result.error().message("用户名或密码不正确");
        }
        return Result.ok().data("token", token);
    }

    /**
     * 注册超级管理员账号。
     */
    @RepeatLimit
    @PostMapping("/register")
    public Result register(@RequestBody RegisterUser user) {
        topManagerService.register(user);
        return Result.ok().message("注册成功");
    }

    /**
     * 根据超级管理员编号发送账号关联邮箱，并返回邮箱地址。
     */
    @RepeatLimit
    @PostMapping("/getEmail")
    public Result getEmail(String userNumber) throws Exception {
        String email = topManagerService.sendAccountEmail(userNumber);
        return Result.ok().data("email", email);
    }

    /**
     * 校验邮箱验证码是否正确。
     */
    @RepeatLimit
    @PostMapping("/getValidate")
    public Result getValidate(String email, String validateCode) {
        topManagerService.validateCode(email, validateCode);
        return Result.ok().message("验证成功");
    }

    /**
     * 向指定邮箱发送验证码。
     */
    @RepeatLimit
    @PostMapping("/validateEmail")
    public Result validateEmail(String email) throws Exception {
        topManagerService.sendValidateEmail(email);
        return Result.ok().message("验证码已发送");
    }

    /**
     * 当前登录超级管理员修改自己的登录密码。
     */
    @RepeatLimit
    @PostMapping("/modifyPass")
    public Result modifyPassword(String userId, String password, Authentication authentication) {
        SecurityUtils.requireSelf(authentication, userId);
        topManagerService.updatePassword(userId, password);
        return Result.ok();
    }

    /**
     * 当前登录超级管理员修改自己的手机号。
     */
    @RepeatLimit
    @PostMapping("/modifyPhone")
    public Result modifyPhone(String userId, String phone, Authentication authentication) {
        SecurityUtils.requireSelf(authentication, userId);
        topManagerService.updatePhone(userId, phone);
        return Result.ok();
    }

    /**
     * 当前登录超级管理员修改自己的个人简介。
     */
    @RepeatLimit
    @PostMapping("/modifyDescription")
    public Result modifyDescription(String userId, String description, Authentication authentication) {
        SecurityUtils.requireSelf(authentication, userId);
        topManagerService.updateDescription(userId, description);
        return Result.ok();
    }

    /**
     * 当前登录超级管理员修改自己的昵称。
     */
    @RepeatLimit
    @PostMapping("/modifyNickname")
    public Result modifyNickname(String userId, String nickname, Authentication authentication) {
        SecurityUtils.requireSelf(authentication, userId);
        topManagerService.updateNickname(userId, nickname);
        return Result.ok();
    }

    /**
     * 当前登录超级管理员修改自己的校区、学院和专业信息。
     */
    @RepeatLimit
    @PostMapping("/modifyMajor")
    public Result modifyMajor(String userId, String campus, String school, String major, Authentication authentication) {
        SecurityUtils.requireSelf(authentication, userId);
        topManagerService.updateMajor(userId, campus, school, major);
        return Result.ok();
    }

    /**
     * 当前登录超级管理员查询自己的个人资料。
     */
    @RepeatLimit
    @PostMapping("/profile")
    public Result getProfile(String userId, Authentication authentication) {
        SecurityUtils.requireSelf(authentication, userId);
        User user = topManagerService.getByUserId(userId);
        return Result.ok().data("user", user);
    }

    /**
     * 根据请求头 token 解析当前超级管理员身份，返回前端需要的用户名和头像信息。
     */
    @RepeatLimit
    @GetMapping("/info")
    public Result info(@RequestHeader(value = "X-Token", required = false) String xToken,
                       @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = JwtUtils.resolveToken(xToken, authorization);
        String username = JwtUtils.getClaimsByToken(token).getSubject();
        String url = "https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2021%2F1120%2F783a7b4ej00r2tvvx002fd200hs00hsg00hs00.jpg&thumbnail=660x2147483647&quality=80&type=jpg";
        return Result.ok().data("name", username).data("avatar", url);
    }

    /**
     * 超级管理员退出登录，前端收到成功响应后清理本地 token。
     */
    @RepeatLimit
    @PostMapping("/logout")
    public Result logout() {
        return Result.ok();
    }

    /**
     * 超级管理员手动新增用户账号。
     */
    @RepeatLimit
    @PostMapping("/addUser")
    public Result addUser(@RequestBody RegisterUser user) {
        topManagerService.addUser(user);
        return Result.ok().message("添加成功");
    }

    /**
     * 超级管理员查询全部用户列表，支持搜索和分页。
     */
    @RepeatLimit
    @RequestMapping("/getAllUsers")
    public Result getAllUsers(String searchInfo, Integer pageNum, Integer pageSize) {
        List<User> users = topManagerService.getAllUsers(searchInfo);
        return PageUtils.ok("items", users, pageNum, pageSize);
    }

    /**
     * 超级管理员修改指定用户的基础资料。
     */
    @RepeatLimit
    @PostMapping("/modifyUserInfo")
    public Result modifyUserInfo(@RequestBody User user) {
        topManagerService.updateUserInfo(user);
        return Result.ok();
    }

    /**
     * 超级管理员删除指定用户账号。
     */
    @RepeatLimit
    @RequestMapping("/deleteUser")
    public Result deleteUser(@RequestBody User user) {
        topManagerService.deleteUser(user);
        return Result.ok();
    }

    /**
     * 超级管理员上传用户 Excel 文件，并将批量导入任务加入后台处理队列。
     */
    @RepeatLimit
    @PostMapping("/uploadExcel")
    public ResponseEntity<Map<String, String>> uploadExcel(@RequestParam("file") MultipartFile file) {
        StoredFile storedFile = fileStorageService.store(file);
        topManagerService.enqueueUploadExcel(storedFile.getFilePath());

        Map<String, String> response = new HashMap<>();
        response.put("fileUrl", storedFile.getFileUrl());
        response.put("message", "上传成功，已开始导入");
        return ResponseEntity.ok(response);
    }

    /**
     * 下载已上传的 Excel 文件，文件名必须是 xls 或 xlsx 后缀。
     */
    @GetMapping("/downloadExcel")
    public void downloadExcel(String fileName, HttpServletResponse response) throws IOException {
        if (!isExcelFile(fileName)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        byte[] bytes = fileStorageService.readUploadFile(fileName);
        if (bytes == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }

    private boolean isExcelFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx");
    }
}
