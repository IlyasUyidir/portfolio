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
  budget: Budget;
  spentAmount: number;
  remainingAmount: number;
  spentPercentage: number;
  alertStatus: 'NORMAL' | 'WARNING' | 'CRITICAL';
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
  month?: string;           // "2026-06" (optional since backend doesn't send it)
  monthlyBalance: number;   // centimes
  totalIncome: number;      // centimes
  totalExpenses: number;    // centimes
  savingsRate: number;      // percentage (e.g. 35.4)
  revenueVsPreviousMonth?: number;   // % change (optional, missing in backend)
  expensesVsPreviousMonth?: number;  // % change (optional, missing in backend)
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
