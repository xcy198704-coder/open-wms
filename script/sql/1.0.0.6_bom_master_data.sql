-- ============================================================
-- 1.0.0.6 BOM 仓库目录主数据（商品分类 + 商品档案 + 箱规）
-- 数据源：C:\Users\lenovo\Desktop\数据表格\BOM汇总.xlsx（2026-09-11）
--         料号管控 + 工程BOM + 产品页签（37 个成品）
-- 生成：2026-11-25   幂等：重复执行不新增
-- 纪律：只新增，不删除/不修改既有商品、分类与库存
-- ============================================================
SET NAMES utf8mb4;

-- ---------- 1) 商品类别：3 大类 + 14 中类 ----------
INSERT INTO base_product_type (parent_id,type_name,root_name,node_level,full_type_id,full_type_name,type_code,enable,state,del_flag,tenant_id,create_time)
SELECT 0,'成品',NULL,0,'0','根','BOM-FG',1,'closed',0,'000000',NOW()
WHERE NOT EXISTS (SELECT 1 FROM base_product_type WHERE type_code='BOM-FG' AND del_flag=0);
INSERT INTO base_product_type (parent_id,type_name,root_name,node_level,full_type_id,full_type_name,type_code,enable,state,del_flag,tenant_id,create_time)
SELECT 0,'原材料',NULL,0,'0','根','BOM-RM',1,'closed',0,'000000',NOW()
WHERE NOT EXISTS (SELECT 1 FROM base_product_type WHERE type_code='BOM-RM' AND del_flag=0);
INSERT INTO base_product_type (parent_id,type_name,root_name,node_level,full_type_id,full_type_name,type_code,enable,state,del_flag,tenant_id,create_time)
SELECT 0,'包装材料',NULL,0,'0','根','BOM-PKG',1,'closed',0,'000000',NOW()
WHERE NOT EXISTS (SELECT 1 FROM base_product_type WHERE type_code='BOM-PKG' AND del_flag=0);

INSERT INTO base_product_type (parent_id,type_name,root_name,node_level,full_type_id,full_type_name,type_code,enable,state,del_flag,tenant_id,create_time)
SELECT r.type_id,'PCB',r.type_name,1,CONCAT('0,',r.type_id),CONCAT('根,',r.type_name,',','PCB'),'BOM-RM-PCB',1,'closed',0,'000000',NOW()
FROM base_product_type r WHERE r.type_code='BOM-RM' AND r.del_flag=0
AND NOT EXISTS (SELECT 1 FROM base_product_type t WHERE t.type_code='BOM-RM-PCB' AND t.del_flag=0);
INSERT INTO base_product_type (parent_id,type_name,root_name,node_level,full_type_id,full_type_name,type_code,enable,state,del_flag,tenant_id,create_time)
SELECT r.type_id,'POGO探针',r.type_name,1,CONCAT('0,',r.type_id),CONCAT('根,',r.type_name,',','POGO探针'),'BOM-RM-POGO',1,'closed',0,'000000',NOW()
FROM base_product_type r WHERE r.type_code='BOM-RM' AND r.del_flag=0
AND NOT EXISTS (SELECT 1 FROM base_product_type t WHERE t.type_code='BOM-RM-POGO' AND t.del_flag=0);
INSERT INTO base_product_type (parent_id,type_name,root_name,node_level,full_type_id,full_type_name,type_code,enable,state,del_flag,tenant_id,create_time)
SELECT r.type_id,'其他包材',r.type_name,1,CONCAT('0,',r.type_id),CONCAT('根,',r.type_name,',','其他包材'),'BOM-PK-OTHER',1,'closed',0,'000000',NOW()
FROM base_product_type r WHERE r.type_code='BOM-PKG' AND r.del_flag=0
AND NOT EXISTS (SELECT 1 FROM base_product_type t WHERE t.type_code='BOM-PK-OTHER' AND t.del_flag=0);
INSERT INTO base_product_type (parent_id,type_name,root_name,node_level,full_type_id,full_type_name,type_code,enable,state,del_flag,tenant_id,create_time)
SELECT r.type_id,'塑料件',r.type_name,1,CONCAT('0,',r.type_id),CONCAT('根,',r.type_name,',','塑料件'),'BOM-RM-PLASTIC',1,'closed',0,'000000',NOW()
FROM base_product_type r WHERE r.type_code='BOM-RM' AND r.del_flag=0
AND NOT EXISTS (SELECT 1 FROM base_product_type t WHERE t.type_code='BOM-RM-PLASTIC' AND t.del_flag=0);
INSERT INTO base_product_type (parent_id,type_name,root_name,node_level,full_type_id,full_type_name,type_code,enable,state,del_flag,tenant_id,create_time)
SELECT r.type_id,'外购连接器',r.type_name,1,CONCAT('0,',r.type_id),CONCAT('根,',r.type_name,',','外购连接器'),'BOM-RM-CONN',1,'closed',0,'000000',NOW()
FROM base_product_type r WHERE r.type_code='BOM-RM' AND r.del_flag=0
AND NOT EXISTS (SELECT 1 FROM base_product_type t WHERE t.type_code='BOM-RM-CONN' AND t.del_flag=0);
INSERT INTO base_product_type (parent_id,type_name,root_name,node_level,full_type_id,full_type_name,type_code,enable,state,del_flag,tenant_id,create_time)
SELECT r.type_id,'套管护套',r.type_name,1,CONCAT('0,',r.type_id),CONCAT('根,',r.type_name,',','套管护套'),'BOM-RM-SLEEVE',1,'closed',0,'000000',NOW()
FROM base_product_type r WHERE r.type_code='BOM-RM' AND r.del_flag=0
AND NOT EXISTS (SELECT 1 FROM base_product_type t WHERE t.type_code='BOM-RM-SLEEVE' AND t.del_flag=0);
INSERT INTO base_product_type (parent_id,type_name,root_name,node_level,full_type_id,full_type_name,type_code,enable,state,del_flag,tenant_id,create_time)
SELECT r.type_id,'待分类',r.type_name,1,CONCAT('0,',r.type_id),CONCAT('根,',r.type_name,',','待分类'),'BOM-RM-TBD',1,'closed',0,'000000',NOW()
FROM base_product_type r WHERE r.type_code='BOM-RM' AND r.del_flag=0
AND NOT EXISTS (SELECT 1 FROM base_product_type t WHERE t.type_code='BOM-RM-TBD' AND t.del_flag=0);
INSERT INTO base_product_type (parent_id,type_name,root_name,node_level,full_type_id,full_type_name,type_code,enable,state,del_flag,tenant_id,create_time)
SELECT r.type_id,'电缆',r.type_name,1,CONCAT('0,',r.type_id),CONCAT('根,',r.type_name,',','电缆'),'BOM-RM-CABLE',1,'closed',0,'000000',NOW()
FROM base_product_type r WHERE r.type_code='BOM-RM' AND r.del_flag=0
AND NOT EXISTS (SELECT 1 FROM base_product_type t WHERE t.type_code='BOM-RM-CABLE' AND t.del_flag=0);
INSERT INTO base_product_type (parent_id,type_name,root_name,node_level,full_type_id,full_type_name,type_code,enable,state,del_flag,tenant_id,create_time)
SELECT r.type_id,'端子',r.type_name,1,CONCAT('0,',r.type_id),CONCAT('根,',r.type_name,',','端子'),'BOM-RM-TERMINAL',1,'closed',0,'000000',NOW()
FROM base_product_type r WHERE r.type_code='BOM-RM' AND r.del_flag=0
AND NOT EXISTS (SELECT 1 FROM base_product_type t WHERE t.type_code='BOM-RM-TERMINAL' AND t.del_flag=0);
INSERT INTO base_product_type (parent_id,type_name,root_name,node_level,full_type_id,full_type_name,type_code,enable,state,del_flag,tenant_id,create_time)
SELECT r.type_id,'紧固件',r.type_name,1,CONCAT('0,',r.type_id),CONCAT('根,',r.type_name,',','紧固件'),'BOM-RM-FASTENER',1,'closed',0,'000000',NOW()
FROM base_product_type r WHERE r.type_code='BOM-RM' AND r.del_flag=0
AND NOT EXISTS (SELECT 1 FROM base_product_type t WHERE t.type_code='BOM-RM-FASTENER' AND t.del_flag=0);
INSERT INTO base_product_type (parent_id,type_name,root_name,node_level,full_type_id,full_type_name,type_code,enable,state,del_flag,tenant_id,create_time)
SELECT r.type_id,'纸箱纸盒',r.type_name,1,CONCAT('0,',r.type_id),CONCAT('根,',r.type_name,',','纸箱纸盒'),'BOM-PK-CARTON',1,'closed',0,'000000',NOW()
FROM base_product_type r WHERE r.type_code='BOM-PKG' AND r.del_flag=0
AND NOT EXISTS (SELECT 1 FROM base_product_type t WHERE t.type_code='BOM-PK-CARTON' AND t.del_flag=0);
INSERT INTO base_product_type (parent_id,type_name,root_name,node_level,full_type_id,full_type_name,type_code,enable,state,del_flag,tenant_id,create_time)
SELECT r.type_id,'线缆组件',r.type_name,1,CONCAT('0,',r.type_id),CONCAT('根,',r.type_name,',','线缆组件'),'BOM-FG-CABLE',1,'closed',0,'000000',NOW()
FROM base_product_type r WHERE r.type_code='BOM-FG' AND r.del_flag=0
AND NOT EXISTS (SELECT 1 FROM base_product_type t WHERE t.type_code='BOM-FG-CABLE' AND t.del_flag=0);
INSERT INTO base_product_type (parent_id,type_name,root_name,node_level,full_type_id,full_type_name,type_code,enable,state,del_flag,tenant_id,create_time)
SELECT r.type_id,'缓冲材',r.type_name,1,CONCAT('0,',r.type_id),CONCAT('根,',r.type_name,',','缓冲材'),'BOM-PK-CUSHION',1,'closed',0,'000000',NOW()
FROM base_product_type r WHERE r.type_code='BOM-PKG' AND r.del_flag=0
AND NOT EXISTS (SELECT 1 FROM base_product_type t WHERE t.type_code='BOM-PK-CUSHION' AND t.del_flag=0);
INSERT INTO base_product_type (parent_id,type_name,root_name,node_level,full_type_id,full_type_name,type_code,enable,state,del_flag,tenant_id,create_time)
SELECT r.type_id,'袋类',r.type_name,1,CONCAT('0,',r.type_id),CONCAT('根,',r.type_name,',','袋类'),'BOM-PK-BAG',1,'closed',0,'000000',NOW()
FROM base_product_type r WHERE r.type_code='BOM-PKG' AND r.del_flag=0
AND NOT EXISTS (SELECT 1 FROM base_product_type t WHERE t.type_code='BOM-PK-BAG' AND t.del_flag=0);
INSERT INTO base_product_type (parent_id,type_name,root_name,node_level,full_type_id,full_type_name,type_code,enable,state,del_flag,tenant_id,create_time)
SELECT r.type_id,'连接器成品',r.type_name,1,CONCAT('0,',r.type_id),CONCAT('根,',r.type_name,',','连接器成品'),'BOM-FG-CONN',1,'closed',0,'000000',NOW()
FROM base_product_type r WHERE r.type_code='BOM-FG' AND r.del_flag=0
AND NOT EXISTS (SELECT 1 FROM base_product_type t WHERE t.type_code='BOM-FG-CONN' AND t.del_flag=0);
INSERT INTO base_product_type (parent_id,type_name,root_name,node_level,full_type_id,full_type_name,type_code,enable,state,del_flag,tenant_id,create_time)
SELECT r.type_id,'金属件',r.type_name,1,CONCAT('0,',r.type_id),CONCAT('根,',r.type_name,',','金属件'),'BOM-RM-METAL',1,'closed',0,'000000',NOW()
FROM base_product_type r WHERE r.type_code='BOM-RM' AND r.del_flag=0
AND NOT EXISTS (SELECT 1 FROM base_product_type t WHERE t.type_code='BOM-RM-METAL' AND t.del_flag=0);

-- ---------- 2) 临时表装载商品主数据 ----------
DROP TEMPORARY TABLE IF EXISTS tmp_bom_product;
CREATE TEMPORARY TABLE tmp_bom_product (
  product_code varchar(50) NOT NULL PRIMARY KEY, product_name varchar(300) NOT NULL,
  product_model varchar(200) DEFAULT '', type_code varchar(50) NOT NULL,
  unit varchar(20) DEFAULT 'pcs', big_unit varchar(20) DEFAULT NULL, unit_convert decimal(18,0) DEFAULT NULL,
  provider int DEFAULT NULL, role varchar(20) DEFAULT '物料', tier varchar(20) DEFAULT '已确认',
  ver varchar(20) DEFAULT '', bom_prods varchar(500) DEFAULT '', src varchar(200) DEFAULT ''
) DEFAULT CHARSET=utf8mb4;
INSERT INTO tmp_bom_product (product_code,product_name,product_model,type_code,unit,big_unit,unit_convert,provider,role,tier,ver,bom_prods,src) VALUES
('T10110001','线缆组件4116-1550-600','PG-P7（PM256）','BOM-FG-CABLE','pcs','箱',16,NULL,'成品','已确认','A0','','料号管控、xlsm、产品页签、工程BOM'),
('T10110002','线缆组件1400-2120-600','BY-ML(FT)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','待确认','A0','','xlsm、产品页签、工程BOM'),
('T10110003','线缆组件1400-2120-700','BY-ML(FT)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','已确认','A0','','料号管控、xlsm、产品页签、工程BOM'),
('T10110004','线缆组件1400-2120-500','BY-ML(FT)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','已确认','A0','','料号管控、xlsm、产品页签、工程BOM'),
('T10110005','线缆组件1400-5120-800','BY-ML(FT)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','已确认','A0','','料号管控、xlsm、产品页签、工程BOM'),
('T10110006','线缆组件1400-5120-1000','BY-ML(FT)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','已确认','A0','','料号管控、xlsm、产品页签、工程BOM'),
('T10110007','线缆组件1120-1120-1000','BY-ML(FT)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','已确认','A0','','料号管控、xlsm、产品页签、工程BOM'),
('T10110008','线缆组件2256-2400-500/3120-500(MAP1)','SZ-MS(HH)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','已确认','A0','','料号管控、xlsm、产品页签、工程BOM'),
('T10110009','线缆组件2256-2400-500/1120-800(MAP2)','SZ-MS(HH)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','已确认','A0','','料号管控、xlsm、产品页签、工程BOM'),
('T10110010','线缆组件2256-2400-800/1120-800/1120-250(MAP3)','SZ-MS(HH)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','已确认','A0','','料号管控、xlsm、产品页签、工程BOM'),
('T10110011','线缆组件1400-1120-800(MAP4)','SZ-MS(HH)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','已确认','A0','','料号管控、xlsm、产品页签、工程BOM'),
('T10110012','线缆组件1256-1256-500(MAP5)','SZ-MS(HH)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','已确认','A0','','料号管控、xlsm、产品页签、工程BOM'),
('T10110013','线缆组件1256-160-77(LNP电源）','JN-CD(DJ-V200)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','已确认','A1','','料号管控、xlsm、产品页签、工程BOM'),
('T10110014','线缆组件1444-2256-600（PM128）','JN-CD(DJ-V200)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','已确认','A0','','料号管控、xlsm、产品页签、工程BOM'),
('T10110015','线缆组件1444-1256-600（DIO256)','JN-CD(DJ-V200)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','已确认','A0','','料号管控、xlsm、产品页签、工程BOM'),
('T10110016','线缆组件3444-4256-650-1000（DTI)','JN-CD(DJ-V200)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','已确认','A0','','料号管控、xlsm、产品页签、工程BOM'),
('T10110017','线缆组件1444-810-650（LNP通讯)-暂停','JN-CD(DJ-V200)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','已确认','A0','','料号管控、xlsm、产品页签、工程BOM'),
('T10110018','线缆组件1444-2256-600-B（VI64）','JN-CD(DJ-V200)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','待确认','A1','','xlsm、产品页签'),
('T10110019','线缆组件1256-1256-550(MAP5-H)','SZ-MS(HH)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','已确认','A0','','料号管控、xlsm、产品页签、工程BOM'),
('T10110020','线缆组件1500-4116-600JR(PM256-XD-JR)','PG-P7-JR','BOM-FG-CABLE','pcs','箱',16,NULL,'成品','已确认','A0','','料号管控、xlsm、产品页签、工程BOM'),
('T10110021','线缆组件CA-2444-2100-400（UPM32）','SY-A32','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','已确认','A0','','料号管控、产品页签、工程BOM'),
('T10110022','线缆组件4022-1098-600（XPM32）','SY-P32','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','待确认','A0','T10110032/T10110033/T10110034','产品页签'),
('T10110023','线缆组件1444-468-600(PM128-STD)','SY-P4-STD','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','已确认','A0','','料号管控、产品页签、工程BOM'),
('T10110024','线缆组件1444-268-600(PM128-PD)','SY-P4-PD','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','已确认','A0','','料号管控、产品页签、工程BOM'),
('T10110025','线缆组件1550-868-600(PM256-STD)','SY-P7-STD','BOM-FG-CABLE','pcs','箱',16,NULL,'成品','已确认','A0','','料号管控、产品页签、工程BOM'),
('T10110026','线缆组件1272-835-240-300','SY-D7P-HD＆STD(DIO256plus HD＆STD)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','待确认','A0','','产品页签'),
('T10110027','线缆组件1272-835-390-300','SY-D7P-HD＆STD(DIO256plus HD＆STD)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','待确认','A0','','产品页签'),
('T10110028','线缆组件1272-468-600','SY-D7P-HD＆STD(DIO256plus HD＆STD)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','待确认','A0','','产品页签'),
('T10110029','线缆组件1272-868-600','SY-D7P-HD＆STD(DIO256plus HD＆STD)','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','待确认','A0','','产品页签'),
('T10110032','线缆组件4022-1098-600-B（XPM32）','SY-P32','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','待确认','','','产品页签'),
('T10110033','线缆组件4022-1098-450（XPM32）','SY-P32','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','待确认','','','产品页签'),
('T10110034','线缆组件4022-1098-450-B（XPM32）','SY-P32','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','待确认','','','产品页签'),
('T10110035','线缆组件1116-1116-200','ZJ_DIAG','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','待确认','A0','','产品页签'),
('T10110036','线缆组件1272-2116-600','CJ-SY-VHD','BOM-FG-CABLE','pcs',NULL,NULL,NULL,'成品','待确认','A0','','产品页签'),
('T10210003','P-550SRA-45插座连接器（单独产品）','BY-ML(FT)','BOM-FG-CONN','pcs','箱',36,NULL,'成品','已确认','A1','','料号管控、xlsm、产品页签、工程BOM'),
('T10210016','P-550SRA-45-JR插座连接器（单独产品）','JN-CD(DJ-V200)','BOM-FG-CONN','pcs','箱',36,NULL,'成品','已确认','A0','','料号管控、xlsm、产品页签、工程BOM'),
('T10210032','P-098TRA-43插座连接器（XPM32）','SY-P32','BOM-FG-CONN','pcs',NULL,NULL,NULL,'成品','待确认','A0','','产品页签'),
('T10110013-01','LNP-PCB','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110013','料号管控、产品页签、工程BOM'),
('T10210001-02','POGO连接器P256-PG116S：PCB-A','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A3','T10110001/T10110020','料号管控、xlsm、产品页签、工程BOM'),
('T10210001-02-B','POGO连接器P256-PG116S：PCB-A','','BOM-RM-PCB','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110035','产品页签'),
('T10210001-03','POGO连接器P256-PG116S：PCB-B/E','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A3','T10110001/T10110020/T10110035','料号管控、xlsm、产品页签、工程BOM'),
('T10210001-04','POGO连接器P256-PG116S：PCB-C/F','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A3','T10110001/T10110020/T10110035','料号管控、xlsm、产品页签、工程BOM'),
('T10210001-05','POGO连接器P256-PG116S：PCB-D/G','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A3','T10110001/T10110020','料号管控、xlsm、产品页签、工程BOM'),
('T10210002-02','线缆连接器P256-J550S:PCB1(M,N)','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A2','T10110001','料号管控、xlsm、产品页签、工程BOM'),
('T10210002-03','线缆连接器P256-J550S:PCB2(K,L)','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A2','T10110001','料号管控、xlsm、产品页签、工程BOM'),
('T10210002-04','线缆连接器P256-J550S:PCB3(H,J)','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A2','T10110001','料号管控、xlsm、产品页签、工程BOM'),
('T10210002-05','线缆连接器P256-J550S:PCB4(F,G)','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A2','T10110001','料号管控、xlsm、产品页签、工程BOM'),
('T10210002-06','线缆连接器P256-J550S:PCB5(D,E)','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A2','T10110001','料号管控、xlsm、产品页签、工程BOM'),
('T10210002-07','线缆连接器P256-J550S:PCB6(B,C)','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A2','T10110001','料号管控、xlsm、产品页签、工程BOM'),
('T10210002-08','线缆连接器P256-J550S:PCB7(A)','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A2','T10110001','料号管控、xlsm、产品页签、工程BOM'),
('T10210004-05','Memory-HP288-400S：J01-PCB1-A/B','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A2','T10110005','料号管控、xlsm、产品页签、工程BOM'),
('T10210004-06','Memory-HP288-400S：J01-PCB5-J/K','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A2','T10110005','料号管控、xlsm、产品页签、工程BOM'),
('T10210004-07','Memory-HP288-400S：J01-PCB1-A/B-1','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A3','T10110002/T10110003/T10110004/T10110006','料号管控、xlsm、产品页签、工程BOM'),
('T10210004-08','Memory-HP288-400S：JA-PCB1-A/B-1','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110011','料号管控、xlsm、产品页签、工程BOM'),
('T10210004-09','Memory-HP288-400S：JA-PCB1-A/B','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A1','T10110008/T10110009','料号管控、xlsm、产品页签、工程BOM'),
('T10210004-10','Memory-HP288-400S：JA-PCB2-C/D','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A1','T10110008/T10110009','料号管控、xlsm、产品页签、工程BOM'),
('T10210004-11','Memory-HP288-400S：JA-PCB3-E/F','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A1','T10110008/T10110009','料号管控、xlsm、产品页签、工程BOM'),
('T10210004-12','Memory-HP288-400S：JA-PCB4-G/H','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A1','T10110008/T10110009','料号管控、xlsm、产品页签、工程BOM'),
('T10210004-13','Memory-HP288-400S：JA-PCB2-A/B','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A1','T10110010','料号管控、xlsm、产品页签、工程BOM'),
('T10210004-14','Memory-HP288-400S：JA-PCB5-J/K','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A1','T10110010','料号管控、xlsm、产品页签、工程BOM'),
('T10210005-05','Memory-HP288-120S：J02-PCB1-A/B','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A2','T10110005','料号管控、xlsm、产品页签、工程BOM'),
('T10210005-06','Memory-HP288-120S：J06-PCB2-C/D','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A2','T10110005','料号管控、xlsm、产品页签、工程BOM'),
('T10210005-07','Memory-HP288-120S：J02-PCB1-A/B-1','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A3','T10110002/T10110003/T10110004/T10110006/T10110007','料号管控、xlsm、产品页签、工程BOM'),
('T10210005-08','Memory-HP288-120S：JB-PCB1-A/B-1','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110011','料号管控、xlsm、产品页签、工程BOM'),
('T10210005-09','Memory-HP288-120S：JC/D-PCB1-A/B','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A1','T10110008/T10110010','料号管控、xlsm、产品页签、工程BOM'),
('T10210005-10','Memory-HP288-120S：JC/D-PCB2-C/D','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A1','T10110008','料号管控、xlsm、产品页签、工程BOM'),
('T10210005-11','Memory-HP288-120S：JC/D-PCB1-E/F','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A1','T10110008','料号管控、xlsm、产品页签、工程BOM'),
('T10210005-12','Memory-HP288-120S：JC/D-PCB2-G/H','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A1','T10110008','料号管控、xlsm、产品页签、工程BOM'),
('T10210005-13','Memory-HP288-120S：JC-PCB1-A/B-2','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A1','T10110009/T10110010','料号管控、xlsm、产品页签、工程BOM'),
('T10210005-14','Memory-HP288-120S：J01-PCB1-A/B-1','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A1','T10110007','料号管控、产品页签、工程BOM'),
('T10210007-03','JN-CD-P-J444T：PCB-PM128','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110014/T10110017/T10110018/T10110023/T10110024','料号管控、xlsm、产品页签、工程BOM'),
('T10210007-04','JN-CD-P-J444T：PCB1','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110014/T10110017/T10110018/T10110023/T10110024','料号管控、xlsm、产品页签、工程BOM'),
('T10210007-05','JN-CD-P-J444T：PCB2','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110014/T10110017/T10110018/T10110023/T10110024','料号管控、xlsm、产品页签、工程BOM'),
('T10210007-06','JN-CD-P-J444T：PCB3','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110014/T10110017/T10110018/T10110023/T10110024','料号管控、xlsm、产品页签、工程BOM'),
('T10210009-01','JN-CD-P-J444T：PCB-DIO256','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110015','料号管控、xlsm、产品页签、工程BOM'),
('T10210009-02','JN-CD-P-J444T：PCB1','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110015','料号管控、xlsm、产品页签、工程BOM'),
('T10210010-01','JN-CD-P-J444T：PCB-DTI-L','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110016','料号管控、xlsm、产品页签、工程BOM'),
('T10210010-02','JN-CD-P-J444T：PCB1','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110016','料号管控、xlsm、产品页签、工程BOM'),
('T10210010-03','JN-CD-P-J444T：PCB2','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110016','料号管控、xlsm、产品页签、工程BOM'),
('T10210010-04','JN-CD-P-J444T：PCB3','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110016','料号管控、xlsm、产品页签、工程BOM'),
('T10210010-05','JN-CD-P-J444T：PCB4','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110016','料号管控、xlsm、产品页签、工程BOM'),
('T10210010-06','JN-CD-P-J444T：PCB5','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110016','料号管控、xlsm、产品页签、工程BOM'),
('T10210011-01','JN-CD-P-J444T：PCB-DTI-R','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110016','料号管控、xlsm、产品页签、工程BOM'),
('T10210011-04','JN-CD-P-J444T：PCB3','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110016','料号管控、xlsm、产品页签、工程BOM'),
('T10210011-05','JN-CD-P-J444T：PCB4','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A1','T10110016','料号管控、xlsm、产品页签、工程BOM'),
('T10210011-06','JN-CD-P-J444T：PCB5','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110016','料号管控、xlsm、产品页签、工程BOM'),
('T10210011-07','JN-CD-P-J444T：PCB6','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110016','料号管控、xlsm、产品页签、工程BOM'),
('T10210011-08','JN-CD-P-J444T：PCB7','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110016','料号管控、xlsm、产品页签、工程BOM'),
('T10210012-01','JN-CD-P-J444T：PCB-DTI-C','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110016','料号管控、xlsm、产品页签、工程BOM'),
('T10210012-02','JN-CD-P-J444T：PCB1','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110016','料号管控、xlsm、产品页签、工程BOM'),
('T10210012-03','JN-CD-P-J444T：PCB2','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110016','料号管控、xlsm、产品页签、工程BOM'),
('T10210013-01','JN-CD-P-J444T：PCB-LNP','','BOM-RM-PCB','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210013-02','JN-CD-P-J444T：PCB1','','BOM-RM-PCB','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210013-03','JN-CD-P-J444T：PCB2','','BOM-RM-PCB','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210013-04','JN-CD-P-J444T：PCB3','','BOM-RM-PCB','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210013-05','JN-CD-P-J444T：PCB4','','BOM-RM-PCB','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210013-14','JN-CD-P-J444T：PCB13','','BOM-RM-PCB','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210019-02','SY-A32-P-J100S:PCB','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110021','料号管控、产品页签、工程BOM'),
('T10210020-01','SY-A32-P-J444T:PCB-大','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110021','料号管控、产品页签、工程BOM'),
('T10210020-02','SY-A32-P-J444T:PCB-小','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110021','料号管控、产品页签、工程BOM'),
('T10210021-02','SS-P4-PG68S:PCB-A','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110023/T10110024/T10110025','料号管控、工程BOM'),
('T10210021-03','SS-P4-PG68S:PCB-B','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110023/T10110024/T10110025','料号管控、工程BOM'),
('T10210021-04','SS-P4-PG68S:PCB-C','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110023/T10110024/T10110025','料号管控、工程BOM'),
('T10210021-05','SS-P4-PG68S:PCB-D','','BOM-RM-PCB','pcs',NULL,NULL,926,'物料','已确认','A0','T10110023/T10110024/T10110025','料号管控、工程BOM'),
('T10210025-02','SY-D7P-H-J272S-B：PCB','','BOM-RM-PCB','pcs',NULL,NULL,NULL,'物料','待确认','A1','T10110028/T10110029/T10110036','产品页签'),
('T10210026-02','SY-D7P-PG68S-A：PCB-A＆B','','BOM-RM-PCB','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110028/T10110029','产品页签'),
('T10210026-03','SY-D7P-PG68S-A：PCB-C＆D','','BOM-RM-PCB','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110028/T10110029','产品页签'),
('T10210026-06','SS-P4-PG68S:PCB-A&B','','BOM-RM-PCB','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110023','产品页签'),
('T10210026-07','SS-P4-PG68S:PCB-C&D','','BOM-RM-PCB','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110023','产品页签'),
('T10210026-08','SS-P4-PG68S:PCB-A&B','','BOM-RM-PCB','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110024','产品页签'),
('T10210026-09','SS-P4-PG68S:PCB-C&D','','BOM-RM-PCB','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110024','产品页签'),
('T10210026-10','SY-PG-P7-PG68S:PCB-A&B','','BOM-RM-PCB','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110025','产品页签'),
('T10210026-11','SY-PG-P7-PG68S:PCB-C&D','','BOM-RM-PCB','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110025','产品页签'),
('T10210036-01','SY-VHD-PG116S：PCB-A','','BOM-RM-PCB','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110036','产品页签'),
('T10210036-02','SY-VHD--PG116S：PCB-B/E','','BOM-RM-PCB','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110036','产品页签'),
('T10210036-03','SY-VHD--PG116S：PCB-C/F','','BOM-RM-PCB','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110036','产品页签'),
('T10210036-04','SY-VHD--PG116S：PCB-D/G','','BOM-RM-PCB','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110036','产品页签'),
('T10210021','POGO连接器组件：PG68S-BLOCK','SY-A32','BOM-RM-POGO','pcs',NULL,NULL,NULL,'物料','待确认','/','T10110023','产品页签'),
('T10210022','SY-P32-PG022S线缆连接器','SY-P32','BOM-RM-POGO','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110024/T10110032/T10110033/T10110034','产品页签'),
('T10210026','POGO连接器SY-D7P-PG68S-A','SY-D7P-HD＆STD(DIO256plus HD＆STD)','BOM-RM-POGO','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110028/T10110029','产品页签'),
('T10210027','POGO连接器TP-HS-HD-PG68S-B','SY-D7P-HD＆STD(DIO256plus HD＆STD)','BOM-RM-POGO','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110028/T10110029','产品页签'),
('T10210030','POGO连接器组件：PG68S-BLOCK-B','SY-D7M-5G(DIO-MAX5G)','BOM-RM-POGO','pcs',NULL,NULL,NULL,'物料','待确认','/','T10110025','产品页签'),
('T10210036','SY-VHD-PG116S:POGO连接器组件','ZJ_DIAG','BOM-RM-POGO','pcs',NULL,NULL,NULL,'物料','待确认','','T10110036','产品页签'),
('T10330001','单头探针PG-120143-C001','POGO','BOM-RM-POGO','pcs',NULL,NULL,926,'物料','已确认','A0','T10110001/T10110020/T10110022/T10110023/T10110024/T10110025/T10110028/T10110029/T10110032/T10110033/T10110034/T10110035/T10110036','料号管控、xlsm、产品页签、工程BOM'),
('T10330003','PG-110223-T002','POGO','BOM-RM-POGO','pcs',NULL,NULL,926,'物料','已确认','A0','T10110021','料号管控、产品页签、工程BOM'),
('T10330004','PG-110223-M001','POGO','BOM-RM-POGO','pcs',NULL,NULL,926,'物料','已确认','A0','T10110021','料号管控、产品页签、工程BOM'),
('T10330005','PG-090146-T003双头探针','POGO','BOM-RM-POGO','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T20060001','陶瓷型PT1000温度传感器','','BOM-RM-POGO','PCS',NULL,NULL,NULL,'物料','待确认','/','T10110010','产品页签'),
('T10510001-01','纸箱 材质：BC/F ECT=8000N规格：685*415*385mm；16pcs/CTN','','BOM-PK-CARTON','pcs',NULL,NULL,926,'物料','已确认','A2','T10110001/T10210016','料号管控、xlsm、产品页签、工程BOM'),
('T10510001-02','缓冲垫  材质：25kg/m3 白色防静电EPE 规格：680*410*95mm','','BOM-PK-CUSHION','pcs',NULL,NULL,926,'物料','已确认','A2','T10110001/T10210016','料号管控、xlsm、产品页签、工程BOM'),
('T10510001-03','缓冲垫  材质：25kg/m3 白色防静电EPE 规格：545*410*25mm','','BOM-PK-CUSHION','pcs',NULL,NULL,926,'物料','已确认','A2','T10110001/T10210016','料号管控、xlsm、产品页签、工程BOM'),
('T10510001-04','透明尼龙袋900*580mm','','BOM-PK-BAG','pcs',NULL,NULL,926,'物料','已确认','A2','T10110001/T10210016','料号管控、xlsm、产品页签、工程BOM'),
('T10510002-01','纸盒  材质：350g白卡 规格：110*90*57mm;1pcs/BOX','','BOM-PK-CARTON','pcs',NULL,NULL,926,'物料','已确认','A0','T10110020/T10110025/T10210003','料号管控、xlsm、产品页签、工程BOM'),
('T10510002-02','纸箱 材质：BC/F ECT=8000N 规格：465*290*190mm；36pcs/CTN','','BOM-PK-CARTON','pcs',NULL,NULL,926,'物料','已确认','A0','T10110020/T10110025/T10210003','料号管控、xlsm、产品页签、工程BOM'),
('T10510002-03','22kg/m3 白色EPE 规格：110*90*45mm','','BOM-PK-CUSHION','pcs',NULL,NULL,926,'物料','已确认','A0','T10110020/T10110025/T10210003','料号管控、xlsm、产品页签、工程BOM'),
('T10510002-04','22kg/m3 白色EPE 规格：110*90*10mm','','BOM-PK-CUSHION','pcs',NULL,NULL,926,'物料','已确认','A0','T10110020/T10110025/T10210003','料号管控、xlsm、产品页签、工程BOM'),
('T10510003-01','4层纸箱，材质BC/F=8000N,规格96X455X385','','BOM-PK-CARTON','pcs',NULL,NULL,926,'物料','已确认','A0','T10110002/T10110003/T10110004/T10110005/T10110006/T10110007/T10110008/T10110009/T10110010/T10110011/T10110012','料号管控、产品页签、工程BOM'),
('T10510003-02','尾箱2层纸箱，材质BC/F=8000N,规格96X455X205','','BOM-PK-CARTON','pcs',NULL,NULL,926,'物料','已确认','A0','T10110002/T10110003/T10110004/T10110005/T10110006/T10110007/T10110008/T10110009/T10110010/T10110011/T10110012','料号管控、产品页签、工程BOM'),
('T10510003-03','尾箱1层纸箱，材质BC/F=8000N,规格96X455X115','','BOM-PK-CARTON','pcs',NULL,NULL,926,'物料','已确认','A0','T10110002/T10110003/T10110004/T10110005/T10110006/T10110007/T10110008/T10110009/T10110010/T10110011/T10110012','料号管控、产品页签、工程BOM'),
('T10510003-04','缓冲隔层，EPE BUFFER25KG/M³White 规格960X450X90','','BOM-PK-CUSHION','pcs',NULL,NULL,926,'物料','已确认','A0','T10110005/T10110006','料号管控、产品页签、工程BOM'),
('T10510003-04-01','缓冲隔层，EPE BUFFER25KG/M³White 规格960X450X90','','BOM-PK-CUSHION','pcs',NULL,NULL,926,'物料','已确认','A0','T10110002/T10110003/T10110004','料号管控、产品页签、工程BOM'),
('T10510003-04-02','缓冲隔层，EPE BUFFER25KG/M³White 规格960X450X90','','BOM-PK-CUSHION','pcs',NULL,NULL,926,'物料','已确认','A0','T10110007/T10110008/T10110009/T10110010/T10110011/T10110012','料号管控、产品页签、工程BOM'),
('T10510003-05','缓冲盖，EPE BUFFER盖25KG/M³White 规格960X450X20','','BOM-PK-CUSHION','pcs',NULL,NULL,926,'物料','已确认','A0','T10110002/T10110003/T10110004/T10110005/T10110006/T10110007/T10110008/T10110009/T10110010/T10110011/T10110012','料号管控、产品页签、工程BOM'),
('T10510003-06','尼龙真空袋，Bag 规格1150X600','','BOM-PK-BAG','pcs',NULL,NULL,926,'物料','已确认','A0','T10110002/T10110003/T10110004/T10110005/T10110006/T10110007/T10110008/T10110009/T10110010/T10110011/T10110012','料号管控、产品页签、工程BOM'),
('T10510003-07','气泡袋：bubble bag LDPE 规格80X90(100)','','BOM-PK-CUSHION','pcs',NULL,NULL,926,'物料','已确认','A0','T10110002/T10110003/T10110004/T10110005/T10110006/T10110007/T10110008/T10110009/T10110010/T10110011/T10110012','料号管控、产品页签、工程BOM'),
('T10510003-08','橡皮筋：Bubber band 规格38X1.4X1.4','','BOM-PK-BAG','pcs',NULL,NULL,926,'物料','已确认','A0','T10110002/T10110003/T10110004/T10110005/T10110006/T10110007/T10110008/T10110009/T10110010/T10110011/T10110012','料号管控、产品页签、工程BOM'),
('T20030001','4/6聚酯型UPU管（亚德客）US98A060040)','','BOM-PK-OTHER','m',NULL,NULL,NULL,'物料','待确认','/','T10110009/T10110010','产品页签'),
('T20040001','纺织自卷管φ3mn-黑色','','BOM-PK-OTHER','pcs',NULL,NULL,926,'物料','已确认','A0','T10110010','料号管控、产品页签'),
('T20040003','纺织自卷管13mmn-黑色','','BOM-PK-OTHER','pcs',NULL,NULL,926,'物料','已确认','A0','T10110011/T10110025','料号管控、产品页签'),
('T20040004','纺织自卷管φ16mn-黑色','','BOM-PK-OTHER','pcs',NULL,NULL,926,'物料','已确认','A0','T10110009/T10110010/T10110012','料号管控、产品页签'),
('T20040005','纺织自卷管φ16mn-橙色','','BOM-PK-OTHER','pcs',NULL,NULL,926,'物料','已确认','A0','T10110019','料号管控、产品页签'),
('T2004003','纺织自卷管φ13mm(黑色）','','BOM-PK-OTHER','m',NULL,NULL,NULL,'物料','待确认','/','T10110036','产品页签'),
('T10210001-01','POGO连接器P256-PG116S：基座合件','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A4','T10110001/T10110020/T10110035/T10110036','料号管控、xlsm、产品页签、工程BOM'),
('T10210001-09','POGO连接器P256-PG116S：防尘盖','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A0','T10110001/T10110020/T10110021/T10110023/T10110024/T10110025/T10110028/T10110029/T10110035/T10110036','料号管控、xlsm、产品页签、工程BOM'),
('T10210002-01','线缆连接器P256-J550S:基座','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A2','T10110001','料号管控、xlsm、产品页签、工程BOM'),
('T10210002-11','PG-P7:防尘盖','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A0','T10110001/T10210003/T10210016','料号管控、xlsm、产品页签、工程BOM'),
('T10210003-01','插座连接器P-550SRA-45:基座合件','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A1','T10210003','料号管控、xlsm、产品页签、工程BOM'),
('T10210003-02','插座连接器P-550SRA-45:导向板','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A1','T10210003/T10210016','料号管控、xlsm、产品页签、工程BOM'),
('T10210003-10','插座连接器P-550SRA-45:550PIN保护盖','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A0','T10210003/T10210016','料号管控、xlsm、产品页签、工程BOM'),
('T10210004-01','Memory-HP288-400S：基座HOSING','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A0','T10110002/T10110003/T10110004/T10110005/T10110006/T10110008/T10110009/T10110010/T10110011','料号管控、xlsm、产品页签、工程BOM'),
('T10210004-04','Memory-HP288-400S：防尘盖COVER','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A1','T10110002/T10110003/T10110004/T10110005/T10110006/T10110008/T10110009/T10110010/T10110011','料号管控、xlsm、产品页签、工程BOM'),
('T10210005-01','Memory-HP288-120S：基座HOSING','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A0','T10110002/T10110003/T10110004/T10110005/T10110006/T10110007/T10110008/T10110009/T10110010/T10110011','料号管控、xlsm、产品页签、工程BOM'),
('T10210005-04','Memory-HP288-120S：防尘盖COVER','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A1','T10110002/T10110003/T10110004/T10110005/T10110006/T10110007/T10110008/T10110009/T10110010/T10110011','料号管控、xlsm、产品页签、工程BOM'),
('T10210006-01','Memory-LIF256P：基座HOSING','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A1','T10110010/T10110012','料号管控、xlsm、产品页签、工程BOM'),
('T10210006-01-02','Memory-LIF256P：基座HOSING2','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A1','T10110008/T10110009','料号管控、xlsm、产品页签、工程BOM'),
('T10210006-01-03','JN-CD-LIF256P：基座HOSING3','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A2','T10110013/T10110014/T10110015/T10110017/T10110018','料号管控、xlsm、产品页签、工程BOM'),
('T10210006-01-04','JN-CD-LIF256P：基座HOSING4','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A2','T10110016','料号管控、xlsm、产品页签、工程BOM'),
('T10210006-01-05','Memory-LIF256P：基座HOSING5','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A1','T10110019','料号管控、xlsm、产品页签、工程BOM'),
('T10210006-05','Memory-LIF256P：COVER','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A1','T10110008/T10110009/T10110010/T10110012/T10110013/T10110014/T10110015/T10110016/T10110017/T10110018/T10110019','料号管控、xlsm、产品页签、工程BOM'),
('T10210007-01','JN-CD-P-J444T：基座','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A0','T10110014/T10110015/T10110016/T10110017/T10110018/T10110021/T10110023/T10110024','料号管控、xlsm、产品页签、工程BOM'),
('T10210007-21','JN-CD-P-J444T：防尘盖','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A0','T10110014/T10110015/T10110016/T10110017/T10110018/T10110021/T10110023/T10110024','料号管控、xlsm、产品页签、工程BOM'),
('T10210014-05','JN-CD-SSMP-J-10:介质体1','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210014-06','JN-CD-SSMP-J-10:介质体2','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210014-07','JN-CD-SSMP-J-10:介质体3','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210014-08','JN-CD-SSMP-J-10:介质体4','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210014-09','JN-CD-SSMP-J-10:盖板','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210015-05','JN-CD-SSMP-K-1.13:介质体','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210016-01','550S板端连接器：基座','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A0','T10210016','料号管控、xlsm、产品页签、工程BOM'),
('T10210017-01','550S线缆连接器：基座','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A0','T10110020/T10110025','料号管控、xlsm、产品页签、工程BOM'),
('T10210017-06','550S线缆连接器：防尘盖','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A0','T10110020/T10110025','料号管控、xlsm、产品页签、工程BOM'),
('T10210019-01','SY-A32-P-J100S:基座合件','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A0','T10110021','料号管控、产品页签、工程BOM'),
('T10210021-01','SS-P4-PG68S:基座','','BOM-RM-PLASTIC','pcs',NULL,NULL,926,'物料','已确认','A0','T10110023/T10110024/T10110025','料号管控、工程BOM'),
('T10210022-02','SY-P32-PG22S：2*2*5.9塑胶平键','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T10210023-01','SY-D7P-H-J272S-A：基座','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A1','T10110026/T10110027','产品页签'),
('T10210023-01-01','SY-D7P-H-J272S-A：导向柱','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110026/T10110027','产品页签'),
('T10210023-05','SY-D7P-H-J272S-A：防尘盖','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110026/T10110027','产品页签'),
('T10210024-02','SY-D7P-J35S：盖板','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110026/T10110027','产品页签'),
('T10210024-03','SY-D7P-J35S：防尘盖','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110026/T10110027','产品页签'),
('T10210025-01','SY-D7P-H-J272S-B：基座','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110028/T10110029/T10110036','产品页签'),
('T10210025-04','SY-D7P-H-J272S-B：防尘盖','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110028/T10110029/T10110036','产品页签'),
('T10210026-01','SY-D7P-PG68S-A:基座','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110023/T10110024/T10110025/T10110028/T10110029','产品页签'),
('T10210031-02','接线架后胶芯','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T10210031-03','接线架前胶芯','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T10210031-07','防尘盖','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T10210032-03','P-098TRA-43后基座','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10210032','产品页签'),
('T10210032-12','P-098TRA-43保护盖','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10210032','产品页签'),
('T10210032-17','P-098TRA-43防尘盖','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10210032','产品页签'),
('T10210032-18','P-098TRA-43前基座','','BOM-RM-PLASTIC','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10210032','产品页签'),
('T10110001-01','PET单根编织护套-EX601251501','','BOM-RM-SLEEVE','pcs',NULL,NULL,926,'物料','已确认','A0','T10110001','料号管控、xlsm、产品页签、工程BOM'),
('T10210007-17','JN-CD-P-J444T：护套','','BOM-RM-TBD','pcs',NULL,NULL,926,'物料','已确认','A0','T10110014/T10110015/T10110016/T10110017/T10110018/T10110021/T10110023/T10110024','料号管控、xlsm、产品页签、工程BOM'),
('T10210007-18','JN-CD-P-J444T：铆管','','BOM-RM-TBD','pcs',NULL,NULL,926,'物料','已确认','A0','T10110014/T10110015/T10110016/T10110017/T10110018/T10110021/T10110023/T10110024','料号管控、xlsm、产品页签、工程BOM'),
('T10210007-22','HDTM-6-08-1-S-VT-0-1','','BOM-RM-TBD','pcs',NULL,NULL,926,'物料','已确认','A0','T10110014/T10110015/T10110016/T10110017/T10110018/T10110021/T10110023/T10110024','料号管控、xlsm、产品页签、工程BOM'),
('T10210007-23','HDTM-6-08-1-S-VT-4-L-1','','BOM-RM-TBD','pcs',NULL,NULL,926,'物料','已确认','A0','T10110014/T10110015/T10110016/T10110017/T10110018/T10110021/T10110023/T10110024','料号管控、xlsm、产品页签、工程BOM'),
('T10210007-24','HDTM-6-08-1-S-VT-5-R-1','','BOM-RM-TBD','pcs',NULL,NULL,926,'物料','已确认','A0','T10110014/T10110015/T10110016/T10110017/T10110018/T10110021/T10110023/T10110024','料号管控、xlsm、产品页签、工程BOM'),
('T10210007-26','JN-CD-P-J444T:不锈钢螺丝M2X10','','BOM-RM-TBD','pcs',NULL,NULL,926,'物料','已确认','A0','T10110014/T10110015/T10110016/T10110017/T10110018/T10110021/T10110023/T10110024','料号管控、产品页签、工程BOM'),
('T10210008-01','LNP60S-盖板','','BOM-RM-TBD','pcs',NULL,NULL,926,'物料','已确认','A0','T10110013','料号管控、xlsm、产品页签、工程BOM'),
('T10210008-02','LNP60S-铆钉','','BOM-RM-TBD','pcs',NULL,NULL,926,'物料','已确认','A0','T10110013','料号管控、xlsm、产品页签、工程BOM'),
('T10210016-08','550S板端连接器：弹簧','','BOM-RM-TBD','pcs',NULL,NULL,926,'物料','已确认','A0','T10210016','料号管控、xlsm、产品页签、工程BOM'),
('T10210016-09','550S板端连接器：弹板','','BOM-RM-TBD','pcs',NULL,NULL,926,'物料','已确认','A0','T10210016','料号管控、xlsm、产品页签、工程BOM'),
('T10210016-10','550S板端连接器：铆钉','','BOM-RM-TBD','pcs',NULL,NULL,926,'物料','已确认','A0','T10210016','料号管控、xlsm、产品页签、工程BOM'),
('T10210016-11','550S板端连接器：销针','','BOM-RM-TBD','pcs',NULL,NULL,926,'物料','已确认','A0','T10210016','料号管控、产品页签、工程BOM'),
('T10210017-02','550S线缆连接器：套筒接触件','','BOM-RM-TBD','pcs',NULL,NULL,926,'物料','已确认','A0','T10110020/T10110025','料号管控、xlsm、产品页签、工程BOM'),
('T10210017-03','550S线缆连接器：接线板合件','','BOM-RM-TBD','pcs',NULL,NULL,926,'物料','已确认','A0','T10110020/T10110025','料号管控、xlsm、产品页签、工程BOM'),
('T10210017-04','550S线缆连接器：共地片合件L','','BOM-RM-TBD','pcs',NULL,NULL,926,'物料','已确认','A0','T10110020/T10110025','料号管控、xlsm、产品页签、工程BOM'),
('T10210017-05','550S线缆连接器：共地片合件R','','BOM-RM-TBD','pcs',NULL,NULL,926,'物料','已确认','A0','T10110020/T10110025','料号管控、xlsm、产品页签、工程BOM'),
('T10210019','SY-A32-P-J100S','SZ-MS(HH)','BOM-RM-TBD','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110021','产品页签'),
('T10210019-06','SY-A32-P-J100S:套筒','','BOM-RM-TBD','pcs',NULL,NULL,926,'物料','已确认','A0','T10110021','料号管控、产品页签、工程BOM'),
('T10210020','SY-A32-P-J444T','PG-P7-JR','BOM-RM-TBD','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110021','产品页签'),
('T10210001-06','POGO连接器P256-PG116S：φ1.3焊接套筒','','BOM-RM-SLEEVE','pcs',NULL,NULL,926,'物料','已确认','A2','T10110001/T10110020/T10110023/T10110024/T10110025/T10110035/T10110036','料号管控、xlsm、产品页签、工程BOM'),
('T10210019-03','SY-A32-P-J100S:焊接套筒','','BOM-RM-SLEEVE','pcs',NULL,NULL,926,'物料','已确认','A0','T10110021','料号管控、产品页签、工程BOM'),
('T20010001','定制同轴电缆RF1.2(红色)','','BOM-RM-CABLE','m',NULL,NULL,926,'物料','已确认','A0','T10110001/T10110013/T10110014/T10110016/T10110017/T10110020/T10110021/T10110023/T10110024/T10110025','料号管控、xlsm、产品页签、工程BOM'),
('T200100013','定制同轴电缆排线RF1.1*10(深蓝色)','','BOM-RM-CABLE','m',NULL,NULL,NULL,'物料','待确认','/','T10110002','产品页签'),
('T20010002','标准同轴电缆RF0.81（黑色）','','BOM-RM-CABLE','m',NULL,NULL,926,'物料','已确认','A0','T10110001','料号管控、xlsm、产品页签、工程BOM'),
('T20010003','定制单芯排线0.6*8(蓝色)','','BOM-RM-CABLE','m',NULL,NULL,926,'物料','已确认','A0','T10110005','料号管控、xlsm、产品页签、工程BOM'),
('T20010004','定制同轴电缆排线RF1.0*10(深蓝色)','','BOM-RM-CABLE','m',NULL,NULL,926,'物料','已确认','A0','T10110002/T10110003/T10110004/T10110006/T10110007','料号管控、xlsm、产品页签、工程BOM'),
('T20010005','单芯线1.1，线芯0.6(红色)','','BOM-RM-CABLE','m',NULL,NULL,926,'物料','已确认','A0','T10110001/T10110008/T10110009/T10110010/T10110011/T10110015/T10110016','料号管控、xlsm、产品页签、工程BOM'),
('T20010006','标准同轴电缆RF1.13(黑色)','','BOM-RM-CABLE','m',NULL,NULL,926,'物料','已确认','A0','T10110008/T10110009/T10110010/T10110011/T10110012/T10110013/T10110014/T10110015/T10110016/T10110017/T10110018/T10110019/T10110021/T10110023/T10110024','料号管控、xlsm、产品页签、工程BOM'),
('T20010007','同轴电缆RF1.37(黑色)','','BOM-RM-CABLE','m',NULL,NULL,926,'物料','已确认','A0','T10110008/T10110009/T10110019/T10110026/T10110027/T10110028/T10110029/T10110035/T10110036','料号管控、xlsm、产品页签、工程BOM'),
('T20010008','定制同轴电缆排线RF0.8*10(深蓝色)','','BOM-RM-CABLE','m',NULL,NULL,926,'物料','已确认','A0','T10110005','料号管控、xlsm、产品页签、工程BOM'),
('T20010009','定制同轴电缆排线RF0.8*12(深蓝色)','','BOM-RM-CABLE','m',NULL,NULL,926,'物料','已确认','A0','T10110005','料号管控、xlsm、产品页签、工程BOM'),
('T20010010','同轴电缆RF1.5(红色定制,芯线0.7)','','BOM-RM-CABLE','m',NULL,NULL,926,'物料','已确认','A0','T10110017','料号管控、xlsm、产品页签'),
('T20010011','标准同轴电缆RF1.0（黑色）','','BOM-RM-CABLE','m',NULL,NULL,926,'物料','已确认','A0','T10110010/T10110020/T10110022/T10110023/T10110024/T10110025/T10110032/T10110033/T10110034','料号管控、产品页签、工程BOM'),
('T20010012','定制同轴线缆RF1.37（红色）','','BOM-RM-CABLE','m',NULL,NULL,926,'物料','已确认','A0','T10110021','料号管控、工程BOM'),
('T20010013','定制同轴电缆排线RF1.1*10(深蓝色)','','BOM-RM-CABLE','m',NULL,NULL,NULL,'物料','待确认','/','T10110003','产品页签'),
('T20010014','镀银铜丝0.5（短接线）','','BOM-RM-CABLE','m',NULL,NULL,NULL,'物料','待确认','/','T10110020/T10110022/T10110025/T10110032/T10110033/T10110034','产品页签'),
('T20010015','定制屏蔽电源线RF4.0(黑色)','','BOM-RM-CABLE','m',NULL,NULL,NULL,'物料','待确认','/','T10110009/T10110022/T10110032/T10110033/T10110034','产品页签'),
('T10210003-03','插座连接器P-550SRA-45:端子合件A','','BOM-RM-TERMINAL','pcs',NULL,NULL,926,'物料','已确认','A1','T10210003','料号管控、xlsm、产品页签、工程BOM'),
('T10210003-04','插座连接器P-550SRA-45:端子合件B','','BOM-RM-TERMINAL','pcs',NULL,NULL,926,'物料','已确认','A1','T10210003','料号管控、xlsm、产品页签、工程BOM'),
('T10210003-05','插座连接器P-550SRA-45:端子合件C','','BOM-RM-TERMINAL','pcs',NULL,NULL,926,'物料','已确认','A1','T10210003','料号管控、xlsm、产品页签、工程BOM'),
('T10210003-06','插座连接器P-550SRA-45:端子合件D','','BOM-RM-TERMINAL','pcs',NULL,NULL,926,'物料','已确认','A1','T10210003','料号管控、xlsm、产品页签、工程BOM'),
('T10210003-07','插座连接器P-550SRA-45:端子合件E','','BOM-RM-TERMINAL','pcs',NULL,NULL,926,'物料','已确认','A1','T10210003','料号管控、xlsm、产品页签、工程BOM'),
('T10210003-08','插座连接器P-550SRA-45:端子合件F','','BOM-RM-TERMINAL','pcs',NULL,NULL,926,'物料','已确认','A1','T10210003','料号管控、xlsm、产品页签、工程BOM'),
('T10210006-02-01','Memory-LIF256P：2PIN INSERTMOLDING-1','','BOM-RM-TERMINAL','pcs',NULL,NULL,926,'物料','已确认','A5','T10110008/T10110009/T10110013/T10110014/T10110015/T10110017/T10110018/T10110019','料号管控、xlsm、产品页签、工程BOM'),
('T10210006-02-02','Memory-LIF256P：2PIN INSERTMOLDING-2','','BOM-RM-TERMINAL','pcs',NULL,NULL,926,'物料','已确认','A5','T10110008/T10110009/T10110010/T10110012/T10110015/T10110016/T10110019','料号管控、xlsm、产品页签、工程BOM'),
('T10210007-02','JN-CD-P-J444T：鱼眼端子','','BOM-RM-TERMINAL','pcs',NULL,NULL,926,'物料','已确认','A2','T10110014/T10110015/T10110016/T10110017/T10110018/T10110021/T10110023/T10110024','料号管控、xlsm、产品页签、工程BOM'),
('T10210015-09','JN-CD-SSMP-K-1.13：导向针','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210016-02','550S板端连接器：端子合件A','','BOM-RM-TERMINAL','pcs',NULL,NULL,926,'物料','已确认','A0','T10210016','料号管控、xlsm、产品页签、工程BOM'),
('T10210016-03','550S板端连接器：端子合件B','','BOM-RM-TERMINAL','pcs',NULL,NULL,926,'物料','已确认','A0','T10210016','料号管控、xlsm、产品页签、工程BOM'),
('T10210016-04','550S板端连接器：端子合件C','','BOM-RM-TERMINAL','pcs',NULL,NULL,926,'物料','已确认','A0','T10210016','料号管控、xlsm、产品页签、工程BOM'),
('T10210016-05','550S板端连接器：端子合件D','','BOM-RM-TERMINAL','pcs',NULL,NULL,926,'物料','已确认','A0','T10210016','料号管控、xlsm、产品页签、工程BOM'),
('T10210016-06','550S板端连接器：端子合件E','','BOM-RM-TERMINAL','pcs',NULL,NULL,926,'物料','已确认','A0','T10210016','料号管控、xlsm、产品页签、工程BOM'),
('T10210016-07','550S板端连接器：端子合件F','','BOM-RM-TERMINAL','pcs',NULL,NULL,926,'物料','已确认','A0','T10210016','料号管控、xlsm、产品页签、工程BOM'),
('T10210022-03','SY-P32-PG22S：浮动导流针','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T10210022-06','SY-P32-PG22S：导流套筒1','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T10210022-07','SY-P32-PG22S：∅1.3焊接套筒2','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T10210022-08','SY-P32-PG22S：导流套筒2','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T10210022-10','SY-P32-PG22S：∅1.3焊接套筒1','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T10210023-02','SY-D7P-H-J272S-A：端子合件','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110026/T10110027','产品页签'),
('T10210024-01','SY-D7P-J35S：端子合件','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110026/T10110027','产品页签'),
('T10210026-04','SY-D7P-PG68S-A：焊接套筒','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110023/T10110024/T10110025/T10110028/T10110029','产品页签'),
('T10210031-04','电源插针1','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T10210031-05','电源插针2','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T10210031-06','信号焊接套筒','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T10210032-04','P-098TRA-43信号针8','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10210032','产品页签'),
('T10210032-05','P-098TRA-43信号针7','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10210032','产品页签'),
('T10210032-06','P-098TRA-43信号针6','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10210032','产品页签'),
('T10210032-07','P-098TRA-43信号针5','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10210032','产品页签'),
('T10210032-08','P-098TRA-43信号针4','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10210032','产品页签'),
('T10210032-09','P-098TRA-43信号针3','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10210032','产品页签'),
('T10210032-10','P-098TRA-43信号针2','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10210032','产品页签'),
('T10210032-11','P-098TRA-43信号针1','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10210032','产品页签'),
('T10210032-13','P-098TRA-43扭簧电源针1','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10210032','产品页签'),
('T10210032-14','P-098TRA-43扭簧电源针2','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10210032','产品页签'),
('T10210032-15','P-098TRA-43扭簧电源针3','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10210032','产品页签'),
('T10210032-16','P-098TRA-43扭簧电源针4','','BOM-RM-TERMINAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10210032','产品页签'),
('T10210001-07','POGO连接器P256-PG116S：定位螺栓M1*8.8','','BOM-RM-FASTENER','pcs',NULL,NULL,926,'物料','已确认','A1','T10110001/T10110020/T10110021/T10110023/T10110024/T10110025/T10110035/T10110036','料号管控、xlsm、产品页签、工程BOM'),
('T10210001-08','POGO连接器P256-PG116S：平头螺钉M1*2.5','','BOM-RM-FASTENER','pcs',NULL,NULL,926,'物料','已确认','A1','T10110001/T10110020/T10110021/T10110023/T10110024/T10110025/T10110035/T10110036','料号管控、xlsm、产品页签、工程BOM'),
('T10210002-09','线缆连接器P256-J550S:定位螺栓M1.2x33.0','','BOM-RM-FASTENER','pcs',NULL,NULL,926,'物料','已确认','A0','T10110001','料号管控、xlsm、产品页签、工程BOM'),
('T10210002-10','PG-P7:平头螺丝M1.2x5.5','','BOM-RM-FASTENER','pcs',NULL,NULL,926,'物料','已确认','A0','T10110001/T10110014/T10110015/T10110016/T10110017/T10110018/T10110021/T10110023/T10110024','料号管控、xlsm、产品页签、工程BOM'),
('T10210004-02','Memory-HP288-400S：定制M1.2*13.45螺母 SCREW UNT','','BOM-RM-FASTENER','pcs',NULL,NULL,926,'物料','已确认','A1','T10110002/T10110003/T10110004/T10110005/T10110006/T10110008/T10110009/T10110010/T10110011','料号管控、xlsm、产品页签、工程BOM'),
('T10210004-03','Memory-HP288-400S：定制M1.2*5.5平头螺丝 SCREW','','BOM-RM-FASTENER','pcs',NULL,NULL,926,'物料','已确认','A1','T10110002/T10110003/T10110004/T10110005/T10110006/T10110008/T10110009/T10110010/T10110011','料号管控、xlsm、产品页签、工程BOM'),
('T10210005-02','Memory-HP288-120S：定制M1.2*3.3螺母 SCREW UNT','','BOM-RM-FASTENER','pcs',NULL,NULL,926,'物料','已确认','A1','T10110002/T10110003/T10110004/T10110005/T10110006/T10110007/T10110008/T10110009/T10110010/T10110011','料号管控、xlsm、产品页签、工程BOM'),
('T10210005-03','Memory-HP288-120S：定制M1.2*2.2螺丝 SCREW','','BOM-RM-FASTENER','pcs',NULL,NULL,926,'物料','已确认','A1','T10110002/T10110003/T10110004/T10110005/T10110006/T10110007/T10110008/T10110009/T10110010/T10110011','料号管控、xlsm、产品页签、工程BOM'),
('T10210006-06','Memory-LIF256P：铆钉','','BOM-RM-FASTENER','pcs',NULL,NULL,926,'物料','已确认','A0','T10110012','料号管控、xlsm、工程BOM'),
('T10210006-07','JN-CD-LIF256P：等高螺丝','','BOM-RM-FASTENER','pcs',NULL,NULL,926,'物料','已确认','A1','T10110013/T10110014/T10110015/T10110016/T10110017/T10110018','料号管控、xlsm、产品页签、工程BOM'),
('T10210006-08','JN-CD-LIF256P：螺母','','BOM-RM-FASTENER','pcs',NULL,NULL,926,'物料','已确认','A1','T10110013/T10110014/T10110015/T10110016/T10110017/T10110018','料号管控、xlsm、产品页签、工程BOM'),
('T10210007-19','JN-CD-P-J444T：定位螺栓','','BOM-RM-FASTENER','pcs',NULL,NULL,926,'物料','已确认','A0','T10110014/T10110015/T10110016/T10110017/T10110018/T10110021/T10110023/T10110024','料号管控、xlsm、产品页签、工程BOM'),
('T10210015-06','JN-CD-SSMP-K-1.13:垫片1','','BOM-RM-FASTENER','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210015-07','JN-CD-SSMP-K-1.13:垫片2','','BOM-RM-FASTENER','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210015-08','JN-CD-SSMP-K-1.13:挡圈','','BOM-RM-FASTENER','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210015-10','DIN912-1983全牙滚花内六角螺丝M2*6（标准件）','','BOM-RM-FASTENER','pcs',NULL,NULL,NULL,'物料','待确认','/','T10110017','xlsm、产品页签'),
('T10210019-04','SY-A32-P-J100S:圆柱头螺栓M1×6','','BOM-RM-FASTENER','pcs',NULL,NULL,926,'物料','已确认','A0','T10110021','料号管控、产品页签、工程BOM'),
('T10210019-05','SY-A32-P-J100S:M1螺母','','BOM-RM-FASTENER','pcs',NULL,NULL,926,'物料','已确认','A0','T10110021','料号管控、产品页签、工程BOM'),
('T10210022-05','SY-P32-PG22S：∅0.6弹簧','','BOM-RM-FASTENER','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T10210022-13','SY-P32-PG22S：M2.5*8内六角螺钉','','BOM-RM-FASTENER','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T10210023-04','SY-D7P-H-J272S-A：铆管','','BOM-RM-FASTENER','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110026/T10110027','产品页签'),
('T10210025-03','SY-D7P-H-J272S-B：铆管','','BOM-RM-FASTENER','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110020/T10110025/T10110028/T10110029/T10110036','产品页签'),
('T10210026-05','SY-D7P-PG68S-A：弹性开口销','','BOM-RM-FASTENER','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110023/T10110024/T10110025/T10110028/T10110029','产品页签'),
('T10110016-01','SAM公头：SMA-J1.13 (标准件)','','BOM-RM-CONN','pcs',NULL,NULL,926,'物料','已确认','A0','T10110016','料号管控、xlsm、产品页签、工程BOM'),
('T10110016-02','连接器DB2EK-3.81-2P-GN-S','','BOM-RM-CONN','pcs',NULL,NULL,926,'物料','已确认','A0','T10110016','料号管控、xlsm、产品页签、工程BOM'),
('T10210022-01','SY-P32-PG22S：小插头前基座','','BOM-RM-CONN','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T10210022-04','SY-P32-PG22S：小插头信号胶芯','','BOM-RM-CONN','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T10210022-12','SY-P32-PG22S：小插头后基座','','BOM-RM-CONN','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T10210023','线缆连接器SY-D7P-H-J272S-A','SY-P4-STD','BOM-RM-CONN','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110026/T10110027','产品页签'),
('T10210024','FE基座连接器SY-D7P-J35S','SY-P4-PD','BOM-RM-CONN','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110026/T10110027','产品页签'),
('T10210025','线缆连接器SY-D7P-H-J272S-B','SY-P7-STD','BOM-RM-CONN','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110028/T10110029/T10110036','产品页签'),
('T10210031','XPM32-P098S线缆连接器','SY-D7M-5G(DIO-MAX5G)','BOM-RM-CONN','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T10210031-01','大插头前基座','','BOM-RM-CONN','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T20020003','4/6宝塔接头','','BOM-RM-CONN','PCS',NULL,NULL,NULL,'物料','待确认','/','T10110009/T10110010','产品页签'),
('T20020004','快插母头','','BOM-RM-CONN','PCS',NULL,NULL,NULL,'物料','待确认','/','T10110009/T10110010','产品页签'),
('T10210001-10','1.5*2.5*2.6（0.1）导电弹片','','BOM-RM-METAL','pcs',NULL,NULL,NULL,'物料','待确认','/','T10110020/T10110036','产品页签'),
('T10210003-09','插座连接器P-550SRA-45:屏蔽罩','','BOM-RM-METAL','pcs',NULL,NULL,926,'物料','已确认','A1','T10210003/T10210016','料号管控、xlsm、产品页签、工程BOM'),
('T10210006-03','Memory-LIF256P：LOCK PLATE','','BOM-RM-METAL','pcs',NULL,NULL,926,'物料','已确认','A2','T10110008/T10110009/T10110010/T10110012/T10110013/T10110014/T10110015/T10110016/T10110017/T10110018/T10110019','料号管控、xlsm、产品页签、工程BOM'),
('T10210006-04','Memory-LIF256P：GND PLATE','','BOM-RM-METAL','pcs',NULL,NULL,926,'物料','已确认','A1','T10110008/T10110009/T10110010/T10110012/T10110013/T10110014/T10110015/T10110017/T10110018/T10110019','料号管控、xlsm、产品页签、工程BOM'),
('T10210014-01','JN-CD-SSMP-J-10:外导体1','','BOM-RM-METAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210014-02','JN-CD-SSMP-J-10:外导体2','','BOM-RM-METAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210014-03','JN-CD-SSMP-J-10:内导体1','','BOM-RM-METAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210014-04','JN-CD-SSMP-J-10:内导体2','','BOM-RM-METAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210015-01','JN-CD-SSMP-K-1.13:外导体1','','BOM-RM-METAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210015-02','JN-CD-SSMP-K-1.13:外导体2','','BOM-RM-METAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210015-03','JN-CD-SSMP-K-1.13:外导体3','','BOM-RM-METAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210015-04','JN-CD-SSMP-K-1.13:内导体','','BOM-RM-METAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110017','xlsm、产品页签'),
('T10210021-06','PCB-0.9*3.4*2.5共地弹片','','BOM-RM-METAL','pcs',NULL,NULL,926,'物料','已确认','A0','T10110023/T10110024/T10110025','料号管控、工程BOM'),
('T10210022-09','SY-P32-PG22S：信号接地片','','BOM-RM-METAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T10210022-11','SY-P32-PG22S：接地块','','BOM-RM-METAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110022/T10110032/T10110033/T10110034','产品页签'),
('T10210023-03','SY-D7P-H-J272S-A：共地弹片','','BOM-RM-METAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10110026/T10110027','产品页签'),
('T10210032-01','P-098TRA-43屏蔽罩','','BOM-RM-METAL','pcs',NULL,NULL,NULL,'物料','待确认','A0','T10210032','产品页签');

-- ---------- 3) 写入商品档案（按 product_code 幂等） ----------
INSERT INTO base_product
 (product_code,product_name,product_model,product_spec,product_desc,
  big_type_id,big_type_name,type_id,type_name,full_type_id,full_type_name,
  provider_id,provider_code,provider_short_name,
  consignor_id,consignor_code,consignor_name,
  small_unit,big_unit,unit_convert,unit_convert_text,
  purchase_price,sale_price,day_price,activity_price,vip_price,market_price,
  storage_upper,storage_lower,avg_price,weight,
  product_status,promo_status,auditing,open_spec,is_limit_buy,is_hot,click_count,order_num,
  expand_fields,del_flag,tenant_id,create_by_name,create_time)
SELECT t.product_code,t.product_name,NULLIF(t.product_model,''),NULL,
 CONCAT('BOM主数据 | 版本:',IFNULL(NULLIF(t.ver,''),'/'),' | 层级:',t.tier,' | 来源:',t.src,
        IF(t.bom_prods='','',CONCAT(' | 用于:',t.bom_prods))),
 r.type_id,r.type_name,m.type_id,m.type_name,
 CONCAT('0/',r.type_id,'/',m.type_id),CONCAT('根/',r.type_name,'/',m.type_name),
 t.provider,CASE WHEN t.provider=926 THEN 'PV201940371' END,CASE WHEN t.provider=926 THEN '默认供应商' END,
 30,'HZ20180002','默认货主',
 t.unit,t.big_unit,t.unit_convert,
 CASE WHEN t.unit_convert IS NOT NULL THEN CONCAT('1',IFNULL(t.big_unit,'箱'),'=',t.unit_convert,t.unit) END,
 0,0,0,0,0,NULL,0,0,0,0,
 1,0,0,0,0,0,0,0,
 JSON_OBJECT('bomTier',t.tier,'bomVersion',NULLIF(t.ver,''),'bomRole',t.role,'bomSource',t.src),
 0,'000000','系统导入',NOW()
FROM tmp_bom_product t
JOIN base_product_type m ON m.type_code=t.type_code AND m.del_flag=0
JOIN base_product_type r ON r.type_id=m.parent_id AND r.del_flag=0
WHERE NOT EXISTS (SELECT 1 FROM base_product p WHERE p.product_code=t.product_code AND p.del_flag=0);

-- ---------- 3b) 校正商品描述中的“用于成品”（仅本次导入的商品） ----------
UPDATE base_product p JOIN tmp_bom_product t ON t.product_code=p.product_code
SET p.product_desc=CONCAT('BOM主数据 | 版本:',IFNULL(NULLIF(t.ver,''),'/'),' | 层级:',t.tier,' | 来源:',t.src,
    IF(t.bom_prods='','',CONCAT(' | 用于:',t.bom_prods)))
WHERE p.create_by_name='系统导入' AND p.del_flag=0;

-- ---------- 4) 商品箱规：来自 BOM 包装用量的 5 个成品 ----------
INSERT INTO base_box_spec (product_id,spec_name,inner_qty,outer_inner_count,box_code_mode,is_default,enable,remark,del_flag,tenant_id,create_by_name,create_time)
SELECT p.product_id,'外箱16pcs/CTN（纸箱T10510001-01）',16,1,'SKU',1,1,'BOM包装用量原值 1/16',0,'000000','系统导入',NOW()
FROM base_product p WHERE p.product_code='T10110001' AND p.del_flag=0
AND NOT EXISTS (SELECT 1 FROM base_box_spec s WHERE s.product_id=p.product_id AND s.spec_name='外箱16pcs/CTN（纸箱T10510001-01）' AND s.del_flag=0);
INSERT INTO base_box_spec (product_id,spec_name,inner_qty,outer_inner_count,box_code_mode,is_default,enable,remark,del_flag,tenant_id,create_by_name,create_time)
SELECT p.product_id,'外箱16pcs/CTN（纸箱T10510001-01）',16,1,'SKU',1,1,'BOM包装用量原值 1/16',0,'000000','系统导入',NOW()
FROM base_product p WHERE p.product_code='T10110020' AND p.del_flag=0
AND NOT EXISTS (SELECT 1 FROM base_box_spec s WHERE s.product_id=p.product_id AND s.spec_name='外箱16pcs/CTN（纸箱T10510001-01）' AND s.del_flag=0);
INSERT INTO base_box_spec (product_id,spec_name,inner_qty,outer_inner_count,box_code_mode,is_default,enable,remark,del_flag,tenant_id,create_by_name,create_time)
SELECT p.product_id,'外箱16pcs/CTN（纸箱T10510001-01）',16,1,'SKU',1,1,'BOM包装用量原值 1/16',0,'000000','系统导入',NOW()
FROM base_product p WHERE p.product_code='T10110025' AND p.del_flag=0
AND NOT EXISTS (SELECT 1 FROM base_box_spec s WHERE s.product_id=p.product_id AND s.spec_name='外箱16pcs/CTN（纸箱T10510001-01）' AND s.del_flag=0);
INSERT INTO base_box_spec (product_id,spec_name,inner_qty,outer_inner_count,box_code_mode,is_default,enable,remark,del_flag,tenant_id,create_by_name,create_time)
SELECT p.product_id,'内盒1pcs/BOX+外箱36盒/CTN（T10510002-01/02）',1,36,'BOTH',1,1,'BOM包装用量原值 1/36',0,'000000','系统导入',NOW()
FROM base_product p WHERE p.product_code='T10210003' AND p.del_flag=0
AND NOT EXISTS (SELECT 1 FROM base_box_spec s WHERE s.product_id=p.product_id AND s.spec_name='内盒1pcs/BOX+外箱36盒/CTN（T10510002-01/02）' AND s.del_flag=0);
INSERT INTO base_box_spec (product_id,spec_name,inner_qty,outer_inner_count,box_code_mode,is_default,enable,remark,del_flag,tenant_id,create_by_name,create_time)
SELECT p.product_id,'内盒1pcs/BOX+外箱36盒/CTN（T10510002-01/02）',1,36,'BOTH',1,1,'BOM包装用量原值 1/36',0,'000000','系统导入',NOW()
FROM base_product p WHERE p.product_code='T10210016' AND p.del_flag=0
AND NOT EXISTS (SELECT 1 FROM base_box_spec s WHERE s.product_id=p.product_id AND s.spec_name='内盒1pcs/BOX+外箱36盒/CTN（T10510002-01/02）' AND s.del_flag=0);

DROP TEMPORARY TABLE IF EXISTS tmp_bom_product;

-- ---------- 5) 校验：分类树商品数 ----------
SELECT r.type_name AS 大类, m.type_name AS 中类, COUNT(p.product_id) AS 商品数
FROM base_product_type r
JOIN base_product_type m ON m.parent_id=r.type_id AND m.del_flag=0
LEFT JOIN base_product p ON p.type_id=m.type_id AND p.del_flag=0
WHERE r.type_code IN ('BOM-FG','BOM-RM','BOM-PKG')
GROUP BY r.type_name,m.type_name ORDER BY FIELD(r.type_name,'成品','原材料','包装材料'),m.type_name;

-- ---------- 6) 回滚（确认要撤销本次导入时才执行，默认注释） ----------
-- DELETE s FROM base_box_spec s WHERE s.create_by_name='系统导入';
-- DELETE FROM base_product WHERE create_by_name='系统导入' AND tenant_id='000000';
-- DELETE FROM base_product_type WHERE type_code LIKE 'BOM-%';