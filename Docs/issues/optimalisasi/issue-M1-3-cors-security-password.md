# Issue M-1.3 — CORS + Security Headers + Password Policy

> **Audience:** AI agent junior (model kecil). Agent **DILARANG** menjalankan `docker`, `docker compose`, `mvn`, `npm`, atau perintah build/run/test apa pun. Tugas murni menulis/mengedit kode. Manusia yang test manual.
>
> **Prasyarat:** Issue A + B-1 s/d B-8 selesai. Tidak bergantung ke M-1.1/M-1.2, boleh dikerjakan kapan saja setelahnya.

---

## 1. Judul & Ringkasan

TIDAK ada CORS config (frontend akan error saat call API dari origin berbeda). TIDAK ada security headers standar. Password policy saat ini hanya cek panjang minimal 8 karakter.

## 2. Scope

### File yang BOLEH diedit (existing):
- `Backend/src/main/java/com/undangan/online/config/SecurityConfig.java`
- `Backend/src/main/java/com/undangan/online/service/impl/AuthServiceImpl.java`
- `Backend/src/main/resources/application.properties`

### File BARU yang WAJIB dibuat:
- `Backend/src/main/java/com/undangan/online/config/CorsConfig.java`
- `Backend/src/main/java/com/undangan/online/config/SecurityHeadersFilter.java`
- `Backend/src/main/java/com/undangan/online/config/PasswordPolicyConfig.java`

### DILARANG keras:
- Folder `Sql/`, `Docs/`, `Frontend/`, `Database/`, `TemaUndangan/`, `Musics/`, `Images/`.
- `pom.xml`. Lombok. Entity yang sudah ada.

---

## 3. Requirement Fungsional

### CORS
- `CorsConfigurationSource` bean di `CorsConfig` (atau `SecurityConfig`) — allowed origin dari env `FRONTEND_ORIGIN_URL` (default `http://localhost:5173`).
- Allow methods: GET, POST, PUT, PATCH, DELETE, OPTIONS.
- Allow headers: Authorization, Content-Type.
- Allow credentials: `true`.

### Security Headers
`SecurityHeadersFilter` (`@Component`, filter setelah Spring Security) — tambahkan header ke response:
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 1; mode=block`
- `Strict-Transport-Security: max-age=31536000; includeSubDomains`
- `Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self'`
- **HSTS hanya relevan di HTTPS/prod.** Di dev (HTTP), header ini tidak berbahaya kalau tetap dikirim (browser akan abaikan di context HTTP), tapi tetap beri kondisi: cek env `APP_ENV` — kalau nilainya bukan `prod`, skip header `Strict-Transport-Security` saja (header lain tetap dikirim). Default `APP_ENV=dev`.

### Password Policy
Di `AuthServiceImpl.registerClient` — sebelum encode password, validasi:
- Panjang ≥ 8 karakter
- Mengandung huruf besar (A-Z)
- Mengandung huruf kecil (a-z)
- Mengandung digit (0-9)
- Mengandung karakter khusus (`!@#$%^&*()_+-=[]{}|;:,.<>?`)
- Kalau gagal → `AuthException("VALIDATION_ERROR", "<pesan spesifik yang gagal, misal: 'Password harus mengandung minimal 1 huruf besar'>", 400)`.
- **Validasi ini HANYA berlaku untuk registrasi/pembuatan password baru** (mis. `registerClient`, ganti password kalau ada endpoint-nya) — TIDAK mempengaruhi user existing yang sudah punya password lama, mereka tetap bisa login normal.
- `PasswordPolicyConfig` — helper class berisi method statis/instance untuk validasi, dipanggil dari `AuthServiceImpl`.

---

## 4. Requirement Teknis

### Package
- `com.undangan.online.config/` — semua file baru di issue ini masuk sini.

### Konvensi
- Constructor injection. SLF4J Logger untuk log ketika validasi password gagal (level INFO, JANGAN log password mentahnya). `ApiResponse<T>` wrapper. JANGAN Lombok.

### Environment Variable baru
| Variable | Deskripsi | Default |
|---|---|---|
| `FRONTEND_ORIGIN_URL` | CORS allowed origin | `http://localhost:5173` |
| `APP_ENV` | `dev` atau `prod`, dipakai untuk kondisi HSTS header | `dev` |

---

## 5. Definition of Done
- [ ] `CorsConfigurationSource` bean terdaftar, origin dari `FRONTEND_ORIGIN_URL`.
- [ ] `SecurityHeadersFilter` — 5 header wajib selalu ada, HSTS hanya saat `APP_ENV=prod`.
- [ ] Filter terdaftar di `SecurityConfig` (`addFilterAfter`).
- [ ] Password validation — 5 rule, `AuthException` 400 dengan pesan spesifik per rule yang gagal.
- [ ] User existing (password lama) tetap bisa login tanpa terpengaruh validasi baru.
- [ ] `FRONTEND_ORIGIN_URL` dan `APP_ENV` ada di `application.properties`.

---

## 6. Batasan Tegas
1. JANGAN jalankan build tool/docker.
2. JANGAN ubah `Sql/`, `Frontend/`, `Docs/`, `Database/`.
3. JANGAN Lombok, JANGAN tambah dependency.
4. JANGAN terapkan validasi password ke flow login (hanya ke registrasi/set password baru).
5. JANGAN log password mentah di mana pun, termasuk saat validasi gagal.

---

## 7. Catatan Referensi
- `Docs/CLAUDE.md` bagian 6-8.
- `Backend/src/main/java/com/undangan/online/config/SecurityConfig.java` — filter chain, role-based access existing.
- `Backend/src/main/java/com/undangan/online/exception/AuthException.java`
- `Backend/src/main/java/com/undangan/online/service/impl/AuthServiceImpl.java` — tempat menambahkan validasi password.
