# Issue B-1 — Retrofit Modul Client Management

> **Audience:** AI agent junior (model kecil). Agent **SELALU** menjalankan `docker`, `docker compose`, `mvn`, `npm`, atau perintah build/run/test apa pun ketika sesudah menyelesaikan issue.
>
> **Prasyarat:** Issue A (Retrofit Fase 2 Core & Auth) **HARUS sudah selesai & lolos**. Fondasi `ApiResponse<T>` dan global exception handler harus sudah ada.
>
> **Acuan utama:**
> - `Docs/CLAUDE.md` bagian **#6** (Clean Code & OOP), **#7** (Format Response API), **#8** (Logging)
> - `Docs/PRD-Undangan-Online.md` bagian 5 (Use Cases Super Admin — Client Management)
> - `Docs/issues/phase-2/issue.md` & `Docs/issues/phase-3/01-client-management.md` (sumber asli requirement)

---

## 1. Ringkasan & Alasan Retrofit

Modul Client Management adalah entitas inti di mana Super Admin mendaftarkan akun pasangan pengantin (`Client`) dan mengelola status/expired-nya. Dari Fase 2/3 sudah ada `ClientController`, `ClientService`, dan `ClientInitService`.

**Yang perlu diperbaiki di issue ini:**
1. `ClientController` saat ini return `ResponseEntity<?>` atau langsung entity/DTO tanpa `ApiResponse<T>` wrapper → harus konsisten dengan standar baru dari Issue A.
2. `ClientService` belum punya interface (`ClientService` + `ClientServiceImpl`) — harus direfaktor sesuai CLAUDE.md #6.
3. Business logic di controller (mapping entity ke DTO, filtering, dll) — kalau ada, harus dipindah ke service.
4. Logging SLF4J belum konsisten — tambah di operasi penting (create, expire, deactivate).
5. Validasi input: cek apakah `@Valid` + Bean Validation sudah dipakai. Kalau business validation masih manual di service, tetap di service (bukan di controller).

**TIDAK termasuk issue ini:**
- Retrofit `AuthService` / `AuthController` (sudah di Issue A)
- Retrofit `UserController` / `UserService` (Issue B-2)
- Retrofit modul lain (Invitation, Guest, dll — Issue B-3 dst)
- Endpoint baru

---

## 2. Scope — File/Folder yang Boleh Disentuh

Agent HANYA boleh membuat/mengedit file di bawah.

### File yang BOLEH diedit (existing):
- `Backend/src/main/java/com/undangan/online/controller/ClientController.java`
- `Backend/src/main/java/com/undangan/online/service/ClientService.java` — refaktor jadi interface
- `Backend/src/main/java/com/undangan/online/service/ClientInitService.java` — interface + logging
- `Backend/src/main/java/com/undangan/online/dto/CreateClientRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/UpdateClientRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/ClientDto.java`

### File BARU yang WAJIB dibuat:
- `Backend/src/main/java/com/undangan/online/service/impl/ClientServiceImpl.java` — pindah logic dari class `ClientService` lama ke sini
- `Backend/src/main/java/com/undangan/online/service/impl/ClientInitServiceImpl.java` (jika direfaktor jadi interface+impl)

### DILARANG keras:
- Folder `Sql/` — seluruh isinya
- Folder `Docs/`, `Frontend/`, `Database/`
- File di package `entity/`, `repository/`
- Controller / service lain di luar modul Client (Auth, User, Role, dll)
- File `ApiResponse.java` — sudah dibuat di Issue A, JANGAN disentuh kecuali ada bug
- `pom.xml`, `application.properties` — JANGAN disentuh (kecuali harus, catat di akhir)

---

## 3. Checklist Audit

Centang dulu sebelum coding.

### 3.1 Audit Controller
- [ ] Buka `ClientController.java` — hitung berapa endpoint. Apakah ada business logic (mapping, filtering, validasi manual)? Catat semua method dan return type-nya.
- [ ] Apakah return type `ResponseEntity<ClientDto>`, `ResponseEntity<List<ClientDto>>`, atau `ResponseEntity<?>`?
- [ ] Apakah sudah ada `@PreAuthorize("hasRole(...)")`? Apakah konsisten dengan role di `SecurityConfig` (hasil fix Issue A)?
- [ ] Apakah ada `Map.of(...)` untuk return response sukses yang tidak konsisten? (Tanda response wrapper belum dipakai.)

### 3.2 Audit Service
- [ ] Buka `ClientService.java` — apakah class langsung atau sudah interface?
- [ ] Hitung method: ada berapa CRUD method? Berapa yang return `Entity` JPA langsung?
- [ ] Apakah ada method panjang (god method) yang bisa dipecah?
- [ ] Apakah ada try-catch yang seharusnya lempar exception custom (misal: `AuthException` atau bikin exception baru)?

### 3.3 Audit Logging
- [ ] Apakah `ClientService` punya `Logger`?
- [ ] Apakah ada `log.info` untuk create/update/deactivate/expire?
- [ ] Apakah ada log untuk error (catch block tanpa log)?

### 3.4 Audit DTO
- [ ] Apakah `ClientDto` punya setter publik atau field public? (Idealnya immutable: getter only + constructor / builder manual.)
- [ ] Apakah `CreateClientRequest` / `UpdateClientRequest` sudah pakai Bean Validation (`@NotNull`, `@NotBlank`, `@Size`, dll)?

### 3.5 Audit Entity Exposure
- [ ] Apakah ada method controller yang return `Client` (entity JPA) langsung tanpa konversi ke DTO? (Harusnya lewat DTO.)
- [ ] Apakah field entity `passwordHash` (kalau ada di `Client` — biasanya tidak) ter-expose?

---

## 4. Requirement Teknis Perbaikan

Detail lengkap ada di `Docs/CLAUDE.md` bagian **#6, #7, #8** — rujuk saja, jangan tulis ulang.

### 4.1 Refaktor `ClientService` jadi Interface
- `Backend/src/main/java/com/undangan/online/service/ClientService.java` → ubah jadi **interface** dengan method yang relevan (lihat audit 3.1).
- `Backend/src/main/java/com/undangan/online/service/impl/ClientServiceImpl.java` → class `@Service`, implementasi. Pindahkan SEMUA logic dari class lama.
- Constructor injection: dependency yang dipakai (`ClientRepository`, dll). **TIDAK** boleh `@Autowired` field injection.

### 4.2 Refaktor Controller Pakai `ApiResponse`
- Semua endpoint di `ClientController` return `ResponseEntity<ApiResponse<T>>` (T atau `Void` kalau delete).
- Pola:
  - GET → `ApiResponse.ok(data)` atau `ApiResponse.ok("Berhasil mengambil data", data)`
  - POST (create) → `ResponseEntity.status(201).body(ApiResponse.created(data))`
  - PUT/PATCH (update) → `ApiResponse.ok("Data berhasil diupdate", data)`
  - DELETE → `ApiResponse.ok("Data berhasil dihapus", null)` (data null)
- Method controller HARUS hanya:
  - Terima request (parse DTO)
  - Panggil service
  - Return `ApiResponse`
  - **TIDAK BOLEH** ada business logic, mapping manual, atau filtering.
- Inject interface `ClientService` (bukan concrete class) di constructor.

### 4.3 Tambah Logging SLF4J (CLAUDE.md #8)
Di `ClientServiceImpl`, untuk setiap method penting, tambah log:
- `createClient(...)`: `log.info("Client {} created by admin {}", clientCode, adminUsername);`
- `updateClient(...)`: `log.info("Client {} updated by admin {}", clientId, adminUsername);`
- `deactivateClient(...)`: `log.info("Client {} deactivated by admin {}", clientId, adminUsername);`
- `deleteClient(...)`: `log.warn("Client {} deleted by admin {}", clientId, adminUsername);`
- Error (catch blok, kalau ada): `log.error("Failed to create client: {}", clientCode, ex);` (dengan exception object supaya stack trace ikut).
- **JANGAN log** data sensitif: password (kalau pernah diproses), token, email lengkap, dll.

### 4.4 Validasi Input
- `CreateClientRequest` & `UpdateClientRequest` HARUS pakai Bean Validation (`@NotBlank`, `@NotNull`, `@Size`, `@Email`, dll). Field wajib → `@NotBlank` atau `@NotNull`.
- Validasi bisnis (misal: `expiresAt` harus di masa depan, `code` harus unik) → tetap di service layer, **BUKAN** di controller.
- Throws: pakai exception yang sudah ada (`AuthException` atau exception lain yang sudah ter-handle di `GlobalExceptionHandler` Issue A). Kalau butuh exception baru, **catat** di ringkasan akhir.

### 4.5 DTO Immutability (Best Practice, tidak wajib)
- `ClientDto` idealnya hanya punya getter (no setter). Pakai constructor atau static factory method di DTO.
- **TIDAK WAJIB** refaktor jadi immutable penuh kalau akan makan banyak perubahan. Minimal: jangan expose field tanpa getter/setter yang sesuai.

### 4.6 Entity Tidak Boleh Di-expose
- Semua return type dari controller harus DTO, **BUKAN** entity JPA.
- Kalau ada helper method di service yang return `Client` (entity), cek siapa yang panggil — kalau hanya internal service, tidak masalah. Kalau ada yang return ke controller, **WAJIB** konversi ke DTO dulu.
- Cek: `passwordHash` field (kalau ada di `Client` entity) — pastikan TIDAK ikut di DTO output.

---

## 5. Definition of Done

### Standar Berlaku
- [ ] `ClientService` jadi interface, `ClientServiceImpl` di package `impl/`, logic pindah lengkap
- [ ] `ClientController` return `ApiResponse<T>` wrapper di semua endpoint
- [ ] Tidak ada business logic di controller (mapping, filtering, validasi manual dipindah ke service/DTO)
- [ ] Constructor injection dipakai (bukan `@Autowired` field)
- [ ] Logging SLF4J ada di `ClientServiceImpl` untuk create/update/deactivate/delete + error log dengan stack trace
- [ ] DTO request pakai Bean Validation (`@Valid` di controller + constraint di field)
- [ ] Tidak ada return type controller yang expose `Entity` JPA
- [ ] Tidak ada `System.out.println` / `printStackTrace`
- [ ] Tidak ada hardcode credential / secret

### Cross-cutting
- [ ] Folder `Sql/` TIDAK disentuh
- [ ] File `ApiResponse.java` TIDAK disentuh (kecuali ada bug jelas)
- [ ] TIDAK menjalankan `mvn`, `docker`, `npm`, atau build tool
- [ ] TIDAK menambah test code
- [ ] TIDAK menambah endpoint baru
- [ ] TIDAK mengubah business behavior (logic create/update/delete TETAP sama, hanya struktur & format response yang berubah)

### Catatan yang WAJIB ada di ringkasan akhir
- [ ] Endpoint yang disentuh (daftar method HTTP + path)
- [ ] Asumsi yang diambil
- [ ] Bug / catatan di luar scope yang ditemukan (TIDAK diperbaiki)
- [ ] Definition of Done dari bagian #5 yang terpenuhi (checklist dicentang)

---

## 6. Batasan Tegas (Jangan Dilanggar)

1. **JANGAN menjalankan** `docker`, `docker compose`, `mvn`, `./mvnw`, `npm`, atau perintah build/run/test apa pun. Agent TIDAK test.
2. **JANGAN mengubah, menghapus, atau menambah file di folder `Sql/`.**
3. **JANGAN menyentuh** controller/service/file di luar Scope bagian #2. Kalau perlu ubah file lain, **catat** di akhir.
4. **JANGAN pakai** `ddl-auto=update` atau `create`.
5. **JANGAN menambah** library/dependency baru.
6. **JANGAN menambah endpoint baru.**
7. **JANGAN mengubah business behavior** yang sudah ada.
8. **JANGAN log data sensitif.**
9. **JANGAN pakai Lombok.**
10. **JANGAN ubah `Entity` JPA** — kalau entity perlu diserialisasi ke DTO, konversi di service.
11. **Kalau ada bug** di luar scope retrofit → CATAT, jangan perbaiki.
12. **Kalau AMBIGU** → asumsi paling masuk akal, tulis di ringkasan, lanjut.

---

## 7. Catatan untuk Manusia (diisi agent di akhir — WAJIB)

```markdown
## Ringkasan
- Endpoint yang disentuh (method + path):
  - [contoh: "GET /api/v1/admin/clients, POST /api/v1/admin/clients, dst"]
- File dibuat: [daftar path]
- File diubah: [daftar path]
- Asumsi yang diambil:
  - [contoh: "Validasi bisnis seperti expiresAt > today tetap di service, bukan di controller."]
  - [contoh: "DTO ClientDto tidak direfaktor jadi immutable penuh karena perubahan terlalu luas; hanya dijamin tidak expose passwordHash."]
- Bug / catatan di luar scope yang ditemukan (TIDAK diperbaiki):
  - [contoh: "ClientService.createClient() tidak cek apakah clientCode sudah ada sebelum insert — bisa jadi duplicate key error runtime, perlu dicek di issue terpisah."]
- Definition of Done dari bagian #5 yang terpenuhi: [checklist dicentang]
```

---

## 8. Urutan Pengerjaan (Disarankan)

1. **Audit** checklist bagian #3 — catat hasil.
2. **Bikin interface `ClientService`** + `ClientServiceImpl` di package `impl/`.
3. **Refaktor `ClientController`** return `ApiResponse<T>`.
4. **Tambah logging** di `ClientServiceImpl`.
5. **Cek & tambah Bean Validation** di DTO request.
6. **Self-verify** dengan Definition of Done.

---

*Setelah issue ini selesai & lolos verifikasi, lanjut ke Issue B-2 (Retrofit User & Role Management).*