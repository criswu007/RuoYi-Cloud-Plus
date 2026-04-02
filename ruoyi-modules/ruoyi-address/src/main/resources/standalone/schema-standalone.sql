DROP TABLE IF EXISTS address_standard_import_fail_detail;
DROP TABLE IF EXISTS address_standard_import_record;
DROP TABLE IF EXISTS ADDR_SET_SEGM;
DROP TABLE IF EXISTS ADDR_SEGM;
DROP TABLE IF EXISTS spc_station;
DROP TABLE IF EXISTS spc_region;
DROP TABLE IF EXISTS pub_restriction;
DROP TABLE IF EXISTS segm_addr_type;

CREATE TABLE segm_addr_type (
    addr_type_id INT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    no VARCHAR(255),
    level_id INT NOT NULL,
    score INT NOT NULL,
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(255)
);

CREATE TABLE spc_region (
    region_id VARCHAR(24) PRIMARY KEY,
    region_no VARCHAR(80) NOT NULL,
    region_name VARCHAR(80) NOT NULL,
    super_region_id VARCHAR(24),
    delete_state CHAR(1) DEFAULT '0',
    notes VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE spc_station (
    station_id VARCHAR(24) PRIMARY KEY,
    station_name VARCHAR(100) NOT NULL,
    region_id VARCHAR(24),
    manage_type VARCHAR(24),
    notes VARCHAR(255),
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pub_restriction (
    serial_no VARCHAR(24) PRIMARY KEY,
    keyword VARCHAR(80) NOT NULL,
    desc_china VARCHAR(200) NOT NULL,
    code VARCHAR(80),
    notes VARCHAR(255),
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ADDR_SEGM (
    segm_id VARCHAR(24) PRIMARY KEY,
    segm_type INT,
    segm_name VARCHAR(200),
    segm_no VARCHAR(120),
    parent_segm_id VARCHAR(24),
    status INT,
    post_code VARCHAR(255),
    is_city CHAR(1),
    stand_name VARCHAR(400),
    stand_no VARCHAR(300),
    region_id VARCHAR(24),
    notes VARCHAR(255),
    delete_state CHAR(1) DEFAULT '0',
    delete_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_date TIMESTAMP,
    district_id VARCHAR(24),
    service_region_id VARCHAR(24),
    segm_name_fir CHAR(1),
    station_id VARCHAR(24),
    place_type INT,
    cover_num INT,
    installstation_id VARCHAR(24),
    busstation_id VARCHAR(24),
    addr_in_type_ftth INT,
    ftth_pon_type INT,
    addr_in_type_lan INT,
    area_type INT,
    addr_unit_type INT
);

CREATE TABLE ADDR_SET_SEGM (
    set_addr_id VARCHAR(24) PRIMARY KEY,
    segm_id VARCHAR(24),
    delete_state CHAR(1) DEFAULT '0'
);

CREATE TABLE address_standard_import_record (
    id BIGINT PRIMARY KEY,
    batch_no VARCHAR(64) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    status CHAR(1) DEFAULT '0',
    total_count INT DEFAULT 0,
    success_count INT DEFAULT 0,
    fail_count INT DEFAULT 0,
    error_msg VARCHAR(2000),
    update_support BOOLEAN DEFAULT FALSE,
    tenant_id VARCHAR(20) DEFAULT '000000',
    create_dept BIGINT,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0'
);

CREATE INDEX idx_addr_std_imp_record_batch_no ON address_standard_import_record(batch_no);
CREATE INDEX idx_addr_std_imp_record_create_time ON address_standard_import_record(create_time);

CREATE TABLE address_standard_import_fail_detail (
    id BIGINT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    row_num INT NOT NULL,
    parent_stand_name VARCHAR(500),
    segm_name VARCHAR(255),
    segm_type VARCHAR(24),
    addr_level INT,
    status CHAR(1) DEFAULT '2',
    fail_reason VARCHAR(1000),
    raw_payload CLOB,
    tenant_id VARCHAR(20) DEFAULT '000000',
    create_dept BIGINT,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0',
    CONSTRAINT fk_addr_std_imp_fail_batch FOREIGN KEY (batch_id) REFERENCES address_standard_import_record(id)
);

CREATE INDEX idx_addr_std_imp_fail_batch_id ON address_standard_import_fail_detail(batch_id);
CREATE INDEX idx_addr_std_imp_fail_create_time ON address_standard_import_fail_detail(create_time);
