# Stok ve Rezervasyon Modülü

Bu modül, `ProductVariant.stockQuantity` alanını **kullanılabilir stok** olarak yönetir. Her varyantın stoğu bağımsızdır; ürün seviyesinde toplu stok tutulmaz.

## Rezervasyon yaşam döngüsü

1. Yetkili kullanıcı `POST /api/v1/stocks/reservations` ile varyant ve miktar gönderir.
2. Veritabanında `stock_quantity >= quantity` koşullu güncellemesi çalışır. Koşul sağlanırsa stok atomik olarak azaltılır ve `ACTIVE` rezervasyon oluşturulur.
3. Rezervasyon varsayılan olarak 10 dakika geçerlidir (`stock.reservation.duration`).
4. Ödeme tamamlandığında `POST /api/v1/stocks/reservations/{reservationCode}/confirm` çağrılır ve durum `CONFIRMED` olur; stok geri verilmez.
5. Kullanıcı `DELETE /api/v1/stocks/reservations/{reservationCode}` ile vazgeçerse durum `RELEASED` olur ve stok geri eklenir.
6. Zamanlanmış görev, süresi geçen etkin rezervasyonları `EXPIRED` durumuna geçirip stoklarını geri ekler.

Rezervasyon durumları için kilitli okuma kullanılır; bu sayede onay, iptal ve zaman aşımı aynı rezervasyonu iki kez stoklara geri ekleyemez.

## Eşzamanlılık garantisi

Stok azaltma, uygulama belleğinde kontrol edip sonradan yazmak yerine tek SQL ifadesiyle yapılır:

```sql
UPDATE product_variants
SET stock_quantity = stock_quantity - :quantity
WHERE id = :variantId AND stock_quantity >= :quantity
```

Bu işlem ortak veritabanına bağlı tüm uygulama örnekleri için atomiktir; güncelleme sonucu `0` ise istek yetersiz stokla reddedilir. Böylece negatif stok ve overselling oluşmaz. Farklı, birbirinden bağımsız stok depoları kullanılacaksa bu garanti ayrıca dağıtık kilit veya tek yazıcı mimarisiyle genişletilmelidir.

## Uç noktalar

| Metot | Uç nokta | Yetki | Açıklama |
| --- | --- | --- | --- |
| GET | `/api/v1/stocks/variants/{variantId}` | Herkes | Kullanılabilir stok düzeyini görüntüler. |
| PATCH | `/api/v1/stocks/variants/{variantId}` | İlgili satıcı veya admin | Kullanılabilir stok miktarını günceller. |
| POST | `/api/v1/stocks/reservations` | Giriş yapmış kullanıcı | Stok rezerve eder. |
| POST | `/api/v1/stocks/reservations/{reservationCode}/confirm` | Rezervasyon sahibi veya admin | Rezervasyonu kesinleştirir. |
| DELETE | `/api/v1/stocks/reservations/{reservationCode}` | Rezervasyon sahibi veya admin | Rezervasyonu serbest bırakır. |

`PATCH` gövdesi:

```json
{"stockQuantity": 25}
```

`POST /reservations` gövdesi:

```json
{"variantId": 42, "quantity": 2}
```
