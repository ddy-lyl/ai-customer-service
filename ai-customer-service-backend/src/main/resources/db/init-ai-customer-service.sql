-- =============================================================================
-- AI 智能客服 — 数据库一键初始化
-- 库名：customer-service-ai
--
-- Navicat：打开本文件 → 运行全部
-- 账号密码均为 123456
-- =============================================================================

DROP DATABASE IF EXISTS `customer-service-ai`;

CREATE DATABASE `customer-service-ai`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `customer-service-ai`;

SET NAMES utf8mb4;

-- =============================================================================
-- 一、建表
-- =============================================================================

-- 1. 用户与权限
CREATE TABLE `sys_user` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`        VARCHAR(64)  NOT NULL COMMENT '登录用户名',
    `password`        VARCHAR(128) NOT NULL COMMENT 'BCrypt 密文',
    `nickname`        VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    `phone`           VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `email`           VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `avatar_url`      VARCHAR(512) DEFAULT NULL COMMENT '头像',
    `status`          VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    `last_login_time` DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '0未删 1已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

CREATE TABLE `sys_role` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `role_code`   VARCHAR(32)  NOT NULL COMMENT 'USER/STAFF/ADMIN',
    `role_name`   VARCHAR(64)  NOT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

CREATE TABLE `sys_user_role` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT NOT NULL,
    `role_id`     BIGINT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_role_user` (`user_id`),
    KEY `idx_user_role_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

-- 2. 订单
CREATE TABLE `customer_order` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `order_no`         VARCHAR(32)   NOT NULL COMMENT '业务订单号',
    `user_id`          BIGINT        NOT NULL,
    `product_name`     VARCHAR(256)  NOT NULL,
    `product_sku`      VARCHAR(64)   DEFAULT NULL,
    `amount`           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `status`           VARCHAR(32)   NOT NULL COMMENT 'WAIT_PAY/WAIT_SHIPPING/SHIPPED/RECEIVED/REFUNDING/REFUNDED/CLOSED',
    `pay_time`         DATETIME      DEFAULT NULL,
    `shipping_time`    DATETIME      DEFAULT NULL,
    `received_time`    DATETIME      DEFAULT NULL,
    `receiver_name`    VARCHAR(64)   DEFAULT NULL,
    `receiver_phone`   VARCHAR(20)   DEFAULT NULL,
    `receiver_address` VARCHAR(512)  DEFAULT NULL,
    `remark`           VARCHAR(512)  DEFAULT NULL,
    `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`          TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_order_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户订单';

-- 3. 工单
CREATE TABLE `ticket` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `ticket_no`      VARCHAR(32)  NOT NULL COMMENT '工单编号',
    `user_id`        BIGINT       NOT NULL,
    `order_no`       VARCHAR(32)  DEFAULT NULL,
    `session_id`     BIGINT       DEFAULT NULL,
    `title`          VARCHAR(128) NOT NULL,
    `type`           VARCHAR(32)  NOT NULL,
    `description`    TEXT         NOT NULL,
    `status`         VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    `priority`       VARCHAR(16)  NOT NULL DEFAULT 'MEDIUM',
    `staff_id`       BIGINT       DEFAULT NULL,
    `source`         VARCHAR(16)  NOT NULL DEFAULT 'USER',
    `process_result` TEXT         DEFAULT NULL,
    `close_reason`   VARCHAR(512) DEFAULT NULL,
    `resolved_time`  DATETIME     DEFAULT NULL,
    `closed_time`    DATETIME     DEFAULT NULL,
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ticket_no` (`ticket_no`),
    KEY `idx_ticket_user` (`user_id`),
    KEY `idx_ticket_staff` (`staff_id`),
    KEY `idx_ticket_status` (`status`),
    KEY `idx_ticket_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后工单';

CREATE TABLE `ticket_flow` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `ticket_id`     BIGINT       NOT NULL,
    `operator_id`   BIGINT       DEFAULT NULL,
    `operator_name` VARCHAR(64)  DEFAULT NULL,
    `operator_role` VARCHAR(16)  NOT NULL,
    `action`        VARCHAR(16)  NOT NULL,
    `from_status`   VARCHAR(16)  DEFAULT NULL,
    `to_status`     VARCHAR(16)  DEFAULT NULL,
    `remark`        VARCHAR(512) DEFAULT NULL,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_flow_ticket` (`ticket_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单流转记录';

-- 4. AI 会话与消息（含在线客服、乐观锁字段，无需再 ALTER）
CREATE TABLE `chat_session` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`           BIGINT       NOT NULL,
    `title`             VARCHAR(128) DEFAULT NULL,
    `status`            VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    `service_mode`      VARCHAR(16)  NOT NULL DEFAULT 'AI' COMMENT 'AI/WAITING_AGENT/HUMAN',
    `assigned_staff_id` BIGINT       DEFAULT NULL,
    `active_ticket_id`  BIGINT       DEFAULT NULL,
    `handoff_reason`    VARCHAR(32)  DEFAULT NULL,
    `handoff_time`      DATETIME     DEFAULT NULL,
    `last_message_time` DATETIME     DEFAULT NULL,
    `version`           INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_chat_session_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI聊天会话';

CREATE TABLE `chat_message` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
    `session_id`         BIGINT       NOT NULL,
    `user_id`            BIGINT       NOT NULL,
    `role`               VARCHAR(16)  NOT NULL,
    `sender_id`          BIGINT       DEFAULT NULL COMMENT 'STAFF消息发送者',
    `content`            MEDIUMTEXT   NOT NULL,
    `intent`             VARCHAR(32)  DEFAULT NULL,
    `model_name`         VARCHAR(64)  DEFAULT NULL,
    `prompt_tokens`      INT          DEFAULT NULL,
    `completion_tokens`  INT          DEFAULT NULL,
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`            TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_msg_session` (`session_id`),
    KEY `idx_msg_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI聊天消息';

-- 5. 知识库与 RAG
CREATE TABLE `knowledge_base` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(128) NOT NULL,
    `description` VARCHAR(512) DEFAULT NULL,
    `status`      VARCHAR(16)  NOT NULL DEFAULT 'ENABLED',
    `created_by`  BIGINT       DEFAULT NULL,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库';

CREATE TABLE `knowledge_document` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
    `knowledge_base_id`  BIGINT       NOT NULL,
    `original_filename`  VARCHAR(256) NOT NULL,
    `stored_filename`    VARCHAR(256) NOT NULL,
    `file_path`          VARCHAR(512) NOT NULL,
    `file_type`          VARCHAR(16)  NOT NULL,
    `file_size`          BIGINT       DEFAULT 0,
    `status`             VARCHAR(16)  NOT NULL DEFAULT 'UPLOADED',
    `chunk_count`        INT          DEFAULT 0,
    `parsed_content`     MEDIUMTEXT   DEFAULT NULL,
    `fail_reason`        VARCHAR(512) DEFAULT NULL,
    `uploaded_by`        BIGINT       DEFAULT NULL,
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`            TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_doc_kb` (`knowledge_base_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档';

CREATE TABLE `document_chunk` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `knowledge_base_id` BIGINT       NOT NULL,
    `document_id`       BIGINT       NOT NULL,
    `chunk_index`       INT          NOT NULL,
    `content`           TEXT         NOT NULL,
    `content_hash`      VARCHAR(64)  DEFAULT NULL,
    `token_count`       INT          DEFAULT NULL,
    `vector_id`         VARCHAR(128) DEFAULT NULL,
    `embedding_model`   VARCHAR(64)  DEFAULT NULL,
    `vectorized`        TINYINT      NOT NULL DEFAULT 0,
    `metadata_json`     TEXT         DEFAULT NULL,
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_chunk_doc` (`document_id`),
    KEY `idx_chunk_kb` (`knowledge_base_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档切片';

-- 6. AI 审计日志
CREATE TABLE `ai_retrieval_log` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `session_id`        BIGINT       DEFAULT NULL,
    `user_id`           BIGINT       DEFAULT NULL,
    `user_message_id`   BIGINT       DEFAULT NULL,
    `question_text`     TEXT         DEFAULT NULL,
    `knowledge_base_id` BIGINT       DEFAULT NULL,
    `document_id`       BIGINT       DEFAULT NULL,
    `document_name`     VARCHAR(256) DEFAULT NULL,
    `chunk_id`          BIGINT       DEFAULT NULL,
    `chunk_content`     TEXT         DEFAULT NULL,
    `score`             DOUBLE       DEFAULT NULL,
    `rank_no`           INT          DEFAULT NULL,
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_retrieval_session` (`session_id`),
    KEY `idx_retrieval_user_msg` (`user_message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG召回日志';

CREATE TABLE `ai_tool_call_log` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `session_id`      BIGINT       DEFAULT NULL,
    `user_id`         BIGINT       DEFAULT NULL,
    `user_message_id` BIGINT       DEFAULT NULL,
    `tool_name`       VARCHAR(64)  NOT NULL,
    `request_json`    TEXT         DEFAULT NULL,
    `response_json`   TEXT         DEFAULT NULL,
    `status`          VARCHAR(16)  NOT NULL,
    `error_message`   VARCHAR(512) DEFAULT NULL,
    `cost_millis`     BIGINT       DEFAULT NULL,
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_tool_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI工具调用日志';

-- =============================================================================
-- 二、种子数据
-- =============================================================================

SET @pwd = '$2a$10$gY1uLNq0I81SiVSV6VdnA.2zg1LVSLBnIqHPnH9Zne6cL/IebxaXS';

INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `description`) VALUES
(1, 'USER',  '普通用户', '下单、咨询、创建工单'),
(2, 'STAFF', '客服人员', '处理工单、查看订单'),
(3, 'ADMIN', '系统管理员', '用户管理、知识库、工单分配、AI日志');

INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `phone`, `email`, `status`) VALUES
(1, 'admin',   @pwd, '系统管理员', '13800000001', 'admin@demo.com',    'ENABLED'),
(2, 'staff01', @pwd, '客服小王',   '13800000002', 'staff@demo.com',    'ENABLED'),
(3, 'user01',  @pwd, '张三',       '13800000003', 'zhangsan@demo.com', 'ENABLED'),
(4, 'user02',  @pwd, '李四',       '13800000004', 'lisi@demo.com',     'ENABLED');

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 3), (2, 2), (3, 1), (4, 1);

INSERT INTO `customer_order`
(`order_no`, `user_id`, `product_name`, `product_sku`, `amount`, `status`,
 `pay_time`, `shipping_time`, `received_time`,
 `receiver_name`, `receiver_phone`, `receiver_address`) VALUES
('ORD202606010001', 3, '无线蓝牙耳机 Pro',   'SKU-EAR-001',  299.00, 'WAIT_PAY',       NULL,                  NULL,                  NULL, '张三', '13800000003', '北京市朝阳区望京街道 1 号'),
('ORD202606010002', 3, '智能手环 S2',         'SKU-BAND-02',  199.00, 'WAIT_SHIPPING',  '2026-06-01 09:00:00', NULL,                  NULL, '张三', '13800000003', '北京市朝阳区望京街道 1 号'),
('ORD202606010003', 3, 'USB-C 快充数据线 2m', 'SKU-CABLE-03',  39.90, 'SHIPPED',        '2026-06-01 10:00:00', '2026-06-02 08:00:00', NULL, '张三', '13800000003', '北京市朝阳区望京街道 1 号'),
('ORD202606010004', 3, '手机支架 磁吸版',     'SKU-HOLD-04',   49.00, 'RECEIVED',       '2026-05-28 14:00:00', '2026-05-29 10:00:00', '2026-06-01 18:00:00', '张三', '13800000003', '北京市朝阳区望京街道 1 号'),
('ORD202606010005', 3, '降噪耳塞 入门款',     'SKU-EAR-05',    89.00, 'REFUNDING',      '2026-05-25 11:00:00', '2026-05-26 09:00:00', NULL, '张三', '13800000003', '北京市朝阳区望京街道 1 号'),
('ORD202606020001', 4, '机械键盘 K87',        'SKU-KB-087',   459.00, 'WAIT_PAY',       NULL,                  NULL,                  NULL, '李四', '13800000004', '上海市浦东新区张江路 88 号'),
('ORD202606020002', 4, '游戏鼠标 G5',         'SKU-MSE-05',   199.00, 'WAIT_SHIPPING',  '2026-06-01 08:30:00', NULL,                  NULL, '李四', '13800000004', '上海市浦东新区张江路 88 号'),
('ORD202606020003', 4, '显示器支架 双臂',     'SKU-MON-03',   168.00, 'SHIPPED',        '2026-05-30 16:00:00', '2026-05-31 14:00:00', NULL, '李四', '13800000004', '上海市浦东新区张江路 88 号'),
('ORD202606020004', 4, 'HDMI 2.1 线缆 3m',    'SKU-HDMI-21',   79.00, 'RECEIVED',       '2026-05-27 12:00:00', '2026-05-28 11:00:00', '2026-05-30 20:00:00', '李四', '13800000004', '上海市浦东新区张江路 88 号'),
('ORD202606020005', 4, '笔记本散热底座',      'SKU-COOL-05',  129.00, 'CLOSED',         '2026-05-20 10:00:00', '2026-05-21 09:00:00', '2026-05-24 15:00:00', '李四', '13800000004', '上海市浦东新区张江路 88 号');
