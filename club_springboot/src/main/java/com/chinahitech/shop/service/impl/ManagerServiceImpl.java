package com.chinahitech.shop.service.impl;

import com.chinahitech.shop.service.ManagerService;
import com.chinahitech.shop.bean.notAddedToDatabase.RegisterUser;
import com.chinahitech.shop.utils.EmailService;
import com.chinahitech.shop.utils.JwtUtils;
import com.chinahitech.shop.utils.PasswordUtil;
import com.chinahitech.shop.utils.RedisUtils;

import com.chinahitech.shop.bean.User;
import com.chinahitech.shop.exception.EntityNotFoundException;
import com.chinahitech.shop.exception.InsertException;
import com.chinahitech.shop.exception.UpdateException;
import com.chinahitech.shop.mapper.ManagerMapper;
import com.chinahitech.shop.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Service
public class ManagerServiceImpl implements ManagerService {
    @Autowired
    private ManagerMapper managerMapper;

    @Override
    public String loginToken(String num, String pwd) {
        User user = login(num, pwd);
        if (user.getPassword() == null || user.getStatus() < 1) {
            return null;
        }
        return JwtUtils.generateToken(user.getUserId(), JwtUtils.ROLE_MANAGER);
    }

    @Override
    public void register(RegisterUser user) {
        validateCode(user.getEmail(), user.getValicode());
        addManager(user.getUserId(), user.getPassword(), user.getEmail());
    }

    @Override
    public String sendAccountEmail(String userNumber) throws Exception {
        User user = getByUserId(userNumber);
        sendValidateEmail(user.getEmail());
        return user.getEmail();
    }

    @Override
    public void validateCode(String email, String validateCode) {
        Object cached = RedisUtils.get(email);
        if (!Objects.equals(cached == null ? null : cached.toString(), validateCode)) {
            throw new ServiceException("验证码错误");
        }
    }

    @Override
    public void sendValidateEmail(String email) throws Exception {
        new EmailService(email).sendEmail();
    }

    public User getByUserId(String num) {
        User stu = managerMapper.getByNum(num);
        if (stu == null) {
            throw new EntityNotFoundException("操作失败");
        }
        return stu;
    }

    public User login(String num, String pwd) {
        User user = managerMapper.getByNum(num);
        if (user == null){
            throw new EntityNotFoundException("操作失败");
        }
        String oldPwd = user.getPassword();

//        String newPwd = MD5handler(pwd,salt);
        if (!PasswordUtil.matches(oldPwd,pwd)){
//            System.out.println(oldPwd);
//            System.out.println(pwd);
            user.setPassword(null);
// throw new PwdNotMatchException("Password error");
// Check user permission level.
//            user.setPassword(null);
        }
        return user;
    }


    public void addManager(String userId, String lastPwd, String email) {
        Date date = new Date();

//            System.out.println(lastPwd);
        String salt = UUID.randomUUID().toString().toUpperCase();
//            System.out.println(salt);
        String currPwd = PasswordUtil.encode(lastPwd);

        User user = managerMapper.getByNum(userId);
        if (user != null) {
            throw new UseridDuplicateException("操作失败");
        } else {
            int i = managerMapper.addManager(userId, currPwd, email, salt, date, date, 1);
            if(i != 1){
                throw new InsertException("操作失败");
            }
        }
    }


    public void updatePassword(String userId, String password){
        User stu = managerMapper.getByNum(userId);
//        String oldMD5pwd = stu.getPwd();
        String salt = stu.getSalt();
//        if (!isEqual(oldMD5pwd, oldPwd, salt)){
// throw new PwdNotMatchException("Password error");
//        }
        String newMD5pwd = PasswordUtil.encode(password);
//        System.out.println(newMD5pwd);
        Date date = new Date();

        stu.setPassword(newMD5pwd);
        stu.setModifyTime(date);

        int i = managerMapper.updateById(stu);
        if(i != 1){
            throw new UpdateException("操作失败");
        }
    }

    public void updatePhone(String userId, String phone){
        Date date = new Date();

        User stu = getByUserId(userId);
        stu.setPhone(phone);
        stu.setModifyTime(date);

        int i = managerMapper.updateById(stu);
        if(i != 1){
            throw new UpdateException("操作失败");
        }
    }

    public void updateDescription(String userId, String description){
        Date date = new Date();

        User stu = getByUserId(userId);
        stu.setDescription(description);
        stu.setModifyTime(date);

        int i = managerMapper.updateById(stu);
        if(i != 1){
            throw new UpdateException("操作失败");
        }
    }

    public void updateNickname(String userId, String nickname){
        Date date = new Date();

        User stu = getByUserId(userId);
        stu.setNickname(nickname);
        stu.setModifyTime(date);

        int i = managerMapper.updateById(stu);
        if(i != 1){
            throw new UpdateException("操作失败");
        }
    }

}


