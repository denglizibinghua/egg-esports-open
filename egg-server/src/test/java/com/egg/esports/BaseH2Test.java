package com.egg.esports;

import com.egg.esports.module.audit.EggOperationLogService;
import com.egg.esports.module.notification.EggNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * H2 内存库测试基类
 * <p>所有测试类继承本类，共享 Spring 上下文和 H2 数据库（DB_CLOSE_DELAY=-1 保证库不关闭）。
 * <p>核心 mapper（EggBetMapper / EggUserMapper / EggMatchMapper）使用真实 H2 数据库，
 * 以便抓到 SQL 层面的乐观锁 bug；通知和操作日志 Service 用 @MockBean 替换，避免副作用。
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseH2Test {

    /** 通知 Service 用 mock 替换，避免写 egg_notification 表 */
    @MockBean
    protected EggNotificationService notificationService;

    /** 操作日志 Service 用 mock 替换，AOP 切面调用 saveAsync 时无副作用 */
    @MockBean
    protected EggOperationLogService operationLogService;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /**
     * 每个测试方法前清空三张核心表，保证测试隔离。
     * 不重置自增计数器（测试中显式指定 ID 插入，H2 允许指定自增列值）。
     */
    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("DELETE FROM egg_bet");
        jdbcTemplate.execute("DELETE FROM egg_match");
        jdbcTemplate.execute("DELETE FROM egg_user");
    }
}
