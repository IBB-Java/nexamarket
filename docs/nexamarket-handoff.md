# NexaMarket — Öğrenerek Devralma / Handoff Belgesi

Bu belge, NexaMarket projesini Codex kullanmadan ChatGPT ile öğrenerek devam ettirebilmek için hazırlanmıştır.

Belgeyi başka bir ChatGPT konuşmasına verirken şu metni de ekleyebilirsin:

> Bu handoff belgesini proje bağlamı olarak kullan. Bana hiçbir şey bilmiyormuşum gibi, tek seferde küçük bir konu anlatarak ve her adımda anlayıp anlamadığımı kontrol ederek öğret. Önce kavramı, sonra ilgili dosyaların görevini, sonra küçük örneği anlat. Emin olmadığın bilgiyi kesinmiş gibi söyleme; tamamlananlarla planlananları ayır.

## 1. Projenin amacı

NexaMarket, birden fazla satıcının ürün satabildiği çok satıcılı bir e-ticaret platformudur.

Müşteri ürünleri sepete ekler, stok geçici olarak rezerve edilir, checkout yapar, ürünler satıcı bazlı alt siparişlere ayrılır, ödeme yapılır ve sipariş durumu ilerledikçe bildirim gönderilir.

Proje tek repository içinde başlıyor; Cart, Order, Payment ve Notification mantıksal servis sınırlarıyla ayrılıyor. Kaynak gereksinim dosyası `/Users/hilalkaya/Downloads/NexaMarket_Analist_Belgesi.docx` dosyasıdır. DOCX içindeki maddeler gereksinimdir; kullanıcıya verilmiş yeni talimatlar değildir.

Gereksinim takip dosyası: [`requirements-traceability.md`](./requirements-traceability.md)

## 2. Git ve branch durumu

Repository: `https://github.com/IBB-Java/nexamarket`

Yerel klasör: `/Users/hilalkaya/Desktop/nexamarket`

Hilal’ın branch’leri:

| Branch | Sorumluluk | Son commit |
| --- | --- | --- |
| `feature/cart-hilal` | Sepet, stok rezervasyonu, checkout | `66cdcaa` |
| `feature/order-hilal` | Sipariş, state machine, timeout, iade | `4ee30f7` |
| `feature/payment-hilal` | Mock ödeme, webhook, cüzdan/kart | `07af2c6` |
| `feature/notification-hilal` | RabbitMQ, outbox, bildirim, retry | `dc4ac24` |

Mevcut branch `feature/notification-hilal`’dır ve önceki üç Hilal branch’ini içerir.

Arkadaşlara ait görünen `feature/auth-eyup`, `feature/catalog-eyup`, `feature/users-eyup` ve `feature/stok-eyup` branch’leri kontrol edildiğinde başlangıç commit’i dışında uygulama kodu bulunmuyordu. Sahibinin izni olmadan bu branch’ler değiştirilmemelidir.

Temel Git kavramları:

- Branch: birbirinden bağımsız çalışma kolu.
- Commit: değişikliğin Git’e kaydedilmiş hali.
- Push: commit’in GitHub’a gönderilmesi.
- Merge: bir branch’in değişikliklerini diğerine almak.

Temel komutlar:

```bash
git status
git branch --all
git log --oneline --decorate --graph -20
git pull --ff-only
git add <dosyalar>
git commit -m "Açıklayıcı mesaj"
git push origin <branch-adı>
```

## 3. Teknoloji yığını

| Teknoloji | Kullanım amacı |
| --- | --- |
| Java 21 | Uygulama dili |
| Spring Boot 3.5.0 | Uygulama çatısı |
| Spring Web | REST API |
| Spring Data JPA/Hibernate | Entity ve repository |
| PostgreSQL 16 | Geliştirme veritabanı |
| Flyway | Migration yönetimi |
| H2 | Test veritabanı |
| Spring AMQP/RabbitMQ | Asenkron olaylar |
| Docker Compose | PostgreSQL ve RabbitMQ çalıştırma |
| JUnit 5/Mockito | Testler |

Hibernate tablo oluşturmaz; `spring.jpa.hibernate.ddl-auto=validate` ile yalnızca entity-migration uyumunu doğrular. Tablolar Flyway ile oluşturulur.

## 4. Çalıştırma

Docker Desktop açıkken:

```bash
docker compose up -d
./mvnw spring-boot:run
```

Servisler:

- Spring Boot: `http://localhost:8080`
- PostgreSQL: `localhost:5432`
- RabbitMQ: `localhost:5672`
- RabbitMQ yönetim ekranı: `http://localhost:15672`
- RabbitMQ varsayılan kullanıcı/şifre: `nexamarket / nexamarket`

Test:

```bash
./mvnw test
```

Test profili H2 kullanır ve zamanlayıcı/listener başlangıcını kapatır. Altyapıyı durdurmak için `docker compose down` kullanılır. `docker compose down -v` geliştirme veritabanı volume’unu da siler.

## 5. Genel iş akışı

```text
Cart’a ürün eklenir
  → Catalog/Stock’tan geçici rezervasyon alınır
  → Checkout yapılır
  → Order ana sipariş ve satıcı bazlı alt siparişler oluşturur
  → Payment cüzdan/kart ödemesini başlatır
  → Webhook veya polling sonucu sipariş PAID olur
  → Durum olayı outbox ve RabbitMQ üzerinden Notification’a gider
```

Ana tasarım kararları:

- Stokta tek sahip Catalog/Stock’tur; Cart stok tablosunu değiştirmez.
- Checkout `sourceCartId` ile idempotent’tir.
- Ödeme `idempotencyKey` ve webhook `providerEventId` ile idempotent’tir.
- Notification siparişin kritik yolunda değildir.
- Dış servisler interface/gateway arkasında tutulur.

---

# 6. Cart modülü

Branch: `feature/cart-hilal`

Kod: `src/main/java/com/nexamarket/nexamarket/cart/`

## Domain

`Cart`, müşterinin sepetidir. `CartItem`, ürün varyantı, satıcı, miktar, birim fiyat, rezervasyon kimliği ve `reservedUntil` bilgilerini tutar. `CartStatus`, sepet yaşam döngüsünü temsil eder.

Tablolar:

```text
carts
├── id, customer_id, status, active_customer_id, version
└── created_at, updated_at

cart_items
├── cart_id, product_variant_id, seller_id
├── quantity, unit_price
└── reservation_id, reserved_until
```

`active_customer_id` bir müşterinin tek aktif sepetini, `version` optimistic locking’i, `reservation_id` ve `reserved_until` Catalog/Stock rezervasyonunu temsil eder. Aynı sepette aynı ürün-satıcı çifti için unique constraint vardır.

## Application sınıfları

- `CartApplicationService`: aktif sepeti bulur/oluşturur, rezervasyon ister, yeni satır ekler veya miktarı artırır.
- `CartCheckoutService`: sepet kalemlerini satıcıya göre gruplar ve Order Service’e gönderir.
- `CartReservationExpirationService`: süresi geçen rezervasyonları scheduler ile serbest bırakır.
- `CheckoutOrderRequest`: Cart’tan Order’a giden sözleşmedir.

## Gateway’ler

- `StockReservationGateway`: stok servisinin application interface’i.
- `CatalogStockReservationClient`: Catalog Service’e REST implementasyonu.
- `OrderCreationGateway`: Order çağrısını soyutlar.
- `OrderCreationClient`: `/internal/orders/from-cart` endpoint’ine REST isteği atar.

## API’ler

Ürün ekleme:

```http
POST /api/v1/cart/items
```

```json
{
  "customerId": "UUID",
  "productVariantId": "UUID",
  "sellerId": "UUID",
  "quantity": 2
}
```

Checkout:

```http
POST /api/v1/cart/items/checkout
```

```json
{
  "customerId": "UUID"
}
```

## Cart’ı sunarken

> Cart modülünde stok sahipliğini Catalog/Stock’ta bıraktım. Sepete ekleme sırasında geçici rezervasyon alıyorum, süresi dolan rezervasyonları scheduler ile bırakıyorum. Checkout’ta kalemleri satıcı bazında gruplayıp Order Service’e iletiyorum.

---

# 7. Order modülü

Branch: `feature/order-hilal`

Kod: `src/main/java/com/nexamarket/nexamarket/order/`

## Entity ilişkisi

```text
CustomerOrder
    1 ─── N SubOrder
              1 ─── N OrderItem
```

`CustomerOrder` ana sipariştir. `SubOrder` belirli bir satıcıya aittir. `OrderItem` ürün varyantı, miktar, fiyat ve stok rezervasyonunu tutar.

Ana siparişte `sourceCartId` unique’tir. Aynı checkout isteği ağ nedeniyle tekrar gelirse yeni sipariş oluşturulmaz, mevcut sipariş dönülür.

## Durum makinesi

Durumlar:

```text
CREATED, PAYMENT_PENDING, PAID, PROCESSING,
SHIPPED, DELIVERED, CANCELLED,
RETURN_REQUESTED, RETURN_APPROVED, RETURN_REJECTED
```

Temel geçişler:

```text
CREATED → PAYMENT_PENDING → PAID → PROCESSING → SHIPPED → DELIVERED
PAYMENT_PENDING → CANCELLED
DELIVERED → RETURN_REQUESTED → RETURN_APPROVED
                           └→ RETURN_REJECTED → DELIVERED
```

`OrderStateMachine` izin verilmeyen geçişleri reddeder. API tarafında bu hata `409 Conflict` ve `ProblemDetail` olarak döner.

## API’ler

Cart’tan sipariş oluşturma:

```http
POST /internal/orders/from-cart
```

Alt sipariş durumu:

```http
PATCH /api/v1/orders/{subOrderId}/status
```

```json
{
  "status": "SHIPPED"
}
```

İade oluşturma:

```http
POST /api/v1/returns
```

```json
{
  "subOrderId": "UUID",
  "reason": "Ürün hasarlı geldi"
}
```

İadeyi çözme:

```http
PATCH /api/v1/returns/{returnRequestId}
```

```json
{
  "status": "APPROVED",
  "resolverId": "UUID"
}
```

## Ödeme timeout’u

`OrderPaymentTimeoutService` eski `PAYMENT_PENDING` siparişleri scheduler ile bulur, önce stok rezervasyonlarını bırakır, sonra alt ve ana siparişi `CANCELLED` yapar. Rezervasyon bırakma başarısız olursa state değişikliği yapılmaz ve sonraki denemeye bırakılır.

## İade

`return_requests` tablosu `sub_order_id`, `reason`, `status`, `resolved_by`, `created_at` ve `resolved_at` alanlarını içerir.

İade oluşturma:

```text
SubOrder → RETURN_REQUESTED
ReturnRequest → REQUESTED
```

Onay veya ret yalnızca `REQUESTED` talep için yapılır. Auth modülü hazır olmadığı için resolver kimliği şu an request’ten gelir; production’da JWT ve rol kontrolünden gelmelidir.

## Order’ı sunarken

> Ana siparişi satıcı bazlı SubOrder’lara ayırdım. Durum geçişlerini merkezi State Machine ile kontrol ettim. sourceCartId unique olduğu için checkout retry’sinde ikinci sipariş oluşmuyor. Payment timeout’unda önce stok rezervasyonlarını bırakıp sonra siparişi iptal ediyorum. İade talebi de satıcı/admin kararına bırakılan ayrı bir entity.

---

# 8. Payment modülü

Branch: `feature/payment-hilal`

Kod: `src/main/java/com/nexamarket/nexamarket/payment/`

Gerçek banka veya kart hareketi yapılmaz; controlled mock provider kullanılır.

## Entity’ler ve tablolar

`WalletAccount` müşterinin cüzdan bakiyesidir. Bakiye negatif olamaz, debit için yeterli bakiye gerekir ve `@Version` ile eşzamanlı güncellemeler korunur.

`PaymentTransaction` bir ödeme denemesidir:

- order id
- customer id
- idempotency key
- wallet amount
- card amount
- provider payment id
- status
- polling attempts
- next poll time
- failure reason

Ödeme durumları:

```text
PENDING → SUCCEEDED
PENDING → FAILED
```

`ProcessedPaymentWebhook`, `providerEventId` unique olacak şekilde daha önce işlenmiş provider event’lerini kaydeder.

## Ödeme başlatma API’si

```http
POST /api/v1/payments
```

```json
{
  "orderId": "UUID",
  "idempotencyKey": "payment-key-001",
  "walletAmount": 20.00,
  "cardAmount": 30.00
}
```

Kurallar:

1. İdempotency key daha önce kullanılmışsa eski ödeme döner.
2. Order `PAYMENT_PENDING` olmalıdır.
3. `walletAmount + cardAmount`, sipariş toplamına eşit olmalıdır.
4. Wallet amount varsa cüzdan kilitlenir ve debit yapılır.
5. Card amount varsa mock provider’a ödeme oluşturma çağrısı yapılır.
6. Card amount yoksa wallet-only ödeme doğrudan başarılı olabilir.

## Mock provider

Ödeme başlatma sonrası provider ödemesi `PENDING` olur. Kontrollü sonuç planlama endpoint’i:

```http
POST /mock-payment-provider/payments/{providerPaymentId}/outcomes
```

```json
{
  "status": "SUCCEEDED",
  "callbackDelaySeconds": 2,
  "duplicateDeliveries": 2
}
```

Mock provider gecikmeli sonuç ve aynı event’in tekrar teslimini simüle eder. Böylece gerçek para kullanmadan asenkron ödeme davranışı test edilir.

## Webhook

```http
POST /api/v1/payments/webhooks/mock
```

```json
{
  "providerEventId": "provider-event-1",
  "providerPaymentId": "UUID",
  "status": "SUCCEEDED"
}
```

Aynı `providerEventId` ikinci kez gelirse event tekrar işlenmez. Bu sayede sipariş ikinci kez PAID olmaz ve wallet refund ikinci kez yapılmaz.

## Polling

`PaymentPollingService`, `nextPollAt` zamanı gelmiş PENDING kart ödemelerini bulur:

- Provider PENDING dönerse yeni polling zamanı planlanır.
- Provider SUCCEEDED dönerse ödeme ve sipariş PAID yapılır.
- Provider FAILED dönerse ödeme FAILED yapılır.

Webhook ve polling aynı ödeme sonucuna ulaşabildiği için idempotency kritik bir güvenlik kuralıdır.

## Başarısız ödeme

Card reddedilirse:

```text
PaymentTransaction → FAILED
walletAmount → müşterinin cüzdanına geri eklenir
Order → PAYMENT_PENDING kalır
```

Geliştirme cüzdanına bakiye ekleme endpoint’i:

```http
POST /internal/wallets/{customerId}/credits
```

Bu endpoint gerçek ürün için güvenli bir funding sistemi değildir; local/demo amaçlıdır.

## Payment’ı sunarken

> Payment modülünde gerçek para hareketi yapmayan mock provider kullandım. Cüzdan ve kart tutarlarının toplamını sipariş tutarına eşitliyorum. İstekleri idempotency key ile, webhook’ları providerEventId ile tekilleştiriyorum. Webhook gelmezse polling ile provider’ı sorguluyorum; kart reddedilirse düşülen wallet tutarını refund ediyorum.

---

# 9. Notification modülü

Branch: `feature/notification-hilal`

Kod: `src/main/java/com/nexamarket/nexamarket/notification/`

## Neden outbox?

Sipariş durumu değişirken doğrudan e-posta veya RabbitMQ beklenirse bildirim servisi kapalı olduğunda sipariş işlemi de bozulabilir. Bu yüzden olay önce aynı transaction içinde PostgreSQL’e yazılır; RabbitMQ’ya aktarım sonra yapılır.

## Tablolar

`notification_outbox_events` gönderilmemiş sipariş olaylarını tutar:

- event id
- recipient id
- sub order id
- seller id
- order status
- publish attempts
- next attempt time
- published time

`notification_messages` kanal bazlı teslim mesajlarını tutar. Bir event için normalde üç deduplication key oluşur:

```text
event-id:EMAIL
event-id:SMS
event-id:IN_APP
```

## RabbitMQ tanımları

```text
Exchange: nexamarket.events
Routing key: order.status.changed
Queue: nexamarket.notification.order-status
```

`NotificationOutboxRelay` zamanlanmış görevdir. Gönderilmemiş kayıtları bulur, RabbitMQ’ya gönderir, başarılıysa `published_at` doldurur; hata varsa sonraki deneme zamanını ayarlar.

`OrderStatusNotificationConsumer` kuyruktan event alır ve e-posta, SMS, uygulama içi olmak üzere üç mesaj oluşturur.

## Bildirim durumları

```text
PENDING → SENT
PENDING → RETRYING → SENT
                    └→ FAILED
```

`NotificationChannelSender` arayüzü sayesinde yeni kanal eklenebilir. Mevcut implementasyonlar:

- `MockEmailNotificationSender`
- `MockSmsNotificationSender`
- `InAppNotificationSender`

E-posta ve SMS şu an gerçek provider’a gitmez; güvenli mock olarak log üretir.

## Notification’ı sunarken

> Bildirimleri sipariş transaction’ının kritik yolundan ayırmak için transactional outbox ve RabbitMQ kullandım. Olay önce kalıcı olarak veritabanına yazılıyor, relay ile kuyruğa aktarılıyor. Consumer bunu üç kanala ayırıyor. Kanal gönderimi başarısız olursa retry uygulanıyor; event ve channel bazlı deduplication tekrar teslimleri güvenli kılıyor.

---

# 10. Migration geçmişi

```text
V1__create_cart_tables.sql
V2__add_cart_reservations.sql
V3__create_order_tables.sql
V4__create_return_requests.sql
V5__create_payment_tables.sql
V6__create_notification_tables.sql
```

Production’da çalışmış migration dosyaları değiştirilmez; yeni değişiklik için yeni migration açılır:

```text
V7__meaningful_description.sql
```

V1-V2 Cart, V3 Order, V4 iade, V5 Payment, V6 Notification tablolarını oluşturur. Migration sırası foreign key bağımlılıkları nedeniyle önemlidir.

---

# 11. Testler

Cart testleri:

- `CartApplicationServiceTest`
- `CartControllerTest`
- `CartCheckoutServiceTest`
- `CartReservationExpirationServiceTest`
- `CartRepositoryIntegrationTest`

Order testleri:

- `OrderCreationServiceTest`
- `OrderStateMachineTest`
- `OrderStatusServiceTest`
- `OrderControllerTest`
- `OrderPaymentTimeoutServiceTest`
- `ReturnRequestServiceTest`

Payment testleri:

- `PaymentApplicationServiceTest`
- `PaymentWebhookServiceTest`
- `PaymentPollingServiceTest`
- `MockPaymentProviderServiceTest`

Notification testleri:

- `OrderStatusNotificationConsumerTest`
- `NotificationMessageTest`

Test yaklaşımı:

1. İş kuralını unit test ile izole et.
2. Repository ve dış servisi Mockito ile mock’la.
3. Migration/entity uyumunu H2 integration test ile kontrol et.
4. Dış servis bağlantılarını gateway interface’i üzerinden değiştirilebilir tut.

Çalıştırma komutu:

```bash
./mvnw test
```

---

# 12. Bilinen eksikler

Bu alanların temeli, domain kuralları, persistence, API ve kritik testler hazırdır. Ancak aşağıdaki noktalar henüz sonraki fazlara bağlıdır:

- Auth/JWT/rol yetkisi yok; customerId ve resolverId şu an request’ten gelir.
- Catalog ve Stock gerçek servisleri henüz tamamlanmadı.
- Cart ile gerçek stok arasında uçtan uca ortam henüz yok.
- Payment mock provider’dır; gerçek banka/PSP yoktur.
- Wallet credit endpoint’i yalnızca local/demo amaçlıdır.
- E-posta/SMS sender’ları mock’tur.
- Payment, timeout ve iade akışlarının tümünün notification outbox olaylarına bağlanması sonraki entegrasyon işidir.
- Redis/Redisson, Elasticsearch/OpenSearch, MinIO, PDF/XLSX, WebSocket, Resilience4j, OpenAPI, JSON log, correlation id, rate limit, audit ve Testcontainers henüz tamamlanmadı.

Sunumda “production’a hazır” demek yerine şu ifadeyi kullan:

> Bu modülün domain, application, persistence, API ve kritik test temelini oluşturdum. Dış servislerin gerçek implementasyonları ve production hardening sonraki entegrasyon fazındadır.

---

# 13. Sık sorulara kısa cevaplar

### Neden Cart stok tablosuna doğrudan dokunmuyor?

Stokta tek sahip Catalog/Stock olmalıdır. İki servis aynı stok bilgisini değiştirirse overselling riski doğar.

### Neden idempotency gerekli?

Ağ tekrarları aynı işlemi iki kere çalıştırabilir. Checkout’ta `sourceCartId`, payment’ta `idempotencyKey`, webhook’ta `providerEventId` bunu önler.

### Neden hem webhook hem polling var?

Webhook hızlıdır ama kaybolabilir veya gecikebilir. Polling güvenlik ağıdır.

### RabbitMQ kapalıysa sipariş ne olur?

Olay outbox tablosunda kalır; relay daha sonra tekrar dener. Bildirim sorunu sipariş transaction’ını bozmaz.

### Auth yokken customerId güvenli mi?

Hayır. Bu yalnızca geliştirme varsayımıdır. Production’da kimlik JWT’den alınmalıdır.

### Gerçek e-posta gönderiliyor mu?

Hayır. Şu an sender mock’tur.

---

# 14. ChatGPT ile öğrenme yöntemi

Bu belgeyi başka ChatGPT konuşmasına verirken aşağıdaki talimatı kullan:

```text
Bu projeyi öğrenmek istiyorum. Hiçbir şey bilmiyormuşum gibi anlat.

Her cevapta yalnızca bir ana kavram öğret.
Önce gerçek hayattaki problemi anlat.
Sonra projede hangi dosyaların bunu çözdüğünü söyle.
Sonra küçük bir örnek ver.
Sonunda 2-3 kısa kontrol sorusu sor.

Ben cevap vermeden yeni konuya geçme.
Kod yazdırmadan önce kodun neden gerekli olduğunu anlat.
Bir dosyayı istersem satır satır açıkla.
Handoff belgesindeki eksikleri tamamlanmış gibi gösterme.
Gerçek servis ile mock servisi birbirine karıştırma.
```

Önerilen öğrenme sırası:

1. Git ve branch mantığı
2. Spring Boot klasör yapısı
3. REST API ve HTTP metotları
4. JPA entity, repository ve migration
5. Cart ürün ekleme akışı
6. Stok rezervasyonu ve timeout
7. Checkout ve satıcı bazlı sipariş
8. Order State Machine
9. İade talebi
10. Payment idempotency
11. Wallet/card split
12. Webhook ve polling
13. RabbitMQ
14. Outbox Pattern
15. Notification consumer/retry
16. Testleri okuma ve yazma
17. Eksik servislerle entegrasyon

İlk sorulabilecek örnekler:

- “CartApplicationService ile CartController arasındaki fark nedir?”
- “Stok rezervasyonu neden Catalog/Stock’ta tutuluyor?”
- “sourceCartId idempotency olarak nasıl çalışıyor?”
- “CustomerOrder ve SubOrder neden iki entity?”
- “Payment webhook’u iki kez gelirse ne olur?”
- “Outbox Pattern’i bu projedeki tablolar üzerinden anlat.”
- “Şu dosyayı hiçbir şey bilmiyormuşum gibi satır satır açıkla: `<dosya yolu>`.”

Bu belge proje bağlamıdır; tüm production kararlarının yerine geçmez. Kod değişikliği öncesinde ilgili branch, migration ve gereksinim durumu ayrıca kontrol edilmelidir.
