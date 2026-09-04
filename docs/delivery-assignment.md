# Kurye atama, rol güvenliği ve ekranlar — teknik teslim raporu

Bu belge, manuel kurye atama ve rol bazlı ekran çalışmasının son teknik
raporudur. `NexaMarket_Analist_Belgesi.docx` içindeki ana proje gereksinimleri
ayrıca `requirements-traceability.md` içinde izlenir.

## 1. Sorunun kaynağı

Ödeme başarılı olduğunda `PaymentApplicationService` ve
`PaymentWebhookService`, `AutomaticCourierAssignmentService.assignAfterPayment`
metodunu çağırıyordu. Bu servis aktif kuryelerden birini seçip
`SubOrder.courierId` alanını doğrudan doldurduğu için ADMIN hiçbir işlem
yapmadan kurye ekranında “atanmış” sipariş oluşuyordu.

## 2. Düzeltme

Ödeme ile kurye ataması arasındaki otomatik çağrı kaldırıldı. Ödeme artık
siparişi yalnızca `PAID` yapar. Kurye atamasının tek giriş noktası ADMIN
yetkili `POST /api/v1/admin/deliveries/assign` endpoint'idir. Kurye listesi de
`SubOrder` tablosunu değil, yalnızca oturumdaki `courierId` değerine ait
`DeliveryAssignment` kayıtlarını sorgular.

## 3. DeliveryAssignment modeli

Her atama ayrı ve silinmeyen bir satırdır:

- `id`, `subOrderId`, `courierId`, `status`
- `assignedAt`, `acceptedAt`, `rejectedAt`, `pickedUpAt`
- `deliveryStartedAt`, `deliveredAt`, `failedAt`
- `rejectionReason`, `failureReasonCode`, `failureDescription`
- optimistic concurrency için `version`
- tek aktif kaydı temsil eden `activeAssignment`

Aktif atamada `active_assignment=TRUE`, terminal kayıtta `NULL` olur. Veritabanı
`(sub_order_id, active_assignment)` unique index'i aynı alt sipariş için iki
aktif atamayı yarış koşulunda bile engeller; terminal satırlardaki `NULL`
değerler sınırsız geçmiş saklanmasına izin verir. `SubOrder.courierId`, güncel
atamayı hızlı göstermek için denormalize işaretçidir; geçmişin kaynağı değildir.

## 4. Ayrı teslimat durum makinesi

```text
ASSIGNED ──→ ACCEPTED ──→ PICKED_UP ──→ IN_TRANSIT ──→ DELIVERED
    │             │             │              │
    └─→ REJECTED  └─────────────┴──────────────┴─→ DELIVERY_FAILED
```

`REJECTED`, `DELIVERED` ve `DELIVERY_FAILED` terminaldir. Tanımlı olmayan her
geçiş `409 Conflict` döndürür. Bu makine `OrderStateMachine`'den ayrıdır.

## 5. Kabul akışı

Kurye yalnızca kendine ait `ASSIGNED` kaydı `/accept` ile kabul eder. Durum
`ACCEPTED`, `acceptedAt` güncel zaman olur. Başka kurye `403`, ikinci kabul
`409` alır.

## 6. Ret akışı

Kurye `ASSIGNED` kaydı `/reject` ile ve zorunlu, en fazla 500 karakterlik
gerekçeyle reddeder. `rejectedAt` ve gerekçe kalıcıdır; aktif işaretçi ile
`SubOrder.courierId` temizlenir. Satır silinmez.

## 7. Başarısız teslimat

`ACCEPTED`, `PICKED_UP` veya `IN_TRANSIT` durumunda `/fail` çağrılabilir.
`VEHICLE_BREAKDOWN`, `ACCIDENT`, `HEALTH_ISSUE`, `CUSTOMER_UNREACHABLE`,
`ADDRESS_PROBLEM`, `PACKAGE_DAMAGED`, `SECURITY_ISSUE`, `OTHER` enum
değerlerinden biri ve açıklama zorunludur. Alt sipariş iptal edilmez; yönetici
destek süreci başlatabilir veya yeniden atayabilir.

## 8. Yeniden atama

Ret veya başarısızlık terminal duruma geçtiği için aktif işaretçi temizlenir.
ADMIN aynı `SubOrder` için başka bir kurye seçtiğinde yeni UUID'li bir
`DeliveryAssignment` oluşturulur. Önceki kurye, gerekçe ve zamanlar değişmeden
geçmişte kalır. Aktif atama varken değiştirme/ikinci atama yapılmaz.

## 9–12. Rol ekranları

| Rol | Gördüğü çalışma alanı | Görmediği alanlar |
| --- | --- | --- |
| CUSTOMER | Mağaza, ürün detayı, favori, sepet, checkout, kendi siparişleri, iadeleri ve profil | Satıcı, kurye, admin yönetimi |
| SELLER | Ayrı satıcı dashboard'u; kendi ürünleri, fiyat/stok/görsel/yayın yönetimi, kendi `SubOrder` kayıtları, iade yönetimi, profil | Marketplace satın alma menüleri, başka satıcı siparişleri, kurye/admin işlemleri |
| COURIER | Sade kurye dashboard'u; yeni atanan, devam eden ve geçmiş teslimatlar; kabul/red/pickup/start/deliver/fail, profil | Katalog, sepet, checkout, mağaza açma, finansal/satıcı alanları, başka kurye işleri |
| ADMIN | Ayrı yönetim dashboard'u; tüm kullanıcılar, roller/durumlar, tüm alt siparişler, kurye atama, teslimat ve red/başarısızlık geçmişi, iadeler | Normal müşteri alışveriş ekranına bağımlı değildir |

## 13. Route protection

Tarayıcı guard'ı `/customer/**`, `/seller/**`, `/courier/**` ve `/admin/**`
yollarını oturum rolüyle karşılaştırır. Yetkisiz veya oturumsuz doğrudan URL
girişi uygun role ya da `/` adresine yönlenir. Spring MVC bu adresleri
`index.html` dosyasına forward ettiği için sayfa yenileme 404 üretmez. Bu guard
yalnızca UX katmanıdır; asıl veri güvenliği backend'dedir.

## 14. Backend authorization

Spring Security URL kuralları ve controller `@PreAuthorize` kontrolleri birlikte
çalışır. ADMIN, COURIER, SELLER ve CUSTOMER endpoint'leri ayrıdır. Kaynak
sahipliği application service içinde tekrar doğrulanır. COURIER katalog arama ve
genel sipariş DTO endpoint'lerine erişemez; yalnız gizlilik kapsamlı
`CourierDeliveryResponse` alır.

## 15. Migration'lar

- `V19__add_user_soft_delete.sql`: `DELETED`, `deleted_at` ve tarihçe koruyan
  soft delete desteği.
- `V20__create_delivery_assignments.sql`: atama tablosu, durum ve aktif-kayıt
  check constraint'leri, concurrency unique index'i, kurye+durum, alt sipariş,
  atanma zamanı ve durum index'leri; teslimat bildirim outbox tablosu.

Eski migration dosyaları değiştirilmedi.

## 16. Yeni teslimat endpoint'leri

| Rol | Yöntem ve yol | Amaç |
| --- | --- | --- |
| ADMIN | `GET /api/v1/admin/deliveries` | Tüm aktif/geçmiş atamalar |
| ADMIN | `POST /api/v1/admin/deliveries/assign` | Aktif kuryeye manuel atama |
| COURIER | `GET /api/v1/courier/deliveries` | Yalnız kendi atamaları |
| COURIER | `PATCH .../{assignmentId}/accept` | Atamayı kabul |
| COURIER | `PATCH .../{assignmentId}/reject` | Gerekçeli ret |
| COURIER | `PATCH .../{assignmentId}/pickup` | Paketi teslim alma |
| COURIER | `PATCH .../{assignmentId}/start` | Dağıtıma çıkma |
| COURIER | `PATCH .../{assignmentId}/deliver` | Teslim tamamlama |
| COURIER | `PATCH .../{assignmentId}/fail` | Kodlu/gerekçeli başarısızlık |

Pickup, mevcut `OrderStateMachine` üzerinden `SubOrder` durumunu `SHIPPED`;
deliver ise `DELIVERED` yapar. Doğrudan durum bypass edilmez.

## 17–18. Testler ve sonuç

`RoleBasedOrderAccessApiIntegrationTest` istenen 25 güvenlik/akış kabul
senaryosunu tek gerçek HTTP yaşam döngüsünde doğrular. Buna ek olarak
`DeliveryAssignmentStateMachineTest`, `OrderStatusServiceTest`,
`RoleBasedOrderQueryServiceTest`, `DeliveryStatusNotificationConsumerTest` ve
`FrontendRoleWorkspaceTest` birim/regresyon kapsamı sağlar.

Son tam doğrulama sonucu: `./mvnw clean verify` ile **110 test, 0 hata,
0 başarısız, 0 atlandı**; JaCoCo'nun bütün genel/kritik kapsam eşikleri geçti.
Testcontainers, V1–V20 migration'larını PostgreSQL 16.15 üzerinde başarıyla
uyguladı.

## 19. Çalıştırma doğrulaması

Son çalışma sonunda Docker Compose, gerçek PostgreSQL migration'ı, sağlık
endpoint'i ve `./mvnw spring-boot:run` ayrıca doğrulanır. Güncel sonuç yukarıdaki
test sonucu ile birlikte teslim notunda yazılır.

## 20. Değiştirilen frontend ekranları

- Giriş sonrası role göre otomatik yönlendirme
- SELLER, COURIER ve ADMIN için birbirinden ayrı pastel dashboard/sidebar
- COURIER için durum filtreleri ve tam teslimat yaşam döngüsü işlemleri
- ADMIN için aktif kurye atama ve silinmeyen DeliveryAssignment geçmişi
- CUSTOMER mağazasında yalnız alışveriş/hesap deneyimi
- Desteklenmeyen CUSTOMER→SELLER dönüşümü nedeniyle giriş yapmış kullanıcıdan
  sahte “Mağazanı Aç” aksiyonunun kaldırılması

## 21. Açık noktalar

Sipariş modelinde teslimat adresi, alıcı görünen adı, telefon ve teslimat notu
alanları bulunmadığı için `CourierDeliveryResponse` bunları uydurarak göstermez.
DTO şu anda işin yapılması için mevcut veriden gereken alt sipariş numarası,
paket sayısı ve durum/zaman bilgilerini içerir. Gerçek adres/iletişim gösterimi
için checkout adres modelinin ayrıca tasarlanması ve kişisel veri maskeleme/
saklama politikası gerekir. Bu eksik, atama ve teslimat yaşam döngüsünü
engellemez.

## Bildirim akışı

Her atama ve durum değişikliği aynı transaction içinde
`delivery_notification_outbox_events` tablosuna yazılır. Relay RabbitMQ'daki
`delivery.status.changed` routing key'ine yayınlar; consumer kanal başına
deduplication key ile e-posta, SMS ve uygulama içi mesaj üretir. Atama olayı
kuryeye, sonraki ilerleme olayları müşteriye yönelir. RabbitMQ geçici olarak
kapalıysa retry yapılır ve iş transaction'ı kaybolmaz.
