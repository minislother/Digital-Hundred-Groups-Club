package com.chinahitech.shop.service.impl;

import com.chinahitech.shop.service.TopManagerService;
import com.chinahitech.shop.bean.notAddedToDatabase.RegisterUser;
import com.chinahitech.shop.utils.EmailService;
import com.chinahitech.shop.utils.JwtUtils;
import com.chinahitech.shop.utils.PasswordUtil;
import com.chinahitech.shop.utils.RedisUtils;
import com.chinahitech.shop.utils.ScanExcel;

import cn.hutool.json.JSONUtil;
import com.chinahitech.shop.bean.User;
import com.chinahitech.shop.constant.MqTopics;
import com.chinahitech.shop.exception.*;
import com.chinahitech.shop.mapper.TopManagerMapper;
import com.chinahitech.shop.mq.KafkaProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 超级管理员业务实现，处理超级管理员登录、用户资料管理、Excel 批量导入和导入消息投递。
 */
@Service
public class TopManagerServiceImpl implements TopManagerService {
    @Autowired
    private TopManagerMapper topManagerMapper;

    @Autowired
    private KafkaProducer kafkaProducer;

    private final String XLSX = ".xlsx";
    private final String XLS=".xls";

    @Override
    public String loginToken(String num, String pwd) {
        User user = login(num, pwd);
        if (user.getPassword() == null || user.getStatus() < 1) {
            return null;
        }
        return JwtUtils.generateToken(user.getUserId(), JwtUtils.ROLE_TOP_MANAGER);
    }

    @Override
    public void register(RegisterUser user) {
        validateCode(user.getEmail(), user.getValicode());
        addManager(user.getUserId(), user.getPassword(), user.getEmail());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUser(RegisterUser user) {
        validateCode(user.getEmail(), user.getValicode());
        addUser(user.getUserId(), user.getPassword(), user.getEmail());
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
        User stu = topManagerMapper.getTopManagerByNum(num);
        if (stu == null) {
            throw new EntityNotFoundException("操作失败");
        }
        return stu;
    }

    public User login(String num, String pwd) {
        User user = topManagerMapper.getTopManagerByNum(num);
        if (user == null){
            throw new EntityNotFoundException("操作失败");
        }
        String oldPwd = user.getPassword();

        String salt = user.getSalt();
        if (!PasswordUtil.matches(oldPwd,pwd)){
            user.setPassword(null);
        }
        return user;
    }


    public void addManager(String userId, String lastPwd, String email) {
        Date date = new Date();
        String salt = UUID.randomUUID().toString().toUpperCase();
        String currPwd = PasswordUtil.encode(lastPwd);

        User user = topManagerMapper.getTopManagerByNum(userId);
        if (user != null) {
            throw new UseridDuplicateException("操作失败");
        } else {
            int i = topManagerMapper.addManager(userId, currPwd, email, salt, date, date, 10);
            if(i != 1){
                throw new InsertException("操作失败");
            }
        }
    }


    public void updatePassword(String userId, String password){
        User stu = getByUserId(userId);
        String newMD5pwd = PasswordUtil.encode(password);

        Date date = new Date();

        stu.setPassword(newMD5pwd);
        stu.setModifyTime(date);

        int i = topManagerMapper.updateById(stu);
        if(i != 1){
            throw new UpdateException("操作失败");
        }
    }

    public void updatePhone(String userId, String phone){
        Date date = new Date();

        User stu = getByUserId(userId);
        stu.setPhone(phone);
        stu.setModifyTime(date);

        int i = topManagerMapper.updateById(stu);
        if(i != 1){
            throw new UpdateException("操作失败");
        }
    }

    public void updateDescription(String userId, String description){
        Date date = new Date();

        User stu = getByUserId(userId);
        stu.setDescription(description);
        stu.setModifyTime(date);

        int i = topManagerMapper.updateById(stu);
        if(i != 1){
            throw new UpdateException("操作失败");
        }
    }

    public void updateNickname(String userId, String nickname){
        Date date = new Date();

        User stu = getByUserId(userId);
        stu.setNickname(nickname);
        stu.setModifyTime(date);

        int i = topManagerMapper.updateById(stu);
        if(i != 1){
            throw new UpdateException("操作失败");
        }
    }

    public void updateMajor(String userId, String campus, String school, String major){
        Date date = new Date();

        User stu = getByUserId(userId);
        stu.setCampus(campus);
        stu.setSchool(school);
        stu.setMajor(major);
        stu.setModifyTime(date);

        int i = topManagerMapper.updateById(stu);
        if(i != 1){
            throw new UpdateException("操作失败");
        }
    }

    public List<User> getAllUsers(String searchInfo) {
        if (searchInfo == null || searchInfo.trim().isEmpty()) {
            return topManagerMapper.getAllUsers();
        } else {
            return topManagerMapper.getUser(searchInfo);
        }
    }


    @Transactional(rollbackFor = Exception.class)
    public void addUser(String userId, String lastPwd, String email) {
        Date date = new Date();

        String salt = UUID.randomUUID().toString().toUpperCase();
        String currPwd = PasswordUtil.encode(lastPwd);
        User user = new User();
        user.setUserId(userId);
        user.setPassword(currPwd);
        user.setSalt(salt);
        user.setEmail(email);
        user.setCreateTime(date);
        user.setModifyTime(date);
        user.setStatus(0);

        int i = topManagerMapper.insert(user);
        if(i != 1){
            throw new InsertException("操作失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(User user){
        Date date = new Date();

        user.setModifyTime(date);

        int i = topManagerMapper.updateById(user);
        if(i != 1){
            throw new UpdateException("操作失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(User user) {
        String id = user.getUserId();
        int i = topManagerMapper.deleteById(id);
        if(i != 1){
            throw new UpdateException("操作失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void uploadExcel(MultipartFile file) {
        try {
            saveImportedUsers(new ScanExcel().readExcel(file));
        } catch (Exception e) {
            throw new FileUploadException("操作失败");
        }
    }

    public void enqueueUploadExcel(String filePath) {
        Map<String, Object> message = new HashMap<>();
        message.put("filePath", filePath);
        if (!kafkaProducer.send(MqTopics.USER_IMPORT, JSONUtil.toJsonStr(message))) {
            importExcelFile(filePath);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void importExcelFile(String filePath) {
        try {
            saveImportedUsers(new ScanExcel().readExcel(new File(filePath)));
        } catch (Exception e) {
            throw new FileUploadException("操作失败");
        }
    }

    private void saveImportedUsers(List<User> users) {
        if (users != null && !users.isEmpty()){
            for (User user : users) {
                Date date = new Date();

        String salt = UUID.randomUUID().toString().toUpperCase();
        String currPwd = PasswordUtil.encode(user.getPassword());
                user.setPassword(currPwd);
                user.setSalt(salt);
                user.setCreateTime(date);
                user.setModifyTime(date);
                user.setStatus(0);
                User tempUser = topManagerMapper.getUserByNum(user.getUserId());
                if (tempUser != null) {
                    throw new UseridDuplicateException("操作失败");
                }
                int i = topManagerMapper.insert(user);
                if(i != 1){
                    throw new InsertException("操作失败");
                }
            }
        } else {
            throw new FileEmptyException("操作失败");
        }
    }
}


