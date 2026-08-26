# NexaMarket

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
