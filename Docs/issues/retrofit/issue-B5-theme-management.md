# Issue B-5 — Retrofit Modul Theme Management (Super Admin & Client)

> **Audience:** AI agent junior (model kecil). Agent **SELALU** menjalankan `docker`, `docker compose`, `mvn`, `npm`, atau perintah build/run/test apa pun ketika sesudah menyelesaikan issue.
>
> **Prasyarat:** Issue A selesai.
>
> **Acuan utama:**
> - `Docs/CLAUDE.md` bagian **#6**, **#7**, **#8**
> - `Docs/PRD-Undangan-Online.md` bagian 5 (Use Case Super Admin — Theme Management)
> - `Docs/issues/phase-3/05-theme-management.md`

---

## 1. Ringkasan & Alasan Retrofit

Modul Theme: Super Admin mengelola master tema (upload file tema ke folder storage, CRUD status active/inactive). Client bisa memilih tema aktif untuk undangannya.

**Controller terkait:**
- `AdminThemeController` — Super Admin CRUD + upload tema
- `ClientThemeController` — Client lihat & pilih tema

**Service terkait:**
- `ThemeStorageService` — handle file upload ke folder `TemaUndangan/`
- `ThemeManagementService` — logic CRUD tema

**Yang perlu diperbaiki:**
1. Kedua controller belum pakai `ApiResponse<T>` wrapper konsisten.
2. `ThemeStorageService` & `ThemeManagementService` belum interface + impl.
3. Upload file: cek apakah MultipartFile handling sudah benar (max size dari env var, storage path konsisten, nama file aman).
4. Logging upload/download tema + status change.
5. Validasi input DTO (theme name, code, status).

**TIDAK termasuk issue ini:**
- Modul lain (Music — Issue B-6, Gallery — Issue B-7, dst)

---

## 2. Scope — File/Folder yang Boleh Disentuh

### File yang BOLEH diedit (existing):
- `Backend/src/main/java/com/undangan/online/controller/AdminThemeController.java`
- `Backend/src/main/java/com/undangan/online/controller/ClientThemeController.java`
- `Backend/src/main/java/com/undangan/online/service/ThemeStorageService.java`
- `Backend/src/main/java/com/undangan/online/service/ThemeManagementService.java`
- `Backend/src/main/java/com/undangan/online/dto/CreateThemeRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/UpdateThemeRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/ThemeDto.java`
- `Backend/src/main/java/com/undangan/online/dto/ThemeListDto.java`

### File BARU yang WAJIB dibuat:
- `Backend/src/main/java/com/undangan/online/service/impl/ThemeStorageServiceImpl.java`
- `Backend/src/main/java/com/undangan/online/service/impl/ThemeManagementServiceImpl.java`

### DILARANG keras:
- Folder `Sql/`
- Folder `Docs/`, `Frontend/`, `Database/`
- Folder `TemaUndangan/` (storage — JANGAN tulis/commit file biner)
- File di package `entity/`, `repository/`
- Controller/service lain di luar Scope #2
- `StorageConfig.java` (Issue A scope — JANGAN disentuh kecuali harus)
- `ApiResponse.java`

---

## 3. Checklist Audit

### 3.1 Audit Controller
- [ ] Daftar endpoint di `AdminThemeController` & `ClientThemeController`.
- [ ] Return type: `ApiResponse<T>` atau plain?
- [ ] `@PreAuthorize` — `AdminThemeController` harus pakai role Super Admin; `ClientThemeController` pakai role Client.
- [ ] Endpoint upload — `@RequestParam("file") MultipartFile` atau `@RequestPart`? Konsisten?
- [ ] Business logic di controller?

### 3.2 Audit Service
- [ ] `ThemeStorageService` & `ThemeManagementService` — class langsung atau interface?
- [ ] Method upload: simpan ke path `STORAGE_THEMES_PATH` (dari `StorageConfig`)? Validasi extension (.zip / .css / .html sesuai PRD)?
- [ ] Method delete: hapus file dari storage atau cuma set inactive? (Cek business intent.)
- [ ] Validasi nama file — apakah ada sanitize (path traversal prevention)?

### 3.3 Audit Logging
- [ ] Logger di service?
- [ ] Log untuk upload (sukses/gagal, size, nama file), delete, activate/deactivate?
- [ ] **JANGAN log** seluruh isi file tema (cuma metadata: name, size, path).

### 3.4 Audit File Upload Safety
- [ ] Max size dari config (`MAX_MUSIC_SIZE_MB` mungkin juga dipakai untuk tema? Cek — atau ada config khusus tema). Validasi di service.
- [ ] Validate file extension / MIME type.
- [ ] Generate nama file aman (UUID atau sanitized original name, hindari path traversal `../`).

### 3.5 Audit DTO
- [ ] Bean Validation di `CreateThemeRequest` & `UpdateThemeRequest`.
- [ ] `ThemeDto` & `ThemeListDto` — tidak expose file path sensitif? (Path ke storage biasanya aman untuk di-expose karena di-mount volume, tapi cek.)

---

## 4. Requirement Teknis Perbaikan

Acuan: `Docs/CLAUDE.md` bagian #6, #7, #8.

### 4.1 Refaktor Service jadi Interface + Impl
- `ThemeStorageService` & `ThemeManagementService` → interface + `*ServiceImpl` di `impl/`.

### 4.2 Refaktor Controller Pakai `ApiResponse`
- Endpoint CRUD → `ResponseEntity<ApiResponse<T>>`.
- Endpoint upload → return `ApiResponse.ok("Tema berhasil diupload", themeDto)`.
- Endpoint delete file → `ApiResponse.ok("File tema berhasil dihapus", null)`.

### 4.3 Upload Safety
- **Path traversal prevention:** sanitize nama file — replace karakter non-alphanumeric (selain `-`, `_`, `.`) dengan `_`. Tolak nama file yang berisi `..` atau `/` atau `\`.
- **File size validation:** cek `MultipartFile.getSize()` SEBELUM save. Kalau > max dari config → throw exception (400 / pesan jelas).
- **Extension check:** sesuai PRD (misal: `.zip`, `.css`, `.html` — cek PRD untuk list pasti).
- **Atomic write:** tulis ke temp file dulu, lalu rename. Cegah setengah file tertinggal kalau write gagal.

### 4.4 Tambah Logging
Di `ThemeStorageServiceImpl`:
- `uploadTheme(...)`: `log.info("Theme file uploaded: name='{}', size={} bytes, by admin {}", sanitizedName, fileSize, actorUsername);`
- `deleteThemeFile(...)`: `log.warn("Theme file deleted: path='{}', by admin {}", relativePath, actorUsername);`

Di `ThemeManagementServiceImpl`:
- `createTheme(...)`: `log.info("Theme {} created by admin {}", code, actorUsername);`
- `updateTheme(...)`: `log.info("Theme {} updated by admin {}", id, actorUsername);`
- `activateTheme(...)` / `deactivateTheme(...)`: `log.info("Theme {} status changed to {} by admin {}", id, status, actorUsername);`
- `deleteTheme(...)`: `log.warn("Theme {} deleted by admin {}", id, actorUsername);`
- Upload/gagal: `log.error("Theme upload failed: name='{}'", name, ex);`

**JANGAN log** isi file tema, hanya metadata.

### 4.5 Bean Validation
- `CreateThemeRequest`: `@NotBlank code`, `@NotBlank name`, `@NotNull templateType`, dll.
- `@RequestParam` file di controller: tidak perlu Bean Validation (Spring otomatis handle).

### 4.6 Entity Tidak Boleh Di-expose
- Response controller → `ThemeDto` / `ThemeListDto`.

---

## 5. Definition of Done

- [ ] `ThemeStorageService` & `ThemeManagementService` jadi interface + `*Impl`
- [ ] Controller return `ApiResponse<T>` di semua endpoint
- [ ] Constructor injection
- [ ] Upload safety: path traversal prevention, size check, extension check
- [ ] Logging SLF4J ada untuk operasi penting (create/update/delete/upload/activate)
- [ ] Bean Validation lengkap di request DTO
- [ ] Tidak ada return type yang expose Entity

### Cross-cutting
- [ ] Folder `Sql/` TIDAK disentuh
- [ ] Folder `TemaUndangan/` TIDAK disentuh sebagai biner (path config saja)
- [ ] `ApiResponse.java` TIDAK disentuh
- [ ] TIDAK menjalankan build tool
- [ ] TIDAK menambah endpoint baru
- [ ] TIDAK mengubah business behavior

### Catatan wajib di ringkasan akhir
- [ ] Endpoint disentuh
- [ ] Asumsi
- [ ] Bug di luar scope
- [ ] DoD checklist

---

## 6. Batasan Tegas

1. JANGAN jalankan build tool.
2. JANGAN ubah file di `Sql/`.
3. JANGAN tulis/commit file biner ke `TemaUndangan/`.
4. JANGAN sentuh file di luar Scope #2.
5. JANGAN pakai `ddl-auto=update`/`create`.
6. JANGAN tambah library baru.
7. JANGAN tambah endpoint baru.
8. JANGAN ubah business behavior.
9. JANGAN log isi file / full path ke tema (cukup nama + metadata).
10. JANGAN pakai Lombok.
11. JANGAN ubah Entity.
12. Bug di luar scope → CATAT.
13. AMBIGU → asumsi + catat.

---

## 7. Catatan untuk Manusia

```markdown
## Ringkasan
- Endpoint disentuh:
  - AdminThemeController: [daftar]
  - ClientThemeController: [daftar]
- File dibuat: [...]
- File diubah: [...]
- Asumsi:
  - [contoh: "Max theme size pakai MAX_MUSIC_SIZE_MB (10MB) — perlu konfirmasi apakah tema butuh limit lebih besar (umumnya tema < 5MB tapi master template kompleks bisa > 10MB)."]
  - [contoh: "Ekstensi file tema yang diterima: .zip, .css, .html — sesuai PRD."]
- Bug di luar scope:
  - [contoh: "ThemeStorageService.uploadTheme tidak ada virus scan — file tema dari Super Admin langsung dipakai client tanpa validasi konten. Di luar scope retrofit."]
- DoD terpenuhi: [checklist]
```

---

## 8. Urutan Pengerjaan

1. Audit #3.
2. Interface `ThemeStorageService` + Impl.
3. Interface `ThemeManagementService` + Impl.
4. Refaktor kedua controller → `ApiResponse`.
5. Tambah logging.
6. Bean Validation di DTO.
7. Self-verify.

---

*Setelah selesai, lanjut ke Issue B-6 (Retrofit Music Library).*