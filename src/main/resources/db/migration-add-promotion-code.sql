-- 仅用于从旧版 schema 升级一次；全新本地库直接执行 schema.sql。
ALTER TABLE promotion ADD COLUMN code VARCHAR(64) NULL AFTER id;
UPDATE promotion SET code = CONCAT('PROMO_', id) WHERE code IS NULL OR TRIM(code) = '';
ALTER TABLE promotion MODIFY code VARCHAR(64) NOT NULL;
ALTER TABLE promotion ADD UNIQUE KEY uk_promotion_code (code);
