ALTER TABLE `address_standard_approval`
    ADD COLUMN `submit_fingerprint` varchar(64) DEFAULT NULL COMMENT '提交内容指纹' AFTER `request_payload`,
    ADD COLUMN `submit_guard_key` varchar(64) NOT NULL DEFAULT 'ACTIVE' COMMENT '重复提交保护占位键' AFTER `submit_fingerprint`;

UPDATE `address_standard_approval`
SET `submit_guard_key` = CASE
    WHEN `approval_status` IN ('WAITING', 'EXECUTING') THEN 'ACTIVE'
    ELSE COALESCE(NULLIF(`apply_no`, ''), CAST(`id` AS CHAR))
END
WHERE `submit_guard_key` IS NULL
   OR `submit_guard_key` = '';

CREATE UNIQUE INDEX `uk_addr_std_approval_submit_guard`
    ON `address_standard_approval` (`submit_user_id`, `operation_type`, `submit_fingerprint`, `submit_guard_key`);
