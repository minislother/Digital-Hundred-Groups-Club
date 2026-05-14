package com.chinahitech.shop.controller;

import com.chinahitech.shop.aop.RepeatLimit;
import com.chinahitech.shop.bean.User;
import com.chinahitech.shop.bean.notAddedToDatabase.RegisterUser;
import com.chinahitech.shop.service.StuService;
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
@RequestMapping("/student")
public class StuController {
    @Autowired
    private StuService stuService;

    @RepeatLimit
    @PostMapping("/login")
    public Result login(String stuNumber, String password) {
        String token = stuService.loginToken(stuNumber, password);
        if (token == null) {
            return Result.error().message("用户名或密码不正确");
        }
        return Result.ok().data("token", token);
    }

    @RepeatLimit
    @PostMapping("/register")
    public Result register(@RequestBody RegisterUser student) {
        stuService.register(student);
        return Result.ok().message("注册成功");
    }

    @RepeatLimit
    @PostMapping("/getEmail")
    public Result getEmail(String stuNumber) throws Exception {
        String email = stuService.sendAccountEmail(stuNumber);
        return Result.ok().data("email", email);
    }

    @RepeatLimit
    @PostMapping("/getValidate")
    public Result getValidate(String email, String validateCode) {
        stuService.validateCode(email, validateCode);
        return Result.ok().message("验证成功");
    }

    @RepeatLimit
    @PostMapping("/validateEmail")
    public Result validateEmail(String email) throws Exception {
        stuService.sendValidateEmail(email);
        return Result.ok().message("验证码已发送");
    }

    @RepeatLimit
    @PostMapping("/modifyPass")
    public Result modifyPassword(String stuNumber, String password, Authentication authentication) {
        SecurityUtils.requireSelf(authentication, stuNumber);
        stuService.updatePassword(stuNumber, password);
        return Result.ok();
    }

    @RepeatLimit
    @PostMapping("/modifyPhone")
    public Result modifyPhone(String stuNumber, String phone, Authentication authentication) {
        SecurityUtils.requireSelf(authentication, stuNumber);
        stuService.updatePhone(stuNumber, phone);
        return Result.ok();
    }

    @RepeatLimit
    @PostMapping("/modifyDescription")
    public Result modifyDescription(String stuNumber, String description, Authentication authentication) {
        SecurityUtils.requireSelf(authentication, stuNumber);
        stuService.updateDescription(stuNumber, description);
        return Result.ok();
    }

    @RepeatLimit
    @PostMapping("/modifyNickname")
    public Result modifyNickname(String stuNumber, String nickname, Authentication authentication) {
        SecurityUtils.requireSelf(authentication, stuNumber);
        stuService.updateNickname(stuNumber, nickname);
        return Result.ok();
    }

    @RepeatLimit
    @PostMapping("/profile")
    public Result getProfile(String stuNumber, Authentication authentication) {
        SecurityUtils.requireSelf(authentication, stuNumber);
        User student = stuService.getByStuNumber(stuNumber);
        return Result.ok().data("student", student);
    }

    @RepeatLimit
    @GetMapping("/info")
    public Result info(@RequestHeader(value = "X-Token", required = false) String xToken,
                       @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = JwtUtils.resolveToken(xToken, authorization);
        String username = JwtUtils.getClaimsByToken(token).getSubject();
        String url = "https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2021%2F1120%2F783a7b4ej00r2tvvx002fd200hs00hsg00hs00hs.jpg&thumbnail=660x2147483647&quality=80&type=jpg";
        return Result.ok().data("name", username).data("avatar", url);
    }

    @RepeatLimit
    @PostMapping("/logout")
    public Result logout() {
        return Result.ok();
    }
}
