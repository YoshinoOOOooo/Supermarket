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
WHERE code = 'STRAWBERRY';

INSERT INTO promotion (
    code, name, type, product_id, discount_rate,
    threshold_amount, reduction_amount, priority, enabled, start_time, end_time
)
VALUES (
    'SPEND_100_SAVE_10', 'Spend 100 Save 10', 'ORDER_THRESHOLD_REDUCTION', NULL, NULL,
    100.00, 10.00, 200, TRUE, NULL, NULL
);
