import React from 'react';

interface BadgeProps {
  label: string;
  color: string;
}

export const Badge: React.FC<BadgeProps> = ({ label, color }) => {
  return (
    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-bg-input border border-border-subtle text-text-primary">
      <span
        className="w-2 h-2 rounded-full mr-2"
        style={{ backgroundColor: color }}
      />
      {label}
    </span>
  );
};
