-- Runs on every startup (spring.sql.init.mode=always). IF NOT EXISTS makes it safe to repeat.

-- One table for Cafe and StudyCafe; visit_type tells them apart.
CREATE TABLE IF NOT EXISTS cafe_visits (
    id            BIGSERIAL     PRIMARY KEY,
    visit_type    VARCHAR(10)   NOT NULL CHECK (visit_type IN ('CAFE', 'STUDY')),
    name          VARCHAR(120)  NOT NULL,
    neighbourhood VARCHAR(120)  NOT NULL,
    visit_date    DATE          NOT NULL,
    order_item    VARCHAR(200)  NOT NULL,
    price         NUMERIC(7, 2) NOT NULL CHECK (price >= 0),
    rating        INTEGER       NOT NULL DEFAULT 0 CHECK (rating BETWEEN 0 AND 5),
    -- study-only columns, null for regular visits
    has_wifi      BOOLEAN,
    has_outlets   BOOLEAN,
    seating       VARCHAR(10)   CHECK (seating IN ('GOOD', 'LIMITED', 'NONE')),
    laptop_policy VARCHAR(100),
    noise_level   VARCHAR(10)   CHECK (noise_level IN ('QUIET', 'MODERATE', 'LOUD'))
);

-- Accounts (Phase 7)
CREATE TABLE IF NOT EXISTS app_users (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(254) NOT NULL UNIQUE,
    display_name  VARCHAR(60)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Each visit belongs to a user. Nullable so rows from before accounts still load.
ALTER TABLE cafe_visits ADD COLUMN IF NOT EXISTS user_id BIGINT REFERENCES app_users (id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_cafe_visits_user ON cafe_visits (user_id);

-- Photos and the public feed (Phase 8)
ALTER TABLE cafe_visits ADD COLUMN IF NOT EXISTS photo_url TEXT;
ALTER TABLE cafe_visits ADD COLUMN IF NOT EXISTS shared BOOLEAN NOT NULL DEFAULT TRUE;
CREATE INDEX IF NOT EXISTS idx_cafe_visits_feed ON cafe_visits (shared, visit_date DESC);
-- false posts the visit without your name on it
ALTER TABLE cafe_visits ADD COLUMN IF NOT EXISTS show_name BOOLEAN NOT NULL DEFAULT TRUE;

-- Where the cafe is. Coordinates come from geocoding the address once, when it's saved.
ALTER TABLE cafe_visits ADD COLUMN IF NOT EXISTS address VARCHAR(200);
ALTER TABLE cafe_visits ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE cafe_visits ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
ALTER TABLE cafe_visits ADD COLUMN IF NOT EXISTS city VARCHAR(120);

-- Anything ordered besides the main drink, like a pastry on the side. One visit, many extras.
CREATE TABLE IF NOT EXISTS visit_extras (
    visit_id   BIGINT        NOT NULL REFERENCES cafe_visits (id) ON DELETE CASCADE,
    item_index INTEGER       NOT NULL,
    name       VARCHAR(120)  NOT NULL,
    price      NUMERIC(7, 2) NOT NULL CHECK (price >= 0),
    PRIMARY KEY (visit_id, item_index)
);

-- Tags like MATCHA or YEMENI (see CafeKind), for filtering. One visit, any number of kinds.
CREATE TABLE IF NOT EXISTS visit_kinds (
    visit_id BIGINT      NOT NULL REFERENCES cafe_visits (id) ON DELETE CASCADE,
    kind     VARCHAR(20) NOT NULL,
    PRIMARY KEY (visit_id, kind)
);

-- Row level security with no policies: Supabase's public Data API (anon/authenticated roles) can't
-- read or write these tables. The backend connects as the table owner, which RLS doesn't apply to.
-- Any new table needs a line here too.
ALTER TABLE app_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE cafe_visits ENABLE ROW LEVEL SECURITY;
ALTER TABLE visit_extras ENABLE ROW LEVEL SECURITY;
ALTER TABLE visit_kinds ENABLE ROW LEVEL SECURITY;
