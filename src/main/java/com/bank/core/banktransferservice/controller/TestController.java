package com.bank.core.banktransferservice.controller;

import com.bank.core.banktransferservice.config.AppProperties;
import com.bank.core.banktransferservice.dto.Result;
import com.bank.core.banktransferservice.dto.SimpleTransferRequest;
import com.bank.core.banktransferservice.dto.TransferResponse;
import com.bank.core.banktransferservice.entity.Account;
import com.bank.core.banktransferservice.entity.TransferRecord;
import com.bank.core.banktransferservice.mapper.AccountMapper;
import com.bank.core.banktransferservice.mapper.TransferRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 测试专用 Controller
 *
 * 【测试模式】
 * 当 app.test-mode.enabled = true 时，提供简化的测试接口：
 * - 只需要传入核心字段：付款账号、收款账号、金额
 * - 自动生成幂等Token和交易流水号
 * - 跳过签名校验和短信验证码校验
 * - 使用默认值填充其他字段
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final AppProperties appProperties;
    private final AccountMapper accountMapper;
    private final TransferRecordMapper transferRecordMapper;

    /**
     * 简化版转账接口（测试专用）
     *
     * 【接口说明】
     * 只需要传入3个核心字段：
     * - payerAcctNo: 付款账号
     * - payeeAcctNo: 收款账号
     * - amount: 转账金额
     *
     * 【测试模式专用】
     * 直接操作数据库，绕过所有校验：
     * - 跳过签名校验
     * - 跳过短信验证码校验
     * - 跳过限额校验
     * - 只校验余额是否充足
     *
     * @param request 简化版转账请求
     * @return 转账响应
     */
    @PostMapping("/transfer")
    @Transactional(rollbackFor = Exception.class)
    public Result<TransferResponse> testTransfer(@RequestBody SimpleTransferRequest request) {
        // 检查是否启用测试模式
        if (!appProperties.getTestMode().isEnabled()) {
            return Result.fail("TEST_MODE_DISABLED", "测试模式未启用，请在 application.yml 中设置 app.test-mode.enabled = true");
        }

        // 手动校验核心字段（测试模式下简化校验）
        if (request.getPayerAcctNo() == null || request.getPayerAcctNo().isEmpty()) {
            return Result.fail("PARAM_INVALID", "付款账号不能为空");
        }
        if (request.getPayeeAcctNo() == null || request.getPayeeAcctNo().isEmpty()) {
            return Result.fail("PARAM_INVALID", "收款账号不能为空");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("PARAM_INVALID", "转账金额必须大于0");
        }

        String payerAcctNo = request.getPayerAcctNo();
        String payeeAcctNo = request.getPayeeAcctNo();
        BigDecimal amount = request.getAmount();

        log.info("【测试模式】收到简化转账请求，付款账号：{}，收款账号：{}，金额：{}",
                payerAcctNo, payeeAcctNo, amount);

        try {
            // 查询付款账户（带悲观锁）
            Account payerAccount = accountMapper.selectByAcctNoForUpdate(payerAcctNo);
            if (payerAccount == null) {
                return Result.fail("10004", "付款账户不存在");
            }

            // 查询收款账户
            Account payeeAccount = accountMapper.selectByAcctNo(payeeAcctNo);
            if (payeeAccount == null) {
                return Result.fail("10005", "收款账户不存在");
            }

            // 校验付款账户余额
            BigDecimal availableBalance = payerAccount.getAvailableBalance();
            if (availableBalance.compareTo(amount) < 0) {
                return Result.fail("10001", "余额不足", TransferResponse.fail(
                        "TEST_TRAN", "10001", "付款账户余额不足",
                        "10001",
                        String.format("当前可用余额：%s，转账金额：%s", availableBalance, amount),
                        false
                ));
            }

            // 扣减付款账户余额
            int payerUpdateCount = accountMapper.deductBalance(payerAcctNo, amount);
            if (payerUpdateCount == 0) {
                return Result.fail("10001", "余额扣减失败", TransferResponse.fail(
                        "TEST_TRAN", "10001", "余额扣减失败",
                        "10001",
                        "账户余额不足或已被其他交易占用",
                        false
                ));
            }

            // 收款账户入账（已确认存在）
            accountMapper.addBalance(payeeAcctNo, amount);
            log.info("【测试模式】收款账户入账成功，账号：{}，金额：{}", payeeAcctNo, amount);

            // 生成交易流水号
            String tranSeqNo = generateTranSeqNo("MB");

            // 插入交易流水
            TransferRecord record = TransferRecord.builder()
                    .tranSeqNo(tranSeqNo)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .channelId("MB")
                    .payerAcctNo(payerAcctNo)
                    .payerAcctName(payerAccount.getAcctName())
                    .payerIdType("01")
                    .payerIdNo("TEST_ID")
                    .payerBankCode(payerAccount.getBankCode())
                    .payerBankName(payerAccount.getBankName())
                    .payeeAcctNo(payeeAcctNo)
                    .payeeAcctName(payeeAccount != null ? payeeAccount.getAcctName() : "未知")
                    .payeeBankCode("TEST_BANK")
                    .payeeBankName("测试银行")
                    .payeeBankUnionCode("TEST_UNION")
                    .amount(amount)
                    .currencyCode("CNY")
                    .bizType("1001")
                    .purposeCode("01")
                    .remitInfo(request.getRemitInfo() != null ? request.getRemitInfo() : "测试转账")
                    .routeMode("AUTO")
                    .feeBearer("01")
                    .feeAmount(BigDecimal.ZERO)
                    .tranStatus("SUCCESS")
                    .respCode("000000")
                    .respMsg("交易成功")
                    .tranTimestamp(LocalDateTime.now())
                    .completedTime(LocalDateTime.now())
                    .build();

            transferRecordMapper.insert(record);

            // 构建成功响应
            String acpTranSeqNo = "ACP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String coreSerialNo = "CORE" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            log.info("【测试模式】转账成功，流水号：{}", tranSeqNo);

            return Result.success(TransferResponse.builder()
                    .header(TransferResponse.ResponseHeader.builder()
                            .tranSeqNo(tranSeqNo)
                            .respTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                            .version("1.0.0")
                            .respCode("000000")
                            .respMsg("交易成功")
                            .build())
                    .body(TransferResponse.ResponseBody.builder()
                            .tranStatus("SUCCESS")
                            .acpTranSeqNo(acpTranSeqNo)
                            .coreSerialNo(coreSerialNo)
                            .payeeAcctMask(maskAccountNo(payeeAcctNo))
                            .actualAmount(amount)
                            .feeAmount(BigDecimal.ZERO)
                            .completedTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                            .build())
                    .build());

        } catch (Exception e) {
            log.error("【测试模式】转账异常：{}", e.getMessage(), e);
            return Result.fail("99999", "系统异常：" + e.getMessage());
        }
    }

    /**
     * 生成交易流水号
     * 格式：YYYYMMDD + PIT + 渠道码 + 8位序号
     */
    private String generateTranSeqNo(String channelId) {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sequence = String.format("%08d", System.currentTimeMillis() % 100000000);
        return dateStr + "PIT" + channelId + sequence;
    }

    /**
     * 隐藏账号信息（脱敏处理）
     * 显示前6位和后4位，中间用*替换
     */
    private String maskAccountNo(String acctNo) {
        if (acctNo == null || acctNo.length() < 10) {
            return acctNo;
        }
        return acctNo.substring(0, 6) + "******" + acctNo.substring(acctNo.length() - 4);
    }

    /**
     * 获取测试账户信息
     */
    @GetMapping("/accounts")
    public Result<Object> getTestAccounts() {
        return Result.success("测试账户信息", new Object[]{
                new AccountInfo("6222021234567890", "张三", new BigDecimal("100000.00"), "普通客户"),
                new AccountInfo("6222021234567891", "李四", new BigDecimal("500000.00"), "VIP客户"),
                new AccountInfo("6222021234567892", "王五", new BigDecimal("1000000.00"), "私银客户"),
                new AccountInfo("6222021234567893", "赵六", new BigDecimal("100.00"), "余额不足测试"),
                new AccountInfo("6228480012345678", "李四收款", new BigDecimal("50000.00"), "跨行收款账户")
        });
    }

    /**
     * 账户信息内部类
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class AccountInfo {
        private String acctNo;
        private String acctName;
        private BigDecimal balance;
        private String description;
    }
}