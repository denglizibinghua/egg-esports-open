package com.egg.esports.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 赔率配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "egg.odds")
public class OddsConfig {

    /** 抽水比例（0.10 = 10%） */
    private BigDecimal commissionRate = new BigDecimal("0.10");

    /** 最低赔率 */
    private BigDecimal minOdds = new BigDecimal("1.01");

    /** 最高赔率 */
    private BigDecimal maxOdds = new BigDecimal("999.00");
}