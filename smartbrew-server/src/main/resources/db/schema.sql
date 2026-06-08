-- ============================================================
-- Smart Brew AI — V1 数据库 DDL
-- 首次启动时自动执行（spring.sql.init.mode=always）
-- ============================================================

CREATE TABLE IF NOT EXISTS device (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id           VARCHAR(64)     NOT NULL COMMENT '设备唯一标识（ESP32 MAC地址）',
    device_name         VARCHAR(100)    NOT NULL COMMENT '设备名称',
    device_secret       VARCHAR(64)     NOT NULL COMMENT '设备密钥（MQTT认证）',
    status              TINYINT         NOT NULL DEFAULT 0 COMMENT '在线状态：0-离线, 1-在线',
    ip_address          VARCHAR(45)     NULL COMMENT '最近IP',
    firmware_version    VARCHAR(20)     NULL COMMENT '固件版本',
    last_heartbeat_time DATETIME        NULL COMMENT '最近心跳时间',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_device_id (device_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

CREATE TABLE IF NOT EXISTS sensor_data (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id       VARCHAR(64)     NOT NULL COMMENT '设备编号',
    tank_temperature DECIMAL(5,2)   NOT NULL COMMENT '桶内温度（℃）',
    env_temperature  DECIMAL(5,2)   NULL COMMENT '环境温度（℃）',
    env_humidity     DECIMAL(5,2)   NULL COMMENT '环境湿度（%RH）',
    create_time      DATETIME(3)    NOT NULL COMMENT '采集时间戳',
    KEY idx_device_time (device_id, create_time),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传感器数据表';

CREATE TABLE IF NOT EXISTS fermentation_batch (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id           VARCHAR(64)     NOT NULL COMMENT '所属设备',
    batch_name          VARCHAR(100)    NOT NULL COMMENT '批次名',
    wine_type           VARCHAR(30)     NOT NULL COMMENT 'FRUIT_WINE/RICE_WINE/MEAD/OTHER',
    ingredients         VARCHAR(500)    NULL COMMENT '原料简述',
    target_temperature  DECIMAL(5,2)    NULL COMMENT '目标温度',
    expected_days       INT UNSIGNED    NULL COMMENT '预计天数',
    start_time          DATETIME        NOT NULL COMMENT '发酵开始时间',
    end_time            DATETIME        NULL COMMENT '结束时间',
    status              TINYINT         NOT NULL DEFAULT 1 COMMENT '1-进行中, 2-已完成, 3-异常中止',
    notes               VARCHAR(500)    NULL COMMENT '备注',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_device_status (device_id, status),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发酵批次表';

CREATE TABLE IF NOT EXISTS device_heartbeat (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id           VARCHAR(64)     NOT NULL COMMENT '设备编号',
    rssi                SMALLINT        NOT NULL COMMENT 'WiFi信号强度',
    heap_free           INT UNSIGNED    NOT NULL COMMENT '可用堆内存',
    firmware_version    VARCHAR(20)     NOT NULL COMMENT '固件版本',
    ip_address          VARCHAR(45)     NOT NULL COMMENT '设备IP',
    uptime_seconds      INT UNSIGNED    NULL COMMENT '运行时长（秒）',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '心跳时间',
    KEY idx_device_time (device_id, create_time),
    KEY idx_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备心跳表';

CREATE TABLE IF NOT EXISTS alarm_record (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id           VARCHAR(64)     NOT NULL COMMENT '设备编号',
    sensor_data_id      BIGINT UNSIGNED NULL COMMENT '传感器数据ID',
    alarm_type          VARCHAR(30)     NOT NULL COMMENT 'HIGH_TEMP/LOW_TEMP/DEVICE_OFFLINE',
    alarm_level         VARCHAR(10)     NOT NULL DEFAULT 'WARN' COMMENT 'WARN/ERROR',
    alarm_title         VARCHAR(100)    NOT NULL COMMENT '告警标题',
    alarm_message       VARCHAR(500)    NOT NULL COMMENT '告警详情',
    alarm_value         DECIMAL(5,2)    NULL COMMENT '实际值',
    threshold_value     DECIMAL(5,2)    NULL COMMENT '阈值',
    is_cleared          TINYINT         NOT NULL DEFAULT 0 COMMENT '0-未消除, 1-已消除',
    cleared_time        DATETIME        NULL COMMENT '消除时间',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_device_alarm (device_id, create_time),
    KEY idx_uncleared (is_cleared, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

CREATE TABLE IF NOT EXISTS ai_analysis_record (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id           VARCHAR(64)     NOT NULL COMMENT '设备编号',
    batch_id            BIGINT UNSIGNED NULL COMMENT '发酵批次ID',
    analysis_type       VARCHAR(30)     NOT NULL COMMENT 'STATUS_ANALYSIS/CYCLE_PREDICT',
    input_snapshot      JSON            NULL COMMENT '输入数据快照',
    prompt              TEXT            NOT NULL COMMENT 'AI提示词',
    analysis_result     TEXT            NOT NULL COMMENT 'AI返回结果',
    status_assessment   VARCHAR(500)    NULL COMMENT '状态评估',
    risk_warning        VARCHAR(500)    NULL COMMENT '风险提示',
    suggestion          VARCHAR(500)    NULL COMMENT '调整建议',
    predicted_end_time  DATETIME        NULL COMMENT '预计完成时间',
    ai_model            VARCHAR(50)     NOT NULL DEFAULT 'deepseek-chat',
    response_time_ms    INT UNSIGNED    NULL COMMENT '响应耗时',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_device_batch (device_id, batch_id),
    KEY idx_type_time (analysis_type, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI分析记录表';

CREATE TABLE IF NOT EXISTS device_control_log (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id           VARCHAR(64)     NOT NULL COMMENT '设备编号',
    control_target      VARCHAR(20)     NOT NULL COMMENT 'FAN/HEATER',
    command             VARCHAR(10)     NOT NULL COMMENT 'ON/OFF',
    trigger_source      VARCHAR(10)     NOT NULL COMMENT 'AUTO/MANUAL',
    operator_id         BIGINT UNSIGNED NULL COMMENT '操作者用户ID',
    trigger_reason      VARCHAR(200)    NULL COMMENT '触发原因',
    mqtt_msg_id         VARCHAR(64)     NULL COMMENT 'MQTT消息ID',
    execute_status      TINYINT         NOT NULL DEFAULT 0 COMMENT '0-已下发, 1-成功, 2-超时, 3-失败',
    execute_time        DATETIME        NULL COMMENT '执行时间',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_device_time (device_id, create_time),
    KEY idx_trigger (trigger_source, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备控制日志表';

CREATE TABLE IF NOT EXISTS device_event (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id           VARCHAR(64)     NOT NULL COMMENT '设备编号',
    event_type          VARCHAR(30)     NOT NULL COMMENT 'DEVICE_ONLINE/OFFLINE/FERMENTATION_START/END/...',
    event_level         VARCHAR(10)     NOT NULL DEFAULT 'INFO' COMMENT 'INFO/WARN/ERROR',
    event_title         VARCHAR(100)    NOT NULL COMMENT '事件摘要',
    event_detail        VARCHAR(500)    NULL COMMENT '事件详情',
    reference_id        BIGINT UNSIGNED NULL COMMENT '关联记录ID',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_device_event (device_id, event_type, create_time),
    KEY idx_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备事件表';

CREATE TABLE IF NOT EXISTS sys_user (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username            VARCHAR(50)     NOT NULL COMMENT '登录用户名',
    password            VARCHAR(255)    NOT NULL COMMENT 'BCrypt密文',
    nickname            VARCHAR(50)     NULL COMMENT '显示昵称',
    avatar_url          VARCHAR(255)    NULL COMMENT '头像URL',
    status              TINYINT         NOT NULL DEFAULT 1 COMMENT '0-禁用, 1-正常',
    last_login_time     DATETIME        NULL COMMENT '最近登录',
    last_login_ip       VARCHAR(45)     NULL COMMENT '最近登录IP',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS system_config (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    config_key          VARCHAR(100)    NOT NULL COMMENT '配置键',
    config_value        VARCHAR(500)    NOT NULL COMMENT '配置值',
    config_type         VARCHAR(20)     NOT NULL DEFAULT 'STRING' COMMENT 'STRING/INT/DOUBLE/BOOLEAN/JSON',
    config_group        VARCHAR(50)     NOT NULL DEFAULT 'SYSTEM' COMMENT 'MQTT/ALARM/SYSTEM/AI',
    description         VARCHAR(200)    NULL COMMENT '说明',
    is_editable         TINYINT         NOT NULL DEFAULT 1 COMMENT '0-只读, 1-可编辑',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 预置系统配置
INSERT IGNORE INTO system_config (config_key, config_value, config_type, config_group, description, is_editable) VALUES
('mqtt.broker.url', 'tcp://localhost:1883', 'STRING', 'MQTT', 'MQTT Broker地址', 0),
('mqtt.topic.data', 'smartbrew/+/data', 'STRING', 'MQTT', '数据上报Topic', 0),
('mqtt.topic.control', 'smartbrew/{deviceId}/control', 'STRING', 'MQTT', '控制下发Topic模板', 0),
('alarm.temp.high', '30.00', 'DOUBLE', 'ALARM', '高温告警阈值（℃）', 1),
('alarm.temp.low', '15.00', 'DOUBLE', 'ALARM', '低温告警阈值（℃）', 1),
('alarm.offline.timeout', '120', 'INT', 'ALARM', '设备离线判定超时（秒）', 1),
('sensor.interval.temp', '30', 'INT', 'SYSTEM', '温度采集间隔（秒）', 0),
('sensor.interval.env', '60', 'INT', 'SYSTEM', '环境采集间隔（秒）', 0),
('ai.model.name', 'deepseek-chat', 'STRING', 'AI', 'AI模型名称', 1),
('ai.analysis.interval', '3600', 'INT', 'AI', 'AI自动分析间隔（秒）', 1);

-- 预置管理员用户（密码: admin123）
INSERT IGNORE INTO sys_user (username, password, nickname, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '管理员', 1);
