import React from 'react';
import type { LucideIcon } from 'lucide-react';

interface KpiCardProps {
  label: string;
  value: string;
  valueColor?: 'success' | 'danger' | 'default';
  icon: LucideIcon;
  trend?: string;
}

export const KpiCard: React.FC<KpiCardProps> = ({ label, value, valueColor = 'default', icon: Icon, trend }) => {
  const colorClass = {
    success: 'text-success',
    danger: 'text-danger',
    default: 'text-text-primary',
  }[valueColor];

  return (
    <div className="bg-bg-card border border-border-subtle rounded-2xl p-6 flex flex-col justify-between">
      <div className="flex justify-between items-start mb-4">
        <h3 className="text-text-secondary text-sm font-medium uppercase tracking-wider">{label}</h3>
        <div className="bg-bg-input p-2 rounded-lg">
          <Icon className="w-5 h-5 text-primary" />
        </div>
      </div>
      <div>
        <p className={`text-3xl font-bold ${colorClass}`}>{value}</p>
        {trend && (
          <p className={`text-sm mt-2 ${trend.startsWith('+') ? 'text-success' : trend.startsWith('-') ? 'text-danger' : 'text-text-secondary'}`}>
            {trend}
          </p>
        )}
      </div>
    </div>
  );
};
