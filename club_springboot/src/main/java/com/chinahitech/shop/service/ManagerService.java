package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.User;
import com.chinahitech.shop.bean.notAddedToDatabase.RegisterUser;

public interface ManagerService {
    String loginToken(String num, String pwd);

    void register(RegisterUser user);

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
}
