# Database Documentation

This document provides a detailed overview of the Folio.io data layer, focusing on the schema design, migration strategy, and data integrity rules.

## 1. Schema Overview

The system uses **PostgreSQL 15** and follows a relational model optimized for financial tracking and user-based isolation.

### Entity Relationship Diagram (Logical)
- **Users** $\xrightarrow{1:N}$ **Categories**
- **Users** $\xrightarrow{1:N}$ **Transactions**
- **Users** $\xrightarrow{1:N}$ **Budgets**
- **Users** $\xrightarrow{1:N}$ **Goals**
- **Categories** $\xrightarrow{1:N}$ **Transactions**
- **Categories** $\xrightarrow{1:N}$ **Budgets**
- **Goals** $\xrightarrow{1:N}$ **Goal Contributions**

### Table Specifications

#### `users`
Stores core identity and access control.
- `role`: Determines feature access (STANDARD, PREMIUM, ADMIN).
- `is_active`: Allows for account suspension without deleting data.

#### `categories`
Handles the classification of money flow.
- `is_system`: If `TRUE`, the category is seeded by the system (e.g., "Salaire") and cannot be deleted by the user.
- `unique_user_category`: Prevents duplicate category names for a single user.

#### `transactions`
The main ledger of the application.
- `amount`: Stored as `BIGINT` in **centimes** to avoid floating-point errors.
- `type`: `REVENU` (Income) or `DEPENSE` (Expense).
- `is_deleted`: Implements **soft-delete**. Queries must always filter by `is_deleted = FALSE`.

#### `budgets`
Tracks monthly spending targets.
- `limit_amount`: Stored in centimes.
- `unique_budget`: Ensures only one budget exists per user, per category, per month/year.

#### `goals` & `goal_contributions`
Tracks savings progress.
- `goals`: Defines the target amount and deadline.
- `goal_contributions`: A ledger of every single addition made toward a goal.

#### `revoked_tokens`
The JWT blacklist.
- Stores the token string and its original `expiry_date`.
- Used by the `JwtFilter` to immediately invalidate sessions on logout.

---

## 2. Migration Strategy

The project uses **Flyway** for versioned database migrations. This ensures that every environment (Dev, CI, Prod) has an identical schema.

### Migration Workflow
1. **Creation**: New changes are added as a new `.sql` file in `backend/src/main/resources/db/migration/`.
2. **Naming**: Files must follow the pattern `V<Version>__<Description>.sql` (e.g., `V1__init_schema.sql`).
3. **Execution**: On application startup, Spring Boot triggers Flyway, which:
   - Scans the `flyway_schema_history` table.
   - Executes any pending scripts in alphabetical/numerical order.
   - Updates the history table with the checksum of the applied script.

### Migration History
- **V1**: Initial schema creation (Users, Categories, Transactions, Budgets, Goals).
- **V2**: Addition of the `revoked_tokens` table for secure logout.

---

## 3. Data Integrity & Performance

### Precision
To maintain absolute financial accuracy, **all monetary values are handled as integers (centimes)**. 
- $1.00 \text{ DH} \rightarrow 100$
- $10.55 \text{ DH} \rightarrow 1055$
Conversion to decimals only happens at the UI layer for display.

### Indexing Strategy
To ensure fast lookups as the transaction history grows, the following indexes are implemented:
- `idx_transactions_user`: Optimizes user-specific ledger views.
- `idx_transactions_date`: Optimizes date-range filtering.
- `idx_transactions_deleted`: Speeds up the common filter `is_deleted = FALSE`.
- `idx_budgets_user_month`: Optimizes monthly budget retrieval.

### Constraints
- `CHECK (amount > 0)`: Ensures that transactions and budgets cannot have negative values (types are handled by the `type` column).
- `ON DELETE CASCADE`: Ensures that if a user is deleted, all their associated financial data is wiped to maintain referential integrity.
