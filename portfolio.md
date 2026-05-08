# GC_2026 — SPRINT PROMPT (3-Day School Project)
## Portefeuille Intelligent — Smart Personal Finance Management System

> **Purpose:** This prompt is designed for a **3-day intensive sprint** to deliver a working prototype.
> We're building an **MVP (Minimum Viable Product)**, not production software.
> Focus on core features that work over perfect architecture.

---

## PROJECT SCOPE — CRITICAL

| What's IN ✅ | What's OUT ❌ |
|---|---|
| User auth (register/login/logout) | Password reset by email |
| Transaction CRUD (add, edit, delete) | Batch import (skip for now) |
| Budget tracking with alerts | PDF exports (use CSV/Excel instead) |
| Savings goals with progress | Redis caching (skip, use DB only) |
| Categories (simple, no hierarchy) | Email notifications (skip) |
| Dashboard with KPIs | Optimistic locking complexity |
| CSV/Excel export | Admin panel (skip) |
| Standard/Premium user roles | Scheduled tasks (skip) |
| Data isolation per user | Swagger/API docs (skip) |
| Simple data validation | Advanced reporting |

---

## TECH STACK — SIMPLIFIED

| Layer | Technology | Notes |
|---|---|---|
| **Backend** | Spring Boot 3.x (Java 17) | `spring-boot-starter-web`, `spring-boot-starter-data-jpa` |
| **Database** | PostgreSQL 15+ | Simple schema, no complex migrations |
| **ORM** | Hibernate + Spring Data JPA | Standard `@Entity`, `@Repository` |
| **Security** | Spring Security + JWT | Simple tokens, no refresh tokens (keep it simple) |
| **Frontend** | React 18 + TypeScript | Functional components + hooks |
| **State** | React hooks (useState/useContext) | No Redux (skip the complexity) |
| **Styling** | TailwindCSS | Pre-configured, no shadcn/ui complexity |
| **Charts** | Recharts | Simple pie/bar charts only |
| **Export** | Apache POI (Excel) + manual CSV |
| **Validation** | Jakarta Bean Validation | Basic `@NotNull`, `@Positive` only |
| **Build** | Maven (backend) + Vite (frontend) | Standard setup |
| **Docker** | Docker Compose | Postgres + app only  |

---

## CURRENCY HANDLING — SIMPLE

- Store all monetary values as `BIGINT` in **centimes** (1 unit = 0.01 currency unit)
- Frontend divides by 100 only for **display**
- No floating-point math ever

---

## DATABASE SCHEMA (PostgreSQL)

```sql
-- Users
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20) DEFAULT 'STANDARD', -- STANDARD | PREMIUM | ADMIN
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
```

---

## BACKEND STRUCTURE (Spring Boot)

```
com.gc2026.portfolio/
├── config/              # DatabaseConfig, SecurityConfig, CorsConfig
├── domain/
│   ├── entity/          # User, Transaction, Category, Budget, Goal, GoalContribution
│   ├── enums/           # TransactionType, UserRole, GoalStatus
│   └── exception/       # ResourceNotFoundException, ValidationException
├── repository/          # Spring Data JPA interfaces
├── service/
│   ├── UserService
│   ├── TransactionService
│   ├── BudgetService
│   ├── GoalService
│   ├── CategoryService
│   └── ExportService
├── controller/
│   ├── AuthController
│   ├── TransactionController
│   ├── BudgetController
│   ├── GoalController
│   ├── CategoryController
│   ├── DashboardController
│   └── ExportController
├── dto/
│   ├── request/         # CreateTransactionRequest, UpdateBudgetRequest, etc.
│   └── response/        # TransactionResponse, BudgetResponse, etc.
├── security/
│   ├── JwtUtil          # Token generation/validation
│   ├── JwtFilter        # Request filter
│   └── CustomUserDetails
└── utils/
    └── CurrencyUtil     # Convert centimes ↔ display
```

### Key Classes Skeleton

```java
// JwtUtil (simple token handling)
public class JwtUtil {
  private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24 hours
  
  public String generateToken(String email) { /* ... */ }
  public String extractEmail(String token) { /* ... */ }
  public boolean isTokenValid(String token) { /* ... */ }
}

// User Service (no password reset, no email)
public class UserService {
  public User register(String email, String username, String password) { /* ... */ }
  public User login(String email, String password) { /* ... */ }
  public User getById(Long id) { /* ... */ }
  public User getCurrentUser() { /* extract from JWT */ }
}

// TransactionService
public class TransactionService {
  public Transaction create(Long userId, CreateTransactionRequest dto) { /* ... */ }
  public Transaction update(Long userId, Long txId, UpdateTransactionRequest dto) { /* ... */ }
  public void delete(Long userId, Long txId) { /* soft delete */ }
  public Page<Transaction> getByUser(Long userId, Pageable page) { /* ... */ }
  public BigDecimal calculateMonthlyBalance(Long userId, YearMonth month) { /* ... */ }
}

// BudgetService
public class BudgetService {
  public Budget createOrUpdate(Long userId, CreateBudgetRequest dto) { /* ... */ }
  public Budget getByUserAndMonth(Long userId, YearMonth month) { /* ... */ }
  public BudgetProgressDTO getProgress(Long budgetId) { 
    // Returns: current%, spent, remaining
  }
}

// GoalService
public class GoalService {
  public Goal create(Long userId, CreateGoalRequest dto) { /* ... */ }
  public Goal addContribution(Long goalId, Long amount) { /* ... */ }
  public GoalProgressDTO getProgress(Long goalId) { 
    // Returns: current%, milestones (25/50/75/100%)
  }
}
```

---

## FRONTEND STRUCTURE (React + TypeScript)

```
src/
├── api/                 # Axios instance, API calls
│   └── apiClient.ts
├── pages/
│   ├── Login.tsx
│   ├── Register.tsx
│   ├── Dashboard.tsx
│   ├── Transactions.tsx
│   ├── Budgets.tsx
│   ├── Goals.tsx
│   ├── Categories.tsx
│   └── Export.tsx
├── components/
│   ├── Navbar.tsx
│   ├── Sidebar.tsx
│   ├── TransactionForm.tsx
│   ├── BudgetCard.tsx
│   ├── GoalCard.tsx
│   ├── ProgressBar.tsx
│   └── ConfirmDialog.tsx
├── hooks/
│   ├── useAuth.ts
│   ├── useFetch.ts
│   └── useCurrency.ts
├── types/
│   └── index.ts         # TypeScript interfaces (match backend DTOs)
├── utils/
│   ├── formatCurrency.ts
│   ├── formatDate.ts
│   └── tokenStorage.ts
└── App.tsx
```

---

## DEVELOPMENT BLOCKS (6 Total) — 8-10 Hours Each

### **BLOCK 1: Backend Setup + Auth (Hours 1-8)**

**Scope:**
- Spring Boot project initialization
- PostgreSQL schema + Flyway migrations (basic)
- User entity + repository + service
- JWT utils (generate, validate tokens)
- Auth controller (register, login, logout)
- Spring Security configuration

**Deliverables:**
- ✅ `POST /api/v1/auth/register` — create account
- ✅ `POST /api/v1/auth/login` — return JWT token
- ✅ `POST /api/v1/auth/logout` — invalidate token (in-memory blacklist)
- ✅ `GET /api/v1/auth/me` — get current user from JWT

**Git Branch:** `feature/block-1-auth`

**Gate Checklist** (5 items):
- [X] Can register with email/username/password
- [X] Can login and receive JWT token
- [X] Invalid credentials return 401
- [X] Can extract user from token on protected endpoint
- [X] Logout works (token blacklist updated)

## Test Block 1

# Start (ensure Docker Postgres is running)
cd backend && ./mvnw spring-boot:run

# Register
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@test.com","username":"testuser","password":"password123"}' 

# Login
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@test.com","password":"password123"}'

# /me (paste token from above)
curl -s http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <TOKEN>"

# Logout
curl -s -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer <TOKEN>"

# Verify logout worked (should 401)
curl -s http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <TOKEN>"

---

### **BLOCK 2: Transaction Management (Hours 9-16)**

**Scope:**
- Transaction entity + CRUD operations
- TransactionService with validation
- TransactionController (create, read, update, delete)
- Pagination + filtering (by date, type, category)
- Data isolation (user can only access own transactions)
- Soft delete (logical delete, not physical)

**Deliverables:**
- ✅ `POST /api/v1/transactions` — add transaction
- ✅ `GET /api/v1/transactions` — list (paginated, filtered)
- ✅ `PUT /api/v1/transactions/{id}` — update
- ✅ `DELETE /api/v1/transactions/{id}` — soft delete
- ✅ Transaction quota enforcement (Standard: 500/month)

**Git Branch:** `feature/block-2-transactions`

**Gate Checklist** (7 items):
- [X] Can create transaction with all required fields
- [X] Invalid amount (negative, zero) rejected
- [X] User can only see own transactions
- [X] Pagination works (page=0&size=20)
- [X] Filtering by date range works
- [X] Standard user hits 500 limit → warning at 400
- [X] Delete marks transaction as deleted, doesn't remove from DB

---

## Test Block 2

# 1. Login to get a token
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@test.com","password":"password123"}'
# 2. Create transaction
curl -s -X POST http://localhost:8080/api/v1/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Salary","amount":250000,"type":"REVENU","categoryId":1,"txDate":"2026-05-08"}'
# 3. List transactions (paginated)
curl -s "http://localhost:8080/api/v1/transactions?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
# 4. Get by ID
curl -s http://localhost:8080/api/v1/transactions/1 \
  -H "Authorization: Bearer $TOKEN"
# 5. Update
curl -s -X PUT http://localhost:8080/api/v1/transactions/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Updated Salary","amount":300000,"type":"REVENU","categoryId":1,"txDate":"2026-05-08"}'
# 6. Delete (soft)
curl -s -X DELETE http://localhost:8080/api/v1/transactions/1 \
  -H "Authorization: Bearer $TOKEN"
# 7. Verify soft-delete (should return 404 or empty list)
curl -s http://localhost:8080/api/v1/transactions/1 \
  -H "Authorization: Bearer $TOKEN"
# 8. Filter tests
curl -s "http://localhost:8080/api/v1/transactions?startDate=2026-05-01&endDate=2026-05-31&type=REVENU" \
  -H "Authorization: Bearer $TOKEN"

### **BLOCK 3: Categories (Hours 17-22)**

**Scope:**
- Category entity (user-owned + system defaults)
- CRUD for user categories
- Category list (system + user categories)
- Validation (unique per user, name required)
- Limit enforcement (Standard: 10, Premium: unlimited)

**Deliverables:**
- ✅ `POST /api/v1/categories` — create custom category
- ✅ `GET /api/v1/categories` — list all (system + user)
- ✅ `PUT /api/v1/categories/{id}` — update custom category
- ✅ `DELETE /api/v1/categories/{id}` — delete (if no transactions)

**Git Branch:** `feature/block-3-categories`

**Gate Checklist** (5 items):
- [ ] Can create custom category with name, color, type
- [ ] System categories visible but not editable
- [ ] Cannot delete category with transactions
- [ ] Standard user limited to 10 custom categories
- [ ] Duplicate name rejected per user

---

### **BLOCK 4: Budgets + Alerts (Hours 23-30)**

**Scope:**
- Budget entity (category + month/year + limit)
- BudgetService (create, update, get progress)
- Real-time spent calculation (from transactions)
- Alert threshold logic (80% warning, 100% critical)
- Budget progress endpoint

**Deliverables:**
- ✅ `POST /api/v1/budgets` — create/update monthly budget
- ✅ `GET /api/v1/budgets/{month}` — list budgets for month
- ✅ `GET /api/v1/budgets/{id}/progress` — spent %, remaining amount
- ✅ Budget alerts (simple: threshold status in response)

**Git Branch:** `feature/block-4-budgets`

**Gate Checklist** (6 items):
- [ ] Can create budget for category + month
- [ ] Spent amount calculated from transactions
- [ ] Progress endpoint returns: spent %, spent amount, remaining
- [ ] Alert triggered at 80% threshold (in response)
- [ ] Cannot create duplicate budget for same category+month
- [ ] Budget limit cannot be zero or negative

---

### **BLOCK 5: Savings Goals (Hours 31-36)**

**Scope:**
- Goal entity (title, target amount, target date)
- Goal contributions (manual additions)
- Goal progress with milestones (25%, 50%, 75%, 100%)
- Status auto-update when target reached
- Standard: 1 active goal; Premium: unlimited

**Deliverables:**
- ✅ `POST /api/v1/goals` — create goal
- ✅ `GET /api/v1/goals` — list user's goals
- ✅ `POST /api/v1/goals/{id}/contribute` — add amount
- ✅ `GET /api/v1/goals/{id}/progress` — progress %, milestones

**Git Branch:** `feature/block-5-goals`

**Gate Checklist** (6 items):
- [ ] Can create goal with title, target amount, target date
- [ ] Can add contributions
- [ ] Progress calculated as current/target %
- [ ] Milestones highlighted (25/50/75/100%)
- [ ] Status auto-changes to ATTEINT when target reached
- [ ] Standard user limited to 1 active goal

---

### **BLOCK 6: Dashboard + Export (Hours 37-40)**

**Scope:**
- Dashboard KPIs (monthly balance, total income, total expenses, savings rate)
- Simple pie chart (top categories by spending)
- Export to CSV (Standard user)
- Export to Excel (Premium user)
- Frontend UI for all pages

**Deliverables:**
- ✅ `GET /api/v1/dashboard/kpis/{month}` — KPI data
- ✅ `GET /api/v1/dashboard/spending` — pie chart data
- ✅ `GET /api/v1/export/csv` — download CSV
- ✅ `GET /api/v1/export/excel` — download Excel (Premium only)

**Git Branch:** `feature/block-6-dashboard`

**Gate Checklist** (7 items):
- [ ] Dashboard shows correct KPIs (balance, income, expenses, rate)
- [ ] Pie chart displays top 8 categories + "Other"
- [ ] CSV export contains all transactions
- [ ] Excel export has proper formatting
- [ ] Premium user can export Excel
- [ ] Standard user cannot access Excel endpoint
- [ ] Frontend Dashboard page renders KPIs + chart

---

## API ENDPOINTS SUMMARY (25 Total)

**Auth (4)**
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`

**Transactions (5)**
- `POST /api/v1/transactions`
- `GET /api/v1/transactions` (paginated)
- `PUT /api/v1/transactions/{id}`
- `DELETE /api/v1/transactions/{id}`
- `GET /api/v1/transactions/{id}`

**Categories (4)**
- `POST /api/v1/categories`
- `GET /api/v1/categories`
- `PUT /api/v1/categories/{id}`
- `DELETE /api/v1/categories/{id}`

**Budgets (3)**
- `POST /api/v1/budgets`
- `GET /api/v1/budgets/{month}`
- `GET /api/v1/budgets/{id}/progress`

**Goals (4)**
- `POST /api/v1/goals`
- `GET /api/v1/goals`
- `POST /api/v1/goals/{id}/contribute`
- `GET /api/v1/goals/{id}/progress`

**Dashboard (2)**
- `GET /api/v1/dashboard/kpis/{month}`
- `GET /api/v1/dashboard/spending`

**Export (2)**
- `GET /api/v1/export/csv` (Standard + Premium)
- `GET /api/v1/export/excel` (Premium only)

---

## FRONTEND PAGES (8 Total)

1. **Login** — email, password
2. **Register** — email, username, password (validation)
3. **Dashboard** — KPI cards + pie chart
4. **Transactions** — list, search, filter, add/edit/delete forms
5. **Budgets** — cards with progress bars, create/edit
6. **Goals** — cards with progress, contribute modal
7. **Categories** — list, create, edit, delete
8. **Export** — CSV/Excel buttons

---

## SECURITY RULES (NON-NEGOTIABLE)

1. **User isolation:** Every query filters by `user_id`. A Standard user cannot access another user's transactions.
2. **JWT in Authorization header:** `Authorization: Bearer <token>`
3. **No sensitive data in JWT:** Only `userId` + `email` + `role`
4. **Role checks:** Endpoints verify user role (Standard vs Premium). Return **403 Forbidden** if access denied.
5. **Input validation:** All DTOs validated with `@NotNull`, `@Positive`, `@Email`, etc.
6. **Error responses:** Never expose stack traces. Return `{ error: "Descriptive message" }`

---

## ENVIRONMENT VARIABLES

```env
# Backend
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/portfolio_db
SPRING_DATASOURCE_USERNAME=portfolio_user
SPRING_DATASOURCE_PASSWORD=secret
JWT_SECRET=your-super-secret-key-min-256-bits-long-asdfghjklqwertyuiopzxcvbnm
JWT_EXPIRATION_MS=86400000

# Frontend
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

---

## DEVELOPMENT WORKFLOW

### Day 1 (Blocks 1-2)
- 8h: Auth setup + API working
- 8h: Transactions CRUD + validation

### Day 2 (Blocks 3-4)
- 6h: Categories (simple)
- 8h: Budgets + alert logic
- 2h: Start React frontend (login + navbar)

### Day 3 (Blocks 5-6)
- 5h: Goals + contributions
- 3h: Dashboard + KPIs
- 2h: Export (CSV/Excel)

---

## TESTING (Simple, Not Extensive)

**Backend (Postman / curl)**
- Test each endpoint manually
- Verify user isolation (create 2 users, confirm they see only own data)
- Check role-based access (create Standard user, try Premium endpoint → 403)
- Test validation (send invalid amounts, missing fields → 400)

**Frontend**
- Login → Dashboard loads
- Create transaction → appears in list
- Filter transactions → works
- Create budget → progress bar appears
- Create goal → progress with milestones
- Export button downloads file

---

## DO's AND DON'Ts

### DO ✅
- Keep it simple — what works is better than perfect
- Use existing libraries (Spring Data, Hibernate, Recharts)
- Test manually before moving to next block
- Push to Git after each block (feature branch → PR → main)
- Ask for clarification if requirements are ambiguous

### DON'T ❌
- Don't add features not in this prompt (no emails, no PDFs, no Redis)
- Don't over-engineer (no design patterns beyond basics)
- Don't write tests (manual testing is fine for a sprint)
- Don't refactor — move forward
- Don't skip validation — invalid data breaks everything

---

## SUCCESS CRITERIA

By end of Day 3, you should have:

✅ **Backend Running**
- All 25 endpoints working
- PostgreSQL with data
- JWT auth working
- User isolation verified

✅ **Frontend Functional**
- Login/Register pages
- Dashboard with KPIs
- Transaction list + form
- Budget cards
- Goal cards
- Export buttons

✅ **Data Verified**
- Create 2 users
- Each sees only own data
- Standard user limited to 500 tx/month
- Standard user limited to 1 goal
- Budget alerts trigger
- Goal progress calculates
- CSV/Excel downloads

✅ **Git Clean**
- 6 feature branches merged to main
- Clear commit messages
- README.md with setup instructions

---

## TROUBLESHOOTING

**"Token invalid"**
- Check `Authorization: Bearer <token>` header format
- Verify `JWT_SECRET` env var matches both backend + frontend (if needed)

**"User isolation broken"**
- Add `WHERE user_id = getCurrentUserId()` to every query
- Test with 2 different user tokens

**"Frontend can't reach backend"**
- Check `VITE_API_BASE_URL`
- Verify backend is running on `localhost:8080`
- Check CORS config in backend

**"Password too weak"**
- No password complexity rules! Any password works.

**"Transaction amount wrong"**
- Remember: stored in **centimes**, display by dividing by 100

---

## FINAL NOTES

This is a **school project**, not production. Your goal is a **working demo**, not a bulletproof system.
Focus on **features that work** over code that's perfect. You have 40 hours max.

Good luck! 🚀
