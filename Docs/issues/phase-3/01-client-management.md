# Fase 3.1 — Client Management

## 1. Judul & Ringkasan

**Judul:** Backend — Modul Client Management (CRUD, Status & Expiry)

**Ringkasan:** Buat modul backend lengkap untuk Super Admin mengelola akun client: create, read, update, delete, serta kontrol status aktif/nonaktif dan tanggal expiry. Modul ini dipakai di Dashboard Super Admin (Web 1).

---

## 2. Scope — File yang BOLEH Disentuh

Agent HANYA boleh membuat/mengedit file dalam folder ini:

```
Backend/src/main/java/com/undangan/online/
├── controller/
│   └── ClientController.java          # BARU
├── service/
│   └── ClientService.java             # BARU
├── dto/
│   ├── ClientDto.java                # BARU
│   ├── CreateClientRequest.java       # BARU
│   └── UpdateClientRequest.java       # BARU
```

**BOLEH dibaca (referensi, tidak boleh diubah):**
- `entity/Client.java` — entity sudah ada, JANGAN ubah
- `repository/ClientRepository.java` — repository sudah ada, JANGAN ubah
- `config/SecurityConfig.java` — untuk memahami prefix endpoint
- `exception/GlobalExceptionHandler.java` — untuk memahami pola error response
- `dto/ErrorResponse.java` — untuk memahami format error
- `exception/AuthException.java` — untuk pola exception
- `controller/AuthController.java` — untuk memahami pola controller
- `service/AuthService.java` — untuk memahami pola service

**JANGAN sentuh file lain.**

---

## 3. Requirement Fungsional

### 3.1 List Client (GET /api/v1/admin/client)
- Ambil semua client, supports pagination.
- Query parameter opsional: `status` (active/inactive/expired), `search` (cari by name/code).
- Response include field: id, code, name, phone, company, activatedAt, expiresAt, status, durationMonths.
- Hanya Super Admin (`ROLE_ADMIN`) yang boleh akses.

### 3.2 Get Client by ID (GET /api/v1/admin/client/{id})
- Ambil detail satu client termasuk jumlah user dan invitation terkait.
- Return 404 jika tidak ditemukan.

### 3.3 Create Client (POST /api/v1/admin/client)
- Body: `{ code, name, phone?, company?, address?, notes?, durationMonths }`.
- Validasi: `code` unik, `name` wajib, `durationMonths` > 0.
- `status` auto-set ke `active`, `activatedAt` = hari ini, `expiresAt` = activatedAt + durationMonths.
- `createdAt` dan `updatedAt` auto-set ke `OffsetDateTime.now()`.
- Generate juga 1 user default untuk client (username = code + suffix `_admin`, role = USER, password placeholder). Gunakan `ClientInitService` jika ada, atau langsung di service ini.

### 3.4 Update Client (PUT /api/v1/admin/client/{id})
- Update field: name, phone, company, address, notes.
- Jika `durationMonths` berubah, recalculate `expiresAt` dari `activatedAt` yang sudah ada.
- Return 404 jika client tidak ditemukan.

### 3.5 Update Status Client (PATCH /api/v1/admin/client/{id}/status)
- Body: `{ status: "active" | "inactive" }`.
- Validasi nilai status. Tidak boleh set expired manual — itu dihitung dari `expiresAt`.

### 3.6 Extend Expiry Client (PATCH /api/v1/admin/client/{id}/extend)
- Body: `{ additionalMonths: number }`.
- Tambahkan `additionalMonths` ke `expiresAt` yang sekarang.
- `durationMonths` ikut diupdate.

### 3.7 Delete Client (DELETE /api/v1/admin/client/{id})
- Soft delete: ubah status jadi `inactive` (JANGAN hapus record dari DB).
- Return 204 No Content.
- Jika client punya user/invitation aktif, tetap boleh di-nonaktifkan.

---

## 4. Requirement Teknis

### 4.1 Struktur Package
```
com.undangan.online.controller.ClientController
com.undangan.online.service.ClientService
com.undangan.online.dto.ClientDto
com.undangan.online.dto.CreateClientRequest
com.undangan.online.dto.UpdateClientRequest
```

### 4.2 Pola Kode
- **Controller**: Konstruktor injection, `@RequestMapping("/api/v1/admin/client")`, `@PreAuthorize("hasRole('ADMIN')")` di class-level, `@Transactional` untuk write operations.
- **Service**: Konstruktor injection, annotation `@Service`, logika bisnis di sini, gunakan repository untuk akses data.
- **DTO**: Java bean (getter/setter manual, bukan record Lombok), gunakan `jakarta.validation` annotation.
- **Naming**: Bahasa Inggris untuk kode, field JSON camelCase.
- **Error handling**: Lempar `AuthException` untuk error bisnis, gunakan `ResponseStatusException` untuk 404, biarkan `GlobalExceptionHandler` handle sisanya.

### 4.3 Konvensi Environment Variable
Tidak perlu menambahkan env var baru. Koneksi database sudah dari:
```
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/undangan}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:undangan}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:undangan}
```

### 4.4 Pola Response
Gunakan Map<String, Object> atau DTO untuk response. Format error sudah ditangani oleh `GlobalExceptionHandler` — konsisten dengan modul auth:
```json
{ "error": "ERROR_CODE", "message": "Pesan error" }
```

### 4.5 Pola Pagination
Gunakan `Pageable` dan `Page` dari Spring Data. Contoh endpoint list:
```java
@GetMapping
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Page<ClientDto>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String search) {
```

---

## 5. Definition of Done

Beri tanda `[x]` jika sudah selesai:

- [ ] `ClientController.java` dibuat di folder `controller/`
- [ ] `ClientService.java` dibuat di folder `service/`
- [ ] `ClientDto.java` dibuat di folder `dto/`
- [ ] `CreateClientRequest.java` dibuat di folder `dto/` dengan validasi
- [ ] `UpdateClientRequest.java` dibuat di folder `dto/` dengan validasi
- [ ] Semua endpoint REST di-implement (GET list, GET by ID, POST, PUT, PATCH status, PATCH extend, DELETE)
- [ ] Endpoint dilindungi `@PreAuthorize("hasRole('ADMIN')")`
- [ ] Service menggunakan `ClientRepository` yang sudah ada
- [ ] Validasi input dengan `@Valid` di controller
- [ ] Error handling lempar exception yang tepat (AuthException / ResponseStatusException)
- [ ] Pola pagination di-implement untuk list
- [ ] Kode mengikuti gaya yang sudah ada di `AuthController` dan `AuthService`
- [ ] Tidak ada perubahan ke file di luar scope

---

## 6. Batasan Tegas

- **JANGAN jalankan** `docker`, `docker compose`, `mvn`, `npm run`, `npm start`, `npm test`.
- **JANGAN sentuh file di folder `Sql/`** — schema database sudah ada dan di-manage manual.
- **JANGAN ubah** `entity/Client.java` — entity sudah ada sesuai schema.
- **JANGAN ubah** `repository/ClientRepository.java` — sudah ada.
- **JANGAN ubah** `config/SecurityConfig.java`.
- **JANGAN tambahkan** dependency baru.
- **JANGAN pakai** `ddl-auto=update` atau `create`.
- Task ini HANYA membuat service + controller + DTO. Schema tidak disentuh.

---

## 7. Catatan Referensi

- Detail bisnis client ada di `Docs/PRD-Undangan-Online.md` bagian 4.1 (Super Admin) dan 9.
- Aturan umum: `Docs/CLAUDE.md`.
- Entity `Client` ada di `Backend/src/main/java/com/undangan/online/entity/Client.java` — pastikan field yang di-update match dengan kolom di entity.
- Field `status` client: `active`, `inactive` (tidak ada `expired` sebagai status manual — expiry dihitung dari `expiresAt`).
- Masa aktif default: 30 hari (sesuai PRD). Jika `durationMonths` tidak diisi saat create, default = 1.
- Saat extend, `expiresAt` BARU = `expiresAt` SEKARANG + `additionalMonths`.
