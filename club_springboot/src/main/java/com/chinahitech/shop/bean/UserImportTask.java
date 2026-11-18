package com.chinahitech.shop.bean;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 持久化导入进度；内部文件位置和执行租约不向浏览器暴露。 */
@Data
public class UserImportTask {
 private String id;
 private String uploader;
 private String status;
 @JsonIgnore private String filePath;
 @JsonIgnore private String owner;
 private long processed;
 private long succeeded;
 private long duplicates;
 private long invalid;
 private Long total;
 private String errorMessage;
}
