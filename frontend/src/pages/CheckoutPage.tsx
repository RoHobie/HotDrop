import React, { useState, useEffect } from 'react';
import { Booking, EventItem } from '../types';
import { api } from '../services/api';
import { useAuth } from '../context/AuthContext';
import {
  Ticket,
  Clock,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
  ArrowLeft,
  Flame,
} from 'lucide-react';

interface CheckoutPageProps {
  eventId: number;
  navigate: (path: string) => void;
}

export const CheckoutPage: React.FC<CheckoutPageProps> = ({ eventId, navigate }) => {
  const { user } = useAuth();
  const [event, setEvent] = useState<EventItem | null>(null);
  const [booking, setBooking] = useState<Booking | null>(null);
  const [secondsRemaining, setSecondsRemaining] = useState<number>(120);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 1. Fetch Event and Queue Info
  useEffect(() => {
    api.getEvent(eventId).then(setEvent).catch(console.error);

    api.getQueueStatus(eventId)
      .then((res) => {
        if (res.secondsRemaining) {
          setSecondsRemaining(res.secondsRemaining);
        }
      })
      .catch((err) => {
        setError(err.message || 'Unable to verify admission.');
      });
  }, [eventId]);

  // 2. Ticking Countdown
  useEffect(() => {
    if (booking) return; // Stop counting if booked

    const timer = setInterval(() => {
      setSecondsRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          setError('Your 2-minute checkout window has expired. Ticket reservation released.');
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [booking]);

  const handleConfirmPurchase = async () => {
    setError(null);
    setSubmitting(true);

    try {
      const booked = await api.bookTicket(eventId);
      setBooking(booked);
    } catch (err: any) {
      setError(err.message || 'Checkout failed. The event may be sold out or your window expired.');
    } finally {
      setSubmitting(false);
    }
  };

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };

  if (booking) {
    return (
      <div className="container" style={{ padding: '40px 24px', maxWidth: '580px' }}>
        <div
          className="card"
          style={{
            textAlign: 'center',
            padding: '40px',
            borderColor: 'var(--success)',
            background: 'linear-gradient(180deg, rgba(16, 185, 129, 0.05), var(--bg-card))',
          }}
        >
          <div
            style={{
              width: '72px',
              height: '72px',
              borderRadius: '50%',
              backgroundColor: 'var(--success-bg)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 20px',
            }}
          >
            <CheckCircle2 size={44} color="#10B981" />
          </div>

          <span className="badge badge-admitted" style={{ marginBottom: '12px' }}>
            PURCHASE CONFIRMED
          </span>

          <h1 style={{ fontSize: '28px', fontWeight: 800, marginBottom: '8px' }}>
            You Got The Drop!
          </h1>
          <p style={{ color: 'var(--text-muted)', marginBottom: '32px' }}>
            Your ticket reservation was atomically verified and permanently secured.
          </p>

          {/* Ticket pass card */}
          <div
            style={{
              backgroundColor: 'var(--bg-main)',
              border: '2px dashed var(--border-color)',
              borderRadius: '16px',
              padding: '24px',
              textAlign: 'left',
              marginBottom: '32px',
              position: 'relative',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '16px' }}>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Event
                </span>
                <div style={{ fontSize: '18px', fontWeight: 700 }}>{booking.eventName}</div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <span style={{ fontSize: '11px', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Booking ID
                </span>
                <div style={{ fontFamily: 'var(--font-mono)', fontSize: '18px', fontWeight: 700, color: 'var(--primary)' }}>
                  #{booking.bookingId}
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', borderTop: '1px solid var(--border-color)', paddingTop: '12px' }}>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Attendee
                </span>
                <div style={{ fontSize: '13px', fontWeight: 600 }}>{user?.name}</div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <span style={{ fontSize: '11px', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Status
                </span>
                <div style={{ fontSize: '13px', fontWeight: 700, color: '#10B981' }}>CONFIRMED</div>
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '12px' }}>
            <button
              onClick={() => navigate('/my-bookings')}
              className="btn btn-primary"
              style={{ flex: 1 }}
            >
              <Ticket size={16} /> View in My Bookings
            </button>
            <button
              onClick={() => navigate('/')}
              className="btn btn-secondary"
              style={{ flex: 1 }}
            >
              Explore Drops
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container" style={{ padding: '40px 24px', maxWidth: '640px' }}>
      <button
        onClick={() => navigate('/')}
        className="btn btn-secondary"
        style={{ marginBottom: '24px', fontSize: '13px' }}
      >
        <ArrowLeft size={16} /> Back to Catalog
      </button>

      <div className="card" style={{ padding: '36px' }}>
        {/* Timer Bar */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            backgroundColor: secondsRemaining < 30 ? 'var(--danger-bg)' : 'var(--bg-main)',
            border: `1px solid ${secondsRemaining < 30 ? 'rgba(239, 68, 68, 0.4)' : 'var(--border-color)'}`,
            borderRadius: '12px',
            padding: '12px 20px',
            marginBottom: '28px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Clock size={18} color={secondsRemaining < 30 ? '#EF4444' : '#F59E0B'} />
            <span style={{ fontSize: '13px', fontWeight: 600 }}>Checkout Window Expiring:</span>
          </div>

          <div
            style={{
              fontFamily: 'var(--font-mono)',
              fontSize: '20px',
              fontWeight: 800,
              color: secondsRemaining < 30 ? '#EF4444' : 'var(--primary)',
            }}
          >
            {formatTime(secondsRemaining)}
          </div>
        </div>

        {error && (
          <div
            style={{
              backgroundColor: 'var(--danger-bg)',
              color: '#F87171',
              padding: '14px 18px',
              borderRadius: '10px',
              fontSize: '14px',
              marginBottom: '24px',
              border: '1px solid rgba(239, 68, 68, 0.4)',
              display: 'flex',
              alignItems: 'center',
              gap: '12px',
            }}
          >
            <AlertCircle size={20} />
            <span>{error}</span>
          </div>
        )}

        <div style={{ marginBottom: '28px' }}>
          <span className="badge badge-admitted" style={{ marginBottom: '8px' }}>
            SLOT ALLOCATED
          </span>
          <h1 style={{ fontSize: '26px', fontWeight: 800, marginBottom: '6px' }}>
            {event?.name || 'Flash Drop Ticket'}
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '14px' }}>
            {event?.description}
          </p>
        </div>

        {/* Order Summary Box */}
        <div
          style={{
            backgroundColor: 'var(--bg-secondary)',
            borderRadius: '14px',
            padding: '20px',
            border: '1px solid var(--border-color)',
            marginBottom: '28px',
          }}
        >
          <div style={{ fontSize: '12px', color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '12px' }}>
            Order Breakdown
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px', fontSize: '14px' }}>
            <span>1x General Admission HotDrop Ticket</span>
            <span style={{ fontWeight: 600 }}>Free Drop Tier</span>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px', fontSize: '14px' }}>
            <span>Verified Queue Access</span>
            <span style={{ color: '#10B981', fontWeight: 600 }}>Guaranteed</span>
          </div>

          <div
            style={{
              borderTop: '1px solid var(--border-color)',
              paddingTop: '12px',
              marginTop: '12px',
              display: 'flex',
              justifyContent: 'space-between',
              fontWeight: 700,
              fontSize: '16px',
            }}
          >
            <span>Total:</span>
            <span style={{ color: 'var(--primary)' }}>$0.00 (Flash Drop)</span>
          </div>
        </div>

        {/* Security / Concurrency note */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            fontSize: '12px',
            color: 'var(--text-muted)',
            marginBottom: '24px',
          }}
        >
          <ShieldCheck size={16} color="#10B981" />
          <span>Backed by PostgreSQL atomic row lock with 0.00% overselling guarantee.</span>
        </div>

        <button
          onClick={handleConfirmPurchase}
          disabled={submitting || secondsRemaining <= 0}
          className="btn btn-primary"
          style={{ width: '100%', padding: '14px', fontSize: '16px' }}
        >
          {submitting ? (
            'Securing Ticket...'
          ) : (
            <>
              <Flame size={18} /> Confirm & Claim Ticket <ArrowRight size={18} />
            </>
          )}
        </button>
      </div>
    </div>
  );
};
