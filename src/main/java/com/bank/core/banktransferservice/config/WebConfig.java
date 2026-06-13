package com.bank.core.banktransferservice.config;

import com.bank.core.banktransferservice.filter.TestModeFilter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类
 *
 * 【测试模式配置】
 * 当测试模式启用时：
 * - 注册测试模式过滤器，优先于其他过滤器执行
 * - 跳过所有参数校验
 * - 确保请求体可以被重复读取
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    /**
     * 注册测试模式过滤器
     *
     * @param testModeFilter 测试模式过滤器
     * @return 过滤器注册 bean
     */
    @Bean
    public FilterRegistrationBean<TestModeFilter> testModeFilterRegistration(TestModeFilter testModeFilter) {
        FilterRegistrationBean<TestModeFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(testModeFilter);
        // 设置最高优先级，确保第一个执行
        registration.setOrder(0);
        // 只对测试接口生效
        registration.addUrlPatterns("/api/test/*");
        return registration;
    }

    /**
     * 配置拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 测试模式下不添加任何拦截器
        if (!appProperties.getTestMode().isEnabled()) {
            // 生产模式下的拦截器配置
            // TODO: 添加生产环境的拦截器
        }
    }

    @PostConstruct
    public void init() {
        log.info("WebConfig initialized, test mode: {}", appProperties.getTestMode().isEnabled());
    }
}