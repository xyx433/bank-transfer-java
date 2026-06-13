package com.bank.core.banktransferservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用配置属性
 *
 * 【测试模式配置】
 * 当 test-mode.enabled = true 时：
 * - 跳过签名校验
 * - 放宽部分参数校验
 * - 使用默认值填充非核心字段
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * 测试模式配置
     */
    private TestMode testMode = new TestMode();

    @Data
    public static class TestMode {
        /**
         * 是否启用测试模式
         */
        private boolean enabled = false;

        /**
         * 默认渠道代码
         */
        private String defaultChannel = "MB";
    }
}