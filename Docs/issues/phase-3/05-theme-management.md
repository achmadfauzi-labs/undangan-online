# Fase 3.5 — Theme Management

## 1. Judul & Ringkasan

**Judul:** Backend — Modul Theme Management (Upload + Assignment)

**Ringkasan:** Buat modul backend untuk Super Admin meng-upload template tema ke folder `tema-undangan/` dan assign tema ke invitation client. Juga include endpoint untuk Client memilih tema dari yang sudah tersedia. Tema disimpan sebagai file di storage, BUKAN di database (di database hanya metadata).

---

## 2. Scope — File yang BOLEH Disentuh

Agent HANYA boleh membuat/mengedit file dalam folder ini:

```
Backend/src/main/java/com/undangan/online/
├── controller/
│   ├── AdminThemeController.java          # BARU — Super Admin: CRUD tema
│   └── ClientThemeController.java        # BARU — Client: pilih tema
├── service/
│   ├── ThemeStorageService.java          # BARU — logika storage upload
│   └── ThemeManagementService.java       # BARU — CRUD + assignment
├── dto/
│   ├── ThemeDto.java                     # BARU
│   ├── CreateThemeRequest.java           # BARU
│   ├── UpdateThemeRequest.java           # BARU
│   └── ThemeListDto.java                 # BARU
```

**BOLEH dibaca (referensi):**
- `entity/Template.java` — sudah ada
- `entity/Invitation.java` — sudah ada
- `repository/TemplateRepository.java` — sudah ada
- `repository/InvitationRepository.java` — sudah ada
- `config/StorageConfig.java` — untuk memahami path storage
- `config/SecurityConfig.java`
- `exception/GlobalExceptionHandler.java`
- `dto/ErrorResponse.java`
- `exception/AuthException.java`
- `controller/AuthController.java`

**Catatan:** Entity `Template` merepresentasikan tema/template. Kolom `folderPath` menunjuk ke folder di `tema-undangan/`. Kolom `thumbnailCss` adalah CSS snippet untuk preview thumbnail (bukan file biner).

**JANGAN sentuh file lain.**

---

## 3. Requirement Fungsional

### 3.1 Theme Management — Super Admin (CRUD)

#### 3.1.1 List Themes (GET /api/v1/admin/themes)
- Ambil semua template, supports pagination.
- Query param opsional: `status` (active/inactive), `category`, `search` (cari by name/code).
- Response: id, code, name, category, folderPath, thumbnailCss, status, createdAt.

#### 3.1.2 Get Theme (GET /api/v1/admin/themes/{id})
- Ambil detail satu theme. Return 404 jika tidak ditemukan.

#### 3.1.3 Create Theme (POST /api/v1/admin/themes)
- Body: multipart/form-data dengan fields:
  - `code` (string, unik, contoh: "batik-2024")
  - `name` (string, contoh: "Batik Elegan")
  - `category` (string, contoh: "batik" | "modern" | "elegant" | "nature" | "sakura" | "white")
  - `thumbnailCss` (string, opsional — CSS untuk preview thumbnail)
  - `zipFile` (file, opsional — file ZIP tema)
- Validasi: `code` unik, `name` wajib, `category` wajib.
- `status` auto-set ke `active`.
- `createdAt`/`updatedAt` = `OffsetDateTime.now()`.
- **Upload ZIP file**: jika `zipFile` disediakan, extract ke `tema-undangan/{code}/`. Process:
  1. Buat folder `tema-undangan/{code}/` (path dari environment `STORAGE_THEMES_PATH`).
  2. Extract ZIP ke folder tersebut.
  3. Set `folderPath` = path relatif dari storage root (misal: `themes/batik-2024/`).
- Jika `zipFile` tidak disediakan, `folderPath` bisa null atau placeholder — Super Admin bisa upload manual lewat SFTP/copy file.

#### 3.1.4 Update Theme (PUT /api/v1/admin/themes/{id}`)
- Update: name, category, thumbnailCss.
- `code` TIDAK BISA diubah.
- Jika ada `zipFile` baru, replace isi folder tema.
- `updatedAt` auto-update.

#### 3.1.5 Update Theme Status (PATCH /api/v1/admin/themes/{id}/status)
- Body: `{ status: "active" | "inactive" }`.
- Tema yang sedang dipakai oleh invitation aktif TETAP BISA di-nonaktifkan (tidak di-block).

#### 3.1.6 Delete Theme (DELETE /api/v1/admin/themes/{id}`)
- Hard delete record dari tabel `template`.
- JANGAN hapus file fisik di storage (biarkan file tetap ada — ini tugas manusia untuk cleanup manual).
- Return 204.

#### 3.1.7 List Theme Categories (GET /api/v1/admin/themes/categories)
- Return list unique categories yang sudah ada di database.
- Bisa hardcoded di service atau query distinct dari tabel.

#### 3.1.8 Upload Theme ZIP (PATCH /api/v1/admin/themes/{id}/upload)
- Endpoint terpisah untuk upload/replace ZIP file tanpa update metadata lain.
- Body: multipart/form-data dengan field `zipFile`.
- Extract dan replace isi folder tema.

---

### 3.2 Theme Selection — Client

#### 3.2.1 List Available Themes (GET /api/v1/client/themes)
- Ambil semua tema dengan `status = active` yang bisa dipilih client.
- Response: id, code, name, category, thumbnailCss, folderPath.
- Tidak ada pagination — jumlah tema terbatas (puluhan), bisa di-fetch semua sekaligus.
- Di-sort by category, kemudian name.

#### 3.2.2 Get Theme Detail (GET /api/v1/client/themes/{id})
- Ambil detail satu theme yang aktif.

#### 3.2.3 Assign Theme to My Invitation (POST /api/v1/client/invitation/theme)
- Client memilih tema untuk invitation miliknya.
- Body: `{ themeId: number }`.
- Validasi: theme harus `status = active`, invitation milik client yang login.
- Update `templateId` di tabel `invitation`.
- `updatedAt` invitation auto-update.
- Response: `{ success: true, message: "Tema berhasil dipilih" }`.

#### 3.2.4 Get Current Theme (GET /api/v1/client/invitation/theme)
- Ambil tema yang sedang aktif di invitation client.
- Return data theme lengkap atau null jika belum dipilih.

#### 3.2.5 Remove Theme (DELETE /api/v1/client/invitation/theme)
- Hapus assignment tema dari invitation (set `templateId = null`).

---

## 4. Requirement Teknis

### 4.1 Struktur Package
```
com.undangan.online.controller.AdminThemeController
com.undangan.online.controller.ClientThemeController
com.undangan.online.service.ThemeStorageService
com.undangan.online.service.ThemeManagementService
com.undangan.online.dto.ThemeDto
com.undangan.online.dto.CreateThemeRequest
com.undangan.online.dto.UpdateThemeRequest
com.undangan.online.dto.ThemeListDto
```

### 4.2 Pola Kode
- **AdminThemeController**: `@PreAuthorize("hasRole('ADMIN')")`.
- **ClientThemeController**: `@PreAuthorize("hasRole('USER')")`.
- **Service pattern**: konstruktor injection, `@Service`. Pisahkan `ThemeStorageService` (handle ZIP extract/file operation) dari `ThemeManagementService` (CRUD + assignment logic).
- **File upload**: gunakan `MultipartFile` + `java.util.zip.ZipInputStream` untuk extract. Pattern upload ada di `StorageConfig.java` untuk referensi path.

### 4.3 Konvensi Environment Variable
Storage path tema dari `application.properties`:
```
app.storage.themes=${STORAGE_THEMES_PATH:/app/storage/themes}
```

### 4.4 ZIP Extraction Pattern
```java
// 1. Get storage root from StorageConfig (themesStoragePath bean)
// 2. Create folder: {storageRoot}/{code}/
// 3. Loop entries dari ZipInputStream
// 4. Extract each entry ke folder tujuan
// 5. Skip directory entries, validate file paths (no path traversal)
// 6. Set folderPath = "{code}/" (relative from storage root)
```

### 4.5 Path Traversal Security
WAJIB validasi: file path dalam ZIP tidak boleh mengandung `..` atau absolute path. Tolak extract jika ada entry yang mencurigakan.

---

## 5. Definition of Done

Beri tanda `[x]` jika sudah selesai:

- [ ] `AdminThemeController.java` dibuat
- [ ] `ClientThemeController.java` dibuat
- [ ] `ThemeStorageService.java` dibuat
- [ ] `ThemeManagementService.java` dibuat
- [ ] Semua DTO dibuat
- [ ] Admin CRUD endpoints (list, get, create, update, update-status, delete, list categories, upload) di-implement
- [ ] Client endpoints (list available, get, assign, get current, remove) di-implement
- [ ] Admin controller dilindungi `@PreAuthorize("hasRole('ADMIN')")`
- [ ] Client controller dilindungi `@PreAuthorize("hasRole('USER')")`
- [ ] ZIP extraction dengan validasi path traversal
- [ ] Theme assignment update `templateId` di invitation
- [ ] Client hanya bisa assign ke invitation miliknya
- [ ] File fisik TIDAK dihapus saat delete record
- [ ] Validation dengan `@Valid`
- [ ] Kode mengikuti gaya project
- [ ] Tidak ada perubahan ke file di luar scope

---

## 6. Batasan Tegas

- **JANGAN jalankan** `docker`, `docker compose`, `mvn`, `npm run`, `npm start`, `npm test`.
- **JANGAN sentuh file di folder `Sql/`**.
- **JANGAN ubah** `entity/Template.java` — sudah ada.
- **JANGAN ubah** `entity/Invitation.java` — sudah ada.
- **JANGAN ubah** `config/SecurityConfig.java`.
- **JANGAN tambahkan** dependency baru. Jika butuh ZIP processing, gunakan `java.util.zip.ZipInputStream` (built-in JDK).
- **JANGAN pakai** `ddl-auto=update` atau `create`.
- **JANGAN hapus file fisik** dari disk saat delete theme — cukup hapus record database.
- Task ini HANYA membuat service + controller + DTO. Schema tidak disentuh.

---

## 7. Catatan Referensi

- Detail tema ada di `Docs/PRD-Undangan-Online.md` bagian 4.1 (Super Admin: kelola template) dan 4.2 (Client: pilih tema).
- PRD: Tema disimpan di folder `tema-undangan/` sebagai file, bukan blob di database. Di database hanya metadata (Template entity).
- Aturan umum: `Docs/CLAUDE.md` bagian 5 tentang storage file.
- Entity `Template` di `Backend/src/main/java/com/undangan/online/entity/Template.java` — cek field: `code`, `name`, `category`, `folderPath`, `thumbnailCss`, `status`.
- Entity `Invitation` punya `templateId` (FK ke Template).
- `StorageConfig.java` sudah punya bean `themesStoragePath()` yang menunjuk ke `STORAGE_THEMES_PATH`.
- Tema baru yang diupload Super Admin disimpan ke folder `themes/{code}/`. Folder ini nanti di-serve oleh frontend/nginx dari volume mount.
- `thumbnailCss` adalah CSS inline untuk render preview card di dashboard (bukan file gambar).
