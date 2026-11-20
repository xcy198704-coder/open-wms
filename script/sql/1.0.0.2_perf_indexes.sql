-- 性能优化：列表分页默认按主键倒序（见 ServiceImplPlus#pageList），
-- 多租户下 WHERE tenant_id = ? ORDER BY <pk> DESC LIMIT ? 会走全表/全索引扫描，
-- 补 (tenant_id, 主键) 复合索引后可降为 ref 访问。
--
-- 说明：单租户且数据量很小时优化器可能仍选择主键（成本相近），
-- 多租户或数据量上来后会使用该索引。

ALTER TABLE `out_order` ADD INDEX `idx_tenant_order` (`tenant_id`, `order_id`);
ALTER TABLE `core_inventory` ADD INDEX `idx_tenant_inventory` (`tenant_id`, `inventory_id`);
