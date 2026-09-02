-- Keep historical order references intact while removing this retired listing
-- from the storefront and seller inventory.
UPDATE products
SET status = 'DELETED', updated_at = CURRENT_TIMESTAMP
WHERE LOWER(name) = 'melike mali'
  AND status <> 'DELETED';
