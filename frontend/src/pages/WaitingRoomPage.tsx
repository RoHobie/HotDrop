import React, { useState, useEffect } from 'react';
import { EventItem, WaitingRoomStatusResponse } from '../types';
import { api } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { Countdown } from '../components/Countdown';
import { AuthModal } from '../components/AuthModal';
import { formatToIstDateTime } from '../utils/dateTime';
import { Clock, ShieldCheck, Shuffle, ArrowLeft, ArrowRight, AlertCircle } from 'lucide-react';

interface WaitingRoomPageProps {
  eventId: number;
  navigate: (path: string) => void;
}

export const WaitingRoomPage: React.FC<WaitingRoomPageProps> = ({ eventId, navigate }) => {
  const { isAuthenticated } = useAuth();
  const [event, setEvent] = useState<EventItem | null>(null);
  const [status, setStatus] = useState<WaitingRoomStatusResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [authModalOpen, setAuthModalOpen] = useState(false);

  // 1. Fetch Event info
  useEffect(() => {
    api.getEvent(eventId)
      .then(setEvent)
      .catch((err) => setError(err.message || 'Failed to load event details.'));
  }, [eventId]);

  // 2. Join waiting room when authenticated
  useEffect(() => {
    if (!isAuthenticated) return;

    let mounted = true;
    const joinAndPoll = async () => {
      try {
        await api.joinWaitingRoom(eventId);
        if (mounted) {
          const st = await api.getWaitingRoomStatus(eventId);
          setStatus(st);
          setLoading(false);
        }
      } catch (err: any) {
        if (mounted) {
          setError(err.message || 'Error joining waiting room.');
          setLoading(false);
        }
      }
    };

    joinAndPoll();

    const interval = setInterval(async () => {
      try {
        const st = await api.getWaitingRoomStatus(eventId);
        if (mounted) {
          setStatus(st);
          // If the status has transitioned to QUEUED or ADMITTED, redirect to queue page
          if (st.status === 'QUEUED' || st.status === 'ADMITTED') {
            navigate(`/queue/${eventId}`);
          }
        }
      } catch (err: any) {
        // If 404 or sale started, try queue status
        if (err.status === 400 || err.status === 404) {
          navigate(`/queue/${eventId}`);
        }
      }
    }, 3000);

    return () => {
      mounted = false;
      clearInterval(interval);
    };
  }, [eventId, isAuthenticated]);

  if (!isAuthenticated) {
    return (
      <div className="container" style={{ padding: '60px 24px', maxWidth: '560px', textAlign: 'center' }}>
        <div className="card" style={{ padding: '40px' }}>
          <Clock size={48} color="var(--primary)" style={{ margin: '0 auto 16px' }} />
          <h2 style={{ fontSize: '24px', fontWeight: 700, marginBottom: '12px' }}>Authentication Required</h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '24px', fontSize: '15px' }}>
            You must be logged in to reserve a spot in the HotDrop Waiting Room.
          </p>
          <button onClick={() => setAuthModalOpen(true)} className="btn btn-primary" style={{ width: '100%' }}>
            Sign In to Join Waiting Room
          </button>
          <button
            onClick={() => navigate('/')}
            className="btn btn-secondary"
            style={{ width: '100%', marginTop: '12px' }}
          >
            <ArrowLeft size={16} /> Back to Events
          </button>
        </div>
        <AuthModal isOpen={authModalOpen} onClose={() => setAuthModalOpen(false)} />
      </div>
    );
  }

  return (
    <div className="container" style={{ padding: '40px 24px', maxWidth: '680px' }}>
      <button
        onClick={() => navigate('/')}
        className="btn btn-secondary"
        style={{ marginBottom: '24px', fontSize: '13px' }}
      >
        <ArrowLeft size={16} /> Back to Catalog
      </button>

      {error ? (
        <div
          className="card"
          style={{
            borderColor: 'var(--danger)',
            backgroundColor: 'var(--danger-bg)',
            padding: '24px',
            textAlign: 'center',
          }}
        >
          <AlertCircle size={32} color="#EF4444" style={{ margin: '0 auto 12px' }} />
          <h3 style={{ fontSize: '18px', color: '#F87171', marginBottom: '8px' }}>Unable to Join Waiting Room</h3>
          <p style={{ color: 'var(--text-muted)', marginBottom: '16px' }}>{error}</p>
          <button onClick={() => navigate('/')} className="btn btn-secondary">
            Return to Events
          </button>
        </div>
      ) : (
        <div className="card" style={{ padding: '36px', textAlign: 'center' }}>
          {/* Top header */}
          <div
            className="badge badge-upcoming"
            style={{ margin: '0 auto 16px' }}
          >
            <Clock size={14} /> PRE-SALE WAITING ROOM
          </div>

          <h1 style={{ fontSize: '28px', fontWeight: 800, marginBottom: '8px' }}>
            {event?.name || 'Flash Drop Event'}
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '15px', marginBottom: '32px' }}>
            {event?.description}
          </p>

          {/* Countdown Clock Box */}
          <div
            style={{
              backgroundColor: 'var(--bg-main)',
              border: '1px solid var(--border-color)',
              borderRadius: '16px',
              padding: '24px',
              marginBottom: '32px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <div
              style={{
                fontSize: '12px',
                color: 'var(--text-muted)',
                marginBottom: '4px',
                textTransform: 'uppercase',
                letterSpacing: '0.08em',
                fontWeight: 600,
              }}
            >
              Sale Starts In:
            </div>
            {event && (
              <div
                style={{
                  fontSize: '13px',
                  fontFamily: 'var(--font-mono)',
                  color: 'var(--primary)',
                  marginBottom: '14px',
                  fontWeight: 600,
                }}
              >
                {formatToIstDateTime(event.saleStartTime)}
              </div>
            )}
            {event && (
              <Countdown
                targetDate={event.saleStartTime}
                onExpire={() => navigate(`/queue/${eventId}`)}
              />
            )}
          </div>

          {/* Fairness Feature Callout */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(2, 1fr)',
              gap: '16px',
              textAlign: 'left',
              marginBottom: '32px',
            }}
          >
            <div
              style={{
                backgroundColor: 'var(--bg-secondary)',
                borderRadius: '12px',
                padding: '16px',
                border: '1px solid var(--border-color)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#F59E0B', marginBottom: '6px' }}>
                <Shuffle size={18} />
                <span style={{ fontWeight: 700, fontSize: '14px' }}>Fair Random Shuffle</span>
              </div>
              <p style={{ fontSize: '13px', color: 'var(--text-muted)', lineHeight: 1.4 }}>
                Arriving early does not give an advantage. Everyone in this room gets an equal random queue assignment.
              </p>
            </div>

            <div
              style={{
                backgroundColor: 'var(--bg-secondary)',
                borderRadius: '12px',
                padding: '16px',
                border: '1px solid var(--border-color)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#10B981', marginBottom: '6px' }}>
                <ShieldCheck size={18} />
                <span style={{ fontWeight: 700, fontSize: '14px' }}>Anti-Bot Protection</span>
              </div>
              <p style={{ fontSize: '13px', color: 'var(--text-muted)', lineHeight: 1.4 }}>
                Token bucket rate limiting and verified JWT authentication prevent automated script hoarding.
              </p>
            </div>
          </div>

          {/* Status Indicator */}
          <div
            style={{
              padding: '16px',
              backgroundColor: 'rgba(59, 130, 246, 0.1)',
              borderRadius: '12px',
              border: '1px solid rgba(59, 130, 246, 0.3)',
              color: '#93C5FD',
              fontSize: '14px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '10px',
            }}
          >
            <span className="pulse-dot" style={{ backgroundColor: '#60A5FA' }} />
            <span>
              {loading
                ? 'Joining room...'
                : `You're verified in the room (${status?.status || 'WAITING'}). Keep this tab open — you'll transition automatically when the drop goes live!`}
            </span>
          </div>

          <button
            onClick={() => navigate(`/queue/${eventId}`)}
            className="btn btn-secondary"
            style={{ marginTop: '20px', width: '100%', fontSize: '13px' }}
          >
            Check Queue Status Directly <ArrowRight size={14} />
          </button>
        </div>
      )}
    </div>
  );
};
