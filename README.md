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
gelen kutusunu `http://localhost:8025` adresinden açıp doğrulama bağlantısına
tıklayabilirsiniz.

### Gerçek Gmail doğrulama e-postası

Gerçek kullanıcılara e-posta göndermek için `.env.example` dosyasını `.env` olarak
kopyalayın ve en alttaki `MAIL_*` değerlerini gönderici Gmail hesabınıza göre
doldurun. Gmail'in normal hesap parolası kullanılmaz: gönderici hesapta iki adımlı
doğrulama açıldıktan sonra oluşturulan 16 karakterlik **uygulama şifresi**
`MAIL_PASSWORD` olur. `.env` dosyası `.gitignore` kapsamındadır; şifre GitHub'a
gönderilmez.

Ayarları değiştirdikten sonra uygulamayı yeniden başlatın. Docker ile çalışıyorsanız
`docker compose up -d --force-recreate app`; IntelliJ/Maven ile çalışıyorsanız
çalışma yapılandırmasına aynı `MAIL_*` ortam değişkenlerini ekleyip uygulamayı
yeniden başlatın. Doğrulama e-postası kayıt olan kişinin gerçek e-posta gelen
kutusuna gider; telefonda e-posta uygulaması varsa bildirim olarak görünür.

## Mağaza arayüzü

Ana sayfa artık yalnızca Swagger ekranı değildir. `http://localhost:8080` adresinde
NexaMarket Store bulunur. Buradan:

- demo mağazayı tek tıkla gerçek katalog API'si üzerinden doldurabilir,
- müşteri hesabı oluşturup giriş yapabilir ve görünür hesap menüsünden güvenle
  çıkış yapabilir,
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
  yayınlayabilir, satıştan kaldırabilir ve geçmiş siparişleri bozmadan silebilirsiniz.

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

1. Müşteri kayıt olur ve JWT alır.
2. Varyantı sepete ekler; stok hemen rezerve edilir.
3. Checkout, kalemleri satıcılara göre alt siparişlere böler.
4. Cüzdan/kart bölüşümlü ödeme tamamlanır; rezervasyon `CONFIRMED` olur.
5. Satıcı kargo durumunu günceller; outbox üzerinden e-posta, SMS, uygulama içi
   bildirim ve yönetici WebSocket olayı üretilir.
6. Teslim edilen sipariş puan kazandırır; onaylanan iade ters puan kaydı açar.

Detaylı DOCX gereksinim eşlemesi için
[gereksinim takip kaydına](docs/requirements-traceability.md), modül ve sunum
notları için [handoff belgesine](docs/nexamarket-handoff.md), dört mantıksal
servisin iletişim kuralları için [mimari belgesine](docs/architecture.md) bakın.
