import { Outlet, Navigate } from 'react-router';
import { Sidebar } from '../components/Sidebar';
import { Topbar } from '../components/Topbar';
import { useAuth } from '../contexts/AuthContext';
import './ClientLayout.css';

export function ClientLayout() {
  const { isAuthenticated, role } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (role !== 'USER') {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar-logo">
          <div className="sidebar-logo-icon">U</div>
          <span className="sidebar-logo-text">Undangan</span>
        </div>
        <Sidebar role="USER" />
        <div className="sidebar-footer" />
      </aside>
      <div className="main-content">
        <Topbar />
        <div className="content-wrapper">
          <Outlet />
        </div>
      </div>
    </div>
  );
}
