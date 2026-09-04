# NexaMarket

NexaMarket, çok satıcılı e-ticaret senaryosunu Spring Boot ile uygulayan bir
ders projesidir. Kayıt/giriş, katalog, stok rezervasyonu, sepet, satıcı bazlı
sipariş, ödeme, iade, bildirim, kampanya, sadakat puanı ve raporlama modülleri
tek uygulamada entegredir.

## İlk kez çalıştırma

1. Docker Desktop’ı açın.
2. Proje klasöründe uygulama dahil bütün sistemi başlatın:

   ```bash
   docker compose up -d
   ```

3. Uygulamanın hazır olmasını bekleyin ve durumunu kontrol edin:

   ```bash
   docker compose ps
   ```

4. Tarayıcıdan mağaza arayüzünü açın: <http://localhost:8080>
5. API dokümantasyonu ve teknik test ekranı için Swagger'ı açın:
   <http://localhost:8080/swagger-ui.html>

Compose; NexaMarket uygulaması, PostgreSQL, RabbitMQ, Redis, Elasticsearch ve
MinIO’yu başlatır. Yalnız altyapıyı Docker'da çalıştırıp uygulamayı IDE/Maven ile
başlatmak isteyenler `docker compose up -d postgres rabbitmq redis elasticsearch
minio` ve ardından `./mvnw spring-boot:run` komutlarını kullanabilir.
MinIO konsolu `http://localhost:9001`, RabbitMQ yönetim paneli ise
`http://localhost:15672` adresindedir. Her ikisinin varsayılan kullanıcı adı ve
parolası `nexamarket` / `nexamarket` (MinIO için `minioadmin` / `minioadmin`) olur.
Kayıt doğrulama e-postaları varsayılan yerel geliştirmede Mailpit'e gönderilir;
gelen kutusunu `http://localhost:8025` adresinden açıp altı haneli kodu mağaza
arayüzündeki doğrulama alanına girebilirsiniz.

### Gerçek Gmail doğrulama kodu e-postası

Gerçek kullanıcılara e-posta göndermek için `.env.example` dosyasını `.env` olarak
kopyalayın ve en alttaki `MAIL_*` değerlerini gönderici Gmail hesabınıza göre
doldurun. Uygulama bu yerel `.env` dosyasını başlangıçta otomatik okur. Gmail'in normal hesap parolası kullanılmaz: gönderici hesapta iki adımlı
doğrulama açıldıktan sonra oluşturulan 16 karakterlik **uygulama şifresi**
`MAIL_PASSWORD` olur. `.env` dosyası `.gitignore` kapsamındadır; şifre GitHub'a
gönderilmez.

Ayarları değiştirdikten sonra uygulamayı yeniden başlatın. Docker ile çalışıyorsanız
`docker compose up -d --force-recreate app`; IntelliJ/Maven ile çalışıyorsanız
çalışma yapılandırmasına aynı `MAIL_*` ortam değişkenlerini ekleyip uygulamayı
yeniden başlatın. Doğrulama kodu kayıt olan kişinin gerçek e-posta gelen
kutusuna gider; telefonda e-posta uygulaması varsa bildirim olarak görünür.
Kod aynı tarayıcıdaki mağaza ekranına girildiği için, yerel `localhost` adresine
dışarıdan bağlantı açılması gerekmez. Kullanıcı kodu girmeden de giriş yapabilir;
uygulama her yeni oturumda doğrulama penceresini yeniden gösterir.

## Mağaza arayüzü

Ana sayfa artık yalnızca Swagger ekranı değildir. `http://localhost:8080` adresinde
NexaMarket Store bulunur. Buradan:

- demo mağazayı tek tıkla gerçek katalog API'si üzerinden doldurabilir,
- kayıt sırasında alıcı, satıcı veya kurye hesabı seçebilir; giriş yapabilir ve
  görünür hesap menüsünden güvenle çıkış yapabilir (ADMIN normal kayıttan seçilemez),
- ürünleri arayabilir, kategoriye göre filtreleyebilir, sıralayabilir, detaylarını
  inceleyebilir ve favorilerine kaydedebilir,
- üst arama alanı, kategori menüsü ve kampanya kartlarıyla güncel mağaza
  deneyimi üzerinden ürünlere hızlıca ulaşabilir,
- ürünleri sepete ekleyip, silme onayıyla sunucu tarafında rezerve edilen sepetini
  yönetebilir,
- mock kart ödemesiyle siparişi tamamlayabilir,
- “Hesabım” ekranından sipariş adımlarını takip edebilir, uygun alt sipariş için
  iade talebi açabilir ve talebin inceleniyor/onaylandı/reddedildi durumunu görebilir,
- satıcı veya yönetici hesabıyla “İade yönetimi” alanından talepleri onaylayabilir
  ya da reddedebilir,
- “Satıcı alanı” ile otomatik stok kodlu yeni ürün ve varyant ekleyebilir, ürünü
  hemen vitrinde yayınlayabilir veya taslak olarak saklayabilir; mevcut ürünleri
  yayınlayabilir, satıştan kaldırabilir, fiyat/stok/görselini güncelleyebilir ve
  yalnızca kendi alt siparişlerini yönetebilirsiniz,
- girişten sonra `SELLER`, `COURIER` ve `ADMIN` rollerine özel, birbirinden
  ayrılmış dashboard ve sidebar kullanabilirsiniz,
- kurye hesabıyla yalnızca yönetici tarafından kendisine atanmış teslimatları
  kabul/reddedebilir; teslim alma, dağıtıma çıkma, teslim ve gerekçeli
  başarısızlık adımlarını ayrı teslimat durum makinesiyle ilerletebilirsiniz,
- yönetici hesabıyla bütün kullanıcıları, siparişleri, rolleri, hesap
  durumlarını, manuel kurye atamalarını ve silinmeyen teslimat geçmişini
  yönetebilirsiniz.

Katalogda yalnızca `ACTIVE` durumundaki ürünler müşteriye gösterilir. Uygulama
açılırken katalog verisi arama indeksine yeniden yazılır; bu nedenle yerel
veritabanındaki ürünler indeks yeniden oluşturulsa bile mağazada kaybolmaz.

Bu ekran, sunumda cart → order → payment → notification akışını Swagger JSON'ları
yerine görünür bir kullanıcı deneyimi üzerinden göstermek için tasarlanmıştır.

## Test ve kalite kontrolü

```bash
./mvnw clean verify
```

Bu komut birim/entegrasyon testlerini, JaCoCo raporunu ve en az %70 satır kapsamı
kontrolünü çalıştırır. Kritik stok, sepet, sipariş durum makinesi, ödeme ve
kampanya sınıflarının her biri için de ayrı ayrı en az %70 kapsam zorunludur.
Docker aktifse ayrıca PostgreSQL 16 üzerinde Flyway migration smoke testi
çalışır; Docker yoksa bu tek test otomatik atlanır. Katalog aramasının 95.
yüzdelik yanıt süresinin 300 ms altında kaldığı kabul testi de bu komuta dahildir.

## Ana kullanıcı akışı

1. Kullanıcı rolünü seçerek kayıt olur; girişte access ve refresh JWT alır.
2. Varyantı sepete ekler; stok hemen rezerve edilir.
3. Checkout, kalemleri satıcılara göre alt siparişlere böler.
4. Cüzdan/kart bölüşümlü ödeme tamamlanır; rezervasyon `CONFIRMED` olur.
5. Satıcı siparişi hazırlamaya alır; ADMIN aktif kuryeyi manuel atar. Kurye
   atamayı kabul edip `PICKED_UP → IN_TRANSIT → DELIVERED` adımlarını ilerletir.
   Ret veya başarısızlıkta tarihçe korunur ve ADMIN yeni atama yapabilir.
   Teslimat olayları da outbox üzerinden RabbitMQ bildirimlerine dönüşür.
6. Teslim edilen sipariş puan kazandırır; onaylanan iade ters puan kaydı açar.

Detaylı DOCX gereksinim eşlemesi için
[gereksinim takip kaydına](docs/requirements-traceability.md), modül ve sunum
notları için [handoff belgesine](docs/nexamarket-handoff.md), dört mantıksal
servisin iletişim kuralları için [mimari belgesine](docs/architecture.md), kurye
atama ve rol ekranlarının teknik açıklaması için
[teslimat raporuna](docs/delivery-assignment.md) bakın.
