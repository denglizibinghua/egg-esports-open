package com.egg.esports.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
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

        // 校验文件头魔数：防止扩展名伪装（如 .exe 改成 .jpg）
        // 至少读取 12 字节，覆盖所有支持格式的魔数长度（WebP 最长 12 字节）
        byte[] header = new byte[12];
        try (InputStream is = file.getInputStream()) {
            int read = is.read(header);
            if (read < 12 || !isValidMagicBytes(header, ext)) {
                throw new BusinessException("文件内容与扩展名不匹配");
            }
        } catch (IOException e) {
            throw new BusinessException("文件读取失败");
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

    /**
     * 校验文件头魔数是否与扩展名匹配
     * <p>各格式魔数参考：
     * <ul>
     *   <li>JPG: FF D8 FF</li>
     *   <li>PNG: 89 50 4E 47 0D 0A 1A 0A</li>
     *   <li>GIF: 47 49 46 38 (GIF8)</li>
     *   <li>WebP: 52 49 46 46 ?? ?? ?? ?? 57 45 42 50 (RIFF...WEBP)</li>
     * </ul>
     *
     * @param header 文件头字节数组（至少 12 字节）
     * @param ext    扩展名（含点号，如 ".jpg"）
     * @return 匹配返回 true
     */
    private boolean isValidMagicBytes(byte[] header, String ext) {
        String extLower = ext.toLowerCase(Locale.ROOT);
        switch (extLower) {
            case ".jpg":
            case ".jpeg":
                // JPG: FF D8 FF
                return (header[0] & 0xFF) == 0xFF
                        && (header[1] & 0xFF) == 0xD8
                        && (header[2] & 0xFF) == 0xFF;
            case ".png":
                // PNG: 89 50 4E 47 0D 0A 1A 0A
                return (header[0] & 0xFF) == 0x89
                        && (header[1] & 0xFF) == 0x50
                        && (header[2] & 0xFF) == 0x4E
                        && (header[3] & 0xFF) == 0x47
                        && (header[4] & 0xFF) == 0x0D
                        && (header[5] & 0xFF) == 0x0A
                        && (header[6] & 0xFF) == 0x1A
                        && (header[7] & 0xFF) == 0x0A;
            case ".gif":
                // GIF: 47 49 46 38 (GIF8)，后续可能是 37 a/39 a
                return (header[0] & 0xFF) == 0x47
                        && (header[1] & 0xFF) == 0x49
                        && (header[2] & 0xFF) == 0x46
                        && (header[3] & 0xFF) == 0x38;
            case ".webp":
                // WebP: RIFF (52 49 46 46) + 4 字节大小 + WEBP (57 45 42 50)
                return (header[0] & 0xFF) == 0x52
                        && (header[1] & 0xFF) == 0x49
                        && (header[2] & 0xFF) == 0x46
                        && (header[3] & 0xFF) == 0x46
                        && (header[8] & 0xFF) == 0x57
                        && (header[9] & 0xFF) == 0x45
                        && (header[10] & 0xFF) == 0x42
                        && (header[11] & 0xFF) == 0x50;
            default:
                return false;
        }
    }
}
