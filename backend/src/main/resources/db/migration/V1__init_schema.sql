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
  amount BIGINT NOT NULL,
  type VARCHAR(20) NOT NULL,
  category_id BIGINT NOT NULL REFERENCES categories(id),
  tx_date DATE NOT NULL,
  description TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT amount_positive CHECK (amount > 0)
);

-- Budgets
CREATE TABLE budgets (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  category_id BIGINT NOT NULL REFERENCES categories(id),
  budget_year INT NOT NULL,
  budget_month INT NOT NULL,
  limit_amount BIGINT NOT NULL,
  alert_threshold INT DEFAULT 80,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT unique_budget UNIQUE (user_id, category_id, budget_year, budget_month),
  CONSTRAINT limit_positive CHECK (limit_amount > 0)
);

-- Savings Goals
CREATE TABLE goals (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title VARCHAR(255) NOT NULL,
  target_amount BIGINT NOT NULL,
  current_amount BIGINT DEFAULT 0,
  target_date DATE NOT NULL,
  status VARCHAR(20) DEFAULT 'EN_COURS',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT target_positive CHECK (target_amount > 0),
  CONSTRAINT target_date_future CHECK (target_date > CURRENT_DATE)
);

-- Goal Contributions
CREATE TABLE goal_contributions (
  id BIGSERIAL PRIMARY KEY,
  goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
  amount BIGINT NOT NULL,
  contribution_date DATE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT amount_positive CHECK (amount > 0)
);

-- Add indexes for performance
CREATE INDEX idx_transactions_user ON transactions(user_id);
CREATE INDEX idx_transactions_date ON transactions(tx_date);
CREATE INDEX idx_budgets_user_month ON budgets(user_id, budget_year, budget_month);
CREATE INDEX idx_goals_user ON goals(user_id);
