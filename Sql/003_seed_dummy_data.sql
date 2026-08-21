-- =====================================================================
-- 003_seed_dummy_data.sql
-- Data CONTOH untuk testing (opsional).
-- Dijalankan SETELAH 002_seed_master_data.sql.
--
-- Berisi 1 client + 1 user client + 1 invitation lengkap dengan:
-- - 2 sesi acara (akad + resepsi)
-- - 2 mempelai (groom + bride)
-- - 3 love story
-- - 3 foto galeri (path placeholder)
-- - 4 guest (mix status RSVP + 2 dengan pesan guestbook)
--
-- File-file di Images/ harus Anda buat manual sebagai placeholder
-- (atau hapus baris gallery jika tidak ingin membuat file dummy).
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- Client contoh: Kirana
-- ---------------------------------------------------------------------
INSERT INTO client (code, name, phone, address,
                    activated_at, duration_months, expires_at,
                    status, notes)
VALUES ('CLT-0001', 'Kirana Ayu R.', '0812xxxxxx01', 'Yogyakarta',
        '2026-08-01', 6, '2027-02-01',
        'active', 'Paket Gold');

-- ---------------------------------------------------------------------
-- User login untuk client di atas
-- Username: kirana / Password: kirana123
-- ---------------------------------------------------------------------
INSERT INTO users (username, email, password_hash, name, phone,
                   role_code, client_id, avatar_color, status)
VALUES ('kirana', 'kirana@mail.com',
        crypt('kirana123', gen_salt('bf', 10)),
        'Kirana Ayu Ramadhani', '0812xxxxxx01',
        'USER',
        (SELECT id FROM client WHERE code = 'CLT-0001'),
        '#8b5cf6',
        'active');

-- ---------------------------------------------------------------------
-- Invitation
-- ---------------------------------------------------------------------
INSERT INTO invitation (client_id, slug, event_type_code,
                        status, welcome_message,
                        template_id, primary_music_id,
                        published_at)
VALUES (
    (SELECT id FROM client WHERE code = 'CLT-0001'),
    'kirana-danendra-2026',
    'PERNIKAHAN',
    'publikasi',
    'Dengan memohon rahmat dan ridho Allah SWT, kami bermaksud menyelenggarakan pernikahan putra-putri kami.',
    (SELECT id FROM template WHERE code = 'TPL-ELEGAN'),
    (SELECT id FROM music  WHERE title = 'Perfect (Instrumental)'),
    NOW()
);

-- ---------------------------------------------------------------------
-- Sesi acara: Akad + Resepsi
-- ---------------------------------------------------------------------
INSERT INTO invitation_session (invitation_id, name, session_date,
                                session_time, location, maps_url,
                                sort_order)
VALUES
  ((SELECT id FROM invitation WHERE slug = 'kirana-danendra-2026'),
   'Akad Nikah', '2026-12-12', '08:00',
   'Kediaman Mempelai Wanita, Jl. Melati No. 12, Yogyakarta',
   'https://maps.google.com/?q=Jl.+Melati+No.+12+Yogyakarta',
   1),
  ((SELECT id FROM invitation WHERE slug = 'kirana-danendra-2026'),
   'Resepsi', '2026-12-12', '11:00 - 14:00',
   'Grand Pillar Ballroom, Jl. Kaliurang KM 8, Yogyakarta',
   'https://maps.google.com/?q=Grand+Pillar+Ballroom+Yogyakarta',
   2);

-- ---------------------------------------------------------------------
-- Mempelai
-- ---------------------------------------------------------------------
INSERT INTO invitation_person (invitation_id, role, name, nickname,
                               parent_names, sort_order)
VALUES
  ((SELECT id FROM invitation WHERE slug = 'kirana-danendra-2026'),
   'groom', 'Danendra Wicaksono', 'Danendra',
   'Bapak Hendra Wijaya & Ibu Siti Aminah', 1),
  ((SELECT id FROM invitation WHERE slug = 'kirana-danendra-2026'),
   'bride', 'Kirana Ayu Ramadhani', 'Kirana',
   'Bapak Slamet Riyadi & Ibu Wulandari', 2);

-- ---------------------------------------------------------------------
-- Love story
-- ---------------------------------------------------------------------
INSERT INTO love_story (invitation_id, title, story_date,
                        description, sort_order)
VALUES
  ((SELECT id FROM invitation WHERE slug = 'kirana-danendra-2026'),
   'Pertama Bertemu', '2019-05-01',
   'Dipertemukan dalam satu organisasi kampus.', 1),
  ((SELECT id FROM invitation WHERE slug = 'kirana-danendra-2026'),
   'Memulai Hubungan', '2021-03-14',
   'Memutuskan untuk menjalani hubungan lebih serius.', 2),
  ((SELECT id FROM invitation WHERE slug = 'kirana-danendra-2026'),
   'Lamaran', '2025-11-02',
   'Sebuah janji suci di hadapan kedua keluarga.', 3);

-- ---------------------------------------------------------------------
-- Galeri (path placeholder — buat file dummy jika ingin tampil)
-- ---------------------------------------------------------------------
INSERT INTO gallery (invitation_id, image_path, caption, sort_order)
VALUES
  ((SELECT id FROM invitation WHERE slug = 'kirana-danendra-2026'),
   'images/sample-1.jpg', 'Prewedding — Candi Borobudur', 1),
  ((SELECT id FROM invitation WHERE slug = 'kirana-danendra-2026'),
   'images/sample-2.jpg', 'Liburan — Bromo',              2),
  ((SELECT id FROM invitation WHERE slug = 'kirana-danendra-2026'),
   'images/sample-3.jpg', 'Lamaran — Yogyakarta',         3);

-- ---------------------------------------------------------------------
-- Guest: mix status RSVP + beberapa sudah isi guestbook
-- invitation_token auto-generated oleh DEFAULT gen_random_uuid().
-- ---------------------------------------------------------------------
INSERT INTO guest (invitation_id, name, category_code, email, phone,
                   attendance_status, party_size, message,
                   is_published, reply)
VALUES
  ((SELECT id FROM invitation WHERE slug = 'kirana-danendra-2026'),
   'Ayu Prasetya',  'TEMAN',    'ayu@mail.com',  '0812xxx',
   'hadir',       2,
   'Selamat menempuh hidup baru! Semoga sakinah mawaddah warahmah.',
   TRUE,  'Terima kasih banyak Ayu 🤍'),

  ((SELECT id FROM invitation WHERE slug = 'kirana-danendra-2026'),
   'Bagas Wirawan', 'KANTOR',   'bagas@mail.com','0813xxx',
   'hadir',       1,
   'Bahagia banget lihat kalian sampai di titik ini!',
   TRUE,  NULL),

  ((SELECT id FROM invitation WHERE slug = 'kirana-danendra-2026'),
   'Nadia Kusuma',  'KELUARGA', 'nadia@mail.com','0821xxx',
   'ragu',        1,
   'Semoga sakinah mawaddah warahmah, aku usahakan hadir ya.',
   TRUE,  NULL),

  ((SELECT id FROM invitation WHERE slug = 'kirana-danendra-2026'),
   'Hendra Saputra','VIP',      'hendra@mail.com','0822xxx',
   'tidak_hadir', 1,
   'Maaf tidak bisa hadir, doa terbaik untuk kalian berdua.',
   TRUE,  NULL);

COMMIT;

-- =====================================================================
-- AKHIR 003_seed_dummy_data.sql
-- =====================================================================
