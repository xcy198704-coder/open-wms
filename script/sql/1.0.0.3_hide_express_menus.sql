-- 快递模块的页面（/tms/express/*）在开源版本中未提供，菜单仍存在于 sys_menu，
-- 点击后会进入空白页。这里先隐藏，待实现快递对接后再置回 visible = 1。
-- 恢复：UPDATE sys_menu SET visible = 1 WHERE path = '/tms/express' OR path LIKE '/tms/express/%';

UPDATE sys_menu
SET visible = 0
WHERE path = '/tms/express'
   OR path LIKE '/tms/express/%';
