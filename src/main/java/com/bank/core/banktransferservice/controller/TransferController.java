package com.bank.core.banktransferservice.controller;

import com.bank.core.banktransferservice.dto.Result;
import com.bank.core.banktransferservice.dto.TransferRequest;
import com.bank.core.banktransferservice.dto.TransferResponse;
import com.bank.core.banktransferservice.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 转账接口 Controller
 *
 * 【RESTful API 设计】
 * 1. 接口路径：/api/v1/transfer
 * 2. 请求方法：POST
 * 3. 请求格式：JSON
 * 4. 响应格式：JSON
 *
 * 【参数校验】
 * 使用 @Valid 注解触发 JSR-303 校验：
 * - 校验失败会抛出 MethodArgumentNotValidException
 * - 由全局异常处理器统一处理
 * - 返回标准错误格式
 *
 * 【日志记录】
 * 记录关键操作日志，便于审计和问题排查
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    /**
     * 个人跨行转账接口
     *
     * 【接口说明】
     * 接口编号：PIT-001 (Personal Interbank Transfer)
     * 调用方式：HTTPS POST
     * 报文格式：JSON
     * 字符编码：UTF-8
     *
     * 【幂等性保证】
     * - 全局唯一交易流水号 + Token防重
     * - 同一个幂等Token只能成功处理一次
     * - 有效期24小时
     *
     * 【安全认证】
     * - 短信验证码校验
     * - 交易密码校验（国密SM4加密）
     * - 设备指纹校验
     * - 人脸识别（金额≥5万元）
     * - 数字签名校验（RSAwithSHA256）
     *
     * @param request 转账请求
     * @return 转账响应
     */
    @PostMapping("/transfer")
    public Result<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        log.info("收到转账请求，流水号：{}，渠道：{}，付款账号：{}，收款账号：{}，金额：{}",
                request.getHeader().getTranSeqNo(),
                request.getHeader().getChannelId(),
                request.getBody().getPayer().getAcctNo(),
                request.getBody().getPayee().getAcctNo(),
                request.getBody().getTransaction().getAmount());

        // 执行转账
        TransferResponse response = transferService.transfer(request);

        // 返回结果
        if ("000000".equals(response.getHeader().getRespCode())) {
            log.info("转账成功，流水号：{}", request.getHeader().getTranSeqNo());
            return Result.success(response);
        } else {
            log.warn("转账失败，流水号：{}，错误码：{}，错误消息：{}",
                    request.getHeader().getTranSeqNo(),
                    response.getHeader().getRespCode(),
                    response.getHeader().getRespMsg());
            return Result.fail(response.getHeader().getRespCode(),
                    response.getHeader().getRespMsg(),
                    response);
        }
    }

    /**
     * 查询交易状态接口
     *
     * 【接口说明】
     * 用于查询交易的处理状态：
     * - SUCCESS: 交易成功
     * - FAILED: 交易失败
     * - PROCESSING: 交易处理中
     * - TIMEOUT: 交易超时
     *
     * @param tranSeqNo 交易流水号
     * @return 交易状态
     */
    @GetMapping("/transfer/status/{tranSeqNo}")
    public Result<TransferResponse> queryTransferStatus(@PathVariable String tranSeqNo) {
        log.info("查询交易状态，流水号：{}", tranSeqNo);

        // TODO: 实现交易状态查询
        // 需要根据 tranSeqNo 查询交易记录并返回状态

        return Result.success(TransferResponse.builder()
                .header(TransferResponse.ResponseHeader.builder()
                        .tranSeqNo(tranSeqNo)
                        .respCode("000000")
                        .respMsg("查询成功")
                        .build())
                .body(TransferResponse.ResponseBody.builder()
                        .tranStatus("SUCCESS")
                        .build())
                .build());
    }

    /**
     * 查询账户信息接口
     *
     * 【接口说明】
     * 用于查询账户基本信息：
     * - 账户余额
     * - 账户状态
     * - 限额信息
     *
     * @param acctNo 账号
     * @return 账户信息
     */
    @GetMapping("/account/{acctNo}")
    public Result<Object> queryAccount(@PathVariable String acctNo) {
        log.info("查询账户信息，账号：{}", acctNo);

        // TODO: 实现账户信息查询
        // 需要根据 acctNo 查询账户信息并返回

        return Result.success("账户信息查询功能待实现");
    }

    /**
     * 查询当日限额使用情况接口
     *
     * 【接口说明】
     * 用于查询当日限额使用情况：
     * - 当日已转账金额
     * - 当日剩余可用额度
     * - 单笔限额
     * - 单日限额
     *
     * @param acctNo   账号
     * @param channelId 渠道代码
     * @return 限额使用情况
     */
    @GetMapping("/account/{acctNo}/limit")
    public Result<Object> queryAccountLimit(
            @PathVariable String acctNo,
            @RequestParam String channelId) {
        log.info("查询账户限额使用情况，账号：{}，渠道：{}", acctNo, channelId);

        // TODO: 实现限额使用情况查询
        // 需要根据 acctNo 和 channelId 查询当日累计金额和限额

        return Result.success("限额使用情况查询功能待实现");
    }
}