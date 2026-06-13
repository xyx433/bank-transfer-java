package com.bank.core.banktransferservice.exception;

import com.bank.core.banktransferservice.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * 【设计目的】
 * 1. 统一处理所有异常，返回标准格式
 * 2. 区分业务异常和系统异常
 * 3. 记录异常日志，便于问题排查
 * 4. 保护敏感信息，避免泄露
 *
 * 【异常分类】
 * 1. 业务异常（BusinessException）：返回错误码和错误消息
 * 2. 参数校验异常：返回字段错误信息
 * 3. 系统异常：返回系统错误码，隐藏具体错误信息
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * 【业务异常处理】
     * 返回具体的错误码和错误消息，便于客户端处理
     *
     * @param e 业务异常
     * @return 错误响应
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：错误码={}，错误消息={}，是否可重试={}",
                e.getErrorCode(), e.getErrorMessage(), e.isRetryable());

        return Result.fail(e.getErrorCode(), e.getErrorMessage());
    }

    /**
     * 处理参数校验异常（@Valid 校验失败）
     *
     * 【参数校验异常处理】
     * JSR-303 校验失败时，返回具体的字段错误信息：
     * - 字段名
     * - 错误消息
     *
     * @param e 参数校验异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 获取所有字段错误
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("参数校验失败：{}", errorMessage);

        return Result.fail("PARAM_INVALID", errorMessage);
    }

    /**
     * 处理绑定异常
     *
     * @param e 绑定异常
     * @return 错误响应
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("参数绑定失败：{}", errorMessage);

        return Result.fail("PARAM_BIND_ERROR", errorMessage);
    }

    /**
     * 处理请求参数缺失异常
     *
     * @param e 参数缺失异常
     * @return 错误响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("请求参数缺失：{}", e.getParameterName());

        return Result.fail("PARAM_MISSING", String.format("缺少必填参数：%s", e.getParameterName()));
    }

    /**
     * 处理请求体解析异常
     *
     * @param e 请求体解析异常
     * @return 错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败：{}", e.getMessage());

        return Result.fail("REQUEST_BODY_INVALID", "请求体格式错误，请检查JSON格式");
    }

    /**
     * 处理参数类型不匹配异常
     *
     * @param e 参数类型不匹配异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配：参数名={}，期望类型={}",
                e.getName(), e.getRequiredType());

        return Result.fail("PARAM_TYPE_ERROR",
                String.format("参数类型错误：%s 应为 %s 类型",
                        e.getName(), e.getRequiredType().getSimpleName()));
    }

    /**
     * 处理唯一键冲突异常（幂等性校验失败）
     *
     * 【唯一键冲突处理】
     * 当插入重复的幂等号时，数据库唯一索引约束会抛出 DuplicateKeyException：
     * - 这是防止重复扣款的第二道防线（数据库层面）
     * - 返回幂等性错误，提示客户端使用新的幂等号
     *
     * @param e 一键冲突异常
     * @return 错误响应
     */
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("唯一键冲突，可能是幂等号重复：{}", e.getMessage());

        return Result.fail("10008", "交易已处理，请勿重复提交");
    }

    /**
     * 处理404异常
     *
     * @param e 404异常
     * @return 错误响应
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("接口不存在：{}", e.getRequestURL());

        return Result.fail("API_NOT_FOUND", String.format("接口不存在：%s", e.getRequestURL()));
    }

    /**
     * 处理其他未捕获异常
     *
     * 【系统异常处理】
     * 对于未预期的异常：
     * - 记录完整的异常堆栈（便于排查）
     * - 返回通用错误消息（避免泄露敏感信息）
     * - 标记为可重试（可能是临时性故障）
     *
     * @param e 未捕获异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        
        // 获取详细错误信息（包括嵌套异常）
        StringBuilder detailMessage = new StringBuilder();
        Throwable current = e;
        int depth = 0;
        while (current != null && depth < 5) {
            if (depth > 0) {
                detailMessage.append(" -> ");
            }
            detailMessage.append(current.getClass().getSimpleName())
                        .append(": ")
                        .append(current.getMessage());
            current = current.getCause();
            depth++;
        }
        
        log.error("详细错误链：{}", detailMessage.toString());

        // 生产环境不应返回具体异常信息，避免泄露敏感信息
        return Result.fail("99999", "系统异常，请稍后重试");
    }

    /**
     * 处理空指针异常
     *
     * @param e 空指针异常
     * @return 错误响应
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常：", e);

        return Result.fail("99999", "系统异常，请稍后重试");
    }

    /**
     * 处理非法参数异常
     *
     * @param e 非法参数异常
     * @return 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数：{}", e.getMessage());

        return Result.fail("PARAM_ILLEGAL", e.getMessage());
    }
}