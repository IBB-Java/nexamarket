# NexaMarket gereksinim takip kaydı

Kaynak: `NexaMarket_Analist_Belgesi.docx`, sürüm 1.0. Bu dosya, belgedeki
gereksinimlerin unutulmaması ve her kararın gerekçesinin kayda geçmesi için
proje boyunca güncellenecektir.

## Varsayımlar

1. Proje tek repoda başlayacak, ancak Catalog, Order, Payment ve Notification
   mantıksal servis sınırlarıyla ayrılacaktır. Servisler arası çağrılar REST ya
   da mesaj kuyruğu üzerinden yapılacaktır.
2. Stok miktarı ve rezervasyonun tek sahibi Catalog Service'tir. Cart Service
   doğrudan katalog tablolarını değiştirmez.
3. Kimlik doğrulama tamamlanana kadar sepet API'si `customerId` değerini istek
   gövdesinden alır. JWT tamamlandığında bu değer doğrulanmış erişim tokenından
   çıkarılacaktır.

## Fonksiyonel gereksinimler

| Gereksinim | Durum | Not |
| --- | --- | --- |
| FR-AUTH-01…04 | Planlandı | Kayıt/giriş, JWT, rol yetkisi ve giriş kilidi. |
| FR-CAT-01…04 | Planlandı | Ürün/varyant/kategori, görsel, arama ve indeksleme. |
| FR-CART-01 | Devam ediyor | Sepete ekleme API'si ve Catalog Service REST rezervasyon sınırı hazır; gerçek stok servisi ve yarış testi eksik. |
| FR-CART-02 | Planlandı | Rezervasyon sonlandırma zamanlayıcısı veya gecikmeli kuyruk eklenecek. |
| FR-CART-03 | Planlandı | Ödeme sonrasında satıcı bazlı alt siparişler Order Service'te oluşturulacak. |
| FR-ORD-01…04 | Planlandı | Merkezi durum makinesi, zaman aşımı, iade ve olay yayını. |
| FR-PAY-01…04 | Planlandı | Mock provider, idempotent webhook, sorgulama ve kısmi ödeme. |
| FR-PROMO-01…02 | Planlandı | Veri odaklı indirim kuralları ve sadakat defteri. |
| FR-NOTIF-01…02 | Planlandı | Kuyruk tüketicisi, e-posta/SMS/uygulama içi bildirim ve retry. |
| FR-REP-01…02 | Planlandı | Arka plan PDF/XLSX raporları ve günlük yönetici özeti. |

## Teknik ve kabul gereksinimleri

| Konu | Durum | Not |
| --- | --- | --- |
| Java 17+ ve Spring Boot 3.x | Tamamlandı | Java 21 hedefi ve Spring Boot 3.5.0. |
| JPA/Hibernate, PostgreSQL, Flyway | Devam ediyor | Şema migration'ları ve Docker PostgreSQL tanımı mevcut; gerçek PostgreSQL doğrulaması Docker Desktop açıldığında yapılacak. |
| RabbitMQ/Kafka, Redis, Redisson | Planlandı | Olaylar, önbellek ve dağıtık stok kilidi. |
| Elasticsearch/OpenSearch | Planlandı | Ürün arama ve indeks güncellemesi. |
| MinIO, PDF, Excel | Planlandı | Görseller ile rapor/fatura çıktıları. |
| WebSocket, Resilience4j, OpenAPI | Planlandı | Canlı admin akışı, dış servis toleransı, Swagger. |
| JSON log, correlation ID, rate limit, audit | Planlandı | Gözlemlenebilirlik, güvenlik ve denetim. |
| Testcontainers ve kabul senaryoları | Planlandı | Overselling, webhook idempotency, durum geçişi ve %70 kritik modül hedefi. |
| Docker Compose tek komut | Devam ediyor | PostgreSQL tanımlı; diğer servisler aşamalı olarak compose dosyasına eklenecek. |
