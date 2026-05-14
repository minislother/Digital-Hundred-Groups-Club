package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.User;
import com.chinahitech.shop.bean.notAddedToDatabase.RegisterUser;

public interface StuService {
    String loginToken(String num, String pwd);

    void register(RegisterUser user);

    String sendAccountEmail(String stuNumber) throws Exception;

    void validateCode(String email, String validateCode);

    void sendValidateEmail(String email) throws Exception;

    User getByStuNumber(String num);

    User login(String num, String pwd);

    void addStudent(String stuNumber, String lastPwd, String email);

    void updatePassword(String stuNumber, String password);

    void updatePhone(String stuNumber, String phone);

    void updateDescription(String stuNumber, String description);

    void updateNickname(String stuNumber, String nickname);
}
