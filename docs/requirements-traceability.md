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
4. Checkout anında Order Service bir ana sipariş ve satıcı bazlı alt siparişleri
   `PAYMENT_PENDING` durumunda oluşturacaktır. Tek ödeme daha sonra bu ana
   siparişi onaylar; böylece ödeme tek işlemken satıcı akışları bağımsız kalır.
5. `sourceCartId`, Cart Service'ten Order Service'e gönderilen checkout isteğinin
   idempotency anahtarıdır. Ağ nedeniyle aynı istek yinelenirse mevcut sipariş
   döndürülür; ikinci bir ana veya alt sipariş oluşturulmaz.

## Fonksiyonel gereksinimler

| Gereksinim | Durum | Not |
| --- | --- | --- |
| FR-AUTH-01…04 | Planlandı | Kayıt/giriş, JWT, rol yetkisi ve giriş kilidi. |
| FR-CAT-01…04 | Planlandı | Ürün/varyant/kategori, görsel, arama ve indeksleme. |
| FR-CART-01 | Devam ediyor | Sepete ekleme API'si ve Catalog Service REST rezervasyon sınırı hazır; gerçek stok servisi ve yarış testi eksik. |
| FR-CART-02 | Devam ediyor | Süresi dolmuş rezervasyonları Catalog Service üzerinden serbest bırakan zamanlayıcı hazır; gerçek Catalog Service ve uçtan uca doğrulama eksik. |
| FR-CART-03 | Devam ediyor | Checkout, kalemleri satıcıya göre gruplayıp gerçek Order Service'e tek istekte iletiyor; ödeme tamamlandığında sipariş onayı eksik. |
| FR-ORD-01 | Devam ediyor | Ana sipariş, alt sipariş ve kalemler idempotent checkout ile oluşturuluyor; merkezi durum makinesi geçersiz geçişleri `409 Conflict` ile reddediyor. |
| FR-ORD-02 | Devam ediyor | Ödeme bekleyen siparişleri zamanlayıcıyla iptal edip stok rezervasyonlarını serbest bırakan akış hazır; gerçek Catalog Service ile uçtan uca doğrulama eksik. |
| FR-ORD-03 | Devam ediyor | İade talebi gerekçesiyle ayrı kaydediliyor; sadece satıcı/admin kararını temsil eden resolver onay/red verebiliyor. Kimlik/rol doğrulaması Auth modülüne bağlı. |
| FR-ORD-04 | Devam ediyor | Alt sipariş durum değişiminde aynı işlemde kalıcı outbox olayı yazılır; RabbitMQ'ya aktarma kuyruk kapalıyken sipariş yolunu bozmaz ve yeniden denenir. Stok/admin tüketicileri sonraki modüllerdedir. |
| FR-PAY-01 | Devam ediyor | Yerel mock sağlayıcı kart ödemesini `PENDING` başlatır; kontrollü başarı/ret, gecikmeli callback ve aynı callback'in tekrar teslimini simüle eder. |
| FR-PAY-02 | Devam ediyor | Sağlayıcının `providerEventId` değeri tekil kaydedilir; aynı webhook yeniden gelirse sipariş ya da cüzdan ikinci kez değişmez. |
| FR-PAY-03 | Devam ediyor | Bekleyen kart işlemleri zamanlayıcıyla mock sağlayıcıdan sorgulanır ve `PENDING` sonuçları yeniden denemek üzere planlanır. Ağ hataları için dayanıklılık/observability katmanı sonraki teknik iş paketindedir. |
| FR-PAY-04 | Devam ediyor | İstek cüzdan ve kart tutarlarını sipariş toplamına eşit olacak şekilde böler; kart ret olursa ayrılan cüzdan tutarı iade edilir. Cüzdan bakiye yükleme yalnızca yerel geliştirme amaçlı iç API'dir. |
| FR-PROMO-01…02 | Planlandı | Veri odaklı indirim kuralları ve sadakat defteri. |
| FR-NOTIF-01 | Devam ediyor | RabbitMQ tüketicisi sipariş olayı için e-posta, SMS ve uygulama içi mesaj üretir. Kanal göndericileri arayüz tabanlıdır; e-posta/SMS şu an güvenli mock'tur. |
| FR-NOTIF-02 | Devam ediyor | Mesajlar kritik akışın dışında teslim edilir; geçici hata sonrası planlı retry, üst sınıra ulaşınca `FAILED` kaydı vardır. Olay ve kanal bazlı tekilleştirme tekrar teslimleri etkisiz kılar. |
| FR-REP-01…02 | Planlandı | Arka plan PDF/XLSX raporları ve günlük yönetici özeti. |

## Teknik ve kabul gereksinimleri

| Konu | Durum | Not |
| --- | --- | --- |
| Java 17+ ve Spring Boot 3.x | Tamamlandı | Java 21 hedefi ve Spring Boot 3.5.0. |
| JPA/Hibernate, PostgreSQL, Flyway | Devam ediyor | Şema migration'ları ve Docker PostgreSQL tanımı mevcut; gerçek PostgreSQL doğrulaması Docker Desktop açıldığında yapılacak. |
| RabbitMQ/Kafka, Redis, Redisson | Devam ediyor | RabbitMQ Compose ve Spring AMQP ile sipariş-bildirim olayı hazır; Redis/Redisson sonraki stok iş paketinde. |
| Elasticsearch/OpenSearch | Planlandı | Ürün arama ve indeks güncellemesi. |
| MinIO, PDF, Excel | Planlandı | Görseller ile rapor/fatura çıktıları. |
| WebSocket, Resilience4j, OpenAPI | Planlandı | Canlı admin akışı, dış servis toleransı, Swagger. |
| JSON log, correlation ID, rate limit, audit | Planlandı | Gözlemlenebilirlik, güvenlik ve denetim. |
| Testcontainers ve kabul senaryoları | Planlandı | Overselling, webhook idempotency, durum geçişi ve %70 kritik modül hedefi. |
| Docker Compose tek komut | Devam ediyor | PostgreSQL tanımlı; diğer servisler aşamalı olarak compose dosyasına eklenecek. |
