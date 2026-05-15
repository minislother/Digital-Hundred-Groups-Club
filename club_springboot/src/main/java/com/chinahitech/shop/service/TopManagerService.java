package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.User;
import com.chinahitech.shop.bean.notAddedToDatabase.RegisterUser;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 超级管理员账号和用户管理服务，负责超级管理员登录、全量用户维护和批量导入。
 */
public interface TopManagerService {
    /**
     * 校验超级管理员账号密码并生成登录令牌。
     */
    String loginToken(String num, String pwd);

    /**
     * 注册超级管理员账号并保存基础账号信息。
     */
    void register(RegisterUser user);

    /**
     * 根据注册信息新增用户账号。
     */
    void addUser(RegisterUser user);

    /**
     * 向账号绑定邮箱发送账号找回或验证邮件。
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
     * 根据用户账号查询用户信息。
     */
    User getByUserId(String num);

    /**
     * 校验超级管理员账号密码并返回用户信息。
     */
    User login(String num, String pwd);

    /**
     * 新增社团管理员账号。
     */
    void addManager(String userId, String lastPwd, String email);

    /**
     * 修改用户密码。
     */
    void updatePassword(String userId, String password);

    /**
     * 修改用户手机号。
     */
    void updatePhone(String userId, String phone);

    /**
     * 修改用户个人简介。
     */
    void updateDescription(String userId, String description);

    /**
     * 修改用户昵称。
     */
    void updateNickname(String userId, String nickname);

    /**
     * 修改用户校区、学院和专业信息。
     */
    void updateMajor(String userId, String campus, String school, String major);

    /**
     * 查询平台全部用户，可按条件筛选。
     */
    List<User> getAllUsers(String searchInfo);

    /**
     * 直接新增用户账号。
     */
    void addUser(String userId, String lastPwd, String email);

    /**
     * 修改用户完整资料。
     */
    void updateUserInfo(User user);

    /**
     * 删除指定用户账号。
     */
    void deleteUser(User user);

    /**
     * 上传并同步导入 Excel 用户数据。
     */
    void uploadExcel(MultipartFile file);

    /**
     * 将 Excel 文件路径加入导入队列，供异步导入处理。
     */
    void enqueueUploadExcel(String filePath);

    /**
     * 读取指定 Excel 文件并批量导入用户数据。
     */
    void importExcelFile(String filePath);
}
