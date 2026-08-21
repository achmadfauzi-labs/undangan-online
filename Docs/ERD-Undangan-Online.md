# ERD — Undangan Online Database Schema

Dokumen ini mencatat schema PostgreSQL 18 untuk project Undangan Online. Dibuat berdasarkan `PRD-Undangan-Online.md` dan `Docs/web-dashboard-html.html`.

**File SQL sumber:**
- `Sql/001_init_schema.sql` — DDL semua tabel
- `Sql/002_seed_master_data.sql` — Data master awal
- `Sql/003_seed_dummy_data.sql` — Data contoh testing

---

## Asumsi Desain (dari PRD bagian 9)

| # | Pertanyaan | Jawaban |
|---|---|---|
| Q1 | Masa aktif default & perpanjangan | Default **1 bulan (30 hari)** via `SETTING DEFAULT_CLIENT_DURATION_MONTHS`. Super Admin bisa override `expires_at` per-client. |
| Q2 | Moderasi guestbook | **Langsung publish** tanpa moderasi. `guest.is_published = TRUE` default, client bisa hide/manual. |
| Q3 | 1 client > 1 invitation? | **Strictly 1 client = 1 active invitation**. Partial unique index mengunci ini. |
| Q4 | RSVP bisa update? | **Ya, berkali-kali.** `guest.invitation_token` (UUID) unik → UPSERT. Reply client tidak hilang saat update. |
| Q5 | Musik & tema upload | **Musik**: library Super Admin + upload custom sendiri per invitation. **Tema**: hanya dari library. |

---

## Daftar Tabel

### 1. `client`
Pelanggan (pasangan pengantin / pemesan).

| Kolom | Tipe | Constraint | Keterangan |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `code` | VARCHAR(20) | UNIQUE, NOT NULL | CLT-0001 |
| `name` | VARCHAR(150) | NOT NULL | Nama kontak utama |
| `phone` | VARCHAR(30) | | |
| `company` | VARCHAR(150) | | |
| `address` | TEXT | | |
| `activated_at` | DATE | NOT NULL, DEFAULT CURRENT_DATE | Tanggal aktivasi |
| `duration_months` | SMALLINT | NOT NULL, CHECK > 0 | Durasi langganan |
| `expires_at` | DATE | NOT NULL | activated_at + duration_months |
| `status` | VARCHAR(20) | CHECK IN ('active','inactive') | |
| `notes` | TEXT | | Catatan internal |
| `created_at` | TIMESTAMPTZ | NOT NULL | |
| `updated_at` | TIMESTAMPTZ | NOT NULL | Auto via trigger |

**Index:** `status`, `expires_at`

---

### 2. `role`
Definisi peran sistem.

| Kolom | Tipe | Constraint |
|---|---|---|
| `code` | VARCHAR(30) | PK, NOT NULL |
| `name` | VARCHAR(100) | NOT NULL |
| `description` | TEXT | |
| `is_system` | BOOLEAN | NOT NULL, DEFAULT FALSE |

**Catatan:** `is_system = TRUE` menandai role default (ADMIN/STAFF/USER) yang tidak boleh dihapus.

---

### 3. `role_permission`
Hak akses menu per role.

| Kolom | Tipe | Constraint |
|---|---|---|
| `role_code` | VARCHAR(30) | FK → role(code), CASCADE |
| `menu_key` | VARCHAR(50) | NOT NULL |
| | | PK (role_code, menu_key) |

**Menu key default:**

| Role | Menu Keys |
|---|---|
| ADMIN | dashboard, clients, users, guests, roles, params, templates, music |
| STAFF | dashboard, clients, guests |
| USER | dashboard, event, gallery, customize, guestbook, clientguests |

---

### 4. `users`
Akun login sistem.

| Kolom | Tipe | Constraint | Keterangan |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `username` | VARCHAR(50) | UNIQUE, NOT NULL | |
| `email` | VARCHAR(150) | UNIQUE | |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt |
| `name` | VARCHAR(150) | NOT NULL | |
| `phone` | VARCHAR(30) | | |
| `avatar_color` | VARCHAR(20) | | Hex color |
| `role_code` | VARCHAR(30) | FK → role(code), RESTRICT | |
| `client_id` | BIGINT | FK → client(id), RESTRICT | HANYA untuk role=USER |
| `status` | VARCHAR(20) | CHECK IN ('active','inactive') | |
| `created_at` | TIMESTAMPTZ | NOT NULL | |
| `updated_at` | TIMESTAMPTZ | NOT NULL | Auto via trigger |

**CHECK constraint:** `role_code = 'USER'` → `client_id IS NOT NULL`, lainnya → `client_id IS NULL`

**Index:** `client_id`, `role_code`

---

### 5. `system_parameter`
Master data fleksibel (jenis acara, kategori tamu, setting).

| Kolom | Tipe | Constraint |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `group_code` | VARCHAR(50) | NOT NULL |
| `code` | VARCHAR(50) | NOT NULL |
| `name` | VARCHAR(150) | NOT NULL |
| `sort_order` | SMALLINT | NOT NULL, DEFAULT 1 |
| `status` | VARCHAR(20) | CHECK IN ('active','inactive') |
| `created_at` | TIMESTAMPTZ | NOT NULL |
| `updated_at` | TIMESTAMPTZ | NOT NULL |

**UNIQUE:** `(group_code, code)`

**Index:** `(group_code, sort_order)`

**Group code default:**
- `EVENT_TYPE`: PERNIKAHAN, LAMARAN, SUNATAN, ULTAH, AQIQAH
- `GUEST_CATEGORY`: KELUARGA, TEMAN, KANTOR, VIP
- `SETTING`: DEFAULT_CLIENT_DURATION_MONTHS, MAX_MUSIC_SIZE_MB, MAX_IMAGE_SIZE_MB, ALLOWED_IMG_EXT, ALLOWED_AUDIO_EXT

---

### 6. `template`
Library tema visual.

| Kolom | Tipe | Constraint |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `code` | VARCHAR(30) | UNIQUE, NOT NULL |
| `name` | VARCHAR(100) | NOT NULL |
| `category` | VARCHAR(30) | CHECK IN ('luxury','minimalist','botanical','lainnya') |
| `folder_path` | VARCHAR(255) | NOT NULL |
| `thumbnail_css` | TEXT | Gradient CSS preview |
| `status` | VARCHAR(20) | CHECK IN ('active','inactive') |
| `created_at` | TIMESTAMPTZ | NOT NULL |
| `updated_at` | TIMESTAMPTZ | NOT NULL |

**Index:** `status`, `category`

---

### 7. `music`
Library musik master (dari Super Admin).

| Kolom | Tipe | Constraint |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `title` | VARCHAR(150) | NOT NULL |
| `artist` | VARCHAR(150) | |
| `file_path` | VARCHAR(255) | NOT NULL |
| `file_size` | INTEGER | CHECK > 0 |
| `status` | VARCHAR(20) | CHECK IN ('active','inactive') |
| `created_at` | TIMESTAMPTZ | NOT NULL |
| `updated_at` | TIMESTAMPTZ | NOT NULL |

**Index:** `status`

**Catatan:** Musik custom per invitation disimpan di `invitation.custom_music_path`, BUKAN di tabel ini.

---

### 8. `invitation`
Data utama undangan per client.

| Kolom | Tipe | Constraint | Keterangan |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `client_id` | BIGINT | FK → client(id), RESTRICT, NOT NULL | |
| `slug` | VARCHAR(100) | UNIQUE, NOT NULL | URL publik: /v/{slug} |
| `event_type_code` | VARCHAR(50) | NOT NULL | Ref system_parameter |
| `status` | VARCHAR(20) | CHECK IN ('draft','publikasi','selesai','archived') | |
| `welcome_message` | TEXT | | |
| `cover_image_path` | VARCHAR(255) | | |
| `template_id` | BIGINT | FK → template(id), SET NULL | |
| `primary_music_id` | BIGINT | FK → music(id), SET NULL | Dari library |
| `custom_music_path` | VARCHAR(255) | | Upload sendiri |
| `custom_music_title` | VARCHAR(150) | | |
| `published_at` | TIMESTAMPTZ | | |
| `created_at` | TIMESTAMPTZ | NOT NULL | |
| `updated_at` | TIMESTAMPTZ | NOT NULL | Auto via trigger |

**Constraint:** XOR musik — `NOT (primary_music_id IS NOT NULL AND custom_music_path IS NOT NULL)`

**Partial Unique Index:** `client_id WHERE status <> 'archived'` — 1 active invitation per client.

**Index:** `template_id`, `status`, `(status, published_at)` WHERE status='publikasi'

---

### 9. `invitation_session`
Multi-sesi acara (akad, resepsi, dll).

| Kolom | Tipe | Constraint |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `invitation_id` | BIGINT | FK → invitation(id), CASCADE, NOT NULL |
| `name` | VARCHAR(100) | NOT NULL |
| `session_date` | DATE | NOT NULL |
| `session_time` | VARCHAR(50) | |
| `location` | TEXT | |
| `maps_url` | VARCHAR(500) | |
| `sort_order` | SMALLINT | NOT NULL, DEFAULT 1 |
| `created_at` | TIMESTAMPTZ | NOT NULL |
| `updated_at` | TIMESTAMPTZ | NOT NULL |

**Index:** `(invitation_id, sort_order)`

---

### 10. `invitation_person`
Mempelai atau penerima acara.

| Kolom | Tipe | Constraint |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `invitation_id` | BIGINT | FK → invitation(id), CASCADE, NOT NULL |
| `role` | VARCHAR(20) | CHECK IN ('groom','bride','person') |
| `name` | VARCHAR(150) | NOT NULL |
| `nickname` | VARCHAR(50) | |
| `parent_names` | VARCHAR(255) | |
| `child_order` | SMALLINT | CHECK > 0 (hanya role='person') |
| `photo_path` | VARCHAR(255) | |
| `sort_order` | SMALLINT | NOT NULL, DEFAULT 1 |

**UNIQUE:** `(invitation_id, role)` — 1 groom, 1 bride, 1 person per invitation.

**Index:** `invitation_id`

---

### 11. `love_story`
Timeline cerita cinta.

| Kolom | Tipe | Constraint |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `invitation_id` | BIGINT | FK → invitation(id), CASCADE, NOT NULL |
| `title` | VARCHAR(150) | NOT NULL |
| `story_date` | DATE | |
| `description` | TEXT | NOT NULL |
| `sort_order` | SMALLINT | NOT NULL, DEFAULT 1 |
| `created_at` | TIMESTAMPTZ | NOT NULL |

**Index:** `(invitation_id, sort_order)`

---

### 12. `gallery`
Foto kenangan.

| Kolom | Tipe | Constraint |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `invitation_id` | BIGINT | FK → invitation(id), CASCADE, NOT NULL |
| `image_path` | VARCHAR(255) | NOT NULL |
| `caption` | VARCHAR(255) | |
| `sort_order` | SMALLINT | NOT NULL, DEFAULT 1 |
| `created_at` | TIMESTAMPTZ | NOT NULL |

**Index:** `(invitation_id, sort_order)`

---

### 13. `guest`
Gabungan: daftar tamu + RSVP + guestbook.

| Kolom | Tipe | Constraint | Keterangan |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `invitation_id` | BIGINT | FK → invitation(id), CASCADE, NOT NULL | |
| `invitation_token` | VARCHAR(64) | UNIQUE, NOT NULL | UUID, untuk RSVP publik |
| `name` | VARCHAR(150) | NOT NULL | |
| `category_code` | VARCHAR(50) | | Ref system_parameter |
| `email` | VARCHAR(150) | | |
| `phone` | VARCHAR(30) | | |
| `attendance_status` | VARCHAR(20) | CHECK IN ('menunggu','undangan_terkirim','hadir','tidak_hadir','ragu') | |
| `party_size` | SMALLINT | CHECK > 0 AND <= 50 | |
| `message` | TEXT | | Ucapan guestbook |
| `is_published` | BOOLEAN | NOT NULL, DEFAULT TRUE | Guestbook langsung publish |
| `reply` | TEXT | | Balasan dari client |
| `created_at` | TIMESTAMPTZ | NOT NULL | |
| `updated_at` | TIMESTAMPTZ | NOT NULL | Auto via trigger |

**Index:**
- `invitation_id`
- `(invitation_id, created_at DESC) WHERE is_published = TRUE` — untuk guestbook publik
- `(invitation_id, attendance_status)` — untuk filter RSVP dashboard

---

## View

### `v_invitation_rsvp_summary`
Agregat RSVP per invitation untuk stat cards di dashboard.

```sql
SELECT
    invitation_id,
    COUNT(*)                                          AS total_guests,
    COUNT(*) FILTER (WHERE attendance_status = 'hadir') AS total_hadir,
    COUNT(*) FILTER (WHERE attendance_status = 'tidak_hadir') AS total_tidak_hadir,
    COUNT(*) FILTER (WHERE attendance_status = 'ragu') AS total_ragu,
    COUNT(*) FILTER (WHERE attendance_status IN ('menunggu','undangan_terkirim')) AS total_belum_respon,
    COALESCE(SUM(party_size) FILTER (WHERE attendance_status = 'hadir'), 0) AS total_party_hadir
FROM guest
GROUP BY invitation_id;
```

---

## Diagram Relasi (ASCII)

```
                         ┌─────────────┐
                         │   client    │
                         │ id (PK)     │
                         │ code (UQ)   │
                         └──────┬──────┘
                                │ 1
           ┌────────────────────┼────────────────────┐
           │ N                  │ N                  │
           ▼                    ▼                    │
      ┌──────────┐        ┌──────────┐                │
      │  users   │        │invitation│                │
      │ id (PK)  │        │ id (PK)  │                │
      │username  │        │ slug (UQ)│                │
      │role_code │        │client_id │                │
      │client_id │        └────┬─────┘                │
      └──────────┘             │ 1                    │
           │                   ├──────► invitation_session
           │                   ├──────► invitation_person
           │                   ├──────► love_story
           │                   ├──────► gallery
           │                   └──────► guest
           │
           ▼ (FK)
      ┌──────────┐
      │   role   │◄───── role_permission
      │code (PK) │         (role_code, menu_key)
      └──────────┘

    Master data (tanpa FK ke client, validasi di app layer):
    ┌────────────────────┐    ┌──────────┐    ┌──────────┐
    │ system_parameter   │    │ template │    │  music   │
    │ (group_code, code)│    │ id (PK)  │    │ id (PK)  │
    │ UQ(group, code)   │    │ code (UQ)│    │file_path │
    └────────────────────┘    └──────────┘    └──────────┘
```

---

## Aturan Naming

| Entity | Naming | Contoh |
|---|---|---|
| Tabel | snake_case, plural | `client`, `role_permission` |
| Kolom | snake_case | `invitation_token`, `expires_at` |
| PK | `id` (BIGSERIAL) | |
| FK | `<tabel_singular>_id` | `client_id`, `invitation_id` |
| Enum value | snake_case, lowercase | `active`, `draft`, `publikasi` |
| Index | `idx_<tabel>_<kolom>` | `idx_client_status` |
| Unique index | `uq_<tabel>_<deskripsi>` | `uq_invitation_active_client` |
| Timestamp | `_at` suffix, TIMESTAMPTZ | `created_at`, `updated_at` |
| Tanggal murni | `_date` suffix, DATE | `session_date`, `expires_at` |

---

## Trigger

`updated_at` auto-set via trigger `trg_set_updated_at()` pada:
- `client`
- `users`
- `system_parameter`
- `template`
- `music`
- `invitation`
- `invitation_session`
- `invitation_person`
- `guest`

---

## Catatan Teknis

1. **Tidak ada FK untuk `event_type_code` dan `category_code`** — validasi dilakukan di service layer Spring Boot untuk menghindari duplikasi `group_code`.

2. **Partial unique index** `uq_invitation_active_client` memastikan 1 client = 1 active invitation. Untuk replace, set status lama ke `archived`.

3. **Guest = RSVP + Guestbook** — satu tabel dengan `attendance_status` sebagai lifecycle state. `invitation_token` UUID memungkinkan RSVP publik (tanpa login) dengan UPSERT semantics.

4. **Musik XOR** — `invitation` punya CHECK constraint yang menjamin hanya satu sumber musik aktif (library atau custom).

5. **File storage** — semua file (gambar, musik, tema) disimpan sebagai PATH string di kolom `*_path`. File biner di folder `Images/`, `Musics/`, `TemaUndangan/` (volume mount Docker).

---

*Terakhir diupdate: 2026-08-21*
