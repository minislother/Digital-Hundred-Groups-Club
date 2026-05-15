package com.chinahitech.shop.service.impl;

import com.chinahitech.shop.service.StuService;
import com.chinahitech.shop.bean.notAddedToDatabase.RegisterUser;
import com.chinahitech.shop.utils.EmailService;
import com.chinahitech.shop.utils.JwtUtils;
import com.chinahitech.shop.utils.PasswordUtil;
import com.chinahitech.shop.utils.RedisUtils;

import com.chinahitech.shop.bean.User;
import com.chinahitech.shop.exception.EntityNotFoundException;
import com.chinahitech.shop.exception.InsertException;
import com.chinahitech.shop.exception.UpdateException;
import com.chinahitech.shop.mapper.StuMapper;
import com.chinahitech.shop.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * 学生账号实现，处理登录令牌、注册、邮箱验证码和学生资料更新。
 */
@Service
public class StuServiceImpl implements StuService {
    @Autowired
    private StuMapper stuMapper;

    @Override
    public String loginToken(String num, String pwd) {
        User stu = login(num, pwd);
        if (stu.getPassword() == null) {
            return null;
        }
        return JwtUtils.generateToken(stu.getUserId(), JwtUtils.ROLE_STUDENT);
    }

    @Override
    public void register(RegisterUser user) {
        validateCode(user.getEmail(), user.getValicode());
        addStudent(user.getUserId(), user.getPassword(), user.getEmail());
    }

    @Override
    public String sendAccountEmail(String stuNumber) throws Exception {
        User student = getByStuNumber(stuNumber);
        sendValidateEmail(student.getEmail());
        return student.getEmail();
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

    public User getByStuNumber(String num) {
        User stu = stuMapper.getByNum(num);
        if (stu == null) {
            throw new EntityNotFoundException("操作失败");
        }
        return stu;
    }

    public User login(String num, String pwd) {
        User stu = stuMapper.getByNum(num);
        if (stu == null){
            throw new EntityNotFoundException("操作失败");
        }
        String oldPwd = stu.getPassword();

        if (!PasswordUtil.matches(oldPwd,pwd)){
            stu.setPassword(null);
        }
        return stu;
    }

    public void addStudent(String stuNumber, String lastPwd, String email) {
        Date date = new Date();
        String salt = UUID.randomUUID().toString().toUpperCase();
        String currPwd = PasswordUtil.encode(lastPwd);

        User stu = stuMapper.getByNum(stuNumber);
        if (stu != null) {
            throw new UseridDuplicateException("操作失败");
        } else {
            int i = stuMapper.addStudent(stuNumber, currPwd, email, salt, date, date, 0);
            if(i != 1){
                throw new InsertException("操作失败");
            }
        }
    }


    public void updatePassword(String stuNumber, String password){
        User stu = stuMapper.getByNum(stuNumber);
        String salt = stu.getSalt();
        String newMD5pwd = PasswordUtil.encode(password);
        Date date = new Date();

        stu.setPassword(newMD5pwd);
        stu.setModifyTime(date);

        int i = stuMapper.updateById(stu);
        if(i != 1){
            throw new UpdateException("操作失败");
        }
    }

    public void updatePhone(String stuNumber, String phone){
        Date date = new Date();

        User stu = getByStuNumber(stuNumber);
        stu.setPhone(phone);
        stu.setModifyTime(date);

        int i = stuMapper.updateById(stu);
        if(i != 1){
            throw new UpdateException("操作失败");
        }
    }

    public void updateDescription(String stuNumber, String description){
        Date date = new Date();

        User stu = getByStuNumber(stuNumber);
        stu.setDescription(description);
        stu.setModifyTime(date);

        int i = stuMapper.updateById(stu);
        if(i != 1){
            throw new UpdateException("操作失败");
        }
    }

    public void updateNickname(String stuNumber, String nickname){
        Date date = new Date();

        User stu = getByStuNumber(stuNumber);
        stu.setNickname(nickname);
        stu.setModifyTime(date);

        int i = stuMapper.updateById(stu);
        if(i != 1){
            throw new UpdateException("操作失败");
        }
    }

}

