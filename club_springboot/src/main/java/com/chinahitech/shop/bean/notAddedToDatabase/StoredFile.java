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

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileUrl() {
        return fileUrl;
    }
}
