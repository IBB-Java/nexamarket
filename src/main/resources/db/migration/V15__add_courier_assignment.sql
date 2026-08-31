ALTER TABLE sub_orders
    ADD COLUMN courier_id BIGINT REFERENCES user_accounts (id);

CREATE INDEX idx_sub_orders_courier_id ON sub_orders (courier_id);
