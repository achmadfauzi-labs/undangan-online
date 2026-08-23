# Issue M-1 — Backend Optimization & Maintenance Improvements

> **Audience:** AI agent junior (model kecil). Agent **DILARANG** menjalankan `docker`, `docker compose`, `mvn`, `npm`, atau perintah build/run/test apa pun. Tugas murni menulis/mengedit kode. Manusia yang test manual.
>
> **Prasyarat:** Issue A + semua issue B-1 s/d B-8 selesai.

> **Update:** Jika agent sudah selesai, cek semua `TODO-M1` di file code. Jika ada TODO yang belum selesai (misal entity/table baru karena butuh SQL), tulis di catatan, jangan skip.

---

## 1. Judul & Ringkasan

**Optimization & Production Hardening — Backend Java 21 / Spring Boot.**

Project sudah lewat retrofit (A + B-1 s/d B-8). Tapi ada **5 gap kritis** untuk production-readiness, security, dan maintainability:

1. **Database Audit Trail (M-1.1):** Log hanya ke file/stdout. Perlu `audit_log` table — catat create/update/delete penting (user, admin, upload, RSVP), login sukses/gagal, dengan IP + User-Agent + masking data sensitif.
2. **JWT Refresh Token (M-1.2):** JWT expired 24 jam, TIDAK ada refresh token — user harus re-login tiap 24 jam. Perlu mekanisme refresh (7 hari).
3. **CORS + Security Headers + Password Policy (M-1.3):** TIDAK ada CORS config (frontend akan error). TIDAK ada security headers. Password policy minimal (hanya 8 char).
4. **Structured Logging (M-1.4):** Logback default, tidak ada Request ID/MDC correlation. Perlu log format konsisten + request tracing.
5. **Client Expiry Enforcement Mid-Session (M-1.5):** Login ngecek client expiry, tapi JwtAuthenticationFilter TIDAK mengecek untuk tiap request — token bisa dipakai meski client sudah expired. Perlu expiry check di filter.

---

## 2. Scope — File/Folder yang Boleh Disentuh

### File yang BOLEH diedit (existing):
- `Backend/src/main/java/com/undangan/online/controller/AuthController.java`
- `Backend/src/main/java/com/undangan/online/config/SecurityConfig.java`
- `Backend/src/main/java/com/undangan/online/config/JwtConfig.java`
- `Backend/src/main/java/com/undangan/online/security/JwtTokenProvider.java`
- `Backend/src/main/java/com/undangan/online/security/JwtAuthenticationFilter.java`
- `Backend/src/main/java/com/undangan/online/service/impl/AuthServiceImpl.java`
- `Backend/src/main/java/com/undangan/online/service/AuthService.java`
- `Backend/src/main/java/com/undangan/online/exception/GlobalExceptionHandler.java`
- `Backend/src/main/java/com/undangan/online/dto/LoginResponse.java`
- `Backend/src/main/resources/application.properties`

### File BARU yang WAJIB dibuat:
- `Backend/src/main/java/com/undangan/online/config/CorsConfig.java`
- `Backend/src/main/java/com/undangan/online/config/SecurityHeadersFilter.java`
- `Backend/src/main/java/com/undangan/online/config/PasswordPolicyConfig.java`
- `Backend/src/main/java/com/undangan/online/filter/RequestLoggingFilter.java`
- `Backend/src/main/java/com/undangan/online/service/RefreshTokenService.java`
- `Backend/src/main/java/com/undangan/online/service/impl/RefreshTokenServiceImpl.java`
- `Backend/src/main/java/com/undangan/online/service/AuditLogService.java`
- `Backend/src/main/java/com/undangan/online/service/impl/AuditLogServiceImpl.java`
- `Backend/src/main/java/com/undangan/online/entity/RefreshToken.java`
- `Backend/src/main/java/com/undangan/online/entity/AuditLog.java`
- `Backend/src/main/java/com/undangan/online/repository/RefreshTokenRepository.java`
- `Backend/src/main/java/com/undangan/online/repository/AuditLogRepository.java`
- `Backend/src/main/java/com/undangan/online/controller/AuditLogController.java`
- `Backend/src/main/java/com/undangan/online/dto/RefreshRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/RefreshResponse.java`
- `Backend/src/main/java/com/undangan/online/dto/AuditLogDto.java`
- `Backend/src/main/resources/logback-spring.xml`

### DILARANG keras:
- Folder `Sql/` (tabel baru butuh SQL manual — lihat section 4.3)
- Folder `Docs/`, `Frontend/`, `Database/`
- Folder `TemaUndangan/`, `Musics/`, `Images/` (storage — tidak commit biner)
- `pom.xml` (tidak boleh tambah dependency baru — gunakan yang sudah ada)
- Lombok (per CLAUDE.md rule #10)
- Entity yang ada (`Users.java`, `Client.java`, `Invitation.java`, dsb.) — tidak boleh diubah. Boleh BUAT entity baru saja.

---

## 3. Requirement Fungsional

### M-1.1: Database Audit Trail

**Entity `AuditLog`:**
- `id` — BIGINT PK
- `userId` — BIGINT (FK ke `users.id`, nullable — untuk aksi anonim seperti public RSVP)
- `username` — VARCHAR(50) (snapshot)
- `role` — VARCHAR(30) (snapshot, ex: "USER", "ADMIN")
- `action` — VARCHAR(30) NOT NULL, CHECK IN (`CREATE`,`UPDATE`,`DELETE`,`LOGIN`,`LOGIN_FAILED`,`READ`,`DELETE_FILE`,`UPLOAD_FILE`)
- `module` — VARCHAR(50) (ex: "invitation", "guest", "music", "user", "auth")
- `entityId` — BIGINT (nullable)
- `oldValue` — TEXT (JSON snapshot sebelum perubahan, mask sensitive)
- `newValue` — TEXT (JSON snapshot setelah perubahan, mask sensitive)
- `ipAddress` — VARCHAR(45)
- `userAgent` — VARCHAR(512)
- `createdAt` — TIMESTAMPTZ NOT NULL DEFAULT NOW()
- Timestamp tidak perlu `updatedAt` (immutable).

**Service `AuditLogService` (interface + impl):**
- `logAction(userId, username, role, action, module, entityId, oldValue, newValue, ipAddress, userAgent)` — insert ke DB.
- `listAll(userId, module, action, startDate, endDate, page, size)` — untuk admin view, dengan pagination.
- `getDetail(id)` — lihat satu log.
- **Sensitive field masking:** field dengan nama `password`, `passwordHash`, `token`, `refreshToken`, `email` — mask valuenya (ex: `***` atau `p***n`).

**Controller `AuditLogController`:**
- `GET /api/v1/admin/audit-logs` — query param: `userId`, `module`, `action`, `startDate`, `endDate`, `page`, `size`. Return `ApiResponse<PagedResult<AuditLogDto>>`.
- `GET /api/v1/admin/audit-logs/{id}` — return `ApiResponse<AuditLogDto>`.

**Auto-capture (yang harus dicatat agent — TODO-M1):**
- `AuthController` — login sukses → audit_log action=LOGIN, login gagal → action=LOGIN_FAILED.
- Semua controller admin + client yang melakukan write (POST/PUT/PATCH/DELETE) — auto-catat action=CREATE/UPDATE/DELETE, module nama controller, entity id, old/new value (untuk entity yang ada — snapshot via reflection/ORM optional). — **Asumsi:** Agent implementasikan di `AuditLogService` + panggil manual di service layer penting (AuthServiceImpl, ClientServiceImpl, dll.) — untuk MVP, cukup login + delete saja. Jika ingin full auto-capture, gunakan `@Aspect` — tapi ini optional (catat).
- `ClientGalleryController`, `AdminMusicController`, `ClientMusicController` — upload file → action=UPLOAD_FILE. Delete → action=DELETE_FILE. (Jika agent sempat, panggil service di impl yang ada.)

### M-1.2: JWT Refresh Token

**Entity `RefreshToken`:**
- `id` — BIGINT PK
- `token` — VARCHAR(255) NOT NULL UNIQUE (UUID string)
- `userId` — BIGINT NOT NULL (FK ke `users.id`, ON DELETE CASCADE)
- `issuedAt` — TIMESTAMPTZ NOT NULL
- `expiresAt` — TIMESTAMPTZ NOT NULL
- `userAgent` — VARCHAR(255) (nullable)
- `ipAddress` — VARCHAR(45) (nullable)
- `createdAt` — TIMESTAMPTZ NOT NULL DEFAULT NOW()

**Service `RefreshTokenService` (interface + impl):**
- `generateRefreshToken(userId, userAgent, ipAddress)` — return refresh token string. Simpan ke DB.
- `validateRefreshToken(token)` — return `Users` jika valid & belum expired, delete & throw `AuthException("INVALID_TOKEN", "Refresh token tidak valid atau expired", 401)` kalau tidak.
- `deleteRefreshToken(token)` — revoke 1 token.
- `deleteByUserId(userId)` — revoke semua token user.

**Controller `AuthController` (edit):**
- `POST /api/v1/auth/refresh`:
  - Request body: `RefreshRequest { refreshToken: string }`
  - Response: `ApiResponse<RefreshResponse { accessToken, refreshToken, expiresIn, tokenType: "Bearer" }>`
  - Logic: validate refresh token → regenerate access token + new refresh token.
- `POST /api/v1/auth/logout`:
  - Request body: `RefreshRequest { refreshToken }` (atau Authorization header)
  - Response: `ApiResponse.ok("Logout berhasil", null)`
  - Logic: delete refresh token.

**AuthServiceImpl.login():**
- Login sukses → generate refresh token → return di `LoginResponse` (tambah field `refreshToken`, `refreshExpiresIn`).

**Config:**
- `app.jwt.refresh-expiration-ms` di `application.properties` — dari env `JWT_REFRESH_EXPIRATION_SECONDS` (default 604800 = 7 hari) × 1000.

### M-1.3: CORS + Security Headers + Password Policy

**CORS:**
- `CorsConfigurationSource` bean di `SecurityConfig` atau `CorsConfig` — allowed origin dari env `FRONTEND_ORIGIN_URL` (default `http://localhost:5173`). Allow methods: GET, POST, PUT, PATCH, DELETE, OPTIONS. Allow headers: Authorization, Content-Type. Allow credentials: `true`.

**Security Headers:**
- `SecurityHeadersFilter` — tambah filter setelah Spring Security — response header:
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
  - `X-XSS-Protection: 1; mode=block`
  - `Strict-Transport-Security: max-age=31536000; includeSubDomains`
  - `Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self'`
  - TODO-M1: HSTS hanya di prod — gunakan `@Profile("prod")` atau cek env di filter.

**Password Policy:**
- Di `AuthServiceImpl.registerClient` — sebelum encode password, validasi:
  - Panjang ≥ 8 karakter
  - Mengandung huruf besar (A-Z)
  - Mengandung huruf kecil (a-z)
  - Mengandung digit (0-9)
  - Mengandung karakter khusus (`!@#$%^&*()_+-=[]{}|;:,.<>?`)
  - Jika gagal → `AuthException("VALIDATION_ERROR", "<pesan spesifik>", 400)`.
- `PasswordPolicyConfig` (optional) — helper class validasi password.

### M-1.4: Structured Logging

**logback-spring.xml:**
- Pattern: `%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ} [%thread] %-5level %logger{36} [%X{requestId}] — %msg%n`
- Root level: INFO
- Package com.undangan.online: INFO
- Package org.springframework.security: WARN (jangan log terlalu verbose)
- File output: `logs/app.log` (rolling daily + size-based, max 10MB, retention 30 hari) **+ console.**

**RequestLoggingFilter (`@Component`, `OncePerRequestFilter`):**
- Setiap request: generate `requestId` (UUID.randomUUID) → `MDC.put("requestId", requestId)`.
- Log incoming: `INFO "Incoming request: {} {} from {}"` — method, URI, remoteAddr.
- Log response time: setelah chain.doFilter, hitung durasi → `INFO "Request {} {} completed in {}ms with status {}"`.
- Log request body — HANYA untuk `application/json`. JANGAN log multipart file content. Mask sensitive fields (`password`, `confirmPassword`, `token`, `refreshToken`).
- `finally { MDC.clear(); }`

**SecurityConfig:**
- Daftarkan `RequestLoggingFilter` via `addFilterBefore(requestLoggingFilter, JwtAuthenticationFilter.class)`.

### M-1.5: Client Expiry Enforcement Mid-Session

**JwtAuthenticationFilter:**
- Setelah extract `clientId` dari token:
  - Jika `clientId != null`: query `ClientRepository.findById(clientId)`.
  - Cek `client.getStatus().equals("active")` dan `!client.getExpiresAt().isBefore(LocalDate.now())`.
  - Jika tidak active atau expired → log warn → throw `AuthException("CLIENT_EXPIRED", "Akun sudah expired", 401)`.
  - Jika `Client` tidak ditemukan → throw `AuthException("CLIENT_EXPIRED", "Akun tidak ditemukan", 401)`.

**GlobalExceptionHandler:**
- `AuthException` handler — untuk code `CLIENT_EXPIRED` → return HTTP 401 (bukan 403). Untuk code lain (`FORBIDDEN`, `USER_INACTIVE`) → gunakan `ex.getStatus()` existing.

---

## 4. Requirement Teknis

### Package & Struktur Folder
- **Entity baru** → `com.undangan.online.entity/` (`RefreshToken.java`, `AuditLog.java`). `@Table(name="refresh_token")` / `@Table(name="audit_log")`.
- **Repository** → `com.undangan.online.repository/` (`RefreshTokenRepository.java`, `AuditLogRepository.java`). Interface, extends `JpaRepository`. Pagination pakai `Pageable`.
- **Service** → interface `com.undangan.online.service/` + impl `com.undangan.online.service.impl/`.
- **Controller** → `com.undangan.online.controller/` (`AuditLogController.java`, edit `AuthController.java`).
- **Config** → `com.undangan.online.config/` (`CorsConfig.java`, `SecurityHeadersFilter.java`, `PasswordPolicyConfig.java`).
- **Filter** → `com.undangan.online.filter/` (`RequestLoggingFilter.java`).
- **DTO** → `com.undangan.online.dto/` (`RefreshRequest.java`, `RefreshResponse.java`, `AuditLogDto.java`).
- **Resources** → `Backend/src/main/resources/logback-spring.xml` (baru), edit `application.properties`.

### Dependency yang boleh dipakai
- Semua yang sudah ada di `pom.xml` (Spring Boot 4.1.0, jjwt, BCrypt, Jackson, Spring Data JPA, Spring Security, validation-api).
- Jika perlu JSON di `oldValue`/`newValue` — gunakan Jackson `ObjectMapper` (bundled di Spring Boot, inject via `@Autowired`).
- `spring-boot-starter-actuator` — sudah ada (exposed health).
- **JANGAN** tambah dependency / ubah `pom.xml`. Kalau butuh library baru → tanya manusia dulu, atau pakai library yang sudah ada.

### Konvensi Environment Variable
| Variable | Deskripsi | Default |
|---|---|---|
| `JWT_SECRET` | JWT signing key (existing) | — |
| `JWT_EXPIRATION_SECONDS` | Access token expiry | 86400 (24 jam) |
| `JWT_REFRESH_EXPIRATION_SECONDS` | Refresh token expiry (BARU) | 604800 (7 hari) |
| `FRONTEND_ORIGIN_URL` | CORS allowed origin (BARU) | `http://localhost:5173` |
| `SPRING_DATASOURCE_URL` | DB koneksi (existing) | jdbc:postgresql://localhost:5432/undangan |
| `SPRING_DATASOURCE_USERNAME` | DB user | `undangan` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `undangan` |

### Schema / Database Requirement — **HARUS ditulis, tidak di-eksekusi oleh agent**

> **ATURAN:** Agent TIDAK boleh menulis/menghapus/mengubah file di `Sql/`. Jika butuh table baru, entity/repository akan tetap kompil (JPA), tapi query ke DB akan error sampai table dibuat. Agent tulis table requirement di bawah ini — manusia yang eksekusi SQL via Sql/.

**Table baru yang butuh SQL manual:**

1. **`refresh_token`** (untuk M-1.2):
```sql
CREATE TABLE refresh_token (
    id          BIGSERIAL PRIMARY KEY,
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

2. **`audit_log`** (untuk M-1.1):
```sql
CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES users(id) ON DELETE SET NULL,
    username    VARCHAR(50),
    role        VARCHAR(30),
    action      VARCHAR(30) NOT NULL
                CHECK (action IN ('CREATE','UPDATE','DELETE','LOGIN','LOGIN_FAILED','READ','UPLOAD_FILE','DELETE_FILE')),
    module      VARCHAR(50),
    entity_id   BIGINT,
    old_value   TEXT,
    new_value   TEXT,
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(512),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_action ON audit_log(action);
CREATE INDEX idx_audit_log_module ON audit_log(module);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at DESC);
```

**Entity JPA mapping:**
- `RefreshToken` — `@Table(name="refresh_token")`, field `userId` → `@Column(name="user_id")`, dst. Jangan mapping `@ManyToOne Users` (hindari eager load complexity — cukup simpan userId).
- `AuditLog` — `@Table(name="audit_log")`, field `entityId` → `@Column(name="entity_id")`, `oldValue` → `@Column(name="old_value", columnDefinition="TEXT")`, dst.

### Coding Conventions
- Ikuti pola interface + impl (misal `AuthService` + `AuthServiceImpl`). Entity tidak punya interface service.
- SLF4J Logger (`LoggerFactory.getLogger(...)`). JANGAN `System.out.println`.
- DTO untuk request/response — jangan expose Entity.
- `ApiResponse<T>` wrapper — semua controller (existing + baru) pakai ini.
- `AuthException(errorCode, message, httpStatus)` — untuk CLIENT_EXPIRED, INVALID_TOKEN, VALIDATION_ERROR, CONFLICT, dll.
- Constructor injection — jangan field injection.
- JANGAN pakai Lombok — tulis getter/setter/constructor manual.

### TODO-M1 markers (agent Wajib tandai)
- Di entity `RefreshToken` — `// TODO-M1: table 'refresh_token' belum ada di DB — butuh SQL di Sql/`.
- Di entity `AuditLog` — `// TODO-M1: table 'audit_log' belum ada di DB — butuh SQL di Sql/`.
- Di `SecurityHeadersFilter` — `// TODO-M1: HSTS hanya di profil prod` — active selalu (atau gunakan env flag).
- Di `GlobalExceptionHandler` — `// TODO-M1: audit log auth exceptions` — panggil `AuditLogService.logAction` untuk login_failed di AuthServiceImpl.
- Di entity/repository baru — tidak punya `@GeneratedValue(strategy=GenerationType.IDENTITY)` — pakai `BIGSERIAL` (manual, tidak perlu annotation khusus, pakai default `GenerationType.IDENTITY`).

---

## 5. Definition of Done

### M-1.1: Database Audit Trail
- [ ] Entity `AuditLog` ada — field lengkap sesuai requirement, `@Table(name="audit_log")`.
- [ ] Repository `AuditLogRepository` — extends `JpaRepository<AuditLog, Long>`, method query by userId+action+module+date range (dengan `Pageable`).
- [ ] Interface `AuditLogService` + impl `AuditLogServiceImpl` — method `logAction`, `listAll`, `getDetail`.
- [ ] Sensitive field masking — field `password`, `token`, `refreshToken`, `email` di mask (ex: `***masked***`) di oldValue/newValue.
- [ ] `AuditLogController` — `GET /api/v1/admin/audit-logs` + `GET /api/v1/admin/audit-logs/{id}` → `ApiResponse`.
- [ ] `AuthServiceImpl.login()` — log login sukses (action=LOGIN) + login gagal (action=LOGIN_FAILED) ke audit_log.
- [ ] LoginResponse — tidak expose refresh token (jika sudah ada di M-1.2).
- [ ] TODO-M1 di entity/repository.

### M-1.2: JWT Refresh Token
- [ ] Entity `RefreshToken` — field lengkap, `@Table(name="refresh_token")`.
- [ ] Repository `RefreshTokenRepository` — `findByToken`, `deleteByUserId`, `deleteByToken`, `findAllByUserIdAndExpiresAtAfter`.
- [ ] Interface `RefreshTokenService` + impl — method `generateRefreshToken`, `validateRefreshToken`, `deleteRefreshToken`, `deleteByUserId`.
- [ ] `AuthController` — `POST /api/v1/auth/refresh` → `ApiResponse<RefreshResponse>`.
- [ ] `AuthController` — `POST /api/v1/auth/logout` → `ApiResponse.ok("Logout berhasil", null)`.
- [ ] `AuthServiceImpl.login()` — generate refresh token, return di `LoginResponse`.
- [ ] DTO `RefreshRequest` (refreshToken) + `RefreshResponse` (accessToken, refreshToken, expiresIn, tokenType).
- [ ] `application.properties` — `app.jwt.refresh-expiration-ms=${JWT_REFRESH_EXPIRATION_SECONDS:604800}000`.

### M-1.3: CORS + Security Headers + Password Policy
- [ ] `CorsConfigurationSource` bean di `SecurityConfig` — allowed origin dari `FRONTEND_ORIGIN_URL` (env), allow credentials=true.
- [ ] `SecurityHeadersFilter` (`@Component`) — tambah 6 header di response (HSTS, X-Frame-Options, X-Content-Type-Options, X-XSS-Protection, CSP). TODO-M1: HSTS conditional di prod.
- [ ] Filter terdaftar di `SecurityConfig` (addFilterAfter).
- [ ] Password validation di `AuthServiceImpl.registerClient` — 5 rule (length, uppercase, lowercase, digit, special). AuthException 400 kalau gagal.
- [ ] `FRONTEND_ORIGIN_URL` di `application.properties`.

### M-1.4: Structured Logging
- [ ] `logback-spring.xml` ada — pattern `[%X{requestId}]`, file output rolling + console.
- [ ] `RequestLoggingFilter` — generate requestId, MDC.put/clear, log incoming + response time + body (mask sensitive).
- [ ] `SecurityConfig` — register RequestLoggingFilter (addFilterBefore).
- [ ] Log request body masking — password/token/refreshToken tidak ter-log plaintext.

### M-1.5: Client Expiry Enforcement Mid-Session
- [ ] `JwtAuthenticationFilter` — cek client expiry (status + expiresAt) tiap request (jika clientId != null).
- [ ] Throw `AuthException("CLIENT_EXPIRED", "...", 401)` jika expired/inactive.
- [ ] `GlobalExceptionHandler` — code CLIENT_EXPIRED → HTTP 401 (bukan 403).
- [ ] TODO-M1 di handler — pastikan logic tidak break aksi lain.

### Cross-cutting
- [ ] Tidak pakai Lombok di file baru — cek semua file .java baru.
- [ ] Semua endpoint (existing + baru) return `ApiResponse<T>`.
- [ ] `Sql/` tidak disentuh — cek tidak ada write ke Sql/.
- [ ] `ApiResponse.java` tidak disentuh.
- [ ] `pom.xml` tidak disentuh.
- [ ] Semua entity baru punya `// TODO-M1` di bagian atas untuk table requirement.

---

## 6. Batasan Tegas

1. JANGAN jalankan build tool (mvn, gradle).
2. JANGAN jalankan docker / docker-compose.
3. JANGAN ubah file di folder `Sql/`, `Frontend/`, `Docs/`, `Database/`.
4. JANGAN tulis/commit file biner ke `TemaUndangan/`, `Musics/`, `Images/`.
5. JANGAN pakai `ddl-auto=update` atau `create` — pakai `validate` (existing).
6. JANGAN pakai Lombok.
7. JANGAN ubah Entity yang ada (`Users`, `Client`, `Invitation`, dll.) — hanya BOLEH bikin entity baru.
8. JANGAN ubah business behavior utama (misal: login ganti jadi async, atau hapus field di LoginResponse). Hanya TAMBAH fitur.
9. JANGAN log data sensitif — password, token, refresh token, email pribadi tamu harus di-mask.
10. JANGAN tambah dependency baru di `pom.xml`.
11. JANGAN tambah endpoint di luar yang dideskripsikan di requirement section 3.

---

## 7. Catatan Referensi

- `Docs/CLAUDE.md` — aturan global (response format, logging, clean code, OOP). Baca section #6–#8.
- `Docs/PRD-Undangan-Online.md` — role & access pattern (Admin/Client/Guest).
- `Sql/001_init_schema.sql` — convention: snake_case, `BIGSERIAL`, trigger `trg_*_updated_at`.
- `Backend/src/main/java/com/undangan/online/dto/ApiResponse.java` — wrapper response (`ok`, `created`, `error`).
- `Backend/src/main/java/com/undangan/online/exception/AuthException.java` — error code + HTTP status.
- `Backend/src/main/java/com/undangan/online/service/impl/AuthServiceImpl.java` — contoh service impl, login flow, client expiry check.
- `Backend/src/main/java/com/undangan/online/config/SecurityConfig.java` — filter chain, role-based access.
- `Backend/src/main/java/com/undangan/online/entity/Client.java` — field `expiresAt`, `status`.
- `Backend/src/main/java/com/undangan/online/entity/Users.java` — field `clientId`, `roleCode`.
- `Backend/src/main/java/com/undangan/online/repository/ClientRepository.java` — query client by id.
- `Backend/src/main/java/com/undangan/online/dto/LoginResponse.java` — response login (akan ditambah refresh token).

---

*Issue M-1 adalah catch-all untuk production hardening. Sub-task boleh dikerjakan secara paralel (masing-masing file baru tidak saling dependen) — tapi table `refresh_token` & `audit_log` di database Wajib dikerjakan manusia pertama kali (via Sql/), karena rule melarang agent sentuh Sql/. Setelah table dibuat, entity+repository+service bisa ngone.*
