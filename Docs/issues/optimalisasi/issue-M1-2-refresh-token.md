# Issue M-1.2 — JWT Refresh Token

> **Audience:** AI agent junior (model kecil). Agent **DILARANG** menjalankan `docker`, `docker compose`, `mvn`, `npm`, atau perintah build/run/test apa pun. Tugas murni menulis/mengedit kode. Manusia yang test manual.
>
> **Prasyarat:** Issue A + B-1 s/d B-8 selesai. M-1.1 disarankan selesai lebih dulu (tidak wajib, file tidak saling dependen).

---

## 1. Judul & Ringkasan

JWT access token expired 24 jam tanpa mekanisme refresh — user harus re-login tiap 24 jam. Tambahkan refresh token (7 hari) dengan endpoint `/auth/refresh` dan `/auth/logout`.

## 2. Scope

### File yang BOLEH diedit (existing):
- `Backend/src/main/java/com/undangan/online/controller/AuthController.java`
- `Backend/src/main/java/com/undangan/online/service/AuthService.java`
- `Backend/src/main/java/com/undangan/online/service/impl/AuthServiceImpl.java`
- `Backend/src/main/java/com/undangan/online/dto/LoginResponse.java`
- `Backend/src/main/resources/application.properties`

### File BARU yang WAJIB dibuat:
- `Backend/src/main/java/com/undangan/online/entity/RefreshToken.java`
- `Backend/src/main/java/com/undangan/online/repository/RefreshTokenRepository.java`
- `Backend/src/main/java/com/undangan/online/service/RefreshTokenService.java`
- `Backend/src/main/java/com/undangan/online/service/impl/RefreshTokenServiceImpl.java`
- `Backend/src/main/java/com/undangan/online/dto/RefreshRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/RefreshResponse.java`

### DILARANG keras:
- Folder `Sql/`, `Docs/`, `Frontend/`, `Database/`, `TemaUndangan/`, `Musics/`, `Images/`.
- `pom.xml`. Lombok. Entity yang sudah ada.

---

## 3. Requirement Fungsional

### Entity `RefreshToken`
- `id` — BIGINT PK
- `token` — VARCHAR(255) NOT NULL UNIQUE (UUID string)
- `userId` — BIGINT NOT NULL (FK ke `users.id`, ON DELETE CASCADE)
- `issuedAt` — TIMESTAMPTZ NOT NULL
- `expiresAt` — TIMESTAMPTZ NOT NULL
- `userAgent` — VARCHAR(255) (nullable)
- `ipAddress` — VARCHAR(45) (nullable)
- `createdAt` — TIMESTAMPTZ NOT NULL DEFAULT NOW()

### Kebijakan sesi (WAJIB diikuti, jangan menebak sendiri)
- **Boleh ada lebih dari satu refresh token aktif per user secara bersamaan** (mendukung login dari beberapa device/browser). Login baru **TIDAK** menghapus refresh token dari device lain.
- **Rotation wajib:** setiap kali `/auth/refresh` dipanggil dan valid, refresh token LAMA **harus dihapus** dan refresh token BARU dibuat, dalam satu transaksi (`@Transactional`). Refresh token lama tidak boleh bisa dipakai lagi setelah rotation. Ini penting untuk keamanan — kalau tidak di-rotate, token yang bocor bisa dipakai berkali-kali tanpa terdeteksi.
- **Cleanup refresh token expired TIDAK termasuk scope issue ini** (belum ada scheduled job). Ini keputusan sadar untuk MVP, dicatat sebagai TODO untuk fase berikutnya — jangan buat scheduled job di issue ini.

### Service `RefreshTokenService` (interface + impl)
- `generateRefreshToken(userId, userAgent, ipAddress)` — buat token UUID baru, simpan ke DB, return token string.
- `validateRefreshToken(token)` — return `Users` jika valid & belum expired. Kalau tidak ditemukan/expired: **hapus row-nya kalau ada** (biar tidak numpuk), lalu throw `AuthException("INVALID_TOKEN", "Refresh token tidak valid atau expired", 401)`.
- `rotateRefreshToken(oldToken, userAgent, ipAddress)` — validasi `oldToken`, hapus row lama, generate token baru untuk user yang sama, return token baru. Dipakai oleh endpoint `/auth/refresh`. (Method ini menggabungkan validate+delete+generate dalam satu `@Transactional`.)
- `deleteRefreshToken(token)` — revoke 1 token (dipakai saat logout).
- `deleteByUserId(userId)` — revoke semua token milik user (opsional dipakai kalau ada fitur "logout semua device" — tidak wajib dipanggil di issue ini, cukup method-nya tersedia).

### Controller `AuthController` (edit)
- `POST /api/v1/auth/refresh`:
  - Request: `RefreshRequest { refreshToken: string }`
  - Response: `ApiResponse<RefreshResponse { accessToken, refreshToken, expiresIn, tokenType: "Bearer" }>`
  - Logic: panggil `rotateRefreshToken()` (bukan validate lalu generate terpisah — harus 1 transaksi).
- `POST /api/v1/auth/logout`:
  - Request: `RefreshRequest { refreshToken }`
  - Response: `ApiResponse.ok("Logout berhasil", null)`
  - Logic: `deleteRefreshToken(token)`.

### `AuthServiceImpl.login()`
- Login sukses → panggil `generateRefreshToken()` → tambahkan field `refreshToken`, `refreshExpiresIn` ke `LoginResponse`.

### Config
- `application.properties`: `app.jwt.refresh-expiration-ms=${JWT_REFRESH_EXPIRATION_SECONDS:604800}000` (env dalam detik, dikonversi ke ms lewat concat literal `000`).

---

## 4. Requirement Teknis

### SQL — HARUS ditulis, TIDAK dieksekusi oleh agent

```sql
CREATE TABLE refresh_token (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    issued_at   TIMESTAMPTZ NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    user_agent  VARCHAR(255),
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_token_user_id ON refresh_token(user_id);
CREATE INDEX idx_refresh_token_expires_at ON refresh_token(expires_at);
```

### Entity JPA mapping
- `@Table(name="refresh_token")`, `userId` → `@Column(name="user_id")`, dst.
- **Jangan** mapping `@ManyToOne Users` — cukup simpan `userId` sebagai kolom biasa (hindari eager load complexity).

### IP Address & User-Agent
- Kalau `IpAddressUtil` sudah dibuat di issue M-1.1, **pakai ulang class itu**, jangan buat baru.
- Kalau M-1.1 belum dikerjakan, buat logic yang sama langsung di `AuthController`/`AuthServiceImpl` (cek `X-Forwarded-For`, fallback `getRemoteAddr()`), dan catat di ringkasan akhir bahwa ini perlu di-refactor ke utility bersama nanti.

### Package
- Entity → `com.undangan.online.entity/`
- Repository → `com.undangan.online.repository/` (`findByToken`, `deleteByToken`, `deleteByUserId`, `findAllByUserIdAndExpiresAtAfter`)
- Service → interface + impl seperti biasa

### Konvensi
- Constructor injection. SLF4J Logger. `ApiResponse<T>` wrapper. JANGAN Lombok.

### TODO-M1 markers
- Di entity `RefreshToken` — `// TODO-M1: table 'refresh_token' belum ada di DB — butuh SQL di Sql/`.
- Di `RefreshTokenServiceImpl` — `// TODO-M1: belum ada scheduled cleanup untuk token expired, dicatat untuk fase berikutnya`.

---

## 5. Definition of Done
- [ ] Entity `RefreshToken` — field lengkap, TODO-M1 marker.
- [ ] `RefreshTokenRepository` — semua method query tersedia.
- [ ] `RefreshTokenService`/`Impl` — `generateRefreshToken`, `validateRefreshToken`, **`rotateRefreshToken`**, `deleteRefreshToken`, `deleteByUserId`.
- [ ] Rotation terjadi dalam 1 `@Transactional`, token lama benar-benar terhapus setelah rotate.
- [ ] Login dari 2 device berbeda tidak saling menghapus refresh token satu sama lain (tidak ada logic invalidate token lain saat login).
- [ ] `AuthController` — `/auth/refresh` (pakai rotateRefreshToken) + `/auth/logout` → `ApiResponse`.
- [ ] `LoginResponse` — tambah `refreshToken`, `refreshExpiresIn`.
- [ ] `application.properties` — `app.jwt.refresh-expiration-ms` dari env.
- [ ] Tidak ada scheduled job cleanup dibuat (di luar scope, cukup TODO-M1).

---

## 6. Batasan Tegas
1. JANGAN jalankan build tool/docker.
2. JANGAN ubah `Sql/`, `Frontend/`, `Docs/`, `Database/`.
3. JANGAN Lombok, JANGAN tambah dependency.
4. JANGAN ubah entity yang sudah ada.
5. JANGAN buat logic invalidate refresh token device lain saat login baru.
6. JANGAN buat scheduled job cleanup di issue ini.

---

## 7. Catatan Referensi
- `Docs/CLAUDE.md` bagian 6-8.
- `Backend/src/main/java/com/undangan/online/dto/ApiResponse.java`
- `Backend/src/main/java/com/undangan/online/exception/AuthException.java`
- `Backend/src/main/java/com/undangan/online/util/IpAddressUtil.java` (dari M-1.1, kalau sudah ada).
- `Sql/001_init_schema.sql` — konvensi snake_case, `BIGSERIAL`.
