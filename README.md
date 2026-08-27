# NexaMarket

NexaMarket, çok satıcılı e-ticaret senaryosunu Spring Boot ile uygulayan bir
ders projesidir. Kayıt/giriş, katalog, stok rezervasyonu, sepet, satıcı bazlı
sipariş, ödeme, iade, bildirim, kampanya, sadakat puanı ve raporlama modülleri
tek uygulamada entegredir.

## İlk kez çalıştırma

1. Docker Desktop’ı açın.
2. Proje klasöründe bütün bağımlılıkları başlatın:

   ```bash
   docker compose up -d
   ```

3. Uygulamayı başlatın:

   ```bash
   ./mvnw spring-boot:run
   ```

4. Tarayıcıdan API dokümantasyonunu açın: <http://localhost:8080/swagger-ui.html>

Compose; PostgreSQL, RabbitMQ, Redis, Elasticsearch ve MinIO’yu başlatır.
MinIO konsolu `http://localhost:9001`, RabbitMQ yönetim paneli ise
`http://localhost:15672` adresindedir. Her ikisinin varsayılan kullanıcı adı ve
parolası `nexamarket` / `nexamarket` (MinIO için `minioadmin` / `minioadmin`) olur.

## Test ve kalite kontrolü

```bash
./mvnw clean verify
```

Bu komut birim/entegrasyon testlerini, JaCoCo raporunu ve en az %70 satır kapsamı
kontrolünü çalıştırır. Docker aktifse ayrıca PostgreSQL 16 üzerinde Flyway migration
smoke testi çalışır; Docker yoksa bu tek test otomatik atlanır.

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
notları için [handoff belgesine](docs/nexamarket-handoff.md) bakın.
