import React from 'react';

interface TopBarProps {
  title: string;
  action?: {
    label: string;
    onClick: () => void;
  };
  actionDisabled?: boolean;
}

export const TopBar: React.FC<TopBarProps> = ({ title, action, actionDisabled }) => {
  const words = title.split(' ');
  const lastWord = words.pop();

  return (
    <header className="h-20 px-8 flex items-center justify-between border-b border-border-subtle bg-bg-base sticky top-0 z-10">
      <h2 className="text-2xl font-bold text-text-primary">
        {words.join(' ')}{' '}
        {lastWord && <span className="text-primary">{lastWord}</span>}
      </h2>
      
      {action && (
        <button
          onClick={action.onClick}
          disabled={actionDisabled}
          className="bg-primary hover:bg-primary-hover text-bg-base font-bold py-2 px-4 rounded-lg transition-colors flex items-center disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          {action.label}
        </button>
      )}
    </header>
  );
};
