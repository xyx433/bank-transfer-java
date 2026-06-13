package com.bank.core.banktransferservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 转账交易流水实体类
 *
 * 【金融安全设计要点】
 * 1. 幂等性保证：idempotencyKey 字段建立唯一索引，防止重复提交
 *    - 同一个幂等号只能成功处理一次
 *    - 重复请求返回首次处理结果
 *    - 有效期24小时
 *
 * 2. 交易状态流转：
 *    INIT -> PROCESSING -> SUCCESS/FAILED/TIMEOUT
 *    - INIT: 初始化，交易刚创建
 *    - PROCESSING: 处理中，正在执行转账
 *    - SUCCESS: 成功，转账完成
 *    - FAILED: 失败，转账失败
 *    - TIMEOUT: 超时，等待人行响应超时
 *
 * 3. 完整记录交易双方信息，支持审计和对账
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRecord {

    /**
     * 流水主键ID
     */
    private Long id;

    /**
     * 交易流水号
     * 格式：YYYYMMDD + PIT + 2位渠道码 + 8位序号
     * 示例：20260611PITMB00000001
     *
     * 全局唯一，用于交易追踪和对账
     */
    private String tranSeqNo;

    /**
     * 幂等号（防重复提交）
     *
     * 【幂等性设计核心】
     * - UUID v4 格式，全局唯一
     * - 数据库唯一索引约束
     * - 插入前先查询是否存在
     * - 存在则返回原交易结果
     * - 有效期24小时
     *
     * 这是防止重复扣款的第一道防线
     */
    private String idempotencyKey;

    /**
     * 渠道代码
     * MB=手机银行，EB=网银，WC=微信小程序
     */
    private String channelId;

    // ==================== 付款人信息 ====================

    /**
     * 付款账号
     */
    private String payerAcctNo;

    /**
     * 付款人姓名
     */
    private String payerAcctName;

    /**
     * 付款人证件类型
     */
    private String payerIdType;

    /**
     * 付款人证件号码
     */
    private String payerIdNo;

    /**
     * 付款行银行代码
     */
    private String payerBankCode;

    /**
     * 付款行名称
     */
    private String payerBankName;

    // ==================== 收款人信息 ====================

    /**
     * 收款账号
     */
    private String payeeAcctNo;

    /**
     * 收款人姓名
     */
    private String payeeAcctName;

    /**
     * 收款行银行代码
     */
    private String payeeBankCode;

    /**
     * 收款行名称
     */
    private String payeeBankName;

    /**
     * 收款行支付系统行号（大额/小额）
     */
    private String payeeBankUnionCode;

    // ==================== 交易信息 ====================

    /**
     * 转账金额（元）
     *
     * 【金额精度】
     * 使用 BigDecimal，数据库 DECIMAL(18,2)
     * 最小金额：0.01元
     */
    private BigDecimal amount;

    /**
     * 币种代码
     */
    private String currencyCode;

    /**
     * 业务类型
     * 1001=普通转账，1002=实时转账，1003=次日到账
     */
    private String bizType;

    /**
     * 附言类型
     * 01=货款，02=工资，03=借款还款，04=其他
     */
    private String purposeCode;

    /**
     * 交易附言
     * 最多100个字符，需进行敏感词过滤
     */
    private String remitInfo;

    /**
     * 路由模式
     * AUTO=系统智能选路，HVPS=大额，BEPS=小额，IBPS=超网
     */
    private String routeMode;

    /**
     * 手续费承担方
     * 01=付款方，02=收款方
     */
    private String feeBearer;

    /**
     * 手续费金额（元）
     */
    private BigDecimal feeAmount;

    // ==================== 清算信息 ====================

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

    // ==================== 安全信息 ====================

    /**
     * 短信验证码（脱敏存储）
     */
    private String smsCode;

    /**
     * 设备指纹
     */
    private String deviceFingerprint;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * GPS经度
     */
    private BigDecimal gpsLongitude;

    /**
     * GPS纬度
     */
    private BigDecimal gpsLatitude;

    // ==================== 交易状态 ====================

    /**
     * 交易状态
     *
     * 【状态流转】
     * INIT -> PROCESSING -> SUCCESS
     * INIT -> PROCESSING -> FAILED
     * INIT -> PROCESSING -> TIMEOUT
     *
     * 状态变更必须通过事务保证原子性
     */
    private String tranStatus;

    /**
     * 响应码
     * 000000=成功，其他=错误码
     */
    private String respCode;

    /**
     * 响应消息
     */
    private String respMsg;

    /**
     * 失败原因码
     */
    private String failReasonCode;

    /**
     * 失败原因描述
     */
    private String failReasonDesc;

    // ==================== 时间戳 ====================

    /**
     * 交易发起时间（毫秒精度）
     * 用于幂等性校验和时间窗口计算
     */
    private LocalDateTime tranTimestamp;

    /**
     * 交易完成时间（毫秒精度）
     */
    private LocalDateTime completedTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 判断交易是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(tranStatus);
    }

    /**
     * 判断交易是否处理中
     */
    public boolean isProcessing() {
        return "PROCESSING".equals(tranStatus);
    }

    /**
     * 判断交易是否失败
     */
    public boolean isFailed() {
        return "FAILED".equals(tranStatus);
    }

    /**
     * 判断交易是否超时
     */
    public boolean isTimeout() {
        return "TIMEOUT".equals(tranStatus);
    }

    /**
     * 判断交易是否终态
     * 终态包括：SUCCESS、FAILED、TIMEOUT
     */
    public boolean isFinalStatus() {
        return isSuccess() || isFailed() || isTimeout();
    }
}