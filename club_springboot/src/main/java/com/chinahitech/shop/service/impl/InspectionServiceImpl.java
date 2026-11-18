package com.chinahitech.shop.service.impl;

import com.chinahitech.shop.bean.Inspection;
import com.chinahitech.shop.exception.EntityNotFoundException;
import com.chinahitech.shop.exception.UpdateException;
import com.chinahitech.shop.mapper.InspectionMapper;
import com.chinahitech.shop.service.InspectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.Date;
import java.util.List;

/**
 * 年审业务实现，处理年审记录查询、材料附件修改、反馈填写和审核状态流转。
 */
@Service
public class InspectionServiceImpl implements InspectionService {
    @Autowired
    private InspectionMapper inspectionMapper;

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public Inspection getInspectionById(int id) {
        Inspection inspection = inspectionMapper.getById(id);
        if (inspection == null) {
            throw new EntityNotFoundException("操作失败");
        }
        return inspection;
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    @Override
    public String getAttachment(int id) {
        return getInspectionById(id).getAttachment();
    }

    @Override
    public List<Inspection> findAllByGroup(String groupName) {
        return inspectionMapper.getByGroup(groupName);
    }

    @Override
    public List<Inspection> findAllByGroupAndYear(String groupName, Year year) {
        return inspectionMapper.getByGroupAndYear(groupName, year);
    }

    @Override
    public List<Inspection> findAll(String searchInfo) {
        if (searchInfo == null || searchInfo.trim().isEmpty()) {
            return inspectionMapper.findAll();
        }
        return inspectionMapper.getBySearchInfo(searchInfo);
    }

    @Override
    public List<Inspection> findAllByYear(Year year) {
        return inspectionMapper.getByYear(year);
    }

    /**
     * 新增业务数据或追加界面状态。
     */
    @Override
    public void addInspection(Inspection inspection) {
        Date date = new Date();
        inspection.setId(null);
        inspection.setYear(Year.now());
        inspection.setCreateTime(date);
        inspection.setModifyTime(date);
        int i = inspectionMapper.insert(inspection);
        if (i != 1) {
            /**
             * 更新表单字段或业务数据。
             */
            throw new UpdateException("操作失败");
        }
    }

    /**
     * 新增业务数据或追加界面状态。
     */
    @Override
    public void addFeedback(int inspectionId, String groupName, String feedback) {
        Date date = new Date();
        int i = inspectionMapper.addFeedback(inspectionId, groupName, feedback, date);
        if (i != 1) {
            /**
             * 更新表单字段或业务数据。
             */
            throw new UpdateException("操作失败");
        }
    }

    @Override
    public void modifyAttachment(int inspectionId, String groupName, String attachment) {
        Date date = new Date();
        int rowsUpdated = inspectionMapper.modifyAttachment(inspectionId, groupName, attachment, date);
        if (rowsUpdated == 0) {
            /**
             * 更新表单字段或业务数据。
             */
            throw new UpdateException("操作失败");
        }
    }

    @Override
    public void confirmApplication(int inspectionId) {
        int i = inspectionMapper.confirmApplicationByid(inspectionId);
        if (i != 1) {
            /**
             * 更新表单字段或业务数据。
             */
            throw new UpdateException("操作失败");
        }
    }

    @Override
    public void denyApplication(int inspectionId) {
        int i = inspectionMapper.denyApplicationByid(inspectionId);
        if (i != 1) {
            /**
             * 更新表单字段或业务数据。
             */
            throw new UpdateException("操作失败");
        }
    }

    /** 按数据库页读取，列表与总数沿用相同业务筛选条件。 */
    @Override
    public com.chinahitech.shop.utils.PageResult<Inspection> getByGroupPage(String groupName, Integer pageNum, Integer pageSize) {
        int offset = com.chinahitech.shop.utils.PageUtils.offset(pageNum, pageSize);
        int limit = com.chinahitech.shop.utils.PageUtils.limit(pageSize);
        var records = inspectionMapper.getByGroupPage(groupName, offset, limit);
        long total = inspectionMapper.getByGroupCount(groupName);
        return com.chinahitech.shop.utils.PageUtils.pageResult(records, total, pageNum, pageSize);
    }

    /** 按数据库页读取，列表与总数沿用相同业务筛选条件。 */
    @Override
    public com.chinahitech.shop.utils.PageResult<Inspection> getByGroupAndYearPage(String groupName, java.time.Year year, Integer pageNum, Integer pageSize) {
        int offset = com.chinahitech.shop.utils.PageUtils.offset(pageNum, pageSize);
        int limit = com.chinahitech.shop.utils.PageUtils.limit(pageSize);
        var records = inspectionMapper.getByGroupAndYearPage(groupName, year, offset, limit);
        long total = inspectionMapper.getByGroupAndYearCount(groupName, year);
        return com.chinahitech.shop.utils.PageUtils.pageResult(records, total, pageNum, pageSize);
    }

    /** 按数据库页读取，列表与总数沿用相同业务筛选条件。 */
    @Override
    public com.chinahitech.shop.utils.PageResult<Inspection> getByYearPage(java.time.Year year, Integer pageNum, Integer pageSize) {
        int offset = com.chinahitech.shop.utils.PageUtils.offset(pageNum, pageSize);
        int limit = com.chinahitech.shop.utils.PageUtils.limit(pageSize);
        var records = inspectionMapper.getByYearPage(year, offset, limit);
        long total = inspectionMapper.getByYearCount(year);
        return com.chinahitech.shop.utils.PageUtils.pageResult(records, total, pageNum, pageSize);
    }

    /** 按数据库页读取，列表与总数沿用相同业务筛选条件。 */
    @Override
    public com.chinahitech.shop.utils.PageResult<Inspection> getBySearchInfoPage(String searchInfo, Integer pageNum, Integer pageSize) {
        int offset = com.chinahitech.shop.utils.PageUtils.offset(pageNum, pageSize);
        int limit = com.chinahitech.shop.utils.PageUtils.limit(pageSize);
        var records = inspectionMapper.getBySearchInfoPage(searchInfo, offset, limit);
        long total = inspectionMapper.getBySearchInfoCount(searchInfo);
        return com.chinahitech.shop.utils.PageUtils.pageResult(records, total, pageNum, pageSize);
    }
}
