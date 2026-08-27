# NexaMarket — gereksinim ve teslim denetimi

Kaynak: `NexaMarket_Analist_Belgesi.docx` (v1.0). Bu kayıt, 27 Ağustos 2026
itibarıyla entegre dalın kod, migration ve test denetimini gösterir. “Karşılandı”
ifadesi, yalnızca tasarım niyetini değil ilgili kodun ve en az bir doğrulama
senaryosunun bulunduğunu belirtir.

## Mimari özeti

Tek Spring Boot uygulamasıdır; modüller `auth`, `catalog`, `users`, `stock`,
`cart`, `order`, `payment`, `notification`, `promotion`, `loyalty` ve `report`
paketleriyle mantıksal servis sınırlarına ayrılmıştır. Modüller aynı veritabanını
paylaşsa da aralarındaki çağrılar arayüz/gateway üzerinden kurulur. Bu yaklaşım,
ders projesi için tek komutta çalıştırılabilirlik ile gerçek servis ayrımına
yakın bir yapı arasında dengedir.

## Fonksiyonel gereksinimler

| Gereksinim | Durum | Kod kanıtı |
| --- | --- | --- |
| FR-AUTH-01 | Karşılandı | BCrypt parola özeti, kayıt/giriş: `auth/application/AuthService` |
| FR-AUTH-02 | Karşılandı | Access + refresh JWT; refresh rotation ve revoke: `RefreshToken`, `JwtService` |
| FR-AUTH-03 | Karşılandı | `CUSTOMER`, `SELLER`, `ADMIN`, `COURIER` rolleri; Security + method güvenliği |
| FR-AUTH-04 | Karşılandı | Yapılandırılabilir başarısız giriş sayısı/kilit süresi: `AuthProperties` |
| FR-CAT-01 | Karşılandı | Kategori, ürün, varyant, nitelik ve varyant stoğu: `catalog/entity` |
| FR-CAT-02 | Karşılandı | Yükleme sonrası ayrı executor’da thumbnail üretimi; MinIO nesne depolama |
| FR-CAT-03 | Karşılandı | Elasticsearch araması: isim, kategori, fiyat, satıcı puanı filtreleri |
| FR-CAT-04 | Karşılandı | Olay sonrası yeniden indeksleme, retry ve en fazla gecikme sınırı |
| FR-CART-01 | Karşılandı | SQL koşullu stok düşümü + Redisson dağıtık kilit; oversell yarış testi |
| FR-CART-02 | Karşılandı | Rezervasyon sonu zamanlayıcıyla stok iadesi |
| FR-CART-03 | Karşılandı | Satıcıya göre alt sipariş bölme; tek ana sipariş ve tek ödeme |
| FR-ORD-01 | Karşılandı | `OrderStateMachine`, geçersiz geçişte `409 Conflict` |
| FR-ORD-02 | Karşılandı | Ödeme zaman aşımı: rezervasyonları bırakır, alt/ana siparişi iptal eder |
| FR-ORD-03 | Karşılandı | İade talep/ret/onay; müşteri sahipliği, satıcı/admin çözüm yetkisi |
| FR-ORD-04 | Karşılandı | Transactional outbox, RabbitMQ, kanal bazlı bildirim ve admin WebSocket |
| FR-PAY-01 | Karşılandı | Gecikmeli/başarılı/başarısız/tekrarlı callback üreten mock sağlayıcı |
| FR-PAY-02 | Karşılandı | Benzersiz `providerEventId` kaydı; yinelenen webhook etkisiz |
| FR-PAY-03 | Karşılandı | Bekleyen işlemler polling, retry/circuit breaker koruması |
| FR-PAY-04 | Karşılandı | Cüzdan + kart bölüşümü, tutar doğrulaması, başarısız kartta cüzdan iadesi |
| FR-PROMO-01 | Karşılandı | Veri tabanlı sabit/yüzde kampanya; stacking denetimi ve %70 tavan |
| FR-PROMO-02 | Karşılandı | Teslimde puan ledger kaydı, onaylanan iadede ters kayıt |
| FR-NOTIF-01 | Karşılandı | Email, in-app, SMS sender arayüzü ve güvenli mock uygulamaları |
| FR-NOTIF-02 | Karşılandı | Kritik yol dışı outbox, retry, hata durumu ve tekilleştirme |
| FR-REP-01 | Karşılandı | Satıcı satış PDF/XLSX işi; executor’da arka plan üretimi ve indirme |
| FR-REP-02 | Karşılandı | UTC günlük yönetici raporu: brüt satış, %10 komisyon ve iade sayısı |

## Teknik ve kabul gereksinimleri

| Konu | Durum | Not |
| --- | --- | --- |
| Java 17+ / Spring Boot 3 | Karşılandı | Java 21 hedefi, Spring Boot 3.5 |
| JPA, PostgreSQL, Flyway | Karşılandı | V1–V13 migration; H2 test doğrulaması ve Docker varsa PostgreSQL Testcontainers testi |
| RabbitMQ | Karşılandı | Kalıcı exchange/queue + outbox relay |
| Redis / Redisson | Karşılandı | 5 saniyelik katalog arama cache’i, stok varyantı Redisson kilidi |
| Elasticsearch | Karşılandı | Gerçek ve bellek-içi gateway seçenekleri |
| MinIO | Karşılandı | Ürün görselleri ve rapor dosyaları için `ObjectStorage` |
| PDF / Excel | Karşılandı | OpenPDF ve Apache POI ile üretilir |
| WebSocket | Karşılandı | STOMP `/ws`, yönetici konusu `/topic/admin/orders` |
| Resilience4j | Karşılandı | Ödeme polling için retry + circuit breaker fallback |
| OpenAPI/Swagger | Karşılandı | Uygulama çalışınca `/swagger-ui.html` |
| JSON log + correlation ID | Karşılandı | Logstash biçimi ve `X-Correlation-Id` |
| Bucket4j rate limit | Karşılandı | IP başına yapılandırılabilir bir dakikalık limit |
| Audit | Karşılandı | Değiştiren HTTP işlemleri için `audit_logs`; parola/gövde saklanmaz |
| Testcontainers | Karşılandı | PostgreSQL migration smoke testi; Docker yoksa otomatik atlanır |
| Kritik satır kapsamı | Karşılandı | JaCoCo `verify` eşiği %70; son ölçüm: **%75,81** (1811/2389) |
| Docker Compose | Karşılandı | PostgreSQL, RabbitMQ, Redis, Elasticsearch ve MinIO tek `docker compose up -d` komutuyla |

## Kabul senaryoları

1. Aynı anda stok rezervasyonu: `StockServiceIntegrationTest` koşullu SQL güncellemesiyle oversell olmadığını doğrular.
2. Yinelenen webhook: `PaymentWebhookServiceTest` aynı sağlayıcı olayının ikinci kez cüzdan veya sipariş değiştirmediğini doğrular.
3. Durum geçişi: `OrderStateMachineTest` izinli/izinli olmayan geçişleri doğrular.
4. Uçtan uca akış: `CheckoutPaymentFlowIntegrationTest` kayıt → sepet → stok rezervasyonu → checkout → cüzdan ödemesi → kesin stok düşümü zincirini doğrular.
5. Migration: `PostgreSqlMigrationContainerTest`, Docker kullanılabiliyorsa migration’ları PostgreSQL 16 üzerinde yürütür.

## Bilerek alınan proje kararları

- Stok, yalnızca Redis kilidine güvenmez: Redisson kilidi uygulama örnekleri arasında sıralama sağlar; PostgreSQL’deki koşullu `UPDATE ... stock >= quantity` ise son savunma hattıdır.
- Bildirim gönderimi sipariş transaction’ında yapılmaz. Önce outbox’a yazılır; RabbitMQ veya e-posta geçici olarak kapalıysa sipariş kaybolmaz.
- Rapor endpoint’i hemen `PENDING` iş döndürür. Dosya hazır olduğunda aynı işin `downloadUrl` alanından alınır; bu, uzun rapor üretiminin HTTP isteğini bloke etmesini önler.
- Testler H2/bellek-içi bileşenlerle hızlıdır. Docker Desktop çalışıyorsa PostgreSQL Testcontainers doğrulaması da devreye girer.
