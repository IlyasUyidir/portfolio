import React from 'react';

export interface ProgressBarProps {
  percent: number;          // 0-100
  status?: 'NORMAL' | 'WARNING' | 'CRITICAL';
  showLabel?: boolean;
}

export const ProgressBar: React.FC<ProgressBarProps> = ({ percent, status = 'NORMAL', showLabel = false }) => {
  const cappedPercent = Math.min(100, Math.max(0, percent));
  
  let colorClass = 'bg-primary';
  if (status === 'WARNING') colorClass = 'bg-warning';
  else if (status === 'CRITICAL') colorClass = 'bg-danger';

  return (
    <div className="w-full flex items-center gap-3">
      <div className="flex-1 h-2 bg-bg-input rounded-full overflow-hidden">
        <div 
          className={`h-full rounded-full transition-all duration-500 ${colorClass}`}
          style={{ width: `${cappedPercent}%` }}
        />
      </div>
      {showLabel && (
        <span className={`text-sm font-bold ${status === 'CRITICAL' ? 'text-danger' : status === 'WARNING' ? 'text-warning' : 'text-text-primary'}`}>
          {percent}%
        </span>
      )}
    </div>
  );
};
