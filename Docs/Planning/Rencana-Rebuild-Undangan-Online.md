# Rencana Rebuild "Undangan Online" — Flow Vibecoding + Template Prompt

Dokumen ini jadi pegangan kerja kamu. Konteks: kamu sudah punya HTML Web Dashboard (demo) dan CLAUDE.md, sekarang mau rebuild jadi project production dengan Spring Boot 4.1.0 (Java 21, Maven) + PostgreSQL 18 + React 19.2.x, semua jalan di Docker, dan workflow-nya: **kamu buat issue.md → agent murah implementasi → kamu sendiri yang test/deploy via Docker**.

---

## 1. Struktur Folder Final

```
undangan-online/
├── docker-compose.yml              # orchestrator penuh (dev) - opsional, gabung semua service
├── README.md
│
├── backend/
│   ├── Dockerfile
│   ├── docker-compose.yml          # standalone dev (backend + koneksi ke db)
│   ├── pom.xml
│   ├── src/main/java/...
│   ├── src/main/resources/
│   │   └── application.properties  # (+ application-dev.properties, application-prod.properties)
│   └── .dockerignore
│
├── database/
│   ├── docker-compose.yml          # postgres 18 standalone
│   └── postgresql.conf (opsional)
│
├── frontend/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── package.json
│   ├── vite.config.js (atau CRA config)
│   └── src/
│
├── tema-undangan/                  # storage tema, di-mount jadi volume ke backend
│   └── {theme-slug}/
│       ├── thumbnail.jpg
│       └── template-files...
│
├── musics/                         # storage musik, volume mount
├── images/                         # storage gambar upload, volume mount
│
├── sql/
│   ├── 001_init_schema.sql
│   ├── 002_seed_master_data.sql
│   ├── 003_seed_dummy_data.sql
│   └── README.md                   # urutan & cara jalanin manual
│
└── docs/
    ├── PRD-Undangan-Online.md
    ├── CLAUDE.md
    ├── web-dashboard-html/         # demo HTML kamu yang sudah ada, jadi referensi desain
    └── issues/
        ├── phase-0/
        ├── phase-1/
        └── ...
```

**Prinsip volume mounting:** `tema-undangan/`, `musics/`, `images/` di-mount sebagai Docker volume ke container backend (misal ke `/app/storage/...`), supaya file yang diupload lewat dashboard persist di host meskipun container di-rebuild. Ini penting kamu tegaskan di issue.md fase backend supaya agent tidak nyimpen file di dalam image.

**Kenapa `sql/` terpisah dari `database/`:** `database/` isinya cuma docker-compose untuk jalanin Postgres kosong. `sql/` isinya script yang kamu jalankan manual (psql/DBeaver) — sesuai requirement kamu bahwa kamu yang pegang kendali schema, bukan auto-migrate dari agent (aman dari resiko agent ngerusak data).

---

## 2. Fase Pengerjaan

Karena tidak bisa 1 prompt langsung jadi, pecah jadi fase kecil yang berurutan, tiap fase = 1 (atau beberapa) issue.md, tiap issue = 1 sesi agent.

### Fase 0 — Bootstrap & Infrastruktur
Tujuan: skeleton folder, docker compose semua service bisa `up` dan saling terhubung, meski belum ada fitur.
- Buat struktur folder sesuai bagian 1.
- `backend`: Spring Boot skeleton kosong (health check endpoint `/actuator/health` saja), Dockerfile, application.properties dengan datasource ke Postgres.
- `database`: docker-compose Postgres 18, expose port, volume persist.
- `frontend`: skeleton React 19.2.x (Vite direkomendasikan), Dockerfile, halaman "Hello".
- Root `docker-compose.yml` yang include/reference ketiga service, network sama, backend depends_on database (healthcheck).
- Kamu test: `docker compose up` semua nyala, backend bisa connect ke db, frontend bisa fetch health check backend.

### Fase 1 — Database Schema
Tujuan: rancang skema tabel lengkap berdasarkan PRD (client, invitation, guest, RSVP, theme, music, image, role/permission, dst).
- Output: file-file di `sql/001_init_schema.sql` dst, bukan JPA auto-ddl. Kamu jalankan manual.
- Sertakan ERD sederhana (bisa minta agent tulis dalam bentuk komentar/mermaid di dalam issue.md, bukan di kode).
- Fase ini **tidak butuh agent coding** — bisa kamu kerjakan sendiri dengan bantuan Claude langsung (bukan agent murah), karena ini fondasi krusial.

### Fase 2 — Backend Core & Auth
- Entity + Repository (JPA, tapi `ddl-auto=validate` atau `none`, bukan `update`, karena schema dari SQL manual).
- Konfigurasi security dasar: JWT atau session, role (Super Admin, Client, Guest/public).
- Endpoint login, register client (jika ada flow-nya).
- Config file upload path (yang mount ke `images/`, `musics/`, `tema-undangan/`).

### Fase 3 — Backend Modul per Fitur (issue terpisah per modul)
Pecah per domain, tiap modul 1 issue.md:
1. Client Management (CRUD, status/expiry) — Super Admin
2. User & Role/Permission Management — Super Admin
3. Invitation/Undangan CRUD (data pasangan, event, galeri) — Client
4. Guest List & RSVP + Guestbook — public + admin moderation
5. Theme Management (upload ke `tema-undangan/`, assign ke invitation)
6. Music Library (upload ke `musics/`, assign ke invitation)
7. Image/Gallery Management (upload ke `images/`)
8. System Parameter (Super Admin)

### Fase 4 — Frontend Setup & Layout
- Routing dasar (React Router), layout sidebar per role (kamu sudah punya referensi visualnya di HTML dashboard lama — jadikan acuan desain, bukan asal generate).
- State management (Context/Zustand/Redux — pilih yang ringan).
- Integrasi API client (axios/fetch) + auth flow.

### Fase 5 — Frontend Modul per Fitur
- Mirror dari Fase 3, satu-satu: halaman Client Management, User Management, Invitation Editor, RSVP & Guestbook viewer, Theme/Music/Image uploader, dst.
- Manfaatkan HTML dashboard lama sebagai **referensi visual/komponen**, minta agent "port" tampilan itu ke React component, bukan desain dari nol.

### Fase 6 — Guest-Facing Invitation Page (Web 3)
- Halaman publik render undangan berdasarkan tema yang dipilih + data dari invitation.
- Bisa dipisah lagi: cover/envelope, countdown, RSVP form, galeri, musik player.
- Kamu sudah punya versi HTML statis (Kirana & Danendra) — jadi referensi kuat untuk porting ke React + data dinamis dari backend.

### Fase 7 — Integrasi Penuh & Deployment Dev
- Pastikan root `docker-compose.yml` menjalankan backend+frontend+db bersamaan, env var konsisten.
- Kamu jalankan smoke test manual sendiri (bukan agent) sesuai checklist di bagian 5.
- Baru setelah ini stabil, mikirin fase produksi (belakangan, sesuai request kamu).

---

## 3. Template Prompt — Generate issue.md

Pakai prompt ini ke Claude (kamu, bukan agent murah) tiap mau mulai 1 task baru:

```
Kamu bertindak sebagai tech lead. Buatkan file issue.md untuk task berikut,
yang nantinya akan dikerjakan oleh AI agent junior (murah, model kecil) yang
TIDAK boleh melakukan testing/validasi sendiri (server dev saya terbatas).

Konteks project:
- Baca docs/PRD-Undangan-Online.md dan docs/CLAUDE.md sebagai acuan utama.
- Stack: Backend Java 21 + Spring Boot 4.1.0 (Maven, jar), Frontend React 19.2.x,
  Database PostgreSQL 18 (schema di-manage manual lewat file sql/, JANGAN pakai
  ddl-auto=update).
- Struktur folder project: [tempel bagian 1 di atas / atau reference file]
- Semua service jalan di Docker (docker compose), agent tidak perlu (dan tidak boleh)
  menjalankan docker/testing sendiri.

Task yang ingin dikerjakan:
[Fase X - nama task, misal: "Modul Client Management (CRUD + status/expiry) - Super Admin"]

Buatkan issue.md dengan struktur berikut:
1. **Judul & Ringkasan** singkat task ini.
2. **Scope** — file/folder apa saja yang boleh disentuh agent (batasi supaya tidak
   nyerempet modul lain).
3. **Requirement fungsional** — daftar detail, mengacu ke PRD.
4. **Requirement teknis** — nama class/package, konvensi penamaan, lokasi file,
   dependency yang boleh dipakai (jangan tambah library baru tanpa izin).
5. **Definition of Done** — checklist yang bisa agent centang sendiri tanpa perlu
   menjalankan aplikasi (misal: "kode compile secara logis", "semua field PRD
   ter-cover", "tidak ada TODO tersisa").
6. **Batasan tegas**:
   - Jangan menjalankan `docker`, `mvn`, `npm run`, atau perintah test apa pun.
   - Jangan mengubah schema database / file di folder sql/.
   - Jangan menyentuh file di luar scope yang disebutkan di poin 2.
7. **Catatan referensi** — sebutkan file HTML dashboard lama yang relevan
   (jika untuk frontend) sebagai referensi visual.

Tulis issue.md ini dalam bahasa Indonesia yang jelas dan actionable, siap saya
copy-paste ke agent.
```

---

## 4. Template Prompt — Untuk Agent Murah (Eksekusi)

Ini yang kamu tempel ke AI agent murah setelah issue.md jadi:

```
Kamu adalah developer yang mengerjakan task sesuai issue.md yang saya berikan.

ATURAN WAJIB:
1. Baca issue.md secara menyeluruh sebelum mulai coding.
2. Hanya kerjakan yang ada di dalam "Scope". Jangan menyentuh file lain.
3. JANGAN menjalankan docker, mvn, npm, atau perintah build/test/run apa pun.
   Tugasmu HANYA menulis/mengedit kode. Saya yang akan test sendiri di
   environment saya.
4. JANGAN mengubah file di folder sql/ atau schema database. Jika kamu butuh
   kolom/tabel baru yang belum ada, TULISKAN sebagai catatan di akhir jawaban,
   jangan langsung eksekusi.
5. Ikuti konvensi kode yang sudah ada di project (package structure, naming,
   style) — cek file existing dulu sebelum menambah yang baru.
6. Jika ada bagian issue.md yang ambigu, buat asumsi paling masuk akal,
   tulis asumsimu di akhir, jangan berhenti nanya.
7. Di akhir, berikan ringkasan singkat: file apa saja yang dibuat/diubah,
   dan bagian mana dari Definition of Done yang menurutmu sudah terpenuhi.

Berikut issue.md-nya:
[paste isi issue.md]
```

---

## 5. Template Checklist Testing Manual (buat kamu sendiri)

Karena testing dipegang kamu sendiri via Docker, siapkan checklist standar tiap selesai 1 issue:

```
[ ] docker compose build tanpa error (service terkait)
[ ] docker compose up, semua container healthy
[ ] Endpoint/halaman baru bisa diakses
[ ] Data yang diinput tersimpan benar di Postgres (cek manual via psql/DBeaver)
[ ] File upload (jika ada) masuk ke folder volume yang benar (images/musics/tema-undangan)
[ ] Tidak ada error di log container (docker compose logs -f)
[ ] Role/permission sesuai (kalau relevan) — test sebagai role berbeda
[ ] Tidak ada perubahan tak terduga di luar scope issue
```

---

## 6. Tips Refinement CLAUDE.md

Supaya makin hemat token & terstruktur, pastikan CLAUDE.md kamu punya bagian:
- **Larangan eksplisit**: "Jangan jalankan docker/mvn/npm run apa pun", "Jangan ubah folder sql/".
- **Referensi wajib dibaca**: path ke PRD dan ke file HTML dashboard lama.
- **Konvensi struktur folder** (tempel ringkas bagian 1 dokumen ini).
- **Format output yang diharapkan** di akhir tiap task (ringkasan file yang diubah).

Kalau mau, saya bisa bantu susun ulang isi CLAUDE.md kamu — tinggal upload file-nya.

---

## 7. Urutan Eksekusi Rekomendasi

1. Fase 0 (kamu + Claude langsung, bukan agent murah — ini fondasi).
2. Fase 1 — schema SQL (kamu + Claude langsung).
3. Fase 2 — baru mulai delegasi ke agent murah pakai template di atas.
4. Fase 3 → 6, satu modul satu issue, jangan gabung banyak modul dalam 1 issue (bikin agent murah gampang salah/ngarang).
5. Fase 7 setelah semua modul lolos checklist manual.

Kalau kamu mau, langkah selanjutnya saya bisa langsung bantu tulis **issue.md untuk Fase 0** supaya kamu tinggal jalankan ke agent.
