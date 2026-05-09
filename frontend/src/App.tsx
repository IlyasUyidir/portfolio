import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { ForgotPassword } from './pages/ForgotPassword';
import { Dashboard } from './pages/Dashboard';
import { Transactions } from './pages/Transactions';
import { TransactionDetailPage } from './pages/TransactionDetailPage';
import { Budgets } from './pages/Budgets';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />

          {/* Protected */}
          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<Dashboard />} />
            
            {/* Future routes will go here and automatically get the AppShell layout */}
            <Route path="/transactions" element={<Transactions />} />
            <Route path="/transactions/:id" element={<TransactionDetailPage />} />
            <Route path="/budgets" element={<Budgets />} />
            <Route path="/goals" element={<div className="p-8">Objectifs Placeholder</div>} />
            <Route path="/categories" element={<div className="p-8">Catégories Placeholder</div>} />
            <Route path="/statistics" element={<div className="p-8">Statistiques Placeholder</div>} />
            <Route path="/export" element={<div className="p-8">Export Placeholder</div>} />
          </Route>

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
