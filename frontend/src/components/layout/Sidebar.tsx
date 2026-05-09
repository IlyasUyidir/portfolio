import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, ArrowLeftRight, PieChart, Target, Tag, BarChart2, Download } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { PremiumBadge } from '../ui/PremiumBadge';

export const Sidebar: React.FC = () => {
  const { user, isPremium } = useAuth();

  const navItems = [
    { label: 'Tableau de bord', icon: LayoutDashboard, route: '/dashboard' },
    { label: 'Transactions', icon: ArrowLeftRight, route: '/transactions' },
    { label: 'Budgets', icon: PieChart, route: '/budgets' },
    { label: 'Objectifs', icon: Target, route: '/goals' },
    { label: 'Catégories', icon: Tag, route: '/categories' },
    { label: 'Statistiques', icon: BarChart2, route: '/statistics', premiumOnly: true },
    { label: 'Export / Import', icon: Download, route: '/export' },
  ];

  return (
    <aside className="w-[220px] bg-bg-sidebar h-full flex flex-col border-r border-border-subtle fixed left-0 top-0">
      <div className="p-6">
        <h1 className="text-xl font-bold text-text-primary">
          Portefeuille
        </h1>
      </div>

      <nav className="flex-1 px-4 space-y-1 overflow-y-auto">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isDisabled = item.premiumOnly && !isPremium;

          return (
            <NavLink
              key={item.route}
              to={isDisabled ? '#' : item.route}
              className={({ isActive }) =>
                `flex items-center px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${isDisabled
                  ? 'opacity-50 cursor-not-allowed text-text-secondary'
                  : isActive
                    ? 'bg-bg-input text-primary'
                    : 'text-text-secondary hover:bg-bg-card hover:text-text-primary'
                }`
              }
              onClick={(e) => isDisabled && e.preventDefault()}
            >
              <Icon className="w-5 h-5 mr-3 flex-shrink-0" />
              {item.label}
              {item.premiumOnly && !isPremium && <PremiumBadge />}
            </NavLink>
          );
        })}
      </nav>

      <div className="p-4 border-t border-border-subtle">
        <div className="flex items-center group cursor-pointer hover:bg-bg-card p-2 rounded-lg transition-colors">
          <div className="w-10 h-10 rounded-full bg-bg-input flex items-center justify-center text-primary font-bold mr-3 flex-shrink-0">
            {user?.username.charAt(0).toUpperCase() || 'U'}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-text-primary truncate">
              {user?.username}
            </p>
            <p className="text-xs text-text-secondary truncate">
              {user?.role}
            </p>
          </div>
        </div>
      </div>
    </aside>
  );
};
