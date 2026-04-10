SET NAMES utf8mb4;

-- ftth_cloud_address 重建 DDL（TiDB/MySQL 兼容版）
-- 生成日期：2026-03-27
-- 生成依据：
-- 1. /Users/criswu/Desktop/广电/历史数据/标准地址模块/DDL/地址表建表sql
-- 2. /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/tmp/history-inventory/expected_schema_from_dict.json
-- 3. /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/tmp/history-inventory/ftth_cloud_address_comments_merged_20260326.sql
-- 表名口径：标准地址表使用文档名 `ADDR_SEGM`，安装地址表使用文档名 `ADDR_SET_SEGM`。
-- 类型映射：VARCHAR2 -> varchar，CHAR(1) -> char(1)，CHAR(n>1) -> varchar(n)，DATE -> datetime，NUMBER -> int/bigint/decimal。
-- 说明：Oracle TABLESPACE / STORAGE / CACHE / SUPPLEMENTAL LOG GROUP 未迁移；ADDR_SEGM 分区、表达式索引、触发器、外键拆到了 optional 脚本。

USE `ftth_cloud_address`;

-- 一、建表

CREATE TABLE `pub_restriction` (
    `serial_no` int NOT NULL COMMENT '流水号（主键）',
    `desc_id` bigint NOT NULL COMMENT '描述字段 ID',
    `desc_china` varchar(120) NOT NULL COMMENT '描述字段',
    `code` varchar(10) NULL COMMENT '代码',
    `keyword` varchar(40) NOT NULL COMMENT '关键字',
    `is_display` char(1) NULL DEFAULT '1' COMMENT '是否显示（0/1/2）',
    `keyword_desc` varchar(80) NULL COMMENT '关键字描述',
    `delete_state` char(1) NULL DEFAULT '0' COMMENT '删除状态（0 = 未删除，1 = 已删除）',
    `delete_time` datetime NULL COMMENT '删除时间',
    `old_id_eqp` int NULL COMMENT '旧设备 ID',
    `old_sp` varchar(8) NULL COMMENT '旧系统标识',
    `lan_id` varchar(10) NULL COMMENT '本地网 ID',
    `create_date` datetime NULL COMMENT '创建日期',
    `old_id` varchar(24) NULL COMMENT '旧 ID',
    CONSTRAINT `CK_PUB_RESTRICTION_DELETE_STAT` CHECK (`is_display` is null or ( `is_display` in ('0','1','2' ))),
    PRIMARY KEY (`serial_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='约束关系';

CREATE TABLE `segm_addr_type` (
    `addr_type_id` int NOT NULL COMMENT '地址类型 ID（主键）',
    `name` varchar(255) NOT NULL COMMENT '地址类型名称',
    `no` varchar(255) NULL COMMENT '地址类型编号',
    `level_id` int NOT NULL COMMENT '级别 ID',
    `score` int NOT NULL COMMENT '权重分值',
    `create_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建日期',
    `notes` varchar(255) NULL COMMENT '备注',
    `rule` char(1) NULL COMMENT '规则标识',
    `expression` varchar(255) NULL COMMENT '规则表达式',
    `version` bigint NULL COMMENT '版本号',
    PRIMARY KEY (`addr_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地址类型表';

CREATE TABLE `spc_region` (
    `region_id` varchar(24) NOT NULL COMMENT '主键，关键字',
    `region_no` varchar(80) NOT NULL COMMENT '分公司编码',
    `region_name` varchar(80) NOT NULL COMMENT '分公司名称',
    `alias` varchar(80) NULL COMMENT '管理区域别名',
    `grade_id` int NULL COMMENT '区域等级',
    `type_id` int NULL COMMENT '区域类型',
    `address` varchar(100) NULL COMMENT '区域中心地址',
    `super_region_id` varchar(24) NULL COMMENT '上级管理区域',
    `parent_id` varchar(24) NULL COMMENT '父级 ID',
    `delete_state` char(1) NULL DEFAULT '0' COMMENT '删除状态（0 = 未删除，1 = 已删除）',
    `delete_time` datetime NULL COMMENT '删除时间',
    `notes` varchar(255) NULL COMMENT '备注',
    `name_ab` varchar(20) NULL COMMENT '拼音缩写',
    `res_type_id` int NOT NULL DEFAULT 200 COMMENT '资源类型',
    `china_name_ab` varchar(40) NULL COMMENT '中文名称缩写',
    `modify_op` int NULL COMMENT '录入人员',
    `modiry_date` datetime NULL COMMENT '录入时间',
    `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `sync_date` datetime NULL COMMENT '同步时间',
    `old_id_eqp` bigint NULL COMMENT '旧设备 ID',
    `old_sp` varchar(8) NULL COMMENT '旧系统标识',
    `lan_id` int NULL COMMENT '本地网 ID',
    `ppdom_id` int NULL COMMENT '域名 ID',
    `create_date` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建日期',
    `create_op` int NULL COMMENT '创建人',
    `crm_region` int NULL COMMENT 'CRM 区域',
    `crm_lan` int NULL COMMENT 'CRM 本地网',
    `sp_region_id` varchar(80) NULL COMMENT '对应服保分公司 id',
    CONSTRAINT `CK_SPC_REGION_DELETE_STATE` CHECK (`delete_state` is null or ( `delete_state` in ('0','1') )),
    PRIMARY KEY (`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分公司';

CREATE TABLE `spc_regional_company` (
    `organize_id` varchar(250) NOT NULL COMMENT '组织ID',
    `organize_name` varchar(500) NULL COMMENT '组织名称',
    `area_code` varchar(250) NULL COMMENT '组织编码',
    `source` varchar(250) NULL COMMENT '来源系统',
    `district_id` varchar(24) NULL COMMENT '地区编码',
    `region_id` varchar(24) NULL COMMENT '关联区域ID',
    `segm_type_priv` int NULL COMMENT '分段类型权限编码',
    PRIMARY KEY (`organize_id`),
    CONSTRAINT `SPC_REGIONAL_COMPANY_UNIQUE` UNIQUE (`area_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织映射表';

CREATE TABLE `spc_station` (
    `station_id` varchar(24) NOT NULL COMMENT '主键，管理站唯一 ID',
    `station_no` varchar(80) NOT NULL COMMENT '管理站编号',
    `china_name` varchar(80) NULL COMMENT '中文名称',
    `china_name_ab` varchar(40) NULL COMMENT '中文名称缩写',
    `alias` varchar(80) NULL COMMENT '别名',
    `name_ab` varchar(80) NULL COMMENT '名称缩写',
    `code` varchar(20) NULL COMMENT '编码',
    `authority` varchar(20) NULL COMMENT '权限 / 管辖范围',
    `type_id` int NULL COMMENT '类型 ID',
    `district_id` varchar(24) NULL COMMENT '区域 ID（关联行政区）',
    `region_id` varchar(24) NOT NULL COMMENT '片区 ID',
    `grade_id` int NULL COMMENT '等级 ID',
    `super_station_id` varchar(24) NULL COMMENT '上级管理站 ID',
    `street_id` varchar(24) NULL COMMENT '街道 ID',
    `doorplate` varchar(200) NULL COMMENT '门牌号',
    `parent_id` varchar(24) NULL COMMENT '父级 ID',
    `delete_state` char(1) NULL DEFAULT '0' COMMENT '删除状态（0 = 未删除，1 = 已删除）',
    `delete_time` datetime NULL COMMENT '删除时间',
    `notes` varchar(255) NULL COMMENT '备注',
    `opr_state_id` int NULL COMMENT '运营状态 ID',
    `mnt_state_id` int NULL COMMENT '维护状态 ID',
    `pos_x` decimal(8,4) NULL COMMENT '坐标 X',
    `pos_y` decimal(8,4) NULL COMMENT '坐标 Y',
    `graph_width` decimal(8,4) NULL COMMENT '图形宽度',
    `graph_height` decimal(8,4) NULL COMMENT '图形高度',
    `isoffset` char(1) NULL DEFAULT '1' COMMENT '是否偏移（0 = 否，1 = 是）',
    `graph_id` bigint NULL COMMENT '图形 ID',
    `childregion_id` varchar(24) NULL COMMENT '子片区 ID',
    `location` varchar(200) NULL COMMENT '位置描述',
    `builddate` datetime NULL COMMENT '建造日期',
    `china_name_full` varchar(80) NULL COMMENT '中文全称',
    `modify_op` int NULL COMMENT '修改人 ID',
    `modiry_date` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `principal` varchar(20) NULL COMMENT '负责人',
    `building_kind` int NULL COMMENT '建筑类型',
    `management_kind` int NULL COMMENT '管理类型',
    `prop_char_id` int NULL COMMENT '属性特征 ID',
    `building_area` decimal(8,2) NULL COMMENT '建筑面积',
    `use_area` decimal(8,2) NULL COMMENT '使用面积',
    `x` decimal(20,10) NULL COMMENT '空间坐标 X',
    `y` decimal(20,10) NULL COMMENT '空间坐标 Y',
    `z` decimal(20,10) NULL COMMENT '空间坐标 Z',
    `floor` varchar(10) NULL COMMENT '楼层',
    `create_time` datetime NULL COMMENT '创建时间',
    `create_date` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建日期',
    `old_id` varchar(100) NULL COMMENT '旧系统 ID',
    `old_sp` varchar(100) NULL COMMENT '旧系统标识',
    `link_man` varchar(100) NULL COMMENT '联系人',
    `link_tele` varchar(100) NULL COMMENT '联系电话',
    `imp_level` int NULL COMMENT '重要等级',
    `bus_level` int NULL COMMENT '业务等级',
    `mnt_dept` varchar(100) NULL COMMENT '维护部门',
    `check_cycle` varchar(100) NULL COMMENT '检查周期',
    `pow_support` varchar(100) NULL COMMENT '供电支持',
    `asset_code` varchar(100) NULL COMMENT '资产编码',
    `rent` varchar(100) NULL COMMENT '租金',
    `mapx` decimal(20,10) NULL COMMENT '地图坐标 X',
    `mapy` decimal(20,10) NULL COMMENT '地图坐标 Y',
    `old_id_eqp` int NULL COMMENT '旧设备 ID',
    `resource_from` int NULL COMMENT '资源来源',
    `serial_no` int NULL COMMENT '序号',
    `zd_street` varchar(60) NULL COMMENT '重点字段 - 街道',
    `zd_zqybm` varchar(20) NULL COMMENT '重点字段 - 区域编码',
    `zd_mphm` varchar(30) NULL COMMENT '重点字段 - 门牌号',
    `zd_lc` int NULL COMMENT '重点字段 - 楼层',
    `zd_lg` decimal(6,2) NULL COMMENT '重点字段 - 量纲',
    `zd_zdmj` decimal(8,2) NULL COMMENT '重点字段 - 占地面积',
    `zd_cjdw` varchar(30) NULL COMMENT '重点字段 - 创建单位',
    `zd_cjr` varchar(20) NULL COMMENT '重点字段 - 创建人',
    `zd_cjrq` datetime NULL COMMENT '重点字段 - 创建日期',
    `zd_jgrq` datetime NULL COMMENT '重点字段 - 竣工日期',
    `zd_jqbm` varchar(50) NULL COMMENT '重点字段 - 交接部门',
    `zd_jqmm` varchar(50) NULL COMMENT '重点字段 - 交接密码',
    `zd_gcbh` varchar(64) NULL COMMENT '重点字段 - 工程编号',
    `zd_gcmc` varchar(64) NULL COMMENT '重点字段 - 工程名称',
    `zd_ygcbh` varchar(64) NULL COMMENT '重点字段 - 原工程编号',
    `zd_ygcmc` varchar(64) NULL COMMENT '重点字段 - 原工程名称',
    `zd_sfcl` int NULL COMMENT '重点字段 - 是否存量',
    `isoutsh` char(1) NULL COMMENT '是否审核',
    `mnt_region_id` varchar(24) NULL COMMENT '维护片区 ID',
    `res_picture` varchar(400) NULL COMMENT '资源图片',
    `res_type_id` int NULL COMMENT '资源类型 ID',
    `coverhouseholds` int NULL COMMENT '覆盖户数',
    `create_op` int NULL COMMENT '录入人员',
    `manage_type` int NULL COMMENT '管理站类型 (2017101 维修，2017102 安装，2017103 营业)',
    CONSTRAINT `CKC_ISOFFSET_SPC_STAT` CHECK (`isoffset` is null or ( `isoffset` in ('0','1') )),
    CONSTRAINT `CK_SPC_STATION_DELETE_STATE` CHECK (`delete_state` is null or ( `delete_state` in ('0','1') )),
    PRIMARY KEY (`station_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理站表';

CREATE TABLE `staff` (
    `staff_id` int NOT NULL COMMENT '员工ID',
    `name` varchar(20) NOT NULL COMMENT '员工姓名',
    `title` int NULL COMMENT '职务编码',
    `station` char(1) NULL COMMENT '所属站点标识',
    `password` varchar(64) NOT NULL COMMENT '登录密码',
    `department` char(1) NULL COMMENT '所属部门标识',
    `workgroup_id` int NULL COMMENT '工作组ID',
    `site_id` bigint NULL COMMENT '站点ID',
    `dept_level_id` int NULL COMMENT '部门层级ID',
    `eff_date` datetime NULL COMMENT '生效时间',
    `exp_date` datetime NULL COMMENT '失效时间',
    `state` varchar(3) NOT NULL COMMENT '状态',
    `state_date` datetime NULL COMMENT '状态时间',
    `ip_limit` char(1) NULL COMMENT 'IP限制标识',
    `ip_subnet` varchar(16) NULL COMMENT 'IP子网',
    `ip_netmask` varchar(16) NULL COMMENT 'IP子网掩码',
    `ip_hostname` varchar(50) NULL COMMENT 'IP主机名',
    `pc_limit` int NULL COMMENT '终端数量限制',
    `pc_id` int NULL COMMENT '终端ID',
    `invoice_serial_nbr` int NULL COMMENT '发票流水号',
    `last_date` datetime NULL COMMENT '最近操作时间',
    `email` varchar(80) NULL COMMENT '邮箱',
    `phone_no` varchar(24) NULL COMMENT '联系电话',
    `delete_state` char(1) NULL DEFAULT '0' COMMENT '删除状态',
    `delete_time` datetime NULL COMMENT '删除时间',
    `remark` varchar(200) NULL COMMENT '备注',
    `staff_alias` varchar(40) NULL COMMENT '员工别名',
    `login_name` varchar(20) NULL COMMENT '登录名',
    `pwd_mod_date` datetime NULL COMMENT '密码修改时间',
    `lan_id` varchar(10) NULL COMMENT '本地网标识',
    `loginuid` varchar(80) NULL COMMENT '登录用户标识',
    `staff_code` varchar(30) NULL COMMENT '员工编码',
    `staff_state` decimal(20,0) NULL COMMENT '员工状态',
    `last_login_date` datetime NULL COMMENT '最近登录时间',
    `erro_times` decimal(20,0) NULL COMMENT '登录失败次数',
    `captcha` varchar(6) NULL COMMENT '验证码',
    `validate_time` datetime NULL COMMENT '验证时间',
    `his_pwd` varchar(256) NULL COMMENT '历史密码',
    `safe_strategy` int NULL COMMENT '安全策略',
    `identity_id` varchar(18) NULL COMMENT '身份证号',
    `hr_code` varchar(25) NULL COMMENT '人力资源编码',
    `logon_number` int NULL COMMENT '允许同时登录的工号个数',
    `is_allow_cs_login` int NULL COMMENT '是否允许登录客户端0允许1不允许',
    `job_id` int NULL COMMENT '所属岗位,取pub_restriction表中的字典值，对应：普通员工  社区经理  社区经理管理员',
    `allow_batch_adjust_parent_addr` varchar(1) NULL COMMENT '是否允许批量调整父地址',
    `region_id` varchar(24) NULL COMMENT '区域ID',
    `addr_batch_oper_permission` varchar(1) NULL COMMENT '地址批量操作权限',
    `opt_node_permission` varchar(1) NULL COMMENT '光节点操作权限',
    PRIMARY KEY (`staff_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工信息表';

CREATE TABLE `ADDR_SEGM` (
    `segm_id` varchar(24) NOT NULL COMMENT '分段地址 ID 即标准地址 ID（主键）',
    `segm_type` int NULL COMMENT '分段地址级别',
    `segm_name` varchar(200) NULL COMMENT '分段地址名称',
    `segm_no` varchar(120) NULL COMMENT '分段地址简拼',
    `parent_segm_id` varchar(24) NULL COMMENT '上级分段地址',
    `outside` int NULL DEFAULT 1 COMMENT '是否展示给前台 0 为是 1 为否',
    `status` int NULL COMMENT '状态',
    `time` datetime NULL COMMENT '变动时间',
    `outregion` int NULL COMMENT '区外',
    `post_code` varchar(255) NULL COMMENT '单项工程编号',
    `is_city` char(1) NULL COMMENT '是否城市',
    `is_band` char(1) NULL COMMENT '是否宽带到户',
    `is_user` char(1) NULL COMMENT '是否为户线',
    `stand_name` varchar(400) NULL COMMENT '标准地址全称',
    `stand_no` varchar(300) NULL COMMENT '标准地址简拼',
    `region_id` varchar(24) NULL COMMENT '所属管理区域',
    `template_id` int NULL COMMENT '模板 ID',
    `notes` varchar(255) NULL COMMENT '备注',
    `delete_state` char(1) NULL DEFAULT '0' COMMENT '删除状态 0 = 未删除 1 = 已删除',
    `delete_time` datetime NULL COMMENT '删除时间',
    `old_data` decimal(20,0) NULL DEFAULT 0 COMMENT '是否旧数据',
    `modify_op` bigint NULL COMMENT '录入人员',
    `modiry_date` datetime NULL COMMENT '录入时间',
    `alias` varchar(80) NULL COMMENT '别名',
    `sync_date` datetime NULL COMMENT '同步时间',
    `create_time` datetime NULL COMMENT '创建时间',
    `is4gis` char(1) NULL DEFAULT '0' COMMENT '是否 GIS 专用',
    `service_region_id` varchar(24) NULL COMMENT '所属社区',
    `lan_id` decimal(20,0) NULL COMMENT '所属本地网',
    `ppdom_id` decimal(20,0) NULL COMMENT '域 ID',
    `old_id_eqp` bigint NULL COMMENT '旧设备 ID',
    `old_sp` varchar(8) NULL COMMENT '旧系统标识',
    `addr_from` varchar(200) NULL COMMENT '地址来源',
    `is_highclass_area` char(1) NULL COMMENT '是否高档住宅或政府小区',
    `create_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建日期',
    `create_op` bigint NULL COMMENT '创建人',
    `border_address` varchar(24) NULL COMMENT '边界地址',
    `x` decimal(20,10) NULL COMMENT '坐标 X',
    `y` decimal(20,10) NULL COMMENT '坐标 Y',
    `exp_date` datetime NULL COMMENT '到期时间',
    `addr_type` int NULL DEFAULT 2140001 COMMENT '地址类型',
    `is_compete_region` char(1) NULL COMMENT '是否竞争区域',
    `is_compete_modify_op` int NULL COMMENT '修改竞争区域的操作人',
    `is_compete_modify_date` datetime NULL COMMENT '竞争区域的修改时间',
    `build_units` decimal(20,0) NULL COMMENT '单元数',
    `build_floors` decimal(20,0) NULL COMMENT '层数',
    `floor_height` decimal(4,1) NULL COMMENT '层高',
    `isbuilding` char(1) NOT NULL DEFAULT '0' COMMENT '是否为楼宇 0 = 否 1 = 是',
    `is_normal_addr` char(1) NULL COMMENT '是否标准地址',
    `is_instal_addr` char(1) NULL DEFAULT '0' COMMENT '是否可安装地址',
    `mnt_type_id` int NULL COMMENT '维护方式：自维、代维',
    `optic_node` varchar(200) NULL COMMENT '光节点',
    `district_id` varchar(24) NULL COMMENT '行政区域',
    `location_id` varchar(24) NULL COMMENT '地块 ID',
    `is_key` int NULL COMMENT '是否主用名：0 - 否，1 - 是',
    `is_instead_indoor_devices` char(1) NULL COMMENT '是否代装室内设备',
    `is_support_cm` char(1) NULL COMMENT '是否支持 CM',
    `is_support_eoc` char(1) NULL COMMENT '是否支持 EOC',
    `net_struct_id` int NULL COMMENT '网络结构 ID',
    `is_support_ipqqm` char(1) NULL COMMENT '是否支持 IP 机顶盒',
    `line_extension_fee` decimal(10,2) NULL COMMENT '线路扩容费',
    `is_line_install` char(1) NULL COMMENT '是否线路安装',
    `net_area_type_id` int NULL COMMENT '网络区域类型 ID',
    `build_grade_id` int NULL COMMENT '建筑等级 ID',
    `build_type_id` int NULL COMMENT '建筑类型 ID',
    `citycom_flag` int NULL COMMENT '城域标识',
    `segm_name_fif` varchar(24) NULL COMMENT '地址名称扩展',
    `segm_name_fir` char(1) NULL COMMENT '是否配套费小区',
    `station_id` varchar(24) NULL COMMENT '地址所属维修管理站',
    `addr_in_type` bigint NULL COMMENT '地址接入方式（旧）',
    `place_type` int NULL COMMENT '场所性质',
    `opr_state` bigint NULL COMMENT '审核状态',
    `cover_num` int NULL COMMENT '楼栋覆盖户数',
    `area_manager` varchar(60) NULL COMMENT '片区经理',
    `installstation_id` varchar(24) NULL COMMENT '地址所属安装管理站',
    `busstation_id` varchar(24) NULL COMMENT '地址所属营业管理站',
    `addr_in_type_ftth` int NULL COMMENT '光纤接入方式',
    `ftth_pon_type` int NULL COMMENT '光纤接入能力',
    `addr_in_type_lan` int NULL COMMENT '电缆接入方式',
    `area_type` int NULL COMMENT '城乡属性',
    `addr_unit_type` int NULL COMMENT '房屋属性',
    `addr_def` varchar(255) NULL COMMENT '房屋定义',
    PRIMARY KEY (`segm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准分段地址';

CREATE TABLE `ADDR_SET_SEGM` (
    `set_addr_id` varchar(24) NOT NULL COMMENT '安装地址 ID（主键）',
    `set_addr_name` varchar(400) NULL COMMENT '安装地址名称',
    `set_type` int NULL COMMENT '地址类型：0 = 伪地址，2 = 到户地址',
    `segm_id` varchar(24) NULL COMMENT '标准地址分段 ID',
    `status` int NULL COMMENT '状态',
    `region_id` varchar(24) NULL COMMENT '所属区域 ID',
    `notes` varchar(2000) NULL COMMENT '备注',
    `delete_state` char(1) NULL DEFAULT '0' COMMENT '删除状态（0 = 未删除，1 = 已删除）',
    `delete_time` datetime NULL COMMENT '删除时间',
    `modiry_date` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `alias` varchar(80) NULL COMMENT '别名',
    `create_date` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建日期',
    `boss_op` varchar(80) NULL COMMENT 'BOSS 操作人',
    `segm_type` int NULL COMMENT '地址分段类型',
    `area_id` bigint NULL COMMENT '片区 ID',
    `set_addr_no` varchar(120) NULL COMMENT '安装地址编号',
    `synchronous_date` datetime NULL COMMENT '同步时间',
    `old_segm_id` varchar(24) NULL COMMENT '旧标准地址 ID',
    `modify_op` int NULL COMMENT '修改人',
    `org_id` varchar(80) NULL COMMENT '组织机构 ID',
    PRIMARY KEY (`set_addr_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安装地址表';

CREATE TABLE `getpageVol` (
    `uuid` varchar(64) NOT NULL COMMENT '主键，唯一标识 ID',
    `returnval` varchar(3000) NULL COMMENT '返回值 / 选址结果信息',
    `systemsource` varchar(10) NULL COMMENT '系统来源',
    `flag` varchar(4) NULL COMMENT '状态标识',
    `createdate` datetime NULL COMMENT '创建时间',
    `enddate` datetime NULL COMMENT '结束时间 / 失效时间',
    `notes` varchar(2000) NULL COMMENT '备注信息',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='选址信息表';

CREATE TABLE `sync_set_addr_info` (
    `seq_id` decimal(24,0) NULL DEFAULT 0 COMMENT '同步序列号',
    `set_segm_id` varchar(24) NOT NULL COMMENT '需同步的安装地址 id',
    `old_segm_id` varchar(24) NULL DEFAULT '0' COMMENT '对应的旧标准地址 id',
    `new_segm_id` varchar(24) NOT NULL COMMENT '对应的新标准地址 id',
    `sync_update_type` int NULL DEFAULT 0 COMMENT '同步状态：0 = 待更新、-1 = 更新失败、-2 = 数据冗余',
    `create_date` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `cust_id` varchar(24) NULL DEFAULT '0' COMMENT '客户 ID',
    `return_message` varchar(500) NULL DEFAULT '' COMMENT '同步返回信息 / 错误说明'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地址变更同步表';

-- 二、二级索引

-- pub_restriction
CREATE INDEX `IDX_PUB_RESTRICTION_CODE` ON `pub_restriction` (`code`);
CREATE INDEX `IDX_PUB_RESTRICTION_DESC_CHINA` ON `pub_restriction` (`desc_china`);
CREATE INDEX `IDX_PUB_RESTRICTION_KEYWORD` ON `pub_restriction` (`keyword`);
CREATE INDEX `IDX_PUB_RESTRICTION_KEYDESC` ON `pub_restriction` (`keyword_desc`);

-- spc_region
CREATE INDEX `IDX_SPC_REGION_TT` ON `spc_region` (`region_id`, `parent_id`, `region_name`, `res_type_id`);
CREATE INDEX `IDX_SPC_REGION_RES_TYPE_ID` ON `spc_region` (`res_type_id`);
CREATE INDEX `IDX_SPC_REGION_PPDOM_ID` ON `spc_region` (`ppdom_id`);
CREATE INDEX `IDX_SPC_REGION_LAN_ID` ON `spc_region` (`lan_id`);
CREATE INDEX `REGION_NO` ON `spc_region` (`region_no`, `delete_state`, `delete_time`);
CREATE INDEX `IDX_SPC_REGION_REGION_NAME` ON `spc_region` (`region_name`);
CREATE INDEX `IDX_SPC_REGION_SUPER_REGION_ID` ON `spc_region` (`super_region_id`);

-- spc_station
CREATE INDEX `IDX_SPC_STATION_SQL` ON `spc_station` (`station_id`, `station_no`, `china_name`);
CREATE INDEX `IDX_SPC_STATION_DISTRICT_ID` ON `spc_station` (`district_id`);
CREATE INDEX `IDX_SPC_STATION_DELETE_STATE` ON `spc_station` (`delete_state`);
CREATE INDEX `IDX_SPC_STATION_DELETE` ON `spc_station` (`delete_time`);
CREATE INDEX `IDX_SPC_STATION_CREATE` ON `spc_station` (`create_time`);
CREATE INDEX `IDX_SPC_STATION_CHILDREGION_ID` ON `spc_station` (`childregion_id`);
CREATE INDEX `IDX_SPC_STATION_STREET_ID` ON `spc_station` (`street_id`);
CREATE INDEX `IDX_SPC_STATION_STATION_ID` ON `spc_station` (`super_station_id`);
CREATE INDEX `IDX_SPC_STATION_REGION_ID` ON `spc_station` (`region_id`);
CREATE INDEX `IDX_SPC_STATION_OLD_ID` ON `spc_station` (`old_id`);
CREATE INDEX `IDX_SPC_STATION_MODIRY` ON `spc_station` (`modiry_date`);
CREATE INDEX `UQ_SPC_STATION_STATION_NO` ON `spc_station` (`station_no`, `delete_time`);

-- staff
CREATE INDEX `IDX_WORKGROUP_ID` ON `staff` (`workgroup_id`);

-- ADDR_SEGM
CREATE INDEX `IDX_ADDR_SEG_T` ON `ADDR_SEGM` (`segm_id`, `parent_segm_id`, `stand_name`);
CREATE INDEX `IDX_ADDR_SEGM_DELETE_STATE` ON `ADDR_SEGM` (`delete_state`);
CREATE INDEX `IDX_ADDR_SEGM_LOCATION` ON `ADDR_SEGM` (`location_id`, `delete_state`);
CREATE INDEX `IDX_ADDR_SEGM_PARENT` ON `ADDR_SEGM` (`parent_segm_id`);
CREATE INDEX `IDX_ADDR_SEGM_REGION_ID` ON `ADDR_SEGM` (`region_id`);
CREATE INDEX `IDX_ADDR_SEGM_SEGM_NAME` ON `ADDR_SEGM` (`segm_name`);
CREATE INDEX `IDX_ADDR_SEGM_SEGM_NO` ON `ADDR_SEGM` (`segm_no`);
CREATE INDEX `IDX_ADDR_SEGM_SEGM_TYPE` ON `ADDR_SEGM` (`segm_type`);
CREATE INDEX `IDX_ADDR_SEGM_STAND` ON `ADDR_SEGM` (`stand_name`);
CREATE INDEX `IDX_CREATE_DATE` ON `ADDR_SEGM` (`create_date`);
CREATE INDEX `IDX_DELETE_TIME` ON `ADDR_SEGM` (`delete_time`);
CREATE INDEX `IDX_MODIRY_DATE` ON `ADDR_SEGM` (`modiry_date`);

-- ADDR_SET_SEGM
CREATE INDEX `IDX_ADDR_SET_SEGM_S` ON `ADDR_SET_SEGM` (`set_addr_name`);
CREATE INDEX `IDX_ADDR_SET_SEGM_T` ON `ADDR_SET_SEGM` (`set_type`);
CREATE INDEX `IDX_ADDR_SET_SEGM_I` ON `ADDR_SET_SEGM` (`segm_id`);
CREATE INDEX `IDX_ADDR_SET_SEGM_D` ON `ADDR_SET_SEGM` (`delete_state`);
CREATE INDEX `PK_SET_ADDR_SEGM_TYPE` ON `ADDR_SET_SEGM` (`segm_type`);
CREATE INDEX `IDX_PUN4` ON `ADDR_SET_SEGM` (`region_id`);
CREATE INDEX `PK_SET_ADDR_SEGM_STATUS` ON `ADDR_SET_SEGM` (`status`);
CREATE INDEX `PK_SET_ADDR_SEGM_CREATE_DATE` ON `ADDR_SET_SEGM` (`create_date`);
CREATE INDEX `PK_SET_ADDR_NO` ON `ADDR_SET_SEGM` (`set_addr_no`);
CREATE INDEX `IDX_ADDR_SET_SEGM_MODIRY_DATE` ON `ADDR_SET_SEGM` (`modiry_date`);

-- sync_set_addr_info
CREATE INDEX `IDX_NEW_SEGM_ID` ON `sync_set_addr_info` (`new_segm_id`);
CREATE INDEX `IDX_OLD_SEGM_ID` ON `sync_set_addr_info` (`old_segm_id`);
CREATE INDEX `IDX_SET_SEGM_ID` ON `sync_set_addr_info` (`set_segm_id`);
