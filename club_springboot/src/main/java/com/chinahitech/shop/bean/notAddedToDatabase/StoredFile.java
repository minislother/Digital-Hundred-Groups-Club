package com.chinahitech.shop.bean.notAddedToDatabase;

public class StoredFile {
    private final String fileName;
    private final String filePath;
    private final String fileUrl;

    public StoredFile(String fileName, String filePath, String fileUrl) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileUrl = fileUrl;
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    public String getFileUrl() {
        return fileUrl;
    }
}
