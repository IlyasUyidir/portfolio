-- ============================================================
-- V1__init_schema.sql — Portefeuille Intelligent
-- ============================================================

-- Users
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20) DEFAULT 'STANDARD',
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Categories (must exist before transactions reference it)
CREATE TABLE categories (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name VARCHAR(100) NOT NULL,
  color VARCHAR(7),
  type VARCHAR(20) NOT NULL,
  is_system BOOLEAN DEFAULT FALSE,
  CONSTRAINT unique_user_category UNIQUE (user_id, name)
);

-- Transactions
CREATE TABLE transactions (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title VARCHAR(255) NOT NULL,
  amount BIGINT NOT NULL, -- in centimes
  type VARCHAR(20) NOT NULL, -- REVENU | DEPENSE
  category_id BIGINT NOT NULL REFERENCES categories(id),
  tx_date DATE NOT NULL,
  description TEXT,
  is_deleted BOOLEAN DEFAULT FALSE, -- ADDED: Required for Block 2 soft-delete
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT tx_amount_positive CHECK (amount > 0) -- CHANGED: Unique name
);

-- Budgets
CREATE TABLE budgets (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  category_id BIGINT NOT NULL REFERENCES categories(id),
  budget_year INT NOT NULL,
  budget_month INT NOT NULL, -- 1-12
  limit_amount BIGINT NOT NULL, -- in centimes
  alert_threshold INT DEFAULT 80, -- percentage
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT unique_budget UNIQUE (user_id, category_id, budget_year, budget_month),
  CONSTRAINT budget_limit_positive CHECK (limit_amount > 0) -- CHANGED: Unique name
);

-- Savings Goals
CREATE TABLE goals (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title VARCHAR(255) NOT NULL,
  target_amount BIGINT NOT NULL, -- in centimes
  current_amount BIGINT DEFAULT 0, -- in centimes
  target_date DATE NOT NULL,
  status VARCHAR(20) DEFAULT 'EN_COURS', -- EN_COURS | ATTEINT | EN_RETARD
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT target_positive CHECK (target_amount > 0)
  -- REMOVED: target_date_future constraint to allow historical data seeding
);

-- Goal Contributions
CREATE TABLE goal_contributions (
  id BIGSERIAL PRIMARY KEY,
  goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
  amount BIGINT NOT NULL, -- in centimes
  contribution_date DATE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT goal_contrib_amount_positive CHECK (amount > 0) -- CHANGED: Unique name
);

-- Add indexes for performance
CREATE INDEX idx_transactions_user ON transactions(user_id);
CREATE INDEX idx_transactions_date ON transactions(tx_date);
CREATE INDEX idx_transactions_deleted ON transactions(is_deleted); -- ADDED: For soft-delete performance
CREATE INDEX idx_budgets_user_month ON budgets(user_id, budget_year, budget_month);
CREATE INDEX idx_goals_user ON goals(user_id);
CREATE INDEX idx_categories_user ON categories(user_id); -- ADDED: For faster lookups
