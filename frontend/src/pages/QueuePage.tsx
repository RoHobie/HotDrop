import React, { useState, useEffect } from 'react';
import { EventItem, QueueStatusResponse } from '../types';
import { api } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { AuthModal } from '../components/AuthModal';
import {
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  ArrowLeft,
  Flame,
  Clock,
  Ticket,
} from 'lucide-react';

interface QueuePageProps {
  eventId: number;
  navigate: (path: string) => void;
}

export const QueuePage: React.FC<QueuePageProps> = ({ eventId, navigate }) => {
  const { isAuthenticated } = useAuth();
  const [event, setEvent] = useState<EventItem | null>(null);
  const [queueStatus, setQueueStatus] = useState<QueueStatusResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [authModalOpen, setAuthModalOpen] = useState(false);

  // 1. Fetch Event Info
  useEffect(() => {
    api.getEvent(eventId)
      .then(setEvent)
      .catch((err) => setError(err.message || 'Failed to load event.'));
  }, [eventId]);

  // 2. Poll Queue Status
  useEffect(() => {
    if (!isAuthenticated) return;

    let mounted = true;

    const poll = async () => {
      try {
        const data = await api.getQueueStatus(eventId);
        if (mounted) {
          setQueueStatus(data);
          setLoading(false);

          // If admitted, redirect directly to checkout
          if (data.status === 'ADMITTED') {
            navigate(`/checkout/${eventId}`);
          }
        }
      } catch (err: any) {
        if (mounted) {
          // If not in queue, attempt to join waiting room or check if pre-sale
          if (err.status === 404) {
            try {
              await api.joinWaitingRoom(eventId);
              const st = await api.getQueueStatus(eventId);
              setQueueStatus(st);
            } catch (joinErr: any) {
              setError(joinErr.message || 'Unable to enter queue.');
            }
          } else {
            setError(err.message || 'Failed to fetch queue status.');
          }
          setLoading(false);
        }
      }
    };

    poll();
    const interval = setInterval(poll, 2500);

    return () => {
      mounted = false;
      clearInterval(interval);
    };
  }, [eventId, isAuthenticated]);

  if (!isAuthenticated) {
    return (
      <div className="container" style={{ padding: '60px 24px', maxWidth: '560px', textAlign: 'center' }}>
        <div className="card" style={{ padding: '40px' }}>
          <Flame size={48} color="var(--primary)" style={{ margin: '0 auto 16px' }} />
          <h2 style={{ fontSize: '24px', fontWeight: 700, marginBottom: '12px' }}>Authentication Required</h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '24px', fontSize: '15px' }}>
            Please log in to hold your position in the ticket queue.
          </p>
          <button onClick={() => setAuthModalOpen(true)} className="btn btn-primary" style={{ width: '100%' }}>
            Sign In to View Queue
          </button>
          <button
            onClick={() => navigate('/')}
            className="btn btn-secondary"
            style={{ width: '100%', marginTop: '12px' }}
          >
            <ArrowLeft size={16} /> Back to Catalog
          </button>
        </div>
        <AuthModal isOpen={authModalOpen} onClose={() => setAuthModalOpen(false)} />
      </div>
    );
  }

  const renderContent = () => {
    if (loading) {
      return (
        <div style={{ padding: '40px 0', textAlign: 'center', color: 'var(--text-muted)' }}>
          Connecting to queue manager...
        </div>
      );
    }

    if (error) {
      return (
        <div style={{ textAlign: 'center', padding: '24px' }}>
          <AlertTriangle size={40} color="#F59E0B" style={{ margin: '0 auto 16px' }} />
          <h3 style={{ fontSize: '18px', marginBottom: '8px' }}>Queue Notice</h3>
          <p style={{ color: 'var(--text-muted)', marginBottom: '20px' }}>{error}</p>
          <button onClick={() => navigate('/')} className="btn btn-secondary">
            Return to Events
          </button>
        </div>
      );
    }

    if (!queueStatus) return null;

    if (queueStatus.status === 'COMPLETED') {
      return (
        <div style={{ textAlign: 'center', padding: '32px 0' }}>
          <CheckCircle2 size={48} color="#10B981" style={{ margin: '0 auto 16px' }} />
          <h2 style={{ fontSize: '22px', fontWeight: 700, marginBottom: '8px' }}>
            Ticket Already Claimed!
          </h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '24px' }}>
            You have already successfully completed a purchase for this event drop.
          </p>
          <button
            onClick={() => navigate('/my-bookings')}
            className="btn btn-primary"
            style={{ margin: '0 auto' }}
          >
            <Ticket size={16} /> View My Bookings
          </button>
        </div>
      );
    }

    if (queueStatus.status === 'EXPIRED') {
      return (
        <div style={{ textAlign: 'center', padding: '32px 0' }}>
          <AlertTriangle size={48} color="#EF4444" style={{ margin: '0 auto 16px' }} />
          <h2 style={{ fontSize: '22px', fontWeight: 700, marginBottom: '8px' }}>
            Admission Window Expired
          </h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '24px' }}>
            The 2-minute reserved checkout window elapsed before booking was confirmed.
          </p>
          <button
            onClick={() => navigate('/')}
            className="btn btn-secondary"
            style={{ margin: '0 auto' }}
          >
            Return to Catalog
          </button>
        </div>
      );
    }

    if (queueStatus.status === 'ADMITTED') {
      return (
        <div style={{ textAlign: 'center', padding: '32px 0' }}>
          <div
            style={{
              width: '64px',
              height: '64px',
              borderRadius: '50%',
              backgroundColor: 'var(--success-bg)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 16px',
            }}
          >
            <CheckCircle2 size={36} color="#10B981" />
          </div>
          <h2 style={{ fontSize: '24px', fontWeight: 800, color: '#34D399', marginBottom: '8px' }}>
            You're Next! Admission Granted
          </h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '28px', maxWidth: '440px', margin: '0 auto 28px' }}>
            Your checkout slot is unlocked. Complete your booking within the 2-minute reservation window.
          </p>
          <button
            onClick={() => navigate(`/checkout/${eventId}`)}
            className="btn btn-primary"
            style={{ fontSize: '16px', padding: '14px 28px', margin: '0 auto' }}
          >
            Proceed to Checkout <ArrowRight size={18} />
          </button>
        </div>
      );
    }

    // Default: QUEUED or WAITING
    const position = queueStatus.queuePosition || 1;
    const total = Math.max(queueStatus.totalInQueue || 1, position);
    const progressPercent = Math.max(5, Math.min(100, Math.round(((total - position + 1) / total) * 100)));

    return (
      <div>
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <div className="badge badge-queued" style={{ margin: '0 auto 16px' }}>
            <span className="pulse-dot" style={{ backgroundColor: '#F59E0B' }} />
            LIVE QUEUE ACTIVE
          </div>

          <div
            style={{
              fontSize: '14px',
              color: 'var(--text-muted)',
              marginBottom: '8px',
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
            }}
          >
            Your Position in Line:
          </div>

          <div
            style={{
              fontSize: '56px',
              fontWeight: 800,
              fontFamily: 'var(--font-mono)',
              color: 'var(--text-main)',
              lineHeight: 1,
              marginBottom: '12px',
            }}
          >
            #{position}
          </div>

          <div style={{ color: 'var(--text-muted)', fontSize: '14px' }}>
            of <span style={{ color: 'var(--text-main)', fontWeight: 600 }}>{total}</span> contenders waiting
          </div>
        </div>

        {/* Progress bar */}
        <div style={{ marginBottom: '32px' }}>
          <div className="progress-container" style={{ height: '10px' }}>
            <div className="progress-bar" style={{ width: `${progressPercent}%` }} />
          </div>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              fontSize: '12px',
              color: 'var(--text-muted)',
              marginTop: '8px',
            }}
          >
            <span>Back of queue</span>
            <span>Admitted to Checkout</span>
          </div>
        </div>

        {/* Info Cards */}
        <div
          style={{
            backgroundColor: 'var(--bg-secondary)',
            borderRadius: '12px',
            padding: '20px',
            border: '1px solid var(--border-color)',
            display: 'flex',
            alignItems: 'center',
            gap: '16px',
          }}
        >
          <Clock size={28} color="#F59E0B" />
          <div style={{ fontSize: '13px', lineHeight: 1.5 }}>
            <strong style={{ color: 'var(--text-main)', display: 'block' }}>
              Batches Admitted Automatically
            </strong>
            <span style={{ color: 'var(--text-muted)' }}>
              HotDrop admits buyers in continuous batches using DB-level concurrency controls (`SKIP LOCKED`). Keep this window open; you will automatically be forwarded to checkout.
            </span>
          </div>
        </div>
      </div>
    );
  };

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
        <div style={{ borderBottom: '1px solid var(--border-color)', paddingBottom: '16px', marginBottom: '24px' }}>
          <h2 style={{ fontSize: '20px', fontWeight: 700 }}>{event?.name || 'Flash Drop'}</h2>
          <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
            {event?.remainingTickets} tickets remaining
          </span>
        </div>

        {renderContent()}
      </div>
    </div>
  );
};
