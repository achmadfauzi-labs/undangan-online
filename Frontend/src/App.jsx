import { BrowserRouter, Routes, Route, Navigate } from 'react-router';
import { AuthProvider } from './contexts/AuthContext';
import { ApiProvider } from './contexts/ApiContext';

// Layouts
import { AdminLayout } from './layouts/AdminLayout';
import { ClientLayout } from './layouts/ClientLayout';

// Pages
import { LoginPage } from './pages/LoginPage';
import { AdminDashboardPage } from './pages/admin/DashboardPage';
import { ClientsPage } from './pages/admin/ClientsPage';
import { ClientDashboardPage } from './pages/client/DashboardPage';
import { ClientInvitationPage } from './pages/client/InvitationPage';
import { PublicInvitationPage } from './pages/public/InvitationPage';

function App() {
  return (
    <BrowserRouter>
      <ApiProvider>
        <AuthProvider>
          <Routes>
            {/* Public */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/undangan/:slug" element={<PublicInvitationPage />} />

            {/* Admin routes — wrapped in AdminLayout */}
            <Route element={<AdminLayout />}>
              <Route index element={<Navigate to="/admin/dashboard" replace />} />
              <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
              <Route path="/admin/clients" element={<ClientsPage />} />
            </Route>

            {/* Client routes — wrapped in ClientLayout */}
            <Route element={<ClientLayout />}>
              <Route index element={<Navigate to="/client/dashboard" replace />} />
              <Route path="/client/dashboard" element={<ClientDashboardPage />} />
              <Route path="/client/invitation" element={<ClientInvitationPage />} />
            </Route>

            {/* Catch-all redirect */}
            <Route path="*" element={<Navigate to="/login" replace />} />
          </Routes>
        </AuthProvider>
      </ApiProvider>
    </BrowserRouter>
  );
}

export default App;
