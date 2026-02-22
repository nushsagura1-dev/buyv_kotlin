-- Migration: Add thumbnail_url column to posts table
-- Date: 2026-02-03
-- Purpose: Support thumbnail URLs for video posts

ALTER TABLE posts ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(512);

-- Optional: Add comment to column
COMMENT ON COLUMN posts.thumbnail_url IS 'URL of the video thumbnail image';
