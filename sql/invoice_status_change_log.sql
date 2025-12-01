-- 发票状态变更调用记录
CREATE TABLE IF NOT EXISTS `invoice_status_change_log` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `invoice_code` varchar(20) DEFAULT NULL COMMENT '发票代码',
    `invoice_number` varchar(50) DEFAULT NULL COMMENT '发票号码',
    `zpfphm` varchar(20) DEFAULT NULL COMMENT '纸票发票号码',
    `previous_invoice_status` varchar(10) DEFAULT NULL COMMENT '变更前发票状态',
    `previous_check_status` varchar(10) DEFAULT NULL COMMENT '变更前勾选状态',
    `current_invoice_status` varchar(10) DEFAULT NULL COMMENT '变更后发票状态',
    `current_check_status` varchar(10) DEFAULT NULL COMMENT '变更后勾选状态',
    `previous_entry_status` varchar(10) DEFAULT NULL COMMENT '变更前入账状态',
    `current_entry_status` varchar(10) DEFAULT NULL COMMENT '变更后入账状态',
    `request_id` varchar(64) DEFAULT NULL COMMENT '请求流水号',
    `request_body` text COMMENT '请求体',
    `response_status` int(11) DEFAULT NULL COMMENT '响应状态码',
    `response_msg` varchar(255) DEFAULT NULL COMMENT '响应消息',
    `response_out_json` text COMMENT '响应输出JSON',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_invoice_code_num` (`invoice_code`,`invoice_number`)
    ) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COMMENT='发票状态变更调用记录';