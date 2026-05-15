package com.chinahitech.shop.controller;

import com.chinahitech.shop.aop.RepeatLimit;
import com.chinahitech.shop.bean.User;
import com.chinahitech.shop.bean.notAddedToDatabase.RegisterUser;
import com.chinahitech.shop.service.ManagerService;
import com.chinahitech.shop.utils.JwtUtils;
import com.chinahitech.shop.utils.Result;
import com.chinahitech.shop.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager")
public class ManagerController {
    @Autowired
    private ManagerService managerService;

    /**
     * 社团管理员登录，校验账号密码并返回 JWT token。
     */
    @RepeatLimit
    @PostMapping("/login")
    public Result login(String userId, String password) {
        String token = managerService.loginToken(userId, password);
        if (token == null) {
            return Result.error().message("用户名或密码不正确");
        }
        return Result.ok().data("token", token);
    }

    /**
     * 注册社团管理员账号。
     */
    @RepeatLimit
    @PostMapping("/register")
    public Result register(@RequestBody RegisterUser user) {
        managerService.register(user);
        return Result.ok().message("注册成功");
    }

    /**
     * 根据管理员编号发送账号关联邮箱，并返回邮箱地址。
     */
    @RepeatLimit
    @PostMapping("/getEmail")
    public Result getEmail(String userNumber) throws Exception {
        String email = managerService.sendAccountEmail(userNumber);
        return Result.ok().data("email", email);
    }

    /**
     * 校验邮箱验证码是否正确。
     */
    @RepeatLimit
    @PostMapping("/getValidate")
    public Result getValidate(String email, String validateCode) {
        managerService.validateCode(email, validateCode);
        return Result.ok().message("验证成功");
    }

    /**
     * 向指定邮箱发送验证码。
     */
    @RepeatLimit
    @PostMapping("/validateEmail")
    public Result validateEmail(String email) throws Exception {
        managerService.sendValidateEmail(email);
        return Result.ok().message("验证码已发送");
    }

    /**
     * 当前登录管理员修改自己的登录密码。
     */
    @RepeatLimit
    @PostMapping("/modifyPass")
    public Result modifyPassword(String userId, String password, Authentication authentication) {
        SecurityUtils.requireSelf(authentication, userId);
        managerService.updatePassword(userId, password);
        return Result.ok();
    }

    /**
     * 当前登录管理员修改自己的手机号。
     */
    @RepeatLimit
    @PostMapping("/modifyPhone")
    public Result modifyPhone(String userId, String phone, Authentication authentication) {
        SecurityUtils.requireSelf(authentication, userId);
        managerService.updatePhone(userId, phone);
        return Result.ok();
    }

    /**
     * 当前登录管理员修改自己的个人简介。
     */
    @RepeatLimit
    @PostMapping("/modifyDescription")
    public Result modifyDescription(String userId, String description, Authentication authentication) {
        SecurityUtils.requireSelf(authentication, userId);
        managerService.updateDescription(userId, description);
        return Result.ok();
    }

    /**
     * 当前登录管理员修改自己的昵称。
     */
    @RepeatLimit
    @PostMapping("/modifyNickname")
    public Result modifyNickname(String userId, String nickname, Authentication authentication) {
        SecurityUtils.requireSelf(authentication, userId);
        managerService.updateNickname(userId, nickname);
        return Result.ok();
    }

    /**
     * 当前登录管理员查询自己的个人资料。
     */
    @RepeatLimit
    @PostMapping("/profile")
    public Result getProfile(String userId, Authentication authentication) {
        SecurityUtils.requireSelf(authentication, userId);
        User user = managerService.getByUserId(userId);
        return Result.ok().data("user", user);
    }

    /**
     * 根据请求头 token 解析当前管理员身份，返回前端需要的用户名和头像信息。
     */
    @RepeatLimit
    @GetMapping("/info")
    public Result info(@RequestHeader(value = "X-Token", required = false) String xToken,
                       @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = JwtUtils.resolveToken(xToken, authorization);
        String username = JwtUtils.getClaimsByToken(token).getSubject();
        String url = "https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2021%2F1120%2F783a7b4ej00r2tvvx002fd200hs00hsg00hs00hs.jpg&thumbnail=660x2147483647&quality=80&type=jpg";
        return Result.ok().data("name", username).data("avatar", url);
    }

    /**
     * 社团管理员退出登录，前端收到成功响应后清理本地 token。
     */
    @RepeatLimit
    @PostMapping("/logout")
    public Result logout() {
        return Result.ok();
    }
}
