# Fase 3.6 — Music Library

## 1. Judul & Ringkasan

**Judul:** Backend — Modul Music Library (Upload + Assignment)

**Ringkasan:** Buat modul backend untuk Super Admin meng-upload file musik ke folder `musics/` dan assign musik ke invitation. Client juga bisa upload musik sendiri (hanya untuk invitation-nya sendiri, sesuai PRD). Musik disimpan sebagai file di storage, di database hanya metadata.

---

## 2. Scope — File yang BOLEH Disentuh

Agent HANYA boleh membuat/mengedit file dalam folder ini:

```
Backend/src/main/java/com/undangan/online/
├── controller/
│   ├── AdminMusicController.java           # BARU — Super Admin: CRUD musik
│   └── ClientMusicController.java         # BARU — Client: upload musik sendiri + pilih musik
├── service/
│   ├── MusicStorageService.java           # BARU — logika storage upload
│   └── MusicManagementService.java         # BARU — CRUD + assignment
├── dto/
│   ├── MusicDto.java                      # BARU
│   ├── CreateMusicRequest.java            # BARU
│   └── ClientUploadMusicRequest.java      # BARU
```

**BOLEH dibaca (referensi):**
- `entity/Music.java` — sudah ada
- `entity/Invitation.java` — sudah ada
- `repository/MusicRepository.java` — sudah ada
- `repository/InvitationRepository.java` — sudah ada
- `config/StorageConfig.java` — untuk memahami path storage
- `config/SecurityConfig.java`
- `exception/GlobalExceptionHandler.java`
- `dto/ErrorResponse.java`
- `exception/AuthException.java`
- `controller/AuthController.java`

**JANGAN sentuh file lain.**

---

## 3. Requirement Fungsional

### 3.1 Music Library Management — Super Admin (CRUD)

#### 3.1.1 List Musics (GET /api/v1/admin/musics)
- Ambil semua musik, supports pagination.
- Query param opsional: `status` (active/inactive), `search` (cari by title/artist).
- Response: id, title, artist, filePath, fileSize, status, createdAt.
- File size dalam KB atau MB (format human-readable di response, simpan dalam byte di DB).

#### 3.1.2 Get Music (GET /api/v1/admin/musics/{id})
- Ambil detail satu musik. Return 404 jika tidak ditemukan.

#### 3.1.3 Create Music (POST /api/v1/admin/musics)
- Body: multipart/form-data dengan fields:
  - `title` (string, wajib)
  - `artist` (string, opsional)
  - `audioFile` (file, wajib — format: mp3, wav, ogg; max 10MB)
- Validasi: `title` wajib, `audioFile` wajib, ekstensi harus salah satu dari `mp3`, `wav`, `ogg`.
- Simpan file ke `musics/` (path dari `STORAGE_MUSICS_PATH`).
- Naming: `{uuid}_{originalFilename}` untuk menghindari duplikat nama.
- Set `filePath` = path relatif dari storage root (misal: `musics/abc123_musik-pernikahan.mp3`).
- `status` auto-set ke `active`.
- `createdAt`/`updatedAt` = `OffsetDateTime.now()`.
- `fileSize` = ukuran file dalam byte.

#### 3.1.4 Update Music Metadata (PUT /api/v1/admin/musics/{id}`)
- Update: `title`, `artist`.
- File audio TIDAK bisa diupdate lewat endpoint ini — ada endpoint terpisah.
- `updatedAt` auto-update.

#### 3.1.5 Update Music File (PATCH /api/v1/admin/musics/{id}/upload)
- Ganti/replace file audio tanpa ubah record. Body: multipart/form-data dengan field `audioFile`.
- Validasi ekstensi dan size.
- Hapus file lama, simpan file baru dengan nama yang sama (atau generate UUID baru).
- Update `fileSize` dan `updatedAt`.

#### 3.1.6 Update Music Status (PATCH /api/v1/admin/musics/{id}/status)
- Body: `{ status: "active" | "inactive" }`.
- Musik yang sedang dipakai invitation aktif TETAP BISA di-nonaktifkan.

#### 3.1.7 Delete Music (DELETE /api/v1/admin/musics/{id}`)
- Hard delete record dari tabel `music`.
- Hapus file audio dari storage.
- Return 204.

---

### 3.2 Client Music Upload & Selection

#### 3.2.1 Upload Custom Music (POST /api/v1/client/musics/upload)
- Client upload musik sendiri untuk invitation-nya.
- Body: multipart/form-data dengan fields:
  - `title` (string, wajib)
  - `artist` (string, opsional)
  - `audioFile` (file, wajib — mp3/wav/ogg, max 10MB)
- Simpan ke `musics/custom/{clientId}/`.
- `status` auto-set ke `active`.
- Record ini terkait dengan client (opsional: simpan `clientId` di Music entity? Cek schema dulu — jika kolom `clientId` TIDAK ada di tabel `music`, simpan di kolom lain seperti `custom_music_title` di Invitation entity sebagai fallback).
- **Asumsi**: Jika tabel `music` tidak punya kolom `clientId`, client upload music akan disimpan recordnya dengan `clientId` = null, dan file path akan mengandung `custom/` prefix. Client hanya bisa manage (view/edit/delete) musik yang file path-nya mengandung `custom/{clientId}/`. Konfirmasi dengan tech lead jika bingung.
- `createdAt`/`updatedAt` = `OffsetDateTime.now()`.

#### 3.2.2 List Available Musics for Client (GET /api/v1/client/musics`)
- Ambil semua musik yang bisa dipilih client:
  1. Musik Super Admin (`status = active`, `clientId = null`)
  2. Musik custom client sendiri (`status = active`, `clientId = {myClientId}`)
- Response: id, title, artist, filePath, isCustom (boolean — true jika milik client sendiri).
- Tanpa pagination — jumlah terbatas.

#### 3.2.3 Get My Custom Musics (GET /api/v1/client/musics/mine)
- Ambil hanya musik custom yang diupload oleh client ini.

#### 3.2.4 Delete My Custom Music (DELETE /api/v1/client/musics/{id}`)
- Client menghapus musik yang dia upload sendiri.
- Validasi: musik harus milik client (`filePath` mengandung `custom/{clientId}/`).
- Hapus file dari storage + record dari DB.
- Return 204.

#### 3.2.5 Assign Music to Invitation (POST /api/v1/client/invitation/music)
- Body: `{ musicId: number }` atau `{ customMusicTitle: string, customMusicPath: string }` (untuk custom music yang sudah diupload).
- Validasi: musik harus aktif dan accessible oleh client.
- Update `primaryMusicId` di tabel `invitation`.
- Hapus `customMusicPath` dan `customMusicTitle` jika assign dari library (reset custom).
- `updatedAt` invitation auto-update.
- Response: `{ success: true, message: "Musik berhasil dipilih" }`.

#### 3.2.6 Remove Music Assignment (DELETE /api/v1/client/invitation/music)
- Hapus assignment musik dari invitation (set `primaryMusicId = null`, `customMusicPath = null`, `customMusicTitle = null`).

#### 3.2.7 Get Current Music (GET /api/v1/client/invitation/music)
- Ambil musik yang sedang aktif di invitation client.
- Jika `primaryMusicId` != null, fetch dari tabel `music`.
- Jika `customMusicPath` != null, return data custom music.
- Return null jika belum dipilih.

---

## 4. Requirement Teknis

### 4.1 Struktur Package
```
com.undangan.online.controller.AdminMusicController
com.undangan.online.controller.ClientMusicController
com.undangan.online.service.MusicStorageService
com.undangan.online.service.MusicManagementService
com.undangan.online.dto.MusicDto
com.undangan.online.dto.CreateMusicRequest
com.undangan.online.dto.ClientUploadMusicRequest
```

### 4.2 Pola Kode
- **AdminMusicController**: `@PreAuthorize("hasRole('ADMIN')")`.
- **ClientMusicController**: `@PreAuthorize("hasRole('USER')")`.
- **Service**: konstruktor injection, `@Service`. Pisahkan `MusicStorageService` (file operation) dari `MusicManagementService` (CRUD + assignment).
- **File validation**: cek ekstensi, cek size, generate UUID filename.

### 4.3 Konvensi Environment Variable
Storage path musik dari `application.properties`:
```
app.storage.musics=${STORAGE_MUSICS_PATH:/app/storage/musics}
spring.servlet.multipart.max-file-size=${MAX_MUSIC_SIZE_MB:10}MB
```

### 4.4 File Naming & Storage
```
// Admin upload:  {storageRoot}/musics/{uuid}_{originalFilename}
// Client custom: {storageRoot}/musics/custom/{clientId}/{uuid}_{originalFilename}
```

### 4.5 Allowed Extensions
`mp3`, `wav`, `ogg`. Tolak file dengan ekstensi lain.

### 4.6 Max File Size
10MB. Sudah dikonfigurasi di `application.properties`, tapi tetap validasi di service.

---

## 5. Definition of Done

Beri tanda `[x]` jika sudah selesai:

- [ ] `AdminMusicController.java` dibuat
- [ ] `ClientMusicController.java` dibuat
- [ ] `MusicStorageService.java` dibuat
- [ ] `MusicManagementService.java` dibuat
- [ ] Semua DTO dibuat
- [ ] Admin CRUD endpoints (list, get, create, update, update-file, update-status, delete) di-implement
- [ ] Client endpoints (upload custom, list available, list mine, delete mine, assign music, remove assignment, get current) di-implement
- [ ] Admin controller dilindungi `@PreAuthorize("hasRole('ADMIN')")`
- [ ] Client controller dilindungi `@PreAuthorize("hasRole('USER')")`
- [ ] File validation (extension, size)
- [ ] File path generation dengan UUID
- [ ] Music assignment update `primaryMusicId` di invitation
- [ ] Custom music terpisah dari library global
- [ ] File dihapus dari storage saat delete
- [ ] Client hanya bisa delete music miliknya sendiri
- [ ] Validation dengan `@Valid`
- [ ] Kode mengikuti gaya project
- [ ] Tidak ada perubahan ke file di luar scope

---

## 6. Batasan Tegas

- **JANGAN jalankan** `docker`, `docker compose`, `mvn`, `npm run`, `npm start`, `npm test`.
- **JANGAN sentuh file di folder `Sql/`**.
- **JANGAN ubah** `entity/Music.java` — sudah ada.
- **JANGAN ubah** `entity/Invitation.java` — sudah ada.
- **JANGAN ubah** `config/SecurityConfig.java`.
- **JANGAN tambahkan** dependency baru. Jika butuh file processing, gunakan API bawaan JDK.
- **JANGAN pakai** `ddl-auto=update` atau `create`.
- Task ini HANYA membuat service + controller + DTO. Schema tidak disentuh.

---

## 7. Catatan Referensi

- Detail musik ada di `Docs/PRD-Undangan-Online.md` bagian 4.1 (Super Admin: kelola musik), 4.2 (Client: pilih musik), dan 5.5 (halaman tamu: musik latar).
- PRD bagian 9: Musik bisa diupload sendiri oleh client dan hanya digunakan oleh client tersebut.
- Entity `Music` di `Backend/src/main/java/com/undangan/online/entity/Music.java` — cek field: `title`, `artist`, `filePath`, `fileSize`, `status`.
- Entity `Invitation` punya `primaryMusicId`, `customMusicPath`, `customMusicTitle`.
- `StorageConfig.java` sudah punya bean `musicsStoragePath()` untuk path storage.
- Musik disimpan di folder `Musics/` (volume mount). File audio served oleh frontend/nginx.
- Jika `Music` entity TIDAK punya kolom `clientId`, gunakan path prefix `custom/{clientId}/` untuk membedakan musik client.
