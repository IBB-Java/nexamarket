ALTER TABLE orders ADD COLUMN customer_email VARCHAR(320);

UPDATE orders order_row
SET customer_email = COALESCE(
    (SELECT account.email FROM user_accounts account WHERE account.id = order_row.customer_id),
    'Silinmiş kullanıcı #' || order_row.customer_id::text
);

ALTER TABLE orders ALTER COLUMN customer_email SET NOT NULL;
