-- ============================================================
-- V4__schema_fixes_and_indexes.sql
-- Batch 3: Schema & Migration pass
-- ============================================================

-- ─── I-7: Add NOT NULL to alert_threshold ──────────────────────────────────────
-- The JPA entity declares @Column(nullable = false) but the original migration
-- omitted NOT NULL (only a DEFAULT 80 was present).  Any row inserted via SQL
-- with an explicit NULL would cause Hibernate to fail when reading it back.
--
-- Safety step 1: Backfill any existing NULLs with the default (80) before adding
-- the constraint.  This is safe on all data sizes because the column is INT and
-- the backfill updates zero rows in any environment that was seeded correctly.
UPDATE budgets SET alert_threshold = 80 WHERE alert_threshold IS NULL;

-- Safety step 2: Now it is safe to add NOT NULL without risk of constraint failure.
ALTER TABLE budgets ALTER COLUMN alert_threshold SET NOT NULL;

-- ─── N-2: Missing indexes ────────────────────────────────────────────────────────
-- These indexes cover the most frequently executed queries identified in the
-- performance audit.  All are CREATE INDEX IF NOT EXISTS so the migration is
-- safe to re-run or apply against an environment that already has them.

-- FK index: transactions.category_id
--   Affected: existsByCategoryIdAndIsDeletedFalse, calculateSpentAmountForCategoryAndMonth,
--             TransactionSpecification category filter
CREATE INDEX IF NOT EXISTS idx_transactions_category_id
    ON transactions (category_id);

-- Enum index: transactions.type
--   Affected: sumAmountByTypeAndDateRange, getTopSpendingCategories, type Specification filter
CREATE INDEX IF NOT EXISTS idx_transactions_type
    ON transactions (type);

-- FK index: goal_contributions.goal_id
--   Affected: findByGoalId (contribution history load)
CREATE INDEX IF NOT EXISTS idx_goal_contributions_goal_id
    ON goal_contributions (goal_id);

-- Enum index: goals.status
--   Affected: countByUserIdAndStatusIn (limit enforcement on every goal create)
CREATE INDEX IF NOT EXISTS idx_goals_status
    ON goals (status);
