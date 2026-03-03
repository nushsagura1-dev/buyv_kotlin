-- Migration: Create promotional_banners table
-- Date: 2026-03-03
-- Purpose: Promotional banners displayed in the BuyV mobile app

CREATE TABLE IF NOT EXISTS promotional_banners (
    id            SERIAL PRIMARY KEY,
    uid           UUID          NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    title         VARCHAR(200)  NOT NULL,
    subtitle      VARCHAR(500),
    image_url     VARCHAR(500)  NOT NULL,
    link_url      VARCHAR(500),
    link_type     VARCHAR(50)   NOT NULL DEFAULT 'product',
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE,
    display_order INTEGER       NOT NULL DEFAULT 0,
    start_date    TIMESTAMPTZ,
    end_date      TIMESTAMPTZ,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- Index for common queries
CREATE INDEX IF NOT EXISTS idx_promotional_banners_active
    ON promotional_banners (is_active, display_order);

COMMENT ON TABLE promotional_banners IS 'Promotional banners displayed in the BuyV mobile app home screen.';
COMMENT ON COLUMN promotional_banners.link_type IS 'One of: product, category, external';
