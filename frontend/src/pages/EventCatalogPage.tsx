import React, { useState, useEffect } from 'react';
import { EventItem, EventStatus } from '../types';
import { api } from '../services/api';
import { Countdown } from '../components/Countdown';
import { Flame, Clock, Users, ArrowRight, RefreshCw } from 'lucide-react';

interface EventCatalogPageProps {
  navigate: (path: string) => void;
}

export const EventCatalogPage: React.FC<EventCatalogPageProps> = ({ navigate }) => {
  const [events, setEvents] = useState<EventItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<string>('ALL');
  const [refreshing, setRefreshing] = useState(false);

  const fetchEvents = async () => {
    try {
      const status = filter === 'ALL' ? undefined : (filter as EventStatus);
      const data = await api.listEvents(status);
      setEvents(data);
    } catch (err) {
      console.error('Failed to load events', err);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchEvents();
    const interval = setInterval(fetchEvents, 8000);
    return () => clearInterval(interval);
  }, [filter]);

  const handleManualRefresh = () => {
    setRefreshing(true);
    fetchEvents();
  };

  const isWaitingRoomOpen = (event: EventItem) => {
    const saleTime = new Date(event.saleStartTime).getTime();
    const now = Date.now();
    const offsetMs = event.waitingRoomOpenOffsetSeconds * 1000;
    return now >= saleTime - offsetMs;
  };

  return (
    <div className="container" style={{ padding: '40px 24px' }}>
      {/* Hero Section */}
      <div
        style={{
          textAlign: 'center',
          maxWidth: '780px',
          margin: '0 auto 48px',
        }}
      >
        <div
          className="badge"
          style={{
            backgroundColor: 'rgba(255, 77, 77, 0.1)',
            color: 'var(--primary)',
            border: '1px solid rgba(255, 77, 77, 0.25)',
            marginBottom: '16px',
          }}
        >
          <Flame size={14} /> ZERO OVERSELLING • CRYPTOGRAPHIC FAIRNESS
        </div>
        <h1
          style={{
            fontSize: '44px',
            fontWeight: 800,
            lineHeight: 1.15,
            marginBottom: '16px',
            letterSpacing: '-0.03em',
          }}
        >
          High-Velocity Flash Sale Drops
        </h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '17px', lineHeight: 1.6 }}>
          Experience deterministic queueing backed by PostgreSQL row locks (`FOR UPDATE SKIP LOCKED`),
          fair waiting room shuffle, and 2-minute guaranteed checkout windows.
        </p>
      </div>

      {/* Filter and Refresh Bar */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '28px',
          flexWrap: 'wrap',
          gap: '12px',
        }}
      >
        <div style={{ display: 'flex', gap: '8px' }}>
          {['ALL', 'LIVE', 'UPCOMING', 'ENDED'].map((status) => (
            <button
              key={status}
              onClick={() => setFilter(status)}
              className="btn btn-secondary"
              style={{
                padding: '8px 16px',
                fontSize: '13px',
                borderColor: filter === status ? 'var(--primary)' : 'var(--border-color)',
                backgroundColor: filter === status ? 'rgba(255, 77, 77, 0.1)' : 'var(--bg-card)',
                color: filter === status ? 'var(--primary)' : 'var(--text-main)',
              }}
            >
              {status}
            </button>
          ))}
        </div>

        <button
          onClick={handleManualRefresh}
          className="btn btn-secondary"
          style={{ padding: '8px 14px', fontSize: '13px' }}
        >
          <RefreshCw size={14} className={refreshing ? 'animate-spin' : ''} /> Refresh Drops
        </button>
      </div>

      {/* Events List */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--text-muted)' }}>
          Loading active flash drops...
        </div>
      ) : events.length === 0 ? (
        <div
          className="card"
          style={{
            textAlign: 'center',
            padding: '48px 24px',
            borderColor: 'var(--border-color)',
          }}
        >
          <Flame size={36} color="var(--text-muted)" style={{ margin: '0 auto 12px' }} />
          <h3 style={{ fontSize: '18px', marginBottom: '6px' }}>No events found</h3>
          <p style={{ color: 'var(--text-muted)', fontSize: '14px' }}>
            There are currently no events matching the "{filter}" filter.
          </p>
        </div>
      ) : (
        <div className="grid-events">
          {events.map((event) => {
            const percentSold =
              event.totalTickets > 0
                ? Math.min(100, Math.round((event.ticketsSold / event.totalTickets) * 100))
                : 0;
            const waitingRoomOpen = isWaitingRoomOpen(event);

            return (
              <div
                key={event.id}
                className="card"
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  borderTop: event.status === 'LIVE' ? '3px solid var(--primary)' : undefined,
                }}
              >
                <div>
                  {/* Top Header */}
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      marginBottom: '16px',
                    }}
                  >
                    <span
                      className={`badge badge-${event.status.toLowerCase()}`}
                    >
                      {event.status === 'LIVE' && <span className="pulse-dot" />}
                      {event.status}
                    </span>

                    <span
                      style={{
                        fontFamily: 'var(--font-mono)',
                        fontSize: '12px',
                        color: 'var(--text-muted)',
                      }}
                    >
                      #{event.id}
                    </span>
                  </div>

                  {/* Title & Description */}
                  <h3
                    style={{
                      fontSize: '20px',
                      fontWeight: 700,
                      marginBottom: '8px',
                      color: 'var(--text-main)',
                    }}
                  >
                    {event.name}
                  </h3>
                  <p
                    style={{
                      color: 'var(--text-muted)',
                      fontSize: '14px',
                      lineHeight: 1.5,
                      marginBottom: '20px',
                      minHeight: '42px',
                    }}
                  >
                    {event.description || 'Exclusive ticket release for HotDrop members.'}
                  </p>

                  {/* Ticket Inventory Bar */}
                  <div style={{ marginBottom: '20px' }}>
                    <div
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        fontSize: '12px',
                        marginBottom: '6px',
                      }}
                    >
                      <span style={{ color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <Users size={14} /> Tickets Claimed
                      </span>
                      <span style={{ fontWeight: 600 }}>
                        {event.ticketsSold} / {event.totalTickets} ({percentSold}%)
                      </span>
                    </div>
                    <div className="progress-container">
                      <div className="progress-bar" style={{ width: `${percentSold}%` }} />
                    </div>
                  </div>
                </div>

                {/* Bottom CTA / Countdown */}
                <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: '16px', marginTop: '12px' }}>
                  {event.status === 'UPCOMING' && (
                    <div style={{ marginBottom: '14px' }}>
                      <div
                        style={{
                          fontSize: '11px',
                          color: 'var(--text-muted)',
                          marginBottom: '6px',
                          textTransform: 'uppercase',
                          letterSpacing: '0.05em',
                        }}
                      >
                        Drop Begins In:
                      </div>
                      <Countdown targetDate={event.saleStartTime} onExpire={fetchEvents} />
                    </div>
                  )}

                  {/* Action Button */}
                  {event.status === 'LIVE' && event.remainingTickets > 0 && (
                    <button
                      onClick={() => navigate(`/queue/${event.id}`)}
                      className="btn btn-primary"
                      style={{ width: '100%' }}
                    >
                      <Flame size={16} /> Enter Queue <ArrowRight size={16} />
                    </button>
                  )}

                  {event.status === 'UPCOMING' && waitingRoomOpen && (
                    <button
                      onClick={() => navigate(`/waiting-room/${event.id}`)}
                      className="btn btn-primary"
                      style={{ width: '100%', background: 'linear-gradient(135deg, #3B82F6, #2563EB)' }}
                    >
                      <Clock size={16} /> Join Waiting Room <ArrowRight size={16} />
                    </button>
                  )}

                  {event.status === 'UPCOMING' && !waitingRoomOpen && (
                    <button
                      disabled
                      className="btn btn-secondary"
                      style={{ width: '100%', opacity: 0.6 }}
                    >
                      Waiting Room Opens {event.waitingRoomOpenOffsetSeconds / 60}m Before Sale
                    </button>
                  )}

                  {(event.status === 'ENDED' || event.remainingTickets <= 0) && (
                    <button
                      disabled
                      className="btn btn-secondary"
                      style={{ width: '100%', opacity: 0.6 }}
                    >
                      Sale Concluded
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
