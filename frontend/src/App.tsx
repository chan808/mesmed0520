import { Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from './shared/components/Layout';
import { ProtectedRoute } from './shared/components/ProtectedRoute';
import { LoginPage } from './features/auth/LoginPage';
import { DashboardPage } from './features/dashboard/DashboardPage';
import { MaterialListPage } from './features/material/MaterialListPage';
import { MaterialCreatePage } from './features/material/MaterialCreatePage';
import { MaterialDetailPage } from './features/material/MaterialDetailPage';

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route path="/" element={<Navigate to="/materials" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />

        <Route path="/materials" element={<MaterialListPage />} />
        <Route path="/materials/new" element={<MaterialCreatePage />} />
        <Route path="/materials/:id" element={<MaterialDetailPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/materials" replace />} />
    </Routes>
  );
}
