# NexaMarket mantıksal servis mimarisi

NexaMarket tek depoda ve tek Spring Boot çalıştırılabilir dosyasında tutulur; buna
karşın işlevler dört bağımsız mantıksal servis sınırına ayrılır. Bu düzen ders
sunumunda anlaşılır kalırken sınırların daha sonra ayrı uygulamalara taşınmasına
izin verir.

## Servis sınırları

1. **Identity:** `auth` ve `users`; hesap, JWT, roller ve satıcı profilleri.
2. **Catalog & Inventory:** `catalog` ve `stock`; ürün, varyant, görsel, arama,
   stok ve rezervasyonlar.
3. **Commerce:** `cart`, `order`, `payment`, `promotion` ve `loyalty`; satın alma
   akışı, sipariş durum makinesi, ödeme ve kampanyalar.
4. **Engagement & Reporting:** `notification` ve `report`; RabbitMQ bildirimleri,
   WebSocket ve asenkron PDF/XLSX raporları.

## İletişim kuralları

| Çağıran | Sağlayan | Taşıma |
| --- | --- | --- |
| Catalog & Inventory | Identity | Korumalı iç REST (`/internal/identity/**`) |
| Commerce | Catalog & Inventory | Korumalı iç REST (`/internal/catalog/**`, `/internal/stocks/**`) |
| Engagement & Reporting | Commerce | Korumalı iç REST (`/internal/orders/report-data/**`) |
| Commerce | Engagement | Transactional outbox + RabbitMQ |
| Engagement | Yönetici arayüzü | STOMP WebSocket |

İç REST uçları kullanıcı JWT’si kabul etmez. İstekler `X-Internal-Api-Key`
başlığıyla doğrulanır; anahtar çalışma ortamında `INTERNAL_API_KEY` değişkeniyle
verilir. Uygulama varsayılan olarak kendi `server.port` adresini kullanır. Aynı
kod ayrı süreçlere bölündüğünde `IDENTITY_SERVICE_BASE_URL`,
`CATALOG_SERVICE_BASE_URL`, `STOCK_SERVICE_BASE_URL` ve
`ORDER_SERVICE_BASE_URL` değiştirilerek çağrılar ilgili servislere yöneltilir.

`ServiceBoundaryArchitectureTest`, bir modülün başka bir servisin application
veya repository sınıflarını yeniden doğrudan içe aktarmasını build sırasında
engeller. Servisler yalnızca ortak taşıma kayıtlarını, REST gateway’lerini veya
RabbitMQ olay sözleşmelerini kullanabilir.
