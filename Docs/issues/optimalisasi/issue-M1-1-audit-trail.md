# Issue M-1.1 — Database Audit Trail

> **Audience:** AI agent junior (model kecil). Agent **DILARANG** menjalankan `docker`, `docker compose`, `mvn`, `npm`, atau perintah build/run/test apa pun. Tugas murni menulis/mengedit kode. Manusia yang test manual.
>
> **Prasyarat:** Issue A + semua issue B-1 s/d B-8 selesai. Kerjakan ini SEBELUM M-1.2/M-1.3/M-1.4 (issue ini membuat 2 utility class yang dipakai ulang oleh issue lain).

---

## 1. Judul & Ringkasan

Menambahkan tabel `audit_log` untuk mencatat aksi penting (create/update/delete, login sukses/gagal, upload/delete file) beserta IP address, User-Agent, dan snapshot data (dengan masking field sensitif).

## 2. Scope

### File BARU yang WAJIB dibuat:
- `Backend/src/main/java/com/undangan/online/entity/AuditLog.java`
- `Backend/src/main/java/com/undangan/online/repository/AuditLogRepository.java`
- `Backend/src/main/java/com/undangan/online/service/AuditLogService.java`
- `Backend/src/main/java/com/undangan/online/service/impl/AuditLogServiceImpl.java`
- `Backend/src/main/java/com/undangan/online/controller/AuditLogController.java`
- `Backend/src/main/java/com/undangan/online/dto/AuditLogDto.java`
- `Backend/src/main/java/com/undangan/online/util/IpAddressUtil.java` — dipakai ulang oleh M-1.2 & M-1.4, JANGAN dibuat ulang di issue lain.
- `Backend/src/main/java/com/undangan/online/util/SensitiveDataMasker.java` — dipakai ulang oleh M-1.4 kalau perlu.

### File yang BOLEH diedit (existing):
- `Backend/src/main/java/com/undangan/online/service/impl/AuthServiceImpl.java` — tambahkan pemanggilan audit log untuk login sukses/gagal.

### DILARANG keras:
- Folder `Sql/` (table baru butuh SQL manual — lihat section 4).
- Folder `Docs/`, `Frontend/`, `Database/`, `TemaUndangan/`, `Musics/`, `Images/`.
- `pom.xml` (tidak boleh tambah dependency baru).
- Lombok.
- Entity yang sudah ada (`Users.java`, `Client.java`, dll.) — hanya boleh dipanggil/dibaca, tidak diubah.

---

## 3. Requirement Fungsional

### Entity `AuditLog`
- `id` — BIGINT PK
- `userId` — BIGINT (FK ke `users.id`, nullable — untuk aksi anonim seperti public RSVP)
- `username` — VARCHAR(50) (snapshot)
- `role` — VARCHAR(30) (snapshot, ex: "USER", "ADMIN")
- `action` — VARCHAR(30) NOT NULL, CHECK IN (`CREATE`,`UPDATE`,`DELETE`,`LOGIN`,`LOGIN_FAILED`,`READ`,`UPLOAD_FILE`,`DELETE_FILE`)
- `module` — VARCHAR(50) (ex: "invitation", "guest", "music", "user", "auth")
- `entityId` — BIGINT (nullable)
- `oldValue` — TEXT (JSON snapshot sebelum perubahan, sudah di-mask)
- `newValue` — TEXT (JSON snapshot setelah perubahan, sudah di-mask)
- `ipAddress` — VARCHAR(45)
- `userAgent` — VARCHAR(512)
- `createdAt` — TIMESTAMPTZ NOT NULL DEFAULT NOW() (immutable, tidak ada `updatedAt`)

### Utility `IpAddressUtil`
Method statis `extractClientIp(HttpServletRequest request)`:
- Cek header `X-Forwarded-For` dulu (bisa berisi banyak IP dipisah koma jika lewat beberapa proxy) — ambil IP **pertama** dari daftar tersebut (itu IP client asli).
- Kalau header itu kosong/null, fallback ke `request.getRemoteAddr()`.
- **Alasan:** di dalam Docker network atau di belakang reverse proxy, `getRemoteAddr()` bisa mengembalikan IP internal gateway, bukan IP client sebenarnya.

### Utility `SensitiveDataMasker`
Method statis `maskAndSerialize(Object entityOrMap, ObjectMapper mapper)`:
- Terima object/entity, convert ke `Map<String, Object>` dulu (via `mapper.convertValue(obj, Map.class)`) — **JANGAN** serialize ke String dulu baru mask pakai regex (rawan bocor kalau ada nested field atau format berubah).
- Untuk key bernama (case-insensitive) `password`, `passwordHash`, `token`, `refreshToken`, `email` — replace value dengan `"***masked***"`.
- Baru setelah itu serialize Map yang sudah dimask ke JSON string via `mapper.writeValueAsString(maskedMap)`.
- Return hasil String tersebut, dipakai untuk isi `oldValue`/`newValue`.

### Service `AuditLogService` (interface + impl)
- `logAction(userId, username, role, action, module, entityId, oldValue, newValue, ipAddress, userAgent)` — insert ke DB via `AuditLogRepository`.
  - **WAJIB dibungkus try-catch di dalam method ini sendiri.** Kalau insert audit log gagal (misal DB lag/error), method ini **TIDAK BOLEH throw exception ke pemanggil** — cukup `log.warn("Gagal mencatat audit log: {}", e.getMessage())` dan return tanpa error. Audit logging tidak boleh membuat operasi bisnis utama (login/create/delete) ikut gagal.
- `listAll(userId, module, action, startDate, endDate, page, size)` — untuk admin view, pakai `Pageable`, default `size=20` kalau tidak dikirim.
- `getDetail(id)` — lihat satu log, throw `AuthException("NOT_FOUND", "Audit log tidak ditemukan", 404)` kalau tidak ada.

### Controller `AuditLogController`
- `GET /api/v1/admin/audit-logs` — query param: `userId`, `module`, `action`, `startDate`, `endDate`, `page`, `size`. Return `ApiResponse<PagedResult<AuditLogDto>>`. Akses: hanya role SUPER_ADMIN.
- `GET /api/v1/admin/audit-logs/{id}` — return `ApiResponse<AuditLogDto>`. Akses: hanya role SUPER_ADMIN.

### Scope pencatatan untuk MVP (jangan lebih dari ini di issue ini)
- `AuthController`/`AuthServiceImpl` — login sukses → `action=LOGIN`, login gagal → `action=LOGIN_FAILED`. IP & User-Agent diambil via `IpAddressUtil` dan `request.getHeader("User-Agent")`.
- **Selain login, JANGAN tambahkan pemanggilan audit log ke controller/service lain di issue ini** — itu di luar scope MVP, dicatat sebagai TODO untuk fase berikutnya (full auto-capture via `@Aspect` bersifat opsional dan TIDAK dikerjakan sekarang).

---

## 4. Requirement Teknis

### SQL — HARUS ditulis, TIDAK dieksekusi oleh agent
Manusia yang jalankan manual setelah agent selesai.

```sql
CREATE TABLE audit_log (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
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

### Entity JPA mapping
- `@Table(name="audit_log")`, `entityId` → `@Column(name="entity_id")`, `oldValue` → `@Column(name="old_value", columnDefinition="TEXT")`, `newValue` → `@Column(name="new_value", columnDefinition="TEXT")`, dst (snake_case di DB, camelCase di Java).
- Field ID pakai `GenerationType.IDENTITY` (sesuai `BIGSERIAL`, tanpa Lombok — getter/setter/constructor manual).

### Package
- Entity → `com.undangan.online.entity/`
- Repository → `com.undangan.online.repository/` (extends `JpaRepository<AuditLog, Long>`, method query pakai `Pageable`)
- Service → interface `com.undangan.online.service/` + impl `com.undangan.online.service.impl/`
- Controller → `com.undangan.online.controller/`
- DTO → `com.undangan.online.dto/`
- Utility → `com.undangan.online.util/` (paket baru, boleh dibuat)

### Konvensi
- Constructor injection, bukan field injection.
- SLF4J Logger, JANGAN `System.out.println`.
- `ApiResponse<T>` wrapper untuk semua response controller.
- JANGAN pakai Lombok.

### TODO-M1 markers (wajib ditulis agent)
- Di entity `AuditLog` — `// TODO-M1: table 'audit_log' belum ada di DB — butuh SQL di Sql/`.

---

## 5. Definition of Done
- [ ] Entity `AuditLog` — field lengkap, `@Table(name="audit_log")`, TODO-M1 marker ada.
- [ ] `IpAddressUtil.extractClientIp()` — cek `X-Forwarded-For` dulu, fallback `getRemoteAddr()`.
- [ ] `SensitiveDataMasker.maskAndSerialize()` — mask di level Map SEBELUM serialize, bukan regex ke JSON string.
- [ ] `AuditLogRepository` — query by userId+action+module+date range dengan `Pageable`.
- [ ] `AuditLogService`/`AuditLogServiceImpl` — `logAction` dibungkus try-catch internal (tidak melempar exception ke pemanggil), `listAll`, `getDetail`.
- [ ] `AuditLogController` — 2 endpoint, akses SUPER_ADMIN only, return `ApiResponse`.
- [ ] `AuthServiceImpl.login()` — panggil `logAction` untuk LOGIN & LOGIN_FAILED, pakai `IpAddressUtil`.
- [ ] Tidak ada pemanggilan audit log di controller/service lain selain AuthServiceImpl (di luar scope MVP ini).
- [ ] `pom.xml`, `Sql/` tidak disentuh.

---

## 6. Batasan Tegas
1. JANGAN jalankan build tool/docker.
2. JANGAN ubah file di `Sql/`, `Frontend/`, `Docs/`, `Database/`.
3. JANGAN pakai Lombok atau tambah dependency baru.
4. JANGAN ubah entity yang sudah ada.
5. JANGAN tambah pemanggilan audit log di luar `AuthServiceImpl` (scope MVP saja).
6. `logAction` TIDAK BOLEH throw exception ke pemanggil — wajib try-catch internal.

---

## 7. Catatan Referensi
- `Docs/CLAUDE.md` bagian 6-8 (Clean Code, Response Format, Logging).
- `Backend/src/main/java/com/undangan/online/dto/ApiResponse.java`
- `Backend/src/main/java/com/undangan/online/service/impl/AuthServiceImpl.java` — tempat menambahkan pemanggilan `logAction`.
- `Sql/001_init_schema.sql` — konvensi snake_case, `BIGSERIAL`.
