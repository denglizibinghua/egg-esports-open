package com.egg.esports.module.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 操作日志 Service（异步写 DB，避免拖慢业务接口）
 * <p>使用自定义线程池，避免占用 ForkJoinPool.commonPool() 影响其他并行流任务。
 */
@Slf4j
@Service
public class EggOperationLogService {

    @Autowired
    private EggOperationLogMapper operationLogMapper;

    /**
     * 自定义线程池：
     * <ul>
     *   <li>核心 2 线程，最大 4 线程，队列容量 1000</li>
     *   <li>守护线程，JVM 退出时不阻塞</li>
     *   <li>拒绝策略：队列满时丢弃并打日志（日志丢失优于拖垮业务）</li>
     * </ul>
     * 不复用 ForkJoinPool.commonPool()，避免与 Stream.parallel() 等并行流任务互相阻塞。
     */
    private final ThreadPoolExecutor logExecutor = new ThreadPoolExecutor(
            2, 4,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "op-log-" + counter.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            },
            new ThreadPoolExecutor.DiscardPolicy() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    log.warn("操作日志队列已满，丢弃日志任务");
                }
            }
    );

    /**
     * 异步保存操作日志
     * 用自定义线程池提交，不阻塞业务线程
     */
    public void saveAsync(EggOperationLog logEntry) {
        CompletableFuture.runAsync(() -> {
            try {
                operationLogMapper.insert(logEntry);
            } catch (Exception e) {
                // 日志写入失败不能影响业务，只打错误日志
                log.error("操作日志保存失败: {}", e.getMessage(), e);
            }
        }, logExecutor);
    }

    /**
     * 优雅关闭线程池：等待最多 5 秒排空队列
     */
    @PreDestroy
    public void shutdown() {
        logExecutor.shutdown();
        try {
            if (!logExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                logExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
