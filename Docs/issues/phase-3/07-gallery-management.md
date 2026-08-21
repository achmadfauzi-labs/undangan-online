# Fase 3.7 — Image/Gallery Management

## 1. Judul & Ringkasan

**Judul:** Backend — Modul Image/Gallery Management

**Ringkasan:** Buat modul backend untuk Client mengelola galeri foto invitation: upload gambar ke folder `images/`, CRUD entries galeri, serta upload foto profil pasangan. Gambar disimpan sebagai file di storage, di database hanya metadata (Gallery entity dan InvitationPerson.photoPath).

---

## 2. Scope — File yang BOLEH Disentuh

Agent HANYA boleh membuat/mengedit file dalam folder ini:

```
Backend/src/main/java/com/undangan/online/
├── controller/
│   └── ClientGalleryController.java        # BARU
├── service/
│   ├── ImageStorageService.java           # BARU — upload/delete file gambar
│   └── GalleryService.java                # BARU — CRUD gallery entries
├── dto/
│   ├── GalleryDto.java                    # BARU
│   ├── CreateGalleryRequest.java          # BARU
│   └── UpdateGalleryRequest.java          # BARU
```

**BOLEH dibaca (referensi):**
- `entity/Gallery.java` — sudah ada
- `entity/InvitationPerson.java` — sudah ada
- `entity/Invitation.java` — sudah ada
- `repository/GalleryRepository.java` — **cek apakah sudah ada; jika belum buat baru**
- `repository/InvitationPersonRepository.java` — **cek apakah sudah ada; jika belum buat baru**
- `repository/InvitationRepository.java` — sudah ada
- `config/StorageConfig.java` — untuk memahami path storage
- `config/SecurityConfig.java`
- `exception/GlobalExceptionHandler.java`
- `dto/ErrorResponse.java`
- `exception/AuthException.java`
- `controller/AuthController.java`

**Catatan Repository:** Jika `GalleryRepository` belum ada, buat dalam scope ini. JpaRepository standar dengan query: `findByInvitationId`, `findByInvitationIdOrderBySortOrderAsc`, `findById`.

**JANGAN sentuh file lain.**

---

## 3. Requirement Fungsional

### 3.1 Gallery CRUD

#### 3.1.1 List Gallery (GET /api/v1/client/invitation/gallery)
- Ambil semua gallery entries milik invitation client. Di-sort by `sortOrder` ASC.
- Response: id, imagePath, caption, sortOrder, createdAt.

#### 3.1.2 Get Gallery Entry (GET /api/v1/client/invitation/gallery/{id})
- Ambil satu gallery entry. Validasi kepemilikan (gallery.invitation.clientId == user.clientId).
- Return 404 jika tidak ditemukan.

#### 3.1.3 Create Gallery Entry with Upload (POST /api/v1/client/invitation/gallery)
- Body: multipart/form-data dengan fields:
  - `imageFile` (file, wajib — format: jpg, jpeg, png, webp; max 5MB)
  - `caption` (string, opsional)
  - `sortOrder` (integer, opsional — jika tidak disediakan, set ke max+1)
- Validasi: `imageFile` wajib, ekstensi harus salah satu dari `jpg`, `jpeg`, `png`, `webp`.
- `invitationId` diambil dari invitation client.
- Simpan file ke `images/{clientId}/{invitationId}/`.
- Naming: `{uuid}_{originalFilename}`.
- Set `imagePath` = path relatif dari storage root (misal: `images/5/12/abc123_foto1.jpg`).
- `createdAt` = `OffsetDateTime.now()`.
- Response: data gallery entry yang baru dibuat.

#### 3.1.4 Update Gallery Entry (PUT /api/v1/client/invitation/gallery/{id}`)
- Update: `caption`, `sortOrder`.
- Ganti gambar: Body multipart/form-data dengan `imageFile` opsional. Jika disediakan, replace file gambar.
- Validasi kepemilikan.
- `updatedAt` auto-update.

#### 3.1.5 Delete Gallery Entry (DELETE /api/v1/client/invitation/gallery/{id}`)
- Hard delete dari tabel `gallery`.
- Hapus file gambar dari storage.
- Validasi kepemilikan.
- Return 204.

#### 3.1.6 Reorder Gallery (PATCH /api/v1/client/invitation/gallery/reorder)
- Bulk update sortOrder. Body: `{ order: [{ id: number, sortOrder: number }, ...] }`.
- Validasi semua entries milik invitation client.

#### 3.1.7 Bulk Upload Gallery (POST /api/v1/client/invitation/gallery/bulk)
- Upload multiple gambar sekaligus. Body: multipart/form-data dengan:
  - `images` (multiple files, array — max 20 files, max 5MB per file)
  - `captions` (opsional — array caption yang match dengan urutan images)
- Set `sortOrder` auto-increment untuk setiap entry.
- Simpan semua ke DB dan storage.
- Response: jumlah berhasil + list gallery entry IDs.

---

### 3.2 Photo Upload — InvitationPerson (Foto Profil Pasangan)

#### 3.2.1 Upload Person Photo (POST /api/v1/client/invitation/persons/{personId}/photo)
- Upload foto profil salah satu pasangan.
- Body: multipart/form-data dengan field `photoFile`.
- Validasi: ekstensi `jpg`, `jpeg`, `png`, `webp`; max 2MB.
- Simpan ke `images/{clientId}/persons/`.
- Naming: `person_{personId}_{uuid}.{ext}`.
- Update `photoPath` di tabel `invitation_person`.
- Hapus file foto lama jika ada.
- Response: `{ success: true, photoPath: "..." }`.

#### 3.2.2 Delete Person Photo (DELETE /api/v1/client/invitation/persons/{personId}/photo)
- Hapus foto profil. Set `photoPath = null` di tabel.
- Hapus file dari storage.
- Validasi kepemilikan person (person.invitation.clientId == user.clientId).

---

### 3.3 Public Gallery (Read-Only)

#### 3.3.1 Get Gallery by Slug (GET /api/v1/public/invitation/{slug}/gallery)
- Ambil gallery publik suatu invitation.
- Lookup invitation by slug.
- Return hanya entries dengan `sortOrder` > 0 (semua entries).
- Response: array of `{ imagePath, caption }`. Hanya field publik.
- Tidak perlu pagination — jumlah foto galeri terbatas.

---

## 4. Requirement Teknis

### 4.1 Struktur Package
```
com.undangan.online.controller.ClientGalleryController
com.undangan.online.service.ImageStorageService
com.undangan.online.service.GalleryService
com.undangan.online.dto.GalleryDto
com.undangan.online.dto.CreateGalleryRequest
com.undangan.online.dto.UpdateGalleryRequest
```

### 4.2 Pola Kode
- **Controller**: konstruktor injection, `@PreAuthorize("hasRole('USER')")` untuk semua endpoint Client, endpoint public di `/api/v1/public/**` tanpa auth.
- **Service**: konstruktor injection, `@Service`. `ImageStorageService` untuk operasi file (upload, delete, replace). `GalleryService` untuk CRUD database.
- **File validation**: cek ekstensi, cek size, generate UUID filename, validasi path traversal (no `..`).

### 4.3 Konvensi Environment Variable
Storage path gambar dari `application.properties`:
```
app.storage.images=${STORAGE_IMAGES_PATH:/app/storage/images}
```
Max image size sudah dikonfigurasi di `application.properties` (`MAX_IMAGE_SIZE_MB:5`).

### 4.4 File Naming & Storage
```
// Gallery: {storageRoot}/images/{clientId}/{invitationId}/{uuid}_{filename}
// Person photo: {storageRoot}/images/{clientId}/persons/person_{personId}_{uuid}.{ext}
```

### 4.5 Allowed Extensions
`jpg`, `jpeg`, `png`, `webp`. Tolak format lain.

### 4.6 Max File Sizes
- Gallery: 5MB per file
- Person photo: 2MB

---

## 5. Definition of Done

Beri tanda `[x]` jika sudah selesai:

- [ ] `ClientGalleryController.java` dibuat
- [ ] `ImageStorageService.java` dibuat
- [ ] `GalleryService.java` dibuat
- [ ] Semua DTO dibuat
- [ ] `GalleryRepository` dibuat (jika belum ada)
- [ ] `InvitationPersonRepository` dibuat (jika belum ada)
- [ ] CRUD gallery endpoints (list, get, create, update, delete, reorder, bulk upload) di-implement
- [ ] Person photo upload & delete endpoints di-implement
- [ ] Public gallery endpoint (read-only) di-implement
- [ ] Semua client endpoint dilindungi `@PreAuthorize("hasRole('USER')")`
- [ ] Authorization check: client hanya bisa akses data miliknya
- [ ] File validation (extension, size per use case)
- [ ] File replacement (update dengan file baru, hapus file lama)
- [ ] File dihapus dari storage saat delete entry/photo
- [ ] Bulk upload dengan multiple files
- [ ] SortOrder auto-increment saat bulk create
- [ ] Path traversal protection (validasi no `..` di filename)
- [ ] Validation dengan `@Valid`
- [ ] Kode mengikuti gaya project
- [ ] Tidak ada perubahan ke file di luar scope

---

## 6. Batasan Tegas

- **JANGAN jalankan** `docker`, `docker compose`, `mvn`, `npm run`, `npm start`, `npm test`.
- **JANGAN sentuh file di folder `Sql/`**.
- **JANGAN ubah** `entity/Gallery.java` — sudah ada.
- **JANGAN ubah** `entity/InvitationPerson.java` — sudah ada.
- **JANGAN ubah** `entity/Invitation.java`.
- **JANGAN ubah** `config/SecurityConfig.java`.
- **JANGAN tambahkan** dependency baru.
- **JANGAN pakai** `ddl-auto=update` atau `create`.
- Task ini HANYA membuat service + controller + DTO + repository. Schema tidak disentuh.

---

## 7. Catatan Referensi

- Detail galeri ada di `Docs/PRD-Undangan-Online.md` bagian 4.2 (Client: upload & kelola galeri) dan 5.1 (halaman tamu: galeri foto).
- PRD bagian 7: Upload file disimpan sebagai file di storage (folder `images/`), bukan blob di database.
- Entity `Gallery` di `Backend/src/main/java/com/undangan/online/entity/Gallery.java` — cek field: `invitationId`, `imagePath`, `caption`, `sortOrder`.
- Entity `InvitationPerson` punya `photoPath` untuk foto profil pasangan.
- `StorageConfig.java` punya bean `imagesStoragePath()` untuk path storage.
- Gambar disimpan di `Images/` (volume mount). File served oleh frontend/nginx.
- Jika bulk upload lebih dari 20 files, return 400 dengan message.
- `sortOrder` default saat tidak disediakan: MAX(sortOrder) + 1, atau 1 jika gallery kosong.
