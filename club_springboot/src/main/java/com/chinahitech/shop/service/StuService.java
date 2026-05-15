package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.User;
import com.chinahitech.shop.bean.notAddedToDatabase.RegisterUser;

/**
 * 学生账号服务，负责学生登录注册、邮箱验证码和个人资料维护。
 */
public interface StuService {
    /**
     * 校验学生账号密码并生成登录令牌。
     */
    String loginToken(String num, String pwd);

    /**
     * 注册学生账号并保存基础账号信息。
     */
    void register(RegisterUser user);

    /**
     * 向学生账号绑定邮箱发送账号找回或验证邮件。
     */
    String sendAccountEmail(String stuNumber) throws Exception;

    /**
     * 校验邮箱验证码是否正确。
     */
    void validateCode(String email, String validateCode);

    /**
     * 向指定邮箱发送验证码邮件。
     */
    void sendValidateEmail(String email) throws Exception;

    /**
     * 根据学生学号查询账号信息。
     */
    User getByStuNumber(String num);

    /**
     * 校验学生账号密码并返回用户信息。
     */
    User login(String num, String pwd);

    /**
     * 新增学生账号。
     */
    void addStudent(String stuNumber, String lastPwd, String email);

    /**
     * 修改学生账号密码。
     */
    void updatePassword(String stuNumber, String password);

    /**
     * 修改学生手机号。
     */
    void updatePhone(String stuNumber, String phone);

    /**
     * 修改学生个人简介。
     */
    void updateDescription(String stuNumber, String description);

    /**
     * 修改学生昵称。
     */
    void updateNickname(String stuNumber, String nickname);
}
