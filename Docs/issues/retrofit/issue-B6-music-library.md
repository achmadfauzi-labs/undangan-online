# Issue B-6 — Retrofit Modul Music Library (Super Admin & Client)

> **Audience:** AI agent junior (model kecil). Agent **SELALU** menjalankan `docker`, `docker compose`, `mvn`, `npm`, atau perintah build/run/test apa pun ketika sesudah menyelesaikan issue.
>
> **Prasyarat:** Issue A selesai.
>
> **Acuan utama:**
> - `Docs/CLAUDE.md` bagian **#6**, **#7**, **#8**
> - `Docs/PRD-Undangan-Online.md` bagian 5 (Use Case Super Admin — Music Library)
> - `Docs/issues/phase-3/06-music-library.md`

---

## 1. Ringkasan & Alasan Retrofit

Modul Music: Super Admin upload & kelola musik library (file `.mp3`/`.wav` ke folder `Musics/`), Client upload musik kustom untuk undangannya sendiri.

**Controller terkait:**
- `AdminMusicController` — Super Admin CRUD + upload musik
- `ClientMusicController` — Client upload musik kustom, pilih musik aktif

**Service terkait:**
- `MusicStorageService` — handle file upload ke `Musics/`
- `MusicManagementService` — logic CRUD musik

**Yang perlu diperbaiki:**
1. Kedua controller belum pakai `ApiResponse<T>` wrapper konsisten.
2. `MusicStorageService` & `MusicManagementService` belum interface + impl.
3. Upload file musik: validasi max size (10MB dari env `MAX_MUSIC_SIZE_MB`), extension `.mp3`/`.wav`/`.ogg`, path safety.
4. Logging upload/delete musik + status change.
5. Bean Validation di DTO.

**TIDAK termasuk issue ini:**
- Modul lain (Theme — B-5, Gallery — B-7, System Parameter — B-8)

---

## 2. Scope — File/Folder yang Boleh Disentuh

### File yang BOLEH diedit (existing):
- `Backend/src/main/java/com/undangan/online/controller/AdminMusicController.java`
- `Backend/src/main/java/com/undangan/online/controller/ClientMusicController.java`
- `Backend/src/main/java/com/undangan/online/service/MusicStorageService.java`
- `Backend/src/main/java/com/undangan/online/service/MusicManagementService.java`
- `Backend/src/main/java/com/undangan/online/dto/CreateMusicRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/ClientUploadMusicRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/MusicDto.java`

### File BARU yang WAJIB dibuat:
- `Backend/src/main/java/com/undangan/online/service/impl/MusicStorageServiceImpl.java`
- `Backend/src/main/java/com/undangan/online/service/impl/MusicManagementServiceImpl.java`

### DILARANG keras:
- Folder `Sql/`
- Folder `Docs/`, `Frontend/`, `Database/`
- Folder `Musics/` (storage — JANGAN tulis/commit file biner)
- File di package `entity/`, `repository/`
- Controller/service lain di luar Scope #2
- `StorageConfig.java` (Issue A scope)
- `ApiResponse.java`

---

## 3. Checklist Audit

### 3.1 Audit Controller
- [ ] Daftar endpoint di `AdminMusicController` & `ClientMusicController`.
- [ ] Return type: `ApiResponse<T>` atau plain?
- [ ] `@PreAuthorize` — Super Admin vs Client role sudah benar?
- [ ] Endpoint upload — `@RequestParam("file") MultipartFile`?
- [ ] Business logic di controller?

### 3.2 Audit Service
- [ ] `MusicStorageService` & `MusicManagementService` — class langsung atau interface?
- [ ] Upload: simpan ke `STORAGE_MUSICS_PATH` (dari `StorageConfig`)? Extension check?
- [ ] Max file size: dari env var (`MAX_MUSIC_SIZE_MB`)? Validasi di service atau hanya di `application.properties`?
- [ ] Delete: hapus file fisik dari storage atau cuma set inactive?
- [ ] Path safety: nama file sanitized?

### 3.3 Audit Logging
- [ ] Logger di service?
- [ ] Log untuk upload (sukses/gagal, size, nama sanitized), delete, activate/deactivate?
- [ ] **JANGAN log** konten/lirik musik atau file audio.

### 3.4 Audit File Upload Safety
- [ ] Max size dari config (10MB). Validasi di service `MultipartFile.getSize()`.
- [ ] Extension whitelist: `.mp3`, `.wav`, `.ogg` (sesuai PRD).
- [ ] MIME type check (ada atau tidak).
- [ ] Generate nama file aman (UUID prefix atau sanitized).
- [ ] Atomic write: temp file → rename.

### 3.5 Audit DTO
- [ ] Bean Validation di `CreateMusicRequest` & `ClientUploadMusicRequest`.
- [ ] `MusicDto` — tidak expose full path ke storage?

---

## 4. Requirement Teknis Perbaikan

Acuan: `Docs/CLAUDE.md` bagian #6, #7, #8.

### 4.1 Refaktor Service jadi Interface + Impl
- `MusicStorageService` & `MusicManagementService` → interface + `*ServiceImpl` di `impl/`.

### 4.2 Refaktor Controller Pakai `ApiResponse`
- Semua endpoint → `ResponseEntity<ApiResponse<T>>`.
- Upload → `ApiResponse.ok("Musik berhasil diupload", musicDto)`.
- Delete → `ApiResponse.ok("Musik berhasil dihapus", null)`.
- List → `ApiResponse.ok(musicList)`.

### 4.3 Upload Safety (Sama Prinsip dengan Theme)
- **Path traversal prevention:** sanitize nama file, reject `..` / `/` / `\`.
- **File size validation:** cek `MultipartFile.getSize()` SEBELUM save. Kalau > `MAX_MUSIC_SIZE_MB * 1024 * 1024` → throw exception (400).
- **Extension whitelist:** `.mp3`, `.wav`, `.ogg` — sesuai PRD. Tolak extension lain.
- **MIME type check (opsional):** kalau pakai `application/octet-stream` atau `audio/mpeg` — boleh dicek, tapi extension whitelist sudah cukup untuk dasar.
- **Atomic write:** tulis ke temp file dulu, lalu rename. Cegah setengah file.

### 4.4 Tambah Logging
Di `MusicStorageServiceImpl`:
- `uploadMusic(...)`: `log.info("Music file uploaded: name='{}', size={} bytes, by {} (role={})", sanitizedName, fileSize, actorUsername, role);`
- `deleteMusicFile(...)`: `log.warn("Music file deleted: path='{}', by {} (role={})", relativePath, actorUsername, role);`
- Gagal: `log.error("Music upload failed: name='{}', size={}", name, size, ex);`

Di `MusicManagementServiceImpl`:
- `createMusic(...)`: `log.info("Music {} created by admin {}", code, actorUsername);`
- `updateMusic(...)`: `log.info("Music {} updated by admin {}", id, actorUsername);`
- `activateMusic(...)` / `deactivateMusic(...)`: `log.info("Music {} status changed to {} by {}", id, status, actorUsername);`
- `deleteMusic(...)`: `log.warn("Music {} deleted by {}", id, actorUsername);`

### 4.5 Bean Validation
- `CreateMusicRequest`: `@NotBlank title`, `@NotBlank artist` (kalau ada), dll.
- `ClientUploadMusicRequest`: `@NotBlank title`, dll.
- Max file size (MultipartFile) → validasi di service, bukan di DTO.

### 4.6 Entity Tidak Boleh Di-expose
- Response controller → `MusicDto`.

---

## 5. Definition of Done

- [ ] `MusicStorageService` & `MusicManagementService` jadi interface + `*Impl`
- [ ] Controller return `ApiResponse<T>` di semua endpoint
- [ ] Constructor injection
- [ ] Upload safety: path traversal, size check, extension whitelist
- [ ] Logging SLF4J ada
- [ ] Bean Validation di request DTO
- [ ] Tidak ada return type yang expose Entity

### Cross-cutting
- [ ] Folder `Sql/` TIDAK disentuh
- [ ] Folder `Musics/` TIDAK disentuh sebagai biner
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
3. JANGAN tulis/commit file audio ke `Musics/`.
4. JANGAN sentuh file di luar Scope #2.
5. JANGAN pakai `ddl-auto=update`/`create`.
6. JANGAN tambah library baru.
7. JANGAN tambah endpoint baru.
8. JANGAN ubah business behavior.
9. JANGAN log konten musik/audio.
10. JANGAN pakai Lombok.
11. JANGAN ubah Entity.
12. Bug di luar scope → CATAT.
13. AMBIGU → asumsi + catat.

---

## 7. Catatan untuk Manusia

```markdown
## Ringkasan
- Endpoint disentuh:
  - AdminMusicController: [daftar]
  - ClientMusicController: [daftar]
- File dibuat: [...]
- File diubah: [...]
- Asumsi:
  - [contoh: "Extension musik yang diterima: .mp3, .wav, .ogg — sesuai PRD."]
  - [contoh: "Max size 10MB dari env MAX_MUSIC_SIZE_MB, validasi di service layer (bukan cuma di application.properties)."]
- Bug di luar scope:
  - [contoh: "MusicStorageService tidak ada virus/malware scan untuk file audio. Di luar scope retrofit."]
- DoD terpenuhi: [checklist]
```

---

## 8. Urutan Pengerjaan

1. Audit #3.
2. Interface `MusicStorageService` + Impl.
3. Interface `MusicManagementService` + Impl.
4. Refaktor kedua controller → `ApiResponse`.
5. Tambah logging.
6. Bean Validation di DTO.
7. Self-verify.

---

*Setelah selesai, lanjut ke Issue B-7 (Retrofit Image/Gallery Management).*
