# Issue M-1.5 — Client Expiry Enforcement Mid-Session

> **Audience:** AI agent junior (model kecil). Agent **DILARANG** menjalankan `docker`, `docker compose`, `mvn`, `npm`, atau perintah build/run/test apa pun. Tugas murni menulis/mengedit kode. Manusia yang test manual.
>
> **Prasyarat:** Issue A + B-1 s/d B-8 selesai. Tidak bergantung ke issue M-1 lainnya, boleh dikerjakan kapan saja.

---

## 1. Judul & Ringkasan

Saat ini status/expiry client hanya dicek waktu login. Setelah login, token tetap valid dipakai sampai 24 jam meskipun akun client di-nonaktifkan atau expired di tengah sesi. Perlu pengecekan expiry di tiap request, bukan cuma saat login.

## 2. Scope

### File yang BOLEH diedit (existing):
- `Backend/src/main/java/com/undangan/online/security/JwtAuthenticationFilter.java`
- `Backend/src/main/java/com/undangan/online/exception/GlobalExceptionHandler.java`

### File BARU: tidak ada.

### DILARANG keras:
- Folder `Sql/`, `Docs/`, `Frontend/`, `Database/`, `TemaUndangan/`, `Musics/`, `Images/`.
- `pom.xml`. Lombok. Entity yang sudah ada (hanya BACA `Client.java`, tidak diubah).

---

## 3. Requirement Fungsional

### `JwtAuthenticationFilter`
- Setelah extract `clientId` dari token:
  - Jika `clientId == null` (misal request dari SUPER_ADMIN yang tidak terikat client) → lanjut seperti biasa, skip pengecekan ini.
  - Jika `clientId != null` → query `ClientRepository.findById(clientId)`.
    - Kalau `Client` tidak ditemukan → `log.warn("Client {} tidak ditemukan saat validasi token", clientId)` → throw `AuthException("CLIENT_EXPIRED", "Akun tidak ditemukan", 401)`.
    - Kalau ditemukan tapi `status` bukan `"active"` ATAU `expiresAt` sudah lewat (`isBefore(LocalDate.now())` atau setara) → `log.warn("Client {} sudah expired/nonaktif, request ditolak", clientId)` → throw `AuthException("CLIENT_EXPIRED", "Akun sudah expired atau nonaktif", 401)`.
- **Pastikan endpoint publik (`/api/v1/public/**`) dan endpoint login (`/api/v1/auth/**`) TIDAK melewati pengecekan ini** — filter ini hanya relevan untuk request yang sudah lolos autentikasi JWT dan punya `clientId`. Cek posisi filter di chain supaya tidak menghalangi request yang memang seharusnya publik.

### `GlobalExceptionHandler`
- Handler `AuthException` — untuk `errorCode = CLIENT_EXPIRED` → response HTTP status **401** (bukan 403, karena ini soal validitas sesi, bukan soal hak akses).
- Untuk `errorCode` lain yang sudah ada (`FORBIDDEN`, `USER_INACTIVE`, dll.) → tetap pakai `ex.getStatus()` seperti sebelumnya, jangan diubah.

### Catatan performa (opsional, tidak wajib di issue ini)
- Pengecekan ini menambah 1 query DB per request yang terautentikasi. Untuk skala puluhan client, ini tidak masalah. **JANGAN** menambahkan caching/Redis untuk ini di issue ini — di luar scope, cukup dicatat sebagai potensi optimasi kalau nanti skala naik jauh.

---

## 4. Requirement Teknis

### Konvensi
- SLF4J Logger untuk log WARN saat client expired/nonaktif ditolak (sertakan `clientId`, jangan log detail lain yang tidak perlu).
- Tidak ada entity/DTO baru di issue ini — murni logic tambahan di filter yang sudah ada.

---

## 5. Definition of Done
- [ ] `JwtAuthenticationFilter` — cek status & expiry `Client` untuk tiap request yang punya `clientId`, skip kalau `clientId == null`.
- [ ] Endpoint `/api/v1/public/**` dan `/api/v1/auth/**` tidak terpengaruh/tidak diblokir oleh pengecekan ini.
- [ ] `AuthException("CLIENT_EXPIRED", ...)` di-throw dengan pesan yang jelas untuk 2 kondisi (tidak ditemukan vs expired/nonaktif).
- [ ] `GlobalExceptionHandler` — `CLIENT_EXPIRED` → HTTP 401.
- [ ] Log WARN tercatat setiap kali request ditolak karena expiry, menyertakan `clientId`.
- [ ] Tidak ada perubahan behavior untuk request SUPER_ADMIN (yang tidak punya `clientId`).
- [ ] Tidak ada caching/Redis ditambahkan (di luar scope).

---

## 6. Batasan Tegas
1. JANGAN jalankan build tool/docker.
2. JANGAN ubah `Sql/`, `Frontend/`, `Docs/`, `Database/`.
3. JANGAN Lombok, JANGAN tambah dependency.
4. JANGAN ubah entity `Client.java` — hanya baca via `ClientRepository`.
5. JANGAN tambahkan caching layer di issue ini.
6. Pastikan logic baru ini tidak sampai memblokir endpoint publik atau login.

---

## 7. Catatan Referensi
- `Docs/CLAUDE.md` bagian 6-8.
- `Backend/src/main/java/com/undangan/online/entity/Client.java` — field `expiresAt`, `status`.
- `Backend/src/main/java/com/undangan/online/repository/ClientRepository.java` — query client by id.
- `Backend/src/main/java/com/undangan/online/exception/AuthException.java` — pola error code + HTTP status existing.
- `Backend/src/main/java/com/undangan/online/config/SecurityConfig.java` — urutan filter chain, path yang dikecualikan dari auth.