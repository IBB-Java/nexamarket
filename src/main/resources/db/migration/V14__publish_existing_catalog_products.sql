-- Older demo products were created as drafts before the seller publication
-- action existed. Make only complete, sellable records visible in the store.
UPDATE products
SET status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP
WHERE status = 'DRAFT'
  AND EXISTS (
      SELECT 1
      FROM product_variants variant
      WHERE variant.product_id = products.id
  );
