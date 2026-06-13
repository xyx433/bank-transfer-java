package com.bank.core.banktransferservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 转账请求 DTO
 *
 * 【JSR-303 校验设计】
 * 使用注解进行参数校验，确保：
 * 1. 必填字段不能为空
 * 2. 字段长度符合规范
 * 3. 金额字段必须大于0
 * 4. 格式校验（如UUID格式）
 *
 * 校验失败会抛出 MethodArgumentNotValidException，
 * 由全局异常处理器统一处理返回错误信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    /**
     * 请求头信息
     */
    @Valid
    @NotNull(message = "请求头不能为空")
    private RequestHeader header;

    /**
     * 请求体信息
     */
    @Valid
    @NotNull(message = "请求体不能为空")
    private RequestBody body;

    /**
     * 数字签名
     */
    @NotBlank(message = "数字签名不能为空")
    @Size(max = 512, message = "数字签名长度不能超过512字符")
    private String signature;

    /**
     * 请求头信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestHeader {

        /**
         * 交易流水号
         * 格式：YYYYMMDD + PIT + 2位渠道码 + 8位序号
         */
        @NotBlank(message = "交易流水号不能为空")
        @Pattern(regexp = "^\\d{8}PIT[A-Z]{2}\\d{8}$", message = "交易流水号格式不正确")
        private String tranSeqNo;

        /**
         * 渠道代码
         * MB=手机银行，EB=网银，WC=微信小程序
         */
        @NotBlank(message = "渠道代码不能为空")
        @Pattern(regexp = "^(MB|EB|WC)$", message = "渠道代码不合法")
        private String channelId;

        /**
         * 交易发起时间
         * ISO 8601 格式
         */
        @NotBlank(message = "交易时间不能为空")
        private String tranTimestamp;

        /**
         * 接口版本号
         */
        @NotBlank(message = "版本号不能为空")
        @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$", message = "版本号格式不正确")
        private String version;

        /**
         * 幂等Token（防重复提交）
         *
         * 【幂等性设计】
         * - UUID v4 格式，全局唯一
         * - 同一个 Token 只能成功处理一次
         * - 有效期24小时
         * - 重复请求返回首次结果
         */
        @NotBlank(message = "幂等Token不能为空")
        @Pattern(regexp = "^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$",
                message = "幂等Token格式不正确，应为UUID v4格式")
        private String idempotencyToken;
    }

    /**
     * 请求体信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestBody {

        /**
         * 付款人信息
         */
        @Valid
        @NotNull(message = "付款人信息不能为空")
        private PayerInfo payer;

        /**
         * 收款人信息
         */
        @Valid
        @NotNull(message = "收款人信息不能为空")
        private PayeeInfo payee;

        /**
         * 交易信息
         */
        @Valid
        @NotNull(message = "交易信息不能为空")
        private TransactionInfo transaction;

        /**
         * 安全认证信息
         */
        @Valid
        @NotNull(message = "安全认证信息不能为空")
        private SecurityInfo security;

        /**
         * 扩展信息
         */
        @Valid
        private ExtendInfo extendInfo;
    }

    /**
     * 付款人信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayerInfo {

        /**
         * 付款账号
         */
        @NotBlank(message = "付款账号不能为空")
        @Size(max = 19, message = "付款账号长度不能超过19位")
        private String acctNo;

        /**
         * 付款人姓名
         */
        @NotBlank(message = "付款人姓名不能为空")
        @Size(max = 120, message = "付款人姓名长度不能超过120字符")
        private String acctName;

        /**
         * 证件类型
         * 01=身份证，02=护照，03=军官证
         */
        @NotBlank(message = "证件类型不能为空")
        @Pattern(regexp = "^0[1-3]$", message = "证件类型不合法")
        private String idType;

        /**
         * 证件号码
         */
        @NotBlank(message = "证件号码不能为空")
        @Size(max = 18, message = "证件号码长度不能超过18位")
        private String idNo;

        /**
         * 付款行银行代码
         */
        @NotBlank(message = "付款行银行代码不能为空")
        @Size(max = 12, message = "付款行银行代码长度不能超过12位")
        private String payerBankCode;

        /**
         * 付款行名称
         */
        @NotBlank(message = "付款行名称不能为空")
        @Size(max = 100, message = "付款行名称长度不能超过100字符")
        private String payerBankName;
    }

    /**
     * 收款人信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayeeInfo {

        /**
         * 收款账号
         */
        @NotBlank(message = "收款账号不能为空")
        @Size(max = 19, message = "收款账号长度不能超过19位")
        private String acctNo;

        /**
         * 收款人姓名
         */
        @NotBlank(message = "收款人姓名不能为空")
        @Size(max = 120, message = "收款人姓名长度不能超过120字符")
        private String acctName;

        /**
         * 收款行银行代码
         */
        @NotBlank(message = "收款行银行代码不能为空")
        @Size(max = 12, message = "收款行银行代码长度不能超过12位")
        private String payeeBankCode;

        /**
         * 收款行名称
         */
        @NotBlank(message = "收款行名称不能为空")
        @Size(max = 100, message = "收款行名称长度不能超过100字符")
        private String payeeBankName;

        /**
         * 收款行支付系统行号
         */
        @NotBlank(message = "收款行支付系统行号不能为空")
        @Size(max = 12, message = "收款行支付系统行号长度不能超过12位")
        private String payeeBankUnionCode;
    }

    /**
     * 交易信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionInfo {

        /**
         * 转账金额（元）
         *
         * 【金额校验】
         * - 必须大于0
         * - 最小金额0.01元
         * - 使用 BigDecimal 保证精度
         */
        @NotNull(message = "转账金额不能为空")
        @DecimalMin(value = "0.01", message = "转账金额必须大于0.01元")
        @Digits(integer = 16, fraction = 2, message = "转账金额格式不正确，整数部分最多16位，小数部分最多2位")
        private java.math.BigDecimal amount;

        /**
         * 币种代码
         */
        @NotBlank(message = "币种代码不能为空")
        @Pattern(regexp = "^[A-Z]{3}$", message = "币种代码格式不正确")
        private String currencyCode;

        /**
         * 业务类型
         * 1001=普通转账，1002=实时转账，1003=次日到账
         */
        @NotBlank(message = "业务类型不能为空")
        @Pattern(regexp = "^100[1-3]$", message = "业务类型不合法")
        private String bizType;

        /**
         * 附言类型
         * 01=货款，02=工资，03=借款还款，04=其他
         */
        @NotBlank(message = "附言类型不能为空")
        @Pattern(regexp = "^0[1-4]$", message = "附言类型不合法")
        private String purposeCode;

        /**
         * 交易附言
         *
         * 【安全校验】
         * - 长度限制：最多100字符
         * - 敏感词过滤：禁止虚拟货币、博彩等关键词
         * - XSS防护：禁止HTML标签、JS脚本
         */
        @Size(max = 100, message = "交易附言长度不能超过100字符")
        private String remitInfo;

        /**
         * 路由模式
         * AUTO=系统智能选路，HVPS=大额，BEPS=小额，IBPS=超网
         */
        @NotBlank(message = "路由模式不能为空")
        @Pattern(regexp = "^(AUTO|HVPS|BEPS|IBPS)$", message = "路由模式不合法")
        private String routeMode;

        /**
         * 手续费承担方
         * 01=付款方，02=收款方
         */
        @NotBlank(message = "手续费承担方不能为空")
        @Pattern(regexp = "^0[1-2]$", message = "手续费承担方不合法")
        private String feeBearer;
    }

    /**
     * 安全认证信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityInfo {

        /**
         * 短信验证码
         */
        @NotBlank(message = "短信验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "短信验证码必须为6位数字")
        private String smsCode;

        /**
         * 交易密码密文（国密SM4加密后Base64）
         */
        @NotBlank(message = "交易密码不能为空")
        @Size(max = 256, message = "交易密码密文长度不能超过256字符")
        private String tradePwdEnc;

        /**
         * 设备指纹
         */
        @NotBlank(message = "设备指纹不能为空")
        @Size(max = 64, message = "设备指纹长度不能超过64字符")
        private String deviceFingerprint;

        /**
         * 人脸识别Token
         * 金额≥5万元必传
         */
        @Size(max = 128, message = "人脸识别Token长度不能超过128字符")
        private String faceToken;

        /**
         * 防重放随机数
         *
         * 【防重放设计】
         * - 格式：{timestamp}_{randomString}
         * - 时间戳与服务器时间偏差不得超过±300秒
         * - 同一个 nonce 全局唯一，已用 nonce 不可重复
         */
        @NotBlank(message = "防重放随机数不能为空")
        @Size(max = 64, message = "防重放随机数长度不能超过64字符")
        private String nonce;
    }

    /**
     * 扩展信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtendInfo {

        /**
         * 客户端IP
         */
        @Size(max = 45, message = "客户端IP长度不能超过45字符")
        private String clientIp;

        /**
         * MAC地址
         */
        @Size(max = 17, message = "MAC地址长度不能超过17字符")
        private String macAddress;

        /**
         * GPS坐标
         */
        @Valid
        private GpsCoordinate gpsCoordinate;

        /**
         * 终端ID
         */
        @Size(max = 20, message = "终端ID长度不能超过20字符")
        private String terminalId;

        /**
         * 终端类型
         */
        @Size(max = 50, message = "终端类型长度不能超过50字符")
        private String terminalType;
    }

    /**
     * GPS坐标
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GpsCoordinate {

        /**
         * 经度
         * 范围：-180 到 180
         */
        @DecimalMin(value = "-180", message = "经度范围不正确")
        @DecimalMax(value = "180", message = "经度范围不正确")
        private java.math.BigDecimal longitude;

        /**
         * 纬度
         * 范围：-90 到 90
         */
        @DecimalMin(value = "-90", message = "纬度范围不正确")
        @DecimalMax(value = "90", message = "纬度范围不正确")
        private java.math.BigDecimal latitude;
    }
}