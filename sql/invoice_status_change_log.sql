-- 发票状态变更调用记录
CREATE TABLE IF NOT EXISTS `invoice_status_change_log` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `invoice_code` VARCHAR(20) DEFAULT NULL COMMENT '发票代码',
    `invoice_number` VARCHAR(20) DEFAULT NULL COMMENT '发票号码',
    `previous_invoice_status` VARCHAR(10) DEFAULT NULL COMMENT '变更前发票状态',
    `previous_check_status` VARCHAR(10) DEFAULT NULL COMMENT '变更前勾选状态',
    `current_invoice_status` VARCHAR(10) DEFAULT NULL COMMENT '变更后发票状态',
    `current_check_status` VARCHAR(10) DEFAULT NULL COMMENT '变更后勾选状态',
    `request_id` VARCHAR(64) DEFAULT NULL COMMENT '请求流水号',
    `request_body` TEXT COMMENT '请求体',
    `response_status` INT DEFAULT NULL COMMENT '响应状态码',
    `response_msg` VARCHAR(255) DEFAULT NULL COMMENT '响应消息',
    `response_out_json` TEXT COMMENT '响应输出JSON',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_invoice_code_num` (`invoice_code`,`invoice_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票状态变更调用记录';


