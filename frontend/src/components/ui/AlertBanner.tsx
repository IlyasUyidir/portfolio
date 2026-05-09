import React from 'react';
import { AlertTriangle, X } from 'lucide-react';

interface AlertBannerProps {
  message: string;
  severity: 'warning' | 'critical';
  onDismiss: () => void;
}

export const AlertBanner: React.FC<AlertBannerProps> = ({ message, severity, onDismiss }) => {
  const isCritical = severity === 'critical';
  
  return (
    <div className={`flex items-center justify-between p-4 rounded-xl border mb-6 ${
      isCritical 
        ? 'bg-danger/10 border-danger/20 text-danger' 
        : 'bg-warning/10 border-warning/20 text-warning'
    }`}>
      <div className="flex items-center gap-3">
        <AlertTriangle className={`w-5 h-5 ${isCritical ? 'text-danger' : 'text-warning'}`} />
        <p className="font-medium text-sm md:text-base">{message}</p>
      </div>
      <button 
        onClick={onDismiss}
        className={`p-1.5 rounded-md transition-colors ${
          isCritical ? 'hover:bg-danger/20 text-danger' : 'hover:bg-warning/20 text-warning'
        }`}
      >
        <X className="w-4 h-4" />
      </button>
    </div>
  );
};
