package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.Inspection;

import java.time.Year;
import java.util.List;

/**
 * 年审业务服务，负责社团年审材料提交、查询、反馈和审核。
 */
public interface InspectionService {
    /**
     * 根据年审 ID 查询年审详情。
     */
    Inspection getInspectionById(int id);

    /**
     * 获取指定年审材料的附件地址。
     */
    String getAttachment(int id);

    /**
     * 查询指定社团的全部年审记录。
     */
    List<Inspection> findAllByGroup(String groupName);

    /**
     * 查询指定社团在指定年份的年审记录。
     */
    List<Inspection> findAllByGroupAndYear(String groupName, Year year);

    /**
     * 查询超级管理员可查看的年审记录，可按条件筛选。
     */
    List<Inspection> findAll(String searchInfo);

    /**
     * 查询指定年份的全部年审记录。
     */
    List<Inspection> findAllByYear(Year year);

    /**
     * 新增社团年审申请或材料记录。
     */
    void addInspection(Inspection inspection);

    /**
     * 为年审记录添加审核反馈。
     */
    void addFeedback(int inspectionId, String groupName, String feedback);

    /**
     * 修改指定社团年审记录的附件地址。
     */
    void modifyAttachment(int inspectionId, String groupName, String attachment);

    /**
     * 通过年审申请。
     */
    void confirmApplication(int inspectionId);

    /**
     * 驳回年审申请。
     */
    void denyApplication(int inspectionId);
}
