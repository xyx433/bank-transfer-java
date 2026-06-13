package com.bank.core.banktransferservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简化版转账请求 DTO（测试专用）
 *
 * 【测试模式专用】
 * 只包含核心字段，简化测试流程：
 * - payerAcctNo: 付款账号
 * - payeeAcctNo: 收款账号
 * - amount: 转账金额
 *
 * 其他字段使用默认值：
 * - 渠道：MB（手机银行）
 * - 业务类型：1001（普通转账）
 * - 币种：CNY（人民币）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleTransferRequest {

    /**
     * 付款账号
     */
    @NotBlank(message = "付款账号不能为空")
    private String payerAcctNo;

    /**
     * 收款账号
     */
    @NotBlank(message = "收款账号不能为空")
    private String payeeAcctNo;

    /**
     * 转账金额（元）
     */
    @NotNull(message = "转账金额不能为空")
    @DecimalMin(value = "0.01", message = "转账金额必须大于0.01元")
    @Digits(integer = 16, fraction = 2, message = "转账金额格式不正确")
    private java.math.BigDecimal amount;

    /**
     * 交易附言（可选）
     */
    private String remitInfo;

    /**
     * 渠道代码（可选，默认为MB）
     */
    private String channelId;
}