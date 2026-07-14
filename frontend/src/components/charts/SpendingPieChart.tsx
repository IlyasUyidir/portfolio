import React from 'react';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from 'recharts';
import type { SpendingCategory } from '../../types';
import { formatCurrency } from '../../utils/formatCurrency';

interface SpendingPieChartProps {
  data: SpendingCategory[];
}

const CustomTooltip = ({ active, payload }: { active?: boolean; payload?: { name: string; value: number }[] }) => {
  if (active && payload && payload.length) {
    return (
      <div className="bg-bg-card border border-border-subtle p-3 rounded-lg shadow-xl">
        <p className="text-text-primary font-medium">{payload[0].name}</p>
        <p className="text-danger font-bold">
          {formatCurrency(payload[0].value * 100)}
        </p>
      </div>
    );
  }
  return null;
};

export const SpendingPieChart: React.FC<SpendingPieChartProps> = ({ data }) => {
  const chartData = data.map(item => ({
    name: item.category.name,
    value: item.totalAmount / 100, // Recharts works better with actual numbers
    color: item.category.color,
  }));

  if (!data || data.length === 0) {
    return (
      <div className="h-full flex items-center justify-center text-text-secondary">
        Aucune donnée
      </div>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={300}>
      <PieChart>
        <Pie
          data={chartData}
          cx="50%"
          cy="50%"
          innerRadius={60}
          outerRadius={100}
          paddingAngle={2}
          dataKey="value"
          stroke="none"
        >
          {chartData.map((entry, index) => (
            <Cell key={`cell-${index}`} fill={entry.color} />
          ))}
        </Pie>
        <Tooltip content={<CustomTooltip />} />
      </PieChart>
    </ResponsiveContainer>
  );
};
