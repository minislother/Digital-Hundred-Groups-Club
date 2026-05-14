package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.User;
import com.chinahitech.shop.bean.notAddedToDatabase.RegisterUser;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TopManagerService {
    String loginToken(String num, String pwd);

    void register(RegisterUser user);

    void addUser(RegisterUser user);

    String sendAccountEmail(String userNumber) throws Exception;

    void validateCode(String email, String validateCode);

    void sendValidateEmail(String email) throws Exception;

    User getByUserId(String num);

    User login(String num, String pwd);

    void addManager(String userId, String lastPwd, String email);

    void updatePassword(String userId, String password);

    void updatePhone(String userId, String phone);

    void updateDescription(String userId, String description);

    void updateNickname(String userId, String nickname);

    void updateMajor(String userId, String campus, String school, String major);

    List<User> getAllUsers(String searchInfo);

    void addUser(String userId, String lastPwd, String email);

    void updateUserInfo(User user);

    void deleteUser(User user);

    void uploadExcel(MultipartFile file);

    void enqueueUploadExcel(String filePath);

    void importExcelFile(String filePath);
}
