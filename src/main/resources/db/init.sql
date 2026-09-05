-- 超市项目完整初始化脚本：建库、建表并写入演示数据。
-- 本脚本可重复执行，但会恢复三种水果和两条默认促销的基准配置。

CREATE DATABASE IF NOT EXISTS supermarket
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE supermarket;

CREATE TABLE IF NOT EXISTS product (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_code (code),
    CONSTRAINT chk_product_unit_price CHECK (unit_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS promotion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(64) NOT NULL,
    product_id BIGINT NULL,
    discount_rate DECIMAL(5, 4) NULL,
    threshold_amount DECIMAL(10, 2) NULL,
    reduction_amount DECIMAL(10, 2) NULL,
    priority INT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    start_time DATETIME NULL,
    end_time DATETIME NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_promotion_code (code),
    KEY idx_promotion_enabled_type (enabled, type),
    KEY idx_promotion_product (product_id),
    CONSTRAINT fk_promotion_product FOREIGN KEY (product_id) REFERENCES product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS promotion_mutex (
    mutex_key VARCHAR(64) NOT NULL,
    PRIMARY KEY (mutex_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS customer_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no CHAR(36) NOT NULL,
    status VARCHAR(32) NOT NULL,
    original_amount DECIMAL(10, 2) NOT NULL,
    discount_amount DECIMAL(10, 2) NOT NULL,
    payable_amount DECIMAL(10, 2) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_order_no (order_no),
    KEY idx_customer_order_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_code VARCHAR(64) NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    original_amount DECIMAL(10, 2) NOT NULL,
    discount_amount DECIMAL(10, 2) NOT NULL,
    payable_amount DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_item_order (order_id),
    KEY idx_order_item_product (product_id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES customer_order (id),
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT chk_order_item_quantity CHECK (quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO promotion_mutex (mutex_key)
VALUES ('GLOBAL_THRESHOLD')
ON DUPLICATE KEY UPDATE mutex_key = VALUES(mutex_key);

INSERT INTO product (code, name, unit_price, enabled)
VALUES
    ('APPLE', 'Apple', 8.00, TRUE),
    ('STRAWBERRY', 'Strawberry', 13.00, TRUE),
    ('MANGO', 'Mango', 20.00, TRUE)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    unit_price = VALUES(unit_price),
    enabled = VALUES(enabled);

INSERT INTO promotion (
    code, name, type, product_id, discount_rate,
    threshold_amount, reduction_amount, priority, enabled, start_time, end_time
)
SELECT
    'STRAWBERRY_80', 'Strawberry 80% Discount', 'PRODUCT_DISCOUNT', id, 0.8000,
    NULL, NULL, 100, TRUE, NULL, NULL
FROM product
WHERE code = 'STRAWBERRY'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    product_id = VALUES(product_id),
    discount_rate = VALUES(discount_rate),
    threshold_amount = NULL,
    reduction_amount = NULL,
    priority = VALUES(priority),
    enabled = VALUES(enabled),
    start_time = NULL,
    end_time = NULL;

INSERT INTO promotion (
    code, name, type, product_id, discount_rate,
    threshold_amount, reduction_amount, priority, enabled, start_time, end_time
)
VALUES (
    'SPEND_100_SAVE_10', 'Spend 100 Save 10', 'ORDER_THRESHOLD_REDUCTION', NULL, NULL,
    100.00, 10.00, 200, TRUE, NULL, NULL
)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    product_id = NULL,
    discount_rate = NULL,
    threshold_amount = VALUES(threshold_amount),
    reduction_amount = VALUES(reduction_amount),
    priority = VALUES(priority),
    enabled = VALUES(enabled),
    start_time = NULL,
    end_time = NULL;
