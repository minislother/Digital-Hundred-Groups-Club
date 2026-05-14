package com.chinahitech.shop.service.impl;

import cn.hutool.core.io.FileUtil;
import com.chinahitech.shop.bean.notAddedToDatabase.StoredFile;
import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png", "gif", "zip", "xls", "xlsx", "txt")
    );

    @Value("${upload-dir}")
    private String uploadDir;

    @Override
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("FILE_EMPTY", "\u6587\u4ef6\u4e0d\u80fd\u4e3a\u7a7a");
        }

        String fileName = generateUniqueFileName(file.getOriginalFilename());
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path targetLocation = uploadPath.resolve(fileName).normalize();
        if (!targetLocation.startsWith(uploadPath)) {
            throw new BusinessException("FILE_NAME_INVALID", "\u6587\u4ef6\u540d\u4e0d\u5408\u6cd5");
        }

        try {
            if (!FileUtil.exist(uploadPath.toString())) {
                FileUtil.mkdir(uploadPath.toString());
            }
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new BusinessException("UPLOAD_ERROR", "\u6587\u4ef6\u4e0a\u4f20\u5931\u8d25");
        }

        String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .pathSegment("upload")
                .pathSegment(fileName)
                .toUriString();
        return new StoredFile(fileName, targetLocation.toString(), fileUrl);
    }

    @Override
    public byte[] readUploadFile(String fileName) throws IOException {
        String safeName = sanitizeFileName(fileName);
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = uploadPath.resolve(safeName).normalize();
        if (!filePath.startsWith(uploadPath)) {
            throw new BusinessException("FILE_NAME_INVALID", "\u6587\u4ef6\u540d\u4e0d\u5408\u6cd5");
        }
        if (!FileUtil.exist(filePath.toString())) {
            return null;
        }
        return FileUtil.readBytes(filePath.toString());
    }

    private String generateUniqueFileName(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String safeName = originalFilename == null || originalFilename.trim().isEmpty()
                ? "file"
                : sanitizeFileName(originalFilename);
        return timestamp + "_" + safeName;
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new BusinessException("FILE_NAME_INVALID", "\u6587\u4ef6\u540d\u4e0d\u5408\u6cd5");
        }
        String normalized = fileName.replace('\\', '/');
        if (normalized.contains("/") || normalized.contains("..")) {
            throw new BusinessException("FILE_NAME_INVALID", "\u6587\u4ef6\u540d\u4e0d\u5408\u6cd5");
        }
        String extension = extension(normalized);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("FILE_TYPE_INVALID", "\u6587\u4ef6\u7c7b\u578b\u4e0d\u5408\u6cd5");
        }
        return normalized;
    }

    private String extension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }
}
