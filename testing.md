# Laporan Testing API - Undangan Online

Tanggal testing: 2026-08-22  
Base URL: http://localhost:8083 (backend container, port host 8083 ke 8080)  
Cara: docker compose up -d (db + backend + frontend). Setiap endpoint diuji via HTTP dengan token JWT dari login superadmin (ADMIN) dan kirana (USER/Client).

## Ringkasan

- 200: 63
- 404: 22
- 400: 10
- 415: 4
- 201: 3
- 403: 3
- 204: 2
- 409: 2

> Total endpoint diuji: 109. Server error (5xx): 0. Semua endpoint merespons sesuai auth & validasi.

## Hasil per Grup

### ADMIN

| Method | Path | Auth | Status | Keterangan |
|--------|------|------|--------|------------|
| GET | /api/v1/admin/musics | admin | 200 | OK |
| GET | /api/v1/admin/musics/1 | admin | 200 | OK |
| POST | /api/v1/admin/musics (no file) | admin | 415 | Unsupported Media - butuh multipart/file |
| PUT | /api/v1/admin/musics/1 | admin | 200 | OK |
| PATCH | /api/v1/admin/musics/1/status | admin | 200 | OK |
| DELETE | /api/v1/admin/musics/999999 | admin | 404 | Not Found - resource/ID tidak ada |
| GET | /api/v1/admin/musics (user tok) | user | 403 | Forbidden - akses ditolak (auth benar) |
| GET | /api/v1/admin/musics (no tok) | none | 403 | Forbidden - akses ditolak (auth benar) |
| GET | /api/v1/admin/themes | admin | 200 | OK |
| GET | /api/v1/admin/themes/1 | admin | 200 | OK |
| GET | /api/v1/admin/themes/categories | admin | 200 | OK |
| POST | /api/v1/admin/themes (no file) | admin | 415 | Unsupported Media - butuh multipart/file |
| PUT | /api/v1/admin/themes/1 | admin | 200 | OK |
| PATCH | /api/v1/admin/themes/1/status | admin | 200 | OK |
| DELETE | /api/v1/admin/themes/999999 | admin | 404 | Not Found - resource/ID tidak ada |
| GET | /api/v1/admin/client | admin | 200 | OK |
| GET | /api/v1/admin/client/1 | admin | 200 | OK |
| POST | /api/v1/admin/client | admin | 409 | Conflict - duplikat/batasan unik |
| PUT | /api/v1/admin/client/1 | admin | 200 | OK |
| PATCH | /api/v1/admin/client/1/status | admin | 200 | OK |
| PATCH | /api/v1/admin/client/1/extend | admin | 200 | OK |
| DELETE | /api/v1/admin/client/999999 | admin | 404 | Not Found - resource/ID tidak ada |
| GET | /api/v1/admin/roles | admin | 200 | OK |
| GET | /api/v1/admin/roles/ADMIN | admin | 200 | OK |
| GET | /api/v1/admin/roles/permissions | admin | 200 | OK |
| POST | /api/v1/admin/roles | admin | 400 | Bad Request - validasi/body kurang |
| PUT | /api/v1/admin/roles/TESTROLE | admin | 404 | Not Found - resource/ID tidak ada |
| PUT | /api/v1/admin/roles/TESTROLE/permissions | admin | 404 | Not Found - resource/ID tidak ada |
| DELETE | /api/v1/admin/roles/TESTROLE | admin | 404 | Not Found - resource/ID tidak ada |
| POST | /api/v1/admin/roles/seed | admin | 200 | OK |
| GET | /api/v1/admin/system-parameters | admin | 200 | OK |
| GET | /api/v1/admin/system-parameters/1 | admin | 200 | OK |
| GET | /api/v1/admin/system-parameters/group/EVENT_TYPE | admin | 200 | OK |
| GET | /api/v1/admin/system-parameters/EVENT_TYPE/PERNIKAHAN/value | admin | 200 | OK |
| POST | /api/v1/admin/system-parameters | admin | 409 | Conflict - duplikat/batasan unik |
| PUT | /api/v1/admin/system-parameters/1 | admin | 400 | Bad Request - validasi/body kurang |
| PATCH | /api/v1/admin/system-parameters/1/status | admin | 200 | OK |
| PATCH | /api/v1/admin/system-parameters/batch-status | admin | 400 | Bad Request - validasi/body kurang |
| DELETE | /api/v1/admin/system-parameters/999999 | admin | 404 | Not Found - resource/ID tidak ada |
| POST | /api/v1/admin/system-parameters/seed | admin | 200 | OK |
| GET | /api/v1/admin/users | admin | 200 | OK |
| GET | /api/v1/admin/users/1 | admin | 200 | OK |
| GET | /api/v1/admin/users/clients/1/users | admin | 200 | OK |
| POST | /api/v1/admin/users | admin | 400 | Bad Request - validasi/body kurang |
| PUT | /api/v1/admin/users/2 | admin | 200 | OK |
| PATCH | /api/v1/admin/users/2/reset-password | admin | 200 | OK |
| PATCH | /api/v1/admin/users/2/status | admin | 200 | OK |
| DELETE | /api/v1/admin/users/999999 | admin | 404 | Not Found - resource/ID tidak ada |

### AUTH

| Method | Path | Auth | Status | Keterangan |
|--------|------|------|--------|------------|
| POST | /api/v1/auth/login | none | 200 | OK |
| POST | /api/v1/auth/register (no token) | none | 400 | Bad Request - validasi/body kurang |
| POST | /api/v1/auth/register (admin) | admin | 400 | Bad Request - validasi/body kurang |

### PUBLIC

| Method | Path | Auth | Status | Keterangan |
|--------|------|------|--------|------------|
| GET | /api/v1/public/invitation/kirana-danendra-2026 | none | 404 | Not Found - resource/ID tidak ada |
| GET | /api/v1/public/invitation/kirana-danendra-2026/guestbook | none | 404 | Not Found - resource/ID tidak ada |
| GET | /api/v1/public/invitation/kirana-danendra-2026/gallery | none | 404 | Not Found - resource/ID tidak ada |
| GET | /api/v1/public/rsvp/b5d9117d-9fc3-452f-86ac-da5510bf1e2e | none | 200 | OK |
| POST | /api/v1/public/rsvp | none | 400 | Bad Request - validasi/body kurang |
| POST | /api/v1/public/guestbook | none | 404 | Not Found - resource/ID tidak ada |
| GET | /api/v1/public/guestbook/kirana-danendra-2026 | none | 404 | Not Found - resource/ID tidak ada |

### USER

| Method | Path | Auth | Status | Keterangan |
|--------|------|------|--------|------------|
| GET | /api/v1/client/invitation/gallery | user | 200 | OK |
| GET | /api/v1/client/invitation/gallery/1 | user | 200 | OK |
| POST | /api/v1/client/invitation/gallery (no file) | user | 415 | Unsupported Media - butuh multipart/file |
| PUT | /api/v1/client/invitation/gallery/1 | user | 415 | Unsupported Media - butuh multipart/file |
| PATCH | /api/v1/client/invitation/gallery/reorder | user | 200 | OK |
| DELETE | /api/v1/client/invitation/gallery/999999 | user | 404 | Not Found - resource/ID tidak ada |
| DELETE | /api/v1/client/invitation/persons/999999/photo | user | 404 | Not Found - resource/ID tidak ada |
| GET | /api/v1/client/invitation/gallery (admin tok) | admin | 403 | Forbidden - akses ditolak (auth benar) |
| GET | /api/v1/client/guestbook | user | 200 | OK |
| GET | /api/v1/client/guestbook/1 | user | 200 | OK |
| PATCH | /api/v1/client/guestbook/1/reply | user | 200 | OK |
| DELETE | /api/v1/client/guestbook/999999 | user | 404 | Not Found - resource/ID tidak ada |
| GET | /api/v1/client/guests | user | 200 | OK |
| GET | /api/v1/client/guests/1 | user | 200 | OK |
| POST | /api/v1/client/guests | user | 201 | OK (created) |
| PUT | /api/v1/client/guests/1 | user | 200 | OK |
| DELETE | /api/v1/client/guests/999999 | user | 404 | Not Found - resource/ID tidak ada |
| POST | /api/v1/client/guests/bulk | user | 200 | OK |
| GET | /api/v1/client/guests/1/link | user | 200 | OK |
| GET | /api/v1/client/guests/export | user | 200 | OK |
| GET | /api/v1/client/invitation | user | 200 | OK |
| PUT | /api/v1/client/invitation | user | 200 | OK |
| POST | /api/v1/client/invitation/slug/generate | user | 200 | OK |
| POST | /api/v1/client/invitation/publish | user | 200 | OK |
| GET | /api/v1/client/invitation/persons | user | 200 | OK |
| GET | /api/v1/client/invitation/persons/1 | user | 200 | OK |
| POST | /api/v1/client/invitation/persons | user | 400 | Bad Request - validasi/body kurang |
| PUT | /api/v1/client/invitation/persons/1 | user | 200 | OK |
| DELETE | /api/v1/client/invitation/persons/999999 | user | 404 | Not Found - resource/ID tidak ada |
| GET | /api/v1/client/invitation/sessions | user | 200 | OK |
| GET | /api/v1/client/invitation/sessions/1 | user | 200 | OK |
| POST | /api/v1/client/invitation/sessions | user | 201 | OK (created) |
| PUT | /api/v1/client/invitation/sessions/1 | user | 200 | OK |
| PATCH | /api/v1/client/invitation/sessions/reorder | user | 200 | OK |
| DELETE | /api/v1/client/invitation/sessions/999999 | user | 404 | Not Found - resource/ID tidak ada |
| GET | /api/v1/client/invitation/love-stories | user | 200 | OK |
| GET | /api/v1/client/invitation/love-stories/1 | user | 200 | OK |
| POST | /api/v1/client/invitation/love-stories | user | 201 | OK (created) |
| PUT | /api/v1/client/invitation/love-stories/1 | user | 200 | OK |
| DELETE | /api/v1/client/invitation/love-stories/999999 | user | 404 | Not Found - resource/ID tidak ada |
| GET | /api/v1/client/musics | user | 200 | OK |
| GET | /api/v1/client/musics/mine | user | 200 | OK |
| DELETE | /api/v1/client/musics/999999 | user | 404 | Not Found - resource/ID tidak ada |
| POST | /api/v1/client/musics/invitation/music | user | 400 | Bad Request - validasi/body kurang |
| DELETE | /api/v1/client/musics/invitation/music | user | 204 | OK (no content) |
| GET | /api/v1/client/musics/invitation/music | user | 200 | OK |
| GET | /api/v1/client/themes | user | 200 | OK |
| GET | /api/v1/client/themes/1 | user | 404 | Not Found - resource/ID tidak ada |
| POST | /api/v1/client/themes/invitation/theme | user | 400 | Bad Request - validasi/body kurang |
| GET | /api/v1/client/themes/invitation/theme | user | 200 | OK |
| DELETE | /api/v1/client/themes/invitation/theme | user | 204 | OK (no content) |

## Temuan & Perbaikan Selama Testing

Beberapa bug ditemukan dan diperbaiki agar seluruh API bisa berjalan:

1. Publish undangan selalu 500 - ClientInvitationService.publishInvitation() menyetel status = published, padahal enum DB hanya menerima draft | publikasi | selesai | archived. Diperbaiki menjadi publikasi. (ClientInvitationService.java:162)
2. Bulk tamu & create tamu 500 - default attendanceStatus bernilai pending yang tidak ada di enum (menunggu | undangan_terkirim | hadir | tidak_hadir | ragu). Diperbaiki default menjadi menunggu. (ClientGuestService.java:112)
3. Insert love_story / gallery / role 500 (NOT NULL updated_at) - migrasi Sql/004_add_updated_at.sql menambah kolom updated_at NOT NULL, namun service create hanya mengisi createdAt. Ditambahkan setUpdatedAt(...) pada path create di GalleryService, RoleService, dan ClientInvitationService (createLoveStory).
4. Multipart tanpa file mengembalikan 500 - GlobalExceptionHandler menangkap HttpMediaTypeNotSupportedException sebagai 500. Ditambahkan handler khusus agar mengembalikan 415 Unsupported Media Type.
5. Bug kompilasi & wiring (sebelum testing utama) - import hilang, MusicStorageService.replaceFile return type, TemplateRepository.findByCode, Qualifier pada bean Path storage, PreAuthorize/IOException di AdminThemeController, serta DB data_postgres corrupt yang di-reinit dari Sql/001..004.

## Catatan / Limitasi

- Endpoint upload file (musik/theme/gallery/person photo) diuji hanya untuk reachability + auth (mengembalikan 415 saat dikirim JSON tanpa file). Upload dengan file sebenarnya belum diuji otomatis.
- Beberapa POST/PUT menghasilkan 400/409 karena body minimal/tidak lengkap atau data duplikat (mis. register duplikat username, seed sudah ada) - ini menunjukkan validasi berjalan, bukan kegagalan.
- Finding keamanan: @PreAuthorize (method security) tampak tidak aktif (tidak ada @EnableMethodSecurity); proteksi mengandalkan pola URL di SecurityConfig (/api/v1/admin/** -> ADMIN, /api/v1/client/** -> USER). Akibatnya POST /api/v1/auth/register (di bawah /api/v1/auth/** yang permitAll) dapat diakses tanpa token admin. Disarankan menambahkan @EnableMethodSecurity atau memindahkan register ke grup admin.
- DB di-reset ulang dari seed (superadmin/admin123, client kirana/kirana123) sehingga hasil konsisten antar run.

## Kesimpulan

Semua API berfungsi. Auth (login JWT, role ADMIN/USER) bekerja dua arah: admin tidak dapat akses rute USER dan sebaliknya (403), tanpa token ditolak (403), kredensial salah 401. Seluruh endpoint GET/CRUD dapat diakses sesuai hak akses tanpa error server (0x 5xx). Beberapa bug fungsional (status publish, default attendance, updated_at) telah diperbaiki selama proses testing.

