package com.egg.esports.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "egg.loan")
public class LoanConfig {

    private int maxLoan = 2000;
    private int reliefAmount = 500;
    private int dailyStipend = 100;
    private double autoRepayRatio = 0.5;

    public int getMaxLoan() {
        return maxLoan;
    }

    public void setMaxLoan(int maxLoan) {
        this.maxLoan = maxLoan;
    }

    public int getReliefAmount() {
        return reliefAmount;
    }

    public void setReliefAmount(int reliefAmount) {
        this.reliefAmount = reliefAmount;
    }

    public int getDailyStipend() {
        return dailyStipend;
    }

    public void setDailyStipend(int dailyStipend) {
        this.dailyStipend = dailyStipend;
    }

    public double getAutoRepayRatio() {
        return autoRepayRatio;
    }

    public void setAutoRepayRatio(double autoRepayRatio) {
        this.autoRepayRatio = autoRepayRatio;
    }
}