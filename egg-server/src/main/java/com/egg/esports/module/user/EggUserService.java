package com.egg.esports.module.user;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.egg.esports.common.BusinessException;
import com.egg.esports.common.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class EggUserService {

    @Autowired
    private EggUserMapper userMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 注册
     */
    public EggUser register(String username, String password, String nickname) {
        // 参数校验
        if (username == null || username.trim().isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        if (username.length() < 3 || username.length() > 20) {
            throw new BusinessException("用户名长度需在3-20个字符之间");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new BusinessException("密码不能为空");
        }
        if (password.length() < 6 || password.length() > 30) {
            throw new BusinessException("密码长度需在6-30个字符之间");
        }

        // 检查用户名是否已存在
        EggUser existing = userMapper.selectByUsername(username);
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }

        // 创建用户
        EggUser user = new EggUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null && !nickname.trim().isEmpty() ? nickname : username);
        user.setPoints(1000);
        user.setTotalBets(0);
        user.setCorrectBets(0);
        user.setWinStreak(0);
        user.setMaxWinStreak(0);
        user.setRole(0);
        user.setCreateTime(LocalDateTime.now());

        userMapper.insert(user);

        return sanitizeUser(user);
    }

    /**
     * 登录
     */
    public Map<String, Object> login(String username, String password) {
        EggUser user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", sanitizeUser(user));

        return result;
    }

    /**
     * 根据 ID 获取用户信息（不含密码）
     */
    public EggUser getById(Long id) {
        EggUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return sanitizeUser(user);
    }

    /**
     * 原子更新积分，防止积分变为负数
     */
    public boolean updatePoints(Long userId, int delta) {
        LambdaUpdateWrapper<EggUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(EggUser::getId, userId);

        if (delta >= 0) {
            wrapper.setSql("points = points + " + delta);
        } else {
            // 防止积分变为负数
            wrapper.setSql("points = GREATEST(0, points + (" + delta + "))");
        }

        int rows = userMapper.update(null, wrapper);
        return rows > 0;
    }

    /**
     * 更新个人资料
     */
    public EggUser updateProfile(Long userId, String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new BusinessException("昵称不能为空");
        }
        if (nickname.length() > 20) {
            throw new BusinessException("昵称长度不能超过20个字符");
        }

        EggUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        user.setNickname(nickname.trim());
        userMapper.updateById(user);
        return sanitizeUser(user);
    }

    /**
     * 上传头像
     */
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new BusinessException("文件名无效");
        }

        String ext = originalName.substring(originalName.lastIndexOf("."));
        if (!ext.matches("(?i)\\.(jpg|jpeg|png|gif|webp)")) {
            throw new BusinessException("仅支持 jpg/png/gif/webp 格式");
        }

        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException("文件大小不能超过 2MB");
        }

        try {
            String fileName = java.util.UUID.randomUUID() + ext;
            java.io.File uploadDir = new java.io.File("uploads/avatars");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            java.io.File dest = new java.io.File(uploadDir, fileName);
            file.transferTo(dest);

            String avatarUrl = "/uploads/avatars/" + fileName;
            EggUser user = userMapper.selectById(userId);
            user.setAvatar(avatarUrl);
            userMapper.updateById(user);

            return avatarUrl;
        } catch (Exception e) {
            throw new BusinessException("头像上传失败");
        }
    }

    /**
     * 清除密码字段，防止泄露
     */
    private EggUser sanitizeUser(EggUser user) {
        user.setPassword(null);
        return user;
    }
}