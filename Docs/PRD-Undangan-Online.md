# PRD — Undangan Online

Status: **Draft v0.1** — mohon direview & diedit sebelum dijadikan acuan resmi untuk Fase 0.

---

## 1. Latar Belakang & Tujuan

Undangan Online adalah platform pembuatan undangan digital (fokus awal: pernikahan) dengan tiga sisi interface:
- **Web 1** — Dashboard Super Admin
- **Web 2** — Dashboard Client (pasangan pengantin)
- **Web 3** — Halaman undangan publik yang dilihat tamu

Tujuan produk: mempermudah Super Admin mengelola banyak undangan client dari satu tempat, dan mempermudah client mengedit undangan mereka sendiri tanpa perlu sentuh kode.

---

## 2. Model Bisnis

- **Request-based, bukan self-service.** Client tidak mendaftar sendiri secara publik. Super Admin yang membuat akun client secara manual (misal setelah ada pemesanan lewat WA/form terpisah di luar sistem).
- Setiap akun client punya **status aktif/nonaktif dan tanggal expiry** yang diatur oleh Super Admin (mengikuti masa berlaku paket yang dibeli, misal H-30 sebelum acara sampai beberapa waktu setelahnya — durasi pasti perlu dikonfirmasi/diisi di parameter sistem).
- Tidak ada modul pembayaran online di scope awal (pembayaran ditangani di luar sistem oleh admin).

## 3. Skala & Asumsi Teknis

- Target awal: **puluhan client aktif** — bukan ribuan.
- Arsitektur tetap **multi-tenant secara data** (1 database, dipisah per `client_id`/`invitation_id`), TAPI tidak perlu kompleksitas seperti sharding, caching layer berat, atau queue system di fase awal. Itu bisa ditambah belakangan kalau skala naik signifikan.
- Traffic terbesar diperkirakan datang dari **Web 3 (halaman tamu)** saat mendekati hari-H acara suatu client — desain schema & query untuk halaman ini sebaiknya sederhana dan cepat (banyak read, sedikit write kecuali RSVP/guestbook).

---

## 4. Role & Hak Akses

### 4.1 Super Admin
- Kelola akun client (create, edit, aktif/nonaktif, set tanggal expiry)
- Kelola user & role/permission (jika ada admin lain selain Super Admin sendiri)
- Lihat daftar tamu/guestbook lintas semua client (global guest list)
- Kelola master data: Tema Undangan, Musik, Parameter Sistem
- Kelola template undangan (upload tema baru ke `tema-undangan/`)

### 4.2 Client (pasangan pengantin)
- Login ke dashboard pribadi, lihat countdown ke hari acara
- Generate link undangan (dengan personalisasi nama tamu via query parameter)
- Edit data pasangan & acara (nama, tanggal, sesi acara, lokasi + link Google Maps)
- Upload & kelola galeri foto
- Pilih tema (dari tema yang disediakan Super Admin) dan musik latar (dari library musik)
- Moderasi guestbook/RSVP undangan miliknya sendiri (approve/hapus komentar tamu)

### 4.3 Guest (tamu, publik, tanpa login)
- Buka link undangan (dengan/tanpa nama personalisasi di URL)
- Lihat detail acara, profil pasangan, love story timeline, galeri foto
- Isi RSVP (hadir/tidak hadir + jumlah tamu, opsional)
- Isi guestbook / ucapan (tampil real-time di halaman jika sudah disetujui/langsung tampil — perlu diputuskan apakah perlu moderasi dulu atau langsung tampil)

---

## 5. Fitur per Halaman (berdasarkan demo yang sudah dibuat)

### 5.1 Web 3 — Halaman Undangan Tamu
- Cover/amplop dengan animasi buka (wax seal)
- Personalisasi nama tamu via query parameter URL
- Countdown real-time ke hari acara
- Profil pasangan pengantin
- Love story timeline
- Detail acara — mendukung multi-sesi (misal akad + resepsi), tiap sesi dengan lokasi & link Google Maps
- Galeri foto
- Form RSVP interaktif
- Guestbook (live, tamu lain bisa lihat ucapan yang masuk)
- Pemilihan tema visual — 6 tema tersedia di demo: ELEGAN, MODERN, BATIK, SAKURA, NATURE, WHITE (tema baru bisa ditambah lewat upload Super Admin)
- Musik latar (autoplay/toggle)

### 5.2 Web 1 — Dashboard Super Admin
- Manajemen client (CRUD, status, expiry)
- Manajemen user & role/permission (checkbox-based per modul)
- Global guest list (lintas semua client)
- Parameter sistem
- Manajemen template/tema undangan
- Manajemen library musik

### 5.3 Web 2 — Dashboard Client
- Dashboard personal dengan countdown
- Generator link undangan
- Editor data pasangan & acara (dengan upload foto)
- Manajemen galeri
- Pemilihan tema & musik
- Moderasi guestbook

---

## 6. Alur Utama (User Flow Ringkas)

1. Client memesan (di luar sistem) → Super Admin buat akun client baru dengan status aktif + tanggal expiry.
2. Client login ke Web 2 → isi data pasangan, acara, upload foto, pilih tema & musik.
3. Client generate link undangan → bagikan ke tamu (manual, lewat WA dsb), bisa dengan parameter nama tamu per orang.
4. Tamu buka link (Web 3) → lihat undangan → isi RSVP & ucapan.
5. Client pantau RSVP & guestbook dari Web 2, bisa moderasi ucapan.
6. Super Admin pantau seluruh client dari Web 1, bisa nonaktifkan akun yang sudah expired.

---

## 7. Non-Functional Requirements

- Halaman Web 3 harus ringan & cepat diakses dari mobile (mayoritas tamu buka dari HP via chat).
- Upload file (foto, musik, tema) disimpan sebagai file di storage (folder `images/`, `musics/`, `tema-undangan/`), bukan blob di database.
- Akses Web 1 & Web 2 wajib login + role-based access control.
- Web 3 sepenuhnya publik, tanpa login.

---

## 8. Di Luar Scope (Fase Awal)

- Pembayaran online / integrasi payment gateway
- Self-service registrasi client
- Multi-bahasa (fokus Bahasa Indonesia dulu)
- Notifikasi email/WhatsApp otomatis
- Mobile app native

---

## 9. Pertanyaan Terbuka (perlu kamu putuskan sebelum Fase 1 — schema database)

- [ ] Berapa lama masa aktif akun client secara default (30 hari setelah acara? 90 hari?), dan apakah bisa diperpanjang manual oleh Super Admin? masa aktif akun client default 30 hari, bisa diperpanjang manual oleh super admin
- [ ] Guestbook: tampil langsung tanpa moderasi, atau perlu approve dulu oleh client sebelum publish? langsung publish saja
- [ ] Apakah 1 client bisa punya lebih dari 1 undangan (misal ganti tema di tengah jalan, riwayat undangan lama), atau strictly 1 client = 1 undangan aktif? strictly 1 client = 1 undangan aktif
- [ ] RSVP: apakah tamu bisa RSVP lebih dari sekali (update jawaban), atau sekali submit terkunci? lebih dari sekali (update jawaban)
- [ ] Musik & tema: apakah client hanya bisa pilih dari yang disediakan Super Admin, atau client juga bisa upload musik/tema sendiri? untuk musik bisa upload sendiri dan hanya digunakan client sendiri. untuk tema hanya yang disediakan super admin

---

*Dokumen ini dibuat berdasarkan scope dari dua demo HTML yang sudah ada (dashboard admin/client & halaman undangan tamu "Kirana & Danendra"). Silakan edit langsung bagian mana pun yang kurang sesuai, terutama bagian 9, sebelum saya lanjutkan ke Fase 0.*
