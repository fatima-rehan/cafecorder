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

-- Anything ordered besides the main drink, like a pastry on the side. One visit, many extras.
CREATE TABLE IF NOT EXISTS visit_extras (
    visit_id   BIGINT        NOT NULL REFERENCES cafe_visits (id) ON DELETE CASCADE,
    item_index INTEGER       NOT NULL,
    name       VARCHAR(120)  NOT NULL,
    price      NUMERIC(7, 2) NOT NULL CHECK (price >= 0),
    PRIMARY KEY (visit_id, item_index)
);
