package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.Inspection;

import java.time.Year;
import java.util.List;

public interface InspectionService {
    Inspection getInspectionById(int id);

    String getAttachment(int id);

    List<Inspection> findAllByGroup(String groupName);

    List<Inspection> findAllByGroupAndYear(String groupName, Year year);

    List<Inspection> findAll(String searchInfo);

    List<Inspection> findAllByYear(Year year);

    void addInspection(Inspection inspection);

    void addFeedback(int inspectionId, String groupName, String feedback);

    void modifyAttachment(int inspectionId, String groupName, String attachment);

    void confirmApplication(int inspectionId);

    void denyApplication(int inspectionId);
}
