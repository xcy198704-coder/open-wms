-- ============================================================================
-- 内外箱管理（外箱套内箱）1.0.0.4
--
-- 内容：
--   1. 新增 base_box_spec（商品箱规）、in_box_code（入库箱码）、out_box_code（出库箱码）
--   2. 现有表加 box_id / outer_box_id 列与索引，core_inventory 走 online DDL
--   3. sys_config 追加 8 个功能开关与编码规则参数
--   4. magic_chain 追加 inBoxScanChain / outBoxScanChain 两条链
--   5. sys_menu 追加两个扫描页菜单与按钮权限（角色授权、租户套餐同步）
--
-- 说明：
--   * 全脚本幂等，可重复执行；加列/加索引前先查 information_schema。
--   * 执行的会话需具备 CREATE ROUTINE 以外的普通 DDL 权限即可（未使用存储过程）。
--   * 功能开关默认 0，升级后行为与升级前一致；回退只需把开关置回 0，DDL 无需回滚。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. 新表
-- ----------------------------------------------------------------------------

-- 1.1 商品箱规：按商品维护内箱装数、每外箱内箱数、箱码模式
CREATE TABLE IF NOT EXISTS `base_box_spec` (
  `spec_id` bigint NOT NULL AUTO_INCREMENT COMMENT '箱规ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `spec_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '规格名称，如 4瓶×2内箱/外箱',
  `inner_qty` decimal(18, 5) NULL DEFAULT NULL COMMENT '每内箱装数（小单位）',
  `outer_inner_count` int NULL DEFAULT 1 COMMENT '每外箱内箱数',
  `box_code_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'NONE' COMMENT '箱码模式 NONE/SKU/INSTANCE/BOTH',
  `box_barcode_rule` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '自印码前缀/规则',
  `is_default` tinyint(1) NULL DEFAULT 0 COMMENT '是否默认',
  `enable` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `expand_fields` json NULL COMMENT '扩展字段',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人id',
  `create_by_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '修改人id',
  `update_by_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除标志',
  `delete_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`spec_id`) USING BTREE,
  UNIQUE INDEX `uk_product_spec`(`tenant_id` ASC, `product_id` ASC, `spec_name` ASC) USING BTREE,
  INDEX `idx_product`(`product_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品箱规' ROW_FORMAT = Dynamic;

-- 1.2 入库箱码：外箱/内箱实例，唯一索引用于扫码判定
CREATE TABLE IF NOT EXISTS `in_box_code` (
  `box_id` bigint NOT NULL AUTO_INCREMENT COMMENT '箱ID',
  `box_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '箱码',
  `code_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'UNIQUE' COMMENT '码类型 FIXED/UNIQUE',
  `box_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'INNER' COMMENT '箱层级 OUTER/INNER',
  `parent_box_id` bigint NULL DEFAULT NULL COMMENT '父外箱ID（内箱指向所属外箱）',
  `parent_box_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '父外箱码',
  `source` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'SYSTEM' COMMENT '箱码来源 SYSTEM/SUPPLIER',
  `order_id` bigint NULL DEFAULT NULL COMMENT '预到货单ID',
  `order_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '预到货单号',
  `order_detail_id` bigint NULL DEFAULT NULL COMMENT '预到货明细ID',
  `enter_id` bigint NULL DEFAULT NULL COMMENT '入库单ID',
  `enter_detail_id` bigint NULL DEFAULT NULL COMMENT '入库单明细ID',
  `shelve_detail_id` bigint NULL DEFAULT NULL COMMENT '上架明细ID',
  `product_id` bigint NULL DEFAULT NULL COMMENT '商品ID',
  `product_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品编号',
  `product_name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品名称',
  `product_spec` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品规格',
  `inner_qty` decimal(18, 5) NULL DEFAULT NULL COMMENT '每内箱装数',
  `outer_inner_count` int NULL DEFAULT 1 COMMENT '每外箱内箱数',
  `plan_qty` decimal(18, 5) NULL DEFAULT NULL COMMENT '计划数量',
  `scanned_qty` decimal(18, 5) NULL DEFAULT 0.00000 COMMENT '已扫数量',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'CREATED' COMMENT '状态 CREATED/PARTIAL/RECEIVED/SHELVED/VOID',
  `scan_by` bigint NULL DEFAULT NULL COMMENT '扫描人id',
  `scan_by_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '扫描人',
  `scan_time` datetime NULL DEFAULT NULL COMMENT '扫描时间',
  `close_time` datetime NULL DEFAULT NULL COMMENT '封箱时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `expand_fields` json NULL COMMENT '扩展字段',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人id',
  `create_by_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '修改人id',
  `update_by_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除标志',
  `delete_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`box_id`) USING BTREE,
  UNIQUE INDEX `uk_box_code`(`tenant_id` ASC, `box_code` ASC) USING BTREE,
  INDEX `idx_parent`(`parent_box_id` ASC) USING BTREE,
  INDEX `idx_order`(`order_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '入库箱码' ROW_FORMAT = Dynamic;

-- 1.3 出库箱码：外箱绑定内箱，供装箱与发货核验
CREATE TABLE IF NOT EXISTS `out_box_code` (
  `box_id` bigint NOT NULL AUTO_INCREMENT COMMENT '箱ID',
  `box_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '箱码',
  `code_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'UNIQUE' COMMENT '码类型 FIXED/UNIQUE',
  `box_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'INNER' COMMENT '箱层级 OUTER/INNER',
  `parent_box_id` bigint NULL DEFAULT NULL COMMENT '父外箱ID（内箱指向所属外箱）',
  `parent_box_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '父外箱码',
  `source` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'SYSTEM' COMMENT '箱码来源 SYSTEM/SUPPLIER',
  `out_order_id` bigint NULL DEFAULT NULL COMMENT '出库单ID',
  `out_order_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '出库单号',
  `order_id` bigint NULL DEFAULT NULL COMMENT '销售单ID',
  `order_detail_id` bigint NULL DEFAULT NULL COMMENT '销售明细ID',
  `package_id` bigint NULL DEFAULT NULL COMMENT '打包单ID',
  `package_detail_id` bigint NULL DEFAULT NULL COMMENT '打包明细ID',
  `product_id` bigint NULL DEFAULT NULL COMMENT '商品ID',
  `product_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品编号',
  `product_name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品名称',
  `product_spec` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品规格',
  `inner_qty` decimal(18, 5) NULL DEFAULT NULL COMMENT '每内箱装数',
  `outer_inner_count` int NULL DEFAULT 1 COMMENT '每外箱内箱数',
  `plan_qty` decimal(18, 5) NULL DEFAULT NULL COMMENT '计划数量',
  `packed_qty` decimal(18, 5) NULL DEFAULT 0.00000 COMMENT '已装箱数量',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'CREATED' COMMENT '状态 CREATED/PACKING/PACKED/VERIFIED/SHIPPED/VOID',
  `scan_by` bigint NULL DEFAULT NULL COMMENT '扫描人id',
  `scan_by_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '扫描人',
  `scan_time` datetime NULL DEFAULT NULL COMMENT '扫描时间',
  `close_time` datetime NULL DEFAULT NULL COMMENT '封箱时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `expand_fields` json NULL COMMENT '扩展字段',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人id',
  `create_by_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '修改人id',
  `update_by_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除标志',
  `delete_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`box_id`) USING BTREE,
  UNIQUE INDEX `uk_box_code`(`tenant_id` ASC, `box_code` ASC) USING BTREE,
  INDEX `idx_parent`(`parent_box_id` ASC) USING BTREE,
  INDEX `idx_order`(`out_order_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '出库箱码' ROW_FORMAT = Dynamic;

-- ----------------------------------------------------------------------------
-- 2. 现有表加列 + 索引
--    每条语句先查 information_schema，未存在才执行，保证幂等。
-- ----------------------------------------------------------------------------

-- 2.1 in_enter_detail.box_id
SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `in_enter_detail` ADD COLUMN `box_id` bigint NULL DEFAULT NULL COMMENT ''箱ID'' AFTER `case_number`',
  'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'in_enter_detail' AND COLUMN_NAME = 'box_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `in_enter_detail` ADD INDEX `idx_box_id`(`box_id` ASC) USING BTREE',
  'DO 0')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'in_enter_detail' AND INDEX_NAME = 'idx_box_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.2 in_shelve_detail.box_id
SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `in_shelve_detail` ADD COLUMN `box_id` bigint NULL DEFAULT NULL COMMENT ''箱ID'' AFTER `case_number`',
  'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'in_shelve_detail' AND COLUMN_NAME = 'box_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `in_shelve_detail` ADD INDEX `idx_box_id`(`box_id` ASC) USING BTREE',
  'DO 0')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'in_shelve_detail' AND INDEX_NAME = 'idx_box_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.3 core_inventory.box_id —— 库存表数据量大，列与索引均走 online DDL
SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `core_inventory` ADD COLUMN `box_id` bigint NULL DEFAULT NULL COMMENT ''箱ID'', ALGORITHM=INPLACE, LOCK=NONE',
  'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'core_inventory' AND COLUMN_NAME = 'box_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `core_inventory` ADD INDEX `idx_box_id`(`box_id` ASC) USING BTREE, ALGORITHM=INPLACE, LOCK=NONE',
  'DO 0')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'core_inventory' AND INDEX_NAME = 'idx_box_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.4 core_inventory_history.box_id
SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `core_inventory_history` ADD COLUMN `box_id` bigint NULL DEFAULT NULL COMMENT ''箱ID'' AFTER `case_number`',
  'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'core_inventory_history' AND COLUMN_NAME = 'box_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `core_inventory_history` ADD INDEX `idx_box_id`(`box_id` ASC) USING BTREE',
  'DO 0')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'core_inventory_history' AND INDEX_NAME = 'idx_box_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.5 out_package_detail.box_id（内箱）/ outer_box_id（外箱）
SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `out_package_detail` ADD COLUMN `box_id` bigint NULL DEFAULT NULL COMMENT ''内箱ID'' AFTER `case_number`',
  'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'out_package_detail' AND COLUMN_NAME = 'box_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `out_package_detail` ADD INDEX `idx_box_id`(`box_id` ASC) USING BTREE',
  'DO 0')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'out_package_detail' AND INDEX_NAME = 'idx_box_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `out_package_detail` ADD COLUMN `outer_box_id` bigint NULL DEFAULT NULL COMMENT ''外箱ID'' AFTER `box_id`',
  'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'out_package_detail' AND COLUMN_NAME = 'outer_box_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `out_package_detail` ADD INDEX `idx_outer_box_id`(`outer_box_id` ASC) USING BTREE',
  'DO 0')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'out_package_detail' AND INDEX_NAME = 'idx_outer_box_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.6 out_order_picking_detail.box_id
SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `out_order_picking_detail` ADD COLUMN `box_id` bigint NULL DEFAULT NULL COMMENT ''箱ID''',
  'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'out_order_picking_detail' AND COLUMN_NAME = 'box_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `out_order_picking_detail` ADD INDEX `idx_box_id`(`box_id` ASC) USING BTREE',
  'DO 0')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'out_order_picking_detail' AND INDEX_NAME = 'idx_box_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------------------------------------------------------
-- 3. sys_config 功能参数
--    开关默认 0，升级后不改变现有流程；config_type = N 表示非系统内置，允许界面修改
-- ----------------------------------------------------------------------------

INSERT INTO `sys_config` (`tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `create_by_name`)
SELECT '000000', '入库内箱级管理开关', 'in_boxLevelEnable', '0', 'N', 1, NOW(), '超级管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'in_boxLevelEnable');

INSERT INTO `sys_config` (`tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `create_by_name`)
SELECT '000000', '入库内箱级模式（MIX=混扫 ONLY_OUTER=仅整箱 ONLY_INNER=仅拆箱）', 'in_boxLevelMode', 'MIX', 'N', 1, NOW(), '超级管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'in_boxLevelMode');

INSERT INTO `sys_config` (`tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `create_by_name`)
SELECT '000000', '入库箱码来源（SYSTEM=仓库自印 SUPPLIER=供应商）', 'in_boxCodeSource', 'SYSTEM', 'N', 1, NOW(), '超级管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'in_boxCodeSource');

INSERT INTO `sys_config` (`tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `create_by_name`)
SELECT '000000', '入库扫固定箱码时自动建内箱实例', 'in_boxAutoCreate', '1', 'N', 1, NOW(), '超级管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'in_boxAutoCreate');

INSERT INTO `sys_config` (`tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `create_by_name`)
SELECT '000000', '出库内箱级管理开关', 'out_boxLevelEnable', '0', 'N', 1, NOW(), '超级管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'out_boxLevelEnable');

INSERT INTO `sys_config` (`tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `create_by_name`)
SELECT '000000', '出库装箱模式（MIX=混扫 ONLY_OUTER=仅外箱 ONLY_INNER=仅内箱）', 'out_boxLevelMode', 'MIX', 'N', 1, NOW(), '超级管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'out_boxLevelMode');

INSERT INTO `sys_config` (`tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `create_by_name`)
SELECT '000000', '出库扫固定箱码时自动建内箱实例', 'out_boxAutoCreate', '1', 'N', 1, NOW(), '超级管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'out_boxAutoCreate');

INSERT INTO `sys_config` (`tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `create_by_name`)
SELECT '000000', '入库自印箱码前缀', 'box_codeRuleIn', 'IN-', 'N', 1, NOW(), '超级管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'box_codeRuleIn');

INSERT INTO `sys_config` (`tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `create_by_name`)
SELECT '000000', '出库自印箱码前缀', 'box_codeRuleOut', 'OUT-', 'N', 1, NOW(), '超级管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'box_codeRuleOut');

-- ----------------------------------------------------------------------------
-- 4. liteflow 链
--    与现有 inScanChain / OutScanChain 并存，服务按开关选链，关闭开关走原链
-- ----------------------------------------------------------------------------

INSERT INTO `magic_chain` (`application_name`, `chain_name`, `chain_desc`, `el_data`, `enable`, `create_by`, `create_by_name`, `create_time`, `del_flag`, `tenant_id`)
SELECT 'Rattan-Plus-Api', 'inBoxScanChain', '内外箱扫描入库流程',
       'THEN(inScanInitCmp,inScanBoxValidationCmp,inScanValidationCmp,inScanGenEnterCmp,inScanGenCheckCmp,inScanGenShelveCmp,inScanInventoryCmp,inScanMqCmp);',
       1, 1, '超级管理员', NOW(), 0, '000000' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `magic_chain` WHERE `chain_name` = 'inBoxScanChain' AND `del_flag` = 0);

INSERT INTO `magic_chain` (`application_name`, `chain_name`, `chain_desc`, `el_data`, `enable`, `create_by`, `create_by_name`, `create_time`, `del_flag`, `tenant_id`)
SELECT 'Rattan-Plus-Api', 'outBoxScanChain', '内外箱装箱出库流程',
       'THEN(outScanInitCmp,outScanBoxValidationCmp,outScanValidationCmp,outScanGenPackageCmp,outScanCalcStatusCmp,outScanGenPlateCmp,OUT_FINISHED_TO_ALLOCATE,OUT_FINISHED_TO_RETURN_ORDER,OUT_FINISHED_TO_BASE_PLATE_OUT,OUT_FINISHED_GENERATING_BILL,OUT_FINISHED_TO_WCS,OUT_FINISHED_TO_PICKING,outScanUpPurchaseCmp);',
       1, 1, '超级管理员', NOW(), 0, '000000' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `magic_chain` WHERE `chain_name` = 'outBoxScanChain' AND `del_flag` = 0);

-- ----------------------------------------------------------------------------
-- 5. sys_menu 菜单与按钮权限
--    5.1 参照「按单扫描入库」（1643，parent 1006 收货作业）、「出库打包校验」（1691，parent 1011 常规出库操作）
--    5.2 以 path 作为幂等键；menu_id 由自增分配
--    5.3 按钮权限在 sys_menu.perms 内声明，角色授权落 sys_role_auth(role_id=1)
--    5.4 非 000000 账套按套餐复制进 sys_tenant_menu，否则账套用户看不到菜单
-- ----------------------------------------------------------------------------

-- 5.1 内外箱扫描入库（挂 收货作业 1006）
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `component_name`, `query_param`,
                        `is_frame`, `is_cache`, `menu_type`, `visible`, `enable`, `perms`, `icon`,
                        `create_by`, `create_time`, `remark`, `create_by_name`)
SELECT '内外箱扫描入库', 1006, 24, '/inbound/scan/box-level-scan', '/inbound/scan/box-level-scan.vue', 'inbound-scan-box-level-scan', NULL,
       0, 1, 'F', 1, 1, 'browse=显示,scan=扫描,close=封箱,save=保存,cancel=撤销,print=打印', '#',
       1, NOW(), '外箱套内箱扫描收货', '超级管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/inbound/scan/box-level-scan');

-- 5.2 内外箱装箱出库（挂 常规出库操作 1011）
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `component_name`, `query_param`,
                        `is_frame`, `is_cache`, `menu_type`, `visible`, `enable`, `perms`, `icon`,
                        `create_by`, `create_time`, `remark`, `create_by_name`)
SELECT '内外箱装箱出库', 1011, 56, '/outbound/scan/box-pack-scan', '/outbound/scan/box-pack-scan.vue', 'outbound-scan-box-pack-scan', NULL,
       0, 1, 'F', 1, 1, 'browse=显示,scan=扫描,pack=装箱,verify=核验,save=保存,cancel=撤销,print=打印', '#',
       1, NOW(), '外箱绑定内箱装箱与发货核验', '超级管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/outbound/scan/box-pack-scan');

SET @menu_id_box_in = (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/inbound/scan/box-level-scan' LIMIT 1);
SET @menu_id_box_out = (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/outbound/scan/box-pack-scan' LIMIT 1);

-- 5.3 超级管理员角色（role_id = 1）按钮授权
INSERT INTO `sys_role_auth` (`role_id`, `user_id`, `menu_id`, `auth_value`, `create_by`, `create_time`, `create_by_name`, `tenant_id`)
SELECT 1, NULL, @menu_id_box_in,
       'browse=1,scan=1,close=1,save=1,cancel=1,print=1', 1, NOW(), '超级管理员', '000000' FROM DUAL
WHERE @menu_id_box_in IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_role_auth` WHERE `role_id` = 1 AND `menu_id` = @menu_id_box_in);

INSERT INTO `sys_role_auth` (`role_id`, `user_id`, `menu_id`, `auth_value`, `create_by`, `create_time`, `create_by_name`, `tenant_id`)
SELECT 1, NULL, @menu_id_box_out,
       'browse=1,scan=1,pack=1,verify=1,save=1,cancel=1,print=1', 1, NOW(), '超级管理员', '000000' FROM DUAL
WHERE @menu_id_box_out IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_role_auth` WHERE `role_id` = 1 AND `menu_id` = @menu_id_box_out);

-- 5.4 账套套餐同步：已含收货作业(1006)的套餐补入库菜单
INSERT INTO `sys_tenant_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `component_name`, `query_param`,
                               `is_frame`, `is_cache`, `menu_type`, `visible`, `enable`, `perms`, `icon`,
                               `create_by`, `create_time`, `create_by_name`, `package_id`, `package_name`)
SELECT m.`menu_id`, m.`menu_name`, m.`parent_id`, m.`order_num`, m.`path`, m.`component`, m.`component_name`, m.`query_param`,
       m.`is_frame`, m.`is_cache`, m.`menu_type`, m.`visible`, m.`enable`, m.`perms`, m.`icon`,
       1, NOW(), '超级管理员', t.`package_id`, t.`package_name`
FROM `sys_menu` m
JOIN (SELECT DISTINCT `package_id`, `package_name` FROM `sys_tenant_menu` WHERE `menu_id` = 1006) t
WHERE m.`path` = '/inbound/scan/box-level-scan'
  AND NOT EXISTS (SELECT 1 FROM `sys_tenant_menu` x WHERE x.`menu_id` = m.`menu_id` AND x.`package_id` = t.`package_id`);

-- 5.5 账套套餐同步：已含常规出库操作(1011)的套餐补出库菜单
INSERT INTO `sys_tenant_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `component_name`, `query_param`,
                               `is_frame`, `is_cache`, `menu_type`, `visible`, `enable`, `perms`, `icon`,
                               `create_by`, `create_time`, `create_by_name`, `package_id`, `package_name`)
SELECT m.`menu_id`, m.`menu_name`, m.`parent_id`, m.`order_num`, m.`path`, m.`component`, m.`component_name`, m.`query_param`,
       m.`is_frame`, m.`is_cache`, m.`menu_type`, m.`visible`, m.`enable`, m.`perms`, m.`icon`,
       1, NOW(), '超级管理员', t.`package_id`, t.`package_name`
FROM `sys_menu` m
JOIN (SELECT DISTINCT `package_id`, `package_name` FROM `sys_tenant_menu` WHERE `menu_id` = 1011) t
WHERE m.`path` = '/outbound/scan/box-pack-scan'
  AND NOT EXISTS (SELECT 1 FROM `sys_tenant_menu` x WHERE x.`menu_id` = m.`menu_id` AND x.`package_id` = t.`package_id`);

-- ----------------------------------------------------------------------------
-- 6. 商品箱规维护菜单（挂在「商品信息」1 下）
-- ----------------------------------------------------------------------------

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `component_name`, `query_param`,
                        `is_frame`, `is_cache`, `menu_type`, `visible`, `enable`, `perms`, `icon`,
                        `create_by`, `create_time`, `remark`, `create_by_name`)
SELECT '商品箱规', 1, 9, '/basic/product/boxSpec', '/basic/product/boxSpec.vue', 'basic-product-box-spec', NULL,
       0, 1, 'F', 1, 1, 'browse=显示,save=保存,delete=删除', '#',
       1, NOW(), '维护商品内箱装数、外箱内箱数与箱码模式', '超级管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/basic/product/boxSpec');

SET @menu_id_box_spec = (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/basic/product/boxSpec' LIMIT 1);

INSERT INTO `sys_role_auth` (`role_id`, `user_id`, `menu_id`, `auth_value`, `create_by`, `create_time`, `create_by_name`, `tenant_id`)
SELECT 1, NULL, @menu_id_box_spec,
       'browse=1,save=1,delete=1', 1, NOW(), '超级管理员', '000000' FROM DUAL
WHERE @menu_id_box_spec IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_role_auth` WHERE `role_id` = 1 AND `menu_id` = @menu_id_box_spec);

INSERT INTO `sys_tenant_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `component_name`, `query_param`,
                               `is_frame`, `is_cache`, `menu_type`, `visible`, `enable`, `perms`, `icon`,
                               `create_by`, `create_time`, `create_by_name`, `package_id`, `package_name`)
SELECT m.`menu_id`, m.`menu_name`, m.`parent_id`, m.`order_num`, m.`path`, m.`component`, m.`component_name`, m.`query_param`,
       m.`is_frame`, m.`is_cache`, m.`menu_type`, m.`visible`, m.`enable`, m.`perms`, m.`icon`,
       1, NOW(), '超级管理员', t.`package_id`, t.`package_name`
FROM `sys_menu` m
JOIN (SELECT DISTINCT `package_id`, `package_name` FROM `sys_tenant_menu` WHERE `menu_id` = 1) t
WHERE m.`path` = '/basic/product/boxSpec'
  AND NOT EXISTS (SELECT 1 FROM `sys_tenant_menu` x WHERE x.`menu_id` = m.`menu_id` AND x.`package_id` = t.`package_id`);

-- 系统管理员角色（role_id = 9）同步新菜单权限
INSERT INTO `sys_role_auth` (`role_id`, `user_id`, `menu_id`, `auth_value`, `create_by`, `create_time`, `create_by_name`, `tenant_id`)
SELECT 9, NULL, m.`menu_id`,
       CASE m.`path`
         WHEN '/basic/product/boxSpec' THEN 'browse=1,save=1,delete=1'
         WHEN '/inbound/scan/box-level-scan' THEN 'browse=1,scan=1,close=1,save=1,cancel=1,print=1'
         WHEN '/outbound/scan/box-pack-scan' THEN 'browse=1,scan=1,pack=1,verify=1,save=1,cancel=1,print=1'
       END,
       1, NOW(), '超级管理员', '000000'
FROM `sys_menu` m
WHERE m.`path` IN ('/basic/product/boxSpec', '/inbound/scan/box-level-scan', '/outbound/scan/box-pack-scan')
  AND NOT EXISTS (SELECT 1 FROM `sys_role_auth` a WHERE a.`role_id` = 9 AND a.`menu_id` = m.`menu_id`);
