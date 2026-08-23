# Issue B-3 — Retrofit Modul Invitation (Client & Public)

> **Audience:** AI agent junior (model kecil). Agent **DILARANG** menjalankan `docker`, `docker compose`, `mvn`, `npm`, atau perintah build/run/test apa pun. Tugas murni menulis/mengedit kode. Manusia yang test manual.
>
> **Prasyarat:** Issue A selesai.
>
> **Acuan utama:**
> - `Docs/CLAUDE.md` bagian **#6**, **#7**, **#8**
> - `Docs/PRD-Undangan-Online.md` bagian 5/6 (Use Case Client — Invitation)
> - `Docs/issues/phase-3/03-invitation-crud.md`

---

## 1. Ringkasan & Alasan Retrofit

Modul Invitation adalah fitur terbesar: client mengelola data undangan (event, person, session, love story), dan guest melihat via `/api/v1/public/invitation/{slug}`. Ada **dua sisi controller**:
1. **Client-side** (`ClientInvitationController`) — operasi CRUD milik client yang login.
2. **Public-side** (`PublicInvitationController`) — read-only untuk guest, tanpa token.

**Yang perlu diperbaiki:**
1. Kedua controller belum pakai `ApiResponse<T>` wrapper konsisten.
2. `ClientInvitationService` belum interface + impl.
3. Validasi ownership (client A tidak boleh akses invitation client B) — cek apakah sudah benar.
4. Logging di operasi penting (create/update/delete invitation, publish, dll).
5. Entity exposure: `Invitation`, `InvitationSession`, `InvitationPerson`, `LoveStory` — pastikan tidak return langsung dari controller.

**TIDAK termasuk issue ini:**
- Modul lain (RSVP, Guestbook — Issue B-4)
- Modul Theme/Music/Gallery — Issue B-5, B-6, B-7

---

## 2. Scope — File/Folder yang Boleh Disentuh

### File yang BOLEH diedit (existing):
- `Backend/src/main/java/com/undangan/online/controller/ClientInvitationController.java`
- `Backend/src/main/java/com/undangan/online/controller/PublicInvitationController.java`
- `Backend/src/main/java/com/undangan/online/service/ClientInvitationService.java`
- `Backend/src/main/java/com/undangan/online/dto/CreateInvitationSessionRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/CreateInvitationPersonRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/CreateLoveStoryRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/UpdateInvitationRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/UpdateInvitationSessionRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/UpdateInvitationPersonRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/UpdateLoveStoryRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/InvitationDto.java`
- `Backend/src/main/java/com/undangan/online/dto/InvitationSessionDto.java`
- `Backend/src/main/java/com/undangan/online/dto/InvitationPersonDto.java`
- `Backend/src/main/java/com/undangan/online/dto/LoveStoryDto.java`

### File BARU yang WAJIB dibuat:
- `Backend/src/main/java/com/undangan/online/service/impl/ClientInvitationServiceImpl.java`

### DILARANG keras:
- Folder `Sql/` — seluruh isinya
- Folder `Docs/`, `Frontend/`, `Database/`
- File di package `entity/`, `repository/`
- Controller/service lain di luar modul Invitation
- `ApiResponse.java`

---

## 3. Checklist Audit

### 3.1 Audit Controller
- [ ] `ClientInvitationController` & `PublicInvitationController` — daftar semua endpoint.
- [ ] Return type: `ResponseEntity<?>` / `ResponseEntity<T>` / `Map.of(...)`?
- [ ] `@PreAuthorize` di client controller — pakai role apa?
- [ ] `PublicInvitationController` — pakai `@PreAuthorize`? (Harusnya tanpa, bebas diakses.)
- [ ] Business logic di controller? (Mapping, validasi slug, ownership check, dll — harusnya di service.)

### 3.2 Audit Service
- [ ] `ClientInvitationService` — class langsung atau interface?
- [ ] Method yang handle CRUD: create / update / delete / publish / unpublish.
- [ ] Method yang ambil dari JWT (`clientId`) — cek apakah ada parameter request yang redundant (jangan sampai client bisa spoof clientId).
- [ ] Apakah ada ownership validation (clientId dari JWT vs clientId di resource)?

### 3.3 Audit Logging
- [ ] Logger di service?
- [ ] Log untuk create/update/delete invitation + publish/unpublish?
- [ ] Catch tanpa log?

### 3.4 Audit Public Endpoint
- [ ] `PublicInvitationController` return apa untuk `GET /api/v1/public/invitation/{slug}`?
- [ ] Apakah sudah include data persons, sessions, template?
- [ ] Apakah return data invitation yang status-nya belum published? (Harusnya filter hanya yang published atau selalu tampilkan? Cek PRD.)

### 3.5 Audit DTO & Validation
- [ ] Bean Validation di request DTO?
- [ ] Slug validation (lowercase, dash only, length)?
- [ ] `InvitationDto` — apakah field sensitive ter-expose?

---

## 4. Requirement Teknis Perbaikan

Acuan: `Docs/CLAUDE.md` bagian #6, #7, #8.

### 4.1 Refaktor Service jadi Interface + Impl
- `ClientInvitationService` → interface.
- `ClientInvitationServiceImpl` (package `impl/`) — pindahkan semua logic.

### 4.2 Refaktor Controller Pakai `ApiResponse`
- `ClientInvitationController` & `PublicInvitationController` — semua return `ResponseEntity<ApiResponse<T>>`.
- Public endpoint: response `ApiResponse.ok(data)` dengan message deskriptif.
- Inject `ClientInvitationService` interface di constructor (kedua controller).

### 4.3 Ownership Validation (PENTING — sesuai CLAUDE.md & PRD)
- **Setiap method di `ClientInvitationService` yang akses data per-client** HARUS:
  - Ambil `clientId` dari JWT (via `SecurityContextHolder` atau parameter injected), BUKAN dari request body/query.
  - Validasi `invitation.clientId == clientId dari token` SEBELUM read/update/delete.
  - Kalau beda → throw exception (403 / `AuthException("ACCESS_DENIED", ..., 403)`).
- Kalau method service sudah ambil dari parameter, **cek apakah controller memang inject dari token**, bukan dari request.

### 4.4 Tambah Logging
Di `ClientInvitationServiceImpl`:
- `createInvitation(...)`: `log.info("Invitation {} created for client {}", slug, clientId);`
- `updateInvitation(...)`: `log.info("Invitation {} updated for client {}", invitationId, clientId);`
- `deleteInvitation(...)`: `log.warn("Invitation {} deleted for client {}", invitationId, clientId);`
- `addSession(...)`: `log.info("Session added to invitation {} by client {}", invitationId, clientId);`
- `addPerson(...)`: `log.info("Person added to invitation {} by client {}", invitationId, clientId);`
- `addLoveStory(...)`: `log.info("LoveStory added to invitation {} by client {}", invitationId, clientId);`
- Validasi gagal (ownership, not found): `log.warn("Access denied: clientId {} tried to access invitation {}", clientId, invitationId);`
- Error: `log.error("...", ex);`

**JANGAN log** data pribadi guest, token, dll.

### 4.5 Public Endpoint Consistency
- `PublicInvitationController` return `ApiResponse.ok(...)`.
- Untuk invitation yang tidak ditemukan → `ApiResponse.error("Undangan tidak ditemukan")` + HTTP 404 (lewat exception yang di-handle global).
- Untuk endpoint guestbook publik (kalau ada di controller ini) — itu masuk Issue B-4. Kalau ada di controller ini, **biarkan dulu**, akan disentuh di Issue B-4.

### 4.6 Entity Tidak Boleh Di-expose
- Semua return controller → DTO. Cek method service yang return entity, refaktor.

### 4.7 Bean Validation di DTO
- `CreateInvitationRequest` (atau set equivalent): `@NotBlank slug`, `@Pattern slug`, `@NotNull eventTypeCode`, dll.
- `CreateInvitationSessionRequest`: `@NotBlank name`, `@NotNull sessionDate`, `@NotBlank sessionTime`, `@NotBlank location`.
- `CreateInvitationPersonRequest`: `@NotBlank role`, `@NotBlank name`.
- `CreateLoveStoryRequest`: `@NotBlank title`, `@NotNull storyDate`, `@NotBlank content`.

---

## 5. Definition of Done

- [ ] `ClientInvitationService` interface, `ClientInvitationServiceImpl` di `impl/`
- [ ] `ClientInvitationController` & `PublicInvitationController` return `ApiResponse<T>`
- [ ] Tidak ada business logic di controller
- [ ] Constructor injection dipakai
- [ ] Ownership validation: `clientId` dari JWT, cross-client access → 403
- [ ] Logging SLF4J ada di service
- [ ] Bean Validation lengkap di request DTO
- [ ] Tidak ada return type controller yang expose Entity

### Cross-cutting
- [ ] Folder `Sql/` TIDAK disentuh
- [ ] `ApiResponse.java` TIDAK disentuh
- [ ] TIDAK menjalankan build tool
- [ ] TIDAK menambah endpoint baru / test code
- [ ] TIDAK mengubah business behavior

### Catatan wajib di ringkasan akhir
- [ ] Endpoint yang disentuh
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
8. JANGAN log data sensitif.
9. JANGAN pakai Lombok.
10. JANGAN ubah Entity.
11. Bug di luar scope → CATAT.
12. AMBIGU → asumsi + catat.

---

## 7. Catatan untuk Manusia

```markdown
## Ringkasan
- Endpoint disentuh:
  - ClientInvitationController: [daftar]
  - PublicInvitationController: [daftar]
- File dibuat: [...]
- File diubah: [...]
- Asumsi:
  - [contoh]
- Bug di luar scope:
  - [contoh: "PublicInvitationController tidak filter invitation yang status='draft' — semua invitation tampil ke publik. Perlu konfirmasi PRD apakah ini intended."]
- DoD terpenuhi: [checklist]
```

---

## 8. Urutan Pengerjaan

1. Audit #3.
2. Interface `ClientInvitationService` + Impl.
3. Refaktor `ClientInvitationController` → `ApiResponse`.
4. Refaktor `PublicInvitationController` → `ApiResponse`.
5. Tambah logging.
6. Bean Validation di DTO.
7. Self-verify.

---

*Setelah selesai, lanjut ke Issue B-4 (Retrofit Guest + RSVP + Guestbook).*