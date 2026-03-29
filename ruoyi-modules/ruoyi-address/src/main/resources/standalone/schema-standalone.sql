DROP TABLE IF EXISTS ADDR_SET_SEGM;
DROP TABLE IF EXISTS ADDR_SEGM;
DROP TABLE IF EXISTS spc_region;
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
