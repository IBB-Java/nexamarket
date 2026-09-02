# Kimlik Doğrulama ve Yetkilendirme

## Uç noktalar

| Yöntem | Yol | Açıklama |
| --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Yalnızca `CUSTOMER` rolünde kullanıcı kaydı |
| `POST` | `/api/v1/auth/login` | Kısa ömürlü access ve uzun ömürlü refresh JWT üretir |
| `POST` | `/api/v1/auth/refresh` | Refresh token rotasyonu yapar; eski token geçersizleşir |
| `POST` | `/api/v1/auth/logout` | Verilen refresh token'ı iptal eder |
| `POST` | `/api/v1/auth/verify-email-code` | E-posta ve altı haneli kodla hesabı doğrular |
| `POST` | `/api/v1/auth/resend-verification` | Doğrulama kodunu tekrar gönderir |
| `GET` | `/api/v1/auth/me` | Geçerli access token ile oturum sahibini döndürür |
| `POST` | `/api/v1/admin/auth/users` | `ADMIN` rolüyle `SELLER`, `ADMIN` veya `COURIER` oluşturur |
| `GET` | `/api/v1/admin/auth/users` | Yalnızca `ADMIN` rolü için kullanıcı listesi |

Access token, `Authorization: Bearer <accessToken>` başlığıyla gönderilir.

## Güvenlik kararları

- Parolalar BCrypt ile hashlenir; hiçbir API yanıtı parola hashini içermez.
- JWT'ler HS tabanlı imzalanır. Üretimde `AUTH_JWT_SECRET` mutlaka benzersiz,
  Base64 kodlu ve en az 32 baytlık gizli anahtarla verilmelidir.
- Her refresh token için veritabanında yalnızca SHA-256 özeti saklanır.
  Yenilemede eski token iptal edilir ve yeni token üretilir; böylece tekrar
  kullanılamaz.
- Varsayılan access token süresi 15 dakika, refresh token süresi 30 gündür.
- Yanlış parola denemeleri kullanıcı bazında sayılır. Varsayılan 5. hatada
  hesap 15 dakika kilitlenir. `AUTH_MAX_FAILED_ATTEMPTS` ve
  `AUTH_LOCK_DURATION` ile değiştirilebilir.
- E-posta doğrulamasında açık kod veritabanına yazılmaz; altı haneli kodun
  yalnızca SHA-256 özeti ve geçerlilik süresi saklanır. Doğrulanmamış kullanıcı
  giriş yapabilir, ancak mağaza her oturum başlangıcında kod penceresini açar.

## Örnek giriş isteği

```json
{
  "email": "customer@example.com",
  "password": "GucluParola!2026"
}
```
