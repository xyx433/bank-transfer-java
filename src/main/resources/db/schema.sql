-- =====================================================
-- 银行核心转账系统 - 数据库表结构设计
-- 文档编号: BRD-TRANSFER-2026-001
-- 创建日期: 2026-06-11
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS bank_core DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE bank_core;

-- =====================================================
-- 1. 账户表 (account)
-- 设计说明:
-- - 金额字段使用 DECIMAL(18,2)，严禁使用 Double/Float，避免精度丢失
-- - 余额字段包含：总余额、冻结金额、可用余额（计算字段）
-- - 支持多币种账户
-- - 包含账户状态、星级（VIP等级）等风控相关字段
-- =====================================================
DROP TABLE IF EXISTS `account`;

CREATE TABLE `account` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '账户主键ID',
    `acct_no` VARCHAR(19) NOT NULL COMMENT '账号（银行卡号）',
    `acct_name` VARCHAR(120) NOT NULL COMMENT '账户名称（户名）',
    `id_type` VARCHAR(2) NOT NULL DEFAULT '01' COMMENT '证件类型：01=身份证，02=护照，03=军官证',
    `id_no` VARCHAR(18) NOT NULL COMMENT '证件号码',
    `bank_code` VARCHAR(12) NOT NULL COMMENT '银行代码（支付系统行号）',
    `bank_name` VARCHAR(100) NOT NULL COMMENT '银行名称',
    `currency_code` VARCHAR(3) NOT NULL DEFAULT 'CNY' COMMENT '币种代码：CNY=人民币，USD=美元',
    `balance` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '账户总余额（元）',
    `frozen_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额（元）',
    `in_transit_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '在途资金（元）',
    `account_status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '账户状态：NORMAL=正常，FROZEN=司法冻结，PARTIAL_FROZEN=部分冻结，SLEEP=休眠户，CLOSED=已销户，LOST=挂失，RESTRICTED=交易限制',
    `star_level` INT NOT NULL DEFAULT 1 COMMENT '客户星级：1-7，5星以上为VIP，7星为私银客户',
    `daily_limit` DECIMAL(18,2) NOT NULL DEFAULT 200000.00 COMMENT '单日转账限额（元）',
    `single_limit` DECIMAL(18,2) NOT NULL DEFAULT 50000.00 COMMENT '单笔转账限额（元）',
    `mobile_phone` VARCHAR(11) COMMENT '预留手机号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_acct_no` (`acct_no`),
    UNIQUE KEY `uk_id_no` (`id_type`, `id_no`),
    KEY `idx_bank_code` (`bank_code`),
    KEY `idx_account_status` (`account_status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户信息表';

-- =====================================================
-- 2. 交易流水表 (transfer_record)
-- 设计说明:
-- - 幂等性保证：idempotency_key 建立唯一索引，防止重复提交
-- - 完整记录转账双方信息、金额、状态、路由等
-- - 支持交易状态流转：INIT -> PROCESSING -> SUCCESS/FAILED/TIMEOUT
-- - 包含渠道、业务类型、手续费等完整业务字段
-- =====================================================
DROP TABLE IF EXISTS `transfer_record`;

CREATE TABLE `transfer_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水主键ID',
    `tran_seq_no` VARCHAR(28) NOT NULL COMMENT '交易流水号，格式：YYYYMMDD + PIT + 2位渠道码 + 8位序号',
    `idempotency_key` VARCHAR(36) NOT NULL COMMENT '幂等号（UUID v4格式），防重复提交',
    `channel_id` VARCHAR(2) NOT NULL COMMENT '渠道代码：MB=手机银行，EB=网银，WC=微信小程序',

    -- 付款人信息
    `payer_acct_no` VARCHAR(19) NOT NULL COMMENT '付款账号',
    `payer_acct_name` VARCHAR(120) NOT NULL COMMENT '付款人姓名',
    `payer_id_type` VARCHAR(2) NOT NULL COMMENT '付款人证件类型',
    `payer_id_no` VARCHAR(18) NOT NULL COMMENT '付款人证件号码',
    `payer_bank_code` VARCHAR(12) NOT NULL COMMENT '付款行银行代码',
    `payer_bank_name` VARCHAR(100) NOT NULL COMMENT '付款行名称',

    -- 收款人信息
    `payee_acct_no` VARCHAR(19) NOT NULL COMMENT '收款账号',
    `payee_acct_name` VARCHAR(120) NOT NULL COMMENT '收款人姓名',
    `payee_bank_code` VARCHAR(12) NOT NULL COMMENT '收款行银行代码',
    `payee_bank_name` VARCHAR(100) NOT NULL COMMENT '收款行名称',
    `payee_bank_union_code` VARCHAR(12) NOT NULL COMMENT '收款行支付系统行号',

    -- 交易信息
    `amount` DECIMAL(18,2) NOT NULL COMMENT '转账金额（元）',
    `currency_code` VARCHAR(3) NOT NULL DEFAULT 'CNY' COMMENT '币种代码',
    `biz_type` VARCHAR(4) NOT NULL COMMENT '业务类型：1001=普通转账，1002=实时转账，1003=次日到账',
    `purpose_code` VARCHAR(2) NOT NULL COMMENT '附言类型：01=货款，02=工资，03=借款还款，04=其他',
    `remit_info` VARCHAR(300) COMMENT '交易附言',
    `route_mode` VARCHAR(4) NOT NULL COMMENT '路由模式：AUTO=系统智能选路，HVPS=大额，BEPS=小额，IBPS=超网',
    `fee_bearer` VARCHAR(2) NOT NULL DEFAULT '01' COMMENT '手续费承担方：01=付款方，02=收款方',
    `fee_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '手续费金额（元）',

    -- 清算信息
    `acp_tran_seq_no` VARCHAR(22) COMMENT '受理平台流水号',
    `core_serial_no` VARCHAR(22) COMMENT '核心系统流水号',
    `host_seq_no` VARCHAR(28) COMMENT '人行清算流水号',
    `host_status` VARCHAR(2) COMMENT '人行返回状态：01=已受理，02=已清算，03=已轧差，04=已到账，99=已撤销',

    -- 安全信息
    `sms_code` VARCHAR(6) COMMENT '短信验证码（脱敏存储）',
    `device_fingerprint` VARCHAR(64) COMMENT '设备指纹',
    `client_ip` VARCHAR(45) COMMENT '客户端IP',
    `gps_longitude` DECIMAL(10,6) COMMENT 'GPS经度',
    `gps_latitude` DECIMAL(10,6) COMMENT 'GPS纬度',

    -- 交易状态
    `tran_status` VARCHAR(16) NOT NULL DEFAULT 'INIT' COMMENT '交易状态：INIT=初始化，PROCESSING=处理中，SUCCESS=成功，FAILED=失败，TIMEOUT=超时',
    `resp_code` VARCHAR(6) COMMENT '响应码',
    `resp_msg` VARCHAR(500) COMMENT '响应消息',
    `fail_reason_code` VARCHAR(10) COMMENT '失败原因码',
    `fail_reason_desc` VARCHAR(500) COMMENT '失败原因描述',

    -- 时间戳
    `tran_timestamp` DATETIME(3) NOT NULL COMMENT '交易发起时间（毫秒精度）',
    `completed_time` DATETIME(3) COMMENT '交易完成时间（毫秒精度）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_idempotency_key` (`idempotency_key`) COMMENT '幂等号唯一索引，防止重复提交',
    UNIQUE KEY `uk_tran_seq_no` (`tran_seq_no`) COMMENT '交易流水号唯一索引',
    KEY `idx_payer_acct_no` (`payer_acct_no`) COMMENT '付款账号索引，用于查询账户交易记录',
    KEY `idx_payee_acct_no` (`payee_acct_no`) COMMENT '收款账号索引',
    KEY `idx_tran_status` (`tran_status`) COMMENT '交易状态索引',
    KEY `idx_create_time` (`create_time`) COMMENT '创建时间索引，用于日切统计',
    KEY `idx_channel_date` (`channel_id`, `create_time`) COMMENT '渠道+日期组合索引，用于限额统计'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='转账交易流水表';

-- =====================================================
-- 3. 每日限额统计表 (daily_limit_stat)
-- 设计说明:
-- - 按自然日统计每个账户在每个渠道的累计转账金额
-- - 支持日切后快速重置
-- - 排除已冲正/已撤销的交易
-- =====================================================
DROP TABLE IF EXISTS `daily_limit_stat`;

CREATE TABLE `daily_limit_stat` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `acct_no` VARCHAR(19) NOT NULL COMMENT '账号',
    `channel_id` VARCHAR(2) NOT NULL COMMENT '渠道代码',
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `total_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '当日累计转账金额（元）',
    `total_count` INT NOT NULL DEFAULT 0 COMMENT '当日累计转账笔数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_acct_channel_date` (`acct_no`, `channel_id`, `stat_date`) COMMENT '账户+渠道+日期唯一索引',
    KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日限额统计表';

-- =====================================================
-- 4. 敏感词黑名单表 (sensitive_word)
-- 设计说明:
-- - 用于交易附言敏感词过滤
-- - 支持正则表达式匹配
-- =====================================================
DROP TABLE IF EXISTS `sensitive_word`;

CREATE TABLE `sensitive_word` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `word` VARCHAR(100) NOT NULL COMMENT '敏感词',
    `category` VARCHAR(20) NOT NULL COMMENT '分类：VIRTUAL_CURRENCY=虚拟货币，GAMBLING=博彩，PORNOGRAPHY=色情，FRAUD=诈骗',
    `is_regex` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否正则表达式：0=否，1=是',
    `status` VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE=启用，INACTIVE=停用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_word` (`word`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感词黑名单表';

-- =====================================================
-- 5. 收款人黑名单表 (payee_blacklist)
-- 设计说明:
-- - 收款账户黑名单，用于风控拦截
-- - 来源：公安电诈涉案名单、行内历史欺诈名单、人行支付系统灰名单
-- =====================================================
DROP TABLE IF EXISTS `payee_blacklist`;

CREATE TABLE `payee_blacklist` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `payee_acct_no` VARCHAR(19) NOT NULL COMMENT '收款账号',
    `payee_acct_name` VARCHAR(120) COMMENT '收款人姓名',
    `source` VARCHAR(20) NOT NULL COMMENT '来源：POLICE=公安电诈，INTERNAL=行内历史，PBOC=人行灰名单',
    `reason` VARCHAR(500) COMMENT '列入原因',
    `status` VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE=启用，INACTIVE=停用',
    `expire_time` DATETIME COMMENT '过期时间（NULL表示永久）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payee_acct_no` (`payee_acct_no`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收款人黑名单表';

-- =====================================================
-- 6. 银行行号表 (bank_info)
-- 设计说明:
-- - 存储人民银行发布的行名行号表
-- - 用于校验收款行信息
-- =====================================================
DROP TABLE IF EXISTS `bank_info`;

CREATE TABLE `bank_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `bank_code` VARCHAR(12) NOT NULL COMMENT '银行代码（支付系统行号）',
    `bank_name` VARCHAR(100) NOT NULL COMMENT '银行名称',
    `union_code` VARCHAR(12) NOT NULL COMMENT '支付系统行号（大额/小额）',
    `status` VARCHAR(10) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL=正常，SUSPENDED=停用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bank_code` (`bank_code`),
    UNIQUE KEY `uk_union_code` (`union_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='银行行号表';

-- =====================================================
-- 7. 防重放Nonce表 (nonce_record)
-- 设计说明:
-- - 记录已使用的nonce，防止重放攻击
-- - 使用布隆过滤器优化查询性能
-- =====================================================
DROP TABLE IF EXISTS `nonce_record`;

CREATE TABLE `nonce_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `nonce` VARCHAR(64) NOT NULL COMMENT '防重放随机数',
    `client_ip` VARCHAR(45) COMMENT '客户端IP',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_nonce` (`nonce`) COMMENT 'nonce唯一索引',
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='防重放Nonce记录表';