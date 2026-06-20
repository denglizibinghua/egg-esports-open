package com.egg.esports.module.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 操作日志 Service（异步写 DB，避免拖慢业务接口）
 */
@Slf4j
@Service
public class EggOperationLogService {

    @Autowired
    private EggOperationLogMapper operationLogMapper;

    /**
     * 异步保存操作日志
     * 用 CompletableFuture.runAsync 提交到 ForkJoinPool，不阻塞业务线程
     */
    public void saveAsync(EggOperationLog logEntry) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                operationLogMapper.insert(logEntry);
            } catch (Exception e) {
                // 日志写入失败不能影响业务，只打错误日志
                log.error("操作日志保存失败: {}", e.getMessage(), e);
            }
        });
    }
}
