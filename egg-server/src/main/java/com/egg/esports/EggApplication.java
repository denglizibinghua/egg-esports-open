package com.egg.esports;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.egg.esports.module")
public class EggApplication {

    public static void main(String[] args) {
        SpringApplication.run(EggApplication.class, args);
    }
}
