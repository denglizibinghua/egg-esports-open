package com.egg.esports.module.user;

import com.egg.esports.BaseH2Test;
import com.egg.esports.common.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EggUserService 测试（注册 + 登录 + 锁定机制）
 * <p>覆盖任务书 G1：注册成功/用户名重复/邮箱重复/登录成功/密码错误/锁定机制（5 次失败锁 15 分钟）
 */
@DisplayName("EggUserService 注册与登录")
class EggUserServiceTest extends BaseH2Test {

    @Autowired
    private EggUserService userService;

    @Autowired
    private EggUserMapper userMapper;

    // ==================== 注册测试 ====================

    @Test
    @DisplayName("注册成功：返回用户（password 已清除），积分 1000")
    void testRegister_success() {
        EggUser user = userService.register("alice", "password123", "Alice", "alice@example.com");

        assertNotNull(user.getId(), "注册后应有 ID");
        assertEquals("alice", user.getUsername());
        assertEquals("Alice", user.getNickname());
        assertEquals("alice@example.com", user.getEmail());
        assertEquals(1000, user.getPoints(), "初始积分应为 1000");
        assertNull(user.getPassword(), "返回的 user 不应含密码");
        assertEquals(0, user.getRole(), "默认角色为普通用户");

        // DB 中密码已加密
        EggUser dbUser = userMapper.selectById(user.getId());
        assertNotNull(dbUser.getPassword(), "DB 中密码不应为空");
        assertTrue(dbUser.getPassword().startsWith("$2"), "密码应为 BCrypt 加密格式");
    }

    @Test
    @DisplayName("注册失败：用户名已存在")
    void testRegister_duplicateUsername() {
        userService.register("bob", "password123", "Bob", null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                userService.register("bob", "anotherpwd", "Bob2", null));
        assertEquals("用户名已存在", ex.getMessage());
    }

    @Test
    @DisplayName("注册失败：邮箱已被注册")
    void testRegister_duplicateEmail() {
        userService.register("user1", "password123", "User1", "shared@example.com");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                userService.register("user2", "password123", "User2", "shared@example.com"));
        assertEquals("该邮箱已被注册", ex.getMessage());
    }

    @Test
    @DisplayName("注册失败：邮箱格式不正确")
    void testRegister_invalidEmail() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                userService.register("user3", "password123", "User3", "not-an-email"));
        assertEquals("邮箱格式不正确", ex.getMessage());
    }

    // ==================== 登录测试 ====================

    @Test
    @DisplayName("登录成功：返回 token 和用户信息")
    void testLogin_success() {
        userService.register("charlie", "secret123", "Charlie", null);

        Map<String, Object> result = userService.login("charlie", "secret123");

        assertNotNull(result.get("token"), "应返回 JWT token");
        EggUser user = (EggUser) result.get("user");
        assertNotNull(user);
        assertEquals("charlie", user.getUsername());
        assertNull(user.getPassword(), "返回的 user 不应含密码");

        // 登录成功后 login_attempts 应重置为 0
        EggUser dbUser = userMapper.selectByUsername("charlie");
        assertEquals(0, dbUser.getLoginAttempts(), "登录成功后失败次数应重置");
    }

    @Test
    @DisplayName("登录失败：密码错误，login_attempts +1")
    void testLogin_wrongPassword() {
        userService.register("dave", "correctpwd", "Dave", null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                userService.login("dave", "wrongpwd"));
        assertEquals("用户名或密码错误", ex.getMessage());

        EggUser dbUser = userMapper.selectByUsername("dave");
        assertEquals(1, dbUser.getLoginAttempts(), "失败次数应为 1");
    }

    @Test
    @DisplayName("登录锁定：连续 5 次失败后锁定 15 分钟")
    void testLogin_lockAfter5Failures() {
        userService.register("eve", "correctpwd", "Eve", null);

        // 前 4 次失败：抛"用户名或密码错误"
        for (int i = 1; i <= 4; i++) {
            BusinessException ex = assertThrows(BusinessException.class, () ->
                    userService.login("eve", "wrongpwd"));
            assertEquals("用户名或密码错误", ex.getMessage());

            EggUser dbUser = userMapper.selectByUsername("eve");
            assertEquals(i, dbUser.getLoginAttempts(), "第 " + i + " 次失败后 attempts 应为 " + i);
            assertNull(dbUser.getLockedUntil(), "前 4 次不应锁定");
        }

        // 第 5 次失败：抛"账户已锁定15分钟"，locked_until 被设置
        BusinessException ex5 = assertThrows(BusinessException.class, () ->
                userService.login("eve", "wrongpwd"));
        assertTrue(ex5.getMessage().contains("锁定"), "第 5 次应提示锁定: " + ex5.getMessage());
        assertEquals(423, ex5.getCode(), "锁定应返回 423 状态码");

        EggUser lockedUser = userMapper.selectByUsername("eve");
        assertNotNull(lockedUser.getLockedUntil(), "locked_until 应已设置");
        assertTrue(lockedUser.getLockedUntil().isAfter(LocalDateTime.now()),
                "locked_until 应在当前时间之后");

        // 第 6 次：即使密码正确也因锁定抛 423
        BusinessException ex6 = assertThrows(BusinessException.class, () ->
                userService.login("eve", "correctpwd"));
        assertEquals(423, ex6.getCode(), "锁定期间登录应返回 423");
        assertTrue(ex6.getMessage().contains("锁定"), "应提示账户已锁定");
    }
}
