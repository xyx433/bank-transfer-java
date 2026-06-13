package com.bank.core.banktransferservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户实体类
 *
 * 【金融安全设计要点】
 * 1. 金额字段使用 BigDecimal 类型，避免浮点数精度丢失问题
 *    - Double/Float 在金融计算中存在精度问题，如 0.1 + 0.2 != 0.3
 *    - BigDecimal 提供精确的十进制计算，适合金融场景
 *
 * 2. 账户状态使用枚举值，确保状态流转的可控性
 *    - NORMAL: 正常状态，允许所有操作
 *    - FROZEN: 司法冻结，禁止所有资金操作
 *    - PARTIAL_FROZEN: 部分冻结，冻结金额内禁止操作
 *    - SLEEP: 休眠户，需激活后才能操作
 *    - CLOSED: 已销户，禁止所有操作
 *    - LOST: 挂失状态，禁止资金转出
 *    - RESTRICTED: 交易限制，反洗钱风控状态
 *
 * 3. 乐观锁版本号字段，防止并发更新导致的数据不一致
 *    - 更新时检查版本号，确保数据未被其他事务修改
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    /**
     * 账户主键ID
     */
    private Long id;

    /**
     * 账号（银行卡号）
     * 长度19位，符合银联卡号规范
     */
    private String acctNo;

    /**
     * 账户名称（户名）
     * 与证件姓名一致
     */
    private String acctName;

    /**
     * 证件类型
     * 01=身份证，02=护照，03=军官证
     */
    private String idType;

    /**
     * 证件号码
     * 用于身份校验和反洗钱
     */
    private String idNo;

    /**
     * 银行代码（支付系统行号）
     * 12位数字，人民银行分配
     */
    private String bankCode;

    /**
     * 银行名称
     */
    private String bankName;

    /**
     * 币种代码
     * CNY=人民币，USD=美元等
     */
    private String currencyCode;

    /**
     * 账户总余额（元）
     *
     * 【重要】使用 BigDecimal 而非 Double
     * 金融系统中，金额精度至关重要：
     * - Double 类型存在精度丢失：0.1 + 0.2 = 0.30000000000000004
     * - BigDecimal 保证精确计算：new BigDecimal("0.1").add(new BigDecimal("0.2")) = 0.3
     * - 数据库对应 DECIMAL(18,2)，整数部分16位，小数部分2位
     * - 最大支持金额：999,999,999,999,999.99 元
     */
    private BigDecimal balance;

    /**
     * 冻结金额（元）
     *
     * 冻结金额不可用于转账，包括：
     * - 司法冻结
     * - 预授权冻结
     * - 保证金冻结
     *
     * 可用余额 = balance - frozenAmount - inTransitAmount
     */
    private BigDecimal frozenAmount;

    /**
     * 在途资金（元）
     *
     * 已发起但尚未完成的转账金额
     * 用于防止超额转账
     *
     * 可用余额计算时需扣除在途资金
     */
    private BigDecimal inTransitAmount;

    /**
     * 账户状态
     *
     * 状态流转规则：
     * NORMAL -> FROZEN: 司法冻结
     * NORMAL -> PARTIAL_FROZEN: 部分冻结
     * NORMAL -> SLEEP: 长期无动账（3年以上）
     * NORMAL -> LOST: 挂失
     * NORMAL -> RESTRICTED: 反洗钱风控
     * NORMAL -> CLOSED: 销户
     * SLEEP -> NORMAL: 激活
     * LOST -> NORMAL: 解挂
     * RESTRICTED -> NORMAL: 风控解除
     */
    private String accountStatus;

    /**
     * 客户星级（1-7）
     *
     * 星级权益：
     * - 1-4星：普通客户，标准手续费
     * - 5星：VIP客户，手续费减免50%
     * - 6星：高端客户，手续费减免70%
     * - 7星：私银客户，手续费全免
     */
    private Integer starLevel;

    /**
     * 单日转账限额（元）
     * 可根据客户等级和风险等级动态调整
     */
    private BigDecimal dailyLimit;

    /**
     * 单笔转账限额（元）
     */
    private BigDecimal singleLimit;

    /**
     * 预留手机号
     * 用于短信验证码校验
     */
    private String mobilePhone;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 乐观锁版本号
     *
     * 【并发控制】
     * 用于防止并发更新导致的数据不一致：
     * - 更新时 WHERE version = oldVersion
     * - 更新成功后 version = version + 1
     * - 如果影响行数为0，说明数据已被其他事务修改
     */
    private Integer version;

    /**
     * 计算可用余额
     *
     * 【金融核心公式】
     * 可用余额 = 总余额 - 冻结金额 - 在途资金
     *
     * 这是转账前余额校验的关键指标
     *
     * @return 可用余额
     */
    public BigDecimal getAvailableBalance() {
        if (balance == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal frozen = frozenAmount != null ? frozenAmount : BigDecimal.ZERO;
        BigDecimal inTransit = inTransitAmount != null ? inTransitAmount : BigDecimal.ZERO;
        return balance.subtract(frozen).subtract(inTransit);
    }

    /**
     * 判断账户是否可转账
     *
     * @return true=可转账，false=不可转账
     */
    public boolean canTransfer() {
        return "NORMAL".equals(accountStatus);
    }

    /**
     * 判断账户是否可收款
     *
     * 收款账户状态校验规则：
     * - NORMAL: 正常收款
     * - SLEEP: 休眠户可收款（入金后自动激活）
     * - FROZEN/CLOSED/LOST: 禁止收款
     *
     * @return true=可收款，false=不可收款
     */
    public boolean canReceive() {
        return "NORMAL".equals(accountStatus) || "SLEEP".equals(accountStatus);
    }
}