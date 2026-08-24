# Issue M-1.4 — Structured Logging

> **Audience:** AI agent junior (model kecil). Agent **DILARANG** menjalankan `docker`, `docker compose`, `mvn`, `npm`, atau perintah build/run/test apa pun. Tugas murni menulis/mengedit kode. Manusia yang test manual.
>
> **Prasyarat:** Issue A + B-1 s/d B-8 selesai. M-1.1 disarankan selesai dulu (ada utility class yang dipakai ulang di sini).

---

## 1. Judul & Ringkasan

Logback masih default, tidak ada Request ID/MDC correlation, sulit menelusuri log satu request yang sama di tengah banyak request bersamaan.

## 2. Scope

### File yang BOLEH diedit (existing):
- `Backend/src/main/java/com/undangan/online/config/SecurityConfig.java` — daftarkan filter baru.

### File BARU yang WAJIB dibuat:
- `Backend/src/main/resources/logback-spring.xml`
- `Backend/src/main/java/com/undangan/online/filter/RequestLoggingFilter.java`

### DILARANG keras:
- Folder `Sql/`, `Docs/`, `Frontend/`, `Database/`, `TemaUndangan/`, `Musics/`, `Images/`.
- `pom.xml`. Lombok. Entity yang sudah ada.

---

## 3. Requirement Fungsional

### `logback-spring.xml`
- Pattern: `%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ} [%thread] %-5level %logger{36} [%X{requestId}] — %msg%n`
- Root level: INFO
- Package `com.undangan.online`: INFO
- Package `org.springframework.security`: WARN (jangan terlalu verbose)
- Output: file `logs/app.log` (rolling daily + size-based, max 10MB per file, retention 30 hari) **DAN** console.

### `RequestLoggingFilter` (`@Component`, extends `OncePerRequestFilter`)
- Setiap request masuk: generate `requestId` (`UUID.randomUUID().toString()`) → `MDC.put("requestId", requestId)`.
- Log incoming: `INFO "Incoming request: {} {} from {}"` — method, URI, IP (pakai `IpAddressUtil.extractClientIp()` dari M-1.1 kalau sudah ada; kalau belum, cek `X-Forwarded-For` lalu fallback `getRemoteAddr()` langsung di sini).
- Setelah `chain.doFilter()` selesai, hitung durasi → `INFO "Request {} {} completed in {}ms with status {}"`.
- Log request body — **HANYA** untuk `Content-Type: application/json`. **JANGAN** log body untuk `multipart/form-data` (file upload) — cukup log ukuran/nama field, bukan isi file.
- Mask field sensitif di body sebelum log: `password`, `confirmPassword`, `token`, `refreshToken` → ganti value dengan `***masked***`. Kalau `SensitiveDataMasker` dari M-1.1 sudah ada, pakai itu; kalau belum, tulis masking sederhana khusus untuk filter ini.
- **WAJIB** `finally { MDC.clear(); }` — supaya `requestId` tidak "bocor" ke request lain di thread yang sama (thread pool reuse).

### `SecurityConfig`
- Daftarkan `RequestLoggingFilter` via `addFilterBefore(requestLoggingFilter, JwtAuthenticationFilter.class)` — supaya request ID sudah ada sebelum proses auth, termasuk untuk request yang authnya gagal.

---

## 4. Requirement Teknis

### Package
- `com.undangan.online.filter/` (baru, kalau `JwtAuthenticationFilter` sudah ada di sini, ikuti lokasi yang sama).

### Konvensi
- Constructor injection. SLF4J Logger. JANGAN Lombok.
- Body request dibaca via `ContentCachingRequestWrapper` (bawaan Spring, bukan dependency baru) supaya body bisa dibaca ulang oleh controller setelah dibaca filter untuk logging.

### Performa
- Filter ini jalan di **setiap** request — pastikan logic membaca body & masking tidak dilakukan untuk request besar/multipart (skip body logging untuk `multipart/form-data`, cek `Content-Type` di awal sebelum baca body).

---

## 5. Definition of Done
- [ ] `logback-spring.xml` — pattern dengan `[%X{requestId}]`, output file rolling + console, retention 30 hari.
- [ ] `RequestLoggingFilter` — generate & set `requestId` ke MDC, log incoming + response time + status.
- [ ] Body JSON di-log dengan masking field sensitif; body multipart TIDAK di-log isinya.
- [ ] `finally { MDC.clear(); }` ada, tidak ada kondisi yang skip ini (termasuk saat exception).
- [ ] Filter terdaftar di `SecurityConfig` sebelum `JwtAuthenticationFilter`.
- [ ] Tidak ada perubahan behavior request/response selain penambahan logging (tidak mengubah body request yang diterima controller).

---

## 6. Batasan Tegas
1. JANGAN jalankan build tool/docker.
2. JANGAN ubah `Sql/`, `Frontend/`, `Docs/`, `Database/`.
3. JANGAN Lombok, JANGAN tambah dependency di luar yang sudah ada di `pom.xml`.
4. JANGAN log isi file upload (multipart content).
5. JANGAN log field sensitif dalam bentuk plaintext.
6. JANGAN lupa `MDC.clear()` di semua jalur keluar (termasuk exception).

---

## 7. Catatan Referensi
- `Docs/CLAUDE.md` bagian 6-8.
- `Backend/src/main/java/com/undangan/online/security/JwtAuthenticationFilter.java` — pola filter existing, ikuti gaya yang sama.
- `Backend/src/main/java/com/undangan/online/config/SecurityConfig.java` — filter chain existing.
- `Backend/src/main/java/com/undangan/online/util/IpAddressUtil.java` dan `SensitiveDataMasker.java` (dari M-1.1, kalau sudah ada).
