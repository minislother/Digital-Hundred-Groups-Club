package com.chinahitech.shop.utils;

import java.util.HashMap;
import java.util.Map;

// 统一返回结果
public class Result {

    private Boolean success;

    private Integer code;

    private String message;

    private Map<String, Object> data = new HashMap<String, Object>();

    /**
     * 读取并返回业务数据或页面状态。
     */
    public Boolean getSuccess() {
        return success;
    }

    /**
     * 更新对象属性或页面状态值。
     */
    public void setSuccess(Boolean success) {
        this.success = success;
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 更新对象属性或页面状态值。
     */
    public void setCode(Integer code) {
        this.code = code;
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    public String getMessage() {
        return message;
    }

    /**
     * 更新对象属性或页面状态值。
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * 更新对象属性或页面状态值。
     */
    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    private Result() {
    }

    public static Result ok() {
        Result r = new Result();
        r.setSuccess(true);
        r.setCode(ResultCode.SUCCESS);
        r.setMessage("成功");
        return r;
    }

    public static Result error() {
        Result r = new Result();
        r.setSuccess(false);
        r.setCode(ResultCode.ERROR);
        r.setMessage("失败");
        return r;
    }

    public Result success(Boolean success) {
        this.setSuccess(success);
        return this;
    }

    public Result message(String message) {
        this.setMessage(message);
        return this;
    }

    public Result code(Integer code) {
        this.setCode(code);
        return this;
    }

    /**
     * 提供组件初始响应式数据。
     */
    public Result data(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    /**
     * 提供组件初始响应式数据。
     */
    public Result data(Map<String, Object> map) {
        this.setData(map);
        return this;
    }
}
