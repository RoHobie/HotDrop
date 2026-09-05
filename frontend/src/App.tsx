import React, { useState, useEffect } from 'react';
import { AuthProvider } from './context/AuthContext';
import { Navbar } from './components/Navbar';
import { EventCatalogPage } from './pages/EventCatalogPage';
import { WaitingRoomPage } from './pages/WaitingRoomPage';
import { QueuePage } from './pages/QueuePage';
import { CheckoutPage } from './pages/CheckoutPage';
import { MyBookingsPage } from './pages/MyBookingsPage';
import { AdminDashboardPage } from './pages/AdminDashboardPage';

export const App: React.FC = () => {
  const [currentPath, setCurrentPath] = useState<string>(window.location.pathname);

  useEffect(() => {
    const handlePopState = () => {
      setCurrentPath(window.location.pathname);
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const navigate = (path: string) => {
    window.history.pushState({}, '', path);
    setCurrentPath(path);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const renderRoute = () => {
    // 1. Waiting room: /waiting-room/:id
    const wrMatch = currentPath.match(/^\/waiting-room\/(\d+)$/);
    if (wrMatch) {
      return <WaitingRoomPage eventId={parseInt(wrMatch[1], 10)} navigate={navigate} />;
    }

    // 2. Queue: /queue/:id
    const queueMatch = currentPath.match(/^\/queue\/(\d+)$/);
    if (queueMatch) {
      return <QueuePage eventId={parseInt(queueMatch[1], 10)} navigate={navigate} />;
    }

    // 3. Checkout: /checkout/:id
    const checkoutMatch = currentPath.match(/^\/checkout\/(\d+)$/);
    if (checkoutMatch) {
      return <CheckoutPage eventId={parseInt(checkoutMatch[1], 10)} navigate={navigate} />;
    }

    // 4. My Bookings: /my-bookings
    if (currentPath === '/my-bookings') {
      return <MyBookingsPage navigate={navigate} />;
    }

    // 5. Admin Dashboard: /admin-dashboard
    if (currentPath === '/admin-dashboard') {
      return <AdminDashboardPage navigate={navigate} />;
    }

    // Default: Event Catalog
    return <EventCatalogPage navigate={navigate} />;
  };

  return (
    <AuthProvider>
      <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
        <Navbar currentPath={currentPath} navigate={navigate} />
        <main style={{ flex: 1 }}>{renderRoute()}</main>
        <footer
          style={{
            borderTop: '1px solid var(--border-color)',
            padding: '24px 0',
            textAlign: 'center',
            fontSize: '13px',
            color: 'var(--text-muted)',
            marginTop: 'auto',
          }}
        >
          <div className="container">
            HotDrop High-Concurrency Platform • Powered by PostgreSQL Row Locks & Fair Waiting Rooms
          </div>
        </footer>
      </div>
    </AuthProvider>
  );
};
