package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.StuApp;

import java.util.List;

public interface StuAppService {
    List<StuApp> query();

    List<StuApp> queryMyapp(String stunumber);

    void insert(StuApp stuApp);

    void submit(StuApp stuApp);

    List<StuApp> queryRecvapp(String groupname);

    StuApp queryDetailapp(Integer id);

    String findIsAccepted(Integer id);

    void updateAttachment(int applicationid, String attachment);

    void confirmApplication(Integer applicationid);

    void denyApplication(Integer applicationid);
}
