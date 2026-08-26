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

Ürün oluşturma işlemi tamamlandıktan sonra `ProductCatalogChangedEvent`
yayımlanır. Elasticsearch indekslemesi veritabanı commit'inden sonra
`catalogIndexTaskExecutor` üzerinde yürütülür; indeksin geçici olarak erişilemez
olması ürün oluşturma işlemini geri almaz.

Üretim varsayılanı Elasticsearch'tür. Bağlantı adresi `ELASTICSEARCH_URIS`
ortam değişkeniyle, varsayılan `http://localhost:9200` değeri değiştirilerek
yapılandırılabilir. Test profilinde aynı arama sözleşmesini uygulayan bellek içi
indeks kullanılır.

Satıcı puanı alanı indeks ve filtre sözleşmesinde hazırdır. Gerçek puan değeri,
`users` modülüyle entegrasyon tamamlandığında katalog belgesine beslenecektir.
