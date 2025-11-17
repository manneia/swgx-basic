-- 发票采集记录表（基础信息）
CREATE TABLE IF NOT EXISTS `invoice_collection_record` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `invoice_type` VARCHAR(10) DEFAULT NULL COMMENT '发票类型代码',
    `invoice_code` VARCHAR(20) DEFAULT NULL COMMENT '发票代码',
    `invoice_number` VARCHAR(20) DEFAULT NULL COMMENT '发票号码',
    `invoice_date` DATETIME DEFAULT NULL COMMENT '开票日期',
    `buyer_name` VARCHAR(200) DEFAULT NULL COMMENT '购方名称',
    `buyer_tax_no` VARCHAR(50) DEFAULT NULL COMMENT '购方纳税识别号',
    `seller_name` VARCHAR(200) DEFAULT NULL COMMENT '销方名称',
    `seller_tax_no` VARCHAR(50) DEFAULT NULL COMMENT '销方纳税识别号',
    `total_amount_tax` DECIMAL(18,2) DEFAULT NULL COMMENT '价税合计',
    `invoice_status` VARCHAR(10) DEFAULT NULL COMMENT '发票状态',
    `check_status` VARCHAR(10) DEFAULT NULL COMMENT '勾选状态',
    `entry_status` VARCHAR(10) DEFAULT NULL COMMENT '入账状态（0-未入账，1-已入账）',
    `check_date` DATE DEFAULT NULL COMMENT '勾选日期',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_invoice_code_number` (`invoice_code`, `invoice_number`),
    KEY `idx_invoice_date` (`invoice_date`),
    KEY `idx_invoice_type` (`invoice_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票采集记录表';


