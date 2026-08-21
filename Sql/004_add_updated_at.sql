-- =====================================================================
-- 004_add_updated_at.sql
-- Tambah kolom updated_at pada tabel role, gallery, dan love_story
-- agar konsisten dengan tabel lain (client, users, template, music,
-- invitation, dsb) yang sudah memilikinya + trigger pemeliharaan.
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- ROLE
-- ---------------------------------------------------------------------
ALTER TABLE role
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

DROP TRIGGER IF EXISTS trg_role_updated_at ON role;
CREATE TRIGGER trg_role_updated_at
    BEFORE UPDATE ON role
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

-- ---------------------------------------------------------------------
-- GALLERY
-- ---------------------------------------------------------------------
ALTER TABLE gallery
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

DROP TRIGGER IF EXISTS trg_gallery_updated_at ON gallery;
CREATE TRIGGER trg_gallery_updated_at
    BEFORE UPDATE ON gallery
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

-- ---------------------------------------------------------------------
-- LOVE_STORY
-- ---------------------------------------------------------------------
ALTER TABLE love_story
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

DROP TRIGGER IF EXISTS trg_love_story_updated_at ON love_story;
CREATE TRIGGER trg_love_story_updated_at
    BEFORE UPDATE ON love_story
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

COMMIT;

-- =====================================================================
-- AKHIR 004_add_updated_at.sql
-- =====================================================================
