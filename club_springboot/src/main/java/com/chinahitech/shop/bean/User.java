package com.chinahitech.shop.bean;

import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Date;

@EqualsAndHashCode(callSuper = false)
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@com.baomidou.mybatisplus.annotation.TableName(value = "user", autoResultMap = true)
public class User{
    @JsonProperty("userId")
    @TableId
    private String userId;
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @com.alibaba.fastjson.annotation.JSONField(serialize = false)
    private String password;
    @com.baomidou.mybatisplus.annotation.TableField(typeHandler = com.chinahitech.shop.utils.NameTypeHandler.class)
    @ToString.Exclude
    private String userName;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @com.alibaba.fastjson.annotation.JSONField(serialize = false)
    private String userNameLookup;
    public String getUserNameLookup() { return com.chinahitech.shop.utils.ContactCipher.lookup(userName, "userName"); }
    @com.baomidou.mybatisplus.annotation.TableField(typeHandler = com.chinahitech.shop.utils.EmailTypeHandler.class)
    @ToString.Exclude
    private String email;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @com.alibaba.fastjson.annotation.JSONField(serialize = false)
    private String emailLookup;
    public String getEmailLookup() { return com.chinahitech.shop.utils.ContactCipher.lookup(email, "email"); }
    @com.baomidou.mybatisplus.annotation.TableField(typeHandler = com.chinahitech.shop.utils.PhoneTypeHandler.class)
    @ToString.Exclude
    private String phone;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @com.alibaba.fastjson.annotation.JSONField(serialize = false)
    private String phoneLookup;
    public String getPhoneLookup() { return com.chinahitech.shop.utils.ContactCipher.lookup(phone, "phone"); }
    private String campus;
    private String school;
    private String major;
    private String description;
    private int status;
    private String nickname;
    private String avatar;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @com.alibaba.fastjson.annotation.JSONField(serialize = false)
    @ToString.Exclude
    private String salt;
    private String createUser;
    private Date createTime;
    private String modifyUser;
    private Date modifyTime;
}
