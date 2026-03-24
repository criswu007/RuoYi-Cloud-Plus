# 南京标准地址核心盘点证据

## 1. 基础连接信息
- 通过 `ADDRESS_DB_URL`/`ADDRESS_DB_USERNAME`（账户 `address`，密码未记录）向历史地址数据库建立连接，执行期间发现 `Current catalog: ftth_cloud_address`，说明目标实例就是 ftth_cloud_address。

## 2. 表数量与数据量
- `TABLE_ROW_COUNTS:tmp_addr_segm_nj_20260317_row_count` row_count=364000
- `TABLE_ROW_COUNTS:tmp_addr_set_segm_nj_20260317_row_count` row_count=1566000
- `TABLE_ROW_COUNTS:spc_station_row_count` row_count=1229
- `TABLE_ROW_COUNTS:spc_region_row_count` row_count=102
- `TABLE_ROW_COUNTS:segm_addr_type_row_count` row_count=18
- `TABLE_ROW_COUNTS:pub_restriction_row_count` row_count=6361
- `TABLE_ROW_COUNTS:spc_regional_company_row_count` row_count=79
- `TABLE_ROW_COUNTS:staff_row_count` row_count=891

## 3. 标准地址主事实表关键分布
### ADDR_MAIN_COLUMNS:tmp_addr_segm_columns
```
column_name=id, column_type=bigint(20) unsigned, is_nullable=NO, column_default=NULL
column_name=segm_id, column_type=varchar(1024), is_nullable=YES, column_default=NULL
column_name=segm_type, column_type=varchar(32), is_nullable=YES, column_default=NULL
column_name=segm_name, column_type=varchar(255), is_nullable=YES, column_default=NULL
column_name=segm_no, column_type=varchar(255), is_nullable=YES, column_default=NULL
column_name=parent_segm_id, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=outside, column_type=varchar(32), is_nullable=YES, column_default=NULL
column_name=status, column_type=varchar(32), is_nullable=YES, column_default=NULL
column_name=time, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=outregion, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=post_code, column_type=varchar(32), is_nullable=YES, column_default=NULL
column_name=is_city, column_type=tinyint(4), is_nullable=YES, column_default=NULL
column_name=is_band, column_type=tinyint(4), is_nullable=YES, column_default=NULL
column_name=is_user, column_type=tinyint(4), is_nullable=YES, column_default=NULL
column_name=stand_name, column_type=varchar(1024), is_nullable=YES, column_default=NULL
column_name=stand_no, column_type=varchar(512), is_nullable=YES, column_default=NULL
column_name=region_id, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=template_id, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=notes, column_type=text, is_nullable=YES, column_default=NULL
column_name=delete_state, column_type=tinyint(4), is_nullable=YES, column_default=NULL
column_name=delete_time, column_type=datetime, is_nullable=YES, column_default=NULL
column_name=old_data, column_type=text, is_nullable=YES, column_default=NULL
column_name=modify_op, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=modiry_date, column_type=datetime, is_nullable=YES, column_default=NULL
column_name=alias, column_type=varchar(255), is_nullable=YES, column_default=NULL
column_name=sync_date, column_type=datetime, is_nullable=YES, column_default=NULL
column_name=create_time, column_type=datetime, is_nullable=YES, column_default=NULL
column_name=is4gis, column_type=tinyint(4), is_nullable=YES, column_default=NULL
column_name=service_region_id, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=lan_id, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=ppdom_id, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=old_id_eqp, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=old_sp, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=addr_from, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=is_highclass_area, column_type=tinyint(4), is_nullable=YES, column_default=NULL
column_name=create_date, column_type=datetime, is_nullable=YES, column_default=NULL
column_name=create_op, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=border_address, column_type=varchar(1024), is_nullable=YES, column_default=NULL
column_name=x, column_type=decimal(18,6), is_nullable=YES, column_default=NULL
column_name=y, column_type=decimal(18,6), is_nullable=YES, column_default=NULL
column_name=exp_date, column_type=datetime, is_nullable=YES, column_default=NULL
column_name=addr_type, column_type=varchar(32), is_nullable=YES, column_default=NULL
column_name=is_compete_region, column_type=tinyint(4), is_nullable=YES, column_default=NULL
column_name=is_compete_modify_op, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=is_compete_modify_date, column_type=datetime, is_nullable=YES, column_default=NULL
column_name=build_units, column_type=varchar(32), is_nullable=YES, column_default=NULL
column_name=build_floors, column_type=varchar(32), is_nullable=YES, column_default=NULL
column_name=floor_height, column_type=varchar(32), is_nullable=YES, column_default=NULL
column_name=isbuilding, column_type=tinyint(4), is_nullable=YES, column_default=NULL
column_name=is_normal_addr, column_type=tinyint(4), is_nullable=YES, column_default=NULL
column_name=is_instal_addr, column_type=tinyint(4), is_nullable=YES, column_default=NULL
column_name=mnt_type_id, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=optic_node, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=district_id, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=location_id, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=is_key, column_type=tinyint(4), is_nullable=YES, column_default=NULL
column_name=is_instead_indoor_devices, column_type=tinyint(4), is_nullable=YES, column_default=NULL
column_name=is_support_cm, column_type=tinyint(4), is_nullable=YES, column_default=NULL
column_name=is_support_eoc, column_type=tinyint(4), is_nullable=YES, column_default=NULL
column_name=net_struct_id, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=is_support_ipqqm, column_type=tinyint(4), is_nullable=YES, column_default=NULL
column_name=line_extension_fee, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=is_line_install, column_type=tinyint(4), is_nullable=YES, column_default=NULL
column_name=net_area_type_id, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=build_grade_id, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=build_type_id, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=citycom_flag, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=segm_name_fif, column_type=varchar(255), is_nullable=YES, column_default=NULL
column_name=segm_name_fir, column_type=varchar(255), is_nullable=YES, column_default=NULL
column_name=station_id, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=addr_in_type, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=place_type, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=opr_state, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=cover_num, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=area_manager, column_type=varchar(128), is_nullable=YES, column_default=NULL
column_name=installstation_id, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=busstation_id, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=addr_in_type_ftth, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=ftth_pon_type, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=addr_in_type_lan, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=area_type, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=addr_unit_type, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=addr_def, column_type=text, is_nullable=YES, column_default=NULL
```
### ADDR_MAIN_COLUMNS:tmp_addr_segm_segm_type_distribution
```
segm_type=180007, total=336020
segm_type=180013, total=20379
segm_type=180006, total=3110
segm_type=180005, total=2320
segm_type=180009, total=1824
segm_type=180095, total=277
segm_type=180004, total=54
segm_type=180010, total=8
segm_type=180002, total=6
segm_type=180003, total=1
segm_type=180015, total=1
```
### NULL_CHECKS:tmp_addr_segm_missing_parent_count
```
missing=74661
```
### DUPLICATE_CHECKS:tmp_addr_segm_duplicate_segm_id_top20
```
[EMPTY RESULT]
```

## 4. 安装地址关联事实关键分布
### INSTALL_ADDR_MAIN_COLUMNS:tmp_addr_set_segm_columns
```
column_name=id, column_type=bigint(20) unsigned, is_nullable=NO, column_default=NULL
column_name=set_addr_id, column_type=varchar(255), is_nullable=NO, column_default=NULL
column_name=set_addr_name, column_type=varchar(1024), is_nullable=YES, column_default=NULL
column_name=set_type, column_type=varchar(32), is_nullable=YES, column_default=NULL
column_name=segm_id, column_type=varchar(255), is_nullable=YES, column_default=NULL
column_name=status, column_type=varchar(50), is_nullable=YES, column_default=NULL
column_name=region_id, column_type=varchar(255), is_nullable=YES, column_default=NULL
column_name=notes, column_type=varchar(1024), is_nullable=YES, column_default=NULL
column_name=delete_state, column_type=tinyint(4), is_nullable=YES, column_default=NULL
column_name=delete_time, column_type=datetime, is_nullable=YES, column_default=NULL
column_name=modiry_date, column_type=datetime, is_nullable=YES, column_default=NULL
column_name=alias, column_type=varchar(255), is_nullable=YES, column_default=NULL
column_name=create_date, column_type=datetime, is_nullable=YES, column_default=NULL
column_name=boss_op, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=segm_type, column_type=varchar(32), is_nullable=YES, column_default=NULL
column_name=area_id, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=set_addr_no, column_type=varchar(512), is_nullable=YES, column_default=NULL
column_name=synchronous_date, column_type=datetime, is_nullable=YES, column_default=NULL
column_name=old_segm_id, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=modify_op, column_type=varchar(64), is_nullable=YES, column_default=NULL
column_name=org_id, column_type=varchar(64), is_nullable=YES, column_default=NULL
```
### INSTALL_ADDR_MAIN_COLUMNS:tmp_addr_set_segm_set_type_distribution
```
set_type=NULL, total=1562375
set_type=0, total=3625
```
### RELATION_CHECKS:tmp_addr_set_segm_missing_standard_count
```
missing=1290542
```
### DUPLICATE_CHECKS:tmp_addr_set_segm_duplicate_set_addr_id_top20
```
[EMPTY RESULT]
```

## 5. 区域/站点/字典辅助表分布
### `spc_station` & `spc_region`
- `TABLE_ROW_COUNTS:spc_station_row_count` row_count=1229
- `TABLE_ROW_COUNTS:spc_region_row_count` row_count=102
### DICTIONARY_CHECKS:segm_addr_type_level_distribution
```
level_id=60, total=2
level_id=100, total=1
level_id=70, total=1
level_id=80, total=1
level_id=30, total=1
level_id=35, total=1
level_id=1, total=1
level_id=95, total=1
level_id=99, total=1
level_id=8, total=1
level_id=10, total=1
level_id=65, total=1
level_id=20, total=1
level_id=90, total=1
level_id=5, total=1
level_id=15, total=1
level_id=50, total=1
```
### DICTIONARY_CHECKS:pub_restriction_code_distribution
```
code=NULL, total=4977
code=1, total=44
code=2, total=39
code=UNKNOW, total=31
code=3, total=28
code=0, total=23
code=4, total=21
code=7, total=21
code=35600108, total=21
code=6, total=20
code=5, total=18
code=35600103, total=15
code=8, total=13
code=35600109, total=12
code=9, total=12
code=O, total=11
code=35600112, total=11
code=10, total=11
code=IP, total=10
code=NP, total=10
```
### DICTIONARY_CHECKS:spc_regional_company_seg_type_priv_distribution
```
segm_type_priv=180013, total=79
```
### `segm_addr_type` / `pub_restriction` / `spc_regional_company` 行数
- `TABLE_ROW_COUNTS:segm_addr_type_row_count` row_count=18
- `TABLE_ROW_COUNTS:pub_restriction_row_count` row_count=6361
- `TABLE_ROW_COUNTS:spc_regional_company_row_count` row_count=79

## 6. 关键关联一致性检查
- `RELATION_CHECKS:tmp_addr_segm_missing_region_link_count` 输出为 `[EMPTY RESULT]`，表明 `region_id` 关联的区域记录都存在。
- `RELATION_CHECKS:tmp_addr_segm_missing_station_id_count` missing=1，说明有 1 条标准地址记录无法在 `spc_station` 中找到对应 `station_id`。
- `RELATION_CHECKS:tmp_addr_segm_missing_installstation_id_count` missing=0，`installstation_id` 均在 `spc_station` 中匹配。
- `RELATION_CHECKS:tmp_addr_segm_missing_busstation_id_count` missing=0，`busstation_id` 也均有落库记录。
- `NULL_CHECKS:tmp_addr_segm_missing_parent_count` missing=74661，表明仍有大量上级地址缺失，需要进一步核实。

## 7. 原始异常与待确认项
- 安装地址表中 `INSTALL_ADDR_MAIN_COLUMNS:tmp_addr_set_segm_set_type_distribution` 结果显示 `set_type=NULL` 约 1,562,375 条，`set_type=0` 仅 3,625 条，说明绝大多数安装地址缺少类型标识。
- `RELATION_CHECKS:tmp_addr_set_segm_missing_standard_count` missing=1290542，表示近 1.29M 条安装地址未关联到 `tmp_addr_segm`，需要对接入数据源或识别策略给出解释。
- `NULL_CHECKS:tmp_addr_segm_missing_parent_count` missing=74661 仍属于重复出现的异常节点。
- `RELATION_CHECKS:tmp_addr_segm_missing_station_id_count` missing=1，属于极少数但仍需人工确认字段值和站点表能否补录。
