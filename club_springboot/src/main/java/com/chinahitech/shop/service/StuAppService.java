package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.StuApp;

import java.util.List;

/**
 * 学生入团申请业务服务，负责申请提交、查询、附件更新和审核流转。
 */
public interface StuAppService {
    /**
     * 查询全部学生入团申请。
     */
    List<StuApp> query();

    /**
     * 查询指定学生提交的入团申请。
     */
    List<StuApp> queryMyapp(String stunumber);

    /**
     * 新增一条学生入团申请草稿或记录。
     */
    void insert(StuApp stuApp);

    /**
     * 提交学生入团申请并进入社团管理员审核流程。
     */
    void submit(StuApp stuApp);

    /**
     * 查询指定社团收到的入团申请。
     */
    List<StuApp> queryRecvapp(String groupname);

    /**
     * 查询学生入团申请详情。
     */
    StuApp queryDetailapp(Integer id);

    /**
     * 查询学生入团申请是否已通过。
     */
    String findIsAccepted(Integer id);

    /**
     * 更新学生入团申请附件地址。
     */
    void updateAttachment(int applicationid, String attachment);

    /**
     * 通过学生入团申请。
     */
    void confirmApplication(Integer applicationid);

    /**
     * 驳回学生入团申请。
     */
    void denyApplication(Integer applicationid);
}
