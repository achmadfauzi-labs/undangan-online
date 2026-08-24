# Issue A — Retrofit Fase 2 (Backend Core & Auth): Fondasi Standar CLAUDE.md

> **Audience:** AI agent junior (model kecil). Agent **SELALU** menjalankan `docker`, `docker compose`, `mvn`, `npm`, atau perintah build/run/test apa pun ketika sesudah menyelesaikan issue.
>
> **Posisi dalam rantai retrofit:** Ini issue **PERTAMA** yang harus dikerjakan. Setelah selesai & lolos verifikasi, baru lanjut ke Issue B-1, B-2, dst (satu per satu).
>
> **Acuan utama (WAJIB dibaca ulang, jangan dari memori):**
> - `Docs/CLAUDE.md` — bagian **#6** (Clean Code & OOP), **#7** (Format Response API), **#8** (Logging)
> - `Docs/PRD-Undangan-Online.md` — bagian 4 (Role & Hak Akses)
> - `Backend/src/main/java/com/undangan/online/OnlineApplication.java` — JANGAN disentuh

---

## 1. Ringkasan & Alasan Retrofit

Fase 2 sudah jalan (login, register, JWT, security config), tapi **belum konsisten** dengan standar terbaru di `CLAUDE.md`:

1. **Response format masih lawas** — pakai `ErrorResponse` lama (`error`, `message`, `timestamp`) dan success response plain (bukan dibungkus `ApiResponse<T>`). Seharusnya semua response sukses & gagal dibungkus wrapper generic.
2. **Logging belum konsisten** — beberapa service pakai SLF4J, beberapa tidak ada logging sama sekali. Auth/login/operasional security belum diaudit.
3. **Service belum punya interface** — `AuthService` langsung class, tidak ada `AuthService` interface + `AuthServiceImpl`. Ini menghambat mockability dan konsistensi OOP.
4. **Role di `SecurityConfig` pakai nama salah** — saat ini `hasRole("ADMIN")` dan `hasRole("USER")`, padahal di CLAUDE.md & PRD namanya `SUPER_ADMIN` dan `CLIENT`. Nama ini harus konsisten dengan `role.code` di schema (`Sql/002_seed_master_data.sql`).

**Tujuan issue ini:** Membangun fondasi standar (response wrapper, global handler, logging konsisten, role name benar, service interface) yang akan dipakai SEMUA issue retrofit berikutnya (Issue B-*). Setelah issue ini selesai, agent B-* tinggal menerapkannya ke modul masing-masing.

**TIDAK termasuk issue ini:** Retrofit modul bisnis (Client Management, Invitation, dll) — itu Issue B-1, B-2, dst.

---

## 2. Scope — File/Folder yang Boleh Disentuh

Agent HANYA boleh membuat/mengedit file di bawah. Di luar daftar → JANGAN disentuh.

### File yang BOLEH diedit (existing):
- `Backend/pom.xml` — tambahkan dependency baru saja (jangan hapus yang sudah ada)
- `Backend/src/main/resources/application.properties` — boleh tambah baris konfigurasi
- `Backend/src/main/java/com/undangan/online/config/SecurityConfig.java`
- `Backend/src/main/java/com/undangan/online/config/JwtConfig.java`
- `Backend/src/main/java/com/undangan/online/security/JwtTokenProvider.java`
- `Backend/src/main/java/com/undangan/online/security/JwtAuthenticationFilter.java`
- `Backend/src/main/java/com/undangan/online/security/CustomUserDetailsService.java`
- `Backend/src/main/java/com/undangan/online/service/AuthService.java` (existing) — akan direfaktor jadi interface + impl
- `Backend/src/main/java/com/undangan/online/service/ClientInitService.java`
- `Backend/src/main/java/com/undangan/online/controller/AuthController.java` (existing) — akan dibungkus ApiResponse
- `Backend/src/main/java/com/undangan/online/exception/AuthException.java`
- `Backend/src/main/java/com/undangan/online/exception/GlobalExceptionHandler.java`

### File BARU yang WAJIB dibuat:
- `Backend/src/main/java/com/undangan/online/dto/ApiResponse.java` — generic response wrapper
- `Backend/src/main/java/com/undangan/online/service/AuthServiceImpl.java` — implementasi (pindahkan logic dari `AuthService` class)
- (Interface `AuthService` tetap di `service/AuthService.java` yang akan direfaktor dari class jadi interface)

### DILARANG keras:
- Folder `Sql/` — seluruh isinya
- Folder `Docs/`, `Frontend/`, `Database/`
- File `Backend/src/main/java/com/undangan/online/OnlineApplication.java`
- File di package `entity/`, `repository/`, `dto/` (selain `ApiResponse.java` baru)
- Semua controller lain (`ClientController`, `UserController`, dll) — itu Issue B-*
- Semua service lain (`ClientService`, `UserService`, dll) — itu Issue B-*

---

## 3. Checklist Audit (Jalankan Satu-satu Sebelum Coding)

Centang hasil audit di awal, baru mulai coding.

### 3.1 Audit Response Format
- [ ] Buka `GlobalExceptionHandler.java` — handler return `ErrorResponse` (field: `error`, `message`, `timestamp`). Apakah ini sesuai CLAUDE.md bagian #7? (Standar baru: `ApiResponse<T>` dengan field `success`, `message`, `data`, opsional `errors`.)
- [ ] Buka `AuthController.java` — return `ResponseEntity<LoginResponse>` langsung, tanpa dibungkus `ApiResponse`. TIDAK sesuai standar.
- [ ] Cek semua error code yang dipakai di handler: apakah konsisten dengan PRD (contoh: `INVALID_CREDENTIALS`, `USER_INACTIVE`, `ACCESS_DENIED`, dll)?

### 3.2 Audit Security & Role
- [ ] Buka `SecurityConfig.java` — saat ini `hasRole("ADMIN")` & `hasRole("USER")`. Cek `Sql/002_seed_master_data.sql` dan `entity/Role.java` untuk nama role yang sebenarnya.
- [ ] Cek `AuthService.login()` — bikin role string `"ROLE_" + user.getRoleCode()`. Apakah `user.getRoleCode()` dari DB (`ADMIN`/`STAFF`/`USER`) atau nama lain? Apakah nama ini cocok dengan konstanta di `SecurityConfig`?

### 3.3 Audit Logging
- [ ] Buka `AuthService.java` — apakah ada `Logger log = LoggerFactory.getLogger(...)`? Apakah ada `log.info(...)` untuk event login berhasil, `log.warn(...)` untuk login gagal, `log.error(...)` untuk exception?
- [ ] Buka `JwtAuthenticationFilter` — apakah ada logging untuk token invalid / expired?
- [ ] Buka `JwtTokenProvider` — apakah ada logging untuk generate token?

### 3.4 Audit Clean Code (OOP & Layered)
- [ ] Apakah `AuthService` punya interface? (Kalau belum class langsung, perlu direfaktor jadi interface + Impl sesuai CLAUDE.md bagian #6.)
- [ ] Apakah `AuthController` ada business logic, atau murni delegasi ke service? (Harusnya murni delegasi.)
- [ ] Apakah ada method panjang di `AuthService` yang bisa dipecah jadi private method?

### 3.5 Audit Dependency
- [ ] Cek `Backend/pom.xml` — versi Spring Boot saat ini `4.1.0`. Cek apakah ada update patch (`4.1.1`) lewat context7. Versi JWT (`jjwt 0.12.6`) — cek apakah masih latest stable.
- [ ] Apakah ada dependency yang tidak terpakai (misal: `spring-boot-starter-actuator` dipakai atau tidak)?

---

## 4. Requirement Teknis Perbaikan

Acuan detail: **`Docs/CLAUDE.md` bagian #6, #7, #8** — JANGAN tulis ulang di sini, cukup rujuk.

### 4.1 Bikin `ApiResponse<T>` Generic Wrapper (Fondasi)
- Lokasi: `Backend/src/main/java/com/undangan/online/dto/ApiResponse.java`
- Field sesuai CLAUDE.md #7: `boolean success`, `String message`, `T data`, opsional `List<String> errors`.
- Sediakan **static factory method**:
  - `ApiResponse.ok(T data)` → `success=true, message="OK", data=data`
  - `ApiResponse.ok(String message, T data)` → custom message
  - `ApiResponse.created(T data)` → `success=true, message="Berhasil dibuat", data=data`
  - `ApiResponse.error(String message, List<String> errors)` → `success=false`
  - `ApiResponse.error(String message)` → `success=false, errors=null`
- Pakai `@JsonInclude(JsonInclude.Include.NON_NULL)` supaya field kosong tidak ikut di-serialize.

### 4.2 Refaktor `GlobalExceptionHandler` Pakai `ApiResponse`
- Semua handler di file ini return `ResponseEntity<ErrorResponse>` → ganti jadi `ResponseEntity<ApiResponse<?>>`.
- Mapping status code TETAP sama (401, 403, 400, 500) — jangan ubah status.
- `data` di `ApiResponse` untuk error = `null`.
- `errors` di `ApiResponse` untuk `MethodArgumentNotValidException` = list pesan validasi.
- Tambah handler untuk `AccessDeniedException` (Spring Security) → 403 `ApiResponse.error("Akses ditolak")`.
- Tambah handler untuk `AuthenticationException` / `BadCredentialsException` → 401.
- Logging: `log.error("Unhandled exception", ex)` tetap dipertahankan untuk generic handler.

### 4.3 Refaktor `SecurityConfig` (Role Name)
- **Hard-code role name yang benar** dari `Sql/002_seed_master_data.sql`. Berdasarkan role yang ada (cek file), ganti:
  - `hasRole("ADMIN")` → cek apakah di seed pakai `SUPER_ADMIN` atau `ADMIN` — pakai yang sesuai dengan data. Kalau di seed `ADMIN` dan CLAUDE.md juga menyebut `SUPER_ADMIN`, **pakai yang sesuai dengan `Role.code` di DB** (cek `entity/Role.java` dan `Sql/002_seed_master_data.sql`).
  - `hasRole("USER")` → cek apakah `CLIENT` atau `USER` — pakai yang sesuai dengan data di DB.
- **PENTING:** Jangan ubah data di `Sql/`. Hanya sesuaikan konstanta di Java dengan data existing.
- Kalau ada perbedaan antara nama di DB vs CLAUDE.md, **catat di ringkasan akhir** sebagai catatan untuk manusia (bisa jadi DB perlu diupdate, atau CLAUDE.md perlu klarifikasi).

### 4.4 Bikin Interface `AuthService` + `AuthServiceImpl`
- `Backend/src/main/java/com/undangan/online/service/AuthService.java` → ubah jadi **interface** dengan method:
  ```java
  LoginResponse login(String username, String password);
  UserDto registerClient(RegisterClientRequest request);
  ```
- `Backend/src/main/java/com/undangan/online/service/AuthServiceImpl.java` → class baru, implementasi interface. Pindahkan SEMUA logic dari class `AuthService` lama ke sini. Tambah annotation `@Service`.
- Constructor injection: `UsersRepository`, `ClientRepository`, `PasswordEncoder`, `JwtConfig`, `JwtTokenProvider`.
- Jangan hapus file lama sampai `AuthServiceImpl` sudah jadi dan kompilasi aman secara logika (agent tidak compile, tapi cek manual: semua method yang dipanggil controller harus ada di interface).

### 4.5 Tambah Logging SLF4J (CLAUDE.md #8)
- **`AuthServiceImpl.login()`**:
  - Sukses: `log.info("User {} logged in successfully (role={})", username, user.getRoleCode());`
  - Gagal (user tidak ditemukan): `log.warn("Login failed: username '{}' not found", username);`
  - Gagal (password salah): `log.warn("Login failed: invalid password for username '{}'", username);`
  - User inactive: `log.warn("Login blocked: user '{}' is inactive", username);`
  - Client expired: `log.warn("Login blocked: client {} expired for user '{}'", clientId, username);`
- **`AuthServiceImpl.registerClient()`**:
  - Sukses: `log.info("User {} registered as CLIENT for clientId={} by admin action", newUsername, clientId);`
  - Conflict: `log.warn("Register failed: username/email already exists (username='{}')", request.getUsername());`
- **`JwtAuthenticationFilter`**: log `WARN` untuk token invalid / expired (jangan log tokennya, hanya metadata: remote address, path).
- **`JwtTokenProvider.generateToken()`**: log `INFO` setiap generate (untuk audit trail, tanpa log token value).
- **JANGAN log** password, token JWT, email lengkap, atau data sensitif lain.

### 4.6 Refaktor `AuthController` Pakai `ApiResponse`
- `POST /api/v1/auth/login` → return `ResponseEntity<ApiResponse<LoginResponse>>` dengan `ApiResponse.ok("Login berhasil", response)`.
- `POST /api/v1/auth/register` → return `ResponseEntity<ApiResponse<UserDto>>` dengan status 201 + `ApiResponse.created(user)`.
- Controller TETAP hanya delegasi — tidak ada business logic baru.
- Inject interface `AuthService` (bukan concrete class) di constructor.

### 4.7 Pertahankan `ErrorResponse` (Jangan Hapus)
- File `Backend/src/main/java/com/undangan/online/dto/ErrorResponse.java` lama **jangan dihapus** — mungkin dipakai di tempat lain (Issue B-*). Biarkan deprecated atau unused dulu. Agent B-* akan bersihkan kemudian.

### 4.8 Dependency Check via context7
- Cek versi `Spring Boot` terbaru: apakah ada patch `4.1.1` atau lebih baru? Kalau ada, **catat di ringkasan akhir** sebagai rekomendasi (TIDAK eksekusi update versi — di luar scope retrofit, beda dengan "struktur & konsistensi").
- Cek versi `io.jsonwebtoken:jjwt` — apakah `0.12.6` masih latest? Kalau ada lebih baru, catat juga.
- **JANGAN ubah versi** tanpa izin eksplisit manusia — tugas ini murni struktur.

---

## 5. Definition of Done

### Standar Berlaku
- [ ] `ApiResponse<T>` dibuat dengan static factory method sesuai CLAUDE.md #7
- [ ] `GlobalExceptionHandler` return `ApiResponse<?>` di semua handler, status HTTP TETAP sama
- [ ] Tambah handler untuk `AccessDeniedException` (403) dan `BadCredentialsException` (401)
- [ ] `SecurityConfig` pakai role name yang konsisten dengan DB (cek `Sql/002_seed_master_data.sql` + `entity/Role.java`)
- [ ] `AuthService` jadi interface, logic pindah ke `AuthServiceImpl` dengan annotation `@Service`
- [ ] `AuthController` inject `AuthService` interface (bukan concrete), return `ApiResponse<>` wrapper
- [ ] SLF4J logging ada di `AuthServiceImpl` (login success/fail, register success/conflict), `JwtAuthenticationFilter` (invalid token), `JwtTokenProvider` (generate token)
- [ ] **TIDAK ada** `System.out.println` / `printStackTrace` di file yang disentuh
- [ ] **TIDAK ada** field `Entity` JPA di-expose langsung via controller return type
- [ ] **TIDAK ada** hardcode credential / secret / path

### Cross-cutting
- [ ] Folder `Sql/` **TIDAK** disentuh
- [ ] File `OnlineApplication.java` **TIDAK** disentuh
- [ ] **TIDAK** menjalankan `mvn`, `docker`, `npm`, atau build tool apa pun
- [ ] **TIDAK** menambah test code
- [ ] **TIDAK** menambah fitur / endpoint baru
- [ ] **TIDAK** mengubah business behavior (logic login/register TETAP sama, hanya struktur & format response yang berubah)

### Catatan yang WAJIB ada di ringkasan akhir
- [ ] Versi dependency yang dicek via context7 + rekomendasi update (kalau ada)
- [ ] Perbedaan role name (DB vs CLAUDE.md) — kalau ditemukan
- [ ] Asumsi yang diambil (misal: pilih role mana yang dipakai di `hasRole()`)

---

## 6. Batasan Tegas (Jangan Dilanggar)

1. **JANGAN menjalankan** `docker`, `docker compose`, `mvn`, `./mvnw`, `npm`, atau perintah build/run/test apa pun. Agent TIDAK test.
2. **JANGAN mengubah, menghapus, atau menambah file di folder `Sql/`.**
3. **JANGAN menyentuh** controller/service/file di luar Scope bagian #2. Kalau perlu ubah file lain, **catat** di akhir, minta konfirmasi.
4. **JANGAN pakai** `ddl-auto=update` atau `create` di konfigurasi JPA. Schema sudah ada di `Sql/`.
5. **JANGAN menambah** library/dependency baru di luar yang sudah ada di `pom.xml`, kecuali SLF4J (sudah transitive dari Spring Boot starter — tidak perlu declare).
6. **JANGAN menambah endpoint baru.** Kalau merasa butuh endpoint baru untuk testing, **catat** di akhir, jangan buat.
7. **JANGAN mengubah business behavior** yang sudah ada. Logic login/register/expiry check TETAP sama. Hanya struktur (response wrapper, logging, interface) yang berubah.
8. **JANGAN log data sensitif** (password, JWT token value, email lengkap) — mask atau skip.
9. **JANGAN pakai Lombok** (`@Data`, `@Builder`, dll) — sesuai CLAUDE.md.
10. **JANGAN hapus file `ErrorResponse.java`** — biarkan, akan dibersihkan di issue B-* (jika sudah tidak dipakai).
11. **Kalau ada bug** di luar scope retrofit (misal: logic validation yang salah, query tidak efisien, N+1 problem) → **CATAT** di akhir jawaban, JANGAN perbaiki di issue ini.
12. **Kalau AMBIGU** → buat asumsi paling masuk akal berdasarkan PRD/CLAUDE.md, tulis asumsi di ringkasan akhir, lanjut. Jangan berhenti untuk bertanya.

---

## 7. Catatan untuk Manusia (diisi agent di akhir — WAJIB)

Agent HARUS menutup jawaban dengan format:

```markdown
## Ringkasan
- File dibuat: [daftar path]
- File diubah: [daftar path]
- Asumsi yang diambil:
  - [contoh: "Role name di hasRole() menggunakan 'ADMIN' sesuai Sql/002_seed_master_data.sql — bukan 'SUPER_ADMIN' seperti tertulis di CLAUDE.md. Perlu klarifikasi manusia apakah CLAUDE.md perlu diupdate atau Sql/ perlu disesuaikan."]
- Perbedaan / discrepancy ditemukan (perlu dikonfirmasi manusia):
  - [contoh: "CLAUDE.md #10 menyebut role SUPER_ADMIN, tapi Sql/002_seed_master_data.sql pakai ADMIN. Dipakai yang sesuai DB."]
- Versi dependency dicek via context7:
  - Spring Boot: 4.1.0 (saat ini) vs [versi terbaru] — [rekomendasi / tidak direkomendasikan update di issue ini]
  - jjwt: 0.12.6 (saat ini) vs [versi terbaru] — [status]
- Bug / catatan di luar scope yang ditemukan (TIDAK diperbaiki):
  - [contoh: "AuthService.login() tidak handle case user.clientId menunjuk ke client yang sudah dihapus (hard delete vs soft delete belum jelas)"]
- Definition of Done dari bagian #5 yang terpenuhi: [checklist dicentang]
```

---

## 8. Urutan Pengerjaan (Disarankan)

1. **Bikin `ApiResponse<T>` wrapper** (fondasi).
2. **Refaktor `GlobalExceptionHandler`** pakai `ApiResponse` + tambah handler `AccessDeniedException` & `BadCredentialsException`.
3. **Bikin interface `AuthService`** (rename class existing jadi `AuthServiceImpl` setelah logic dipindah) — atau cara lain: bikin `AuthService` baru sebagai interface, pindahkan logic class lama ke `AuthServiceImpl` baru, hapus logic dari class lama. **JANGAN hapus class lama sebelum `AuthServiceImpl` selesai.**
4. **Update `AuthController`** inject interface + return `ApiResponse`.
5. **Fix `SecurityConfig`** role name.
6. **Tambah logging SLF4J** di `AuthServiceImpl`, `JwtAuthenticationFilter`, `JwtTokenProvider`.
7. **Cek dependency via context7**, catat di ringkasan.
8. **Self-verify** dengan Definition of Done.

---

*Setelah issue ini selesai & lolos verifikasi manusia, lanjut ke Issue B-1 (Retrofit Client Management).*
