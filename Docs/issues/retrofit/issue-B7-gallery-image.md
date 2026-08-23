# Issue B-7 — Retrofit Modul Image/Gallery Management

> **Audience:** AI agent junior (model kecil). Agent **DILARANG** menjalankan `docker`, `docker compose`, `mvn`, `npm`, atau perintah build/run/test apa pun. Tugas murni menulis/mengedit kode. Manusia yang test manual.
>
> **Prasyarat:** Issue A selesai.
>
> **Acuan utama:**
> - `Docs/CLAUDE.md` bagian **#6**, **#7**, **#8**
> - `Docs/PRD-Undangan-Online.md` bagian 6 (Use Case Client — Gallery)
> - `Docs/issues/phase-3/07-gallery-management.md`

---

## 1. Ringkasan & Alasan Retrofit

Modul Gallery: Client mengelola foto/gambar untuk undangan mereka (upload ke `Images/`, CRUD gallery entries). Gallery entries terkait ke `Invitation`.

**Controller terkait:**
- `ClientGalleryController` — Client CRUD gallery

**Service terkait:**
- `ImageStorageService` — handle file upload ke `Images/`
- `GalleryService` — logic CRUD gallery entries

**Yang perlu diperbaiki:**
1. Controller belum pakai `ApiResponse<T>` wrapper.
2. `ImageStorageService` & `GalleryService` belum interface + impl.
3. Upload gambar: validasi max size (5MB dari env `MAX_IMAGE_SIZE_MB`), extension whitelist (`.jpg`, `.jpeg`, `.png`, `.webp`), path safety.
4. Logging upload/delete gambar.
5. Ownership validation: client hanya bisa akses gallery invitation miliknya.

**TIDAK termasuk issue ini:**
- Modul lain (Music — B-6, System Parameter — B-8)

---

## 2. Scope — File/Folder yang Boleh Disentuh

### File yang BOLEH diedit (existing):
- `Backend/src/main/java/com/undangan/online/controller/ClientGalleryController.java`
- `Backend/src/main/java/com/undangan/online/service/ImageStorageService.java`
- `Backend/src/main/java/com/undangan/online/service/GalleryService.java`
- `Backend/src/main/java/com/undangan/online/dto/CreateGalleryRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/UpdateGalleryRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/GalleryDto.java`

### File BARU yang WAJIB dibuat:
- `Backend/src/main/java/com/undangan/online/service/impl/ImageStorageServiceImpl.java`
- `Backend/src/main/java/com/undangan/online/service/impl/GalleryServiceImpl.java`

### DILARANG keras:
- Folder `Sql/`
- Folder `Docs/`, `Frontend/`, `Database/`
- Folder `Images/` (storage — JANGAN tulis/commit file gambar)
- File di package `entity/`, `repository/`
- Controller/service lain di luar Scope #2
- `StorageConfig.java` (Issue A scope)
- `ApiResponse.java`

---

## 3. Checklist Audit

### 3.1 Audit Controller
- [ ] Daftar endpoint di `ClientGalleryController`.
- [ ] Return type: `ApiResponse<T>` atau plain?
- [ ] `@PreAuthorize("hasRole('CLIENT')")` ada? (atau yang setara dari Issue A fix.)
- [ ] Apakah ada parameter `invitationId` di request? Ownership check bagaimana?
- [ ] Business logic di controller?

### 3.2 Audit Service
- [ ] `ImageStorageService` & `GalleryService` — class langsung atau interface?
- [ ] Upload: simpan ke `STORAGE_IMAGES_PATH`? Extension whitelist? Max size 5MB?
- [ ] Delete: hapus file fisik atau cuma set inactive?
- [ ] Path safety: nama file sanitized (GUID prefix)?
- [ ] Atomic write?

### 3.3 Audit Logging
- [ ] Logger di service?
- [ ] Log untuk upload (sukses/gagal, size, extension), delete gallery entry?

### 3.4 Audit Ownership
- [ ] `GalleryService` — bagaimana ownership dicek? Apakah `invitationId` dari request parameter divalidasi bahwa invitation tersebut milik `clientId` dari token?
- [ ] Kalau client A coba akses/modify gallery client B → 403?

### 3.5 Audit File Upload Safety
- [ ] Extension whitelist: `.jpg`, `.jpeg`, `.png`, `.gif`, `.webp`?
- [ ] Max size 5MB dari env `MAX_IMAGE_SIZE_MB`. Validasi di service.
- [ ] MIME type check (Content-Type header bisa spoofed — cukup extension whitelist).
- [ ] Image dimension check? (Di luar scope retrofit kalau belum ada — CATAT saja.)

### 3.6 Audit DTO
- [ ] Bean Validation di `CreateGalleryRequest` & `UpdateGalleryRequest`.
- [ ] `GalleryDto` — expose full path ke storage? (Biasanya hanya relative path / URL.)

---

## 4. Requirement Teknis Perbaikan

Acuan: `Docs/CLAUDE.md` bagian #6, #7, #8.

### 4.1 Refaktor Service jadi Interface + Impl
- `ImageStorageService` & `GalleryService` → interface + `*ServiceImpl` di `impl/`.

### 4.2 Refaktor Controller Pakai `ApiResponse`
- Semua endpoint → `ResponseEntity<ApiResponse<T>>`.

### 4.3 Ownership Validation
- `GalleryService` — setiap method yang akses gallery by `invitationId` HARUS:
  - Lookup `Invitation` by `invitationId`
  - Validasi `invitation.clientId == clientId dari JWT`
  - Kalau beda → throw `AuthException("ACCESS_DENIED", ..., 403)`
- Ini sama prinsipnya dengan Invitation ownership di Issue B-3.

### 4.4 Upload Safety
- **Extension whitelist:** `.jpg`, `.jpeg`, `.png`, `.gif`, `.webp` (lowercase check).
- **File size validation:** `MultipartFile.getSize()` > `MAX_IMAGE_SIZE_MB * 1024 * 1024` → throw (400).
- **Path traversal prevention:** sanitize nama file, reject `..` / `/` / `\`.
- **Atomic write:** temp file → rename.
- **GUID filename:** generate `UUID.randomUUID().toString() + "_" + sanitizedOriginalName` untuk menghindari nama duplikat & path traversal.

### 4.5 Tambah Logging
Di `ImageStorageServiceImpl`:
- `uploadImage(...)`: `log.info("Gallery image uploaded: originalName='{}', savedAs='{}', size={}, invitationId={}, clientId={}", originalName, savedName, fileSize, invitationId, clientId);`
- `deleteImage(...)`: `log.warn("Gallery image deleted: path='{}', by clientId={}", relativePath, clientId);`
- Gagal: `log.error("Gallery image upload failed: name='{}', invitationId={}", name, invitationId, ex);`

Di `GalleryServiceImpl`:
- `createGalleryEntry(...)`: `log.info("Gallery entry created: invitationId={}, clientId={}", invitationId, clientId);`
- `updateGalleryEntry(...)`: `log.info("Gallery entry {} updated: invitationId={}, clientId={}", entryId, invitationId, clientId);`
- `deleteGalleryEntry(...)`: `log.warn("Gallery entry {} deleted: invitationId={}, clientId={}", entryId, invitationId, clientId);`
- Ownership violation: `log.warn("Gallery access denied: clientId={} tried to access gallery invitationId={}", clientId, invitationId);`

### 4.6 Bean Validation
- `CreateGalleryRequest`: `@NotBlank title`, `@NotNull orderIndex` (kalau ada), dll.
- Max file size (MultipartFile) → validasi di service.

### 4.7 Entity Tidak Boleh Di-expose
- Response controller → `GalleryDto`.

---

## 5. Definition of Done

- [ ] `ImageStorageService` & `GalleryService` jadi interface + `*Impl`
- [ ] Controller return `ApiResponse<T>` di semua endpoint
- [ ] Constructor injection
- [ ] Ownership validation: `clientId` dari JWT vs `invitation.clientId`
- [ ] Upload safety: extension whitelist, size check, path traversal prevention
- [ ] Logging SLF4J ada
- [ ] Bean Validation di request DTO
- [ ] Tidak ada return type yang expose Entity

### Cross-cutting
- [ ] Folder `Sql/` TIDAK disentuh
- [ ] Folder `Images/` TIDAK disentuh sebagai biner
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
3. JANGAN tulis/commit file gambar ke `Images/`.
4. JANGAN sentuh file di luar Scope #2.
5. JANGAN pakai `ddl-auto=update`/`create`.
6. JANGAN tambah library baru.
7. JANGAN tambah endpoint baru.
8. JANGAN ubah business behavior.
9. JANGAN log konten gambar.
10. JANGAN pakai Lombok.
11. JANGAN ubah Entity.
12. Bug di luar scope → CATAT.
13. AMBIGU → asumsi + catat.

---

## 7. Catatan untuk Manusia

```markdown
## Ringkasan
- Endpoint disentuh:
  - ClientGalleryController: [daftar]
- File dibuat: [...]
- File diubah: [...]
- Asumsi:
  - [contoh: "Extension gambar yang diterima: .jpg, .jpeg, .png, .gif, .webp — sesuai standar image untuk web."]
  - [contoh: "Max size 5MB dari env MAX_IMAGE_SIZE_MB."]
- Bug di luar scope:
  - [contoh: "ImageStorageService tidak ada image dimension validation — oversized image (>4096x4096) bisa makan storage banyak. Perlu follow-up."]
- DoD terpenuhi: [checklist]
```

---

## 8. Urutan Pengerjaan

1. Audit #3.
2. Interface `ImageStorageService` + Impl.
3. Interface `GalleryService` + Impl.
4. Refaktor controller → `ApiResponse`.
5. Tambah logging.
6. Bean Validation di DTO.
7. Self-verify.

---

*Setelah selesai, lanjut ke Issue B-8 (Retrofit System Parameter).*
