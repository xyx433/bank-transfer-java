package com.bank.core.banktransferservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回格式 Result<T>
 *
 * 【设计目的】
 * 1. 统一所有接口的返回格式，便于前端处理
 * 2. 包含响应码、消息和数据三个核心字段
 * 3. 支持泛型，可返回任意类型的数据
 *
 * 【响应码规范】
 * - 000000: 成功
 * - 10001-10012: 业务错误（不可重试或有限重试）
 * - 20001-20004: 系统错误（可重试）
 * - 99999: 未知错误
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 响应码
     * 000000=成功，其他=错误码
     */
    private String code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 创建成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code("000000")
                .message("操作成功")
                .data(data)
                .success(true)
                .build();
    }

    /**
     * 创建成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return Result.<T>builder()
                .code("000000")
                .message("操作成功")
                .success(true)
                .build();
    }

    /**
     * 创建成功响应（自定义消息）
     */
    public static <T> Result<T> success(String message, T data) {
        return Result.<T>builder()
                .code("000000")
                .message(message)
                .data(data)
                .success(true)
                .build();
    }

    /**
     * 创建失败响应
     */
    public static <T> Result<T> fail(String code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .success(false)
                .build();
    }

    /**
     * 创建失败响应（带数据）
     */
    public static <T> Result<T> fail(String code, String message, T data) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .success(false)
                .build();
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return "000000".equals(code);
    }
}