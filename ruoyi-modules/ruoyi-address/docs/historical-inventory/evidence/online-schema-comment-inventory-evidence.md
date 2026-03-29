# 线上最新非备份表结构盘点证据

## 1. 范围规则

- 当前线上盘点范围以 `ftth_cloud_address` catalog 下 `table_name not like 'bak%'` 的非备份表为准。
- `ADDR_SEGM` 与 `ADDR_SET_SEGM` 的表结构按全量 `information_schema` 盘点，数据观测按 `crc32(主键) % 100 = 0` 的 1% hash sample 统计。
- 抽样盘点仅用于值域/空值/分布观察，不替代全量数据质量结论。

## 2. 非备份表清单与总数

```text
TABLE_COUNT	10
TABLE	ADDR_SEGM
TABLE	ADDR_SET_SEGM
TABLE	getpageVol
TABLE	pub_restriction
TABLE	segm_addr_type
TABLE	spc_region
TABLE	spc_regional_company
TABLE	spc_station
TABLE	staff
TABLE	sync_set_addr_info
```

## 3. 表级结构摘要

```text
table_name	row_count	table_comment	column_total	comment_total
ADDR_SEGM	2318000	标准分段地址	82	82
ADDR_SET_SEGM	100000	安装地址表	20	20
getpageVol	0	选址信息表	7	7
pub_restriction	6344	约束关系	14	14
segm_addr_type	19	地址类型表	10	10
spc_region	103	分公司	28	28
spc_regional_company	80	组织映射表	7	7
spc_station	1230	管理站表	85	85
staff	892	员工	48	5
sync_set_addr_info	0	地址变更同步表	8	8
```

## 4. 字段注释清单

```text
table_name	ordinal	column_name	column_type	is_nullable	column_key	column_comment
ADDR_SEGM	1	segm_id	varchar(24)	NO	PRI	分段地址 ID 即标准地址 ID（主键）
ADDR_SEGM	2	segm_type	int(11)	YES	MUL	分段地址级别
ADDR_SEGM	3	segm_name	varchar(200)	YES	MUL	分段地址名称
ADDR_SEGM	4	segm_no	varchar(120)	YES	MUL	分段地址简拼
ADDR_SEGM	5	parent_segm_id	varchar(24)	YES	MUL	上级分段地址
ADDR_SEGM	6	outside	int(11)	YES		是否展示给前台 0 为是 1 为否
ADDR_SEGM	7	status	int(11)	YES		状态
ADDR_SEGM	8	time	datetime	YES		变动时间
ADDR_SEGM	9	outregion	int(11)	YES		区外
ADDR_SEGM	10	post_code	varchar(255)	YES		单项工程编号
ADDR_SEGM	11	is_city	char(1)	YES		是否城市
ADDR_SEGM	12	is_band	char(1)	YES		是否宽带到户
ADDR_SEGM	13	is_user	char(1)	YES		是否为户线
ADDR_SEGM	14	stand_name	varchar(400)	YES	MUL	标准地址全称
ADDR_SEGM	15	stand_no	varchar(300)	YES		标准地址简拼
ADDR_SEGM	16	region_id	varchar(24)	YES	MUL	所属管理区域
ADDR_SEGM	17	template_id	int(11)	YES		模板 ID
ADDR_SEGM	18	notes	varchar(255)	YES		备注
ADDR_SEGM	19	delete_state	char(1)	YES	MUL	删除状态 0 = 未删除 1 = 已删除
ADDR_SEGM	20	delete_time	datetime	YES	MUL	删除时间
ADDR_SEGM	21	old_data	decimal(20,0)	YES		是否旧数据
ADDR_SEGM	22	modify_op	bigint(20)	YES		录入人员
ADDR_SEGM	23	modiry_date	datetime	YES	MUL	录入时间
ADDR_SEGM	24	alias	varchar(80)	YES		别名
ADDR_SEGM	25	sync_date	datetime	YES		同步时间
ADDR_SEGM	26	create_time	datetime	YES		创建时间
ADDR_SEGM	27	is4gis	char(1)	YES		是否 GIS 专用
ADDR_SEGM	28	service_region_id	varchar(24)	YES		所属社区
ADDR_SEGM	29	lan_id	decimal(20,0)	YES		所属本地网
ADDR_SEGM	30	ppdom_id	decimal(20,0)	YES		域 ID
ADDR_SEGM	31	old_id_eqp	bigint(20)	YES		旧设备 ID
ADDR_SEGM	32	old_sp	varchar(8)	YES		旧系统标识
ADDR_SEGM	33	addr_from	varchar(200)	YES		地址来源
ADDR_SEGM	34	is_highclass_area	char(1)	YES		是否高档住宅或政府小区
ADDR_SEGM	35	create_date	datetime	NO	MUL	创建日期
ADDR_SEGM	36	create_op	bigint(20)	YES		创建人
ADDR_SEGM	37	border_address	varchar(24)	YES		边界地址
ADDR_SEGM	38	x	decimal(20,10)	YES		坐标 X
ADDR_SEGM	39	y	decimal(20,10)	YES		坐标 Y
ADDR_SEGM	40	exp_date	datetime	YES		到期时间
ADDR_SEGM	41	addr_type	int(11)	YES		地址类型
ADDR_SEGM	42	is_compete_region	char(1)	YES		是否竞争区域
ADDR_SEGM	43	is_compete_modify_op	int(11)	YES		修改竞争区域的操作人
ADDR_SEGM	44	is_compete_modify_date	datetime	YES		竞争区域的修改时间
ADDR_SEGM	45	build_units	decimal(20,0)	YES		单元数
ADDR_SEGM	46	build_floors	decimal(20,0)	YES		层数
ADDR_SEGM	47	floor_height	decimal(4,1)	YES		层高
ADDR_SEGM	48	isbuilding	char(1)	NO		是否为楼宇 0 = 否 1 = 是
ADDR_SEGM	49	is_normal_addr	char(1)	YES		是否标准地址
ADDR_SEGM	50	is_instal_addr	char(1)	YES		是否可安装地址
ADDR_SEGM	51	mnt_type_id	int(11)	YES		维护方式：自维、代维
ADDR_SEGM	52	optic_node	varchar(200)	YES		光节点
ADDR_SEGM	53	district_id	varchar(24)	YES		行政区域
ADDR_SEGM	54	location_id	varchar(24)	YES	MUL	地块 ID
ADDR_SEGM	55	is_key	int(11)	YES		是否主用名：0 - 否，1 - 是
ADDR_SEGM	56	is_instead_indoor_devices	char(1)	YES		是否代装室内设备
ADDR_SEGM	57	is_support_cm	char(1)	YES		是否支持 CM
ADDR_SEGM	58	is_support_eoc	char(1)	YES		是否支持 EOC
ADDR_SEGM	59	net_struct_id	int(11)	YES		网络结构 ID
ADDR_SEGM	60	is_support_ipqqm	char(1)	YES		是否支持 IP 机顶盒
ADDR_SEGM	61	line_extension_fee	decimal(10,2)	YES		线路扩容费
ADDR_SEGM	62	is_line_install	char(1)	YES		是否线路安装
ADDR_SEGM	63	net_area_type_id	int(11)	YES		网络区域类型 ID
ADDR_SEGM	64	build_grade_id	int(11)	YES		建筑等级 ID
ADDR_SEGM	65	build_type_id	int(11)	YES		建筑类型 ID
ADDR_SEGM	66	citycom_flag	int(11)	YES		城域标识
ADDR_SEGM	67	segm_name_fif	varchar(24)	YES		地址名称扩展
ADDR_SEGM	68	segm_name_fir	char(1)	YES		是否配套费小区
ADDR_SEGM	69	station_id	varchar(24)	YES		地址所属维修管理站
ADDR_SEGM	70	addr_in_type	bigint(20)	YES		地址接入方式（旧）
ADDR_SEGM	71	place_type	int(11)	YES		场所性质
ADDR_SEGM	72	opr_state	bigint(20)	YES		审核状态
ADDR_SEGM	73	cover_num	int(11)	YES		楼栋覆盖户数
ADDR_SEGM	74	area_manager	varchar(60)	YES		片区经理
ADDR_SEGM	75	installstation_id	varchar(24)	YES		地址所属安装管理站
ADDR_SEGM	76	busstation_id	varchar(24)	YES		地址所属营业管理站
ADDR_SEGM	77	addr_in_type_ftth	int(11)	YES		光纤接入方式
ADDR_SEGM	78	ftth_pon_type	int(11)	YES		光纤接入能力
ADDR_SEGM	79	addr_in_type_lan	int(11)	YES		电缆接入方式
ADDR_SEGM	80	area_type	int(11)	YES		城乡属性
ADDR_SEGM	81	addr_unit_type	int(11)	YES		房屋属性
ADDR_SEGM	82	addr_def	varchar(255)	YES		房屋定义
ADDR_SET_SEGM	1	set_addr_id	varchar(24)	NO	PRI	安装地址 ID（主键）
ADDR_SET_SEGM	2	set_addr_name	varchar(400)	YES	MUL	安装地址名称
ADDR_SET_SEGM	3	set_type	int(11)	YES	MUL	地址类型：0 = 伪地址，2 = 到户地址
ADDR_SET_SEGM	4	segm_id	varchar(24)	YES	MUL	标准地址分段 ID
ADDR_SET_SEGM	5	status	int(11)	YES	MUL	状态
ADDR_SET_SEGM	6	region_id	varchar(24)	YES	MUL	所属区域 ID
ADDR_SET_SEGM	7	notes	varchar(2000)	YES		备注
ADDR_SET_SEGM	8	delete_state	char(1)	YES	MUL	删除状态（0 = 未删除，1 = 已删除）
ADDR_SET_SEGM	9	delete_time	datetime	YES		删除时间
ADDR_SET_SEGM	10	modiry_date	datetime	YES	MUL	修改时间
ADDR_SET_SEGM	11	alias	varchar(80)	YES		别名
ADDR_SET_SEGM	12	create_date	datetime	YES	MUL	创建日期
ADDR_SET_SEGM	13	boss_op	varchar(80)	YES		BOSS 操作人
ADDR_SET_SEGM	14	segm_type	int(11)	YES	MUL	地址分段类型
ADDR_SET_SEGM	15	area_id	bigint(20)	YES		片区 ID
ADDR_SET_SEGM	16	set_addr_no	varchar(120)	YES	MUL	安装地址编号
ADDR_SET_SEGM	17	synchronous_date	datetime	YES		同步时间
ADDR_SET_SEGM	18	old_segm_id	varchar(24)	YES		旧标准地址 ID
ADDR_SET_SEGM	19	modify_op	int(11)	YES		修改人
ADDR_SET_SEGM	20	org_id	varchar(80)	YES		组织机构 ID
getpageVol	1	uuid	varchar(64)	NO	PRI	主键，唯一标识 ID
getpageVol	2	returnval	varchar(3000)	YES		返回值 / 选址结果信息
getpageVol	3	systemsource	varchar(10)	YES		系统来源
getpageVol	4	flag	varchar(4)	YES		状态标识
getpageVol	5	createdate	datetime	YES		创建时间
getpageVol	6	enddate	datetime	YES		结束时间 / 失效时间
getpageVol	7	notes	varchar(2000)	YES		备注信息
pub_restriction	1	serial_no	int(11)	NO	PRI	流水号（主键）
pub_restriction	2	desc_id	bigint(20)	NO		描述字段 ID
pub_restriction	3	desc_china	varchar(120)	NO	MUL	描述字段
pub_restriction	4	code	varchar(10)	YES	MUL	代码
pub_restriction	5	keyword	varchar(40)	NO	MUL	关键字
pub_restriction	6	is_display	char(1)	YES		是否显示（0/1/2）
pub_restriction	7	keyword_desc	varchar(80)	YES	MUL	关键字描述
pub_restriction	8	delete_state	char(1)	YES		删除状态（0 = 未删除，1 = 已删除）
pub_restriction	9	delete_time	datetime	YES		删除时间
pub_restriction	10	old_id_eqp	int(11)	YES		旧设备 ID
pub_restriction	11	old_sp	varchar(8)	YES		旧系统标识
pub_restriction	12	lan_id	varchar(10)	YES		本地网 ID
pub_restriction	13	create_date	datetime	YES		创建日期
pub_restriction	14	old_id	varchar(24)	YES		旧 ID
segm_addr_type	1	addr_type_id	int(11)	NO	PRI	地址类型 ID（主键）
segm_addr_type	2	name	varchar(255)	NO		地址类型名称
segm_addr_type	3	no	varchar(255)	YES		地址类型编号
segm_addr_type	4	level_id	int(11)	NO		级别 ID
segm_addr_type	5	score	int(11)	NO		权重分值
segm_addr_type	6	create_date	datetime	NO		创建日期
segm_addr_type	7	notes	varchar(255)	YES		备注
segm_addr_type	8	rule	char(1)	YES		规则标识
segm_addr_type	9	expression	varchar(255)	YES		规则表达式
segm_addr_type	10	version	bigint(20)	YES		版本号
spc_region	1	region_id	varchar(24)	NO	PRI	主键，关键字
spc_region	2	region_no	varchar(80)	NO	MUL	分公司编码
spc_region	3	region_name	varchar(80)	NO	MUL	分公司名称
spc_region	4	alias	varchar(80)	YES		管理区域别名
spc_region	5	grade_id	int(11)	YES		区域等级
spc_region	6	type_id	int(11)	YES		区域类型
spc_region	7	address	varchar(100)	YES		区域中心地址
spc_region	8	super_region_id	varchar(24)	YES	MUL	上级管理区域
spc_region	9	parent_id	varchar(24)	YES		父级 ID
spc_region	10	delete_state	char(1)	YES		删除状态（0 = 未删除，1 = 已删除）
spc_region	11	delete_time	datetime	YES		删除时间
spc_region	12	notes	varchar(255)	YES		备注
spc_region	13	name_ab	varchar(20)	YES		拼音缩写
spc_region	14	res_type_id	int(11)	NO	MUL	资源类型
spc_region	15	china_name_ab	varchar(40)	YES		中文名称缩写
spc_region	16	modify_op	int(11)	YES		录入人员
spc_region	17	modiry_date	datetime	YES		录入时间
spc_region	18	create_time	datetime	YES		创建时间
spc_region	19	sync_date	datetime	YES		同步时间
spc_region	20	old_id_eqp	bigint(20)	YES		旧设备 ID
spc_region	21	old_sp	varchar(8)	YES		旧系统标识
spc_region	22	lan_id	int(11)	YES	MUL	本地网 ID
spc_region	23	ppdom_id	int(11)	YES	MUL	域名 ID
spc_region	24	create_date	datetime	YES		创建日期
spc_region	25	create_op	int(11)	YES		创建人
spc_region	26	crm_region	int(11)	YES		CRM 区域
spc_region	27	crm_lan	int(11)	YES		CRM 本地网
spc_region	28	sp_region_id	varchar(80)	YES		对应服保分公司 id
spc_regional_company	1	organize_id	varchar(250)	NO	PRI	组织ID
spc_regional_company	2	organize_name	varchar(500)	YES		组织名称
spc_regional_company	3	area_code	varchar(250)	YES	UNI	组织编码
spc_regional_company	4	source	varchar(250)	YES		来源系统
spc_regional_company	5	district_id	varchar(24)	YES		地区编码
spc_regional_company	6	region_id	varchar(24)	YES		关联区域ID
spc_regional_company	7	segm_type_priv	int(11)	YES		分段类型权限编码
spc_station	1	station_id	varchar(24)	NO	PRI	主键，管理站唯一 ID
spc_station	2	station_no	varchar(80)	NO	MUL	管理站编号
spc_station	3	china_name	varchar(80)	YES		中文名称
spc_station	4	china_name_ab	varchar(40)	YES		中文名称缩写
spc_station	5	alias	varchar(80)	YES		别名
spc_station	6	name_ab	varchar(80)	YES		名称缩写
spc_station	7	code	varchar(20)	YES		编码
spc_station	8	authority	varchar(20)	YES		权限 / 管辖范围
spc_station	9	type_id	int(11)	YES		类型 ID
spc_station	10	district_id	varchar(24)	YES	MUL	区域 ID（关联行政区）
spc_station	11	region_id	varchar(24)	NO	MUL	片区 ID
spc_station	12	grade_id	int(11)	YES		等级 ID
spc_station	13	super_station_id	varchar(24)	YES	MUL	上级管理站 ID
spc_station	14	street_id	varchar(24)	YES	MUL	街道 ID
spc_station	15	doorplate	varchar(200)	YES		门牌号
spc_station	16	parent_id	varchar(24)	YES		父级 ID
spc_station	17	delete_state	char(1)	YES	MUL	删除状态（0 = 未删除，1 = 已删除）
spc_station	18	delete_time	datetime	YES	MUL	删除时间
spc_station	19	notes	varchar(255)	YES		备注
spc_station	20	opr_state_id	int(11)	YES		运营状态 ID
spc_station	21	mnt_state_id	int(11)	YES		维护状态 ID
spc_station	22	pos_x	decimal(8,4)	YES		坐标 X
spc_station	23	pos_y	decimal(8,4)	YES		坐标 Y
spc_station	24	graph_width	decimal(8,4)	YES		图形宽度
spc_station	25	graph_height	decimal(8,4)	YES		图形高度
spc_station	26	isoffset	char(1)	YES		是否偏移（0 = 否，1 = 是）
spc_station	27	graph_id	bigint(20)	YES		图形 ID
spc_station	28	childregion_id	varchar(24)	YES	MUL	子片区 ID
spc_station	29	location	varchar(200)	YES		位置描述
spc_station	30	builddate	datetime	YES		建造日期
spc_station	31	china_name_full	varchar(80)	YES		中文全称
spc_station	32	modify_op	int(11)	YES		修改人 ID
spc_station	33	modiry_date	datetime	YES	MUL	修改时间
spc_station	34	principal	varchar(20)	YES		负责人
spc_station	35	building_kind	int(11)	YES		建筑类型
spc_station	36	management_kind	int(11)	YES		管理类型
spc_station	37	prop_char_id	int(11)	YES		属性特征 ID
spc_station	38	building_area	decimal(8,2)	YES		建筑面积
spc_station	39	use_area	decimal(8,2)	YES		使用面积
spc_station	40	x	decimal(20,10)	YES		空间坐标 X
spc_station	41	y	decimal(20,10)	YES		空间坐标 Y
spc_station	42	z	decimal(20,10)	YES		空间坐标 Z
spc_station	43	floor	varchar(10)	YES		楼层
spc_station	44	create_time	datetime	YES	MUL	创建时间
spc_station	45	create_date	datetime	YES		创建日期
spc_station	46	old_id	varchar(100)	YES	MUL	旧系统 ID
spc_station	47	old_sp	varchar(100)	YES		旧系统标识
spc_station	48	link_man	varchar(100)	YES		联系人
spc_station	49	link_tele	varchar(100)	YES		联系电话
spc_station	50	imp_level	int(11)	YES		重要等级
spc_station	51	bus_level	int(11)	YES		业务等级
spc_station	52	mnt_dept	varchar(100)	YES		维护部门
spc_station	53	check_cycle	varchar(100)	YES		检查周期
spc_station	54	pow_support	varchar(100)	YES		供电支持
spc_station	55	asset_code	varchar(100)	YES		资产编码
spc_station	56	rent	varchar(100)	YES		租金
spc_station	57	mapx	decimal(20,10)	YES		地图坐标 X
spc_station	58	mapy	decimal(20,10)	YES		地图坐标 Y
spc_station	59	old_id_eqp	int(11)	YES		旧设备 ID
spc_station	60	resource_from	int(11)	YES		资源来源
spc_station	61	serial_no	int(11)	YES		序号
spc_station	62	zd_street	varchar(60)	YES		重点字段 - 街道
spc_station	63	zd_zqybm	varchar(20)	YES		重点字段 - 区域编码
spc_station	64	zd_mphm	varchar(30)	YES		重点字段 - 门牌号
spc_station	65	zd_lc	int(11)	YES		重点字段 - 楼层
spc_station	66	zd_lg	decimal(6,2)	YES		重点字段 - 量纲
spc_station	67	zd_zdmj	decimal(8,2)	YES		重点字段 - 占地面积
spc_station	68	zd_cjdw	varchar(30)	YES		重点字段 - 创建单位
spc_station	69	zd_cjr	varchar(20)	YES		重点字段 - 创建人
spc_station	70	zd_cjrq	datetime	YES		重点字段 - 创建日期
spc_station	71	zd_jgrq	datetime	YES		重点字段 - 竣工日期
spc_station	72	zd_jqbm	varchar(50)	YES		重点字段 - 交接部门
spc_station	73	zd_jqmm	varchar(50)	YES		重点字段 - 交接密码
spc_station	74	zd_gcbh	varchar(64)	YES		重点字段 - 工程编号
spc_station	75	zd_gcmc	varchar(64)	YES		重点字段 - 工程名称
spc_station	76	zd_ygcbh	varchar(64)	YES		重点字段 - 原工程编号
spc_station	77	zd_ygcmc	varchar(64)	YES		重点字段 - 原工程名称
spc_station	78	zd_sfcl	int(11)	YES		重点字段 - 是否存量
spc_station	79	isoutsh	char(1)	YES		是否审核
spc_station	80	mnt_region_id	varchar(24)	YES		维护片区 ID
spc_station	81	res_picture	varchar(400)	YES		资源图片
spc_station	82	res_type_id	int(11)	YES		资源类型 ID
spc_station	83	coverhouseholds	int(11)	YES		覆盖户数
spc_station	84	create_op	int(11)	YES		录入人员
spc_station	85	manage_type	int(11)	YES		管理站类型 (2017101 维修，2017102 安装，2017103 营业)
staff	1	staff_id	int(11)	NO	PRI	ID
staff	2	name	varchar(20)	NO		name
staff	42	logon_number	int(11)	YES		允许同时登录的工号个数
staff	43	is_allow_cs_login	int(11)	YES		是否允许登录客户端0允许1不允许
staff	44	job_id	int(11)	YES		所属岗位,取pub_restriction表中的字典值，对应：普通员工  社区经理  社区经理管理员
sync_set_addr_info	1	seq_id	decimal(24,0)	YES		同步序列号
sync_set_addr_info	2	set_segm_id	varchar(24)	NO	MUL	需同步的安装地址 id
sync_set_addr_info	3	old_segm_id	varchar(24)	YES	MUL	对应的旧标准地址 id
sync_set_addr_info	4	new_segm_id	varchar(24)	NO	MUL	对应的新标准地址 id
sync_set_addr_info	5	sync_update_type	int(11)	YES		同步状态：0 = 待更新、-1 = 更新失败、-2 = 数据冗余
sync_set_addr_info	6	create_date	datetime	YES		创建时间
sync_set_addr_info	7	cust_id	varchar(24)	YES		客户 ID
sync_set_addr_info	8	return_message	varchar(500)	YES		同步返回信息 / 错误说明
```

## 5. 大表抽样观测

```text
table_name	metric_type	field_name	field_value	metric_count
ADDR_SEGM	sample_meta	sample_rule	crc32(segm_id)%100=0	23289
ADDR_SEGM	sample_meta	total_rows	-	2318000
ADDR_SEGM	null_rate	parent_segm_id	NON_EMPTY	23264
ADDR_SEGM	null_rate	parent_segm_id	NULL_OR_EMPTY	25
ADDR_SEGM	null_rate	region_id	NON_EMPTY	23289
ADDR_SEGM	null_rate	station_id	NULL_OR_EMPTY	1730
ADDR_SEGM	null_rate	station_id	NON_EMPTY	21559
ADDR_SEGM	null_rate	installstation_id	NULL_OR_EMPTY	23289
ADDR_SEGM	null_rate	busstation_id	NULL_OR_EMPTY	23289
ADDR_SEGM	dist	segm_type	180007	16819
ADDR_SEGM	dist	segm_type	180006	4057
ADDR_SEGM	dist	segm_type	180095	984
ADDR_SEGM	dist	segm_type	180009	661
ADDR_SEGM	dist	segm_type	180013	633
ADDR_SEGM	dist	segm_type	180005	93
ADDR_SEGM	dist	segm_type	180004	23
ADDR_SEGM	dist	segm_type	180010	15
ADDR_SEGM	dist	segm_type	180003	2
ADDR_SEGM	dist	segm_type	180099	1
ADDR_SEGM	dist	segm_type	180012	1
ADDR_SEGM	dist	status	2140900	23289
ADDR_SEGM	dist	delete_state	0	22126
ADDR_SEGM	dist	delete_state	1	1163
ADDR_SEGM	dist	outside	1	23289
ADDR_SEGM	dist	addr_type	2140500	17281
ADDR_SEGM	dist	addr_type	2140001	5354
ADDR_SEGM	dist	addr_type	null	654
ADDR_SEGM	dist	place_type	2140800	14945
ADDR_SEGM	dist	place_type	null	8189
ADDR_SEGM	dist	place_type	2140802	88
ADDR_SEGM	dist	place_type	2140801	54
ADDR_SEGM	dist	place_type	2140803	12
ADDR_SEGM	dist	place_type	2140806	1
ADDR_SEGM	dist	opr_state	2141002	23172
ADDR_SEGM	dist	opr_state	2141000	101
ADDR_SEGM	dist	opr_state	2141003	16
ADDR_SEGM	dist	area_type	null	23109
ADDR_SEGM	dist	area_type	2140511	180
ADDR_SEGM	dist	addr_unit_type	null	23040
ADDR_SEGM	dist	addr_unit_type	2140521	221
ADDR_SEGM	dist	addr_unit_type	2140522	28
ADDR_SEGM	dist	addr_from	NULL	21869
ADDR_SEGM	dist	addr_from	前台选址生成	646
ADDR_SEGM	dist	addr_from	批量导入	535
ADDR_SEGM	dist	addr_from	后端录入	239
ADDR_SET_SEGM	sample_meta	sample_rule	crc32(set_addr_id)%100=0	1023
ADDR_SET_SEGM	sample_meta	total_rows	-	100000
ADDR_SET_SEGM	null_rate	segm_id	NON_EMPTY	1023
ADDR_SET_SEGM	null_rate	region_id	NON_EMPTY	1023
ADDR_SET_SEGM	null_rate	area_id	NON_NULL	48
ADDR_SET_SEGM	null_rate	area_id	NULL	975
ADDR_SET_SEGM	null_rate	org_id	NULL_OR_EMPTY	975
ADDR_SET_SEGM	null_rate	org_id	NON_EMPTY	48
ADDR_SET_SEGM	dist	set_type	null	975
ADDR_SET_SEGM	dist	set_type	0	48
ADDR_SET_SEGM	dist	status	null	975
ADDR_SET_SEGM	dist	status	2140900	48
ADDR_SET_SEGM	dist	delete_state	0	1014
ADDR_SET_SEGM	dist	delete_state	1	9
ADDR_SET_SEGM	dist	segm_type	180007	1012
ADDR_SET_SEGM	dist	segm_type	180013	11
```
