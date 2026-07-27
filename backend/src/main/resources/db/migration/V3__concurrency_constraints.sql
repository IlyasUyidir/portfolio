-- ============================================================
-- V3__concurrency_constraints.sql
-- Batch 2: Concurrency & Locking fixes
-- ============================================================

-- C-1: Add optimistic-lock version column to goals.
-- Hibernate's @Version mechanism requires this column.
-- Default 0 so all existing rows start at a known version.
ALTER TABLE goals ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- I-1: Partial unique index — enforce the "one active goal per STANDARD user" invariant
-- at the DB level.  Only rows with status = 'EN_COURS' or 'EN_RETARD' participate.
-- Achieved goals (ATTEINT) are excluded so users can create new goals after finishing one.
--
-- PostgreSQL supports partial indexes natively.
-- NOTE: If running against H2 in tests, H2 supports partial indexes since 2.x
-- (used by Spring Boot 3.x), so this migration is compatible with the test suite.
CREATE UNIQUE INDEX IF NOT EXISTS idx_goals_one_active_per_user
    ON goals (user_id)
    WHERE status IN ('EN_COURS', 'EN_RETARD');
