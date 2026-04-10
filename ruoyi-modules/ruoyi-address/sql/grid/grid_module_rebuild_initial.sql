SET NAMES utf8mb4;

-- 网格模块初版重建 DDL（基于样例、字段注释与装载 SQL 推断）
-- 说明：当前为第一轮自动推断结果，主键/非空/索引仍可能需要结合现场库或甲方最终口径再收敛。

USE `ftth_cloud_address`;

CREATE TABLE `grid_TOJF` (
    `GRID_ID` bigint NOT NULL COMMENT '网格id',
    `GRID_NAME` varchar(32) NOT NULL COMMENT '网格名称',
    `GRID_CODE` varchar(32) NOT NULL,
    `GRID_TYPE` varchar(8) NOT NULL COMMENT '网格类型',
    `GRID_COVER_TYPE` varchar(8) NULL,
    `GRID_CUS_TYPE` varchar(8) NOT NULL,
    `STATE` varchar(8) NOT NULL COMMENT '网格状态',
    `BUSINESS_ID` int NOT NULL COMMENT '网格公司id',
    `BUSINESS_NAME` varchar(32) NOT NULL COMMENT '网格公司名称',
    `REGION_ID` int NOT NULL COMMENT '区域id',
    `REGION_NAME` varchar(32) NOT NULL COMMENT '区域名称',
    `DEPT_ID` int NOT NULL COMMENT '部门id',
    `DEPT_NAME` varchar(32) NOT NULL COMMENT '部门名称',
    `TEAM_ID` int NOT NULL COMMENT '团队id',
    `TEAM_NAME` varchar(32) NOT NULL COMMENT '团队名称',
    `MGR_ID` int NULL COMMENT '网格经理id',
    `MGR_NAME` varchar(32) NULL COMMENT '网格经理名称',
    `ETL_CYCLE` int NOT NULL COMMENT '数据日期',
    `ETL_DATE` datetime NOT NULL COMMENT '推送日期',
    `REGION_MGR_NAME` varchar(32) NULL COMMENT '区域经理名称',
    PRIMARY KEY (`GRID_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网格信息表';

CREATE TABLE `grid_manager_tojf` (
    `PARTY_ROLE_ID` int NOT NULL COMMENT '人员id（网格经理id）',
    `STAFF_NAME` varchar(32) NOT NULL COMMENT '人员名称（网格经理名称）',
    `STAFF_DESC` varchar(32) NULL COMMENT '描述',
    `MOBILE_PHONE` varchar(16) NULL COMMENT '电话',
    `ORG_ID` int NOT NULL COMMENT '区域id',
    `ORG_NAME` varchar(32) NOT NULL COMMENT '区域名称',
    `P_NAME` varchar(32) NOT NULL COMMENT '人员角色名称',
    `STATE` varchar(8) NOT NULL COMMENT '状态',
    `business_id` int NOT NULL COMMENT '网格公司id',
    `ETL_CYCLE` int NOT NULL COMMENT '数据日期',
    `ETL_DATE` datetime NOT NULL COMMENT '推送日期',
    PRIMARY KEY (`PARTY_ROLE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网格经理信息表';

CREATE TABLE `ORGANIZATION_TOJF` (
    `PARTY_ID` int NOT NULL COMMENT '组织id',
    `PARENT_PARTY_ID` int NOT NULL COMMENT '上级组织id',
    `ORG_CODE` varchar(32) NOT NULL COMMENT '组织code',
    `ORG_NAME` varchar(32) NOT NULL COMMENT '组织名称',
    `ORG_LEVEL` varchar(8) NOT NULL COMMENT '组织层级',
    `PATH_CODE` varchar(64) NOT NULL,
    `ORG_TYPE` varchar(8) NOT NULL COMMENT '组织类型',
    `ORG_TYPE_ID` varchar(8) NOT NULL,
    `ETL_CYCLE` int NOT NULL COMMENT '数据日期',
    `ETL_DATE` datetime NOT NULL COMMENT '推送日期',
    PRIMARY KEY (`PARTY_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网格组织信息表';

CREATE TABLE `GRID_ADDR_REL_TOJF` (
    `SEGM_ID` varchar(24) NOT NULL COMMENT '标准地址id',
    `STAND_NAME` varchar(255) NOT NULL COMMENT '标准地址名称',
    `GRID_ID` bigint NOT NULL COMMENT '网格id',
    `CREATE_TIME` datetime NOT NULL COMMENT '关联关系生成时间',
    `BUSINESS_ID` int NOT NULL COMMENT '网格公司id',
    `ETL_CYCLE` int NOT NULL COMMENT '数据生成时间',
    `ETL_DATE` datetime NOT NULL COMMENT '数据推送时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网格与标准地址关联表';

CREATE TABLE `CUST_GRID_REL_TOJF_TD` (
    `CUST_ID` int NOT NULL COMMENT '客户编号',
    `MGR_DEPT_ID` int NULL COMMENT '网格部门id',
    `MGR_DEPT_NAME` varchar(32) NULL COMMENT '网格部门名称',
    `REGION_ID` int NULL COMMENT '区域id',
    `REGION_NAME` varchar(32) NULL COMMENT '区域名称',
    `TEAM_ID` int NULL COMMENT '团队id',
    `TEAM_NAME` varchar(32) NULL COMMENT '团队名称',
    `GRID_ID` bigint NULL COMMENT '网格id',
    `GRID_NAME` varchar(32) NULL COMMENT '网格名称',
    `MGR_ID` int NULL COMMENT '网格经理id',
    `MGR_NAME` varchar(32) NULL COMMENT '网格经理名称',
    `GRID_TYPE` varchar(1) NULL,
    `NOTES` varchar(255) NULL COMMENT '备注',
    `AREA_ID` int NOT NULL COMMENT '分公司id',
    `AREA_NAME` varchar(32) NOT NULL COMMENT '分公司名称',
    `ETL_CYCLE` int NOT NULL COMMENT '数据日期',
    `ETL_DATE` datetime NOT NULL COMMENT '推送时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户网格归属关系表';

CREATE TABLE `cust_TOJF` (
    `CUST_NAME` varchar(32) NOT NULL COMMENT '客户名称',
    `CUST_CODE` varchar(32) NOT NULL COMMENT '客户编码',
    `CUST_ID` int NOT NULL COMMENT '客户 ID',
    `CUST_TYPE_BOSS` int NOT NULL COMMENT 'BOSS 客户类型',
    `CONTACT_ADDR` varchar(255) NOT NULL COMMENT '联系地址',
    `MS_AREA_ID` bigint NULL COMMENT '网格 ID',
    `ETL_CYCLE` int NOT NULL COMMENT '数据日期',
    `ETL_DATE` datetime NOT NULL COMMENT '推送时间',
    `BUSINESS_ID` int NOT NULL COMMENT '网格公司 ID',
    PRIMARY KEY (`CUST_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户网格基础信息表';

CREATE TABLE `cust_TOJF_TD` (
    `cust_name` varchar(32) NOT NULL COMMENT '客户名称',
    `cust_code` varchar(32) NOT NULL COMMENT '客户编码',
    `cust_id` int NOT NULL COMMENT '客户 ID',
    `cust_type_boss` int NOT NULL COMMENT 'BOSS 客户类型',
    `contact_addr` varchar(255) NOT NULL COMMENT '联系地址',
    `ms_area_id` bigint NULL COMMENT '网格 ID',
    `business_id` int NOT NULL COMMENT '网格公司 ID',
    `ETL_CYCLE` int NOT NULL COMMENT '数据日期',
    `ETL_DATE` datetime NOT NULL COMMENT '推送时间',
    PRIMARY KEY (`cust_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户网格基础信息表（TD）';
