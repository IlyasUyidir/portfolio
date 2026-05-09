# Portefeuille Intelligent — Frontend Implementation Plan
### React + TypeScript · Vite · Tailwind CSS

> **Purpose:** This document is the strict, authoritative guide for building the
> frontend of *Portefeuille Intelligent*. Follow the blocks in order. Do **not**
> implement features that are not listed here. When in doubt, re-read the scope.

---

## Table of Contents

1. [Tech Stack & Libraries](#1-tech-stack--libraries)
2. [Project Structure](#2-project-structure)
3. [Design System & Theme](#3-design-system--theme)
4. [TypeScript Interfaces (DTOs)](#4-typescript-interfaces-dtos)
5. [API Client Layer](#5-api-client-layer)
6. [Component Architecture](#6-component-architecture)
7. [Routing Strategy](#7-routing-strategy)
8. [State & Auth Strategy](#8-state--auth-strategy)
9. [Currency & Date Utilities](#9-currency--date-utilities)
10. [Block-by-Block Execution Plan](#10-block-by-block-execution-plan)
11. [Environment Variables](#11-environment-variables)
12. [Development Rules](#12-development-rules)

---

## 1. Tech Stack & Libraries

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| **Framework** | React | 18.x | UI rendering, functional components + hooks |
| **Language** | TypeScript | 5.x | Type safety, strict mode enabled |
| **Build Tool** | Vite | 5.x | Fast HMR dev server, production bundler |
| **Styling** | Tailwind CSS | 3.x | Utility-first styling, dark theme |
| **Routing** | React Router DOM | 6.x | SPA navigation, protected routes |
| **HTTP Client** | Axios | 1.x | API calls, interceptors for JWT injection |
| **Forms** | React Hook Form | 7.x | Form state, validation, error messages |
| **Charts** | Recharts | 2.x | Pie chart (spending), Bar chart (revenue vs expenses) |
| **Global State** | React Context API | built-in | Auth state only (user, token, role) |
| **Local State** | useState / useReducer | built-in | Page-level and component-level state |
| **Icons** | Lucide React | latest | Consistent SVG icon set |

> **No Redux. No React Query. No external state manager.** The backend is
> simple and paginated; direct `useEffect` + `useState` is sufficient for
> this sprint scope.

### Installation Command

```bash
npm create vite@latest portefeuille-frontend -- --template react-ts
cd portefeuille-frontend
npm install react-router-dom axios react-hook-form recharts lucide-react
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
```

---

## 2. Project Structure

```
src/
├── api/
│   ├── apiClient.ts          # Axios instance + JWT interceptor
│   ├── authApi.ts            # register, login, logout, me
│   ├── transactionApi.ts     # CRUD + list (paginated)
│   ├── categoryApi.ts        # list, create, update, delete
│   ├── budgetApi.ts          # create, list by month, progress
│   ├── goalApi.ts            # create, list, contribute, progress
│   ├── dashboardApi.ts       # KPIs, spending breakdown
│   └── exportApi.ts          # CSV, Excel download (blob)
│
├── components/
│   ├── layout/
│   │   ├── AppShell.tsx      # Sidebar + top bar wrapper
│   │   ├── Sidebar.tsx       # Left nav (icons + labels)
│   │   └── TopBar.tsx        # Page title + "+ Nouvelle transaction" button
│   ├── ui/
│   │   ├── KpiCard.tsx       # Stat card (solde, revenus, dépenses, taux)
│   │   ├── AlertBanner.tsx   # Red/yellow inline budget alert bar
│   │   ├── ProgressBar.tsx   # Generic progress bar (budget/goal)
│   │   ├── Badge.tsx         # Category color badge (Alimentation, etc.)
│   │   ├── ConfirmDialog.tsx # Modal: "Confirmer la suppression?"
│   │   ├── EmptyState.tsx    # No data placeholder
│   │   └── PremiumBadge.tsx  # "PRO" badge + upgrade prompt
│   ├── transactions/
│   │   ├── TransactionTable.tsx   # Sortable table with pagination
│   │   ├── TransactionFilters.tsx # Date range, type, category dropdowns
│   │   ├── TransactionForm.tsx    # Add/edit form (Dépense / Revenu tabs)
│   │   └── TransactionDetail.tsx  # Read-only detail view card
│   ├── budgets/
│   │   ├── BudgetCard.tsx         # Category, limit, spent, progress bar
│   │   └── BudgetForm.tsx         # Create/edit budget form
│   ├── goals/
│   │   ├── GoalCard.tsx           # Title, progress, milestones, status
│   │   ├── GoalForm.tsx           # Create goal form
│   │   └── ContributeModal.tsx    # Add contribution modal
│   ├── charts/
│   │   ├── SpendingPieChart.tsx   # Recharts PieChart (top 8 + Autres)
│   │   └── RevenueExpensesBar.tsx # Recharts BarChart (last 6 months)
│   └── categories/
│       ├── CategoryList.tsx       # System + user categories table
│       └── CategoryForm.tsx       # Create/edit category (name, color, type)
│
├── context/
│   └── AuthContext.tsx       # AuthProvider, useAuth hook
│
├── hooks/
│   ├── useAuth.ts            # Shortcut: useContext(AuthContext)
│   └── usePagination.ts      # page, size, totalPages state helper
│
├── pages/
│   ├── Login.tsx
│   ├── Register.tsx
│   ├── Dashboard.tsx
│   ├── Transactions.tsx
│   ├── TransactionDetailPage.tsx
│   ├── Budgets.tsx
│   ├── Goals.tsx
│   ├── Categories.tsx
│   └── Export.tsx
│
├── types/
│   └── index.ts              # All TypeScript interfaces (mirror backend DTOs)
│
├── utils/
│   ├── formatCurrency.ts     # centimes → "1 200,00 DH"
│   ├── formatDate.ts         # ISO → "14 juin 2026"
│   └── tokenStorage.ts       # get/set/remove JWT from localStorage
│
├── App.tsx                   # Router + AuthProvider wrapper
├── main.tsx                  # ReactDOM.createRoot
└── index.css                 # Tailwind directives + CSS variables
```

---

## 3. Design System & Theme

The mockups use a **dark theme** with a dark navy/charcoal background, yellow
primary action color, and colored category badges. Replicate this exactly.

### `tailwind.config.ts`

```typescript
import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Background hierarchy
        'bg-base':    '#0F1117',  // page background
        'bg-card':    '#1A1D2E',  // card / panel background
        'bg-sidebar': '#13151F',  // sidebar background
        'bg-input':   '#252836',  // form input background

        // Brand / accent
        'primary':    '#F5C518',  // yellow CTA (buttons, links)
        'primary-hover': '#D4A800',

        // Status
        'danger':     '#EF4444',  // budget dépassé, negative amounts
        'warning':    '#F59E0B',  // budget alert 80%
        'success':    '#22C55E',  // positive amounts, goal atteint
        'info':       '#3B82F6',  // neutral info

        // Text
        'text-primary':   '#FFFFFF',
        'text-secondary': '#9CA3AF',
        'text-muted':     '#6B7280',

        // Border
        'border-subtle':  '#2A2D3E',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
    },
  },
  plugins: [],
} satisfies Config
```

### Global CSS Variables (`index.css`)

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  body {
    @apply bg-bg-base text-text-primary;
    font-family: 'Inter', sans-serif;
  }

  /* Scrollbar styling to match dark theme */
  ::-webkit-scrollbar { width: 6px; }
  ::-webkit-scrollbar-track { background: #1A1D2E; }
  ::-webkit-scrollbar-thumb { background: #3A3D50; border-radius: 3px; }
}
```

---

## 4. TypeScript Interfaces (DTOs)

All interfaces in `src/types/index.ts` mirror backend response DTOs exactly.

```typescript
// ─── Auth ─────────────────────────────────────────────────────────────────
export interface LoginRequest {
  email: string;
  password: string;
}
export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
}
export interface AuthResponse {
  token: string;
  user: UserProfile;
}
export interface UserProfile {
  id: number;
  email: string;
  username: string;
  role: 'STANDARD' | 'PREMIUM' | 'ADMIN';
  createdAt: string;
}

// ─── Transactions ──────────────────────────────────────────────────────────
export type TransactionType = 'REVENU' | 'DEPENSE';

export interface Transaction {
  id: number;
  title: string;
  amount: number;           // in centimes — divide by 100 for display
  type: TransactionType;
  category: Category;
  txDate: string;           // ISO date: "2026-06-14"
  description?: string;
  createdAt: string;
}
export interface CreateTransactionRequest {
  title: string;
  amount: number;           // in centimes
  type: TransactionType;
  categoryId: number;
  txDate: string;
  description?: string;
}
export interface TransactionPage {
  content: Transaction[];
  totalElements: number;
  totalPages: number;
  number: number;           // current page (0-indexed)
  size: number;
}

// ─── Categories ────────────────────────────────────────────────────────────
export interface Category {
  id: number;
  name: string;
  color: string;            // hex color: "#F59E0B"
  type: TransactionType | 'BOTH';
  isSystem: boolean;
}
export interface CreateCategoryRequest {
  name: string;
  type: TransactionType | 'BOTH';
  color: string;
}

// ─── Budgets ───────────────────────────────────────────────────────────────
export interface Budget {
  id: number;
  category: Category;
  budgetYear: number;
  budgetMonth: number;
  limitAmount: number;      // centimes
  alertThreshold: number;   // percentage (default 80)
}
export interface BudgetProgress {
  budgetId: number;
  category: Category;
  limitAmount: number;
  spentAmount: number;
  remainingAmount: number;
  spentPercentage: number;
  alertStatus: 'OK' | 'WARNING' | 'CRITICAL';
}
export interface CreateBudgetRequest {
  categoryId: number;
  budgetYear: number;
  budgetMonth: number;
  limitAmount: number;      // centimes
  alertThreshold?: number;
}

// ─── Goals ─────────────────────────────────────────────────────────────────
export type GoalStatus = 'EN_COURS' | 'ATTEINT' | 'EN_RETARD';

export interface Goal {
  id: number;
  title: string;
  targetAmount: number;     // centimes
  currentAmount: number;    // centimes
  targetDate: string;
  status: GoalStatus;
  createdAt: string;
}
export interface GoalProgress {
  goalId: number;
  title: string;
  targetAmount: number;
  currentAmount: number;
  progressPercentage: number;
  milestones: {
    twentyFive: boolean;
    fifty: boolean;
    seventyFive: boolean;
    hundred: boolean;
  };
  status: GoalStatus;
}
export interface CreateGoalRequest {
  title: string;
  targetAmount: number;     // centimes
  targetDate: string;
}
export interface ContributeRequest {
  amount: number;           // centimes
}

// ─── Dashboard ─────────────────────────────────────────────────────────────
export interface DashboardKpis {
  month: string;            // "2026-06"
  monthlyBalance: number;   // centimes
  totalRevenue: number;     // centimes
  totalExpenses: number;    // centimes
  savingsRate: number;      // percentage (e.g. 35.4)
  revenueVsPreviousMonth: number;   // % change
  expensesVsPreviousMonth: number;  // % change
}
export interface SpendingCategory {
  category: Category;
  totalAmount: number;      // centimes
}

// ─── Pagination helper ─────────────────────────────────────────────────────
export interface PaginationParams {
  page: number;
  size: number;
  startDate?: string;
  endDate?: string;
  type?: TransactionType;
  categoryId?: number;
}
```

---

## 5. API Client Layer

### `src/api/apiClient.ts`

```typescript
import axios from 'axios';
import { getToken, removeToken } from '../utils/tokenStorage';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL, // http://localhost:8080/api/v1
  headers: { 'Content-Type': 'application/json' },
});

// Attach JWT to every request automatically
apiClient.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Global error handling: 401 → clear token and redirect to /login
apiClient.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      removeToken();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default apiClient;
```

### `src/utils/tokenStorage.ts`

```typescript
const TOKEN_KEY = 'pi_jwt_token';
export const getToken = (): string | null => localStorage.getItem(TOKEN_KEY);
export const setToken = (token: string): void => localStorage.setItem(TOKEN_KEY, token);
export const removeToken = (): void => localStorage.removeItem(TOKEN_KEY);
```

### API Module Signatures

```typescript
// authApi.ts
export const register = (data: RegisterRequest): Promise<AuthResponse>
export const login = (data: LoginRequest): Promise<AuthResponse>
export const logout = (): Promise<void>
export const getMe = (): Promise<UserProfile>

// transactionApi.ts
export const listTransactions = (params: PaginationParams): Promise<TransactionPage>
export const getTransaction = (id: number): Promise<Transaction>
export const createTransaction = (data: CreateTransactionRequest): Promise<Transaction>
export const updateTransaction = (id: number, data: CreateTransactionRequest): Promise<Transaction>
export const deleteTransaction = (id: number): Promise<void>

// categoryApi.ts
export const listCategories = (): Promise<Category[]>
export const createCategory = (data: CreateCategoryRequest): Promise<Category>
export const updateCategory = (id: number, data: CreateCategoryRequest): Promise<Category>
export const deleteCategory = (id: number): Promise<void>

// budgetApi.ts
export const createBudget = (data: CreateBudgetRequest): Promise<Budget>
export const listBudgetsByMonth = (month: string): Promise<BudgetProgress[]>  // "2026-06"
export const getBudgetProgress = (id: number): Promise<BudgetProgress>

// goalApi.ts
export const listGoals = (): Promise<Goal[]>
export const createGoal = (data: CreateGoalRequest): Promise<Goal>
export const contribute = (id: number, data: ContributeRequest): Promise<Goal>
export const getGoalProgress = (id: number): Promise<GoalProgress>

// dashboardApi.ts
export const getKpis = (month: string): Promise<DashboardKpis>
export const getSpending = (): Promise<SpendingCategory[]>

// exportApi.ts — returns Blob for file download
export const downloadCsv = (): Promise<Blob>
export const downloadExcel = (): Promise<Blob>
```

---

## 6. Component Architecture

### Layout Components

#### `AppShell.tsx`
Wraps every protected page. Renders `<Sidebar />` on the left and the page
content area on the right. Applies the dark background and correct padding.

```
┌──────────────────────────────────────────────────────┐
│  Sidebar (fixed, 220px) │  <Outlet /> (scrollable)  │
└──────────────────────────────────────────────────────┘
```

**Props:** none — reads location from React Router to highlight active nav item.

#### `Sidebar.tsx`
**Visual:** Dark `bg-sidebar` background, logo top-left, nav links with icons,
user avatar + name + role badge bottom-left.

**Nav Links:**
| Label | Icon | Route |
|---|---|---|
| Tableau de bord | `LayoutDashboard` | `/dashboard` |
| Transactions | `ArrowLeftRight` | `/transactions` |
| Budgets | `PieChart` | `/budgets` |
| Objectifs | `Target` | `/goals` |
| Catégories | `Tag` | `/categories` |
| Statistiques | `BarChart2` | — (PRO badge, disabled for STANDARD) |
| Export / Import | `Download` | `/export` |
| Profil | `User` | — (future) |

#### `TopBar.tsx`
**Props:** `title: string`, `action?: { label: string; onClick: () => void }`

Renders the page title (bold yellow "de bord" suffix as in mockup) and the
yellow `+ Nouvelle transaction` button when an action is passed.

---

### UI Primitive Components

#### `KpiCard.tsx`
**Props:**
```typescript
interface KpiCardProps {
  label: string;            // "SOLDE DU MOIS"
  value: string;            // "+4 250 DH"
  valueColor?: 'success' | 'danger' | 'default'; // default = white
  icon: LucideIcon;
  trend?: string;           // "+12% vs mai"
}
```
Renders a dark card with label, large value, icon top-right, and optional
trend line below.

#### `AlertBanner.tsx`
**Props:**
```typescript
interface AlertBannerProps {
  message: string;
  severity: 'warning' | 'critical';
  onDismiss: () => void;
}
```
Horizontal bar inside the dashboard content area. Yellow for `warning`, red for
`critical`. Dismissable (×). Matches the "Alerte critique : Votre budget
« Alimentation » a dépassé..." banner in the mockup.

#### `ProgressBar.tsx`
**Props:**
```typescript
interface ProgressBarProps {
  percent: number;          // 0-100
  status?: 'ok' | 'warning' | 'critical';
  showLabel?: boolean;
}
```
Color logic: `ok` → green, `warning` → yellow, `critical` → red. Used in
`BudgetCard` and `GoalCard`.

#### `Badge.tsx`
**Props:** `label: string; color: string` (hex)

Small rounded pill. Used to show category name with its associated color dot.

#### `ConfirmDialog.tsx`
**Props:** `isOpen; title; message; onConfirm; onCancel`

Centered modal overlay for destructive actions (delete transaction, etc.).

#### `PremiumBadge.tsx`
Renders the yellow `PRO` badge shown next to "Statistiques" in the sidebar.
Also renders the upgrade prompt banner shown on the Standard dashboard.

---

### Transaction Components

#### `TransactionTable.tsx`
**Props:** `transactions: Transaction[]; onEdit; onDelete; onView`

Renders the sortable table from mockup page 4:
- Columns: Date ↑, Titre, Catégorie (badge), Type (colored chip), Montant
  (green/red), Actions (view · edit · delete icons)
- Row click → navigate to `/transactions/:id`

#### `TransactionFilters.tsx`
**Props:** `filters; onChange`

Renders the filter bar: search input, "Tous les types" dropdown, "Toutes
catégories" dropdown, date range pickers (start/end), Min/Max amount inputs,
Filtrer + Réinitialiser buttons.

#### `TransactionForm.tsx`
**Props:** `mode: 'create' | 'edit'; initialData?; onSuccess; onCancel`

Matches mockup page 5. Uses React Hook Form.
- Toggle: Dépense | Revenu (controls type)
- Fields: Titre*, Montant* (DH suffix), Catégorie* (select), Date*, Description
  (optional textarea)
- Validation: title required, amount > 0, category required, date required
- Submit: "Enregistrer la transaction", Cancel: "Annuler"

**Amount field rule:** User enters human-readable amount (e.g. `320.00`).
Multiply by 100 before sending to API. Divide by 100 when populating edit form.

#### `TransactionDetail.tsx`
**Props:** `transaction: Transaction; onEdit; onArchive`

Matches mockup page 6 — card view with: icon+title+amount header, Dépense badge,
DATE/MONTANT/CATÉGORIE/TYPE/CRÉÉ LE/MODIFIÉ LE fields, description block,
soft-delete note, "Modifier" (yellow) + "Archiver" buttons.

---

### Budget Components

#### `BudgetCard.tsx`
**Props:** `progress: BudgetProgress; onEdit; onDelete`

Matches mockup page 7. Shows:
- Category icon + name + status badge (Dépassé / Alerte 80%)
- Three-line stats: Dépensé X DH · Limite Y DH · Reste Z DH
- `ProgressBar` with correct color for `alertStatus`
- Percentage label top-right (e.g. "115%")
- Edit + Delete icons

#### `BudgetForm.tsx`
**Props:** `initialData?; categories: Category[]; onSuccess; onCancel`

Fields: Catégorie (select), Mois (month picker — `budgetYear` + `budgetMonth`),
Limite (DH input), Seuil d'alerte % (default 80).

---

### Goal Components

#### `GoalCard.tsx`
**Props:** `goal: Goal; progress: GoalProgress; onContribute; onDelete`

Shows: goal title, status chip, target date, current/target amounts,
`ProgressBar`, milestone markers at 25/50/75/100% (filled dots when reached).

#### `ContributeModal.tsx`
**Props:** `goalId; goalTitle; onSuccess; onClose`

Simple modal: amount input (DH), "Ajouter" button. Calls
`goalApi.contribute()`.

---

### Chart Components

#### `SpendingPieChart.tsx`
**Props:** `data: SpendingCategory[]`

Recharts `PieChart` + `Tooltip`. Maps backend `SpendingCategory[]` to
`{ name, value, fill }`. Uses category colors. Matches mockup left chart.

#### `RevenueExpensesBar.tsx`
**Props:** `data: { month: string; revenue: number; expenses: number }[]`

Recharts `BarChart` with two bar series (green = revenue, red = expenses).
Last 6 months. Matches mockup right chart. Note: data is mocked on the
frontend since the backend only provides the current month's KPIs.

---

## 7. Routing Strategy

### Route Map

```
/                      → Redirect to /dashboard (if authenticated) or /login
/login                 → Login page              (public)
/register              → Register page           (public)
/dashboard             → Dashboard               (protected)
/transactions          → Transaction list        (protected)
/transactions/:id      → Transaction detail      (protected)
/transactions/new      → Add transaction form    (protected)
/budgets               → Budget list + forms     (protected)
/goals                 → Goal list + forms       (protected)
/categories            → Category management     (protected)
/export                → Export page             (protected)
```

### `App.tsx` Structure

```tsx
<AuthProvider>
  <BrowserRouter>
    <Routes>
      {/* Public */}
      <Route path="/login"    element={<Login />} />
      <Route path="/register" element={<Register />} />

      {/* Protected — wrapped in AppShell */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route path="/"               element={<Navigate to="/dashboard" />} />
          <Route path="/dashboard"      element={<Dashboard />} />
          <Route path="/transactions"   element={<Transactions />} />
          <Route path="/transactions/:id" element={<TransactionDetailPage />} />
          <Route path="/budgets"        element={<Budgets />} />
          <Route path="/goals"          element={<Goals />} />
          <Route path="/categories"     element={<Categories />} />
          <Route path="/export"         element={<Export />} />
        </Route>
      </Route>

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  </BrowserRouter>
</AuthProvider>
```

### `ProtectedRoute.tsx`

```tsx
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export const ProtectedRoute = () => {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
};
```

---

## 8. State & Auth Strategy

### Token Storage

JWT token is stored in **`localStorage`** under key `pi_jwt_token`.

- **On login success:** call `setToken(response.token)`, store `UserProfile`
  in `AuthContext`.
- **On logout:** call `removeToken()`, clear context, redirect to `/login`.
- **On app load:** read token from `localStorage`, call `GET /auth/me` to
  restore user profile (validates token is still live). If it fails → clear
  token.

### `AuthContext.tsx`

```tsx
interface AuthContextValue {
  user: UserProfile | null;
  isAuthenticated: boolean;
  isPremium: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  isLoading: boolean;        // true while restoring session on app boot
}
```

**Init flow on `<AuthProvider>` mount:**

```
1. getToken() → if null → set isAuthenticated = false
2. if token exists → call getMe()
   → success → set user, isAuthenticated = true
   → failure → removeToken(), isAuthenticated = false
3. set isLoading = false
```

**Global state kept in context:** `user`, `isAuthenticated`, `isPremium`.

**Everything else** (transaction list, budgets, goals) lives in **local page
state** via `useState` + `useEffect`. No need for global state here.

### Role-Based UI Gating

```tsx
const { isPremium } = useAuth();

// Disable/hide premium-only features:
{!isPremium && <PremiumBadge />}

// In sidebar, Statistics link:
<NavLink
  to="/statistics"
  className={!isPremium ? 'opacity-40 pointer-events-none' : ''}
>
  Statistiques {!isPremium && <PremiumBadge />}
</NavLink>
```

---

## 9. Currency & Date Utilities

### `formatCurrency.ts`

```typescript
/**
 * Converts centimes to display string.
 * 1250000 → "+12 500,00 DH"
 * -32000  → "-320,00 DH"
 */
export const formatCurrency = (centimes: number, showSign = false): string => {
  const value = centimes / 100;
  const formatted = new Intl.NumberFormat('fr-MA', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Math.abs(value));

  const sign = centimes < 0 ? '-' : showSign ? '+' : '';
  return `${sign}${formatted} DH`;
};

/**
 * Converts user input (e.g. "320.50") to centimes for the API.
 * "320.50" → 32050
 */
export const toCentimes = (input: string | number): number => {
  return Math.round(parseFloat(String(input)) * 100);
};

/**
 * Converts centimes back to input value string for form fields.
 * 32050 → "320.50"
 */
export const fromCentimes = (centimes: number): string => {
  return (centimes / 100).toFixed(2);
};
```

### `formatDate.ts`

```typescript
/**
 * "2026-06-14" → "14 juin 2026"
 */
export const formatDate = (isoDate: string): string => {
  return new Date(isoDate).toLocaleDateString('fr-MA', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  });
};

/**
 * Returns current month as "2026-06"
 */
export const currentMonth = (): string => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
};
```

---

## 10. Block-by-Block Execution Plan

> Each block maps directly to one backend block. Complete one block fully
> before starting the next. Push to a feature branch after each block.

---

### ✅ Block F1 — Project Setup & Auth UI
**Maps to:** Backend Block 1 (Auth)
**Branch:** `feature/frontend-block-1-auth`
**Estimated time:** 4–6 hours

**Goals:**
- Vite + React + TypeScript project scaffold
- Tailwind configured with dark theme tokens
- `tokenStorage.ts` + `apiClient.ts` with interceptors
- `AuthContext.tsx` with full init flow
- Login page UI (matching mockup exactly)
- Register page UI (matching mockup exactly)
- Forgot password page (UI only — no backend endpoint)
- React Router with `ProtectedRoute`

**Step-by-step tasks:**

1. **Scaffold** — `npm create vite`, install all dependencies, configure
   `tailwind.config.ts` with custom color tokens from §3.
2. **Token utils** — implement `tokenStorage.ts` (get/set/remove).
3. **API client** — implement `apiClient.ts` with JWT interceptor + 401 handler.
4. **Auth API** — implement `authApi.ts` (register, login, logout, getMe).
5. **AuthContext** — implement `AuthContext.tsx` + `useAuth.ts` hook.
6. **Login page** — two-panel layout (left: branding/logo, right: form).
   Use `react-hook-form` for validation.
   Fields: email (required, valid email), password (required).
   "Se connecter" button → call `login()` → redirect to `/dashboard`.
   "Continuer avec Google" button → render only, no action (out of scope).
7. **Register page** — centered card layout.
   Fields: username, email, password, confirm password.
   Validation: passwords match, min 8 chars.
   "Créer mon compte" → call `register()` → redirect to `/dashboard`.
8. **Forgot password page** — centered card, email field, "Envoyer le lien"
   button (displays success message, no real API call — out of scope).
9. **ProtectedRoute** — redirect unauthenticated users to `/login`.
10. **Smoke test:** Can register, login, see `/dashboard` placeholder,
    logout redirects to `/login`.

**Gate Checklist:**
- [X] `npm run dev` starts without errors
- [X] Can register a new user
- [X] Can login and see `/dashboard` (even if just "Dashboard placeholder")
- [X] Invalid credentials show error message under form fields
- [X] Unauthenticated visit to `/dashboard` redirects to `/login`
- [X] Logout clears token and redirects to `/login`

---

### ✅ Block F2 — App Shell & Dashboard
**Maps to:** Backend Block 2 + Block 6 (Transactions, Dashboard)
**Branch:** `feature/frontend-block-2-dashboard`
**Estimated time:** 5–7 hours

**Goals:**
- `AppShell` with `Sidebar` and `TopBar`
- Dashboard page with KPI cards, alerts, two charts
- Minimal transaction data flowing from the API

**Step-by-step tasks:**

1. **Sidebar** — implement full nav from §6. Highlight active route.
   Bottom section: avatar circle (user initials), username, role badge.
   Mark "Statistiques" as PRO-only (disabled for STANDARD users).
2. **TopBar** — page title with yellow accent on last word (use `<span>`),
   optional yellow CTA button (passed as prop).
3. **AppShell** — `flex h-screen` layout: sidebar fixed left, scrollable right.
4. **KpiCard** — implement with icon, value, label, trend (§6).
5. **AlertBanner** — implement dismissable banner (§6).
6. **SpendingPieChart** — implement with Recharts PieChart. Use category
   colors from API.
7. **RevenueExpensesBar** — implement with Recharts BarChart. Hard-code 6
   month labels; use `totalRevenue`/`totalExpenses` from KPI for current month,
   zero-fill the rest for the sprint.
8. **Dashboard page** — wire all pieces together:
   - Month selector state (default: `currentMonth()`)
   - Fetch `getKpis(month)` → populate 4 `KpiCard`s
   - Fetch `getSpending()` → populate `SpendingPieChart`
   - Render `AlertBanner` if any budget has `CRITICAL` status (check budget
     list in Block F4)
   - Render "Transactions récentes" — last 5 transactions (reuse data from
     `listTransactions({ page: 0, size: 5 })`)
   - "Premium upgrade" banner for STANDARD users (bottom of revenue chart)
9. **PremiumBadge** component — yellow pill.

**Gate Checklist:**
- [X] Sidebar renders with all nav links
- [X] Active page is highlighted in sidebar
- [X] Dashboard KPI cards show real numbers from API
- [X] Pie chart renders top spending categories with colors
- [X] "Voir tout →" link navigates to `/transactions`
- [X] PRO badge appears on Statistics nav link for STANDARD users

---

### ✅ Block F3 — Transactions
**Maps to:** Backend Block 2 (Transactions CRUD)
**Branch:** `feature/frontend-block-3-transactions`
**Estimated time:** 6–8 hours

**Goals:**
- Full transactions list with pagination and filters
- Add / Edit form (modal or page)
- Transaction detail page
- Delete with confirmation

**Step-by-step tasks:**

1. **TransactionFilters** — implement the filter bar (§6). State: `filters`
   object. On change → call parent's `onFilterChange`.
2. **TransactionTable** — implement sortable table (§6). Render category badge
   with color, type chip (green "Revenu" / red "Dépense"), formatted amount
   (+green / −red), action icons (view · edit · delete).
3. **Transactions page** — assemble:
   - State: `transactions`, `page`, `totalPages`, `filters`, `isLoading`
   - On mount + filter change → `listTransactions(params)`
   - Pagination controls (prev/next + page numbers, matching mockup)
   - Limit warning banner for STANDARD users approaching 500
   - "Vous avez utilisé X transactions sur 500" badge
   - "Passer Premium →" link
4. **TransactionForm** — implement with React Hook Form (§6):
   - Tab toggle: Dépense | Revenu
   - All fields with validation
   - On submit: `toCentimes(amount)` before sending to API
   - On edit: `fromCentimes(transaction.amount)` to pre-fill
   - Success → refresh list + close form
5. **Add button** — `TopBar` action "+" → open `TransactionForm` in a slide-in
   panel or navigate to `/transactions/new`.
6. **TransactionDetailPage** — renders `TransactionDetail` card (§6).
   "Modifier" → navigate to edit form. "Archiver" → call `deleteTransaction()`,
   show `ConfirmDialog` first.
7. **ConfirmDialog** — implement (§6).
8. **EmptyState** — show when no transactions match filters.

**Gate Checklist:**
- [ ] Transaction list loads with pagination
- [ ] Filters (type, category, date range) update results correctly
- [ ] Can create a new transaction (form validates, API call succeeds)
- [ ] Can edit an existing transaction (pre-filled form)
- [ ] Delete shows confirmation dialog, then removes from list
- [ ] Transaction detail page shows all fields
- [ ] Amount displays correctly (centimes ÷ 100 = DH)
- [ ] Standard user sees 500-transaction warning banner

---

### ✅ Block F4 — Budgets
**Maps to:** Backend Block 4 (Budgets)
**Branch:** `feature/frontend-block-4-budgets`
**Estimated time:** 3–4 hours

**Goals:**
- Budget list for selected month with progress bars and alert states
- Create/edit budget form
- Alert banners on dashboard wired to budget status

**Step-by-step tasks:**

1. **BudgetCard** — implement (§6) with `ProgressBar`, color-coded by
   `alertStatus`, edit/delete icons.
2. **BudgetForm** — implement with React Hook Form. Category select pulls from
   `listCategories()`. Month picker: year + month selects (or `<input
   type="month">`). Alert threshold slider (default 80%).
3. **Budgets page** — assemble:
   - Month selector (default: `currentMonth()`)
   - Fetch `listBudgetsByMonth(month)` → renders list of `BudgetCard`s
   - Alert banners at top: CRITICAL first (red), then WARNING (yellow), then OK
     (hidden). Each dismissable.
   - Summary KPIs: Budget total alloué, Total dépensé, Reste global
   - "+ Définir un budget" button → opens `BudgetForm`
4. **Wire to Dashboard** — on Dashboard load, also fetch
   `listBudgetsByMonth(currentMonth())` and show the first CRITICAL/WARNING
   alert in the dashboard `AlertBanner`.

**Gate Checklist:**
- [ ] Budget list renders for current month
- [ ] Progress bars are green/yellow/red based on status
- [ ] Summary KPIs (total allocated, spent, remaining) are correct
- [ ] Can create a new budget (category + month + limit)
- [ ] Alert banners appear for WARNING (80%) and CRITICAL (100%+) budgets
- [ ] Dashboard shows at least the most critical budget alert

---

### ✅ Block F5 — Goals
**Maps to:** Backend Block 5 (Goals)
**Branch:** `feature/frontend-block-5-goals`
**Estimated time:** 3–4 hours

**Goals:**
- Goals list with progress + milestones
- Create goal form
- Add contribution modal

**Step-by-step tasks:**

1. **GoalCard** — implement (§6). Milestones: four dots at 25%, 50%, 75%, 100%
   below the progress bar. Filled yellow when reached.
   Status chip: "EN COURS" (blue), "ATTEINT" (green), "EN RETARD" (red).
2. **GoalForm** — React Hook Form. Fields: Titre, Montant cible (DH), Date
   cible (date picker).
3. **ContributeModal** — amount input + "Ajouter" button (§6).
4. **Goals page** — assemble:
   - Fetch `listGoals()` → for each goal, fetch `getGoalProgress(goal.id)`
   - Render `GoalCard` for each
   - "+ Nouvel objectif" button → opens `GoalForm`
   - For STANDARD users: if already has 1 active goal, show "Limite atteinte
     (Standard : 1 objectif actif)" notice and disable create button
5. **Contribution flow** — GoalCard "Contribuer" button → opens
   `ContributeModal` → calls `goalApi.contribute()` → refreshes progress.

**Gate Checklist:**
- [ ] Goals list renders with progress bars and milestone dots
- [ ] Milestone dots fill when threshold is reached
- [ ] Status chip changes correctly (EN COURS / ATTEINT / EN RETARD)
- [ ] Can create a new goal
- [ ] Contribution modal adds to current amount and refreshes UI
- [ ] STANDARD user blocked from creating second active goal (UI disabled + notice)

---

### ✅ Block F6 — Categories & Export
**Maps to:** Backend Block 3 (Categories) + Block 6 (Export)
**Branch:** `feature/frontend-block-6-categories-export`
**Estimated time:** 3–4 hours

**Goals:**
- Categories management page
- Export page with CSV/Excel download

**Step-by-step tasks:**

1. **CategoryList** — table showing all categories. System categories: read-only
   (no edit/delete buttons, "Système" badge). User categories: editable.
   Color swatch shown in each row.
2. **CategoryForm** — fields: Nom, Type (Dépense/Revenu/Les deux), Couleur
   (color input `<input type="color">`).
3. **Categories page** — STANDARD: shows limit warning ("X/10 catégories
   personnalisées"). "+ Nouvelle catégorie" button.
4. **Export page** — two sections:
   - CSV (Standard + Premium): "Télécharger CSV" yellow button
   - Excel (Premium only): "Télécharger Excel" button, disabled + lock icon
     for STANDARD users with upgrade prompt
   - Both buttons call `exportApi.downloadCsv()` / `downloadExcel()`, then
     trigger browser download:
     ```typescript
     const url = URL.createObjectURL(blob);
     const a = document.createElement('a');
     a.href = url; a.download = 'transactions.csv'; a.click();
     ```
5. **Wire categories to forms** — ensure `TransactionForm` and `BudgetForm`
   `<select>` fields use live data from `listCategories()`.

**Gate Checklist:**
- [ ] System categories are visible but not editable
- [ ] Can create, edit, delete custom categories
- [ ] STANDARD user sees 10-category limit warning
- [ ] CSV download triggers file save
- [ ] Excel download works for PREMIUM, shows locked state for STANDARD
- [ ] Category color dots appear in transaction table and budget cards

---

## 11. Environment Variables

**`.env`** (in project root, git-ignored):

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

**`.env.example`** (committed to git):

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

Access in code: `import.meta.env.VITE_API_BASE_URL`

---

## 12. Development Rules

### DO ✅
- Follow the blocks in order — do not start Block F3 until Block F2 is done
- Always divide API amounts by 100 for display; multiply by 100 before sending
- Use `react-hook-form` for all forms — no manual state for form fields
- Use the `useAuth()` hook for all role/auth checks — never read `localStorage` directly in components
- Handle loading states (spinner or skeleton) and error states on every API call
- Use `formatCurrency()` and `formatDate()` everywhere — no inline formatting
- Keep pages thin — pages fetch data and pass it to components as props

### DON'T ❌
- Do not use Redux, React Query, Zustand, or any external state manager
- Do not add pages or features not listed in this plan
- Do not use `any` in TypeScript — use proper interfaces from `types/index.ts`
- Do not hardcode colors — always use Tailwind tokens defined in `tailwind.config.ts`
- Do not skip the `ConfirmDialog` before destructive actions
- Do not store sensitive data beyond the JWT token in `localStorage`
- Do not implement email/password reset UI flow (it's out of scope)

### Error Handling Pattern

Every API call in a page follows this pattern:

```typescript
const [isLoading, setIsLoading] = useState(true);
const [error, setError] = useState<string | null>(null);

useEffect(() => {
  setIsLoading(true);
  setError(null);
  someApi.call()
    .then(data => setState(data))
    .catch(err => setError(err.response?.data?.error ?? 'Une erreur est survenue'))
    .finally(() => setIsLoading(false));
}, [dependencies]);

if (isLoading) return <div className="text-text-secondary">Chargement...</div>;
if (error)     return <div className="text-danger">{error}</div>;
```

### Git Workflow

```
main
 └── feature/frontend-block-1-auth
 └── feature/frontend-block-2-dashboard
 └── feature/frontend-block-3-transactions
 └── feature/frontend-block-4-budgets
 └── feature/frontend-block-5-goals
 └── feature/frontend-block-6-categories-export
```

Merge each branch to `main` only when its Gate Checklist is fully complete.

---

*End of Frontend Implementation Plan — Portefeuille Intelligent v1.0*
