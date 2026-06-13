package com.bank.core.banktransferservice.mapper;

import com.bank.core.banktransferservice.entity.TransferRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 转账交易流水 Mapper 接口
 *
 * 【金融安全设计要点】
 * 1. 幂等性查询：根据 idempotencyKey 查询是否存在
 * 2. 插入流水时使用数据库唯一索引约束
 * 3. 状态更新使用乐观锁，防止并发修改
 */
@Mapper
public interface TransferRecordMapper {

    /**
     * 根据幂等号查询交易记录
     *
     * 【幂等性核心】
     * 在插入流水前，先根据 idempotencyKey 查询是否已存在：
     * - 存在且已成功：返回原交易结果，不重新扣款
     * - 存在但处理中：返回处理中状态
     * - 存在但失败：允许重新发起（使用新的幂等号）
     * - 不存在：继续处理
     *
     * 这是防止重复扣款的第一道防线
     *
     * @param idempotencyKey 幂等号
     * @return 交易记录
     */
    TransferRecord selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    /**
     * 根据交易流水号查询交易记录
     *
     * @param tranSeqNo 交易流水号
     * @return 交易记录
     */
    TransferRecord selectByTranSeqNo(@Param("tranSeqNo") String tranSeqNo);

    /**
     * 根据主键查询交易记录
     *
     * @param id 主键ID
     * @return 交易记录
     */
    TransferRecord selectById(@Param("id") Long id);

    /**
     * 插入交易流水
     *
     * 【插入流水核心逻辑】
     * - idempotencyKey 字段有唯一索引约束
     * - 如果重复插入相同幂等号，会抛出 DuplicateKeyException
     * - 这是防止重复扣款的第二道防线（数据库层面）
     *
     * 插入时机：
     * - 在扣款前插入流水（状态为 INIT 或 PROCESSING）
     * - 扣款成功后更新状态为 SUCCESS
     * - 扣款失败后更新状态为 FAILED
     *
     * @param record 交易记录
     * @return 影响行数
     */
    int insert(TransferRecord record);

    /**
     * 更新交易状态
     *
     * 【状态更新】
     * 状态流转必须保证原子性：
     * - 使用乐观锁防止并发修改
     * - WHERE 条件包含当前状态校验
     *
     * @param id 主键ID
     * @param tranStatus 新状态
     * @param respCode 响应码
     * @param respMsg 响应消息
     * @param completedTime 完成时间
     * @return 影响行数
     */
    int updateStatus(
            @Param("id") Long id,
            @Param("tranStatus") String tranStatus,
            @Param("respCode") String respCode,
            @Param("respMsg") String respMsg,
            @Param("completedTime") LocalDateTime completedTime
    );

    /**
     * 更新交易状态（带失败原因）
     *
     * @param id 主键ID
     * @param tranStatus 新状态
     * @param respCode 响应码
     * @param respMsg 响应消息
     * @param failReasonCode 失败原因码
     * @param failReasonDesc 失败原因描述
     * @param completedTime 完成时间
     * @return 影响行数
     */
    int updateStatusWithFailReason(
            @Param("id") Long id,
            @Param("tranStatus") String tranStatus,
            @Param("respCode") String respCode,
            @Param("respMsg") String respMsg,
            @Param("failReasonCode") String failReasonCode,
            @Param("failReasonDesc") String failReasonDesc,
            @Param("completedTime") LocalDateTime completedTime
    );

    /**
     * 更新清算信息
     *
     * @param id 主键ID
     * @param acpTranSeqNo 受理平台流水号
     * @param coreSerialNo 核心系统流水号
     * @param hostSeqNo 人行清算流水号
     * @param hostStatus 人行返回状态
     * @return 影响行数
     */
    int updateHostInfo(
            @Param("id") Long id,
            @Param("acpTranSeqNo") String acpTranSeqNo,
            @Param("coreSerialNo") String coreSerialNo,
            @Param("hostSeqNo") String hostSeqNo,
            @Param("hostStatus") String hostStatus
    );

    /**
     * 查询账户当日累计转账金额
     *
     * 【日切限额统计】
     * 用于校验单日累计限额：
     * - 统计当日已成功的转账金额
     * - 排除已冲正/已撤销的交易
     * - 按渠道分别统计
     *
     * @param payerAcctNo 付款账号
     * @param channelId 渠道代码
     * @param statDate 统计日期
     * @return 累计金额
     */
    BigDecimal sumDailyAmountByAcctNo(
            @Param("payerAcctNo") String payerAcctNo,
            @Param("channelId") String channelId,
            @Param("statDate") LocalDate statDate
    );

    /**
     * 查询账户当日转账笔数
     *
     * @param payerAcctNo 付款账号
     * @param channelId 渠道代码
     * @param statDate 统计日期
     * @return 转账笔数
     */
    int countDailyByAcctNo(
            @Param("payerAcctNo") String payerAcctNo,
            @Param("channelId") String channelId,
            @Param("statDate") LocalDate statDate
    );

    /**
     * 查询账户交易记录列表
     *
     * @param acctNo 账号
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 交易记录列表
     */
    List<TransferRecord> selectByAcctNoAndTimeRange(
            @Param("acctNo") String acctNo,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 查询处理中的交易记录
     *
     * 【超时交易处理】
     * 用于查询超时未完成的交易：
     * - 状态为 PROCESSING
     * - 超过一定时间未完成
     * - 需要主动查询人行状态或冲正
     *
     * @param timeoutMinutes 超时分钟数
     * @return 超时交易列表
     */
    List<TransferRecord> selectProcessingRecords(
            @Param("timeoutMinutes") int timeoutMinutes
    );
}