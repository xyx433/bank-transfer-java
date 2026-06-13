package com.bank.core.banktransferservice.mapper;

import com.bank.core.banktransferservice.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * 账户 Mapper 接口
 *
 * 【金融安全设计要点】
 * 1. 余额扣减使用悲观锁（SELECT FOR UPDATE）或乐观锁（版本号）
 * 2. 扣减时必须校验余额充足，防止超卖
 * 3. 使用预编译语句，防止 SQL 注入
 */
@Mapper
public interface AccountMapper {

    /**
     * 根据账号查询账户信息
     *
     * @param acctNo 账号
     * @return 账户信息
     */
    Account selectByAcctNo(@Param("acctNo") String acctNo);

    /**
     * 根据账号查询账户信息（带悲观锁）
     *
     * 【并发控制】
     * 使用 SELECT FOR UPDATE 获取行级锁：
     * - 在事务中执行，锁定账户行
     * - 其他事务必须等待锁释放
     * - 防止并发扣款导致的余额不一致
     *
     * 这是防止"余额临界点并发击穿"的关键措施
     *
     * @param acctNo 账号
     * @return 账户信息
     */
    Account selectByAcctNoForUpdate(@Param("acctNo") String acctNo);

    /**
     * 扣减账户余额（乐观锁方式）
     *
     * 【余额扣减核心逻辑】
     * 使用乐观锁防止并发扣款超卖：
     * - WHERE 条件包含版本号校验
     * - 同时校验余额充足（balance >= amount）
     * - 更新成功返回 1，失败返回 0
     *
     * SQL 示例：
     * UPDATE account
     * SET balance = balance - #{amount},
     *     version = version + 1,
     *     update_time = NOW()
     * WHERE acct_no = #{acctNo}
     *   AND balance >= #{amount}
     *   AND version = #{version}
     *
     * 如果影响行数为 0，说明：
     * 1. 余额不足
     * 2. 数据已被其他事务修改（版本号不匹配）
     *
     * @param acctNo 账号
     * @param amount 扣减金额
     * @param version 当前版本号
     * @return 影响行数（1=成功，0=失败）
     */
    int deductBalanceWithOptimisticLock(
            @Param("acctNo") String acctNo,
            @Param("amount") BigDecimal amount,
            @Param("version") Integer version
    );

    /**
     * 扣减账户余额（悲观锁方式，配合 SELECT FOR UPDATE 使用）
     *
     * 【悲观锁扣减】
     * 在事务中配合 selectByAcctNoForUpdate 使用：
     * 1. 先 SELECT FOR UPDATE 锁定账户行
     * 2. 校验余额充足
     * 3. 执行扣减
     * 4. 事务提交释放锁
     *
     * SQL 示例：
     * UPDATE account
     * SET balance = balance - #{amount},
     *     update_time = NOW()
     * WHERE acct_no = #{acctNo}
     *   AND balance >= #{amount}
     *
     * @param acctNo 账号
     * @param amount 扣减金额
     * @return 影响行数（1=成功，0=失败）
     */
    int deductBalance(
            @Param("acctNo") String acctNo,
            @Param("amount") BigDecimal amount
    );

    /**
     * 增加账户余额
     *
     * 【收款入账】
     * 收款账户余额增加：
     * - 无需版本号校验（入金操作）
     * - 需校验账户状态（是否允许入金）
     *
     * @param acctNo 账号
     * @param amount 增加金额
     * @return 影响行数
     */
    int addBalance(
            @Param("acctNo") String acctNo,
            @Param("amount") BigDecimal amount
    );

    /**
     * 更新账户状态
     *
     * @param acctNo 账号
     * @param status 新状态
     * @return 影响行数
     */
    int updateStatus(
            @Param("acctNo") String acctNo,
            @Param("status") String status
    );

    /**
     * 增加冻结金额
     *
     * @param acctNo 账号
     * @param amount 冻结金额
     * @return 影响行数
     */
    int addFrozenAmount(
            @Param("acctNo") String acctNo,
            @Param("amount") BigDecimal amount
    );

    /**
     * 减少冻结金额
     *
     * @param acctNo 账号
     * @param amount 解冻金额
     * @return 影响行数
     */
    int reduceFrozenAmount(
            @Param("acctNo") String acctNo,
            @Param("amount") BigDecimal amount
    );

    /**
     * 增加在途资金
     *
     * 【在途资金管理】
     * 转账发起时增加在途资金：
     * - 防止超额转账
     * - 转账完成后减少在途资金
     *
     * @param acctNo 账号
     * @param amount 在途金额
     * @return 影响行数
     */
    int addInTransitAmount(
            @Param("acctNo") String acctNo,
            @Param("amount") BigDecimal amount
    );

    /**
     * 减少在途资金
     *
     * @param acctNo 账号
     * @param amount 减少金额
     * @return 影响行数
     */
    int reduceInTransitAmount(
            @Param("acctNo") String acctNo,
            @Param("amount") BigDecimal amount
    );
}