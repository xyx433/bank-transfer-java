package com.bank.core.banktransferservice.exception;

import lombok.Getter;

/**
 * 业务异常类
 *
 * 【设计目的】
 * 1. 统一的业务异常封装，包含错误码和错误消息
 * 2. 支持是否可重试标识
 * 3. 全局异常处理器统一处理返回标准格式
 *
 * 【错误码规范】
 * - 10001-10012: 业务错误（不可重试或有限重试）
 * - 20001-20004: 系统错误（可重试）
 * - 99999: 未知错误
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final String errorCode;

    /**
     * 错误消息
     */
    private final String errorMessage;

    /**
     * 是否可重试
     */
    private final boolean retryable;

    /**
     * 失败原因码
     */
    private final String failReasonCode;

    /**
     * 失败原因描述
     */
    private final String failReasonDesc;

    /**
     * 构造业务异常
     *
     * @param errorCode    错误码
     * @param errorMessage 错误消息
     * @param retryable    是否可重试
     */
    public BusinessException(String errorCode, String errorMessage, boolean retryable) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.retryable = retryable;
        this.failReasonCode = errorCode;
        this.failReasonDesc = errorMessage;
    }

    /**
     * 构造业务异常（带详细失败原因）
     *
     * @param errorCode      错误码
     * @param errorMessage   错误消息
     * @param retryable      是否可重试
     * @param failReasonCode 失败原因码
     * @param failReasonDesc 失败原因描述
     */
    public BusinessException(String errorCode, String errorMessage, boolean retryable,
                             String failReasonCode, String failReasonDesc) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.retryable = retryable;
        this.failReasonCode = failReasonCode;
        this.failReasonDesc = failReasonDesc;
    }

    // ==================== 预定义业务异常 ====================

    /**
     * 余额不足异常
     * 错误码：10001
     */
    public static BusinessException balanceInsufficient(String message) {
        return new BusinessException("10001", "付款账户余额不足", false, "10001", message);
    }

    /**
     * 单日限额超限异常
     * 错误码：10002
     */
    public static BusinessException dailyLimitExceeded(String message) {
        return new BusinessException("10002", "单日累计转账限额超限", false, "10002", message);
    }

    /**
     * 单笔限额超限异常
     * 错误码：10003
     */
    public static BusinessException singleLimitExceeded(String message) {
        return new BusinessException("10003", "单笔转账限额超限", false, "10003", message);
    }

    /**
     * 账户状态异常
     * 错误码：10004
     */
    public static BusinessException accountStatusAbnormal(String message) {
        return new BusinessException("10004", "账户状态异常", false, "10004", message);
    }

    /**
     * 反洗钱可疑交易异常
     * 错误码：10005
     */
    public static BusinessException amlSuspicious(String message) {
        return new BusinessException("10005", "反洗钱可疑交易", false, "10005", message);
    }

    /**
     * 收款人黑名单异常
     * 错误码：10006
     */
    public static BusinessException payeeBlocked(String message) {
        return new BusinessException("10006", "收款账户在黑名单中", false, "10006", message);
    }

    /**
     * 认证失败异常
     * 错误码：10007
     */
    public static BusinessException authFailed(String message) {
        return new BusinessException("10007", "安全认证失败", true, "10007", message);
    }

    /**
     * 重放攻击异常
     * 错误码：10008
     */
    public static BusinessException replayAttack(String message) {
        return new BusinessException("10008", "检测到重放攻击", false, "10008", message);
    }

    /**
     * 路由不可用异常
     * 错误码：10009
     */
    public static BusinessException routeUnavailable(String message) {
        return new BusinessException("10009", "清算路由不可用", true, "10009", message);
    }

    /**
     * 收款人信息不匹配异常
     * 错误码：10010
     */
    public static BusinessException payeeInfoMismatch(String message) {
        return new BusinessException("10010", "收款人信息不匹配", false, "10010", message);
    }

    /**
     * 手续费计算错误异常
     * 错误码：10011
     */
    public static BusinessException feeCalcError(String message) {
        return new BusinessException("10011", "手续费计算错误", false, "10011", message);
    }

    /**
     * 交易附言被拦截异常
     * 错误码：10012
     */
    public static BusinessException remitInfoBlocked(String message) {
        return new BusinessException("10012", "交易附言包含敏感信息", false, "10012", message);
    }

    /**
     * 系统超时异常
     * 错误码：20001
     */
    public static BusinessException systemTimeout(String message) {
        return new BusinessException("20001", "系统超时", true, "20001", message);
    }

    /**
     * 核心系统不可用异常
     * 错误码：20002
     */
    public static BusinessException coreSystemUnavailable(String message) {
        return new BusinessException("20002", "核心系统不可用", true, "20002", message);
    }

    /**
     * 未知错误异常
     * 错误码：99999
     */
    public static BusinessException unknownError(String message) {
        return new BusinessException("99999", "未知错误", false, "99999", message);
    }
}