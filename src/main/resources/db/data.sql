INSERT INTO product (code, name, unit_price, enabled)
VALUES ('APPLE', 'Apple', 8.00, TRUE)
ON DUPLICATE KEY UPDATE code = VALUES(code);

INSERT INTO product (code, name, unit_price, enabled)
VALUES ('STRAWBERRY', 'Strawberry', 13.00, TRUE)
ON DUPLICATE KEY UPDATE code = VALUES(code);

INSERT INTO product (code, name, unit_price, enabled)
VALUES ('MANGO', 'Mango', 20.00, TRUE)
ON DUPLICATE KEY UPDATE code = VALUES(code);

INSERT INTO promotion (name, type, product_id, discount_rate, priority, enabled)
SELECT 'Strawberry 80% Discount', 'PRODUCT_DISCOUNT', p.id, 0.80, 100, TRUE
FROM product p
WHERE p.code = 'STRAWBERRY'
  AND NOT EXISTS (
      SELECT 1 FROM promotion existing
      WHERE existing.type = 'PRODUCT_DISCOUNT'
        AND existing.product_id = p.id
        AND existing.discount_rate = 0.80
  );

INSERT INTO promotion (name, type, threshold_amount, reduction_amount, priority, enabled)
SELECT 'Spend 100 Save 10', 'ORDER_THRESHOLD_REDUCTION', 100.00, 10.00, 200, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM promotion existing
    WHERE existing.type = 'ORDER_THRESHOLD_REDUCTION'
      AND existing.threshold_amount = 100.00
      AND existing.reduction_amount = 10.00
);
