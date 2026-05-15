package com.chinahitech.shop.service.impl;

import com.chinahitech.shop.bean.StuApp;
import com.chinahitech.shop.exception.ApplyException;
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
        List<StuApp> appList = stuAppMapper.getByStuAndGroup(stuApp.getStuNumber(), stuApp.getGroupName());
        if (appList.size() >= 5) {
            throw new ApplyException("操作失败");
        }
        int i = stuAppMapper.insert(stuApp);
        if (i != 1) {
            throw new InsertException("操作失败");
        }
    }

    @Override
    public void submit(StuApp stuApp) {
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

    @Override
    public void updateAttachment(int applicationid, String attachment) {
        StuApp stuApp = queryDetailapp(applicationid);
        stuApp.setAttachment(attachment);

        int i = stuAppMapper.updateById(stuApp);
        if (i != 1) {
            throw new UpdateException("操作失败");
        }
    }

    @Override
    public void confirmApplication(Integer applicationid) {
        StuApp stuApp = queryDetailapp(applicationid);
        stuApp.setIsAccepted(true);

        int i = stuAppMapper.updateById(stuApp);
        if (i != 1) {
            throw new UpdateException("操作失败");
        }
    }

    @Override
    public void denyApplication(Integer applicationid) {
        StuApp stuApp = queryDetailapp(applicationid);
        stuApp.setIsAccepted(false);

        int i = stuAppMapper.updateById(stuApp);
        if (i != 1) {
            throw new UpdateException("操作失败");
        }
    }
}
