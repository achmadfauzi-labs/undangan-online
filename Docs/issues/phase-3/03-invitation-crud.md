# Fase 3.3 — Invitation/Undangan CRUD

## 1. Judul & Ringkasan

**Judul:** Backend — Modul Invitation CRUD (Data Pasangan, Event, Love Story, Person)

**Ringkasan:** Buat modul backend lengkap untuk Client mengelola undangan mereka sendiri. Scope ini mencakup CRUD data pasangan (InvitationPerson), sesi acara (InvitationSession), love story, serta update data utama invitation. Semua endpoint di `/api/v1/client/invitation`, dilindungi `@PreAuthorize("hasRole('USER')")` (Client).

**Catatan penting:** Modul ini TIDAK membuat invitation baru (create dilakukan oleh Super Admin). Modul ini mengelola data di dalam invitation yang SUDAH ada dan milik client yang login.

---

## 2. Scope — File yang BOLEH Disentuh

Agent HANYA boleh membuat/mengedit file dalam folder ini:

```
Backend/src/main/java/com/undangan/online/
├── controller/
│   └── ClientInvitationController.java   # BARU
├── service/
│   └── ClientInvitationService.java      # BARU
├── dto/
│   ├── InvitationDto.java                # BARU
│   ├── InvitationPersonDto.java          # BARU
│   ├── CreateInvitationPersonRequest.java   # BARU
│   ├── UpdateInvitationPersonRequest.java   # BARU
│   ├── InvitationSessionDto.java        # BARU
│   ├── CreateInvitationSessionRequest.java  # BARU
│   ├── UpdateInvitationSessionRequest.java  # BARU
│   ├── LoveStoryDto.java                # BARU
│   ├── CreateLoveStoryRequest.java      # BARU
│   ├── UpdateLoveStoryRequest.java     # BARU
│   └── UpdateInvitationRequest.java     # BARU
```

**BOLEH dibaca (referensi, tidak boleh diubah):**
- `entity/Invitation.java` — sudah ada
- `entity/InvitationPerson.java` — sudah ada
- `entity/InvitationSession.java` — sudah ada
- `entity/LoveStory.java` — sudah ada
- `entity/Client.java` — sudah ada
- `entity/Template.java` — sudah ada
- `entity/Music.java` — sudah ada
- `repository/InvitationRepository.java` — sudah ada
- `repository/InvitationPersonRepository.java` — **cek apakah sudah ada; jika belum, buat baru di scope ini**
- `repository/InvitationSessionRepository.java` — **cek apakah sudah ada; jika belum, buat baru di scope ini**
- `repository/LoveStoryRepository.java` — **cek apakah sudah ada; jika belum, buat baru di scope ini**
- `security/JwtTokenProvider.java` — untuk extract user context
- `security/JwtAuthenticationFilter.java` — untuk memahami user info dari JWT
- `config/SecurityConfig.java`
- `exception/GlobalExceptionHandler.java`
- `dto/ErrorResponse.java`
- `exception/AuthException.java`
- `controller/AuthController.java`

**Catatan Repository:** Jika `InvitationPersonRepository`, `InvitationSessionRepository`, atau `LoveStoryRepository` BELUM ada di `repository/`, buat juga dalam scope ini (interface JpaRepository standar, tidak perlu custom query kecuali ada kebutuhan).

**JANGAN sentuh file lain.**

---

## 3. Requirement Fungsional

### 3.1 Get My Invitation (GET /api/v1/client/invitation)
- Ambil invitation milik client yang sedang login.
- Cara dapat `clientId`: extract dari JWT token (claim `clientId`). Gunakan `JwtTokenProvider.extractClaim(token, "clientId", Long.class)` atau dari `SecurityContextHolder`.
- Jika client belum punya invitation, return 404 dengan message "Invitation belum dibuat".
- Response lengkap: semua field invitation + nested persons, sessions, loveStories.

### 3.2 Update Invitation Metadata (PUT /api/v1/client/invitation)
- Update field utama invitation: `welcomeMessage`, `eventTypeCode`, `status`.
- `updatedAt` auto-update.
- Client hanya boleh update INVITATION MILIKNYA SENDIRI — cek `invitation.clientId == user.clientId`.

### 3.3 Generate Slug (POST /api/v1/client/invitation/slug/generate)
- Generate slug unik untuk invitation.
- Slug dibuat dari nama client atau nama pasangan (parameter opsional: `baseSlug?`). Format: lowercase, replace spasi dengan `-`, tambahkan random 4 karakter alphanumeric.
- Validasi slug unik (cek `InvitationRepository.findBySlug`). Jika sudah ada, regenerate.
- Save slug ke invitation milik client.
- Response: `{ slug: "kirana-dan-danendra-a7b2" }`.

### 3.4 Publish Invitation (POST /api/v1/client/invitation/publish)
- Set `status` invitation jadi `published`.
- Set `publishedAt` = `OffsetDateTime.now()`.
- Jika invitation belum punya minimal 1 session dan 1 person, return 400.

### 3.5 Person Management (InvitationPerson — data pasangan)

#### 3.5.1 List Persons (GET /api/v1/client/invitation/persons)
- Ambil semua `InvitationPerson` milik invitation client.

#### 3.5.2 Get Person (GET /api/v1/client/invitation/persons/{id})
- Ambil satu person by ID. Return 404 jika tidak ditemukan atau milik client lain.

#### 3.5.3 Create Person (POST /api/v1/client/invitation/persons)
- Body: `{ role: "pria" | "wanita", name, nickname?, parentNames?, childOrder?, photoPath?, sortOrder }`.
- Validasi: `role` harus `pria` atau `wanita`, `name` wajib, `sortOrder` wajib.
- `invitationId` diambil dari invitation client yang login. Jangan terima `invitationId` dari body.
- `createdAt`/`updatedAt` = `OffsetDateTime.now()`.
- Secara default invitation punya 2 orang (pria + wanita), tapi boleh lebih (misal pasangan + kedua orang tua).

#### 3.5.4 Update Person (PUT /api/v1/client/invitation/persons/{id})
- Update semua field opsional: name, nickname, parentNames, childOrder, photoPath, sortOrder.
- `role` TIDAK BISA diubah setelah create (karena secara bisnis, gender tidak berubah).
- Validasi kepemilikan (clientId must match).
- `updatedAt` auto-update.

#### 3.5.5 Delete Person (DELETE /api/v1/client/invitation/persons/{id})
- Hard delete dari tabel `invitation_person`.
- Validasi kepemilikan. Return 404 jika tidak ditemukan.
- Return 204.

---

### 3.6 Session Management (InvitationSession — sesi acara)

#### 3.6.1 List Sessions (GET /api/v1/client/invitation/sessions)
- Ambil semua session milik invitation client, di-sort by `sortOrder` ASC.

#### 3.6.2 Get Session (GET /api/v1/client/invitation/sessions/{id})
- Ambil satu session by ID. Return 404 jika tidak ditemukan atau milik client lain.

#### 3.6.3 Create Session (POST /api/v1/client/invitation/sessions)
- Body: `{ name, sessionDate, sessionTime?, location?, mapsUrl?, sortOrder }`.
- Validasi: `name` wajib, `sessionDate` wajib, `sortOrder` wajib.
- `invitationId` diambil dari invitation client.
- `createdAt`/`updatedAt` = `OffsetDateTime.now()`.
- Typical sessions: "Akad", "Resepsi", "Siraman", dll.

#### 3.6.4 Update Session (PUT /api/v1/client/invitation/sessions/{id})
- Update semua field: name, sessionDate, sessionTime, location, mapsUrl, sortOrder.
- Validasi kepemilikan.
- `updatedAt` auto-update.

#### 3.6.5 Delete Session (DELETE /api/v1/client/invitation/sessions/{id})
- Hard delete. Validasi kepemilikan. Return 204.

#### 3.6.6 Reorder Sessions (PATCH /api/v1/client/invitation/sessions/reorder)
- Body: `{ order: [{ id: number, sortOrder: number }, ...] }`.
- Bulk update sortOrder beberapa session sekaligus.
- Validasi semua session milik invitation client.

---

### 3.7 Love Story Management (LoveStory)

#### 3.7.1 List Love Stories (GET /api/v1/client/invitation/love-stories)
- Ambil semua love story milik invitation client, di-sort by `sortOrder` ASC.

#### 3.7.2 Get Love Story (GET /api/v1/client/invitation/love-stories/{id})
- Ambil satu love story by ID.

#### 3.7.3 Create Love Story (POST /api/v1/client/invitation/love-stories)
- Body: `{ title, storyDate?, description, sortOrder }`.
- Validasi: `title` wajib, `description` wajib, `sortOrder` wajib.
- `invitationId` diambil dari invitation client.
- `createdAt` = `OffsetDateTime.now()`.

#### 3.7.4 Update Love Story (PUT /api/v1/client/invitation/love-stories/{id})
- Update: title, storyDate, description, sortOrder.
- Validasi kepemilikan.

#### 3.7.5 Delete Love Story (DELETE /api/v1/client/invitation/love-stories/{id})
- Hard delete. Validasi kepemilikan. Return 204.

---

### 3.8 Cover Image Upload (Opsional — TIDAK wajib di fase ini)
- Jika waktu memungkinkan, buat endpoint `POST /api/v1/client/invitation/cover` yang menerima multipart file dan menyimpan ke `images/` storage path, lalu update `coverImagePath` di invitation.
- Jika tidak memungkinkan, skip — akan dibuat di modul Gallery Management.

---

## 4. Requirement Teknis

### 4.1 Struktur Package
```
com.undangan.online.controller.ClientInvitationController
com.undangan.online.service.ClientInvitationService
com.undangan.online.dto.InvitationDto
com.undangan.online.dto.InvitationPersonDto
com.undangan.online.dto.CreateInvitationPersonRequest
com.undangan.online.dto.UpdateInvitationPersonRequest
com.undangan.online.dto.InvitationSessionDto
com.undangan.online.dto.CreateInvitationSessionRequest
com.undangan.online.dto.UpdateInvitationSessionRequest
com.undangan.online.dto.LoveStoryDto
com.undangan.online.dto.CreateLoveStoryRequest
com.undangan.online.dto.UpdateLoveStoryRequest
com.undangan.online.dto.UpdateInvitationRequest
```

### 4.2 Pola Kode
- **Controller**: Konstruktor injection, `@PreAuthorize("hasRole('USER')")` di class-level (karena ini endpoint client), gunakan `JwtTokenProvider` untuk extract `clientId` dari token. Contoh pattern:

```java
@PreAuthorize("hasRole('USER')")
public ResponseEntity<?> createPerson(@Valid @RequestBody CreateInvitationPersonRequest request,
        @RequestHeader("Authorization") String authHeader) {
    // extract clientId dari JWT
    String token = authHeader.substring(7);
    Long clientId = tokenProvider.extractClaim(token, "clientId", Long.class);
    // ...
}
```

Atau lebih rapi: buat private helper method untuk dapat `clientId` dari `SecurityContextHolder`.

- **Authorization check**: Setiap operasi WAJIB verifikasi bahwa data yang diakses MILIK client yang login. Gunakan pattern:

```java
Invitation invitation = invitationRepository.findByClientId(clientId)
    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "..."));
// kemudian cek: invitation.getClientId().equals(clientId)
```

- **Service**: Konstruktor injection, `@Service`.
- **DTO**: Java bean dengan getter/setter, validasi `jakarta.validation`.

### 4.3 Konvensi Environment Variable
Tidak perlu menambah env var baru. Storage path sudah dari `application.properties`:
```
app.storage.themes=${STORAGE_THEMES_PATH:/app/storage/themes}
app.storage.musics=${STORAGE_MUSICS_PATH:/app/storage/musics}
app.storage.images=${STORAGE_IMAGES_PATH:/app/storage/images}
```

### 4.4 Pola Slug Generation
Slug: lowercase, alphanumeric + dash, panjang total max 100 karakter (sesuai kolom di entity). Pattern:
```
// 1. Clean: lowercase, replace spaces with -, remove special chars
// 2. Truncate to max 80 chars
// 3. Generate random suffix: 4 alphanumeric chars
// 4. Check uniqueness, regenerate if exists
// 5. Final format: "nama-pasangan-a7b2"
```

---

## 5. Definition of Done

Beri tanda `[x]` jika sudah selesai:

- [ ] `ClientInvitationController.java` dibuat
- [ ] `ClientInvitationService.java` dibuat
- [ ] Semua DTO untuk invitation, person, session, love story dibuat
- [ ] `InvitationPersonRepository` dibuat (jika belum ada)
- [ ] `InvitationSessionRepository` dibuat (jika belum ada)
- [ ] `LoveStoryRepository` dibuat (jika belum ada)
- [ ] GET my invitation endpoint di-implement
- [ ] PUT invitation metadata di-implement
- [ ] POST generate slug di-implement
- [ ] POST publish invitation di-implement
- [ ] CRUD person endpoints (list, get, create, update, delete) di-implement
- [ ] CRUD session endpoints (list, get, create, update, delete, reorder) di-implement
- [ ] CRUD love story endpoints (list, get, create, update, delete) di-implement
- [ ] Semua endpoint client-facing dilindungi `@PreAuthorize("hasRole('USER')")`
- [ ] Authorization check: client hanya bisa akses data miliknya sendiri
- [ ] Validation dengan `@Valid`
- [ ] Error handling konsisten
- [ ] Pola slug generation + uniqueness check di-implement
- [ ] Publish check: minimal 1 session + 1 person
- [ ] Kode mengikuti gaya project
- [ ] Tidak ada perubahan ke file di luar scope

---

## 6. Batasan Tegas

- **JANGAN jalankan** `docker`, `docker compose`, `mvn`, `npm run`, `npm start`, `npm test`.
- **JANGAN sentuh file di folder `Sql/`**.
- **JANGAN ubah** `entity/Invitation.java`, `entity/InvitationPerson.java`, `entity/InvitationSession.java`, `entity/LoveStory.java` — entity sudah ada.
- **JANGAN ubah** `repository/InvitationRepository.java` — sudah ada.
- **JANGAN ubah** `config/SecurityConfig.java`.
- **JANGAN tambahkan** dependency baru.
- **JANGAN pakai** `ddl-auto=update` atau `create`.
- **JANGAN buat endpoint untuk create invitation baru** — create invitation dilakukan oleh Super Admin (bukan di scope ini).
- Task ini HANYA membuat service + controller + DTO + repository (jika belum ada). Schema tidak disentuh.

---

## 7. Catatan Referensi

- Detail invitation ada di `Docs/PRD-Undangan-Online.md` bagian 4.2 (Client) dan 5.1-5.3.
- Aturan umum: `Docs/CLAUDE.md`.
- Entity `Invitation` di `Backend/src/main/java/com/undangan/online/entity/Invitation.java` — cek field yang tersedia.
- Entity `InvitationPerson`: `role` = "pria" atau "wanita", `parentNames` = nama orang tua, `childOrder` = urutan anak.
- Entity `InvitationSession`: `sessionDate` = LocalDate, `sessionTime` = String (misal "09:00 - 12:00").
- Strictly 1 client = 1 undangan aktif (PRD bagian 9). Tidak perlu handle multiple invitation per client.
- Slug uniqueness: cek `InvitationRepository.findBySlug(String slug)`.
- Cover image upload: JANGAN implement di fase ini, skip jika tidak cukup waktu.
