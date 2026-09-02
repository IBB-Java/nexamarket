-- Add a presentation-ready selection only when the local database already has
-- an active seller.  A clean automated test database intentionally receives no
-- demo owner account, so this migration remains safe in every environment.
INSERT INTO products (name, description, base_price, seller_id, status, created_at, updated_at)
SELECT seed.name, seed.description, seed.base_price, seller.id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    VALUES
        ('Aria Paslanmaz Su Matarası', 'Gün boyu serin kalması için mat yüzeyli, yeniden kullanılabilir su matarası.', 529.90),
        ('Mira Keten Defter', 'Düşüncelerini ve günlük planlarını saklamak için zarif keten kapaklı defter.', 189.90),
        ('Solis Bluetooth Hoparlör', 'Kompakt tasarımda dengeli ses ve kolay taşınabilirlik.', 799.90),
        ('Orbit Masa Saati', 'Çalışma alanına sıcaklık katan, yalın pirinç görünümlü masa saati.', 449.90),
        ('Pera Mum Seti', 'Üç farklı yumuşak tonda, akşamlarını sakinleştiren kokulu mum seti.', 299.90),
        ('Atlas Sırt Çantası', 'Günlük kullanım için hafif, düzenli bölmeli ve dayanıklı sırt çantası.', 1049.90),
        ('Lento Kablosuz Şarj Standı', 'Telefonunu masanda dik konumda şarj eden sade kablosuz şarj standı.', 649.90),
        ('Noma Cam Sürahi', 'Sofralar için oluklu cam gövde ve zarif renkli kulp.', 399.90),
        ('Vela Bitki Saksısı', 'İç mekân bitkilerine eşlik eden, heykelsi formda seramik saksı.', 279.90),
        ('Riva Yoga Matı', 'Evde veya stüdyoda konforlu pratik için kaymaz yüzeyli yoga matı.', 759.90),
        ('Pixel Mekanik Klavye', 'Masaüstüne yumuşak bir renk dokunuşu katan kompakt mekanik klavye.', 1299.90),
        ('Sera Pamuklu Battaniye', 'Yumuşak pamuk dokusuyla koltuk ve yatak odaları için sıcak bir katman.', 899.90),
        ('Eko Ahşap Kesme Tahtası', 'Doğal ahşap dokulu, mutfakta günlük kullanım için sağlam kesme tahtası.', 329.90),
        ('Aura Aromaterapi Difüzörü', 'Ortamı nazikçe kokulandıran, sessiz çalışmalı seramik difüzör.', 689.90),
        ('Comet Akıllı Bileklik', 'Günlük hareketlerini takip etmene yardımcı olan hafif akıllı bileklik.', 1099.90),
        ('Cielo Kahve Öğütücü', 'Taze kahve deneyimi için ayarlanabilir öğütüm kademeli kompakt değirmen.', 1499.90),
        ('Kora Seramik Tabak Seti', 'Sofraya pastel bir uyum getiren, dört parçalı el işçiliği hissi veren set.', 749.90),
        ('Dune Güneş Gözlüğü', 'Günlük kullanıma uygun, amber tonlu hafif çerçeve ve koyu camlar.', 999.90),
        ('Halo Taşınabilir Projektör', 'Film gecelerini her odaya taşıyan kompakt, taşınabilir projektör.', 2399.90),
        ('Fika Çay Demliği', 'Demleme hazneli şeffaf cam gövdesiyle çay keyfi için zarif demlik.', 549.90)
) AS seed(name, description, base_price)
CROSS JOIN (
    SELECT MIN(id) AS id
    FROM user_accounts
    WHERE role = 'SELLER' AND status = 'ACTIVE'
) AS seller
WHERE seller.id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM products product WHERE product.name = seed.name);

INSERT INTO product_categories (product_id, category_id)
SELECT product.id, category.id
FROM (
    VALUES
        ('Aria Paslanmaz Su Matarası', 'Yaşam'), ('Mira Keten Defter', 'Yaşam'),
        ('Solis Bluetooth Hoparlör', 'Teknoloji'), ('Orbit Masa Saati', 'Ev'),
        ('Pera Mum Seti', 'Ev'), ('Atlas Sırt Çantası', 'Yaşam'),
        ('Lento Kablosuz Şarj Standı', 'Teknoloji'), ('Noma Cam Sürahi', 'Ev'),
        ('Vela Bitki Saksısı', 'Ev'), ('Riva Yoga Matı', 'Yaşam'),
        ('Pixel Mekanik Klavye', 'Teknoloji'), ('Sera Pamuklu Battaniye', 'Ev'),
        ('Eko Ahşap Kesme Tahtası', 'Ev'), ('Aura Aromaterapi Difüzörü', 'Ev'),
        ('Comet Akıllı Bileklik', 'Teknoloji'), ('Cielo Kahve Öğütücü', 'Ev'),
        ('Kora Seramik Tabak Seti', 'Ev'), ('Dune Güneş Gözlüğü', 'Yaşam'),
        ('Halo Taşınabilir Projektör', 'Teknoloji'), ('Fika Çay Demliği', 'Ev')
) AS seed(name, category_name)
JOIN products product ON product.name = seed.name
JOIN categories category ON category.name = seed.category_name
WHERE NOT EXISTS (
    SELECT 1 FROM product_categories link
    WHERE link.product_id = product.id AND link.category_id = category.id
);

INSERT INTO product_variants (product_id, sku, price, stock_quantity, version, created_at, updated_at)
SELECT product.id, seed.sku, seed.price, seed.stock_quantity, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    VALUES
        ('Aria Paslanmaz Su Matarası', 'NEXA-ARIA-007', 529.90, 16), ('Mira Keten Defter', 'NEXA-MIRA-008', 189.90, 24),
        ('Solis Bluetooth Hoparlör', 'NEXA-SOLIS-009', 799.90, 11), ('Orbit Masa Saati', 'NEXA-ORBIT-010', 449.90, 9),
        ('Pera Mum Seti', 'NEXA-PERA-011', 299.90, 18), ('Atlas Sırt Çantası', 'NEXA-ATLAS-012', 1049.90, 7),
        ('Lento Kablosuz Şarj Standı', 'NEXA-LENTO-013', 649.90, 14), ('Noma Cam Sürahi', 'NEXA-NOMA-014', 399.90, 13),
        ('Vela Bitki Saksısı', 'NEXA-VELA-015', 279.90, 10), ('Riva Yoga Matı', 'NEXA-RIVA-016', 759.90, 8),
        ('Pixel Mekanik Klavye', 'NEXA-PIXEL-017', 1299.90, 6), ('Sera Pamuklu Battaniye', 'NEXA-SERA-018', 899.90, 12),
        ('Eko Ahşap Kesme Tahtası', 'NEXA-EKO-019', 329.90, 15), ('Aura Aromaterapi Difüzörü', 'NEXA-AURA-020', 689.90, 9),
        ('Comet Akıllı Bileklik', 'NEXA-COMET-021', 1099.90, 7), ('Cielo Kahve Öğütücü', 'NEXA-CIELO-022', 1499.90, 5),
        ('Kora Seramik Tabak Seti', 'NEXA-KORA-023', 749.90, 8), ('Dune Güneş Gözlüğü', 'NEXA-DUNE-024', 999.90, 10),
        ('Halo Taşınabilir Projektör', 'NEXA-HALO-025', 2399.90, 4), ('Fika Çay Demliği', 'NEXA-FIKA-026', 549.90, 11)
) AS seed(name, sku, price, stock_quantity)
JOIN products product ON product.name = seed.name
WHERE NOT EXISTS (SELECT 1 FROM product_variants variant WHERE variant.sku = seed.sku);
