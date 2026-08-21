# CLAUDE.md — Undangan Online

Dokumen ini adalah aturan wajib untuk setiap AI agent (termasuk Claude Code) yang bekerja di project ini. Baca ini SEBELUM membaca file lain.

---

## 1. Konteks Project (baca sekali, jangan re-read tiap task)

- Nama: **Undangan Online** — platform undangan digital, 3 sisi interface: Super Admin, Client (pasangan pengantin), Guest (tamu publik).
- Dokumen sumber kebenaran: `Docs/PRD-Undangan-Online.md`. Kalau requirement tidak jelas, cek di sana dulu sebelum bertanya/berasumsi.
- Referensi desain visual: `Docs/web-dashboard-html/` — HTML demo yang sudah disetujui. Untuk task Frontend, PORT tampilan dari sini, jangan desain ulang dari nol.
- Folder `Docs/planning/` berisi catatan proses internal, TIDAK perlu dibaca untuk mengerjakan task apa pun.

## 2. Stack (fixed, jangan diganti tanpa izin eksplisit)

| Layer | Teknologi |
|---|---|
| Backend | Java 21, Spring Boot 4.1.0, Maven, packaging jar, konfigurasi via `application.properties` |
| Database | PostgreSQL 18, schema **manual via file SQL**, bukan JPA auto-ddl |
| Frontend | React 19.2.x |
| Infra | Docker Compose (dev only untuk saat ini) |

Jangan menambah library/dependency baru di luar yang sudah ada tanpa disebutkan eksplisit di issue.md.

## 3. Struktur Folder

```
undangan-online/
├── Backend/          # Spring Boot app
├── Database/         # docker-compose Postgres
├── Frontend/         # React app
├── TemaUndangan/     # storage tema (volume mount, JANGAN commit isi biner besar)
├── Musics/            # storage musik (volume mount)
├── Images/            # storage gambar (volume mount)
├── Sql/               # SQL manual, dijalankan oleh manusia — LIHAT ATURAN #4
└── Docs/               # PRD, CLAUDE.md, referensi HTML lama, issue.md per task
```

## 4. ATURAN KERAS — PELANGGARAN = TASK DITOLAK

1. **JANGAN pernah menjalankan** `docker`, `docker compose`, `mvn`, `npm run`, `npm start`, `npm test`, atau perintah build/run/test apa pun. Tugasmu murni menulis/mengedit kode. Testing dilakukan manual oleh manusia di luar sesi ini.
2. **JANGAN mengubah, menghapus, atau menambah file di folder `Sql/`.** Kalau task butuh perubahan schema (tabel/kolom baru), TULISKAN kebutuhan itu sebagai catatan di akhir jawaban — jangan eksekusi sendiri.
3. **JANGAN menyentuh file di luar scope** yang disebutkan di issue.md yang sedang dikerjakan. Kalau perlu ubah file lain (misal shared config), sebutkan di catatan akhir, minta konfirmasi dulu.
4. **JANGAN pakai `ddl-auto=update` atau `create`** di konfigurasi JPA. Entity harus match schema yang sudah ada di `Sql/`, gunakan `validate` atau `none`.
5. File upload (tema/musik/gambar) HARUS disimpan ke folder volume (`TemaUndangan/`, `Musics/`, `Images/`) via path config, BUKAN disimpan sebagai blob di Database atau di dalam image Docker.
6. Kalau ada bagian task yang ambigu: buat asumsi paling masuk akal berdasarkan PRD, jalan terus, tulis asumsimu di ringkasan akhir. Jangan berhenti untuk bertanya di tengah kalau masih bisa lanjut dengan asumsi wajar.

## 5. Aturan Hemat Token (WAJIB diikuti tiap sesi)

- **Jangan baca seluruh isi folder/project di awal sesi.** Baca hanya file yang relevan dengan task yang sedang dikerjakan (biasanya: issue.md terkait + file yang akan diedit + PRD bagian relevan saja, bukan full PRD kalau tidak perlu).
- **Jangan print/quote ulang isi file besar di response.** Kalau perlu rujuk sesuatu, sebut nama file & baris, jangan tempel ulang isinya.
- **Gunakan edit bertarget** (diff/patch pada bagian yang berubah), bukan menulis ulang seluruh file kalau perubahannya kecil.
- **Jangan jelaskan langkah demi langkah secara panjang** sebelum coding. Langsung eksekusi, baru beri ringkasan singkat di akhir.
- **Satu task = satu issue.md.** Jangan mengerjakan atau membahas task lain di luar issue.md yang sedang aktif dalam sesi yang sama, meskipun terlihat "sekalian".
- Kalau butuh konteks dari file lain yang belum dibaca, baca sekali saja di awal, jangan re-read berulang kali dalam sesi yang sama.

## 6. Konvensi Kode

- **Backend**: struktur package per domain (`controller`, `service`, `repository`, `entity`, `dto`) di dalam masing-masing modul (mis. `client`, `invitation`, `guest`, `theme`, `music`). Ikuti pola yang sudah ada di modul lain sebelum menambah yang baru — cek dulu, jangan asal generate gaya baru.
- **Frontend**: struktur folder per fitur (`features/client-management/`, `features/invitation-editor/`, dst), komponen reusable di `components/`. Styling mengikuti tema visual dari `Docs/web-dashboard-html/`.
- **Naming**: Bahasa Inggris untuk kode (variable, class, endpoint), Bahasa Indonesia boleh untuk komentar/dokumentasi kalau perlu.
- **REST endpoint**: prefix `/api/v1/...`, response format konsisten (cek endpoint modul lain yang sudah ada sebagai referensi sebelum bikin format baru).

## 7. Format Output Wajib di Akhir Setiap Task

Setiap selesai mengerjakan task, tutup dengan ringkasan singkat (bukan penjelasan panjang):

```
## Ringkasan
- File dibuat: [daftar]
- File diubah: [daftar]
- Asumsi yang diambil: [daftar, kalau ada]
- Kebutuhan schema baru (jika ada): [daftar, TIDAK dieksekusi]
- Definition of Done dari issue.md yang terpenuhi: [checklist]
```

## 8. Role & Model Bisnis (ringkas, detail lengkap di PRD)

- Super Admin buat akun client secara manual (request-based, bukan self-service pendaftaran publik).
- Skala awal: puluhan client aktif — jangan over-engineer (tidak perlu sharding, queue system, caching layer berat di fase ini).
- 3 role: Super Admin (kelola semua client & master data), Client (kelola undangan sendiri), Guest (publik, tanpa login, akses halaman undangan + RSVP + guestbook).

---

*Kalau ada instruksi di issue.md yang bertentangan dengan file ini, file CLAUDE.md ini yang menang, kecuali disebutkan eksplisit sebagai pengecualian oleh manusia.*