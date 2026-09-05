import React, { useState, useEffect } from 'react';
import { EventItem, PlatformSalesSummaryResponse, EventSalesResponse } from '../types';
import { api } from '../services/api';
import { useAuth } from '../context/AuthContext';
import {
  Shield,
  PlusCircle,
  BarChart3,
  Flame,
  Ticket,
  Calendar,
  XCircle,
  RefreshCw,
  ArrowLeft,
} from 'lucide-react';

interface AdminDashboardPageProps {
  navigate: (path: string) => void;
}

export const AdminDashboardPage: React.FC<AdminDashboardPageProps> = ({ navigate }) => {
  const { isAdmin } = useAuth();
  const [summary, setSummary] = useState<PlatformSalesSummaryResponse | null>(null);
  const [events, setEvents] = useState<EventItem[]>([]);
  const [selectedEventSales, setSelectedEventSales] = useState<EventSalesResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Form states
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [totalTickets, setTotalTickets] = useState(50);
  const [saleStartMinutesFromNow, setSaleStartMinutesFromNow] = useState(2);
  const [waitingRoomOffsetMinutes, setWaitingRoomOffsetMinutes] = useState(5);

  const loadData = async () => {
    try {
      const [sum, evts] = await Promise.all([
        api.getPlatformSalesSummary(),
        api.listEvents(),
      ]);
      setSummary(sum);
      setEvents(evts);
    } catch (err: any) {
      setError(err.message || 'Failed to load admin telemetry.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!isAdmin) return;
    loadData();
    const interval = setInterval(loadData, 5000);
    return () => clearInterval(interval);
  }, [isAdmin]);

  const handleCreateEvent = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);
    setCreating(true);

    try {
      const saleStartTime = new Date(Date.now() + saleStartMinutesFromNow * 60 * 1000).toISOString();
      const waitingRoomOpenOffsetSeconds = waitingRoomOffsetMinutes * 60;

      await api.createEvent({
        name,
        description,
        totalTickets,
        saleStartTime,
        waitingRoomOpenOffsetSeconds,
      });

      setSuccess(`Flash drop "${name}" successfully scheduled!`);
      setName('');
      setDescription('');
      loadData();
    } catch (err: any) {
      setError(err.message || 'Failed to create event.');
    } finally {
      setCreating(false);
    }
  };

  const handleCancelEvent = async (eventId: number) => {
    if (!confirm('Are you sure you want to cancel this flash drop?')) return;
    try {
      await api.cancelEvent(eventId);
      loadData();
    } catch (err: any) {
      alert('Cancel failed: ' + err.message);
    }
  };

  const inspectEventSales = async (eventId: number) => {
    try {
      const sales = await api.getEventSales(eventId);
      setSelectedEventSales(sales);
    } catch (err: any) {
      alert('Failed to get sales: ' + err.message);
    }
  };

  if (!isAdmin) {
    return (
      <div className="container" style={{ padding: '60px 24px', maxWidth: '560px', textAlign: 'center' }}>
        <div className="card" style={{ padding: '40px' }}>
          <Shield size={48} color="#EF4444" style={{ margin: '0 auto 16px' }} />
          <h2 style={{ fontSize: '24px', fontWeight: 700, marginBottom: '12px' }}>Admin Access Only</h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '24px' }}>
            This portal is restricted to users with the Administrator role.
          </p>
          <button onClick={() => navigate('/')} className="btn btn-secondary">
            <ArrowLeft size={16} /> Back to Catalog
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="container" style={{ padding: '40px 24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '32px' }}>
        <div>
          <div className="badge badge-upcoming" style={{ marginBottom: '8px' }}>
            <Shield size={12} /> ADMIN OPERATING CONSOLE
          </div>
          <h1 style={{ fontSize: '32px', fontWeight: 800 }}>Flash Sale Operations</h1>
        </div>

        <button onClick={loadData} className="btn btn-secondary" style={{ fontSize: '13px' }}>
          <RefreshCw size={14} /> Refresh Metrics
        </button>
      </div>

      {error && (
        <div
          style={{
            backgroundColor: 'var(--danger-bg)',
            color: '#F87171',
            padding: '12px 16px',
            borderRadius: '10px',
            marginBottom: '24px',
          }}
        >
          {error}
        </div>
      )}

      {success && (
        <div
          style={{
            backgroundColor: 'var(--success-bg)',
            color: '#34D399',
            padding: '12px 16px',
            borderRadius: '10px',
            marginBottom: '24px',
          }}
        >
          {success}
        </div>
      )}

      {/* Sales Summary KPI Cards */}
      {summary && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginBottom: '36px' }}>
          <div className="card" style={{ padding: '20px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-muted)', fontSize: '12px', marginBottom: '8px' }}>
              <Calendar size={16} /> TOTAL DROPS
            </div>
            <div style={{ fontSize: '28px', fontWeight: 800, fontFamily: 'var(--font-mono)' }}>
              {summary.totalEvents}
            </div>
          </div>

          <div className="card" style={{ padding: '20px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#10B981', fontSize: '12px', marginBottom: '8px' }}>
              <Flame size={16} /> ACTIVE LIVE DROPS
            </div>
            <div style={{ fontSize: '28px', fontWeight: 800, fontFamily: 'var(--font-mono)', color: '#34D399' }}>
              {summary.activeEventsCount}
            </div>
          </div>

          <div className="card" style={{ padding: '20px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-muted)', fontSize: '12px', marginBottom: '8px' }}>
              <Ticket size={16} /> TOTAL INVENTORY
            </div>
            <div style={{ fontSize: '28px', fontWeight: 800, fontFamily: 'var(--font-mono)' }}>
              {summary.totalTicketsAvailable}
            </div>
          </div>

          <div className="card" style={{ padding: '20px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--primary)', fontSize: '12px', marginBottom: '8px' }}>
              <Ticket size={16} /> TICKETS CLAIMED
            </div>
            <div style={{ fontSize: '28px', fontWeight: 800, fontFamily: 'var(--font-mono)', color: 'var(--primary)' }}>
              {summary.totalTicketsSold}
            </div>
          </div>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: '28px', alignItems: 'start' }}>
        {/* Events Table */}
        <div className="card" style={{ padding: '24px' }}>
          <h2 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '16px' }}>Manage Event Drops</h2>

          {loading ? (
            <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--text-muted)' }}>Loading events...</div>
          ) : events.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--text-muted)' }}>No events registered yet.</div>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--border-color)', color: 'var(--text-muted)' }}>
                    <th style={{ padding: '10px 8px' }}>ID</th>
                    <th style={{ padding: '10px 8px' }}>Name</th>
                    <th style={{ padding: '10px 8px' }}>Status</th>
                    <th style={{ padding: '10px 8px' }}>Claimed</th>
                    <th style={{ padding: '10px 8px' }}>Starts At</th>
                    <th style={{ padding: '10px 8px', textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {events.map((evt) => (
                    <tr key={evt.id} style={{ borderBottom: '1px solid rgba(51, 65, 85, 0.5)' }}>
                      <td style={{ padding: '12px 8px', fontFamily: 'var(--font-mono)' }}>#{evt.id}</td>
                      <td style={{ padding: '12px 8px', fontWeight: 600 }}>{evt.name}</td>
                      <td style={{ padding: '12px 8px' }}>
                        <span className={`badge badge-${evt.status.toLowerCase()}`}>{evt.status}</span>
                      </td>
                      <td style={{ padding: '12px 8px', fontFamily: 'var(--font-mono)' }}>
                        {evt.ticketsSold} / {evt.totalTickets}
                      </td>
                      <td style={{ padding: '12px 8px', color: 'var(--text-muted)' }}>
                        {new Date(evt.saleStartTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </td>
                      <td style={{ padding: '12px 8px', textAlign: 'right' }}>
                        <div style={{ display: 'flex', gap: '6px', justifyContent: 'flex-end' }}>
                          <button
                            onClick={() => inspectEventSales(evt.id)}
                            className="btn btn-secondary"
                            style={{ padding: '4px 8px', fontSize: '11px' }}
                            title="Inspect live sales"
                          >
                            <BarChart3 size={12} /> Metrics
                          </button>
                          {evt.status !== 'CANCELLED' && evt.status !== 'ENDED' && (
                            <button
                              onClick={() => handleCancelEvent(evt.id)}
                              className="btn btn-danger"
                              style={{ padding: '4px 8px', fontSize: '11px' }}
                              title="Cancel drop"
                            >
                              <XCircle size={12} />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Create Drop Form */}
        <div className="card" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
            <PlusCircle size={18} color="var(--primary)" />
            <h2 style={{ fontSize: '18px', fontWeight: 700 }}>Schedule Flash Drop</h2>
          </div>

          <form onSubmit={handleCreateEvent} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '12px', color: 'var(--text-muted)', marginBottom: '4px' }}>
                Drop Title
              </label>
              <input
                type="text"
                required
                placeholder="Taylor Swift Surprise Drop"
                style={{ width: '100%' }}
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '12px', color: 'var(--text-muted)', marginBottom: '4px' }}>
                Description
              </label>
              <textarea
                rows={2}
                placeholder="Exclusive VIP admission pass"
                style={{ width: '100%' }}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '12px', color: 'var(--text-muted)', marginBottom: '4px' }}>
                Total Tickets Available
              </label>
              <input
                type="number"
                min={1}
                max={50000}
                required
                style={{ width: '100%' }}
                value={totalTickets}
                onChange={(e) => setTotalTickets(parseInt(e.target.value) || 1)}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '12px', color: 'var(--text-muted)', marginBottom: '4px' }}>
                Starts in (minutes from now)
              </label>
              <input
                type="number"
                min={1}
                max={1440}
                required
                style={{ width: '100%' }}
                value={saleStartMinutesFromNow}
                onChange={(e) => setSaleStartMinutesFromNow(parseInt(e.target.value) || 1)}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '12px', color: 'var(--text-muted)', marginBottom: '4px' }}>
                Waiting Room Open (mins before sale)
              </label>
              <input
                type="number"
                min={1}
                max={120}
                required
                style={{ width: '100%' }}
                value={waitingRoomOffsetMinutes}
                onChange={(e) => setWaitingRoomOffsetMinutes(parseInt(e.target.value) || 1)}
              />
            </div>

            <button
              type="submit"
              disabled={creating}
              className="btn btn-primary"
              style={{ width: '100%', marginTop: '8px', padding: '12px' }}
            >
              {creating ? 'Scheduling...' : 'Launch Flash Drop'}
            </button>
          </form>
        </div>
      </div>

      {/* Live Sales Modal */}
      {selectedEventSales && (
        <div className="modal-backdrop" onClick={() => setSelectedEventSales(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h3 style={{ fontSize: '18px', fontWeight: 700 }}>Telemetry: {selectedEventSales.eventName}</h3>
              <button onClick={() => setSelectedEventSales(null)} style={{ color: 'var(--text-muted)' }}>
                ✕
              </button>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '12px', marginBottom: '20px' }}>
              <div style={{ backgroundColor: 'var(--bg-secondary)', padding: '14px', borderRadius: '10px' }}>
                <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>TICKETS CLAIMED</span>
                <div style={{ fontSize: '20px', fontWeight: 800, fontFamily: 'var(--font-mono)' }}>
                  {selectedEventSales.ticketsSold} / {selectedEventSales.totalTickets}
                </div>
              </div>

              <div style={{ backgroundColor: 'var(--bg-secondary)', padding: '14px', borderRadius: '10px' }}>
                <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>CURRENT QUEUE DEPTH</span>
                <div style={{ fontSize: '20px', fontWeight: 800, fontFamily: 'var(--font-mono)', color: 'var(--accent)' }}>
                  {selectedEventSales.totalInQueue} users
                </div>
              </div>

              <div style={{ backgroundColor: 'var(--bg-secondary)', padding: '14px', borderRadius: '10px' }}>
                <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>CONFIRMED BOOKINGS</span>
                <div style={{ fontSize: '20px', fontWeight: 800, fontFamily: 'var(--font-mono)', color: '#10B981' }}>
                  {selectedEventSales.completedBookings}
                </div>
              </div>

              <div style={{ backgroundColor: 'var(--bg-secondary)', padding: '14px', borderRadius: '10px' }}>
                <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>PERCENT SOLD OUT</span>
                <div style={{ fontSize: '20px', fontWeight: 800, fontFamily: 'var(--font-mono)', color: 'var(--primary)' }}>
                  {selectedEventSales.percentSold}%
                </div>
              </div>
            </div>

            <button onClick={() => setSelectedEventSales(null)} className="btn btn-secondary" style={{ width: '100%' }}>
              Close Telemetry
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
