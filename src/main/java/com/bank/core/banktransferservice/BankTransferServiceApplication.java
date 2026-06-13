package com.bank.core.banktransferservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 银行核心转账系统启动类
 *
 * 【关键配置】
 * @MapperScan: 扫描 Mapper 接口，自动创建实现类
 * @SpringBootApplication: Spring Boot 自动配置
 */
@SpringBootApplication
@MapperScan("com.bank.core.banktransferservice.mapper")
public class BankTransferServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankTransferServiceApplication.class, args);
    }

}
