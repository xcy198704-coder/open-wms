-- ============================================================
-- 1.0.0.7 TMX 平台对接 · 集成参数（WMS 侧）
--
-- 背景：WMS 定位为 TMX 平台的仓储作业执行端，主数据与账务库存归 TMX，
--       本侧只留「对接参数」。设计见 D:\易软通\TMX对接-分工与接入说明.md
--       与 D:\易软通\TMX对接实施计划.md（Task 3）。
--
-- 纪律：
--   * 全脚本幂等，可重复执行（与 1.0.0.4 同款写法）。
--   * 两个开关默认 0：升级后不改变现有任何流程；开关关闭时行为与今天逐字节一致。
--   * 共 8 个键：2 开关 + 3 连接参数 + 1 服务账号口令 + 1 水位。
--   * config_type = 'N' 表示非系统内置，允许在「系统参数」界面维护。
--   * **令牌值不进 Git**：本脚本只建空值，真实令牌由现场在系统参数界面填写
--     （或按站点策略改走环境变量，见 TmxIntegrationConfig 注释）。
--
-- 执行：
--   mysql -uroot -h127.0.0.1 --default-character-set=utf8mb4 yrt-open-wms < 1.0.0.7_tmx_integration_config.sql
-- ============================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1) 开关
-- ---------------------------------------------------------------------------

-- 物料主数据同步总开关（默认 0）
INSERT INTO `sys_config` (`tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `create_by_name`)
SELECT '000000', 'TMX 物料主数据同步开关（0=关 1=开）', 'tmx_syncEnable', '0', 'N', 1, NOW(), '超级管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'tmx_syncEnable');

-- 出入库单据回写开关（默认 0）
INSERT INTO `sys_config` (`tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `create_by_name`)
SELECT '000000', 'TMX 出入库单据回写开关（0=关 1=开）', 'tmx_writebackEnable', '0', 'N', 1, NOW(), '超级管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'tmx_writebackEnable');

-- ---------------------------------------------------------------------------
-- 2) 连接参数
-- ---------------------------------------------------------------------------

-- TMX 服务地址（生产 https://192.168.110.222:6002；默认空=未配置）
INSERT INTO `sys_config` (`tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `create_by_name`)
SELECT '000000', 'TMX 平台服务地址（如 https://192.168.110.222:6002）', 'tmx_baseUrl', '', 'N', 1, NOW(), '超级管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'tmx_baseUrl');

-- 服务账号令牌（仅短期联调用；脚本不填值，现场维护）
INSERT INTO `sys_config` (`tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `create_by_name`)
SELECT '000000', 'TMX 服务账号令牌（仅短期联调；长期请用账号口令自动续期）', 'tmx_token', '', 'N', 1, NOW(), '超级管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'tmx_token');

-- 服务账号口令（优先于 tmx_token）：平台 JWT 默认 7 天有效
-- （TMX config.yaml 的 auth.token_expiry=168 小时），静态令牌一周必失效；
-- 配了账号+口令，WMS 侧 TmxLoginTokenProvider 会自动登录换令牌并在临期前续期。
-- 口令值不进 Git，由现场在系统参数界面维护。
INSERT INTO `sys_config` (`tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `create_by_name`)
SELECT '000000', 'TMX 服务账号（仅授 materials 只读 + BOM 查看码）', 'tmx_username', '', 'N', 1, NOW(), '超级管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'tmx_username');

INSERT INTO `sys_config` (`tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `create_by_name`)
SELECT '000000', 'TMX 服务账号口令（现场填写，勿提交到 Git）', 'tmx_password', '', 'N', 1, NOW(), '超级管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'tmx_password');

-- 调用超时（毫秒，默认 5000）
INSERT INTO `sys_config` (`tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `create_by_name`)
SELECT '000000', 'TMX 接口调用超时（毫秒）', 'tmx_timeoutMs', '5000', 'N', 1, NOW(), '超级管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'tmx_timeoutMs');

-- ---------------------------------------------------------------------------
-- ---------------------------------------------------------------------------
-- 3) 同步水位（增量同步的断点，由作业自动维护，不要手改）
-- ---------------------------------------------------------------------------
-- ---------------------------------------------------------------------------

INSERT INTO `sys_config` (`tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `create_by_name`)
SELECT '000000', 'TMX 物料同步水位（作业自动维护，勿手改）', 'tmx_syncWatermark', '', 'N', 1, NOW(), '超级管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'tmx_syncWatermark');

-- ---------------------------------------------------------------------------
-- 4) 校验
-- ---------------------------------------------------------------------------

SELECT `config_key`, `config_name`, `config_value`
FROM `sys_config`
WHERE `config_key` IN ('tmx_syncEnable','tmx_writebackEnable','tmx_baseUrl','tmx_token','tmx_username',
                       'tmx_password','tmx_timeoutMs','tmx_syncWatermark')
ORDER BY `config_key`;
