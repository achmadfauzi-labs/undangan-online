# CLAUDE.md — Undangan Online

Dokumen ini adalah aturan wajib untuk setiap AI agent (termasuk Claude Code) yang bekerja di project ini. Baca ini SEBELUM membaca file lain.

---

## 1. Konteks Project (baca sekali, jangan re-read tiap task)

- Nama: **Undangan Online** — platform undangan digital, 3 sisi interface: Admin, Client (pasangan pengantin), Guest (tamu publik).
- Dokumen sumber kebenaran: `Docs/PRD-Undangan-Online.md`. Kalau requirement tidak jelas, cek di sana dulu sebelum bertanya/berasumsi.
- Referensi desain visual: `Docs/web-dashboard-html/` — HTML demo yang sudah disetujui. Untuk task frontend, PORT tampilan dari sini, jangan desain ulang dari nol.
- Folder `Docs/planning/` berisi catatan proses internal (punya Gooojay) — TIDAK perlu dan TIDAK boleh dibaca untuk mengerjakan task apa pun.

## 2. Stack (fixed, jangan diganti tanpa izin eksplisit)

| Layer | Teknologi |
|---|---|
| Backend | Java 21, Spring Boot 4.1.0, Maven, packaging jar, konfigurasi via `application.properties` |
| Database | PostgreSQL 18, schema **manual via file SQL**, bukan JPA auto-ddl |
| Frontend | React 19.2.x |
| Infra | Docker Compose (dev only untuk saat ini) |

**Wajib cek versi terbaru sebelum menambah/mengupdate dependency:** sebelum menuliskan versi library/dependency apa pun di `pom.xml` atau `package.json` (di luar versi fixed di tabel atas), cek dokumentasi terkini via **context7** dulu untuk memastikan versi yang dipakai up-to-date dan kompatibel dengan stack di atas. Jangan menebak versi dari memori/training data.

Jangan menambah library/dependency baru di luar yang sudah ada tanpa disebutkan eksplisit di issue.md.

## 3. Struktur Folder

```
undangan-online/
├── Backend/           # Spring Boot app
├── Database/          # docker-compose Postgres
├── Frontend/          # React app
├── TemaUndangan/       # storage tema (volume mount, JANGAN commit isi biner besar)
├── Musics/             # storage musik (volume mount)
├── Images/             # storage gambar (volume mount)
├── Sql/                # SQL manual, dijalankan oleh manusia — LIHAT ATURAN #4
└── Docs/               # PRD, CLAUDE.md, referensi HTML lama, issue.md per task
```

Nama folder top-level pakai **PascalCase**, ikuti persis seperti di atas (`Backend`, bukan `backend`). Konsisten-kan ini di semua path yang ditulis di kode, docker-compose, maupun dokumentasi.

## 4. ATURAN KERAS — PELANGGARAN = TASK DITOLAK

1. **Setiap menyelesaikan issue Kamu harus melakaukan Testing** `docker`, `docker compose`, `mvn`, `npm run`, `npm start`, `npm test`, atau perintah build/run/test apa pun untuk menverifikasi apakah sudah mudah sudah sesuai dan tidak ada bug.
2. **JANGAN mengubah, menghapus, atau menambah file di folder `Sql/`.** Kalau task butuh perubahan schema (tabel/kolom baru), TULISKAN kebutuhan itu sebagai catatan di akhir jawaban — jangan eksekusi sendiri.
3. **JANGAN menyentuh file di luar scope** yang disebutkan di issue.md yang sedang dikerjakan. Kalau perlu ubah file lain (misal shared config), sebutkan di catatan akhir, minta konfirmasi dulu.
4. **JANGAN pakai `ddl-auto=update` atau `create`** di konfigurasi JPA. Entity harus match schema yang sudah ada di `Sql/`, gunakan `validate` atau `none`.
5. File upload (tema/musik/gambar) HARUS disimpan ke folder volume (`TemaUndangan/`, `Musics/`, `Images/`) via path config, BUKAN disimpan sebagai blob di database atau di dalam image Docker.
6. Kalau ada bagian task yang ambigu: buat asumsi paling masuk akal berdasarkan PRD, jalan terus, tulis asumsimu di ringkasan akhir. Jangan berhenti untuk bertanya di tengah kalau masih bisa lanjut dengan asumsi wajar.

## 5. Aturan Hemat Token (WAJIB diikuti tiap sesi)

- **Jangan baca seluruh isi folder/project di awal sesi.** Baca hanya file yang relevan dengan task yang sedang dikerjakan (biasanya: issue.md terkait + file yang akan diedit + PRD bagian relevan saja, bukan full PRD kalau tidak perlu).
- **Jangan print/quote ulang isi file besar di response.** Kalau perlu rujuk sesuatu, sebut nama file & baris, jangan tempel ulang isinya.
- **Gunakan edit bertarget** (diff/patch pada bagian yang berubah), bukan menulis ulang seluruh file kalau perubahannya kecil.
- **Jangan jelaskan langkah demi langkah secara panjang** sebelum coding. Langsung eksekusi, baru beri ringkasan singkat di akhir.
- **Satu task = satu issue.md.** Jangan mengerjakan atau membahas task lain di luar issue.md yang sedang aktif dalam sesi yang sama, meskipun terlihat "sekalian".
- Kalau butuh konteks dari file lain yang belum dibaca, baca sekali saja di awal, jangan re-read berulang kali dalam sesi yang sama.

## 6. Konvensi Kode Backend — Clean Code & OOP (WAJIB)

- **Prinsip OOP ditegakkan**: enkapsulasi (jangan expose field entity langsung tanpa getter/setter atau builder), single responsibility per class, gunakan interface untuk service layer (`XxxService` interface + `XxxServiceImpl`) supaya mudah di-mock/di-extend.
- **Layered architecture ketat**: `controller` → `service` → `repository` → `entity`. Controller TIDAK boleh berisi business logic, cuma terima request, panggil service, kembalikan response. Business logic hidup di service layer.
- **DTO wajib dipakai** untuk request/response — jangan expose Entity JPA langsung ke luar (controller tidak boleh return Entity, harus return DTO/Response object).
- **Method pendek & fokus** — kalau satu method mengerjakan banyak hal berbeda, pecah jadi private method dengan nama jelas. Hindari god-class (satu class yang menangani terlalu banyak tanggung jawab).
- **Struktur package per domain**, konsisten di semua modul:
  ```
  com.undanganonline.backend.<domain>/
  ├── controller/
  ├── service/
  ├── service/impl/
  ├── repository/
  ├── entity/
  ├── dto/request/
  ├── dto/response/
  └── exception/ (kalau ada exception khusus domain ini)
  ```
- Sebelum menambah modul baru, cek dulu struktur modul yang sudah ada (kalau sudah ada dari Fase 2/3) supaya konsisten — jangan bikin pola baru yang beda sendiri.

## 7. Format Response API (WAJIB, semua endpoint konsisten)

Semua response dari backend (sukses maupun gagal) HARUS mengikuti format berikut, dibungkus lewat satu wrapper/handler global (misal `ApiResponse<T>` + `@ControllerAdvice` untuk exception handling), bukan ditulis manual berulang di tiap controller:

```json
{
  "success": true,
  "message": "User logged in successfully",
  "data": { }
}
```

- `success`: boolean, `true` untuk response 2xx, `false` untuk error.
- `message`: string deskriptif dalam Bahasa Indonesia atau Inggris (konsisten pilih satu — default Bahasa Indonesia untuk pesan user-facing, kecuali sudah ada standar lain di project).
- `data`: berisi payload hasil (object, array, atau `null` kalau tidak ada data, misal untuk aksi delete).
- Untuk kondisi error, tambahkan field `errors` (opsional, array of string/object) berisi detail validasi jika relevan, tetap dalam struktur yang sama:
  ```json
  {
    "success": false,
    "message": "Validation failed",
    "data": null,
    "errors": ["email is required", "password must be at least 8 characters"]
  }
  ```
- Implementasikan ini SEKALI sebagai generic response wrapper + global exception handler di awal (kalau belum ada dari fase sebelumnya), lalu semua controller pakai wrapper yang sama — jangan bikin format response manual berbeda-beda per endpoint.

## 8. Logging (WAJIB, informatif)

- Setiap operasi penting (create/update/delete, login, upload file, perubahan status client) HARUS dicatat ke log dengan level yang sesuai:
  - `INFO` — operasi berhasil yang penting untuk audit (misal "Client {id} created by admin {adminId}")
  - `WARN` — kondisi tidak normal tapi masih ditangani (misal percobaan akses ke data client lain, expiry mendekati)
  - `ERROR` — exception/kegagalan yang perlu perhatian, sertakan stack trace
- Format log konsisten, minimal sertakan: aksi yang terjadi, entitas/ID terkait, dan aktor (user/role yang melakukan) — jangan log pesan generik seperti `"error occurred"` tanpa konteks.
- **Jangan log data sensitif** (password, token JWT mentah, data pribadi tamu secara lengkap) — mask atau exclude field tersebut.
- Gunakan SLF4J (`Logger`/`LoggerFactory`, bukan `System.out.println`) di setiap service layer untuk operasi yang disebutkan di atas.
- Contoh pola: `log.info("Invitation {} created for client {}", invitationId, clientId);`

## 9. Format Output Wajib di Akhir Setiap Task

Setiap selesai mengerjakan task, tutup dengan ringkasan singkat (bukan penjelasan panjang):

```
## Ringkasan
- File dibuat: [daftar]
- File diubah: [daftar]
- Asumsi yang diambil: [daftar, kalau ada]
- Kebutuhan schema baru (jika ada): [daftar, TIDAK dieksekusi]
- Versi dependency yang dicek via context7 (jika ada): [daftar]
- Definition of Done dari issue.md yang terpenuhi: [checklist]
```

## 10. Role & Model Bisnis (ringkas, detail lengkap di PRD)

- Admin buat akun client secara manual (request-based, bukan self-service pendaftaran publik).
- Skala awal: puluhan client aktif — jangan over-engineer (tidak perlu sharding, queue system, caching layer berat di fase ini).
- 3 role: Admin (kelola semua client & master data), Client (kelola undangan sendiri), Guest (publik, tanpa login, akses halaman undangan + RSVP + guestbook via prefix `/api/v1/public/**`).

---

*Kalau ada instruksi di issue.md yang bertentangan dengan file ini, file CLAUDE.md ini yang menang, kecuali disebutkan eksplisit sebagai pengecualian oleh manusia.*