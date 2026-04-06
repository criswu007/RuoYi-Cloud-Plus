create table if not exists address_search_sync_log (
    id bigint primary key,
    business_type varchar(32) not null,
    entity_type varchar(32) not null,
    entity_id varchar(32) not null,
    phase varchar(32) not null,
    success_flag char(1) not null,
    error_message varchar(1000),
    created_time datetime not null
);

create table if not exists address_search_repair_task (
    id bigint primary key,
    entity_type varchar(32) not null,
    entity_id varchar(32) not null,
    repair_action varchar(32) not null,
    payload_json text,
    status varchar(32) not null,
    retry_count int not null,
    created_time datetime not null,
    updated_time datetime not null
);

create table if not exists address_search_maintenance_task (
    id bigint primary key,
    task_type varchar(32) not null,
    target_alias varchar(128) not null,
    physical_index_name varchar(128),
    status varchar(32) not null,
    current_phase varchar(32) not null,
    total_count bigint not null default 0,
    processed_count bigint not null default 0,
    progress_percent int not null default 0,
    error_message varchar(1000),
    trigger_by varchar(64),
    started_time datetime,
    finished_time datetime,
    created_time datetime not null,
    updated_time datetime not null
);
