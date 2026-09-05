import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { AuthModal } from './AuthModal';
import { Flame, Ticket, Shield, LogOut, User as UserIcon } from 'lucide-react';

interface NavbarProps {
  currentPath: string;
  navigate: (path: string) => void;
}

export const Navbar: React.FC<NavbarProps> = ({ currentPath, navigate }) => {
  const { user, isAuthenticated, isAdmin, logout } = useAuth();
  const [authModalOpen, setAuthModalOpen] = useState(false);

  return (
    <>
      <header
        style={{
          borderBottom: '1px solid var(--border-color)',
          backgroundColor: 'rgba(17, 24, 39, 0.8)',
          backdropFilter: 'blur(12px)',
          position: 'sticky',
          top: 0,
          zIndex: 40,
        }}
      >
        <div
          className="container"
          style={{
            height: '70px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          {/* Brand */}
          <div
            onClick={() => navigate('/')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              cursor: 'pointer',
              userSelect: 'none',
            }}
          >
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '10px',
                background: 'linear-gradient(135deg, #FF4D4D, #F59E0B)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: '0 0 15px var(--primary-glow)',
              }}
            >
              <Flame size={22} color="white" />
            </div>
            <div>
              <span style={{ fontSize: '18px', fontWeight: 800, letterSpacing: '-0.02em', color: '#fff' }}>
                HOT<span style={{ color: 'var(--primary)' }}>DROP</span>
              </span>
              <span
                style={{
                  display: 'block',
                  fontSize: '10px',
                  color: 'var(--text-muted)',
                  letterSpacing: '0.05em',
                  fontWeight: 600,
                  lineHeight: 1,
                }}
              >
                FLASH SALE PLATFORM
              </span>
            </div>
          </div>

          {/* Nav links */}
          <nav style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <button
              onClick={() => navigate('/')}
              className="btn"
              style={{
                color: currentPath === '/' ? 'var(--primary)' : 'var(--text-main)',
                backgroundColor: currentPath === '/' ? 'rgba(255, 77, 77, 0.1)' : 'transparent',
              }}
            >
              <Flame size={16} /> Events
            </button>

            {isAuthenticated && (
              <button
                onClick={() => navigate('/my-bookings')}
                className="btn"
                style={{
                  color: currentPath === '/my-bookings' ? 'var(--primary)' : 'var(--text-main)',
                  backgroundColor: currentPath === '/my-bookings' ? 'rgba(255, 77, 77, 0.1)' : 'transparent',
                }}
              >
                <Ticket size={16} /> My Bookings
              </button>
            )}

            {isAdmin && (
              <button
                onClick={() => navigate('/admin-dashboard')}
                className="btn"
                style={{
                  color: currentPath === '/admin-dashboard' ? 'var(--primary)' : 'var(--text-main)',
                  backgroundColor: currentPath === '/admin-dashboard' ? 'rgba(255, 77, 77, 0.1)' : 'transparent',
                }}
              >
                <Shield size={16} /> Admin Portal
              </button>
            )}
          </nav>

          {/* User actions */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            {isAuthenticated && user ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-main)' }}>
                    {user.name}
                  </div>
                  <div style={{ display: 'flex', gap: '4px', justifyContent: 'flex-end' }}>
                    <span
                      className="badge"
                      style={{
                        padding: '1px 6px',
                        fontSize: '9px',
                        backgroundColor: isAdmin ? 'rgba(239, 68, 68, 0.2)' : 'rgba(59, 130, 246, 0.2)',
                        color: isAdmin ? '#F87171' : '#60A5FA',
                        border: 'none',
                      }}
                    >
                      {user.role}
                    </span>
                  </div>
                </div>

                <button
                  onClick={logout}
                  className="btn btn-secondary"
                  title="Log out"
                  style={{ padding: '8px', borderRadius: '8px' }}
                >
                  <LogOut size={16} />
                </button>
              </div>
            ) : (
              <button
                onClick={() => setAuthModalOpen(true)}
                className="btn btn-primary"
              >
                <UserIcon size={16} /> Sign In
              </button>
            )}
          </div>
        </div>
      </header>

      <AuthModal isOpen={authModalOpen} onClose={() => setAuthModalOpen(false)} />
    </>
  );
};
