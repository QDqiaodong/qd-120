CREATE DATABASE IF NOT EXISTS stopper_asset DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE stopper_asset;

CREATE TABLE IF NOT EXISTS stopper (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stopper_no VARCHAR(50) NOT NULL COMMENT '挡块编号',
    spec VARCHAR(100) DEFAULT NULL COMMENT '规格型号',
    adapt_equipment VARCHAR(200) DEFAULT NULL COMMENT '适配设备',
    station VARCHAR(100) DEFAULT NULL COMMENT '存放工位',
    storage_time DATETIME DEFAULT NULL COMMENT '入库时间',
    image_url VARCHAR(500) DEFAULT NULL COMMENT '图片地址',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，2-报废',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stopper_no (stopper_no),
    KEY idx_station (station),
    KEY idx_spec (spec),
    KEY idx_status (status),
    KEY idx_adapt_equipment (adapt_equipment),
    KEY idx_combo_filter (spec, station, status, stopper_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限位挡块基础档案表';

CREATE TABLE IF NOT EXISTS stopper_station (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    station_name VARCHAR(100) NOT NULL COMMENT '工位名称',
    zone VARCHAR(50) DEFAULT NULL COMMENT '所属区域',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_station_name (station_name),
    KEY idx_zone (zone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工位配置表';

INSERT IGNORE INTO stopper_station (station_name, zone, remark, create_time, update_time, deleted) VALUES
('A区-工位01', 'A区', 'A区1号工位', '2024-01-01 00:00:00', '2024-01-01 00:00:00', 0),
('A区-工位02', 'A区', 'A区2号工位', '2024-01-01 00:00:00', '2024-01-01 00:00:00', 0),
('A区-工位03', 'A区', 'A区3号工位', '2024-01-01 00:00:00', '2024-01-01 00:00:00', 0),
('B区-工位01', 'B区', 'B区1号工位', '2024-01-01 00:00:00', '2024-01-01 00:00:00', 0),
('B区-工位02', 'B区', 'B区2号工位', '2024-01-01 00:00:00', '2024-01-01 00:00:00', 0),
('C区-工位01', 'C区', 'C区1号工位', '2024-01-01 00:00:00', '2024-01-01 00:00:00', 0),
('维修区', '维修区', '维修工位', '2024-01-01 00:00:00', '2024-01-01 00:00:00', 0);

CREATE TABLE IF NOT EXISTS stopper_shift (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stopper_id BIGINT NOT NULL COMMENT '挡块ID',
    stopper_no VARCHAR(50) DEFAULT NULL COMMENT '挡块编号',
    spec VARCHAR(100) DEFAULT NULL COMMENT '规格型号（快照）',
    adapt_equipment VARCHAR(200) DEFAULT NULL COMMENT '适配设备（快照）',
    from_station VARCHAR(100) DEFAULT NULL COMMENT '原工位',
    to_station VARCHAR(100) DEFAULT NULL COMMENT '目标工位',
    operator VARCHAR(100) DEFAULT NULL COMMENT '操作人',
    shift_reason VARCHAR(500) DEFAULT NULL COMMENT '移位原因',
    shift_time DATETIME DEFAULT NULL COMMENT '移位时间',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_stopper_id (stopper_id),
    KEY idx_shift_time (shift_time),
    KEY idx_from_station (from_station),
    KEY idx_to_station (to_station)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挡块工位移位记录表';

CREATE TABLE IF NOT EXISTS stopper_inventory (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    inventory_no VARCHAR(50) NOT NULL COMMENT '盘点单号',
    inventory_month VARCHAR(20) DEFAULT NULL COMMENT '盘点月份',
    total_count INT DEFAULT 0 COMMENT '账面数量',
    actual_count INT DEFAULT 0 COMMENT '实盘数量',
    diff_count INT DEFAULT 0 COMMENT '差异数量',
    inventory_status VARCHAR(20) DEFAULT 'PROCESSING' COMMENT '盘点状态：PROCESSING-进行中，COMPLETED-已完成',
    operator VARCHAR(100) DEFAULT NULL COMMENT '操作人',
    inventory_time DATETIME DEFAULT NULL COMMENT '盘点时间',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_no (inventory_no),
    KEY idx_inventory_month (inventory_month),
    KEY idx_inventory_status (inventory_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挡块月度资产清点表';

CREATE TABLE IF NOT EXISTS stopper_inventory_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    inventory_id BIGINT NOT NULL COMMENT '盘点单ID',
    stopper_id BIGINT NOT NULL COMMENT '挡块ID',
    stopper_no VARCHAR(50) DEFAULT NULL COMMENT '挡块编号',
    spec VARCHAR(100) DEFAULT NULL COMMENT '规格型号',
    station VARCHAR(100) DEFAULT NULL COMMENT '存放工位',
    inventory_status TINYINT DEFAULT 0 COMMENT '盘点状态：0-未盘，1-已盘，2-差异',
    diff_reason VARCHAR(500) DEFAULT NULL COMMENT '差异原因',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_inventory_id (inventory_id),
    KEY idx_stopper_id (stopper_id),
    KEY idx_station (station)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挡块盘点明细表';

CREATE TABLE IF NOT EXISTS stopper_inventory_freeze (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    inventory_id BIGINT NOT NULL COMMENT '盘点单ID',
    stopper_id BIGINT NOT NULL COMMENT '挡块ID',
    stopper_no VARCHAR(50) DEFAULT NULL COMMENT '挡块编号（冻结快照）',
    spec VARCHAR(100) DEFAULT NULL COMMENT '规格型号（冻结快照）',
    station VARCHAR(100) DEFAULT NULL COMMENT '存放工位（冻结快照）',
    status TINYINT DEFAULT NULL COMMENT '状态（冻结快照）：1-正常，2-报废',
    freeze_time DATETIME DEFAULT NULL COMMENT '冻结时间',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_stopper (inventory_id, stopper_id),
    KEY idx_inventory_id (inventory_id),
    KEY idx_stopper_id (stopper_id),
    KEY idx_stopper_no (stopper_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挡块月度清点冻结快照表';

CREATE TABLE IF NOT EXISTS stopper_scrap (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stopper_id BIGINT NOT NULL COMMENT '挡块ID',
    stopper_no VARCHAR(50) DEFAULT NULL COMMENT '挡块编号',
    spec VARCHAR(100) DEFAULT NULL COMMENT '规格型号',
    scrap_reason VARCHAR(500) DEFAULT NULL COMMENT '报废原因',
    scrap_degree VARCHAR(50) DEFAULT NULL COMMENT '磨损程度',
    operator VARCHAR(100) DEFAULT NULL COMMENT '操作人',
    scrap_time DATETIME DEFAULT NULL COMMENT '报废时间',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_stopper_id (stopper_id),
    KEY idx_scrap_time (scrap_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挡块报废归档表';

CREATE TABLE IF NOT EXISTS stopper_maintenance (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stopper_id BIGINT NOT NULL COMMENT '挡块ID',
    stopper_no VARCHAR(50) DEFAULT NULL COMMENT '挡块编号',
    spec VARCHAR(100) DEFAULT NULL COMMENT '规格型号',
    adapt_equipment VARCHAR(200) DEFAULT NULL COMMENT '适配设备',
    original_station VARCHAR(100) DEFAULT NULL COMMENT '原工位',
    repair_reason VARCHAR(500) DEFAULT NULL COMMENT '维修原因',
    expected_return_date DATE DEFAULT NULL COMMENT '预计返回日期',
    send_time DATETIME DEFAULT NULL COMMENT '送修时间',
    send_operator VARCHAR(100) DEFAULT NULL COMMENT '送修操作人',
    status TINYINT DEFAULT 1 COMMENT '状态：1-维修中，2-已完成',
    complete_time DATETIME DEFAULT NULL COMMENT '完成时间',
    complete_operator VARCHAR(100) DEFAULT NULL COMMENT '完成操作人',
    outcome VARCHAR(20) DEFAULT NULL COMMENT '处理结果：RETURN-返回原工位，SCRAP-转报废',
    return_station VARCHAR(100) DEFAULT NULL COMMENT '返回工位',
    complete_remark VARCHAR(500) DEFAULT NULL COMMENT '完成备注',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_stopper_id (stopper_id),
    KEY idx_status (status),
    KEY idx_send_time (send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挡块维修周转记录表';

INSERT IGNORE INTO stopper (stopper_no, spec, adapt_equipment, station, storage_time, image_url, status, remark, create_time, update_time, deleted) VALUES
('STP-001', 'D型-25x30', '全自动组装机A-01', 'A区-工位01', '2024-01-15 09:00:00', '', 1, '标准挡块', '2024-01-15 09:00:00', '2024-01-15 09:00:00', 0),
('STP-002', 'D型-25x30', '全自动组装机A-02', 'A区-工位01', '2024-01-15 09:05:00', '', 1, '标准挡块', '2024-01-15 09:05:00', '2024-01-15 09:05:00', 0),
('STP-003', 'D型-30x40', '全自动组装机A-01', 'A区-工位02', '2024-01-20 14:00:00', '', 1, '大号挡块', '2024-01-20 14:00:00', '2024-01-20 14:00:00', 0),
('STP-004', 'L型-20x25', '半自动贴片机B-01', 'B区-工位01', '2024-02-01 10:00:00', '', 1, 'L型挡块', '2024-02-01 10:00:00', '2024-02-01 10:00:00', 0),
('STP-005', 'L型-20x25', '半自动贴片机B-02', 'B区-工位01', '2024-02-01 10:05:00', '', 1, 'L型挡块', '2024-02-01 10:05:00', '2024-02-01 10:05:00', 0),
('STP-006', 'L型-20x25', '半自动贴片机B-03', 'B区-工位02', '2024-02-05 11:00:00', '', 1, '', '2024-02-05 11:00:00', '2024-02-05 11:00:00', 0),
('STP-007', 'U型-35x50', '大型压装机C-01', 'C区-工位01', '2024-03-10 08:30:00', '', 1, 'U型重型挡块', '2024-03-10 08:30:00', '2024-03-10 08:30:00', 0),
('STP-008', 'U型-35x50', '大型压装机C-02', 'C区-工位01', '2024-03-10 08:35:00', '', 1, '', '2024-03-10 08:35:00', '2024-03-10 08:35:00', 0),
('STP-009', 'D型-25x30', '全自动组装机A-03', 'A区-工位03', '2024-03-15 16:00:00', '', 1, '', '2024-03-15 16:00:00', '2024-03-15 16:00:00', 0),
('STP-010', 'D型-30x40', '全自动组装机A-04', 'A区-工位03', '2024-04-01 09:00:00', '', 1, '', '2024-04-01 09:00:00', '2024-04-01 09:00:00', 0);
