# NexaMarket — gereksinim ve teslim denetimi

Kaynak: `NexaMarket_Analist_Belgesi.docx` (v1.0). Bu kayıt, 4 Eylül 2026
itibarıyla entegre dalın kod, migration ve test denetimini gösterir. “Karşılandı”
ifadesi, yalnızca tasarım niyetini değil ilgili kodun ve en az bir doğrulama
senaryosunun bulunduğunu belirtir.

## Mimari özeti

Tek Spring Boot çalıştırılabilir dosyası içinde **Identity**, **Catalog &
Inventory**, **Commerce** ve **Engagement & Reporting** olmak üzere dört mantıksal
servis bulunur. Servis sınırlarını geçen senkron işlemler korumalı iç REST,
asenkron bildirimler ise transactional outbox ve RabbitMQ kullanır; servisler
birbirlerinin application/service/repository sınıflarını doğrudan çağırmaz.
`ServiceBoundaryArchitectureTest` bu kuralı derleme sırasında korur. Ayrıntılı
harita `docs/architecture.md` dosyasındadır.

## Fonksiyonel gereksinimler

| Gereksinim | Durum | Kod kanıtı |
| --- | --- | --- |
| FR-AUTH-01 | Karşılandı | BCrypt parola özeti, kayıt/giriş: `auth/application/AuthService` |
| FR-AUTH-02 | Karşılandı | Access + refresh JWT; refresh rotation ve revoke: `RefreshToken`, `JwtService` |
| FR-AUTH-03 | Karşılandı | `CUSTOMER`, `SELLER`, `ADMIN`, `COURIER`; self-register CUSTOMER/SELLER/COURIER, ADMIN yalnızca yönetici ataması; güncel hesap/rol kontrolü yapan JWT filtresi |
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
| FR-ORD-03 | Karşılandı | İade talep/ret/onay; müşteri sahipliği, satıcı/admin çözüm yetkisi; müşteri ve yönetim web ekranları |
| FR-ORD-04 | Karşılandı | Transactional outbox, RabbitMQ, kanal bazlı bildirim ve admin WebSocket |
| FR-ORD-05 | Karşılandı | Role göre DTO/sorgu: müşteri kendi siparişi, satıcı kendi SubOrder'ı, kurye yalnız kendi `CourierDeliveryResponse` kayıtları, yönetici tümü; tekil siparişte de sahiplik kontrolü |
| FR-ORD-06 | Karşılandı | Ayrı `DeliveryAssignment`, yalnız ADMIN tarafından manuel atama, kabul/ret/teslim/başarısızlık state machine'i, geçmiş ve güvenli yeniden atama |
| FR-USER-01 | Karşılandı | `ACTIVE`/`DISABLED`/`DELETED`, `deleted_at`, token iptali; sipariş geçmişini koruyan soft delete ve cascade'siz kullanıcı kimlikleri |
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
| JPA, PostgreSQL, Flyway | Karşılandı | V1–V20 migration; H2 test doğrulaması ve Docker varsa PostgreSQL Testcontainers testi |
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
| Testcontainers | Karşılandı | Testcontainers 2.0.5 ile PostgreSQL 16 üzerinde V1–V20 migration testi; Docker yoksa otomatik atlanır |
| p95 performans | Karşılandı | `ApiPerformanceAcceptanceTest`, ısınma sonrası katalog araması p95 değerini **300 ms altında** zorunlu tutar |
| Kritik satır kapsamı | Karşılandı | JaCoCo genel eşik %70, kritik sınıflarda ayrı %70; son ölçüm: **%77,77** (2432/3127) |
| Servisler arası iletişim | Karşılandı | İç REST + `X-Internal-Api-Key`; bildirimlerde outbox + RabbitMQ; mimari sınır testi |
| Docker Compose | Karşılandı | Uygulama, PostgreSQL, RabbitMQ, Redis, Elasticsearch ve MinIO tek `docker compose up -d` komutuyla |

## Kabul senaryoları

1. Aynı anda stok rezervasyonu: `StockServiceIntegrationTest` koşullu SQL güncellemesiyle oversell olmadığını doğrular.
2. Yinelenen webhook: `PaymentWebhookServiceTest` aynı sağlayıcı olayının ikinci kez cüzdan veya sipariş değiştirmediğini doğrular.
3. Durum geçişi: `OrderStateMachineTest` izinli/izinli olmayan geçişleri doğrular.
4. Uçtan uca akış: `CheckoutPaymentFlowIntegrationTest` kayıt → sepet → stok rezervasyonu → checkout → cüzdan ödemesi → kesin stok düşümü zincirini doğrular.
5. Migration: `PostgreSqlMigrationContainerTest`, Docker kullanılabiliyorsa migration’ları PostgreSQL 16 üzerinde yürütür.
6. Performans: `ApiPerformanceAcceptanceTest`, katalog aramasının p95 yanıt süresini 300 ms altında doğrular.
7. Servis sınırı: `ServiceBoundaryArchitectureTest`, dört mantıksal servis arasındaki yasak doğrudan Java bağımlılıklarını tarar.
8. İç API güvenliği: `InternalEndpointSecurityIntegrationTest`, anahtarsız isteğin reddedildiğini ve geçerli iç anahtarın kabul edildiğini doğrular.
9. E-posta doğrulama güvenliği: `EmailVerificationServiceTest`, altı haneli doğrulama kodunun gönderildiğini, açık kod yerine SHA-256 özetinin saklandığını ve geçerli kodun hesabı doğruladığını doğrular.
10. Kurye atama ve rol güvenliği: `RoleBasedOrderAccessApiIntegrationTest`, ADMIN ataması olmadan görünmezliği, sahipliği, kabul/ret/fail/yeniden atama/tam teslim akışını ve silinen-devre dışı kullanıcı geçmişini doğrular.
11. Ayrı teslimat state machine'i: `DeliveryAssignmentStateMachineTest` izinli ve `409` üreten geçersiz geçişleri doğrular.
12. Rol ekranı regresyonu: `FrontendRoleWorkspaceTest`, rol route'larını ve kurye ekranının eski durum-bypass endpoint'ini kullanmadığını doğrular.

Son doğrulama: Docker açıkken `./mvnw clean verify` ile **111 test, 0 hata,
0 başarısız, 0 atlandı**. PostgreSQL Testcontainers testi 20 migration'ın
tamamını gerçek PostgreSQL 16.15 örneğine uyguladı; JaCoCo genel ve kritik sınıf
eşiklerinin tamamı geçti.

## Bilerek alınan proje kararları

- Stok, yalnızca Redis kilidine güvenmez: Redisson kilidi uygulama örnekleri arasında sıralama sağlar; PostgreSQL’deki koşullu `UPDATE ... stock >= quantity` ise son savunma hattıdır.
- Bildirim gönderimi sipariş transaction’ında yapılmaz. Önce outbox’a yazılır; RabbitMQ veya e-posta geçici olarak kapalıysa sipariş kaybolmaz.
- Rapor endpoint’i hemen `PENDING` iş döndürür. Dosya hazır olduğunda aynı işin `downloadUrl` alanından alınır; bu, uzun rapor üretiminin HTTP isteğini bloke etmesini önler.
- Testler H2/bellek-içi bileşenlerle hızlıdır. Docker Desktop çalışıyorsa PostgreSQL Testcontainers doğrulaması da devreye girer.
