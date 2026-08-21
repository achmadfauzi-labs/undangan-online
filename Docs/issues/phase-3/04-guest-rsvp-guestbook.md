# Fase 3.4 — Guest List & RSVP + Guestbook

## 1. Judul & Ringkasan

**Judul:** Backend — Modul Guest List, RSVP & Guestbook

**Ringkasan:** Buat modul backend untuk Guest List management (Client-side), RSVP submission (Guest/public), dan Guestbook (Guest/public + Client moderation). Ada 3 kelompok endpoint:
1. `/api/v1/client/guests` — Client mengelola guest list (CRUD)
2. `/api/v1/public/rsvp` — Tamu submitting RSVP (tanpa login)
3. `/api/v1/client/guestbook` — Client moderation guestbook

---

## 2. Scope — File yang BOLEH Disentuh

Agent HANYA boleh membuat/mengedit file dalam folder ini:

```
Backend/src/main/java/com/undangan/online/
├── controller/
│   ├── ClientGuestController.java         # BARU — CRUD guest list oleh Client
│   ├── PublicRsvpController.java          # BARU — RSVP submission (public)
│   └── ClientGuestbookController.java     # BARU — Guestbook moderation oleh Client
├── service/
│   ├── ClientGuestService.java            # BARU
│   ├── PublicRsvpService.java             # BARU
│   └── ClientGuestbookService.java        # BARU
├── dto/
│   ├── GuestDto.java                      # BARU
│   ├── CreateGuestRequest.java            # BARU
│   ├── UpdateGuestRequest.java            # BARU
│   ├── RsvpSubmitRequest.java             # BARU
│   ├── RsvpUpdateRequest.java             # BARU
│   ├── GuestbookEntryDto.java             # BARU
│   └── GuestbookModerationDto.java        # BARU
```

**BOLEH dibaca (referensi):**
- `entity/Guest.java` — sudah ada
- `entity/Invitation.java` — sudah ada
- `repository/GuestRepository.java` — **cek apakah sudah ada; jika belum buat baru**
- `repository/InvitationRepository.java` — sudah ada
- `controller/PublicInvitationController.java` — untuk memahami pola endpoint public
- `controller/AuthController.java` — pola controller
- `security/JwtTokenProvider.java`
- `exception/GlobalExceptionHandler.java`
- `dto/ErrorResponse.java`
- `exception/AuthException.java`

**Catatan Repository:** Jika `GuestRepository` belum ada di `repository/`, buat dalam scope ini. JpaRepository standar dengan query method `findByInvitationId` dan `findByInvitationIdAndIsPublishedTrue`.

**JANGAN sentuh file lain.**

---

## 3. Requirement Fungsional

### 3.1 Guest List Management — Client (CRUD)

#### 3.1.1 List Guests (GET /api/v1/client/guests)
- Ambil semua guest milik invitation client yang login. Supports pagination.
- Query param opsional: `status` (attending/not_attending/pending), `search` (cari by name).
- Response: id, invitationToken, name, categoryCode, email, phone, attendanceStatus, partySize, isPublished, createdAt.
- Di-sort by `createdAt` DESC.

#### 3.1.2 Get Guest (GET /api/v1/client/guests/{id})
- Ambil detail satu guest. Validasi kepemilikan (guest.invitation.clientId == user.clientId).
- Return 404 jika tidak ditemukan.

#### 3.1.3 Create Guest (POST /api/v1/client/guests)
- Body: `{ name, categoryCode?, email?, phone?, attendanceStatus?, partySize?, invitationToken? }`.
- Validasi: `name` wajib.
- `invitationId` diambil dari invitation client yang login.
- `invitationToken`: jika tidak disediakan, generate UUID baru (64 char).
- `attendanceStatus` default = `pending`.
- `partySize` default = 1.
- `isPublished` default = true.
- `createdAt`/`updatedAt` = `OffsetDateTime.now()`.
- `message` dan `reply` null saat create.
- Token uniqueness: cek `invitationToken` unik per invitation. Jika duplicate, generate yang baru.

#### 3.1.4 Update Guest (PUT /api/v1/client/guests/{id}`)
- Update: name, categoryCode, email, phone, partySize.
- `attendanceStatus` TIDAK bisa diubah lewat endpoint ini — ada endpoint terpisah (untuk RSVP guest).
- Validasi kepemilikan.
- `updatedAt` auto-update.

#### 3.1.5 Delete Guest (DELETE /api/v1/client/guests/{id}`)
- Hard delete dari tabel `guest`. Validasi kepemilikan. Return 204.

#### 3.1.6 Bulk Create Guests (POST /api/v1/client/guests/bulk)
- Body: `{ guests: [{ name, categoryCode?, email?, phone? }, ...] }`.
- Create multiple guests sekaligus. Maks 100 guests per request.
- Generate `invitationToken` unik untuk setiap guest.
- `partySize` default 1, `attendanceStatus` default `pending`.
- Response: jumlah yang berhasil dibuat + list guest ID.

#### 3.1.7 Generate Guest Link (GET /api/v1/client/guests/{id}/link)
- Generate link undangan dengan token untuk satu guest.
- Format: `{ baseUrl: "https://domain.com", fullUrl: "https://domain.com/undangan/{slug}?to={name}&token={invitationToken}" }`.
- `slug` diambil dari invitation client.

#### 3.1.8 Export Guest List (GET /api/v1/client/guests/export)
- Return semua guest tanpa pagination (untuk export). Content-Type: `application/json`.
- Format array of objects dengan field yang relevan.
- Validasi kepemilikan.

---

### 3.2 RSVP Submission — Public (Guest/Tamu)

#### 3.2.1 Submit RSVP (POST /api/v1/public/rsvp)
- Guestsubmit RSVP menggunakan invitation token. TANPA login.
- Body: `{ token: string, attendanceStatus: "attending" | "not_attending", partySize?, message? }`.
- Validasi: `token` wajib dan harus ditemukan di tabel `guest`.
- `attendanceStatus`: `attending` atau `not_attending`.
- `partySize`: jika `attending`, wajib > 0. Default = 1 jika attending dan tidak disediakan.
- `message`: opsional (untuk guestbook/guestbook use case).
- Jika guest sudah pernah RSVP (attendanceStatus bukan `pending`), UPDATE jawaban (overwrite).
- `updatedAt` auto-update.
- Response: `{ success: true, message: "RSVP berhasil tercatat" }`.

#### 3.2.2 Get RSVP Status (GET /api/v1/public/rsvp/{token})
- Guest cek status RSVP mereka sendiri.
- Response: `{ name, attendanceStatus, partySize, message }` atau 404 jika token tidak valid.

---

### 3.3 Guestbook — Public + Client Moderation

#### 3.3.1 Submit Guestbook (POST /api/v1/public/guestbook)
- Tamu isi guestbook. TANPA login. Berbeda dari RSVP — guestbook adalah kolom ucapan/doa.
- Body: `{ slug: string, name: string, message: string }`.
- Validasi: `name` wajib, `message` wajib (min 3 char, max 500 char).
- Lookup invitation by `slug`.
- Cek apakah guest sudah ada di guest list dengan nama yang sama? Tidak perlu — guestbook bisa diisi oleh siapa saja yang buka link.
- **Guestbook = RSVP guest**: jika ada guest dengan `invitationToken` yang cocok, link ke record guest. Jika tidak ada (tamu umum), buat record guest baru (tanpa token atau dengan generated token).
- Approach: cek apakah `invitationToken` ada di body/param. Jika ada, find/update guest. Jika tidak ada, buat guest baru dengan `invitationToken = UUID.randomUUID()`, `attendanceStatus = pending`.
- `isPublished` = `true` (PRD: langsung publish tanpa moderasi, bagian 9).
- `createdAt`/`updatedAt` = `OffsetDateTime.now()`.
- Response: `{ success: true, message: "Ucapan berhasil dikirim" }`.

#### 3.3.2 Get Guestbook by Slug (GET /api/v1/public/guestbook/{slug})
- Ambil semua guestbook entries untuk satu invitation.
- Hanya entries dengan `isPublished=true`.
- Di-sort by `createdAt` DESC.
- Response: array of `{ name, message, createdAt }` (hanya field publik, BUKAN attendanceStatus/phone/email).

#### 3.3.3 List All Entries (Client) (GET /api/v1/client/guestbook)
- Ambil SEMUA guestbook entries milik invitation client, termasuk `isPublished=false`.
- Supports pagination.
- Query param opsional: `isPublished` (true/false), `search` (cari by name).
- Response lengkap: semua field termasuk `attendanceStatus`, `reply`, `message`.

#### 3.3.4 Get Entry (Client) (GET /api/v1/client/guestbook/{id}`)
- Ambil satu guestbook entry. Validasi kepemilikan.

#### 3.3.5 Reply to Guestbook (PATCH /api/v1/client/guestbook/{id}/reply)
- Client merespons/membalas ucapan guest.
- Body: `{ reply: string }`.
- Validasi kepemilikan.
- `updatedAt` auto-update.

#### 3.3.6 Delete Guestbook Entry (DELETE /api/v1/client/guestbook/{id})
- Hard delete guestbook entry. Validasi kepemilikan. Return 204.

---

## 4. Requirement Teknis

### 4.1 Struktur Package
```
com.undangan.online.controller.ClientGuestController
com.undangan.online.controller.PublicRsvpController
com.undangan.online.controller.ClientGuestbookController
com.undangan.online.service.ClientGuestService
com.undangan.online.service.PublicRsvpService
com.undangan.online.service.ClientGuestbookService
com.undangan.online.dto.GuestDto
com.undangan.online.dto.CreateGuestRequest
com.undangan.online.dto.UpdateGuestRequest
com.undangan.online.dto.RsvpSubmitRequest
com.undangan.online.dto.RsvpUpdateRequest
com.undangan.online.dto.GuestbookEntryDto
com.undangan.online.dto.GuestbookModerationDto
```

### 4.2 Pola Kode
- `PublicRsvpController` dan `PublicGuestbookController` (bagian 3.3.1-3.3.2): endpoint PUBLIC, tidak pakai `@PreAuthorize`, tetapi harus validasi token/invitation existence.
- `ClientGuestController` dan `ClientGuestbookController`: pakai `@PreAuthorize("hasRole('USER')")`.
- Service pattern: konstruktor injection, `@Service`.
- Authorization: client hanya bisa akses data milik invitation yang `clientId`-nya match.

### 4.3 Konvensi Environment Variable
Tidak perlu menambah env var baru.

### 4.4 Token Generation
Untuk `invitationToken`: gunakan `UUID.randomUUID().toString().replace("-", "")` (32 char) atau `java.security.SecureRandom` untuk 64 char. Sesuaikan dengan kolom `invitation_token` di entity (VARCHAR 64).

### 4.5 Guest vs Guestbook
Secara data, guestbook entries ADALAH record di tabel `guest` (sama tabel). Perbedaannya:
- Guest: `attendanceStatus` != null, `invitationToken` != null
- Guestbook: bisa dari guest yang sudah ada RSVP, atau guest baru yang HANYA mengisi ucapan (tanpa RSVP)

Jika RSVP dan guestbook dari tamu yang sama: update existing guest record, jangan buat duplikat. Cek apakah ada guest dengan `name` + `invitationId` yang match.

---

## 5. Definition of Done

Beri tanda `[x]` jika sudah selesai:

- [ ] `ClientGuestController.java` dibuat
- [ ] `PublicRsvpController.java` dibuat
- [ ] `ClientGuestbookController.java` dibuat
- [ ] `ClientGuestService.java` dibuat
- [ ] `PublicRsvpService.java` dibuat
- [ ] `ClientGuestbookService.java` dibuat
- [ ] Semua DTO dibuat
- [ ] `GuestRepository` dibuat (jika belum ada)
- [ ] CRUD guest list endpoints (list, get, create, update, delete, bulk create, generate link, export) di-implement
- [ ] RSVP endpoints (submit, get status) di-implement — public, tanpa auth
- [ ] Guestbook public endpoint (submit, get by slug) di-implement — public
- [ ] Guestbook client endpoints (list, get, reply, delete) di-implement
- [ ] Client guest controller dilindungi `@PreAuthorize("hasRole('USER')")`
- [ ] Client guestbook controller dilindungi `@PreAuthorize("hasRole('USER')")`
- [ ] Authorization check: client hanya bisa akses data miliknya
- [ ] Guestbook langsung publish (`isPublished=true`)
- [ ] RSVP bisa di-update (bukan sekali terkunci — sesuai PRD)
- [ ] Guestbook submit bikin guest record jika belum ada
- [ ] Validation dengan `@Valid`
- [ ] Token uniqueness di-handle saat create
- [ ] Kode mengikuti gaya project
- [ ] Tidak ada perubahan ke file di luar scope

---

## 6. Batasan Tegas

- **JANGAN jalankan** `docker`, `docker compose`, `mvn`, `npm run`, `npm start`, `npm test`.
- **JANGAN sentuh file di folder `Sql/`**.
- **JANGAN ubah** `entity/Guest.java` — sudah ada sesuai schema.
- **JANGAN ubah** `entity/Invitation.java`.
- **JANGAN ubah** `config/SecurityConfig.java` — untuk menambahkan path public/guestbook, cukup gunakan endpoint baru, JANGAN ubah SecurityConfig.
- **JANGAN tambahkan** dependency baru.
- **JANGAN pakai** `ddl-auto=update` atau `create`.
- **JANGAN tambahkan** endpoint RSVP atau guestbook ke `SecurityConfig` — endpoint baru cukup `permitAll()` secara default jika path sudah `/api/v1/public/**`.
- Task ini HANYA membuat service + controller + DTO + repository. Schema tidak disentuh.

---

## 7. Catatan Referensi

- Detail RSVP & guestbook ada di `Docs/PRD-Undangan-Online.md` bagian 4.3 (Guest) dan 5.1.
- PRD bagian 9: RSVP bisa lebih dari sekali (update jawaban). Guestbook langsung publish tanpa moderasi.
- PRD bagian 9: Guestbook entries tidak perlu moderation — langsung tampil.
- Entity `Guest` di `Backend/src/main/java/com/undangan/online/entity/Guest.java` — cek field `attendanceStatus`, `isPublished`, `reply`.
- Aturan umum: `Docs/CLAUDE.md`.
- Endpoint public di `PublicInvitationController.java` sudah `permitAll` di SecurityConfig, pattern yang sama berlaku untuk `/api/v1/public/rsvp/**` dan `/api/v1/public/guestbook/**`.
- `InvitationRepository.findBySlug(slug)` sudah ada untuk lookup invitation by slug.
- Untuk bulk create: jika ada guest dengan nama duplikat dalam satu invitation, tetap boleh (bedakan oleh ID, bukan nama).
