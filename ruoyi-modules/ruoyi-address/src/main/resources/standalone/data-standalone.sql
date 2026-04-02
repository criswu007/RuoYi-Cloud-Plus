INSERT INTO segm_addr_type (addr_type_id, name, no, level_id, score, notes) VALUES
    (180000, '省、自治区', 'PROVINCE', 10, 10, 'standalone 种子数据'),
    (180001, '市', 'CITY', 20, 20, 'standalone 种子数据'),
    (180015, '市区', 'URBAN', 30, 30, 'standalone 种子数据'),
    (180002, '县、区', 'DISTRICT', 40, 40, 'standalone 种子数据'),
    (180003, '乡、镇、街、道', 'STREET', 50, 50, 'standalone 种子数据'),
    (180008, '村', 'VILLAGE', 60, 60, 'standalone 种子数据'),
    (180004, '路、里、弄、巷', 'ROAD', 70, 70, 'standalone 种子数据'),
    (180011, '庄、组、队', 'TEAM', 80, 80, 'standalone 种子数据'),
    (180005, '建筑群、小区', 'COMMUNITY', 90, 90, 'standalone 种子数据'),
    (180012, '期、区、座', 'PHASE', 100, 100, 'standalone 种子数据'),
    (180013, '建筑、楼栋', 'BUILDING', 110, 110, 'standalone 种子数据'),
    (180014, '横向建筑', 'BLOCK', 120, 120, 'standalone 种子数据'),
    (180009, '建筑单元', 'UNIT', 130, 130, 'standalone 种子数据'),
    (180006, '层、楼', 'FLOOR', 140, 140, 'standalone 种子数据'),
    (180010, '门牌号', 'DOOR', 150, 150, 'standalone 种子数据'),
    (180007, '房间号', 'ROOM', 160, 160, 'standalone 种子数据'),
    (180095, '伪地址', 'PSEUDO', 170, 170, 'standalone 种子数据'),
    (180099, '地址补充描述', 'SUPPLEMENT', 180, 180, 'standalone 种子数据'),
    (180100, '尾级地址（选址生成）', 'TAIL', 190, 190, 'standalone 种子数据');

INSERT INTO spc_region (region_id, region_no, region_name, super_region_id, delete_state, notes) VALUES
    ('320000', '320000', '江苏省', NULL, '0', '一级区域'),
    ('320100', '320100', '南京市', '320000', '0', '二级区域');

INSERT INTO spc_station (station_id, station_name, region_id, manage_type, notes) VALUES
    ('ST320100WX001', '洪武路维修站', '320100', '2017101', 'standalone 维修管理站'),
    ('ST320100AZ001', '洪武路安装站', '320100', '2017102', 'standalone 安装管理站'),
    ('ST320100YY001', '新街口营业站', '320100', '2017103', 'standalone 营业管理站'),
    ('ST320200WX001', '洪武东路维修站', '320200', '2017101', 'standalone 异区域维修管理站');

INSERT INTO pub_restriction (serial_no, keyword, desc_china, code, notes) VALUES
    ('2140900', 'ADDR_SEGM_STATUS', '有效', 'VALID', 'standalone 地址状态'),
    ('2140901', 'ADDR_SEGM_STATUS', '无效', 'INVALID', 'standalone 地址状态'),
    ('2140760', 'ADDR_IN_TYPE_FTTH', 'FTTH_双纤', 'FTTH_DOUBLE', 'standalone 光纤接入方式'),
    ('2140761', 'ADDR_IN_TYPE_FTTH', 'FTTH_单纤', 'FTTH_SINGLE', 'standalone 光纤接入方式'),
    ('2141301', 'FTTH_PON_TYPE', '1G-PON', 'GPON_1G', 'standalone 光纤接入能力'),
    ('2141302', 'FTTH_PON_TYPE', '10G-PON', 'GPON_10G', 'standalone 光纤接入能力'),
    ('2140770', 'ADDR_IN_TYPE_LAN', 'LAN', 'LAN', 'standalone 电缆接入方式'),
    ('2140780', 'ADDR_IN_TYPE_LAN', 'CMTS', 'CMTS', 'standalone 电缆接入方式'),
    ('2140511', 'AREA_TYPE', '城区', 'URBAN', 'standalone 城乡属性'),
    ('2140512', 'AREA_TYPE', '镇区', 'TOWN', 'standalone 城乡属性'),
    ('2140800', 'ADDR_PLACE_TYPE', '普通住宅', 'HOUSE', 'standalone 房屋属性'),
    ('2140801', 'ADDR_PLACE_TYPE', '商业楼宇', 'BUILDING', 'standalone 房屋属性');

INSERT INTO ADDR_SEGM (
    segm_id, segm_type, segm_name, segm_no, parent_segm_id, status, post_code, is_city,
    stand_name, stand_no, region_id, notes, delete_state, create_date, district_id, service_region_id,
    segm_name_fir, station_id, place_type, cover_num, installstation_id, busstation_id,
    addr_in_type_ftth, ftth_pon_type, addr_in_type_lan, area_type, addr_unit_type
) VALUES
    ('000000000000000000000301', 180015, '主城区', 'ZCQ', '320100', 2140900, NULL, 'Y', '江苏省南京市主城区', 'JSSNJSZCQ', '320100', 'standalone 样例', '0', TIMESTAMP '2026-03-29 18:47:03', '320100', '320100', 'N', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    ('000000000000000000000302', 180002, '白下区', 'BXQ', '000000000000000000000301', 2140900, NULL, 'Y', '江苏省南京市主城区白下区', 'JSSNJSZCQBXQ', '320100', 'standalone 样例', '0', TIMESTAMP '2026-03-29 18:47:03', '320100', '320100', 'N', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    ('000000000000000000000303', 180003, '淮海路街道', 'HHLJD', '000000000000000000000302', 2140900, NULL, 'Y', '江苏省南京市主城区白下区淮海路街道', 'JSSNJSZCQBXQHHLJD', '320100', 'standalone 样例', '0', TIMESTAMP '2026-03-29 18:47:03', '320100', '320100', 'N', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    ('000000000000000000000304', 180004, '太安路', 'TAL', '000000000000000000000303', 2140900, NULL, 'Y', '江苏省南京市主城区白下区淮海路街道太安路', 'JSSNJSZCQBXQHHLJDTAL', '320100', 'standalone 样例', '0', TIMESTAMP '2026-03-29 18:47:03', '320100', '320100', 'N', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    ('000000000000000000000305', 180010, '88号', '88H', '000000000000000000000304', 2140900, 'GC-2026-001', 'Y', '江苏省南京市主城区白下区淮海路街道太安路88号', 'JSSNJSZCQBXQHHLJDTAL88H', '320100', 'standalone 样例', '0', TIMESTAMP '2026-03-29 18:47:03', '320100', '320100', 'N', 'ST320100WX001', 2140800, NULL, 'ST320100AZ001', 'ST320100YY001', 2140760, 2141301, 2140770, 2140511, NULL),
    ('000000000000000000000306', 180005, '四方新村', 'SFXC', '000000000000000000000305', 2140900, NULL, 'Y', '江苏省南京市主城区白下区淮海路街道太安路88号四方新村', 'JSSNJSZCQBXQHHLJDTAL88HSFXC', '320100', 'standalone 样例', '0', TIMESTAMP '2026-03-29 18:47:03', '320100', '320100', 'Y', NULL, 2140800, 128, NULL, NULL, 2140760, 2141301, NULL, 2140511, 2140800),
    ('000000000000000000000307', 180012, '一期', 'YQ', '000000000000000000000306', 2140900, NULL, 'Y', '江苏省南京市主城区白下区淮海路街道太安路88号四方新村一期', 'JSSNJSZCQBXQHHLJDTAL88HSFXCYQ', '320100', 'standalone 样例', '0', TIMESTAMP '2026-03-29 18:47:03', '320100', '320100', 'N', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    ('000000000000000000000308', 180013, '1栋', '1D', '000000000000000000000307', 2140900, NULL, 'Y', '江苏省南京市主城区白下区淮海路街道太安路88号四方新村一期1栋', 'JSSNJSZCQBXQHHLJDTAL88HSFXCYQ1D', '320100', 'standalone 样例', '0', TIMESTAMP '2026-03-29 18:47:03', '320100', '320100', 'N', NULL, NULL, 64, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    ('000000000000000000000309', 180009, '1单元', '1DY', '000000000000000000000308', 2140900, NULL, 'Y', '江苏省南京市主城区白下区淮海路街道太安路88号四方新村一期1栋1单元', 'JSSNJSZCQBXQHHLJDTAL88HSFXCYQ1D1DY', '320100', 'standalone 样例', '0', TIMESTAMP '2026-03-29 18:47:03', '320100', '320100', 'N', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    ('000000000000000000000310', 180006, '10层', '10C', '000000000000000000000309', 2140900, NULL, 'Y', '江苏省南京市主城区白下区淮海路街道太安路88号四方新村一期1栋1单元10层', 'JSSNJSZCQBXQHHLJDTAL88HSFXCYQ1D1DY10C', '320100', 'standalone 样例', '0', TIMESTAMP '2026-03-29 18:47:03', '320100', '320100', 'N', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    ('000000000000000000000311', 180007, '1001室', '1001S', '000000000000000000000310', 2140900, NULL, 'Y', '江苏省南京市主城区白下区淮海路街道太安路88号四方新村一期1栋1单元10层1001室', 'JSSNJSZCQBXQHHLJDTAL88HSFXCYQ1D1DY10C1001S', '320100', 'standalone 样例', '0', TIMESTAMP '2026-03-29 18:47:03', '320100', '320100', 'N', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    ('000000000000000000000401', 180002, '江宁区', 'JNQ', '320100', 2140900, NULL, 'Y', '江苏省南京市江宁区', 'JSSNJSJNQ', '320100', 'standalone 样例', '0', TIMESTAMP '2026-03-29 18:47:03', '320100', '320100', 'N', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    ('000000000000000000000402', 180004, '竹山路', 'ZSL', '000000000000000000000401', 2140900, NULL, 'Y', '江苏省南京市江宁区竹山路', 'JSSNJSJNQZSL', '320100', 'standalone 样例', '0', TIMESTAMP '2026-03-29 18:47:03', '320100', '320100', 'N', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    ('000000000000000000000403', 180004, '胜太路', 'STL', '000000000000000000000401', 2140900, NULL, 'Y', '江苏省南京市江宁区胜太路', 'JSSNJSJNQSTL', '320100', 'standalone 样例', '0', TIMESTAMP '2026-03-29 18:47:03', '320100', '320100', 'N', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    ('000000000000000000000404', 180005, '百家湖花园', 'BJHHY', '000000000000000000000402', 2140900, NULL, 'Y', '江苏省南京市江宁区竹山路百家湖花园', 'JSSNJSJNQZSLBJHHY', '320100', 'standalone 样例', '0', TIMESTAMP '2026-03-29 18:47:03', '320100', '320100', 'N', NULL, 2140800, 360, NULL, NULL, NULL, NULL, NULL, NULL, 2140800),
    ('000000000000000000000405', 180005, '金鹰国际花园', 'JYGJHY', '000000000000000000000403', 2140900, NULL, 'Y', '江苏省南京市江宁区胜太路金鹰国际花园', 'JSSNJSJNQSTLJYGJHY', '320100', 'standalone 样例', '0', TIMESTAMP '2026-03-29 18:47:03', '320100', '320100', 'N', NULL, 2140800, 480, NULL, NULL, NULL, NULL, NULL, NULL, 2140800),
    ('000000000000000000000406', 180013, '1栋', '1D', '000000000000000000000404', 2140900, NULL, 'Y', '江苏省南京市江宁区竹山路百家湖花园1栋', 'JSSNJSJNQZSLBJHHY1D', '320100', 'standalone 样例', '0', TIMESTAMP '2026-03-29 18:47:03', '320100', '320100', 'N', NULL, NULL, 120, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    ('000000000000000000000407', 180013, '2栋', '2D', '000000000000000000000404', 2140900, NULL, 'Y', '江苏省南京市江宁区竹山路百家湖花园2栋', 'JSSNJSJNQZSLBJHHY2D', '320100', 'standalone 样例', '0', TIMESTAMP '2026-03-29 18:47:03', '320100', '320100', 'N', NULL, NULL, 120, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    ('000000000000000000000408', 180013, '3栋', '3D', '000000000000000000000404', 2140900, NULL, 'Y', '江苏省南京市江宁区竹山路百家湖花园3栋', 'JSSNJSJNQZSLBJHHY3D', '320100', 'standalone 样例', '0', TIMESTAMP '2026-03-29 18:47:03', '320100', '320100', 'N', NULL, NULL, 120, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    ('000000000000000000000409', 180013, '8栋', '8D', '000000000000000000000405', 2140900, NULL, 'Y', '江苏省南京市江宁区胜太路金鹰国际花园8栋', 'JSSNJSJNQSTLJYGJHY8D', '320100', 'standalone 样例', '0', TIMESTAMP '2026-03-29 18:47:03', '320100', '320100', 'N', NULL, NULL, 120, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    ('000000000000000000000410', 180013, '9栋', '9D', '000000000000000000000405', 2140900, NULL, 'Y', '江苏省南京市江宁区胜太路金鹰国际花园9栋', 'JSSNJSJNQSTLJYGJHY9D', '320100', 'standalone 样例', '0', TIMESTAMP '2026-03-29 18:47:03', '320100', '320100', 'N', NULL, NULL, 120, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO ADDR_SET_SEGM (set_addr_id, segm_id, delete_state) VALUES
    ('SET000000000000000000001', '000000000000000000000311', '0');

INSERT INTO address_standard_import_record (
    id, batch_no, file_name, status, total_count, success_count, fail_count,
    error_msg, update_support, tenant_id, create_dept, create_by, create_time, update_by, update_time, del_flag
) VALUES (
    1001, 'IMP202604020001', 'standard-address-import-demo.xlsx', '2', 3, 2, 1,
    '第 2 行父级地址不存在', FALSE, '000000', 103, 1, TIMESTAMP '2026-04-02 09:30:00', 1, TIMESTAMP '2026-04-02 09:30:00', '0'
);

INSERT INTO address_standard_import_fail_detail (
    id, batch_id, row_num, parent_stand_name, segm_name, segm_type, addr_level,
    status, fail_reason, raw_payload, tenant_id, create_dept, create_by, create_time, update_by, update_time, del_flag
) VALUES (
    2001, 1001, 2, '江苏省南京市鼓楼区测试路', '测试失败地址', '180013', 11,
    '2', '父级地址不存在', '{"parentStandName":"江苏省南京市鼓楼区测试路","segmName":"测试失败地址","addrLevel":11}', '000000', 103, 1,
    TIMESTAMP '2026-04-02 09:30:00', 1, TIMESTAMP '2026-04-02 09:30:00', '0'
);
