# Issue — Fase 2: Backend Core & Auth

> **Audience:** AI agent junior (model kecil). Agent **DILARANG** menjalankan docker/mvn/npm atau perintah build apa pun — hanya menulis/mengedit file. Manusia yang akan test manual via `docker compose up`.
>
> **Prasyarat:** Fase 0 (bootstrap infrastruktur) harus sudah selesai dan lolos sebelum mulai fase ini. Semua file yang sudah ada di `Backend/` dari Fase 0 **jangan dihapus/di-overwrite** — agent HANYA menambah, tidak mengganti yang sudah ada (kecuali `pom.xml` dan `application.properties` yang memang perlu ditambahkan dependency/konfigurasi).

---

## 1. Judul & Ringkasan

**Judul:** `[Fase 2] Backend Core & Auth — Entity + Repository (JPA) + JWT Security + Login/Register + Config Upload Path`

**Ringkasan (1 paragraf):**
Fase ini membangun fondasi backend: (a) entity JPA yang match schema `Sql/001_init_schema.sql`, (b) repository Spring Data JPA untuk setiap entity, (c) konfigurasi Spring Security dengan JWT (BCrypt password, role-based access: SUPER_ADMIN / CLIENT / GUEST), (d) endpoint autentikasi `POST /api/v1/auth/login` dan `POST /api/v1/auth/register` (register HANYA untuk CLIENT, dibuat oleh Super Admin), dan (e) konfigurasi path upload storage (mount ke `Images/`, `Musics/`, `TemaUndangan/`). Tidak ada logika bisnis (RSVP, guestbook, gallery CRUD, dll) — itu fase berikutnya.

**Yang TIDAK termasuk fase ini:**
RSVP, guestbook, gallery, love story, invitation session, invitation person, template, music, system parameter CRUD; front-end autentikasi; token refresh; payment; notification; test code.

---

## 2. Scope — File/Folder yang Boleh Disentuh

Agent HANYA boleh membuat/mengedit file di lokasi di bawah. Di luar daftar ini → JANGAN disentuh.

### Folder existing (edit file yang sudah ada, jangan hapus/rename):
- `Backend/pom.xml` — tambah dependency (bagian 4.2)
- `Backend/src/main/resources/application.properties` — tambah konfigurasi security & storage (bagian 4.4)
- `Backend/src/main/java/com/undangan/online/OnlineApplication.java` — **JANGAN disentuh**

### File baru yang WAJIB dibuat:

**Package `config`:**
- `Backend/src/main/java/com/undangan/online/config/SecurityConfig.java`
- `Backend/src/main/java/com/undangan/online/config/JwtConfig.java`
- `Backend/src/main/java/com/undangan/online/config/StorageConfig.java`

**Package `security`:**
- `Backend/src/main/java/com/undangan/online/security/JwtTokenProvider.java`
- `Backend/src/main/java/com/undangan/online/security/JwtAuthenticationFilter.java`
- `Backend/src/main/java/com/undangan/online/security/CustomUserDetailsService.java`

**Package `entity`:**
- `Backend/src/main/java/com/undangan/online/entity/Client.java`
- `Backend/src/main/java/com/undangan/online/entity/Role.java`
- `Backend/src/main/java/com/undangan/online/entity/RolePermission.java`
- `Backend/src/main/java/com/undangan/online/entity/Users.java`
- `Backend/src/main/java/com/undangan/online/entity/SystemParameter.java`
- `Backend/src/main/java/com/undangan/online/entity/Template.java`
- `Backend/src/main/java/com/undangan/online/entity/Music.java`
- `Backend/src/main/java/com/undangan/online/entity/Invitation.java`
- `Backend/src/main/java/com/undangan/online/entity/InvitationSession.java`
- `Backend/src/main/java/com/undangan/online/entity/InvitationPerson.java`
- `Backend/src/main/java/com/undangan/online/entity/LoveStory.java`
- `Backend/src/main/java/com/undangan/online/entity/Gallery.java`
- `Backend/src/main/java/com/undangan/online/entity/Guest.java`

**Package `repository`:**
- `Backend/src/main/java/com/undangan/online/repository/ClientRepository.java`
- `Backend/src/main/java/com/undangan/online/repository/RoleRepository.java`
- `Backend/src/main/java/com/undangan/online/repository/UsersRepository.java`
- `Backend/src/main/java/com/undangan/online/repository/SystemParameterRepository.java`
- `Backend/src/main/java/com/undangan/online/repository/TemplateRepository.java`
- `Backend/src/main/java/com/undangan/online/repository/MusicRepository.java`
- `Backend/src/main/java/com/undangan/online/repository/InvitationRepository.java`
- `Backend/src/main/java/com/undangan/online/repository/GuestRepository.java`

**Package `controller` (autentikasi & public read-only):**
- `Backend/src/main/java/com/undangan/online/controller/AuthController.java`
- `Backend/src/main/java/com/undangan/online/controller/PublicInvitationController.java`

**Package `dto`:**
- `Backend/src/main/java/com/undangan/online/dto/LoginRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/LoginResponse.java`
- `Backend/src/main/java/com/undangan/online/dto/RegisterClientRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/ErrorResponse.java`
- `Backend/src/main/java/com/undangan/online/dto/UserDto.java`

**Package `exception`** (minimal — 2 file):
- `Backend/src/main/java/com/undangan/online/exception/AuthException.java`
- `Backend/src/main/java/com/undangan/online/exception/GlobalExceptionHandler.java`

**Package `service`** (autentikasi & init schema):
- `Backend/src/main/java/com/undangan/online/service/AuthService.java`
- `Backend/src/main/java/com/undangan/online/service/ClientInitService.java`

### DILARANG keras:
- Folder `Sql/` — seluruh isinya
- Folder `Docs/`, `Frontend/`, `Database/`
- Folder `TemaUndangan/`, `Musics/`, `Images/` — **JANGAN isi file biner**, cuma baca dari `application.properties`
- Folder `.claude/`, `.git/`
- File `Backend/pom.xml` yang sudah ada dari Fase 0 — agent BOLEH tambahkan dependency, TIDAK BOLEH hapus yang sudah ada
- File `Backend/src/main/resources/application.properties` yang sudah ada — agent BOLEH tambahkan di baris bawah, TIDAK BOLEH hapus konfigurasi yang sudah ada
- `Backend/src/main/java/com/undangan/online/OnlineApplication.java` — JANGAN disentuh

---

## 3. Requirement Fungsional

### 3.1 Entity & Repository

**13 entity** harus dibuat sesuai schema `Sql/001_init_schema.sql`:
`Client`, `Role`, `RolePermission`, `Users`, `SystemParameter`, `Template`, `Music`, `Invitation`, `InvitationSession`, `InvitationPerson`, `LoveStory`, `Gallery`, `Guest`.

Aturan entity:
- Pakai anotasi JPA (`@Entity`, `@Table`, `@Column`, `@Id`, `@GeneratedValue`, `@ManyToOne`, `@OneToMany`, dll.)
- `ddl-auto=none` atau `validate` — agent tidak menjalankan `mvn`, tapi harus memastikan entity konsisten dengan schema agar manusia bisa `docker compose up` tanpa error Hibernate.
- `BIGSERIAL` → `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- `TIMESTAMPTZ` → `OffsetDateTime` atau `Instant`
- `DATE` → `LocalDate`
- Enum-like kolom (`status`, `attendance_status`, `role`) → `String` (bukan enum Java), validasi di service layer.
- Kolom dengan constraint `CHECK IN (...)` → tulis sebagai `@Column(columnDefinition = "VARCHAR(20)")` + anotasi lain, tapi validasi logika di service.
- **Mapping relasi** (`@ManyToOne`, `@OneToMany`) harus konsisten: jangan ada orphan record tanpa FK yang di-manage.
- Trigger `updated_at` tidak perlu direplikasi di Java (handled oleh PostgreSQL). Kolom `updated_at` tetap ada di entity sebagai `OffsetDateTime updatedAt` — Hibernate cukup baca, tidak perlu trigger di app layer.

**8 repository** untuk query yang akan dibutuhkan fase berikutnya:
`ClientRepository`, `RoleRepository`, `UsersRepository`, `SystemParameterRepository`, `TemplateRepository`, `MusicRepository`, `InvitationRepository`, `GuestRepository`.

Minimal query yang harus ada di repository:
- `ClientRepository`: `findByCode(String)` — `Optional<Client>`
- `RoleRepository`: `findByCode(String)` — `Optional<Role>`
- `UsersRepository`: `findByUsername(String)` — `Optional<Users>`; `findByEmail(String)`; `findByClientId(Long)` — `List<Users>`
- `InvitationRepository`: `findBySlug(String)` — `Optional<Invitation>`; `findByClientId(Long)` — `Optional<Invitation>`; `findByStatus(String)` — `List<Invitation>`
- `GuestRepository`: `findByInvitationToken(String)` — `Optional<Guest>`; `findByInvitationId(Long)` — `List<Guest>`; `findByInvitationIdAndIsPublishedTrue(Long)` — `List<Guest>` (untuk guestbook publik)
- `SystemParameterRepository`: `findByGroupCodeAndCode(String, String)` — `Optional<SystemParameter>`
- `TemplateRepository`: `findByStatus(String)` — `List<Template>`
- `MusicRepository`: `findByStatus(String)` — `List<Music>`

Tidak perlu repository untuk `RolePermission`, `InvitationSession`, `InvitationPerson`, `LoveStory`, `Gallery` di fase ini (akan dipakai fase bisnis).

### 3.2 Konfigurasi Security (JWT + RBAC)

**Arsitektur akses:**

| Prefix endpoint | Role yang boleh akses | Catatan |
|---|---|---|
| `/api/v1/public/**` | SEMUA (tanpa token) | Guest: halaman undangan, RSVP, guestbook |
| `/api/v1/admin/**` | `ROLE_SUPER_ADMIN` | Dashboard Super Admin |
| `/api/v1/client/**` | `ROLE_CLIENT` | Dashboard Client, cek client_id milik sendiri |
| `/api/v1/auth/**` | SEMUA (tanpa token) | Login & register endpoint |

**Login (`POST /api/v1/auth/login`):**
- Body: `{ "username": "string", "password": "string" }`
- BCrypt verify password dari kolom `users.password_hash`
- Return: `{ "accessToken": "jwt...", "tokenType": "Bearer", "expiresIn": 86400, "user": { "id", "username", "name", "roleCode", "clientId" } }`
- Gagal login: HTTP 401 `{ "error": "INVALID_CREDENTIALS", "message": "Username atau password salah" }`
- User tidak aktif (`status != 'active'`): HTTP 403 `{ "error": "USER_INACTIVE", "message": "Akun tidak aktif" }`
- Client expired (`client.expires_at < today`): HTTP 403 `{ "error": "CLIENT_EXPIRED", "message": "Akun sudah expired" }`

**Register client (`POST /api/v1/auth/register`):**
- **HANYA Super Admin** yang boleh mendaftarkan client baru. Dipakai oleh Super Admin dari Web 1 untuk membuatkan akun client secara manual (sesuai PRD: "request-based, bukan self-service").
- Body: `{ "username": "string", "email": "string", "password": "string", "name": "string", "clientId": number }`
- Validasi: username unik, email unik, password min 8 char, clientId valid dan milik role CLIENT.
- Password di-hash BCrypt sebelum disimpan.
- Return: `{ "user": { "id", "username", "name", "roleCode", "clientId" }, "message": "User berhasil dibuat" }`
- Conflict (username/email duplikat): HTTP 409

**Konfigurasi JWT:**
- Secret key dibaca dari environment variable `${JWT_SECRET}` — **WAJIB**, jangan hardcode.
- Algoritma: HS256.
- Claims wajib: `sub` (username), `role` (ROLE_SUPER_ADMIN / ROLE_CLIENT), `clientId` (nullable — null untuk SUPER_ADMIN).
- Expired: 24 jam (86400 detik).
- Token dikirim di header `Authorization: Bearer <token>` untuk endpoint yang butuh auth.

### 3.3 Public Endpoint (Guest, tanpa login)

**`GET /api/v1/public/invitation/{slug}`** — data undangan publik (baca saja, tanpa token):
- Return 200 dengan data: `{ "slug", "eventTypeCode", "status", "welcomeMessage", "templateId", "template": { "code", "name", "folderPath", "thumbnailCss" }, "persons": [{ "role", "name", "nickname", "parentNames", "photoPath" }], "sessions": [{ "name", "sessionDate", "sessionTime", "location", "mapsUrl" }] }`.
- Tidak ditemukan: 404.
- Ini endpoint untuk Guest membuka halaman undangan. Data harus cukup untuk render Web 3.

**`GET /api/v1/public/invitation/{slug}/guestbook`** — ucapan tamu:
- Return 200: array `{ "name", "message", "createdAt" }` yang `isPublished = true`, urut `createdAt DESC`.

**Catatan:** RSVP submit (Upsert Guest) dan guestbook submit POST — itu fase 3. Di fase 2 ini HANYA GET/read public.

### 3.4 Client Isolation (Penting!)

Untuk semua endpoint di `/api/v1/client/**`:
- Token JWT mengandung `clientId`. Service layer WAJIB validasi bahwa `users.clientId` dari token == `clientId` di resource yang diakses.
- Kalau client A coba akses data client B → HTTP 403 `{ "error": "ACCESS_DENIED", "message": "Anda tidak memiliki akses ke resource ini" }`.
- Ini berlaku juga untuk `InvitationRepository.findByClientId(Long clientId)` — harus pakai clientId dari token, bukan dari parameter request.

### 3.5 Konfigurasi File Upload Path

**StorageConfig** membaca dari env var (sudah di-mount di docker-compose dari Fase 0):
- `STORAGE_THEMES_PATH` (default: `/app/storage/themes`)
- `STORAGE_MUSICS_PATH` (default: `/app/storage/musics`)
- `STORAGE_IMAGES_PATH` (default: `/app/storage/images`)

Agent **tidak perlu** membuat endpoint upload di fase ini. Yang perlu dibuat:
1. `StorageConfig.java` — bean `Path` untuk ketiga folder storage, pakai `Paths.get(env)`.
2. Validasi bahwa folder tujuan ada (`Files.exists()`) — `throw` RuntimeException saat startup jika tidak ada (early fail).
3. Konfigurasi max file size: 10 MB untuk musik (`@Value` dari env `MAX_MUSIC_SIZE_MB`, default 10), 5 MB untuk gambar (env `MAX_IMAGE_SIZE_MB`, default 5).
4. Konfigurasi multipart di `application.properties`:
   ```properties
   spring.servlet.multipart.max-file-size=10MB
   spring.servlet.multipart.max-request-size=50MB
   ```

### 3.6 Client Init Service (Data Master)

`ClientInitService` berjalan saat aplikasi start (`@PostConstruct` atau `@EventListener(ApplicationReadyEvent.class)`):
1. Cek apakah role `ADMIN`, `STAFF`, `USER` sudah ada di tabel `role`. Kalau belum, insert dari seed data di `Sql/002_seed_master_data.sql`.
2. Jika ada failure koneksi DB di sini → log ERROR, **tidak crash** aplikasi. Biarkan aplikasi tetap jalan — manusia yang troubleshoot.

Ini opsional tapi sangat membantu. Jika terasa terlalu kompleks, skip — data master di-seed manual lewat `Sql/002_seed_master_data.sql`.

---

## 4. Requirement Teknis

### 4.1 Package & Struktur

```
com.undangan.online/
├── OnlineApplication.java          # existing, JANGAN diubah
├── config/
│   ├── SecurityConfig.java         # NEW
│   ├── JwtConfig.java              # NEW
│   └── StorageConfig.java          # NEW
├── security/
│   ├── JwtTokenProvider.java       # NEW
│   ├── JwtAuthenticationFilter.java # NEW
│   └── CustomUserDetailsService.java # NEW
├── entity/
│   ├── Client.java                 # NEW — 13 entity
│   ├── Role.java
│   ├── RolePermission.java
│   ├── Users.java
│   ├── SystemParameter.java
│   ├── Template.java
│   ├── Music.java
│   ├── Invitation.java
│   ├── InvitationSession.java
│   ├── InvitationPerson.java
│   ├── LoveStory.java
│   ├── Gallery.java
│   └── Guest.java
├── repository/
│   ├── ClientRepository.java       # NEW — 8 repository
│   ├── RoleRepository.java
│   ├── UsersRepository.java
│   ├── SystemParameterRepository.java
│   ├── TemplateRepository.java
│   ├── MusicRepository.java
│   ├── InvitationRepository.java
│   └── GuestRepository.java
├── controller/
│   ├── AuthController.java          # NEW — 2 controller
│   └── PublicInvitationController.java
├── dto/
│   ├── LoginRequest.java            # NEW — 5 DTO
│   ├── LoginResponse.java
│   ├── RegisterClientRequest.java
│   ├── UserDto.java
│   └── ErrorResponse.java
├── service/
│   ├── AuthService.java             # NEW — 2 service
│   └── ClientInitService.java       # NEW (opsional)
└── exception/
    ├── AuthException.java           # NEW — 2 exception
    └── GlobalExceptionHandler.java
```

### 4.2 Dependency yang Boleh Dipakai (di `pom.xml`)

Agent HARUS tambahkan dependency ini ke `Backend/pom.xml` yang sudah ada (append, jangan hapus yang sudah ada):

| Dependency | Artifact | Tujuan |
|---|---|---|
| Security | `spring-boot-starter-security` | JWT + RBAC |
| Validation | `spring-boot-starter-validation` | `@Valid` di request body |
| JWT | `io.jsonwebtoken:jjwt-api` | Token generation & validation |
| JWT Runtime | `io.jsonwebtoken:jjwt-impl` (scope: runtime) | Implementasi JJWT |
| JWT Jackson | `io.jsonwebtoken:jjwt-jackson` (scope: runtime) | JSON serialization JJWT |

**DILARANG ditambahkan (fase ini):** Lombok, MapStruct, Swagger/SpringDoc, Redis, Kafka, Thymeleaf, dll. — semua di luar tabel di atas.

### 4.3 Konvensi Environment Variable

**Semua** credential, secret, dan path HARUS dibaca dari env var. Berikut daftar yang harus dibaca:

| Env Var | Tujuan | Default (dev) |
|---|---|---|
| `JWT_SECRET` | Secret key JWT (min 256-bit) | **(tidak ada default — WAJIB di-set di docker-compose)** |
| `JWT_EXPIRATION_SECONDS` | TTL token | `86400` (24 jam) |
| `STORAGE_THEMES_PATH` | Path mount tema | `/app/storage/themes` |
| `STORAGE_MUSICS_PATH` | Path mount musik | `/app/storage/musics` |
| `STORAGE_IMAGES_PATH` | Path mount gambar | `/app/storage/images` |
| `MAX_MUSIC_SIZE_MB` | Max size file musik | `10` |
| `MAX_IMAGE_SIZE_MB` | Max size file gambar | `5` |

Konvensi: semua nama env var pakai UPPER_SNAKE_CASE, prefix sesuai domain (`JWT_`, `STORAGE_`, `MAX_`).

### 4.4 Update `application.properties`

Tambahkan di bawah file yang sudah ada (JANGAN hapus yang sudah ada):

```properties
# === Fase 2: Security & Storage ===

# JWT
app.jwt.secret=${JWT_SECRET:}
app.jwt.expiration-ms=${JWT_EXPIRATION_SECONDS:86400}000

# Storage paths (mount dari docker-compose)
app.storage.themes=${STORAGE_THEMES_PATH:/app/storage/themes}
app.storage.musics=${STORAGE_MUSICS_PATH:/app/storage/musics}
app.storage.images=${STORAGE_IMAGES_PATH:/app/storage/images}

# Max file upload
spring.servlet.multipart.max-file-size=${MAX_MUSIC_SIZE_MB:10}MB
spring.servlet.multipart.max-request-size=50MB

# Spring Security — nonaktifkan default login form (API-only)
spring.security.filter.order=1
```

### 4.5 Konfigurasi Docker Compose (Update)

Manusia akan menambahkan ke `docker-compose.yml` root. Agent HARUS tulis di catatan akhir jawaban apa yang manusia perlu tambahkan. Template:

```yaml
# Di service backend, tambahkan blok environment ini:
  backend:
    environment:
      # ... existing dari Fase 0 ...
      JWT_SECRET: ${JWT_SECRET:-change-me-in-production-use-256-bit-key}
      JWT_EXPIRATION_SECONDS: "86400"
      STORAGE_THEMES_PATH: /app/storage/themes
      STORAGE_MUSICS_PATH: /app/storage/musics
      STORAGE_IMAGES_PATH: /app/storage/images
      MAX_MUSIC_SIZE_MB: "10"
      MAX_IMAGE_SIZE_MB: "5"
```

### 4.6 Konvensi Response

**Success:**
```json
// Login
{ "accessToken": "...", "tokenType": "Bearer", "expiresIn": 86400, "user": { "id": 1, "username": "admin", "name": "Admin", "roleCode": "ADMIN", "clientId": null } }

// Register
{ "user": { ... }, "message": "User berhasil dibuat" }

// Public invitation
{ "slug": "...", "eventTypeCode": "...", "persons": [], "sessions": [], "template": null }
```

**Error (HTTP 4xx):**
```json
{ "error": "ERROR_CODE", "message": "Human-readable message", "timestamp": "2026-08-21T10:00:00Z" }
```

**Error code yang harus dipakai:**
`INVALID_CREDENTIALS`, `USER_INACTIVE`, `CLIENT_EXPIRED`, `ACCESS_DENIED`, `NOT_FOUND`, `CONFLICT`, `VALIDATION_ERROR`, `BAD_REQUEST`.

### 4.7 Format Tanggal & Waktu

- Request/response JSON: ISO-8601 (`OffsetDateTime` → `"2026-08-21T10:00:00+07:00"`).
- Tanggal murni (acara, expiry): `LocalDate` → `"2026-08-21"`.
- Session time: `String` (bebas format, sesuai data di schema `VARCHAR(50)`).

---

## 5. Definition of Done (Checklist yang Agent Centang Sendiri)

Agent HARUS mencentang setiap item di bawah sebagai bukti self-verification. Tidak perlu menjalankan aplikasi — cek dari file yang ditulis.

### Entity & Mapping
- [ ] 13 entity dibuat di `entity/` package, semua sesuai schema `Sql/001_init_schema.sql` (nama tabel via `@Table(name = "...")`, nama kolom via `@Column(name = "...")`)
- [ ] `BIGSERIAL` → `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- [ ] `TIMESTAMPTZ` → `OffsetDateTime`, `DATE` → `LocalDate`
- [ ] Mapping `@ManyToOne`/`@OneToMany` konsisten (Client ↔ Invitation, Invitation ↔ sessions/persons/gallery/lovestory/guest, Role ↔ RolePermission, Users ↔ Role/Client)
- [ ] Enum-like kolom (`status`, `role`) pakai `String`, bukan enum Java
- [ ] `Users.passwordHash` pakai `BCrypt` encoding — kolom di entity String, tidak ada anotasi `@JsonIgnore` (di handle di service, bukan entity level)

### Repository
- [ ] 8 repository dibuat di `repository/` package
- [ ] `InvitationRepository.findBySlug()` → `Optional<Invitation>`
- [ ] `UsersRepository.findByUsername()` → `Optional<Users>`
- [ ] `GuestRepository.findByInvitationIdAndIsPublishedTrue()` → `List<Guest>` (untuk guestbook publik)
- [ ] Spring Data JPA naming convention dipakai untuk query methods yang tidak perlu JPQL

### Security
- [ ] `SecurityConfig` mengkonfigurasi: `/api/v1/auth/**` & `/api/v1/public/**` → `permitAll()`; `/api/v1/admin/**` → `hasRole("SUPER_ADMIN")`; `/api/v1/client/**` → `hasRole("CLIENT")`;其余 → `authenticated()`
- [ ] `JwtAuthenticationFilter` membaca header `Authorization: Bearer <token>`, validasi signature + expired, set `SecurityContext`
- [ ] `JwtTokenProvider` generate token dengan claims: `sub` (username), `role`, `clientId`; verify token; extract claims
- [ ] `CustomUserDetailsService` load user dari `UsersRepository.findByUsername()`
- [ ] `AuthService` BCrypt verify password; cek `users.status == 'active'`; cek `client.expires_at >= today`; throw `AuthException` dengan error code yang tepat jika gagal
- [ ] Login: 401 INVALID_CREDENTIALS / 403 USER_INACTIVE / 403 CLIENT_EXPIRED
- [ ] Register: 409 CONFLICT jika username/email duplikat
- [ ] JWT secret dari `${JWT_SECRET}` env var — **tidak ada fallback string hardcode** untuk secret

### Public Controller
- [ ] `GET /api/v1/public/invitation/{slug}` → 200 + data invitation + template + persons + sessions, atau 404
- [ ] `GET /api/v1/public/invitation/{slug}/guestbook` → 200 + list guest yang `isPublished=true`, urut `createdAt DESC`
- [ ] Tidak ada token required untuk kedua endpoint di atas

### Client Isolation
- [ ] Semua endpoint `/api/v1/client/**` pakai token JWT dengan `ROLE_CLIENT`
- [ ] `AuthService` / service layer ambil `clientId` dari JWT, bukan dari request parameter
- [ ] Jika token `clientId` ≠ resource `clientId` → 403 ACCESS_DENIED

### Storage
- [ ] `StorageConfig` baca 3 env var path + 2 env var max size
- [ ] Bean `Path` untuk `themes`, `musics`, `images` dibuat
- [ ] Konfigurasi multipart max-file-size di `application.properties` (berdasarkan env var)

### Dependency & Config
- [ ] `pom.xml`新增 tepat: `spring-boot-starter-security`, `spring-boot-starter-validation`, `jjwt-api`, `jjwt-impl` (runtime), `jjwt-jackson` (runtime) — tidak lebih
- [ ] `application.properties` updated dengan blok Fase 2, tidak menghapus yang sudah ada dari Fase 0
- [ ] `pom.xml` tidak menghapus dependency yang sudah ada dari Fase 0

### Exception Handling
- [ ] `GlobalExceptionHandler` tangani: `AuthException` (401/403), `MethodArgumentNotValidException` (400), `EntityNotFoundException`/`HttpMessageNotReadableException` (400), generic `Exception` (500)
- [ ] Semua error response pakai format `{ "error": "...", "message": "...", "timestamp": "..." }`

### Cross-cutting
- [ ] Folder `Sql/` TIDAK disentuh
- [ ] Folder `Frontend/`, `Docs/`, `Database/` TIDAK disentuh
- [ ] **Tidak ada test code** baru
- [ ] **Tidak ada TODO/FIXME/placeholder kosong** yang belum diisi
- [ ] Semua credential/secret lewat env var — tidak ada string hardcode di kode
- [ ] File `OnlineApplication.java` tidak diubah

---

## 6. Batasan Tegas (Jangan Dilanggar)

1. **JANGAN menjalankan** `docker`, `docker compose`, `mvn`, `./mvnw`, `npm`, atau perintah build/run/test apa pun.

2. **JANGAN menyentuh folder `Sql/`** — seluruh isinya. Kalau merasa butuh perubahan schema, tulis sebagai catatan di akhir jawaban.

3. **JANGAN bikin controller/service/repository/entity untuk fitur bisnis** berikut di fase ini:
   - RSVP (upsert Guest)
   - Guestbook submit
   - Template CRUD (Super Admin)
   - Music CRUD (Super Admin)
   - System parameter CRUD
   - Invitation create/update (client)
   - Gallery, love story, invitation session CRUD
   - User management (Super Admin CRUD user)
   Semua itu mulai di Fase 3+.

4. **JANGAN hardcode JWT secret** di kode. Harus dari `${JWT_SECRET}` env var. Jika env var kosong, aplikasi boleh throw exception saat startup (early fail) — tapi secret TIDAK boleh ada string literal di kode.

5. **JANGAN pakai `@JsonIgnore` di entity untuk kolom `passwordHash`** — itu di-handle di service layer (entity tetap punya kolom tersebut).

6. **JANGAN pakai Lombok** (`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`, dll.) — sesuai CLAUDE.md. Tulis getter/setter/manual constructor kalau perlu.

7. **JANGAN bikin endpoint upload file** di fase ini. StorageConfig hanya disiapkan. Upload endpoint mulai di Fase 3.

8. **JANGAN mengubah Spring Boot version** di `pom.xml` (saat ini `4.1.0`).

9. **JANGAN tambah CI/CD, test code, atau file dokumentasi** baru.

10. **JANGAN akses `Frontend/`** untuk hal apa pun.

11. **JIKA AMBIGU**, pilih asumsi paling masuk akal, tulis di ringkasan akhir, lanjut jalan. Jangan berhenti untuk bertanya.

---

## 7. Catatan Referensi

**WAJIB** baca sebelum mulai coding (hanya bagian yang relevan):

- `Docs/CLAUDE.md` — baca seluruhnya. Aturan di CLAUDE.md **menang** kalau konflik dengan issue ini.
- `Sql/001_init_schema.sql` — baca **seluruhnya**. Ini sumber kebenaran untuk entity mapping. Jika ada detail yang tidak jelas, tulis asumsi di catatan.
- `Sql/002_seed_master_data.sql` — baca untuk memahami data master awal (role: ADMIN/STAFF/USER).
- `Docs/ERD-Undangan-Online.md` — baca section 1-4, 8-12 (tabel utama + relasi + aturannya). Tidak perlu baca view.
- `Docs/PRD-Undangan-Online.md` — baca section 4 (Role & Hak Akses) dan 7 (Non-Functional Requirements). Jangan implementasikan fitur bisnis dari section 5/6.

**File yang JANGAN dibaca untuk fase ini:**
- `Docs/web-dashboard-html.html` — itu untuk fase frontend, tidak relevan di fase backend.
- `Docs/Planning/` — internal note, tidak perlu.
- `Sql/003_seed_dummy_data.sql` — data testing, tidak relevan untuk core entity.

---

## 8. Catatan untuk Manusia (diisi saat selesai — WAJIB)

Agent HARUS menutup jawaban dengan ringkasan dalam format ini:

```markdown
## Ringkasan
- File dibuat: [daftar path file baru]
- File diubah: [daftar path file yang dimodifikasi]
- Asumsi yang diambil: [daftar, kosongkan jika tidak ada]
- Kebutuhan schema baru (jika ada): [daftar, atau "Tidak ada"]
- Yang perlu ditambahkan manusia ke docker-compose.yml root:
  [paste blok environment dari bagian 4.5]
- Catatan lain untuk manusia: [hal penting yang perlu diketahui sebelum test manual]
```

---

## 9. Checklist Self-Verification Agent (copy-paste & centang)

```markdown
## ✅ Self-Verification Checklist

### Entity & Mapping
- [ ] 13 entity dibuat, semua @Table/@Column sesuai schema
- [ ] BIGSERIAL → GenerationType.IDENTITY
- [ ] TIMESTAMPTZ → OffsetDateTime, DATE → LocalDate
- [ ] @ManyToOne/@OneToMany mapping konsisten
- [ ] Enum kolom pakai String

### Repository
- [ ] 8 repository dibuat
- [ ] findBySlug, findByUsername, findByInvitationIdAndIsPublishedTrue ada

### Security
- [ ] SecurityConfig: permitAll / hasRole sesuai prefix
- [ ] JwtAuthenticationFilter baca Bearer token, set SecurityContext
- [ ] JwtTokenProvider generate & verify JWT
- [ ] CustomUserDetailsService load dari DB
- [ ] AuthService: BCrypt verify, cek status aktif, cek client expiry
- [ ] Login: 401/403 error code benar
- [ ] Register: 409 conflict untuk duplikat
- [ ] JWT_SECRET dari env var, tidak ada fallback hardcode

### Public Controller
- [ ] GET /api/v1/public/invitation/{slug} → 200/404
- [ ] GET /api/v1/public/invitation/{slug}/guestbook → 200 + published guests

### Client Isolation
- [ ] /api/v1/client/** → hasRole(CLIENT)
- [ ] clientId dari JWT, bukan dari request param
- [ ] Akses lintas client → 403 ACCESS_DENIED

### Storage
- [ ] StorageConfig: 3 Path bean + 2 max-size config
- [ ] Multipart max-file-size di application.properties

### Dependency & Config
- [ ] pom.xml: +security +validation +jjwt (5 artifact)
- [ ] application.properties: +blok Fase 2, tidak hapus Fase 0
- [ ] Tidak ada dependency baru di luar tabel

### Exception & Response
- [ ] GlobalExceptionHandler: AuthException, ValidationException, generic
- [ ] Error format: { "error", "message", "timestamp" }

### Cross-cutting
- [ ] Sql/ tidak disentuh
- [ ] Tidak ada test code baru
- [ ] Tidak ada TODO/FIXME
- [ ] Tidak ada hardcode credential
- [ ] OnlineApplication.java tidak diubah
```

---

*Issue ini untuk Fase 2 saja. Setelah selesai & lolos verifikasi checklist, manusia akan test manual. Baru kemudian lanjut ke Fase 3 (fitur bisnis).*
