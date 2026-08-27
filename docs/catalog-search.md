# Katalog Arama API'si

## Uç nokta

`GET /api/v1/products/search`

Desteklenen sorgu parametreleri:

| Parametre | Açıklama |
| --- | --- |
| `q` | Ürün adı, açıklaması ve kategori adlarında metin araması |
| `categoryId` | Kategori kimliği |
| `minPrice` | Ürünün herhangi bir varyantıyla kesişen minimum fiyat |
| `maxPrice` | Ürünün herhangi bir varyantıyla kesişen maksimum fiyat |
| `minSellerRating` | Minimum satıcı puanı (0-5) |
| `page` | Sıfır tabanlı sayfa numarası |
| `size` | Sayfa büyüklüğü (1-100) |

Yalnızca `ACTIVE` durumundaki ürünler sonuçlara dahil edilir. Fiyat filtresi,
ürünün en düşük ve en yüksek varyant fiyatlarıyla arama aralığının kesişmesine
göre uygulanır.

## İndeksleme

Ürün oluşturma veya güncelleme işlemi tamamlandıktan sonra
`ProductCatalogChangedEvent` yayımlanır. Elasticsearch indekslemesi veritabanı
commit'inden sonra `catalogIndexTaskExecutor` üzerinde yürütülür; indeksin geçici
olarak erişilemez olması katalog işlemini geri almaz.

`PATCH /api/v1/products/{productId}` uç noktası satıcının ürün açıklamasını,
taban fiyatını ve mevcut varyantların fiyat/stok miktarını kısmi olarak
günceller. İstek, ürünü oluşturan satıcının `X-Seller-Id` başlığını taşımalıdır.
Örnek gövde:

```json
{
  "description": "Güncellenmiş açıklama",
  "basePrice": 199.99,
  "variants": [
    {"id": 17, "price": 219.99, "stockQuantity": 8}
  ]
}
```

Arama belgesi varyant fiyat aralığının yanında `totalStock` ve `inStock`
alanlarını da içerir. Böylece fiyat ve stok değişiklikleri bir sonraki indeks
sürümünde birlikte görünür.

### Nihai tutarlılık sınırı

Sağlıklı Elasticsearch bağlantısı ve normal görev kuyruğu yükü altında ürün
değişikliğinin arama sonuçlarına **en geç 5 saniye** içinde yansıması hedeflenir.
İndeksleme geçici hata aldığında aynı görev 250 ms arayla en fazla 5 kez
denenir. Süre veya deneme sınırı aşılırsa katalog işlemi geri alınmaz; ihlal hata
seviyesinde loglanır. Değerler aşağıdaki ortam değişkenleriyle ayarlanabilir:

| Değişken | Varsayılan | Açıklama |
| --- | --- | --- |
| `CATALOG_INDEX_MAX_DELAY` | `5s` | Commit ile başarılı indeksleme arasındaki üst sınır hedefi |
| `CATALOG_INDEX_RETRY_DELAY` | `250ms` | Denemeler arası bekleme |
| `CATALOG_INDEX_MAX_ATTEMPTS` | `5` | En fazla indeksleme denemesi |

Üretim varsayılanı Elasticsearch'tür. Bağlantı adresi `ELASTICSEARCH_URIS`
ortam değişkeniyle, varsayılan `http://localhost:9200` değeri değiştirilerek
yapılandırılabilir. Test profilinde aynı arama sözleşmesini uygulayan bellek içi
indeks kullanılır.

Satıcı puanı alanı indeks ve filtre sözleşmesinde hazırdır. Gerçek puan değeri,
`users` modülüyle entegrasyon tamamlandığında katalog belgesine beslenecektir.
