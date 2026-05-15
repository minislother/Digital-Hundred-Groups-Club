package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.User;
import com.chinahitech.shop.bean.notAddedToDatabase.RegisterUser;

/**
 * 社团管理员账号服务，负责管理员登录注册、邮箱验证码和个人资料维护。
 */
public interface ManagerService {
    /**
     * 校验管理员账号密码并生成登录令牌。
     */
    String loginToken(String num, String pwd);

    /**
     * 注册社团管理员账号并保存基础账号信息。
     */
    void register(RegisterUser user);

    /**
     * 向管理员账号绑定邮箱发送账号找回或验证邮件。
     */
    String sendAccountEmail(String userNumber) throws Exception;

    /**
     * 校验邮箱验证码是否正确。
     */
    void validateCode(String email, String validateCode);

    /**
     * 向指定邮箱发送验证码邮件。
     */
    void sendValidateEmail(String email) throws Exception;

    /**
     * 根据管理员账号查询用户信息。
     */
    User getByUserId(String num);

    /**
     * 校验管理员账号密码并返回用户信息。
     */
    User login(String num, String pwd);

    /**
     * 新增社团管理员账号。
     */
    void addManager(String userId, String lastPwd, String email);

    /**
     * 修改管理员账号密码。
     */
    void updatePassword(String userId, String password);

    /**
     * 修改管理员手机号。
     */
    void updatePhone(String userId, String phone);

    /**
     * 修改管理员个人简介。
     */
    void updateDescription(String userId, String description);

    /**
     * 修改管理员昵称。
     */
    void updateNickname(String userId, String nickname);
}
