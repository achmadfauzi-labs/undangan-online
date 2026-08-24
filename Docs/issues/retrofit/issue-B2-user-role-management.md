# Issue B-2 — Retrofit Modul User & Role Management

> **Audience:** AI agent junior (model kecil). Agent **DILARANG** menjalankan `docker`, `docker compose`, `mvn`, `npm`, atau perintah build/run/test apa pun. Tugas murni menulis/mengedit kode. Manusia yang test manual.
>
> **Prasyarat:** Issue A (Fondasi `ApiResponse`) sudah selesai. Bisa dikerjakan paralel/independen dengan Issue B-1 (tidak ada cross-dependency modul).
>
> **Acuan utama:**
> - `Docs/CLAUDE.md` bagian **#6**, **#7**, **#8**
> - `Docs/PRD-Undangan-Online.md` bagian 5 (Use Cases Super Admin — User & Role)
> - `Docs/issues/phase-3/02-user-role-management.md`

---

## 1. Ringkasan & Alasan Retrofit

Modul User & Role Management mengatur akun user (CRUD user, ganti password, kelola role + permission). Dari Fase 3 sudah ada `UserController`, `RoleController`, `UserService`, `RoleService`, dan entity terkait (`Users`, `Role`, `RolePermission`).

**Yang perlu diperbaiki:**
1. `UserController` & `RoleController` belum pakai `ApiResponse<T>` wrapper konsisten (atau sebagian pakai, sebagian tidak).
2. `UserService` & `RoleService` belum interface + impl.
3. Logging belum konsisten di operasi penting (create user, delete user, ubah role).
4. `passwordHash` harus TIDAK pernah ter-expose di response — cek apakah DTO sudah handle.
5. Validasi input (Bean Validation) mungkin belum lengkap di DTO.

**TIDAK termasuk issue ini:**
- Modul Client Management (Issue B-1)
- Modul lain (Auth — sudah di Issue A, Invitation — Issue B-3, dst)

---

## 2. Scope — File/Folder yang Boleh Disentuh

### File yang BOLEH diedit (existing):
- `Backend/src/main/java/com/undangan/online/controller/UserController.java`
- `Backend/src/main/java/com/undangan/online/controller/RoleController.java`
- `Backend/src/main/java/com/undangan/online/service/UserService.java`
- `Backend/src/main/java/com/undangan/online/service/RoleService.java`
- `Backend/src/main/java/com/undangan/online/dto/CreateUserRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/UpdateUserRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/UserDto.java`
- `Backend/src/main/java/com/undangan/online/dto/UserListDto.java`
- `Backend/src/main/java/com/undangan/online/dto/CreateRoleRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/UpdateRolePermissionsRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/RoleDto.java`

### File BARU yang WAJIB dibuat:
- `Backend/src/main/java/com/undangan/online/service/impl/UserServiceImpl.java`
- `Backend/src/main/java/com/undangan/online/service/impl/RoleServiceImpl.java`

### DILARANG keras:
- Folder `Sql/` — seluruh isinya
- Folder `Docs/`, `Frontend/`, `Database/`
- File di package `entity/`, `repository/`
- `AuthService` / `AuthController` (Issue A)
- `ClientController` / `ClientService` (Issue B-1)
- File `ApiResponse.java`

---

## 3. Checklist Audit

### 3.1 Audit Controller
- [ ] Buka `UserController` & `RoleController`. Daftar semua endpoint (method + path) + return type saat ini.
- [ ] Apakah pakai `ResponseEntity<ApiResponse<T>>` atau `ResponseEntity<T>` langsung / `Map.of(...)`?
- [ ] Apakah `@PreAuthorize("hasRole(...)")` konsisten?
- [ ] Apakah ada business logic di controller?

### 3.2 Audit Service
- [ ] Apakah `UserService` & `RoleService` class langsung atau sudah interface?
- [ ] Method yang return `Entity` JPA langsung (jangan sampai)!
- [ ] Validasi unik (username/email/role code) — di service atau controller?
- [ ] Password handling: hash BCrypt hanya di service, JANGAN di controller atau DTO.

### 3.3 Audit Logging
- [ ] Logger ada di kedua service?
- [ ] Logging untuk create user, delete user, update role, ubah password — sudah ada?
- [ ] Apakah ada catch tanpa log?

### 3.4 Audit DTO & Security
- [ ] `UserDto` — apakah ada `passwordHash` field? (Kalau ada, HARUS dihapus dari DTO.)
- [ ] `CreateUserRequest` & `UpdateUserRequest` — Bean Validation lengkap?
- [ ] Apakah response dari endpoint list user filter password? (Cek method service.)

### 3.5 Audit Role & Permission
- [ ] `RoleService` — apa bedanya `Role` dengan `RolePermission`?
- [ ] Apakah `UpdateRolePermissionsRequest` validasi permission yang ada (cek master permission)?
- [ ] Apakah update permission atomic (semua sukses atau semua gagal)?

---

## 4. Requirement Teknis Perbaikan

Acuan detail: `Docs/CLAUDE.md` bagian #6, #7, #8.

### 4.1 Refaktor Service jadi Interface + Impl
- `UserService` → interface; logic pindah ke `UserServiceImpl` (di package `impl/`).
- `RoleService` → interface; logic pindah ke `RoleServiceImpl` (di package `impl/`).
- Constructor injection; `@Service` di class Impl.

### 4.2 Refaktor Controller Pakai `ApiResponse`
- Semua endpoint di `UserController` & `RoleController` return `ResponseEntity<ApiResponse<T>>`.
- Pola response:
  - GET → `ApiResponse.ok("...", data)` atau `ApiResponse.ok(data)` (default message)
  - POST create → `ApiResponse.created(data)` dengan HTTP 201
  - PUT/PATCH → `ApiResponse.ok("Data berhasil diupdate", data)`
  - DELETE → `ApiResponse.ok("Data berhasil dihapus", null)`
- Inject interface service di constructor.
- **TIDAK ada** business logic di controller.

### 4.3 Tambah Logging SLF4J
Di `UserServiceImpl`:
- `createUser(...)`: `log.info("User {} created with role {} by admin {}", username, roleCode, actorUsername);`
- `updateUser(...)`: `log.info("User {} updated by admin {}", userId, actorUsername);`
- `deleteUser(...)`: `log.warn("User {} deleted by admin {}", userId, actorUsername);`
- `changePassword(...)`: `log.info("Password changed for user {} by {}", userId, actorUsername);`
- Validasi gagal / duplikat: `log.warn("Create user failed: username '{}' or email already exists", username);`
- Error: `log.error("...", ex);`

Di `RoleServiceImpl`:
- `createRole(...)`: `log.info("Role {} created by admin {}", code, actorUsername);`
- `updateRolePermissions(...)`: `log.info("Permissions updated for role {} by admin {}", roleCode, actorUsername);`
- `deleteRole(...)`: `log.warn("Role {} deleted by admin {}", roleCode, actorUsername);`

**JANGAN log** password plaintext, hash password, email lengkap.

### 4.4 Password Safety
- DTO `UserDto` TIDAK BOLEH punya field `passwordHash` atau `password`. Cek dan hapus kalau ada.
- Response dari endpoint yang mengembalikan user TIDAK BOLEH mengandung password / hash dalam bentuk apa pun.
- Password hanya di-handle di:
  - Request DTO (`CreateUserRequest`, `UpdateUserRequest` — boleh `password`)
  - Service layer (BCrypt encode sebelum save)
  - **TIDAK PERNAH** di response atau log.

### 4.5 Bean Validation di DTO
Pastikan:
- `CreateUserRequest`: `@NotBlank username`, `@NotBlank email`, `@Email email`, `@Size(min=8) password`, `@NotBlank name`, `@NotNull roleCode`.
- `UpdateUserRequest`: field yang opsional pakai `@Size` (untuk validasi panjang kalau diisi).
- `CreateRoleRequest`: `@NotBlank code`, `@NotBlank name`.

### 4.6 Entity Tidak Boleh Di-expose
- Semua response controller → DTO, **BUKAN** entity.
- Cek method service: kalau ada yang return `Users` atau `Role` entity langsung, refaktor ke DTO.

---

## 5. Definition of Done

- [ ] `UserService` & `RoleService` jadi interface; `*ServiceImpl` di package `impl/`
- [ ] `UserController` & `RoleController` return `ApiResponse<T>` di semua endpoint
- [ ] Tidak ada business logic di controller
- [ ] Constructor injection dipakai
- [ ] Logging SLF4J ada di service untuk operasi penting + error log
- [ ] `UserDto` TIDAK punya field `passwordHash` / `password`
- [ ] Bean Validation lengkap di request DTO
- [ ] Tidak ada return type controller yang expose Entity
- [ ] Tidak ada `System.out.println` / `printStackTrace`

### Cross-cutting
- [ ] Folder `Sql/` TIDAK disentuh
- [ ] File `ApiResponse.java` TIDAK disentuh
- [ ] TIDAK menjalankan build tool
- [ ] TIDAK menambah test code / endpoint baru
- [ ] TIDAK mengubah business behavior

### Catatan wajib di ringkasan akhir
- [ ] Endpoint yang disentuh
- [ ] Asumsi
- [ ] Bug di luar scope (TIDAK diperbaiki)
- [ ] DoD checklist dicentang

---

## 6. Batasan Tegas

1. JANGAN jalankan `docker`/`docker compose`/`mvn`/`npm`/build tool apa pun.
2. JANGAN ubah file di `Sql/`.
3. JANGAN sentuh controller/service/file di luar Scope #2.
4. JANGAN pakai `ddl-auto=update`/`create`.
5. JANGAN tambah library baru.
6. JANGAN tambah endpoint baru.
7. JANGAN ubah business behavior.
8. JANGAN log password/hash/email lengkap.
9. JANGAN pakai Lombok.
10. JANGAN ubah Entity.
11. Bug di luar scope → CATAT saja.
12. AMBIGU → asumsi + tulis di ringkasan.

---

## 7. Catatan untuk Manusia

```markdown
## Ringkasan
- Endpoint disentuh (method + path):
  - [UserController: ..., RoleController: ...]
- File dibuat: [...]
- File diubah: [...]
- Asumsi:
  - [contoh]
- Bug di luar scope (TIDAK diperbaiki):
  - [contoh: "UserService.createUser() tidak handle concurrent request untuk username yang sama — bisa jadi race condition, perlu transaction isolation review."]
- DoD terpenuhi: [checklist]
```

---

## 8. Urutan Pengerjaan

1. Audit checklist #3.
2. Bikin interface `UserService` + `UserServiceImpl`.
3. Bikin interface `RoleService` + `RoleServiceImpl`.
4. Refaktor `UserController` & `RoleController` → `ApiResponse<T>`.
5. Tambah logging di kedua `*Impl`.
6. Hapus `passwordHash` dari `UserDto` kalau ada.
7. Bean Validation di DTO.
8. Self-verify.

---

*Setelah selesai, lanjut ke Issue B-3 (Retrofit Invitation).*