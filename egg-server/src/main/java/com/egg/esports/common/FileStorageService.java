package com.egg.esports.common;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口
 * 支持本地存储、OSS、MinIO 等多种实现
 */
public interface FileStorageService {

    /**
     * 上传文件
     * @param file      上传的文件
     * @param directory 存储子目录（如 avatars）
     * @return 文件访问 URL
     */
    String upload(MultipartFile file, String directory);

    /**
     * 删除文件
     * @param fileUrl 文件 URL 或相对路径
     */
    void delete(String fileUrl);
}
