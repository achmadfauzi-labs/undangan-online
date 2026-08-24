-- =====================================================================
-- 001_init_schema.sql
-- Undangan Online — PostgreSQL 18 schema (manual DDL)
-- Sesuai PRD-Undangan-Online.md v0.1
--
-- CATATAN UMUM
-- - Naming: snake_case, Bahasa Inggris.
-- - PK: BIGINT GENERATED ALWAYS AS IDENTITY (Postgres 10+).
-- - Timestamps: TIMESTAMPTZ. Tanggal murni (acara, expiry): DATE.
-- - Enum-like: VARCHAR + CHECK (lebih fleksibel daripada CREATE TYPE ENUM
--   bila perlu menambah nilai tanpa ALTER TYPE transaction).
-- - File di storage (gambar/musik/tema) disimpan sebagai PATH string,
--   bukan blob (lihat PRD bagian 7 & CLAUDE.md aturan #5).
-- - Semua tabel ber-prefix id BIGINT auto-increment.
-- - updated_at di-maintain via trigger (lihat fungsi di bawah).
-- =====================================================================

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;  -- gen_random_uuid()

-- ---------------------------------------------------------------------
-- Helper: trigger function untuk set updated_at = NOW() pada UPDATE
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION trg_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- 1. CLIENT — data pelanggan (pasangan pengantin / pemesan)
-- =====================================================================
CREATE TABLE client (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code            VARCHAR(20)  NOT NULL UNIQUE,             -- CLT-0001, auto-generated
    name            VARCHAR(150) NOT NULL,                   -- nama kontak utama
    phone           VARCHAR(30),
    company         VARCHAR(150),
    address         TEXT,
    activated_at    DATE         NOT NULL DEFAULT CURRENT_DATE,
    duration_months SMALLINT     NOT NULL DEFAULT 1
                    CHECK (duration_months > 0 AND duration_months <= 120),
    expires_at      DATE         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'active'
                    CHECK (status IN ('active','inactive')),
    notes           TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_client_expires_after_activated
        CHECK (expires_at >= activated_at)
);

CREATE INDEX idx_client_status     ON client(status);
CREATE INDEX idx_client_expires_at ON client(expires_at);

CREATE TRIGGER trg_client_updated_at
    BEFORE UPDATE ON client
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

COMMENT ON TABLE client IS
    'Pelanggan (pengantin/pemesan). Masa aktif dihitung dari activated_at + duration_months.';

-- =====================================================================
-- 2. ROLE — definisi peran sistem
-- =====================================================================
CREATE TABLE role (
    code        VARCHAR(30)  PRIMARY KEY,                -- ADMIN / STAFF / USER
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    is_system   BOOLEAN      NOT NULL DEFAULT FALSE,    -- TRUE = tidak boleh dihapus
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE role IS
    'Definisi role. is_system=TRUE untuk role default (ADMIN/STAFF/USER) yang tidak boleh dihapus.';

-- =====================================================================
-- 3. ROLE_PERMISSION — hak akses menu per role
-- =====================================================================
CREATE TABLE role_permission (
    role_code VARCHAR(30) NOT NULL
              REFERENCES role(code) ON DELETE CASCADE,
    menu_key  VARCHAR(50) NOT NULL,        -- 'dashboard','clients','event', dll
    PRIMARY KEY (role_code, menu_key)
);

COMMENT ON TABLE role_permission IS
    'Menu yang boleh diakses tiap role. menu_key harus konsisten dengan frontend route.';

-- =====================================================================
-- 4. USERS — akun login sistem
-- =====================================================================
CREATE TABLE users (
    id            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(150) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,                  -- BCrypt (cost 10)
    name          VARCHAR(150) NOT NULL,
    phone         VARCHAR(30),
    avatar_color  VARCHAR(20),                            -- hex color untuk avatar
    role_code     VARCHAR(30)  NOT NULL
                  REFERENCES role(code) ON DELETE RESTRICT,
    client_id     BIGINT
                  REFERENCES client(id) ON DELETE RESTRICT,
    status        VARCHAR(20)  NOT NULL DEFAULT 'active'
                  CHECK (status IN ('active','inactive')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    -- USER role WAJIB terkait satu client; ADMIN/STAFF WAJIB tanpa client.
    CONSTRAINT chk_user_client_link_by_role CHECK (
        (role_code = 'USER'  AND client_id IS NOT NULL) OR
        (role_code <> 'USER' AND client_id IS NULL)
    )
);

CREATE INDEX idx_users_client ON users(client_id);
CREATE INDEX idx_users_role   ON users(role_code);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

COMMENT ON TABLE users IS
    'Akun login. USER role = client pengantin (1 user ↔ 1 client). ADMIN/STAFF = tim internal.';

-- =====================================================================
-- 5. SYSTEM_PARAMETER — master data fleksibel (group + code + name)
-- =====================================================================
CREATE TABLE system_parameter (
    id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    group_code VARCHAR(50)  NOT NULL,        -- 'EVENT_TYPE', 'GUEST_CATEGORY', 'SETTING'
    code       VARCHAR(50)  NOT NULL,        -- 'PERNIKAHAN', 'TEMAN', dll
    name       VARCHAR(150) NOT NULL,        -- label tampil
    sort_order SMALLINT     NOT NULL DEFAULT 1,
    status     VARCHAR(20)  NOT NULL DEFAULT 'active'
               CHECK (status IN ('active','inactive')),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (group_code, code)
);

CREATE INDEX idx_param_group_order
    ON system_parameter(group_code, sort_order);

CREATE TRIGGER trg_param_updated_at
    BEFORE UPDATE ON system_parameter
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

COMMENT ON TABLE system_parameter IS
    'Master data fleksibel: jenis acara, kategori tamu, setting sistem. UNIQUE(group,code).';

-- =====================================================================
-- 6. TEMPLATE — library tema visual
-- =====================================================================
CREATE TABLE template (
    id            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code          VARCHAR(30)  NOT NULL UNIQUE,            -- 'TPL-ELEGAN'
    name          VARCHAR(100) NOT NULL,
    category      VARCHAR(30)  NOT NULL
                  CHECK (category IN ('luxury','minimalist','botanical','lainnya')),
    folder_path   VARCHAR(255) ,                  -- path relatif di TemaUndangan/
    thumbnail_css TEXT,                                   -- gradient CSS untuk preview
    status        VARCHAR(20)  NOT NULL DEFAULT 'active'
                  CHECK (status IN ('active','inactive')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_template_status   ON template(status);
CREATE INDEX idx_template_category ON template(category);

CREATE TRIGGER trg_template_updated_at
    BEFORE UPDATE ON template
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

COMMENT ON TABLE template IS
    'Library tema undangan. Aset file ada di folder folder_path; DB hanya menyimpan path.';

-- =====================================================================
-- 7. MUSIC — library musik master
-- =====================================================================
CREATE TABLE music (
    id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title      VARCHAR(150) NOT NULL,
    artist     VARCHAR(150),
    file_path  VARCHAR(255) NOT NULL,                     -- path relatif di Musics/
    file_size  INTEGER      CHECK (file_size > 0),        -- bytes
    status     VARCHAR(20)  NOT NULL DEFAULT 'active'
               CHECK (status IN ('active','inactive')),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_music_status ON music(status);

CREATE TRIGGER trg_music_updated_at
    BEFORE UPDATE ON music
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

COMMENT ON TABLE music IS
    'Library musik yang disediakan Super Admin. Musik custom per-undangan TIDAK di sini — lihat invitation.custom_music_path.';

-- =====================================================================
-- 8. INVITATION — data utama undangan per client
-- =====================================================================
CREATE TABLE invitation (
    id                  BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    client_id           BIGINT       NOT NULL
                        REFERENCES client(id) ON DELETE RESTRICT,
    slug                VARCHAR(100) NOT NULL UNIQUE,     -- URL publik: /v/{slug}
    event_type_code     VARCHAR(50)  NOT NULL,             -- ref system_parameter
    status              VARCHAR(20)  NOT NULL DEFAULT 'draft'
                        CHECK (status IN ('draft','publikasi','selesai','archived')),
    welcome_message     TEXT,
    cover_image_path    VARCHAR(255),
    template_id         BIGINT
                        REFERENCES template(id) ON DELETE SET NULL,
    -- Musik: pilih dari library ATAU upload sendiri (XOR)
    primary_music_id    BIGINT
                        REFERENCES music(id) ON DELETE SET NULL,
    custom_music_path   VARCHAR(255),
    custom_music_title  VARCHAR(150),
    published_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    -- Constraint: tidak boleh dua sumber musik aktif bersamaan
    CONSTRAINT chk_invitation_music_xor CHECK (
        NOT (primary_music_id IS NOT NULL AND custom_music_path IS NOT NULL)
    )
);

-- Asumsi Q3: strictly 1 client = 1 active invitation.
-- Hanya 1 invitation non-archived per client. Untuk re-create, set status='archived' dulu.
CREATE UNIQUE INDEX uq_invitation_active_client
    ON invitation(client_id)
    WHERE status <> 'archived';

-- Index untuk query Web 3 (publik) — cari berdasarkan slug
CREATE INDEX idx_invitation_template ON invitation(template_id);
CREATE INDEX idx_invitation_status   ON invitation(status);
CREATE INDEX idx_invitation_published
    ON invitation(status, published_at)
    WHERE status = 'publikasi';

CREATE TRIGGER trg_invitation_updated_at
    BEFORE UPDATE ON invitation
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

COMMENT ON TABLE invitation IS
    'Undangan milik satu client. Slug = URL publik Web 3. Musik XOR (library atau custom).';

-- =====================================================================
-- 9. INVITATION_SESSION — multi-sesi acara (akad, resepsi, dst)
-- =====================================================================
CREATE TABLE invitation_session (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invitation_id   BIGINT       NOT NULL
                    REFERENCES invitation(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,                 -- 'Akad Nikah'
    session_date    DATE         NOT NULL,
    session_time    VARCHAR(50),                           -- '08:00' atau '11:00 - 14:00'
    location        TEXT,
    maps_url        VARCHAR(500),
    sort_order      SMALLINT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_session_invitation_order
    ON invitation_session(invitation_id, sort_order);

CREATE TRIGGER trg_session_updated_at
    BEFORE UPDATE ON invitation_session
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

COMMENT ON TABLE invitation_session IS
    'Sesi acara (akad/resepsi/dst). Minimal 1 sesi per invitation. Sesi pertama = acara utama.';

-- =====================================================================
-- 10. INVITATION_PERSON — mempelai atau penerima acara
-- =====================================================================
CREATE TABLE invitation_person (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invitation_id   BIGINT       NOT NULL
                    REFERENCES invitation(id) ON DELETE CASCADE,
    role            VARCHAR(20)  NOT NULL
                    CHECK (role IN ('groom','bride','person')),
    name            VARCHAR(150) NOT NULL,
    nickname        VARCHAR(50),
    parent_names    VARCHAR(255),
    child_order     SMALLINT     CHECK (child_order > 0), -- hanya untuk role=person
    photo_path      VARCHAR(255),
    sort_order      SMALLINT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Maks 1 groom + 1 bride + 1 person per invitation
CREATE UNIQUE INDEX uq_person_role_per_invitation
    ON invitation_person(invitation_id, role);

CREATE INDEX idx_person_invitation ON invitation_person(invitation_id);

CREATE TRIGGER trg_person_updated_at
    BEFORE UPDATE ON invitation_person
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

COMMENT ON TABLE invitation_person IS
    'Mempelai (groom/bride) atau penerima acara (person untuk non-pernikahan/lamaran).';

-- =====================================================================
-- 11. LOVE_STORY — timeline cerita cinta
-- =====================================================================
CREATE TABLE love_story (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invitation_id   BIGINT       NOT NULL
                    REFERENCES invitation(id) ON DELETE CASCADE,
    title           VARCHAR(150) NOT NULL,
    story_date      DATE,
    description     TEXT         NOT NULL,
    sort_order      SMALLINT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_story_invitation_order
    ON love_story(invitation_id, sort_order);

COMMENT ON TABLE love_story IS
    'Timeline love story per invitation. Ditampilkan di Web 3.';

-- =====================================================================
-- 12. GALLERY — foto kenangan
-- =====================================================================
CREATE TABLE gallery (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invitation_id   BIGINT       NOT NULL
                    REFERENCES invitation(id) ON DELETE CASCADE,
    image_path      VARCHAR(255) NOT NULL,
    caption         VARCHAR(255),
    sort_order      SMALLINT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_gallery_invitation_order
    ON gallery(invitation_id, sort_order);

COMMENT ON TABLE gallery IS
    'Galeri foto per invitation. File disimpan di Images/, DB hanya simpan path.';

-- =====================================================================
-- 13. GUEST — gabungan: daftar tamu + RSVP + guestbook
-- ---------------------------------------------------------------------
-- Pertanyaan Q4: RSVP bisa update berkali-kali → UPSERT via invitation_token.
-- - Saat client bikin daftar tamu di Web 2 → INSERT (status default 'menunggu')
-- - Saat tamu isi RSVP di Web 3 → UPDATE row berdasarkan invitation_token
--   (jika token tidak ada, INSERT baru — RSVP spontan dari publik)
-- - Reply dari client disimpan terpisah (kolom reply) sehingga tidak hilang
--   saat guest update message/attendance mereka.
-- Pertanyaan Q2: guestbook langsung publish → is_published DEFAULT TRUE.
-- =====================================================================
CREATE TABLE guest (
    id                  BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invitation_id       BIGINT       NOT NULL
                        REFERENCES invitation(id) ON DELETE CASCADE,
    invitation_token    VARCHAR(64)  NOT NULL UNIQUE
                        DEFAULT gen_random_uuid()::text,
    name                VARCHAR(150) NOT NULL,
    category_code       VARCHAR(50),                       -- ref system_parameter
    email               VARCHAR(150),
    phone               VARCHAR(30),
    attendance_status   VARCHAR(20)  NOT NULL DEFAULT 'menunggu'
                        CHECK (attendance_status IN
                              ('menunggu','undangan_terkirim','hadir','tidak_hadir','ragu')),
    party_size          SMALLINT     NOT NULL DEFAULT 1
                        CHECK (party_size > 0 AND party_size <= 50),
    message             TEXT,                              -- ucapan guestbook
    is_published        BOOLEAN      NOT NULL DEFAULT TRUE,
    reply               TEXT,                              -- balasan dari client
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Index untuk query Web 2 (client dashboard) dan Web 3 (publik)
CREATE INDEX idx_guest_invitation
    ON guest(invitation_id);

-- Untuk Buku Tamu (Web 3 publik): hanya published, urut terbaru
CREATE INDEX idx_guest_invitation_published
    ON guest(invitation_id, created_at DESC)
    WHERE is_published = TRUE;

-- Untuk filter RSVP di dashboard
CREATE INDEX idx_guest_invitation_attendance
    ON guest(invitation_id, attendance_status);

CREATE TRIGGER trg_guest_updated_at
    BEFORE UPDATE ON guest
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

COMMENT ON TABLE guest IS
    'Tamu + RSVP + guestbook dalam satu tabel. invitation_token = UUID untuk RSVP publik (UPSERT).';

-- =====================================================================
-- 14. AUDIT LOG — catatan aktivitas sistem (CRUD, login, upload file, dll)
-- =====================================================================
CREATE TABLE audit_log (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT REFERENCES users(id) ON DELETE SET NULL,
    username    VARCHAR(50),
    role        VARCHAR(30),
    action      VARCHAR(30) NOT NULL
                CHECK (action IN ('CREATE','UPDATE','DELETE','LOGIN','LOGIN_FAILED','READ','UPLOAD_FILE','DELETE_FILE')),
    module      VARCHAR(50),
    entity_id   BIGINT,
    old_value   TEXT,
    new_value   TEXT,
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(512),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_action ON audit_log(action);
CREATE INDEX idx_audit_log_module ON audit_log(module);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at DESC);

-- =====================================================================
-- 15. REFRESH TOKEN — untuk JWT refresh token (opsional, bila pakai refresh token)
-- =====================================================================
CREATE TABLE refresh_token (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    issued_at   TIMESTAMPTZ NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    user_agent  VARCHAR(255),
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_token_user_id ON refresh_token(user_id);
CREATE INDEX idx_refresh_token_expires_at ON refresh_token(expires_at);

-- =====================================================================
-- VIEW opsional: ringkasan RSVP per invitation (untuk dashboard)
-- =====================================================================
CREATE OR REPLACE VIEW v_invitation_rsvp_summary AS
SELECT
    invitation_id,
    COUNT(*)                                                        AS total_guests,
    COUNT(*) FILTER (WHERE attendance_status = 'hadir')             AS total_hadir,
    COUNT(*) FILTER (WHERE attendance_status = 'tidak_hadir')       AS total_tidak_hadir,
    COUNT(*) FILTER (WHERE attendance_status = 'ragu')              AS total_ragu,
    COUNT(*) FILTER (WHERE attendance_status IN ('menunggu',
                                                  'undangan_terkirim')) AS total_belum_respon,
    COALESCE(SUM(party_size) FILTER (WHERE attendance_status = 'hadir'),
             0)                                                     AS total_party_hadir
FROM guest
GROUP BY invitation_id;

COMMENT ON VIEW v_invitation_rsvp_summary IS
    'Agregat RSVP per invitation. Dipakai dashboard client (stat cards).';

COMMIT;

-- =====================================================================
-- AKHIR 001_init_schema.sql
-- =====================================================================
