package com.chinahitech.shop.service.impl;

import com.chinahitech.shop.bean.notAddedToDatabase.StoredFile;
import com.chinahitech.shop.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("文件存储服务单元测试")
@ExtendWith(MockitoExtension.class)
class FileStorageServiceImplTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("存储文件 - 文件为空抛出异常")
    void testStore_EmptyFile_ThrowException() {
        // Given - 准备测试数据
        FileStorageServiceImpl fileStorageService = createService();
        MockMultipartFile file = new MockMultipartFile("file", new byte[0]);

        // When & Then - 执行被测试方法并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> fileStorageService.store(file));
        assertTrue(exception.getMessage().contains("文件不能为空"));
    }

    @Test
    @DisplayName("存储文件 - 成功写入并可读取")
    void testStore_ValidFile_Success() throws Exception {
        // Given - 准备测试数据和请求上下文
        FileStorageServiceImpl fileStorageService = createService();
        bindRequestContext();
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "demo.txt", "text/plain", content);

        // When - 执行被测试方法
        StoredFile storedFile = fileStorageService.store(file);
        byte[] readContent = fileStorageService.readUploadFile(storedFile.getFileName());

        // Then - 验证结果
        assertNotNull(storedFile);
        assertTrue(storedFile.getFileName().endsWith("_demo.txt"));
        assertTrue(storedFile.getFileUrl().contains("/upload/"));
        assertTrue(Files.exists(tempDir.resolve(storedFile.getFileName())));
        assertArrayEquals(content, readContent);
    }

    @Test
    @DisplayName("存储文件 - 文件名包含路径穿越抛出异常")
    void testStore_PathTraversalFileName_ThrowException() {
        // Given - 准备测试数据
        FileStorageServiceImpl fileStorageService = createService();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../evil.jsp",
                "text/plain",
                "bad".getBytes(StandardCharsets.UTF_8)
        );

        // When & Then - 执行被测试方法并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> fileStorageService.store(file));
        assertTrue(exception.getMessage().contains("文件名不合法"));
    }

    @Test
    @DisplayName("读取文件 - 文件不存在返回空")
    void testReadUploadFile_NotExists_ReturnNull() throws Exception {
        // Given - 准备测试数据
        FileStorageServiceImpl fileStorageService = createService();

        // When - 执行被测试方法
        byte[] result = fileStorageService.readUploadFile("missing.txt");

        // Then - 验证结果
        assertNull(result);
    }

    @Test
    @DisplayName("读取文件 - 文件名包含路径穿越抛出异常")
    void testReadUploadFile_PathTraversal_ThrowException() {
        // Given - 准备测试数据
        FileStorageServiceImpl fileStorageService = createService();

        // When & Then - 执行被测试方法并验证异常
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fileStorageService.readUploadFile("../secret.txt")
        );
        assertTrue(exception.getMessage().contains("文件名不合法"));
    }

    private FileStorageServiceImpl createService() {
        FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());
        return fileStorageService;
    }

    private void bindRequestContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8081);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
