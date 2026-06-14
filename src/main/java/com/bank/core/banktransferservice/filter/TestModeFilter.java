package com.bank.core.banktransferservice.filter;

import com.bank.core.banktransferservice.config.AppProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.*;

/**
 * 测试模式过滤器
 *
 * 【功能说明】
 * 1. 当测试模式启用时，跳过所有参数校验
 * 2. 包装请求体，确保输入流可以被重复读取（防止流被消费后无法再次读取）
 * 3. 放行测试接口路径，不进行签名校验
 *
 * 【请求体重复读取问题】
 * 如果日志拦截器或其他过滤器提前调用了 request.getInputStream()，
 * 请求体流会被消费，导致后续 @RequestBody 注解无法读取数据。
 * 本过滤器通过包装 HttpServletRequest 解决这个问题。
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class TestModeFilter implements Filter {

    private final AppProperties appProperties;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestUri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        
        log.info("【TestModeFilter】收到请求：{} {}，测试模式：{}", method, requestUri, appProperties.getTestMode().isEnabled());

        // 如果测试模式启用，并且是测试接口，跳过校验
        if (appProperties.getTestMode().isEnabled() && requestUri.startsWith("/api/test")) {
            log.info("【测试模式】放行测试接口：{} {}", method, requestUri);

            // 包装请求，确保请求体可以被重复读取
            HttpServletRequest wrappedRequest = new MultiReadHttpServletRequest(httpRequest);
            chain.doFilter(wrappedRequest, response);
            log.info("【测试模式】请求处理完成：{} {}", method, requestUri);
            return;
        }

        // 非测试接口，正常处理
        log.info("【TestModeFilter】非测试接口，继续处理：{} {}", method, requestUri);
        chain.doFilter(request, response);
    }

    /**
     * 可重复读取的 HttpServletRequest 包装类
     *
     * 【设计目的】
     * 当多个过滤器需要读取请求体时（如日志记录、参数校验等），
     * 原始的 InputStream 只能读取一次，会导致后续过滤器无法获取数据。
     * 本包装类将请求体缓存到字节数组中，支持多次读取。
     */
    private static class MultiReadHttpServletRequest extends HttpServletRequestWrapper {

        private byte[] body;

        public MultiReadHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            // 将请求体读取到字节数组中
            try (InputStream is = request.getInputStream()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                body = baos.toByteArray();
            }
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new ByteArrayServletInputStream(body);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        }
    }

    /**
     * 基于字节数组的 ServletInputStream 实现
     */
    private static class ByteArrayServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream inputStream;

        public ByteArrayServletInputStream(byte[] body) {
            this.inputStream = new ByteArrayInputStream(body);
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            // 不支持异步读取
        }
    }
}