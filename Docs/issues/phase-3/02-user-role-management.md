# Fase 3.2 — User & Role/Permission Management

## 1. Judul & Ringkasan

**Judul:** Backend — Modul User & Role/Permission Management

**Ringkasan:** Buat modul backend lengkap untuk Super Admin mengelola user (CRUD, reset password) dan role/permission management. Semua endpoint di bawah `/api/v1/admin/users` dan `/api/v1/admin/roles`, dilindungi `@PreAuthorize("hasRole('ADMIN')")`.

---

## 2. Scope — File yang BOLEH Disentuh

Agent HANYA boleh membuat/mengedit file dalam folder ini:

```
Backend/src/main/java/com/undangan/online/
├── controller/
│   ├── UserController.java             # BARU
│   └── RoleController.java            # BARU
├── service/
│   ├── UserService.java               # BARU
│   └── RoleService.java               # BARU
├── dto/
│   ├── UserDto.java                   # OVERRIDE / update jika perlu
│   ├── CreateUserRequest.java         # BARU
│   ├── UpdateUserRequest.java         # BARU
│   ├── RoleDto.java                   # BARU
│   ├── CreateRoleRequest.java         # BARU
│   ├── UpdateRolePermissionsRequest.java  # BARU
│   └── UserListDto.java               # BARU
```

**BOLEH dibaca (referensi, tidak boleh diubah):**
- `entity/Users.java` — entity sudah ada, JANGAN ubah struktur field
- `entity/Role.java` — entity sudah ada, JANGAN ubah struktur field
- `entity/RolePermission.java` — entity sudah ada
- `entity/RolePermissionId.java` — embeddable ID
- `entity/Client.java` — entity sudah ada (untuk relasi clientId di Users)
- `repository/UsersRepository.java` — repository sudah ada
- `repository/RoleRepository.java` — repository sudah ada
- `config/SecurityConfig.java` — untuk memahami prefix endpoint
- `exception/GlobalExceptionHandler.java`
- `dto/ErrorResponse.java`
- `exception/AuthException.java`
- `controller/AuthController.java` — pola controller
- `service/AuthService.java` — pola service + password encoding
- `config/JwtConfig.java` — untuk reference

**Catatan:** Jika `UserDto.java` sudah ada dan cocok, gunakan yang ada. Jika perlu menambah field (misal untuk include avatarColor), buat DTO baru `UserListDto.java`.

**JANGAN sentuh file lain.**

---

## 3. Requirement Fungsional

### 3.1 User Management

#### 3.1.1 List Users (GET /api/v1/admin/users)
- Ambil semua user, supports pagination.
- Query param opsional: `clientId` (filter by client), `roleCode` (filter by role), `status`, `search` (cari by name/username).
- Response: id, username, email, name, phone, avatarColor, roleCode, clientId, status, createdAt.
- Semua user tanpa melihat clientId (Super Admin melihat SEMUA user).

#### 3.1.2 Get User by ID (GET /api/v1/admin/users/{id})
- Ambil detail satu user. Return 404 jika tidak ditemukan.

#### 3.1.3 Create User (POST /api/v1/admin/users)
- Body: `{ username, email, password, name, phone?, roleCode, clientId? }`.
- Validasi: `username` unik, `email` unik, `password` min 8 char, `roleCode` harus ada di tabel `role`.
- Password di-hash dengan `PasswordEncoder` (BCrypt).
- `status` auto-set ke `active`, `createdAt`/`updatedAt` = `OffsetDateTime.now()`.
- `avatarColor` opsional, bisa di-generate random hex color atau null.
- `passwordHash` tidak pernah dikembalikan di response.

#### 3.1.4 Update User (PUT /api/v1/admin/users/{id})
- Update: email, name, phone, roleCode.
- Validasi: email unik (jika berubah), roleCode valid.
- `updatedAt` auto-update.
- Password TIDAK bisa diubah lewat endpoint ini — ada endpoint terpisah.

#### 3.1.5 Reset Password (PATCH /api/v1/admin/users/{id}/reset-password)
- Body: `{ newPassword: string }`.
- Validasi: password min 8 char.
- Hash password baru dengan BCrypt.
- `updatedAt` auto-update.

#### 3.1.6 Update Status User (PATCH /api/v1/admin/users/{id}/status)
- Body: `{ status: "active" | "inactive" }`.
- Validasi nilai status.

#### 3.1.7 Delete User (DELETE /api/v1/admin/users/{id})
- Hard delete dari tabel `users`.
- Jika user terakhir dengan role ADMIN, tetap boleh dihapus (tidak ada batasan self-delete).
- Return 204.

#### 3.1.8 List Users by Client (GET /api/v1/admin/clients/{clientId}/users)
- Ambil semua user yang принадлежат ke satu client. Supports pagination.
- Endpoint shortcut — sama logicnya dengan list users tapi auto-filter by clientId.

---

### 3.2 Role & Permission Management

#### 3.2.1 List Roles (GET /api/v1/admin/roles)
- Ambil semua role.
- Response: code, name, description, isSystem, permissions (list menu_key).
- Role system (`isSystem=true`) tidak bisa dihapus/diedit code-nya.

#### 3.2.2 Get Role by Code (GET /api/v1/admin/roles/{code})
- Ambil detail satu role termasuk list permissions.
- Return 404 jika tidak ditemukan.

#### 3.2.3 Create Role (POST /api/v1/admin/roles)
- Body: `{ code, name, description?, permissions: ["menu_key_1", "menu_key_2", ...] }`.
- Validasi: `code` unik, `name` wajib, tidak boleh menggunakan `isSystem=true` saat create.
- `isSystem` auto-set ke `false`, `createdAt` = `OffsetDateTime.now()`.
- Untuk setiap `menu_key` di permissions, buat record `RolePermission` baru.

#### 3.2.4 Update Role (PUT /api/v1/admin/roles/{code})
- Update `name` dan `description` saja.
- `code` TIDAK BISA diubah. Jika role `isSystem=true`, tetap boleh update name/description.
- Return 404 jika role tidak ditemukan.

#### 3.2.5 Update Role Permissions (PUT /api/v1/admin/roles/{code}/permissions)
- Body: `{ permissions: ["menu_key_1", "menu_key_2", ...] }`.
- Replace semua permission yang lama dengan yang baru.
- Jika role `isSystem=true`, tetap boleh update permissions.
- Return 404 jika role tidak ditemukan.

#### 3.2.6 Delete Role (DELETE /api/v1/admin/roles/{code})
- Hard delete role dan semua `RolePermission` terkait.
- Jika ada user yang menggunakan role ini, tetap boleh dihapus (user tidak ikut terhapus — field `role_code` akan orphan/null). Mungkin perlu keputusan: block delete jika ada user aktif? Buat asumsi: BLOCK delete jika ada user dengan role tersebut (cek `Users` table).
- Jika role `isSystem=true`, RETURN 403 Forbidden.
- Return 204.

#### 3.2.7 List Menu Keys / Permission Options (GET /api/v1/admin/roles/permissions)
- Return list semua `menu_key` yang valid di sistem.
- Bisa hardcoded list di service, atau baca dari existing `RolePermission` records.
- Ini untuk frontend mengisi checkbox saat create/edit role.
- Menu key yang valid (fase 3): `client_management`, `user_management`, `invitation_management`, `guest_management`, `theme_management`, `music_management`, `gallery_management`, `system_parameter`.

---

## 4. Requirement Teknis

### 4.1 Struktur Package
```
com.undangan.online.controller.UserController
com.undangan.online.controller.RoleController
com.undangan.online.service.UserService
com.undangan.online.service.RoleService
com.undangan.online.dto.UserDto (update/jika perlu)
com.undangan.online.dto.UserListDto
com.undangan.online.dto.CreateUserRequest
com.undangan.online.dto.UpdateUserRequest
com.undangan.online.dto.RoleDto
com.undangan.online.dto.CreateRoleRequest
com.undangan.online.dto.UpdateRolePermissionsRequest
```

### 4.2 Pola Kode
- **Controller**: Konstruktor injection, annotasi class-level untuk `@PreAuthorize`, `@Transactional` untuk write operations.
- **Service**: Konstruktor injection, `@Service`, logika bisnis. Inject `PasswordEncoder` untuk password hashing.
- **DTO**: Java bean dengan getter/setter, validasi dengan `jakarta.validation`.
- **RolePermission handling**: Di service, untuk create/update permissions, delete semua RolePermission lama lalu insert yang baru. Gunakan cascade orphanRemoval jika perlu, atau manual delete + insert.

### 4.3 Konvensi Environment Variable
Tidak perlu menambah env var baru. Koneksi database sudah dari `application.properties`.

### 4.4 Pola Error
- `AuthException(errorCode, message, status)` untuk error bisnis.
- `ResponseStatusException(HttpStatus.NOT_FOUND, "...")` untuk 404.
- Format response konsisten: `{ "error": "CODE", "message": "..." }`.

---

## 5. Definition of Done

Beri tanda `[x]` jika sudah selesai:

- [ ] `UserController.java` dibuat di `controller/`
- [ ] `RoleController.java` dibuat di `controller/`
- [ ] `UserService.java` dibuat di `service/`
- [ ] `RoleService.java` dibuat di `service/`
- [ ] `CreateUserRequest.java`, `UpdateUserRequest.java`, `UserListDto.java` dibuat di `dto/`
- [ ] `RoleDto.java`, `CreateRoleRequest.java`, `UpdateRolePermissionsRequest.java` dibuat di `dto/`
- [ ] Semua endpoint user di-implement (list, get, create, update, reset-password, update-status, delete, list-by-client)
- [ ] Semua endpoint role di-implement (list, get, create, update, update-permissions, delete, list-permissions)
- [ ] Semua endpoint dilindungi `@PreAuthorize("hasRole('ADMIN')")`
- [ ] Password di-hash dengan BCrypt (`PasswordEncoder`)
- [ ] Validasi input dengan `@Valid`
- [ ] Password tidak pernah dikembalikan di response
- [ ] `isSystem=true` role tidak bisa dihapus atau code-nya diubah
- [ ] Delete role diblok jika ada user aktif dengan role tersebut
- [ ] Pola exception handling konsisten
- [ ] Kode mengikuti gaya yang sudah ada di project
- [ ] Tidak ada perubahan ke file di luar scope

---

## 6. Batasan Tegas

- **JANGAN jalankan** `docker`, `docker compose`, `mvn`, `npm run`, `npm start`, `npm test`.
- **JANGAN sentuh file di folder `Sql/`** — schema database sudah ada dan di-manage manual.
- **JANGAN ubah** `entity/Users.java`, `entity/Role.java`, `entity/RolePermission.java` — entity sudah ada sesuai schema.
- **JANGAN ubah** `repository/UsersRepository.java`, `repository/RoleRepository.java` — sudah ada.
- **JANGAN ubah** `config/SecurityConfig.java`.
- **JANGAN tambahkan** dependency baru.
- **JANGAN pakai** `ddl-auto=update` atau `create`.
- Task ini HANYA membuat service + controller + DTO. Schema tidak disentuh.

---

## 7. Catatan Referensi

- Detail role & permission ada di `Docs/PRD-Undangan-Online.md` bagian 4 (Role & Hak Akses).
- Aturan umum: `Docs/CLAUDE.md`.
- Entity `Users` di `Backend/src/main/java/com/undangan/online/entity/Users.java`.
- Entity `Role` di `Backend/src/main/java/com/undangan/online/entity/Role.java`.
- Entity `RolePermission` di `Backend/src/main/java/com/undangan/online/entity/RolePermission.java`.
- Password hashing: gunakan `PasswordEncoder` yang sudah dikonfigurasi di `SecurityConfig` (BCrypt).
- Sistem role di awal sudah punya minimal 2 role: `ADMIN` (Super Admin) dan `USER` (Client user). Role baru bisa dibuat oleh Super Admin.
- Menu key permission list di fase 3: `client_management`, `user_management`, `invitation_management`, `guest_management`, `theme_management`, `music_management`, `gallery_management`, `system_parameter`. Konfirmasi dengan tech lead jika ada pertanyaan.
