package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.notAddedToDatabase.StoredFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {
    StoredFile store(MultipartFile file);

    byte[] readUploadFile(String fileName) throws IOException;
}
