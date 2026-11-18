package com.chinahitech.shop.service.impl;

import com.chinahitech.shop.bean.StuApp;
import com.chinahitech.shop.exception.ApplyException;
import com.chinahitech.shop.exception.EntityNotFoundException;
import com.chinahitech.shop.exception.InsertException;
import com.chinahitech.shop.exception.UpdateException;
import com.chinahitech.shop.mapper.StuAppMapper;
import com.chinahitech.shop.service.GroupService;
import com.chinahitech.shop.service.StuAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 学生入团申请实现，处理申请创建、提交、附件更新和审核状态变更。
 */
@Service
public class StuAppServiceImpl implements StuAppService {
    @Autowired
    private StuAppMapper stuAppMapper;

    @Autowired
    private GroupService groupService;

    @Override
    public List<StuApp> query() {
        return stuAppMapper.findall();
    }

    @Override
    public List<StuApp> queryMyapp(String stunumber) {
        return stuAppMapper.findMyapp(stunumber);
    }

    @Override
    public void insert(StuApp stuApp) {
        validateApplication(stuApp);
        List<StuApp> appList = stuAppMapper.getByStuAndGroup(stuApp.getStuNumber(), stuApp.getGroupName());
        if (appList != null && appList.size() >= 5) {
            throw new ApplyException("操作失败");
        }
        int i = stuAppMapper.insert(stuApp);
        if (i != 1) {
            throw new InsertException("操作失败");
        }
    }

    /**
     * 提交表单数据并触发后端业务处理。
     */
    @Override
    public void submit(StuApp stuApp) {
        validateApplication(stuApp);
        stuApp.setCreateTime(new Date());
        insert(stuApp);
        groupService.addHot(stuApp.getGroupName());
    }

    @Override
    public List<StuApp> queryRecvapp(String groupname) {
        return stuAppMapper.findRecvapp(groupname);
    }

    @Override
    public StuApp queryDetailapp(Integer id) {
        return stuAppMapper.getById(id);
    }

    @Override
    public String findIsAccepted(Integer id) {
        return stuAppMapper.findIsAccepted(id);
    }

    /**
     * 更新表单字段或业务数据。
     */
    @Override
    public void updateAttachment(int applicationid, String attachment) {
        StuApp stuApp = requireApplication(applicationid);
        stuApp.setAttachment(attachment);

        int i = stuAppMapper.updateById(stuApp);
        if (i != 1) {
            /**
             * 更新表单字段或业务数据。
             */
            throw new UpdateException("操作失败");
        }
    }

    @Override
    public void confirmApplication(Integer applicationid) {
        StuApp stuApp = requireApplication(applicationid);
        stuApp.setIsAccepted(true);

        int i = stuAppMapper.updateById(stuApp);
        if (i != 1) {
            /**
             * 更新表单字段或业务数据。
             */
            throw new UpdateException("操作失败");
        }
    }

    @Override
    public void denyApplication(Integer applicationid) {
        StuApp stuApp = requireApplication(applicationid);
        stuApp.setIsAccepted(false);

        int i = stuAppMapper.updateById(stuApp);
        if (i != 1) {
            /**
             * 更新表单字段或业务数据。
             */
            throw new UpdateException("操作失败");
        }
    }

    private void validateApplication(StuApp stuApp) {
        if (stuApp == null || stuApp.getStuNumber() == null || stuApp.getGroupName() == null) {
            throw new ApplyException("申请信息不完整");
        }
    }

    private StuApp requireApplication(Integer applicationid) {
        StuApp stuApp = queryDetailapp(applicationid);
        if (stuApp == null) {
            throw new EntityNotFoundException("申请记录不存在");
        }
        return stuApp;
    }

    /** 按数据库页读取，列表与总数沿用相同业务筛选条件。 */
    @Override
    public com.chinahitech.shop.utils.PageResult<StuApp> findallPage(Integer pageNum, Integer pageSize) {
        int offset = com.chinahitech.shop.utils.PageUtils.offset(pageNum, pageSize);
        int limit = com.chinahitech.shop.utils.PageUtils.limit(pageSize);
        var records = stuAppMapper.findallPage(offset, limit);
        long total = stuAppMapper.findallCount();
        return com.chinahitech.shop.utils.PageUtils.pageResult(records, total, pageNum, pageSize);
    }

    /** 按数据库页读取，列表与总数沿用相同业务筛选条件。 */
    @Override
    public com.chinahitech.shop.utils.PageResult<StuApp> findMyappPage(String stunumber, Integer pageNum, Integer pageSize) {
        int offset = com.chinahitech.shop.utils.PageUtils.offset(pageNum, pageSize);
        int limit = com.chinahitech.shop.utils.PageUtils.limit(pageSize);
        var records = stuAppMapper.findMyappPage(stunumber, offset, limit);
        long total = stuAppMapper.findMyappCount(stunumber);
        return com.chinahitech.shop.utils.PageUtils.pageResult(records, total, pageNum, pageSize);
    }

    /** 按数据库页读取，列表与总数沿用相同业务筛选条件。 */
    @Override
    public com.chinahitech.shop.utils.PageResult<StuApp> findRecvappPage(String groupName, Integer pageNum, Integer pageSize) {
        int offset = com.chinahitech.shop.utils.PageUtils.offset(pageNum, pageSize);
        int limit = com.chinahitech.shop.utils.PageUtils.limit(pageSize);
        var records = stuAppMapper.findRecvappPage(groupName, offset, limit);
        long total = stuAppMapper.findRecvappCount(groupName);
        return com.chinahitech.shop.utils.PageUtils.pageResult(records, total, pageNum, pageSize);
    }
}
