# Issue B-4 — Retrofit Modul Guest, RSVP, dan Guestbook

> **Audience:** AI agent junior (model kecil). Agent **SELALU** menjalankan `docker`, `docker compose`, `mvn`, `npm`, atau perintah build/run/test apa pun ketika sesudah menyelesaikan issue.
>
> **Prasyarat:** Issue A selesai.
>
> **Acuan utama:**
> - `Docs/CLAUDE.md` bagian **#6**, **#7**, **#8**
> - `Docs/PRD-Undangan-Online.md` bagian 5/6 (Use Case Client & Guest — Guest/RSVP/Guestbook)
> - `Docs/issues/phase-3/04-guest-rsvp-guestbook.md`

---

## 1. Ringkasan & Alasan Retrofit

Modul ini punya **3 subdomain** yang saling terkait tapi dari sisi retrofit berdiri sendiri:
1. **Guest** — client mengelola daftar tamu (`Guest` entity) untuk tiap invitation.
2. **RSVP** — guest publik mengisi konfirmasi kehadiran (`attendance_status` di Guest), via `/api/v1/public/invitation/{slug}/rsvp`.
3. **Guestbook** — guest publik menulis ucapan (`is_published` flag, moderasi).

**Controller terkait (sudah ada di project):**
- `ClientGuestController` — client CRUD guest
- `ClientGuestbookController` — client moderasi guestbook
- `PublicRsvpController` — guest submit RSVP (publik, tanpa login)

**Yang perlu diperbaiki:**
1. Ketiga controller belum pakai `ApiResponse<T>` wrapper konsisten.
2. `ClientGuestService`, `ClientGuestbookService`, `PublicRsvpService` belum interface + impl.
3. Validasi `invitationToken` di RSVP publik — harus dicek (apakah token-based atau slug-based).
4. Logging di create/update/delete guest + publish/unpublish guestbook.
5. Data sensitif: nama tamu, pesan guestbook — perlakukan sesuai CLAUDE.md #8 (log dengan masking atau hindari full text).

**TIDAK termasuk issue ini:**
- Modul lain (Invitation — Issue B-3, Theme — Issue B-5, dst)

---

## 2. Scope — File/Folder yang Boleh Disentuh

### File yang BOLEH diedit (existing):
- `Backend/src/main/java/com/undangan/online/controller/ClientGuestController.java`
- `Backend/src/main/java/com/undangan/online/controller/ClientGuestbookController.java`
- `Backend/src/main/java/com/undangan/online/controller/PublicRsvpController.java`
- `Backend/src/main/java/com/undangan/online/service/ClientGuestService.java`
- `Backend/src/main/java/com/undangan/online/service/ClientGuestbookService.java`
- `Backend/src/main/java/com/undangan/online/service/PublicRsvpService.java`
- `Backend/src/main/java/com/undangan/online/dto/CreateGuestRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/UpdateGuestRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/GuestDto.java`
- `Backend/src/main/java/com/undangan/online/dto/RsvpSubmitRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/RsvpUpdateRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/GuestbookEntryDto.java`
- `Backend/src/main/java/com/undangan/online/dto/GuestbookModerationDto.java`

### File BARU yang WAJIB dibuat:
- `Backend/src/main/java/com/undangan/online/service/impl/ClientGuestServiceImpl.java`
- `Backend/src/main/java/com/undangan/online/service/impl/ClientGuestbookServiceImpl.java`
- `Backend/src/main/java/com/undangan/online/service/impl/PublicRsvpServiceImpl.java`

### DILARANG keras:
- Folder `Sql/`
- Folder `Docs/`, `Frontend/`, `Database/`
- File di package `entity/`, `repository/`
- Controller/service lain di luar Scope #2
- `ApiResponse.java`

---

## 3. Checklist Audit

### 3.1 Audit Controller
- [ ] Daftar endpoint di `ClientGuestController`, `ClientGuestbookController`, `PublicRsvpController`.
- [ ] Return type: `ApiResponse<T>` atau plain / `Map.of(...)`?
- [ ] `@PreAuthorize` — `PublicRsvpController` harus TANPA `@PreAuthorize` (publik, bebas akses).
- [ ] Business logic di controller?

### 3.2 Audit Service
- [ ] Ketiga service: class langsung atau interface?
- [ ] Method ownership validation: apakah `clientId` selalu dari JWT?
- [ ] RSVP service: bagaimana lookup Guest? By `invitationToken`? By invitation ID + token kombinasi? By slug?
- [ ] Guestbook publik: siapa yang boleh baca published entries? Siapa yang boleh submit? (Public — tanpa login.)

### 3.3 Audit Logging
- [ ] Logger di ketiga service?
- [ ] Log untuk create/delete guest, submit RSVP, submit guestbook, moderasi (publish/unpublish)?
- [ ] **PENTING:** pesan guestbook & nama tamu adalah data pribadi — JANGAN log full text. Pakai masking: `log.info("Guestbook entry submitted: id={}, nameLength={}", entryId, name.length());`

### 3.4 Audit RSVP
- [ ] Apakah RSVP submit pakai invitationToken? Validasi token + expiry?
- [ ] Apakah RSVP idempotent (submit 2x dengan token sama = update, bukan duplicate)?
- [ ] Attendance status enum: apa value-nya? (cek schema — `attending`, `not_attending`, `pending`?)
- [ ] Apakah response balik ke guest include data yang sudah diupdate?

### 3.5 Audit Guestbook
- [ ] Guestbook publik: return semua yang `isPublished = true`, atau paginated? (Cek apakah ada pagination — kalau ada, sudah benar?)
- [ ] Guestbook submit: apakah perlu moderation (default `isPublished = false`) atau langsung publish? Cek PRD/schema.
- [ ] Apakah guest yang sama bisa submit berkali-kali? (Idempotency / rate limit — di luar scope retrofit.)

---

## 4. Requirement Teknis Perbaikan

Acuan: `Docs/CLAUDE.md` bagian #6, #7, #8.

### 4.1 Refaktor Service jadi Interface + Impl
- Ketiga service → interface + `*ServiceImpl` di package `impl/`.

### 4.2 Refaktor Controller Pakai `ApiResponse`
- Semua controller return `ResponseEntity<ApiResponse<T>>`.
- `PublicRsvpController` — JANGAN tambah `@PreAuthorize` (publik). SecurityConfig sudah permit `/api/v1/public/**` (kalau sudah benar dari Issue A).

### 4.3 Ownership Validation
- `ClientGuestService` & `ClientGuestbookService` — `clientId` HARUS dari JWT. Cross-client → 403.

### 4.4 Tambah Logging (dengan Masking)
**PENTING untuk modul ini:** Nama tamu & pesan guestbook = data pribadi.

Di `ClientGuestServiceImpl`:
- `createGuest(...)`: `log.info("Guest created: invitationId={}, nameLength={}, by clientId={}", invitationId, name.length(), clientId);`
- `deleteGuest(...)`: `log.warn("Guest deleted: id={}, by clientId={}", guestId, clientId);`
- `bulkImport(...)` (kalau ada): `log.info("Bulk guest import: invitationId={}, count={}, by clientId={}", invitationId, count, clientId);`

Di `ClientGuestbookServiceImpl`:
- `publishEntry(...)`: `log.info("Guestbook entry {} published by clientId={}", entryId, clientId);`
- `unpublishEntry(...)`: `log.info("Guestbook entry {} unpublished by clientId={}", entryId, clientId);`
- `deleteEntry(...)`: `log.warn("Guestbook entry {} deleted by clientId={}", entryId, clientId);`

Di `PublicRsvpServiceImpl`:
- `submitRsvp(...)`: `log.info("RSVP submitted: invitationId={}, attendance={}", invitationId, attendance);` (JANGAN log nama/email/phone lengkap)
- `updateRsvp(...)`: `log.info("RSVP updated: invitationId={}, attendance={}", invitationId, attendance);`
- **JANGAN log** message guestbook, nama tamu, email, phone — pakai masking atau skip.

### 4.5 RSVP Validation
- Validasi `invitationToken` (atau mekanisme lookup guest) — pastikan tidak bisa ditebak.
- Kalau RSVP untuk invitation yang sudah expired (`eventDate < today` atau flag lain), return error.
- Attendance status: pakai konstanta String dari schema (`attending`, `not_attending`, `pending`). Validasi di service.

### 4.6 Guestbook Validation
- Pesan max length (cek schema — biasanya `VARCHAR(500)` atau `TEXT`).
- Nama: required, max length.
- Default `isPublished` saat submit publik — cek schema/PRD (biasanya `false` untuk moderasi, atau `true` untuk langsung publish).

### 4.7 Bean Validation di DTO
- `CreateGuestRequest`: `@NotBlank name`, dll.
- `RsvpSubmitRequest`: `@NotBlank invitationToken`, `@NotNull attendanceStatus` (atau nama field yang dipakai), `@Size(max=...) message`.
- `GuestbookEntryDto` (kalau dipakai untuk submit): `@NotBlank name`, `@NotBlank message`, `@Size(max=500) message`.

### 4.8 Entity Tidak Boleh Di-expose
- Semua return controller → DTO.

---

## 5. Definition of Done

- [ ] Ketiga service jadi interface + `*ServiceImpl`
- [ ] Ketiga controller return `ApiResponse<T>`
- [ ] `PublicRsvpController` tidak punya `@PreAuthorize` (publik)
- [ ] Constructor injection
- [ ] Logging SLF4J ada dengan **masking data pribadi**
- [ ] Ownership validation (clientId dari JWT)
- [ ] Bean Validation lengkap di DTO
- [ ] Tidak ada return type controller yang expose Entity

### Cross-cutting
- [ ] Folder `Sql/` TIDAK disentuh
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
3. JANGAN sentuh file di luar Scope #2.
4. JANGAN pakai `ddl-auto=update`/`create`.
5. JANGAN tambah library baru.
6. JANGAN tambah endpoint baru.
7. JANGAN ubah business behavior.
8. **JANGAN log** nama tamu / email / phone / pesan guestbook dalam bentuk lengkap — pakai masking.
9. JANGAN pakai Lombok.
10. JANGAN ubah Entity.
11. Bug di luar scope → CATAT.
12. AMBIGU → asumsi + catat.

---

## 7. Catatan untuk Manusia

```markdown
## Ringkasan
- Endpoint disentuh:
  - ClientGuestController: [daftar]
  - ClientGuestbookController: [daftar]
  - PublicRsvpController: [daftar]
- File dibuat: [...]
- File diubah: [...]
- Asumsi:
  - [contoh: "Default isPublished untuk guestbook publik = false (moderasi dulu). Kalau PRD menentukan true, perlu follow-up issue terpisah."]
  - [contoh: "RSVP pakai invitationToken (bukan invitationId) — sesuai cara Guest entity saat ini."]
- Bug di luar scope:
  - [contoh: "PublicRsvpService.submitRsvp tidak ada rate limit — guest bisa spam submit. Di luar scope retrofit."]
- DoD terpenuhi: [checklist]
```

---

## 8. Urutan Pengerjaan

1. Audit #3.
2. Interface `ClientGuestService` + Impl.
3. Interface `ClientGuestbookService` + Impl.
4. Interface `PublicRsvpService` + Impl.
5. Refaktor ketiga controller → `ApiResponse`.
6. Tambah logging (dengan masking).
7. Bean Validation di DTO.
8. Self-verify.

---

*Setelah selesai, lanjut ke Issue B-5 (Retrofit Theme Management).*