package com.egg.esports.module.user;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.egg.esports.common.BusinessException;
import com.egg.esports.common.FileStorageService;
import com.egg.esports.common.JwtUtils;
import com.egg.esports.config.LoanConfig;
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

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private LoanConfig loanConfig;

    /**
     * 注册
     */
    public EggUser register(String username, String password, String nickname, String email) {
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

        // Email validation (optional field)
        if (email != null && !email.trim().isEmpty()) {
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                throw new BusinessException("邮箱格式不正确");
            }
            // Check email uniqueness (optional)
            EggUser existingEmail = userMapper.selectByEmail(email);
            if (existingEmail != null) {
                throw new BusinessException("该邮箱已被注册");
            }
            user.setEmail(email);
        }

        user.setPoints(1000);
        user.setTotalBets(0);
        user.setCorrectBets(0);
        user.setWinStreak(0);
        user.setMaxWinStreak(0);
        user.setRole(0);
        user.setLoanAmount(0);
        user.setLoanLimit(loanConfig.getMaxLoan());
        user.setCreateTime(LocalDateTime.now());

        userMapper.insert(user);

        return sanitizeUser(user);
    }

    /**
     * 登录
     * <p>失败5次锁定15分钟（按账号）
     */
    public Map<String, Object> login(String username, String password) {
        EggUser user = userMapper.selectByUsername(username);
        // 用户不存在也抛相同消息，防止用户名枚举
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 1. 检查账户是否被锁定
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            long remainMinutes = java.time.Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1;
            throw new BusinessException(423, "账户已锁定，请" + remainMinutes + "分钟后再试");
        }

        // 2. 校验密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            // 失败次数 +1
            userMapper.incrementLoginAttempts(user.getId());
            int attempts = (user.getLoginAttempts() == null ? 0 : user.getLoginAttempts()) + 1;

            if (attempts >= 5) {
                // 达到5次，锁定15分钟
                LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(15);
                LambdaUpdateWrapper<EggUser> lockWrapper = new LambdaUpdateWrapper<>();
                lockWrapper.eq(EggUser::getId, user.getId())
                        .set(EggUser::getLockedUntil, lockedUntil);
                userMapper.update(null, lockWrapper);
                log.warn("用户[{}]连续登录失败5次，锁定至 {}", username, lockedUntil);
                throw new BusinessException(423, "账户已锁定15分钟");
            }
            throw new BusinessException("用户名或密码错误");
        }

        // 3. 登录成功，重置失败次数
        userMapper.resetLoginAttempts(user.getId());

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
        // 委托给文件存储服务（支持本地/OSS/MinIO 切换）
        String avatarUrl = fileStorageService.upload(file, "avatars");

        EggUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 删除旧头像（如果存在）
        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            fileStorageService.delete(user.getAvatar());
        }

        user.setAvatar(avatarUrl);
        userMapper.updateById(user);

        return avatarUrl;
    }

    /**
     * 修改密码
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            throw new BusinessException("原密码不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new BusinessException("新密码不能为空");
        }
        if (newPassword.length() < 6 || newPassword.length() > 30) {
            throw new BusinessException("新密码长度需在6-30个字符之间");
        }
        EggUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        // Update
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    /**
     * 清除密码字段，防止泄露
     */
    private EggUser sanitizeUser(EggUser user) {
        user.setPassword(null);
        return user;
    }
}