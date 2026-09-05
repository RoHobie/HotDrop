import React, { useState, useEffect } from 'react';
import { Booking } from '../types';
import { api } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { Ticket, Calendar, QrCode, ArrowRight, ArrowLeft } from 'lucide-react';

interface MyBookingsPageProps {
  navigate: (path: string) => void;
}

export const MyBookingsPage: React.FC<MyBookingsPageProps> = ({ navigate }) => {
  const { isAuthenticated, user } = useAuth();
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isAuthenticated) return;

    api.getMyBookings()
      .then(setBookings)
      .catch((err) => setError(err.message || 'Failed to load bookings.'))
      .finally(() => setLoading(false));
  }, [isAuthenticated]);

  if (!isAuthenticated) {
    return (
      <div className="container" style={{ padding: '60px 24px', maxWidth: '560px', textAlign: 'center' }}>
        <div className="card" style={{ padding: '40px' }}>
          <Ticket size={48} color="var(--primary)" style={{ margin: '0 auto 16px' }} />
          <h2 style={{ fontSize: '24px', fontWeight: 700, marginBottom: '12px' }}>Authentication Required</h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '24px', fontSize: '15px' }}>
            Please sign in to view your secured HotDrop tickets and passes.
          </p>
          <button onClick={() => navigate('/')} className="btn btn-secondary">
            <ArrowLeft size={16} /> Return to Events
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="container" style={{ padding: '40px 24px' }}>
      <div style={{ marginBottom: '32px' }}>
        <h1 style={{ fontSize: '32px', fontWeight: 800, marginBottom: '8px' }}>My Confirmed Bookings</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '15px' }}>
          All verified flash drop tickets secured by your account ({user?.email}).
        </p>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--text-muted)' }}>
          Retrieving ticket passes...
        </div>
      ) : error ? (
        <div className="card" style={{ padding: '24px', color: 'var(--danger)' }}>
          {error}
        </div>
      ) : bookings.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '60px 24px' }}>
          <Ticket size={48} color="var(--text-muted)" style={{ margin: '0 auto 16px' }} />
          <h2 style={{ fontSize: '20px', fontWeight: 700, marginBottom: '8px' }}>No Tickets Secured Yet</h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '24px', maxWidth: '400px', margin: '0 auto 24px' }}>
            Join an upcoming waiting room or enter an active drop queue to claim tickets.
          </p>
          <button onClick={() => navigate('/')} className="btn btn-primary" style={{ margin: '0 auto' }}>
            Browse Live Drops <ArrowRight size={16} />
          </button>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))', gap: '24px' }}>
          {bookings.map((booking) => (
            <div
              key={booking.bookingId}
              className="card"
              style={{
                position: 'relative',
                overflow: 'hidden',
                background: 'linear-gradient(145deg, var(--bg-card), var(--bg-secondary))',
                border: '1px solid var(--border-color)',
              }}
            >
              <div
                style={{
                  position: 'absolute',
                  top: 0,
                  left: 0,
                  right: 0,
                  height: '4px',
                  background: 'linear-gradient(90deg, var(--primary), var(--accent))',
                }}
              />

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '16px' }}>
                <span className="badge badge-admitted">{booking.status}</span>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: '12px', color: 'var(--text-muted)' }}>
                  PASS #{booking.bookingId}
                </span>
              </div>

              <h3 style={{ fontSize: '20px', fontWeight: 700, marginBottom: '16px', color: 'var(--text-main)' }}>
                {booking.eventName}
              </h3>

              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-muted)', fontSize: '13px', marginBottom: '24px' }}>
                <Calendar size={14} />
                <span>Booked: {new Date(booking.bookedAt).toLocaleString()}</span>
              </div>

              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  backgroundColor: 'var(--bg-main)',
                  borderRadius: '12px',
                  padding: '12px 16px',
                  border: '1px solid var(--border-color)',
                }}
              >
                <div>
                  <div style={{ fontSize: '11px', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                    Attendee Name
                  </div>
                  <div style={{ fontSize: '14px', fontWeight: 600 }}>{user?.name}</div>
                </div>

                <div
                  style={{
                    backgroundColor: '#fff',
                    padding: '6px',
                    borderRadius: '6px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  <QrCode size={28} color="#000" />
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
