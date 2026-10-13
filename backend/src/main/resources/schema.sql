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
