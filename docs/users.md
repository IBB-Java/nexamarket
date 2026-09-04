# Kullanıcı ve Satıcı Profilleri

## Kullanıcı profili

`GET` ve `PATCH /api/v1/users/me` oturum sahibinin ad, soyad ve telefon
bilgilerini döndürür veya günceller. Bu uç noktalar access token gerektirir ve
başka bir kullanıcının profil kimliğini kabul etmez.

## Satıcı mağaza profili

`SELLER` rolündeki kullanıcılar `POST /api/v1/sellers/me` ile mağaza profili
oluşturur. Yeni profil `PENDING_APPROVAL` durumunda başlar; bu aşamada herkese
açık mağaza uç noktasında görünmez.

`ADMIN` rolü aşağıdaki uç noktalarla inceleme yapar:

| Yöntem | Yol | Açıklama |
| --- | --- | --- |
| `GET` | `/api/v1/admin/sellers/pending` | Onay bekleyen mağazalar |
| `PATCH` | `/api/v1/admin/sellers/{sellerId}/status` | `ACTIVE`, `REJECTED` veya `SUSPENDED` durumu |
| `PATCH` | `/api/v1/admin/users/{userId}/status` | Kullanıcıyı `ACTIVE` veya `DISABLED` yapar |
| `PATCH` | `/api/v1/admin/users/{userId}/role` | Kullanıcıya güvenli şekilde rol atar |
| `DELETE` | `/api/v1/admin/users/{userId}` | Hesabı soft-delete eder; geçmiş siparişleri korur |

Silme işlemi kullanıcı satırını fiziksel olarak kaldırmaz. Hesap `DELETED`
durumuna alınır, `deleted_at` zamanı yazılır ve refresh tokenları iptal edilir.
Devre dışı veya silinmiş kullanıcı giriş yapamaz; daha önce aldığı access token
da güncel hesap durumu kontrolü nedeniyle kullanılamaz. `CustomerOrder`,
`SubOrder` ve `OrderItem` kayıtları kullanıcı hesabından cascade ile silinmez.

## Role göre sipariş ve teslimat görünürlüğü

`GET /api/v1/orders` authenticated principal içindeki role göre filtrelenir:

- `CUSTOMER`: yalnızca kendi siparişleri,
- `SELLER`: yalnızca `sellerId` kendisine ait alt siparişler,
- `COURIER`: genel Order DTO'sunu göremez; yalnızca kendisine ait
  `DeliveryAssignment` kayıtlarını gizlilik kapsamlı `CourierDeliveryResponse`
  ile görür,
- `ADMIN`: bütün alt siparişler.

`GET /api/v1/orders/{orderId}` aynı kaynak sahipliği kontrolünü tek sipariş için
de uygular. Yönetici `POST /api/v1/admin/deliveries/assign` ile aktif bir
`COURIER` atar. Ödeme sonrasında otomatik atama yapılmaz. Kurye kabul, ret,
teslim alma, dağıtım, teslim ve gerekçeli başarısızlık işlemlerini yalnızca kendi
assignment kimliğiyle yapabilir. Ret ve başarısızlık geçmişi silinmez; yeni
atama yeni bir satır olarak oluşturulur.

Yalnızca `ACTIVE` mağazalar `GET /api/v1/sellers/{sellerId}` üzerinden
herkese açıktır. Komisyon oranı ilk oluşturma sırasında %10 olarak atanır ve
satıcı yanıtında görünür.
