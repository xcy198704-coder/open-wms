# TMX 物料同步 · 对拍核对与回滚运行手册（Task 6）

> 联调时照着本手册逐步跑、逐步记录。查询集在 `script/tmx_sync_verify.sql`（WMS 侧，
> 只读；回滚段默认注释在文件末尾）。
>
> 适用：Task 4（物料主数据同步）+ Task 5（分类映射）+ Task 8（箱规自动生成）的首次
> 全量同步联调。写于 2026-11-27（代码完成、联调未执行时）。

---

## 0. 前置条件（不满足不开工）

| # | 条件 | 检查方式 |
|---|---|---|
| 1 | TMX 侧接口已部署，服务账号可调 | `GET /api/wms/parts?limit=1` 带 Bearer 回 200；无 `materials.bom.views` 码时 bom-lines 会 403（预期，先只验 parts） |
| 2 | WMS 八+一键就绪 | `SELECT config_key,config_value FROM sys_config WHERE config_key LIKE 'tmx_%';`——`tmx_baseUrl`/令牌或账号口令非空、`tmx_syncEnable=1`（箱规联调时再开 `tmx_boxSpecEnable=1`） |
| 3 | 证书/网络通 | WMS 机（192.168.110.133）到 TMX（192.168.110.222:6002）HTTPS 通；自签证书已按现场方案信任 |
| 4 | 快照已存 | `tmx_sync_verify.sql` 第 0 段结果贴到核对记录（库存两值 + 商品数 + tmx_ 参数） |
| 5 | 回滚方案已读 | 本手册第 6 节；确认回滚段 SQL 无人误执行 |

**执行位置**：WMS 侧查询在 133 的 MySQL 客户端跑；TMX 侧查询由 TMX 维护方（或有
222 权限的人）跑，回贴结果。**WMS 不直连 TMX 库**（K10：禁跨模块直写，读也走对拍
双方各自执行、汇合结果的形式）。

---

## 1. 对拍流程

### Step 1 数量对拍

- TMX 侧：`SELECT COUNT(*) FROM parts;`
  （注意：TMX 的 parts 表**没有 is_deleted 列**，计数即全量；废止态在
  `product_state`，当前 DTO 未暴露，见第 5 节缺口 1）
- WMS 侧：`tmx_sync_verify.sql` 第 1 段 `wms_synced`

**判定**：`WMS = TMX − 虚拟件数 − 停用未建档数`。差集必须与同步作业日志里的例外
清单**逐条对上**：

- 虚拟件（TMX `part_attr=虚拟`，WMS 不建档案）——11-25 梳理的口径是 13 个，以本次
  作业日志「虚拟件不建商品档案」条数为准；
- 停用未建档（`approve_state=已驳回` 且 WMS 原来没有）——作业日志「TMX 停用料号，
  WMS 未建档，跳过」条数；
- 对不上的任何一条 = 不通过，先查作业日志再查数据，**不许调口径凑数**。

**预期正向差**：`T10110030`/`T10110031`（无 BOM 产品）——人工导入时未建，但 TMX
料号主档里有，同步应把它们补进 WMS。这两个数应体现在 `WMS` 里。

### Step 2 集合对拍

- WMS 侧：第 2 段导出 `product_code` 为 CSV（一列，排序）；
- TMX 侧：`SELECT part_no FROM parts ORDER BY part_no;` 导出 CSV；
- 求差（Git Bash）：`comm -3 <(sort -u wms.csv) <(sort -u tmx.csv)`
  - 只在 TMX 有的 = 应落在 Step 1 差集解释里（虚拟件/停用未建档）；
  - 只在 WMS 有的 = **必须为 0**（多出来的商品没有来源，是 bug）。

### Step 3 抽样字段全等

抽 20 个料号（IN 列表在 `tmx_sync_verify.sql` 第 3 段，联调时补满）：

- 分层：3 大类各抽 3-4 个；
- 边界必抽：1 个虚拟件（验证它**不**在 WMS）、1 个多段品名、1 个待分类、1 个停用态、
  `T10110001`/`T10210003`（箱规关联）、`T10110030`/`31`（补齐项）；
- 逐字段与 TMX `GET /api/wms/part?part_no=` 的返回比：`product_name`/`product_spec`/
  `small_unit`/类别（WMS `type_name` 对 TMX `material_category` 的映射，见
  `CategoryMapping`）/`product_model`（项目串 `internalCode(customerCode)`）。
- `product_desc` 应含「TMX同步 版本X 更新于…」。

**多段品名提示**：TMX 的 `name` 只是第一段（`name2`/`name3` 在后段，DTO 未带），
多段品名料号按第一段比即可（缺口 2）。

### Step 4 库存影响核对

同步后跑第 6 段，与第 0 段快照比：`inv_rows` 与 `inv_storage` **两值都必须一致**。
同步只动 `base_product`/`base_box_spec`/`sys_config`，不碰 `core_inventory`——
不一致就是有代码越界写了库存，停联调查代码。

### Step 5 增量与水位验证（本手册补充步，验 Task 4 的水位语义）

1. 记下当前水位（第 7 段）；
2. 在 TMX 改一个料号（改名或改规格），等几分钟；
3. 重跑同步作业；
4. 验证：该料号在 WMS 已更新；**其他料号的 `updated_at` 不变**（只动变化的）；
5. 水位应前进到「本次首次响应的 server_time − 5 分钟」附近；
6. 反验再跑一次：第二跑应只处理极少数行（水位之后的），作业摘要里「更新」数接近 0。

水位规则备忘：下次水位 = 本次**首次响应**的 `server_time` 留 5 分钟边际（服务端
时钟）；边际内的重复扫描由 `product_code` 幂等 upsert 消化。若发现丢行，先查是不是
有人用了客户端时钟。

### Step 6 箱规对拍（Task 8，`tmx_boxSpecEnable=1` 后）

- 第 5 段结果：5 条人工导入箱规应被**同名刷新**（数值不变），不是变成 10 条；
- 「重复箱规检查」查询必须为空；
- 作业日志例外清单：当前契约下多包装行产品（实测 16 个全部）会进例外——
  **这是预期行为**（无 `child_part_name` 不猜），不是失败；TMX 侧补了字段后重跑，
  例外应清空、箱规应落库。

---

## 2. 通过标准（全部满足才算联调通过）

1. 数量差集 = 例外清单，逐条可解释；
2. 集合差集中「只在 WMS 有」为 0；
3. 抽样 20 个字段全等（多段品名按第一段）；
4. 库存两值同步前后一致；
5. 增量只动变化料号、水位按规则前进、空跑接近 0 更新；
6. 箱规 5 条同名刷新、无重复、例外清单符合预期；
7. 作业日志无未解释异常。

---

## 3. 已知契约缺口（联调时盯着，别当 bug 报）

| # | 缺口 | 现状 | 联调时怎么看 |
|---|---|---|---|
| 1 | `PartBrief`/`PartDetail` 无 `product_state`/`is_deleted` | WMS 看不见废止/软删态，停用判定只认 `approve_state=已驳回` | 若发现废止料号被当活跃同步，**是预期的**（缺口未补）；记录清单，TMX 补字段后重跑修正 |
| 2 | `Part.Name` 只是品名第一段（name2/name3 未随 DTO 下发） | 多段品名料号同步后只有第一段 | 对拍按第一段；若业务要求全名，TMX DTO 补全名字段 |
| 3 | `BomLineRow` 无 `child_part_name` | 箱规多包装行无法自动分内外盒 | 见 Step 6；字段补上后 WMS 规则即通 |

---

## 4. 回滚演练（Step 5 回滚）

**场景 A：撤销一次同步、准备重跑**（保留分类，同步重建商品还要用）

```sql
-- 顺序来自 script/tmx_sync_verify.sql 回滚段 A1→A4（逐条取消注释执行）
DELETE FROM base_product WHERE create_by_name='系统导入' AND tenant_id='000000';
DELETE FROM base_box_spec WHERE create_by_name='系统导入';
DELETE FROM sys_config WHERE config_key='tmx_syncWatermark';
UPDATE sys_config SET config_value='0' WHERE config_key IN ('tmx_syncEnable','tmx_boxSpecEnable');
```

**场景 B：全量回退到 1.0.0.6 导入之前**（连分类一起撤，之后要走 1.0.0.6 重建）

A 的全部 + `DELETE FROM base_product_type WHERE type_code LIKE 'BOM-%';`

**回滚后验证**（回滚段末尾三条，应全为 0/空）：

- 商品数 0、箱规数 0、（B 时）分类数 0、tmx_ 参数只剩开关和连接参数；
- 重跑验证：走 1.0.0.6 重建 → 开 `tmx_syncEnable=1` → 手动跑一次同步 → 重走
  本手册 Step 1-4，结果应与首次一致（幂等性证明）。

**回滚前再存一次 0) 快照**——回滚本身也是对账，前后都要有数。

---

## 5. 核对记录模板（联调时逐项贴结果）

```
日期/执行人：
TMX 部署版本/commit：        WMS jar 构建号：
[ ] Step 0 快照：inv_rows=____ inv_storage=____ product_rows=____
[ ] Step 1 数量：TMX=____ WMS=____ 虚拟件=____ 停用未建档=____ → 对得上/对不上
[ ] Step 2 集合：只在 TMX 有=____（清单） 只在 WMS 有=____（必须 0）
[ ] Step 3 抽样：20/20 字段全等（例外：____）
[ ] Step 4 库存：前 (____,____) 后 (____,____) → 一致/不一致
[ ] Step 5 增量：变化料号已更新/未更新；水位 ____→____；空跑更新数=____
[ ] Step 6 箱规：5 条同名刷新/异常；重复箱规查询=空/非空；例外 ____ 条（符合预期？）
[ ] 回滚演练：场景 A/B，回滚后验证全 0，重跑结果与首次一致
结论：通过 / 不通过（不通过项 + 原因 + 处置）
```
