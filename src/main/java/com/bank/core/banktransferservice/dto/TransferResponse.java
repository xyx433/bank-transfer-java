package com.bank.core.banktransferservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 转账响应 DTO
 *
 * 【响应设计】
 * 1. 统一的响应结构，包含 header 和 body
 * 2. 成功和失败响应使用相同结构，便于客户端处理
 * 3. 包含完整的交易追踪信息，支持审计和对账
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {

    /**
     * 响应头信息
     */
    private ResponseHeader header;

    /**
     * 响应体信息
     */
    private ResponseBody body;

    /**
     * 响应头信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseHeader {

        /**
         * 交易流水号
         * 与请求中的 tranSeqNo 一致
         */
        private String tranSeqNo;

        /**
         * 响应时间
         * ISO 8601 格式
         */
        private String respTime;

        /**
         * 接口版本号
         */
        private String version;

        /**
         * 响应码
         * 000000=成功，其他=错误码
         *
         * 【错误码规范】
         * - 000000: 成功
         * - 10001: BALANCE_INSUFFICIENT - 余额不足
         * - 10002: DAILY_LIMIT_EXCEEDED - 单日限额超限
         * - 10003: SINGLE_LIMIT_EXCEEDED - 单笔限额超限
         * - 10004: ACCOUNT_STATUS_ABNORMAL - 账户状态异常
         * - 10005: AML_SUSPICIOUS - 反洗钱可疑交易
         * - 10006: PAYEE_BLOCKED - 收款人黑名单
         * - 10007: AUTH_FAILED - 认证失败
         * - 10008: REPLAY_ATTACK - 重放攻击
         * - 10009: ROUTE_UNAVAILABLE - 路由不可用
         * - 10010: PAYEE_INFO_MISMATCH - 收款人信息不匹配
         * - 10011: FEE_CALC_ERROR - 手续费计算错误
         * - 10012: REMIT_INFO_BLOCKED - 交易附言被拦截
         * - 20001: SYSTEM_TIMEOUT - 系统超时
         * - 20002: CORE_SYSTEM_UNAVAILABLE - 核心系统不可用
         * - 99999: UNKNOWN_ERROR - 未知错误
         */
        private String respCode;

        /**
         * 响应消息
         */
        private String respMsg;
    }

    /**
     * 响应体信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseBody {

        /**
         * 交易状态
         * SUCCESS / FAILED / PROCESSING / TIMEOUT
         */
        private String tranStatus;

        /**
         * 受理平台流水号
         */
        private String acpTranSeqNo;

        /**
         * 核心系统流水号
         */
        private String coreSerialNo;

        /**
         * 人行清算流水号
         */
        private String hostSeqNo;

        /**
         * 人行返回状态
         * 01=已受理，02=已清算，03=已轧差，04=已到账，99=已撤销
         */
        private String hostStatus;

        /**
         * 收款账号脱敏
         * 格式：前6位 + **** + 后4位
         * 示例：622848******5678
         */
        private String payeeAcctMask;

        /**
         * 实际转账金额（元）
         */
        private java.math.BigDecimal actualAmount;

        /**
         * 手续费金额（元）
         */
        private java.math.BigDecimal feeAmount;

        /**
         * 交易完成时间
         */
        private String completedTime;

        /**
         * 失败原因码
         * 仅失败响应包含
         */
        private String failReasonCode;

        /**
         * 失败原因描述
         * 仅失败响应包含
         */
        private String failReasonDesc;

        /**
         * 是否允许重试
         * 仅失败响应包含
         */
        private Boolean retryFlag;
    }

    /**
     * 创建成功响应
     */
    public static TransferResponse success(String tranSeqNo, String acpTranSeqNo,
                                           String coreSerialNo, java.math.BigDecimal actualAmount,
                                           java.math.BigDecimal feeAmount) {
        return TransferResponse.builder()
                .header(ResponseHeader.builder()
                        .tranSeqNo(tranSeqNo)
                        .respTime(java.time.LocalDateTime.now().toString())
                        .version("1.0.0")
                        .respCode("000000")
                        .respMsg("交易成功")
                        .build())
                .body(ResponseBody.builder()
                        .tranStatus("SUCCESS")
                        .acpTranSeqNo(acpTranSeqNo)
                        .coreSerialNo(coreSerialNo)
                        .actualAmount(actualAmount)
                        .feeAmount(feeAmount)
                        .completedTime(java.time.LocalDateTime.now().toString())
                        .build())
                .build();
    }

    /**
     * 创建失败响应
     */
    public static TransferResponse fail(String tranSeqNo, String respCode, String respMsg,
                                        String failReasonCode, String failReasonDesc,
                                        boolean retryFlag) {
        return TransferResponse.builder()
                .header(ResponseHeader.builder()
                        .tranSeqNo(tranSeqNo)
                        .respTime(java.time.LocalDateTime.now().toString())
                        .version("1.0.0")
                        .respCode(respCode)
                        .respMsg(respMsg)
                        .build())
                .body(ResponseBody.builder()
                        .tranStatus("FAILED")
                        .failReasonCode(failReasonCode)
                        .failReasonDesc(failReasonDesc)
                        .retryFlag(retryFlag)
                        .build())
                .build();
    }
}