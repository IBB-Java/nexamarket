# Katalog Görsel Akışı

## Yükleme

`POST /api/v1/products/{productId}/images` uç noktası `multipart/form-data` içinde `file` alanı bekler.
JPEG ve PNG dosyaları desteklenir; varsayılan üst sınır 5 MB'dir.

İstek orijinal görseli nesne depolamaya kaydeder ve `202 Accepted` ile
`PENDING_THUMBNAIL` durumunu döndürür. Thumbnail, veritabanı işlemi commit
edildikten sonra `thumbnailTaskExecutor` üzerinde üretilir.

Durum akışı:

1. `PENDING_THUMBNAIL`
2. `READY` veya `FAILED`

Güncel durum `GET /api/v1/products/{productId}/images/{imageId}` ile sorgulanır.
Orijinal ve thumbnail içerikleri yanıttaki URL'lerden indirilebilir.

## MinIO ayarları

Uygulama varsayılan olarak S3 uyumlu MinIO depolamasını kullanır:

| Ortam değişkeni | Varsayılan değer |
| --- | --- |
| `MINIO_ENDPOINT` | `http://localhost:9000` |
| `MINIO_ACCESS_KEY` | `minioadmin` |
| `MINIO_SECRET_KEY` | `minioadmin` |
| `MINIO_BUCKET` | `nexamarket-products` |
| `CATALOG_IMAGE_MAX_BYTES` | `5242880` |

Test profilinde dış servise ihtiyaç duymayan bellek içi nesne depolama kullanılır.
