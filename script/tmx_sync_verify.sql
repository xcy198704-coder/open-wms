-- ============================================================
-- TMX 同步对拍核对（Task 6）—— WMS 侧查询集
--
-- 配 script/tmx_sync_verify.md 运行手册使用。本文件全部为只读查询；
-- 回滚段在文件末尾、默认注释，确认要撤销时才逐条取消注释。
--
-- 执行：
--   mysql -uroot -h127.0.0.1 --default-character-set=utf8mb4 yrt-open-wms
--     < script/tmx_sync_verify.sql
-- 或按手册分步在客户端里跑（推荐：每步结果贴回核对记录）。
--
-- 口径提醒：
--   * 「系统导入」= 人工导入（1.0.0.6）与同步作业（Task 4）共同的创建人标记，
--     对拍集合以此为准；同步按 product_code upsert，不会因标记相同而插重。
--   * TMX 侧查询由 TMX 维护方执行（parts 表无 is_deleted 列，计数就是全量；
--     废止态在 product_state，当前 DTO 未暴露，见手册已知缺口）。
-- ============================================================

-- ------------------------------------------------------------
-- 0) 同步前快照（Step 4 库存影响核对 + 回滚基线，跑同步前必须先存）
-- ------------------------------------------------------------
SELECT COUNT(*) AS inv_rows, IFNULL(SUM(product_storage), 0) AS inv_storage FROM core_inventory;
SELECT COUNT(*) AS product_rows FROM base_product WHERE create_by_name='系统导入' AND del_flag=0;
SELECT config_key, config_value FROM sys_config WHERE config_key LIKE 'tmx_%' ORDER BY config_key;

-- ------------------------------------------------------------
-- 1) 数量对拍（Step 1）
--    WMS 侧读数；TMX 侧对应：SELECT COUNT(*) FROM parts;
--    期望差集 = 虚拟件（partAttr=虚拟，WMS 不建）+ 停用未建档（已驳回且 WMS 没有）
--    差集必须与作业日志的例外清单逐条对上，对不上就是问题。
-- ------------------------------------------------------------
SELECT COUNT(*) AS wms_synced FROM base_product WHERE create_by_name='系统导入' AND del_flag=0;

-- 被同步作业标记停用的商品（应为「TMX 停用且无库存」的那批）：
SELECT product_code, product_name, product_status
FROM base_product
WHERE product_status = 2 AND create_by_name='系统导入';

-- ------------------------------------------------------------
-- 2) 集合对拍（Step 2）
--    导出为 CSV，与 TMX 侧料号集合求差（见手册的 diff 方法）。
-- ------------------------------------------------------------
SELECT product_code
FROM base_product
WHERE create_by_name='系统导入' AND del_flag=0
ORDER BY product_code;

-- ------------------------------------------------------------
-- 3) 抽样字段全等（Step 3）
--    边界料号必抽（虚拟件/多段品名/待分类/停用态/无 BOM 补齐项）；
--    其余按大类分层抽满 20 个。IN 列表在联调时按实际抽样替换。
--    多段品名提示：TMX 的 name 只是第一段（name2/name3 在后段，DTO 未带），
--    这类料号按「第一段」比，全量一致性等 TMX 补字段（见手册已知缺口）。
-- ------------------------------------------------------------
SELECT product_code, product_name, product_spec, small_unit,
       type_name, big_type_name, product_model, product_desc
FROM base_product
WHERE product_code IN (
  'T10110001',  -- 成品·线缆组件（16pcs/CTN 箱规）
  'T10110030',  -- 无 BOM 产品：人工导入未建，同步应补齐
  'T10110031',  -- 无 BOM 产品：同上
  'T10210003',  -- 成品·连接器（1pcs/BOX×36 箱规）
  'T10510001-01', -- 包材·纸箱
  'T20010001'   -- 电缆
  -- …联调时补满 20 个（每大类 3-4 个 + 边界必抽项）
);

-- ------------------------------------------------------------
-- 4) 类别映射核对（Task 5 的落盘核验：13 类逐一映射，空/未知/未定 → TBD）
-- ------------------------------------------------------------
SELECT t.type_code, COUNT(*) AS product_count
FROM base_product p JOIN base_product_type t ON p.type_id = t.type_id
WHERE p.create_by_name='系统导入' AND p.del_flag=0
GROUP BY t.type_code
ORDER BY t.type_code;

-- 待分类清单（应只有研发待补类的料号；数量应等于作业日志的 WARN 条数）：
SELECT p.product_code, p.product_name
FROM base_product p JOIN base_product_type t ON p.type_id = t.type_id
WHERE p.create_by_name='系统导入' AND t.type_code = 'BOM-RM-TBD'
ORDER BY p.product_code;

-- ------------------------------------------------------------
-- 5) 箱规对拍（Task 8）
--    人工导入的 5 条应被同名刷新（不是变成 10 条）；例外清单料号不应落库。
-- ------------------------------------------------------------
SELECT p.product_code, s.spec_name, s.inner_qty, s.outer_inner_count,
       s.box_code_mode, s.remark
FROM base_box_spec s JOIN base_product p ON s.product_id = p.product_id
WHERE s.create_by_name='系统导入' AND s.del_flag=0
ORDER BY p.product_code, s.spec_name;

-- 重复箱规检查（同一商品多条箱规；结果必须为空）：
SELECT p.product_code, COUNT(*) AS spec_count
FROM base_box_spec s JOIN base_product p ON s.product_id = p.product_id
WHERE s.del_flag = 0
GROUP BY p.product_code
HAVING COUNT(*) > 1;

-- ------------------------------------------------------------
-- 6) 库存影响核对（Step 4：与 0) 快照比，两行必须一致——同步不得动库存）
-- ------------------------------------------------------------
SELECT COUNT(*) AS inv_rows, IFNULL(SUM(product_storage), 0) AS inv_storage FROM core_inventory;

-- ------------------------------------------------------------
-- 7) 水位（Step 5 增量验证后水位应前进；格式 yyyy-MM-dd HH:mm:ss）
-- ------------------------------------------------------------
SELECT config_key, config_value FROM sys_config WHERE config_key = 'tmx_syncWatermark';

-- ============================================================
-- 回滚段（默认注释！确认要撤销时才逐条取消注释，回滚前先存 0) 快照）
--
-- 两级回滚，按场景选：
--   A. 撤销一次同步、准备重跑（保留分类——同步重建商品时还要用）：
--      删商品 + 删箱规 + 清水位 + 开关归 0
--   B. 全量回退到 1.0.0.6 导入之前（连分类一起，需要时再走 1.0.0.6 重建）：
--      A 的全部 + 删分类
-- ============================================================

-- ---- A1) 删商品（系统导入标记 = 人工导入 + 同步作业共同标记，删即全撤） ----
-- DELETE FROM base_product WHERE create_by_name='系统导入' AND tenant_id='000000';

-- ---- A2) 删箱规（作业写入与人工导入同标记，一起撤） ----
-- DELETE FROM base_box_spec WHERE create_by_name='系统导入';

-- ---- A3) 清同步水位（不清则下次同步从旧水位继续，重跑不完整） ----
-- DELETE FROM sys_config WHERE config_key='tmx_syncWatermark';

-- ---- A4) 开关归 0（作业停跑；键保留，值清 0） ----
-- UPDATE sys_config SET config_value='0' WHERE config_key IN ('tmx_syncEnable','tmx_boxSpecEnable');

-- ---- B1) 全量回滚才执行：连分类一起删（执行前确认没有其他模块引用 BOM- 分类） ----
-- DELETE FROM base_product_type WHERE type_code LIKE 'BOM-%';

-- ---- 回滚后验证（应全部为 0/空，然后可重跑 1.0.0.6 + 开同步） ----
-- SELECT COUNT(*) FROM base_product WHERE create_by_name='系统导入';
-- SELECT COUNT(*) FROM base_box_spec WHERE create_by_name='系统导入';
-- SELECT COUNT(*) FROM base_product_type WHERE type_code LIKE 'BOM-%';
-- SELECT config_key, config_value FROM sys_config WHERE config_key LIKE 'tmx_%';
