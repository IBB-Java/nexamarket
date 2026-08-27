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

Yalnızca `ACTIVE` mağazalar `GET /api/v1/sellers/{sellerId}` üzerinden
herkese açıktır. Komisyon oranı ilk oluşturma sırasında %10 olarak atanır ve
satıcı yanıtında görünür.
