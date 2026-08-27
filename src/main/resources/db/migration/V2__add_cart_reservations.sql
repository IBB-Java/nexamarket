ALTER TABLE carts
    ADD COLUMN active_customer_id BIGINT;

UPDATE carts
SET active_customer_id = customer_id
WHERE status = 'ACTIVE';

ALTER TABLE carts
    ADD CONSTRAINT uk_carts_active_customer UNIQUE (active_customer_id);

ALTER TABLE cart_items
    ADD COLUMN reservation_code VARCHAR(36) NOT NULL;

ALTER TABLE cart_items
    ADD COLUMN reserved_until TIMESTAMP WITH TIME ZONE NOT NULL;
