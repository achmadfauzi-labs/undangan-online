import { useNavigate } from 'react-router';
import { useAuth } from '../contexts/AuthContext';
import { logout } from '../api/authApi';
import './Topbar.css';

function getInitials(name) {
  if (!name) return '?';
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
  return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
}

function getRoleLabel(role) {
  if (role === 'ADMIN') return 'Administrator';
  if (role === 'USER') return 'Client';
  return role || '';
}

export function Topbar({ title }) {
  const { user, logout: authLogout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    // localStorage cleanup
    authLogout();
    navigate('/login');
  };

  return (
    <header className="topbar">
      <div className="topbar-left">
        <h1 className="topbar-title">{title || 'Dashboard'}</h1>
      </div>
      <div className="topbar-right">
        <div className="topbar-user">
          <div className="topbar-avatar">{getInitials(user?.name)}</div>
          <div className="topbar-user-info">
            <span className="topbar-user-name">{user?.name || 'User'}</span>
            <span className="topbar-user-role">{getRoleLabel(user?.roleCode)}</span>
          </div>
        </div>
        <button className="btn btn-ghost" onClick={handleLogout} title="Logout">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
            <polyline points="16 17 21 12 16 7" />
            <line x1="21" y1="12" x2="9" y2="12" />
          </svg>
        </button>
      </div>
    </header>
  );
}
