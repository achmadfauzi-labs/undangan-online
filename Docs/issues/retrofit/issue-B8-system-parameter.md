# Issue B-8 — Retrofit Modul System Parameter

> **Audience:** AI agent junior (model kecil). Agent **SELALU** menjalankan `docker`, `docker compose`, `mvn`, `npm`, atau perintah build/run/test apa pun ketika sesudah menyelesaikan issue.
>
> **Prasyarat:** Issue A selesai.
>
> **Acuan utama:**
> - `Docs/CLAUDE.md` bagian **#6**, **#7**, **#8**
> - `Docs/PRD-Undangan-Online.md` bagian 5 (Use Case Super Admin — System Parameter)
> - `Docs/issues/phase-3/08-system-parameter.md`

---

## 1. Ringkasan & Alasan Retrofit

Modul System Parameter: Super Admin mengelola parameter sistem (master data lookup seperti `event_type`, `rsvp_attendance_status`, dll.) via tabel `system_parameter`.

**Controller terkait:**
- `SystemParameterController` — Super Admin CRUD parameter

**Service terkait:**
- `SystemParameterService` — logic read/update parameter

**Yang perlu diperbaiki:**
1. Controller belum pakai `ApiResponse<T>` wrapper.
2. `SystemParameterService` belum interface + impl.
3. Logging di create/update/delete parameter.
4. Bean Validation di DTO.
5. Ini modul paling sederhana di antara semua — scope kecil, cepat selesai.

**TIDAK termasuk issue ini:**
- Modul lain (Gallery — B-7)

---

## 2. Scope — File/Folder yang Boleh Disentuh

### File yang BOLEH diedit (existing):
- `Backend/src/main/java/com/undangan/online/controller/SystemParameterController.java`
- `Backend/src/main/java/com/undangan/online/service/SystemParameterService.java`
- `Backend/src/main/java/com/undangan/online/dto/CreateSystemParameterRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/UpdateSystemParameterValueRequest.java`
- `Backend/src/main/java/com/undangan/online/dto/SystemParameterDto.java`

### File BARU yang WAJIB dibuat:
- `Backend/src/main/java/com/undangan/online/service/impl/SystemParameterServiceImpl.java`

### DILARANG keras:
- Folder `Sql/`
- Folder `Docs/`, `Frontend/`, `Database/`
- File di package `entity/`, `repository/`
- Controller/service lain di luar Scope #2
- `ApiResponse.java`

---

## 3. Checklist Audit

### 3.1 Audit Controller
- [ ] Daftar endpoint di `SystemParameterController`.
- [ ] Return type: `ApiResponse<T>` atau plain?
- [ ] `@PreAuthorize` — harus Super Admin.
- [ ] Business logic di controller?

### 3.2 Audit Service
- [ ] `SystemParameterService` — class langsung atau interface?
- [ ] Method: ada findAll, findByGroupCode, findByGroupCodeAndCode, create, updateValue?
- [ ] Validasi: groupCode + code harus unik?

### 3.3 Audit Logging
- [ ] Logger di service?
- [ ] Log untuk create/update/delete parameter?

### 3.4 Audit DTO
- [ ] Bean Validation di `CreateSystemParameterRequest` & `UpdateSystemParameterValueRequest`.
- [ ] `SystemParameterDto` — field apa saja? Apakah ada field sensitif?

### 3.5 Audit Business Logic
- [ ] Apakah ada parameter yang readonly (tidak boleh diubah)? Cek annotation atau flag di entity.
- [ ] Apakah perubahan value divalidasi (misal: enum-like value harus salah satu dari daftar yang diizinkan)?

---

## 4. Requirement Teknis Perbaikan

Acuan: `Docs/CLAUDE.md` bagian #6, #7, #8.

### 4.1 Refaktor Service jadi Interface + Impl
- `SystemParameterService` → interface + `SystemParameterServiceImpl` di `impl/`.

### 4.2 Refaktor Controller Pakai `ApiResponse`
- Semua endpoint → `ResponseEntity<ApiResponse<T>>`.
- GET list → `ApiResponse.ok(list)`
- GET by group → `ApiResponse.ok(list)`
- POST create → `ApiResponse.created(param)`
- PUT update value → `ApiResponse.ok("Parameter berhasil diupdate", param)`
- DELETE → `ApiResponse.ok("Parameter berhasil dihapus", null)`

### 4.3 Tambah Logging
Di `SystemParameterServiceImpl`:
- `createParameter(...)`: `log.info("System parameter created: groupCode={}, code={}, by admin {}", groupCode, code, actorUsername);`
- `updateParameterValue(...)`: `log.info("System parameter {} updated: groupCode={}, code={}, by admin {}", id, groupCode, code, actorUsername);`
- `deleteParameter(...)`: `log.warn("System parameter {} deleted: groupCode={}, code={}, by admin {}", id, groupCode, code, actorUsername);`
- Conflict (duplikat): `log.warn("System parameter create failed: groupCode={}, code={} already exists", groupCode, code);`

### 4.4 Bean Validation
- `CreateSystemParameterRequest`: `@NotBlank groupCode`, `@NotBlank code`, `@NotBlank value`, `@Size(max=100) groupCode`, dll.
- `UpdateSystemParameterValueRequest`: `@NotBlank value`, dll.

### 4.5 Entity Tidak Boleh Di-expose
- Response controller → `SystemParameterDto`.

### 4.6 Validasi Bisnis (Catat Jika Ada)
- Jika ada parameter readonly (flag `isReadOnly` atau `isSystem = true`) → update/delete harus ditolak dengan `AuthException("ACCESS_DENIED", "Parameter sistem tidak dapat diubah", 403)`. Ini **TIDAK WAJIB** di-issue ini kalau belum ada di kode — catat saja sebagai temuan.
- Jika ada validasi value format (misal: harus numeric, harus salah satu dari list) → tetap di service layer.

---

## 5. Definition of Done

- [ ] `SystemParameterService` jadi interface + `SystemParameterServiceImpl`
- [ ] Controller return `ApiResponse<T>` di semua endpoint
- [ ] Constructor injection
- [ ] Logging SLF4J ada untuk create/update/delete
- [ ] Bean Validation di request DTO
- [ ] Tidak ada return type yang expose Entity

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
  - SystemParameterController: [daftar]
- File dibuat: [...]
- File diubah: [...]
- Asumsi:
  - [contoh: "Tidak ada parameter readonly di entity — semua parameter bisa di-CUD."]
  - [contoh: "Tidak ada validasi format value (misal: harus enum) — semua value dianggap string bebas."]
- Bug di luar scope:
  - [contoh: "SystemParameter entity tidak ada field isReadOnly/isSystem — semua parameter bisa diupdate/delete. Kalau ada parameter sistem kritis, perlu follow-up untuk menambahkan flag ini."]
- DoD terpenuhi: [checklist]
```

---

## 8. Urutan Pengerjaan

1. Audit #3.
2. Interface `SystemParameterService` + Impl.
3. Refaktor controller → `ApiResponse`.
4. Tambah logging.
5. Bean Validation di DTO.
6. Self-verify.

---

*Issue B-8 adalah issue terakhir di chain retrofit. Setelah selesai semua (A + B-1 s/d B-8), seluruh kode backend Fase 2-3 sudah sesuai standar CLAUDE.md. Sisa pekerjaan: bersihkan file yang tidak terpakai (`ErrorResponse` lama kalau sudah tidak dipakai, class service lama yang sudah dipindah, dll.) — ini bisa dilakukan sebagai issue opsional terakhir "Retrofit Final Cleanup" jika diperlukan.*
