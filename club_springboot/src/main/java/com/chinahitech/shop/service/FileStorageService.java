package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.notAddedToDatabase.StoredFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件存储服务，负责上传文件落盘和已上传文件读取。
 */
public interface FileStorageService {
    /**
     * 保存上传文件并返回可访问的文件信息。
     */
    StoredFile store(MultipartFile file);

    /**
     * 读取已上传文件内容。
     */
    byte[] readUploadFile(String fileName) throws IOException;
}
