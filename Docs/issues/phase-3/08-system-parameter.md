# Fase 3.8 — System Parameter Management

## 1. Judul & Ringkasan

**Judul:** Backend — Modul System Parameter Management

**Ringkasan:** Buat modul backend untuk Super Admin mengelola parameter sistem (key-value configuration yang mengatur perilaku aplikasi secara global). Contoh: durasi default paket client, batas upload file, template URL base, dll. Semua endpoint di `/api/v1/admin/system-parameters`, dilindungi `@PreAuthorize("hasRole('ADMIN')")`.

---

## 2. Scope — File yang BOLEH Disentuh

Agent HANYA boleh membuat/mengedit file dalam folder ini:

```
Backend/src/main/java/com/undangan/online/
├── controller/
│   └── SystemParameterController.java     # BARU
├── service/
│   └── SystemParameterService.java       # BARU
├── dto/
│   ├── SystemParameterDto.java           # BARU
│   ├── CreateSystemParameterRequest.java # BARU
│   └── UpdateSystemParameterValueRequest.java  # BARU
```

**BOLEH dibaca (referensi):**
- `entity/SystemParameter.java` — sudah ada
- `repository/SystemParameterRepository.java` — sudah ada
- `config/SecurityConfig.java`
- `exception/GlobalExceptionHandler.java`
- `dto/ErrorResponse.java`
- `exception/AuthException.java`
- `controller/AuthController.java`
- `service/AuthService.java` — untuk memahami pola service

**JANGAN sentuh file lain.**

---

## 3. Requirement Fungsional

### 3.1 List System Parameters (GET /api/v1/admin/system-parameters)
- Ambil semua system parameters, supports pagination.
- Query param opsional: `groupCode` (filter by group), `status` (active/inactive), `search` (cari by name/code).
- Response: id, groupCode, code, name, sortOrder, status, createdAt, updatedAt.
- Di-sort by `groupCode`, kemudian `sortOrder`.

### 3.2 Get System Parameter (GET /api/v1/admin/system-parameters/{id})
- Ambil satu system parameter by ID. Return 404 jika tidak ditemukan.

### 3.3 Get Parameters by Group (GET /api/v1/admin/system-parameters/group/{groupCode})
- Ambil semua parameter dalam satu group. Tanpa pagination (jumlah kecil).
- Response: array of `{ code, name, status }` (TANPA value di listing — khusus untuk keamanan).
- Untuk dapat VALUE, gunakan endpoint detail atau khusus.

### 3.4 Get Parameter Value (GET /api/v1/admin/system-parameters/{groupCode}/{code}/value)
- Ambil VALUE spesifik dari satu parameter.
- Response: `{ groupCode, code, name, value }`.
- Ini endpoint terpisah karena di listing group value disembunyikan.

### 3.5 Create System Parameter (POST /api/v1/admin/system-parameters)
- Body: `{ groupCode, code, name, sortOrder?, status? }`.
- Validasi: `groupCode` wajib, `code` wajib + unik, `name` wajib.
- `sortOrder` default = 0.
- `status` default = `active`.
- `createdAt`/`updatedAt` = `OffsetDateTime.now()`.
- **CATATAN**: Entity `SystemParameter` TIDAK punya kolom `value`. Parameter system di phase ini HANYA manage metadata (group, code, name, sort, status). Nilai/value dari parameter dibaca dari tabel `system_parameter` berdasarkan `group_code + code` oleh service lain (bukan di scope ini).
- Jika aplikasi butuh menyimpan VALUE, buat entity atau tabel terpisah, atau gunakan kolom `name` sebagai nilai. **Asumsi**: Value parameter disimpan sebagai `name` field. Client/Frontend membaca parameter lalu interpretasi `name` sebagai value. Konfirmasi dengan tech lead.

### 3.6 Update System Parameter Metadata (PUT /api/v1/admin/system-parameters/{id}`)
- Update: `name`, `sortOrder`.
- `groupCode` dan `code` TIDAK BISA diubah.
- `updatedAt` auto-update.

### 3.7 Update Parameter Status (PATCH /api/v1/admin/system-parameters/{id}/status)
- Body: `{ status: "active" | "inactive" }`.

### 3.8 Delete System Parameter (DELETE /api/v1/admin/system-parameters/{id})
- Hard delete dari tabel `system_parameter`.
- Return 204.

### 3.9 Batch Update Status (PATCH /api/v1/admin/system-parameters/batch-status)
- Bulk update status beberapa parameter sekaligus.
- Body: `{ updates: [{ id: number, status: string }, ...] }`.
- Response: jumlah yang diupdate.

---

### 3.10 Grup Parameter Default

Saat initialize sistem, grup dan parameter default berikut harus sudah ada. Jika belum ada record di database, SEED otomatis saat pertama kali service dipanggil (opsional — bisa juga manual insert via SQL). Buat seed data di service:

#### Grup: `client_package`
| code | name (value) | description |
|---|---|---|
| `default_duration_months` | `12` | Durasi default paket client dalam bulan |
| `expiry_buffer_days` | `7` | Buffer hari sebelum acara untuk aktivasi |

#### Grup: `system`
| code | name (value) | description |
|---|---|---|
| `invitation_base_url` | `https://undangan.domain.com` | Base URL untuk link undangan |
| `max_guest_bulk` | `100` | Maksimum guest per bulk upload |

#### Grup: `file_upload`
| code | name (value) | description |
|---|---|---|
| `max_image_size_mb` | `5` | Maksimum ukuran gambar dalam MB |
| `max_music_size_mb` | `10` | Maksimum ukuran file musik dalam MB |
| `allowed_image_extensions` | `jpg,jpeg,png,webp` | Ekstensi gambar yang diizinkan |
| `allowed_music_extensions` | `mp3,wav,ogg` | Ekstensi musik yang diizinkan |

#### Grup: `event_type`
| code | name (value) | description |
|---|---|---|
| `pernikahan` | `Pernikahan` | Tipe acara pernikahan |
| `mutrasi` | `Mutrasi` | Tipe acara mutrasi |
| `ulang_tahun` | `Ulang Tahun` | Tipe acara ulang tahun |
| `khitanan` | `Khitanan` | Tipe acara khitanan |

#### Grup: `guest_category`
| code | name (value) | description |
|---|---|---|
| `family` | `Keluarga` | Kategori tamu keluarga |
| `friend` | `Teman` | Kategori tamu teman |
| `coworker` | `Rekan Kerja` | Kategori tamu rekan kerja |
| `neighbor` | `Tetangga` | Kategori tamu tetangga |

#### Grup: `person_role`
| code | name (value) | description |
|---|---|---|
| `pria` | `Pria` | Role pasangan pria |
| `wanita` | `Wanita` | Role pasangan wanita |

### 3.11 Seed/Initialize Default Parameters (POST /api/v1/admin/system-parameters/seed)
- Endpoint untuk meng-seed parameter default jika belum ada.
- Cek setiap grup & code. Jika belum ada, insert.
- Jika sudah ada, skip (tidak overwrite).
- Response: `{ seeded: number, skipped: number }`.

---

## 4. Requirement Teknis

### 4.1 Struktur Package
```
com.undangan.online.controller.SystemParameterController
com.undangan.online.service.SystemParameterService
com.undangan.online.dto.SystemParameterDto
com.undangan.online.dto.CreateSystemParameterRequest
com.undangan.online.dto.UpdateSystemParameterValueRequest
```

### 4.2 Pola Kode
- **Controller**: konstruktor injection, `@PreAuthorize("hasRole('ADMIN')")`.
- **Service**: konstruktor injection, `@Service`._INCLUDE_ private method untuk seed data.
- **Repository**: gunakan `SystemParameterRepository` yang sudah ada.

### 4.3 Konvensi Environment Variable
Tidak perlu menambah env var baru.

### 4.4 Important Note on Value Storage
Entity `SystemParameter` TIDAK memiliki kolom `value`. Kolom `name` digunakan sebagai nilai parameter. Ini adalah desain awal — jika tech lead memutuskan lain, sesuaikan.

### 4.5 Seed Logic
```java
// Pattern seed di service:
private void seedIfNotExists(String groupCode, String code, String name, Short sortOrder) {
    systemParameterRepository.findByGroupCodeAndCode(groupCode, code)
        .ifPresentOrElse(
            param -> { /* skip, sudah ada */ },
            () -> {
                SystemParameter p = new SystemParameter();
                p.setGroupCode(groupCode);
                p.setCode(code);
                p.setName(name); // value di sini
                p.setSortOrder(sortOrder);
                p.setStatus("active");
                p.setCreatedAt(OffsetDateTime.now());
                p.setUpdatedAt(OffsetDateTime.now());
                systemParameterRepository.save(p);
            }
        );
}
```

---

## 5. Definition of Done

Beri tanda `[x]` jika sudah selesai:

- [ ] `SystemParameterController.java` dibuat
- [ ] `SystemParameterService.java` dibuat
- [ ] Semua DTO dibuat
- [ ] List parameters (paginated, filter by group/status/search) di-implement
- [ ] Get parameter by ID di-implement
- [ ] Get parameters by group di-implement
- [ ] Get parameter value (specific) di-implement
- [ ] Create parameter di-implement
- [ ] Update parameter metadata di-implement
- [ ] Update parameter status di-implement
- [ ] Delete parameter di-implement
- [ ] Batch update status di-implement
- [ ] Seed default parameters di-implement
- [ ] Semua endpoint dilindungi `@PreAuthorize("hasRole('ADMIN')")`
- [ ] groupCode+code uniqueness validation
- [ ] groupCode & code tidak bisa diubah setelah create
- [ ] Validation dengan `@Valid`
- [ ] Kode mengikuti gaya project
- [ ] Tidak ada perubahan ke file di luar scope

---

## 6. Batasan Tegas

- **JANGAN jalankan** `docker`, `docker compose`, `mvn`, `npm run`, `npm start`, `npm test`.
- **JANGAN sentuh file di folder `Sql/`**.
- **JANGAN ubah** `entity/SystemParameter.java` — sudah ada.
- **JANGAN ubah** `repository/SystemParameterRepository.java` — sudah ada.
- **JANGAN ubah** `config/SecurityConfig.java`.
- **JANGAN tambahkan** dependency baru.
- **JANGAN pakai** `ddl-auto=update` atau `create`.
- **JANGAN mengubah** seed data secara otomatis saat aplikasi start (jika ingin ada seed, gunakan endpoint `/seed` yang dipanggil manual).
- Task ini HANYA membuat service + controller + DTO. Schema tidak disentuh.

---

## 7. Catatan Referensi

- Detail parameter sistem ada di `Docs/PRD-Undangan-Online.md` bagian 4.1 (Super Admin: parameter sistem).
- Entity `SystemParameter` di `Backend/src/main/java/com/undangan/online/entity/SystemParameter.java` — cek field: `id`, `groupCode`, `code`, `name`, `sortOrder`, `status`, `createdAt`, `updatedAt`.
- `Repository` sudah ada: `SystemParameterRepository` dengan method `findByGroupCodeAndCode(groupCode, code)`.
- **Catatan penting**: Entity tidak punya kolom `value`. Kolom `name` digunakan sebagai nilai/value parameter. Ini adalah keputusan desain awal. Jika tech lead mengubahnya, sesuaikan.
- Aturan umum: `Docs/CLAUDE.md`.
- Seed data mencakup 5 grup: `client_package`, `system`, `file_upload`, `event_type`, `guest_category`, `person_role`.
- Grup `file_upload` valuenya harus match dengan konfigurasi di `application.properties` (MAX_IMAGE_SIZE_MB, MAX_MUSIC_SIZE_MB). Seed hanya untuk referensi frontend — aplikasi tetap baca dari application.properties.
