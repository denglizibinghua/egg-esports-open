package com.egg.esports.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

/**
 * 本地文件存储实现
 * 文件保存在服务器本地磁盘，通过 /uploads/** 路径访问
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "egg.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    @Value("${egg.upload.path:uploads}")
    private String basePath;

    @Override
    public String upload(MultipartFile file, String directory) {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new BusinessException("文件名无效");
        }

        // 校验扩展名
        String ext = originalName.substring(originalName.lastIndexOf("."));
        if (!ext.matches("(?i)\\.(jpg|jpeg|png|gif|webp)")) {
            throw new BusinessException("仅支持 jpg/png/gif/webp 格式");
        }

        // 校验大小
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException("文件大小不能超过 2MB");
        }

        try {
            String fileName = UUID.randomUUID() + ext;
            File uploadDir = new File(basePath, directory);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            File dest = new File(uploadDir, fileName);
            file.transferTo(dest);

            String url = "/uploads/" + directory + "/" + fileName;
            log.info("文件上传成功: {} ({} bytes)", url, file.getSize());
            return url;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            return;
        }

        try {
            String relativePath = fileUrl.substring("/uploads/".length());
            File file = new File(basePath, relativePath);
            if (file.exists()) {
                file.delete();
                log.info("文件删除成功: {}", fileUrl);
            }
        } catch (Exception e) {
            log.warn("文件删除失败: {} - {}", fileUrl, e.getMessage());
        }
    }
}
