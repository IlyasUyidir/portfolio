import React from 'react';
import { Crown } from 'lucide-react';
import { Link } from 'react-router-dom';

export const PremiumUpsellCard: React.FC = () => {
  return (
    <div className="bg-gradient-to-br from-bg-card to-[#1a1500] p-6 rounded-xl border border-primary/30 shadow-[0_0_15px_rgba(245,197,24,0.1)] flex flex-col items-center justify-between h-full min-h-[250px] relative overflow-hidden group">
      {/* Subtle background glow effect */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-32 h-32 bg-primary/10 blur-[50px] rounded-full pointer-events-none" />

      <div className="flex flex-col items-center py-4 w-full">
        <div className="bg-primary/10 p-5 rounded-full mb-5 relative z-10 text-primary">
          <Crown size={38} strokeWidth={2.5} />
        </div>

        <h3 className="text-xl font-bold text-text-primary mb-3 relative z-10 text-center">
          Débloquez le mode sans limite
        </h3>
        
        <p className="text-sm text-text-secondary max-w-[280px] relative z-10 text-center">
          Atteignez tous vos rêves en même temps. Créez des objectifs illimités et suivez votre progression globale.
        </p>
      </div>

      <Link
        to="/dashboard"
        className="w-full py-3 rounded-lg bg-primary text-bg-base font-bold hover:bg-primary-hover transition-colors shadow-[0_0_15px_rgba(245,197,24,0.2)] relative z-10 flex items-center justify-center"
      >
        Passer Premium
      </Link>
    </div>
  );
};