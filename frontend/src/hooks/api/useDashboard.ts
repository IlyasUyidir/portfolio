import { useQuery } from '../useQuery';
import * as dashboardApi from '../../api/dashboardApi';
import * as transactionApi from '../../api/transactionApi';
import * as budgetApi from '../../api/budgetApi';
import type { DashboardKpis, SpendingCategory, Transaction, BudgetProgress } from '../../types';

interface DashboardData {
  kpis: DashboardKpis | null;
  spending: SpendingCategory[];
  recentTransactions: Transaction[];
  budgetAlerts: BudgetProgress[];
}

export const useDashboard = (month: string) => {
  return useQuery<DashboardData>(async () => {
    const [kpiData, spendingData, txData, budgetsData] = await Promise.all([
      dashboardApi.getKpis(month),
      dashboardApi.getSpending(),
      transactionApi.listTransactions({ page: 0, size: 5 }),
      budgetApi.listBudgetsByMonth(month)
    ]);

    const activeAlerts = budgetsData.filter(b => b.alertStatus === 'CRITICAL' || b.alertStatus === 'WARNING');
    // Sort critical first
    activeAlerts.sort((a, b) => {
      if (a.alertStatus === 'CRITICAL' && b.alertStatus !== 'CRITICAL') return -1;
      if (b.alertStatus === 'CRITICAL' && a.alertStatus !== 'CRITICAL') return 1;
      return 0;
    });

    return {
      kpis: kpiData,
      spending: spendingData,
      recentTransactions: txData.content,
      budgetAlerts: activeAlerts
    };
  }, [month]);
};
