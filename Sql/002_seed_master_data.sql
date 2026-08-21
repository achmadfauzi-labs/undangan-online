-- =====================================================================
-- 002_seed_master_data.sql
-- Data awal WAJIB setelah schema dibuat.
-- Dijalankan SETELAH 001_init_schema.sql.
--
-- Berisi:
-- - 3 role default (ADMIN / STAFF / USER)
-- - permission menu per role
-- - parameter sistem (EVENT_TYPE, GUEST_CATEGORY, SETTING)
-- - 6 tema default (cocok dengan demo HTML)
-- - 4 musik default library
-- - 1 super admin default (username: superadmin, password: admin123)
--
-- CATATAN:
-- - Semua INSERT pakai id BIGINT auto-generated, jadi id bisa berbeda
--   tiap run. Untuk re-run aman, blok transaksi BEGIN/COMMIT akan
--   gagal jika ada duplikat UNIQUE.
-- - Password "admin123" di-hash pakai pgcrypto crypt() dengan salt
--   bcrypt cost 10 — kompatibel dengan Spring Security BCryptPasswordEncoder.
--   GANTI password setelah login pertama.
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- Role default
-- ---------------------------------------------------------------------
INSERT INTO role (code, name, description, is_system) VALUES
  ('ADMIN', 'Administrator',        'Akses penuh ke seluruh sistem',                 TRUE),
  ('STAFF', 'Staff / Operator',     'Mengelola client dan tamu lintas client',       TRUE),
  ('USER',  'Client (Pengantin)',   'Akses ke dashboard & undangan miliknya sendiri', TRUE);

-- ---------------------------------------------------------------------
-- Permission menu per role
-- menu_key HARUS konsisten dengan route frontend (Web 1 & Web 2).
-- ---------------------------------------------------------------------
INSERT INTO role_permission (role_code, menu_key) VALUES
  -- ADMIN: semua menu
  ('ADMIN', 'dashboard'),
  ('ADMIN', 'clients'),
  ('ADMIN', 'users'),
  ('ADMIN', 'guests'),
  ('ADMIN', 'roles'),
  ('ADMIN', 'params'),
  ('ADMIN', 'templates'),
  ('ADMIN', 'music'),
  -- STAFF: operasional saja
  ('STAFF', 'dashboard'),
  ('STAFF', 'clients'),
  ('STAFF', 'guests'),
  -- USER (Client): fitur undangan saja
  ('USER',  'dashboard'),
  ('USER',  'event'),
  ('USER',  'gallery'),
  ('USER',  'customize'),
  ('USER',  'guestbook'),
  ('USER',  'clientguests');

-- ---------------------------------------------------------------------
-- System parameters
-- ---------------------------------------------------------------------
-- EVENT_TYPE (jenis acara)
INSERT INTO system_parameter (group_code, code, name, sort_order) VALUES
  ('EVENT_TYPE', 'PERNIKAHAN', 'Pernikahan',          1),
  ('EVENT_TYPE', 'LAMARAN',    'Lamaran / Tunangan',  2),
  ('EVENT_TYPE', 'SUNATAN',    'Sunatan / Khitanan',  3),
  ('EVENT_TYPE', 'ULTAH',      'Ulang Tahun',         4),
  ('EVENT_TYPE', 'AQIQAH',     'Aqiqah',              5);

-- GUEST_CATEGORY (kategori tamu)
INSERT INTO system_parameter (group_code, code, name, sort_order) VALUES
  ('GUEST_CATEGORY', 'KELUARGA', 'Keluarga',     1),
  ('GUEST_CATEGORY', 'TEMAN',    'Teman',        2),
  ('GUEST_CATEGORY', 'KANTOR',   'Rekan Kantor', 3),
  ('GUEST_CATEGORY', 'VIP',      'VIP',          4);

-- SETTING (pengaturan sistem)
-- DEFAULT_CLIENT_DURATION_MONTHS: dipakai saat super admin buat client baru
INSERT INTO system_parameter (group_code, code, name, sort_order) VALUES
  ('SETTING', 'DEFAULT_CLIENT_DURATION_MONTHS', 'Durasi default client baru (bulan)', 1),
  ('SETTING', 'MAX_MUSIC_SIZE_MB',              'Batas ukuran file musik (MB)',        2),
  ('SETTING', 'MAX_IMAGE_SIZE_MB',              'Batas ukuran file gambar (MB)',       3),
  ('SETTING', 'ALLOWED_IMG_EXT',                'Ekstensi gambar yang diizinkan',      4),
  ('SETTING', 'ALLOWED_AUDIO_EXT',              'Ekstensi audio yang diizinkan',       5);

-- ---------------------------------------------------------------------
-- Templates (library tema) — 6 tema default sesuai demo HTML
-- folder_path relatif terhadap direktori TemaUndangan/ (volume mount).
-- ---------------------------------------------------------------------
INSERT INTO template (code, name, category, folder_path, thumbnail_css) VALUES
  ('TPL-ELEGAN', 'Elegan Gold',      'luxury',     'tema-undangan/elegan/', 'linear-gradient(135deg,#fef3c7,#fcd34d)'),
  ('TPL-MODERN', 'Modern Teal',      'minimalist', 'tema-undangan/modern/', 'linear-gradient(135deg,#ccfbf1,#5eead4)'),
  ('TPL-BATIK',  'Batik Nusantara',  'lainnya',    'tema-undangan/batik/',  'linear-gradient(135deg,#fed7aa,#fb923c)'),
  ('TPL-SAKURA', 'Sakura Romance',   'botanical',  'tema-undangan/sakura/', 'linear-gradient(135deg,#fce7f3,#f472b6)'),
  ('TPL-NATURE', 'Nature Leaf',      'botanical',  'tema-undangan/nature/', 'linear-gradient(135deg,#d1fae5,#34d399)'),
  ('TPL-WHITE',  'White Minimalist', 'minimalist', 'tema-undangan/white/',  'linear-gradient(135deg,#f3f4f6,#d1d5db)');

-- ---------------------------------------------------------------------
-- Music library — 4 musik default
-- file_path relatif terhadap direktori Musics/ (volume mount).
-- ---------------------------------------------------------------------
INSERT INTO music (title, artist, file_path) VALUES
  ('Perfect (Instrumental)', 'Piano Cover',     'musics/perfect-instrumental.mp3'),
  ('A Thousand Years',      'Strings Version', 'musics/a-thousand-years.mp3'),
  ('Canon in D',            'Pachelbel',       'musics/canon-in-d.mp3'),
  ('River Flows in You',    'Yiruma',          'musics/river-flows-in-you.mp3');

-- ---------------------------------------------------------------------
-- Default Super Admin
-- Username: superadmin
-- Password: admin123 (BCrypt cost 10 — kompatibel Spring Security)
-- ⚠️  GANTI password setelah login pertama.
-- ---------------------------------------------------------------------
INSERT INTO users (username, email, password_hash, name, phone,
                   role_code, status)
VALUES (
    'superadmin',
    'admin@undanganonline.id',
    crypt('admin123', gen_salt('bf', 10)),
    'Super Admin',
    '0811-0000-0001',
    'ADMIN',
    'active'
);

COMMIT;

-- =====================================================================
-- AKHIR 002_seed_master_data.sql
-- =====================================================================
