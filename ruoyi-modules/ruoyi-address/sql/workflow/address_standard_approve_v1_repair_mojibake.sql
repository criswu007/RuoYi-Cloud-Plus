SET NAMES utf8mb4;

UPDATE `flow_definition`
SET `flow_name` = CASE
    WHEN `flow_name` REGEXP '[一-龥]' THEN `flow_name`
    ELSE CONVERT(BINARY(CONVERT(`flow_name` USING latin1)) USING utf8mb4)
END
WHERE `flow_code` = 'address_standard_approve_v1';

UPDATE `flow_node`
SET `node_name` = CASE
    WHEN `node_name` IS NULL OR `node_name` REGEXP '[一-龥]' THEN `node_name`
    ELSE CONVERT(BINARY(CONVERT(`node_name` USING latin1)) USING utf8mb4)
END
WHERE `definition_id` IN (
    SELECT `id`
    FROM (
        SELECT `id`
        FROM `flow_definition`
        WHERE `flow_code` = 'address_standard_approve_v1'
    ) `temp`
);

UPDATE `flow_instance`
SET `node_name` = CASE
        WHEN `node_name` IS NULL OR `node_name` REGEXP '[一-龥]' THEN `node_name`
        ELSE CONVERT(BINARY(CONVERT(`node_name` USING latin1)) USING utf8mb4)
    END,
    `def_json` = CASE
        WHEN `def_json` IS NULL OR `def_json` REGEXP '[一-龥]' THEN `def_json`
        ELSE CONVERT(BINARY(CONVERT(`def_json` USING latin1)) USING utf8mb4)
    END
WHERE `definition_id` IN (
    SELECT `id`
    FROM (
        SELECT `id`
        FROM `flow_definition`
        WHERE `flow_code` = 'address_standard_approve_v1'
    ) `temp`
);

UPDATE `flow_task`
SET `node_name` = CASE
    WHEN `node_name` IS NULL OR `node_name` REGEXP '[一-龥]' THEN `node_name`
    ELSE CONVERT(BINARY(CONVERT(`node_name` USING latin1)) USING utf8mb4)
END
WHERE `definition_id` IN (
    SELECT `id`
    FROM (
        SELECT `id`
        FROM `flow_definition`
        WHERE `flow_code` = 'address_standard_approve_v1'
    ) `temp`
);

UPDATE `flow_his_task`
SET `node_name` = CASE
        WHEN `node_name` IS NULL OR `node_name` REGEXP '[一-龥]' THEN `node_name`
        ELSE CONVERT(BINARY(CONVERT(`node_name` USING latin1)) USING utf8mb4)
    END,
    `target_node_name` = CASE
        WHEN `target_node_name` IS NULL OR `target_node_name` REGEXP '[一-龥]' THEN `target_node_name`
        ELSE CONVERT(BINARY(CONVERT(`target_node_name` USING latin1)) USING utf8mb4)
    END
WHERE `definition_id` IN (
    SELECT `id`
    FROM (
        SELECT `id`
        FROM `flow_definition`
        WHERE `flow_code` = 'address_standard_approve_v1'
    ) `temp`
);
