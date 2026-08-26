# NexaMarket

## Ödeme akışını yerelde denemek

Ödeme dalındaki sağlayıcı bir mock'tur; gerçek kart veya para hareketi yapmaz.
Önce sipariş oluşturulduktan sonra müşteri cüzdanına yalnızca geliştirme ortamında
yükleme yapılabilir:

```bash
curl -X POST http://localhost:8080/internal/wallets/<customerId>/credits \
  -H 'Content-Type: application/json' -d '{"amount": "20.00"}'
```

Ardından toplamı cüzdan ve kart arasında bölerek ödeme başlatılır. Aynı
`idempotencyKey` ile yapılan tekrarlar yeni tahsilat oluşturmaz:

```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"<orderId>","idempotencyKey":"demo-odeme-1","walletAmount":"20.00","cardAmount":"30.00"}'
```

Yanıttaki `providerPaymentId` için mock sağlayıcıya sonuç verilir. `duplicateDeliveries`
değeri, webhook'un aynı olay kimliğiyle kaç kez tekrar gönderileceğini gösterir:

```bash
curl -X POST http://localhost:8080/mock-payment-provider/payments/<providerPaymentId>/outcomes \
  -H 'Content-Type: application/json' \
  -d '{"status":"SUCCEEDED","callbackDelaySeconds":2,"duplicateDeliveries":2}'
```

Çok satıcılı e-ticaret platformu. İş gereksinimleri ve teslim durumu için
[gereksinim takip dosyasına](docs/requirements-traceability.md) bakın.

## Yerel olarak çalıştırma

Önce Docker Desktop'ı açın, sonra proje klasöründe PostgreSQL'i başlatın:

```bash
docker compose up -d
```

Ardından uygulamayı çalıştırın:

```bash
./mvnw spring-boot:run
```

Testleri çalıştırmak için Docker'a gerek yoktur; testler H2 ile izole olarak
çalışır:

```bash
./mvnw clean test
```

> `POST /api/v1/cart/items` uç noktası hazırdır. Stok rezervasyonu Catalog
> Service tarafından sağlanacağından, uç noktayı uçtan uca denemeden önce bu
> servis de çalışıyor olmalıdır.
