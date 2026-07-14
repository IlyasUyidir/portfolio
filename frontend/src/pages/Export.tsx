import React, { useState } from 'react';
import { Download, FileText, Table, Lock } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { downloadCsv, downloadExcel } from '../api/exportApi';
import { TopBar } from '../components/layout/TopBar';
import { AlertBanner } from '../components/ui/AlertBanner';

export const Export: React.FC = () => {
  const { isPremium } = useAuth();
  const [isCsvDownloading, setIsCsvDownloading] = useState(false);
  const [isExcelDownloading, setIsExcelDownloading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleDownloadCsv = async () => {
    setIsCsvDownloading(true);
    setError(null);
    try {
      const blob = await downloadCsv();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'transactions.csv';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err: any) {
      setError("Erreur lors du téléchargement du fichier CSV");
    } finally {
      setIsCsvDownloading(false);
    }
  };

  const handleDownloadExcel = async () => {
    if (!isPremium) return;
    setIsExcelDownloading(true);
    setError(null);
    try {
      const blob = await downloadExcel();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'transactions.xlsx';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err: any) {
      setError("Erreur lors du téléchargement du fichier Excel");
    } finally {
      setIsExcelDownloading(false);
    }
  };

  return (
    <div className="flex-1 flex flex-col h-full bg-bg-base">
      <TopBar title="Export des données" />

      <div className="flex-1 p-8 overflow-y-auto">
        <div className="max-w-4xl mx-auto space-y-8">
          
          <div className="mb-8">
            <h1 className="text-3xl font-bold text-text-primary mb-2">Exportez vos transactions</h1>
            <p className="text-text-secondary">
              Téléchargez l'historique complet de vos transactions pour vos propres archives ou pour les analyser dans d'autres outils.
            </p>
          </div>

          {error && (
            <AlertBanner severity="critical" message={error} onDismiss={() => setError(null)} />
          )}

          <div className="grid md:grid-cols-2 gap-6">
            
            {/* CSV Export Card */}
            <div className="bg-bg-card border border-border-subtle rounded-2xl p-8 flex flex-col items-center text-center hover:border-primary/50 transition-colors">
              <div className="w-16 h-16 bg-bg-input rounded-2xl flex items-center justify-center mb-6">
                <FileText className="w-8 h-8 text-primary" />
              </div>
              <h3 className="text-xl font-bold text-text-primary mb-3">Format CSV</h3>
              <p className="text-text-secondary mb-8 flex-1">
                Format universel compatible avec tous les tableurs. Idéal pour un import vers d'autres logiciels.
              </p>
              <button
                onClick={handleDownloadCsv}
                disabled={isCsvDownloading}
                className="w-full flex items-center justify-center py-3 px-4 bg-bg-input hover:bg-border-subtle text-text-primary font-bold rounded-xl transition-colors disabled:opacity-50"
              >
                <Download className="w-5 h-5 mr-2" />
                {isCsvDownloading ? 'Téléchargement...' : 'Télécharger CSV'}
              </button>
            </div>

            {/* Excel Export Card */}
            <div className={`bg-bg-card border border-border-subtle rounded-2xl p-8 flex flex-col items-center text-center transition-colors relative overflow-hidden ${!isPremium ? 'opacity-90 grayscale-[0.2]' : 'hover:border-primary/50'}`}>
              {!isPremium && (
                 <div className="absolute inset-0 bg-bg-base/40 backdrop-blur-[2px] z-10 flex flex-col items-center justify-center p-6">
                    <Lock className="w-12 h-12 text-primary mb-4" />
                    <h4 className="text-xl font-bold text-text-primary mb-2">Fonctionnalité Premium</h4>
                    <p className="text-text-secondary text-sm mb-4">Passez à Premium pour exporter vos données au format Excel formaté.</p>
                 </div>
              )}

              <div className="w-16 h-16 bg-bg-input rounded-2xl flex items-center justify-center mb-6">
                <Table className="w-8 h-8 text-success" />
              </div>
              <h3 className="text-xl font-bold text-text-primary mb-3">Format Excel</h3>
              <p className="text-text-secondary mb-8 flex-1">
                Fichier tableur formaté (.xlsx) prêt à l'emploi avec colonnes typées pour une lecture optimale.
              </p>
              <button
                onClick={handleDownloadExcel}
                disabled={isExcelDownloading || !isPremium}
                className="w-full flex items-center justify-center py-3 px-4 bg-primary hover:bg-primary-hover text-bg-base font-bold rounded-xl transition-colors disabled:opacity-50"
              >
                <Download className="w-5 h-5 mr-2" />
                {isExcelDownloading ? 'Téléchargement...' : 'Télécharger Excel'}
              </button>
            </div>

          </div>
        </div>
      </div>
    </div>
  );
};
