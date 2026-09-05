INSERT INTO product (code, name, unit_price, enabled)
VALUES ('APPLE', 'Apple', 8.00, TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name), unit_price = VALUES(unit_price), enabled = VALUES(enabled);

INSERT INTO product (code, name, unit_price, enabled)
VALUES ('STRAWBERRY', 'Strawberry', 13.00, TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name), unit_price = VALUES(unit_price), enabled = VALUES(enabled);

INSERT INTO product (code, name, unit_price, enabled)
VALUES ('MANGO', 'Mango', 20.00, TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name), unit_price = VALUES(unit_price), enabled = VALUES(enabled);

INSERT INTO promotion (name, type, product_id, discount_rate, priority, enabled)
SELECT 'Strawberry 80% Discount', 'PRODUCT_DISCOUNT', p.id, 0.80, 100, TRUE
FROM product p
WHERE p.code = 'STRAWBERRY'
  AND NOT EXISTS (
      SELECT 1 FROM promotion existing
      WHERE existing.type = 'PRODUCT_DISCOUNT'
        AND existing.name = 'Strawberry 80% Discount'
  );

UPDATE promotion rule_row
JOIN product p ON p.code = 'STRAWBERRY'
SET rule_row.type = 'PRODUCT_DISCOUNT', rule_row.product_id = p.id, rule_row.discount_rate = 0.80,
    rule_row.threshold_amount = NULL, rule_row.reduction_amount = NULL,
    rule_row.priority = 100, rule_row.enabled = TRUE, rule_row.start_time = NULL, rule_row.end_time = NULL
WHERE rule_row.name = 'Strawberry 80% Discount';

INSERT INTO promotion (name, type, threshold_amount, reduction_amount, priority, enabled)
SELECT 'Spend 100 Save 10', 'ORDER_THRESHOLD_REDUCTION', 100.00, 10.00, 200, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM promotion existing
    WHERE existing.name = 'Spend 100 Save 10'
);

UPDATE promotion
SET type = 'ORDER_THRESHOLD_REDUCTION', product_id = NULL, discount_rate = NULL,
    threshold_amount = 100.00, reduction_amount = 10.00, priority = 200, enabled = TRUE,
    start_time = NULL, end_time = NULL
WHERE name = 'Spend 100 Save 10';
