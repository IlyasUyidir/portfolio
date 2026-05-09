import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Wallet, TrendingUp, TrendingDown, PiggyBank, ArrowRight, ArrowUpRight, ArrowDownRight } from 'lucide-react';
import { TopBar } from '../components/layout/TopBar';
import { KpiCard } from '../components/ui/KpiCard';
import { Badge } from '../components/ui/Badge';
import { PremiumBadge } from '../components/ui/PremiumBadge';
import { SpendingPieChart } from '../components/charts/SpendingPieChart';
import { RevenueExpensesBar } from '../components/charts/RevenueExpensesBar';
import { currentMonth, formatDate, formatCurrency } from '../utils';
import { useAuth } from '../hooks/useAuth';
import * as dashboardApi from '../api/dashboardApi';
import * as transactionApi from '../api/transactionApi';
import type { DashboardKpis, SpendingCategory, Transaction } from '../types';

export const Dashboard: React.FC = () => {
  const { isPremium } = useAuth();
  const [month, setMonth] = useState(currentMonth());
  
  const [kpis, setKpis] = useState<DashboardKpis | null>(null);
  const [spending, setSpending] = useState<SpendingCategory[]>([]);
  const [recentTransactions, setRecentTransactions] = useState<Transaction[]>([]);
  
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchDashboardData = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const [kpiData, spendingData, txData] = await Promise.all([
          dashboardApi.getKpis(month),
          dashboardApi.getSpending(),
          transactionApi.listTransactions({ page: 0, size: 5 })
        ]);
        
        setKpis(kpiData);
        setSpending(spendingData);
        setRecentTransactions(txData.content);
      } catch (err: any) {
        setError(err.response?.data?.error || 'Erreur lors du chargement des données');
      } finally {
        setIsLoading(false);
      }
    };

    fetchDashboardData();
  }, [month]);

  // Mock historical data for the bar chart
  const mockedHistoricalData = [
    { month: 'Jan', revenue: 1500000, expenses: 1200000 },
    { month: 'Fév', revenue: 1600000, expenses: 1300000 },
    { month: 'Mar', revenue: 1550000, expenses: 1400000 },
    { month: 'Avr', revenue: 1700000, expenses: 1100000 },
    { month: 'Mai', revenue: 1650000, expenses: 1250000 },
    { 
      month: 'Juin', 
      revenue: kpis?.totalRevenue || 0, 
      expenses: kpis?.totalExpenses || 0 
    },
  ];

  const formatTrend = (value: number) => {
    if (value > 0) return `+${value}% vs mois préc.`;
    if (value < 0) return `${value}% vs mois préc.`;
    return '0% vs mois préc.';
  };

  return (
    <div className="min-h-full pb-10">
      <TopBar title="Tableau de bord" />

      <div className="p-8 max-w-7xl mx-auto space-y-8">
        
        {/* Month Selector & Header */}
        <div className="flex justify-between items-center">
          <h2 className="text-xl font-bold text-text-primary">Aperçu financier</h2>
          <input 
            type="month" 
            value={month}
            onChange={(e) => setMonth(e.target.value)}
            className="bg-bg-input border border-border-subtle rounded-lg px-4 py-2 text-text-primary focus:outline-none focus:border-primary"
          />
        </div>

        {isLoading ? (
          <div className="text-text-secondary py-10 text-center">Chargement...</div>
        ) : error ? (
          <div className="text-danger bg-danger/10 p-4 rounded-lg">{error}</div>
        ) : (
          <>
            {/* KPI Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              <KpiCard 
                label="Solde du mois" 
                value={formatCurrency(kpis?.monthlyBalance || 0, true)} 
                icon={Wallet} 
                valueColor={(kpis?.monthlyBalance || 0) >= 0 ? 'success' : 'danger'}
              />
              <KpiCard 
                label="Revenus" 
                value={formatCurrency(kpis?.totalRevenue || 0)} 
                icon={TrendingUp} 
                trend={formatTrend(kpis?.revenueVsPreviousMonth || 0)}
              />
              <KpiCard 
                label="Dépenses" 
                value={formatCurrency(kpis?.totalExpenses || 0)} 
                icon={TrendingDown} 
                trend={formatTrend(kpis?.expensesVsPreviousMonth || 0)}
              />
              <KpiCard 
                label="Taux d'épargne" 
                value={`${new Intl.NumberFormat('fr-MA', { maximumFractionDigits: 1 }).format(kpis?.savingsRate || 0)} %`} 
                icon={PiggyBank} 
              />
            </div>

            {/* Charts Section */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <div className="bg-bg-card border border-border-subtle rounded-2xl p-6">
                <h3 className="text-lg font-bold text-text-primary mb-6">Dépenses par catégorie</h3>
                <SpendingPieChart data={spending} />
              </div>

              <div className="bg-bg-card border border-border-subtle rounded-2xl p-6 flex flex-col relative overflow-hidden">
                <h3 className="text-lg font-bold text-text-primary mb-6">Évolution des flux</h3>
                <RevenueExpensesBar data={mockedHistoricalData} />
                
                {!isPremium && (
                  <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-bg-card to-bg-card/90 border-t border-border-subtle p-4 flex items-center justify-between backdrop-blur-sm">
                    <div>
                      <p className="text-text-primary font-medium flex items-center">
                        Débloquez l'historique complet <PremiumBadge />
                      </p>
                      <p className="text-text-secondary text-sm">Passez à la version Premium pour voir plus de 6 mois.</p>
                    </div>
                    <button className="bg-primary hover:bg-primary-hover text-bg-base text-sm font-bold py-2 px-4 rounded-lg transition-colors">
                      Mettre à niveau
                    </button>
                  </div>
                )}
              </div>
            </div>

            {/* Recent Transactions */}
            <div className="bg-bg-card border border-border-subtle rounded-2xl p-6">
              <div className="flex justify-between items-center mb-6">
                <h3 className="text-lg font-bold text-text-primary">Transactions récentes</h3>
                <Link to="/transactions" className="text-primary hover:text-primary-hover text-sm font-medium flex items-center">
                  Voir tout <ArrowRight className="w-4 h-4 ml-1" />
                </Link>
              </div>

              {recentTransactions.length === 0 ? (
                <p className="text-text-secondary text-center py-4">Aucune transaction récente.</p>
              ) : (
                <div className="space-y-4">
                  {recentTransactions.map((tx) => (
                    <div key={tx.id} className="flex items-center justify-between p-4 rounded-xl bg-bg-base border border-border-subtle hover:border-primary/50 transition-colors">
                      <div className="flex items-center gap-4">
                        <div className={`p-2 rounded-lg ${tx.type === 'REVENU' ? 'bg-success/10 text-success' : 'bg-danger/10 text-danger'}`}>
                          {tx.type === 'REVENU' ? <ArrowUpRight className="w-5 h-5" /> : <ArrowDownRight className="w-5 h-5" />}
                        </div>
                        <div>
                          <p className="text-text-primary font-medium">{tx.title}</p>
                          <p className="text-text-secondary text-sm">{formatDate(tx.txDate)}</p>
                        </div>
                      </div>
                      <div className="flex items-center gap-6">
                        <Badge label={tx.category.name} color={tx.category.color} />
                        <p className={`font-bold ${tx.type === 'REVENU' ? 'text-success' : 'text-danger'}`}>
                          {tx.type === 'REVENU' ? '+' : '-'}{formatCurrency(tx.amount)}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
};
