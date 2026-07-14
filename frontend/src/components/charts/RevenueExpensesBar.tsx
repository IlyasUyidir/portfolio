import React from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { formatCurrency } from '../../utils/formatCurrency';

interface RevenueExpensesBarProps {
  data: {
    month: string;
    revenue: number;
    expenses: number;
  }[];
}

const CustomTooltip = ({ active, payload, label }: { active?: boolean; payload?: { name: string; value: number; color: string }[]; label?: string }) => {
  if (active && payload && payload.length) {
    return (
      <div className="bg-bg-card border border-border-subtle p-3 rounded-lg shadow-xl">
        <p className="text-text-primary font-medium mb-2">{label}</p>
        {payload.map((entry, index) => (
          <p key={index} style={{ color: entry.color }} className="font-bold text-sm">
            {entry.name} : {formatCurrency(entry.value * 100)}
          </p>
        ))}
      </div>
    );
  }
  return null;
};

export const RevenueExpensesBar: React.FC<RevenueExpensesBarProps> = ({ data }) => {
  const chartData = data.map(item => ({
    name: item.month,
    Revenus: item.revenue / 100,
    Dépenses: item.expenses / 100,
  }));

  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={chartData} margin={{ top: 10, right: 10, left: 10, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#2A2D3E" />
        <XAxis 
          dataKey="name" 
          axisLine={false} 
          tickLine={false} 
          tick={{ fill: '#9CA3AF', fontSize: 12 }} 
          dy={10}
        />
        <YAxis 
          axisLine={false} 
          tickLine={false} 
          tick={{ fill: '#9CA3AF', fontSize: 12 }}
          tickFormatter={(value) => `${value}`}
        />
        <Tooltip content={<CustomTooltip />} cursor={{ fill: '#252836' }} />
        <Legend iconType="circle" wrapperStyle={{ paddingTop: '20px' }} />
        <Bar dataKey="Revenus" fill="#22C55E" radius={[4, 4, 0, 0]} barSize={32} />
        <Bar dataKey="Dépenses" fill="#EF4444" radius={[4, 4, 0, 0]} barSize={32} />
      </BarChart>
    </ResponsiveContainer>
  );
};
