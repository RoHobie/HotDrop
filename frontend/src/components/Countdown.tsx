import React, { useState, useEffect } from 'react';

interface CountdownProps {
  targetDate: string;
  onExpire?: () => void;
  compact?: boolean;
}

interface TimeRemaining {
  totalMs: number;
  days: number;
  hours: number;
  minutes: number;
  seconds: number;
}

export const Countdown: React.FC<CountdownProps> = ({ targetDate, onExpire, compact = false }) => {
  const calculateRemaining = (): TimeRemaining => {
    const totalMs = Math.max(0, new Date(targetDate).getTime() - Date.now());
    const seconds = Math.floor((totalMs / 1000) % 60);
    const minutes = Math.floor((totalMs / 1000 / 60) % 60);
    const hours = Math.floor((totalMs / (1000 * 60 * 60)) % 24);
    const days = Math.floor(totalMs / (1000 * 60 * 60 * 24));
    return { totalMs, days, hours, minutes, seconds };
  };

  const [remaining, setRemaining] = useState<TimeRemaining>(calculateRemaining);

  useEffect(() => {
    const timer = setInterval(() => {
      const updated = calculateRemaining();
      setRemaining(updated);
      if (updated.totalMs <= 0) {
        clearInterval(timer);
        if (onExpire) onExpire();
      }
    }, 1000);

    return () => clearInterval(timer);
  }, [targetDate]);

  if (remaining.totalMs <= 0) {
    return <span style={{ color: 'var(--danger)', fontWeight: 700 }}>SALE LIVE NOW</span>;
  }

  const pad = (n: number) => String(n).padStart(2, '0');

  if (compact) {
    return (
      <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>
        {remaining.days > 0 && `${remaining.days}d `}
        {pad(remaining.hours)}:{pad(remaining.minutes)}:{pad(remaining.seconds)}
      </span>
    );
  }

  return (
    <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
      {remaining.days > 0 && (
        <div style={boxStyle}>
          <span style={numberStyle}>{pad(remaining.days)}</span>
          <span style={labelStyle}>DAYS</span>
        </div>
      )}
      <div style={boxStyle}>
        <span style={numberStyle}>{pad(remaining.hours)}</span>
        <span style={labelStyle}>HRS</span>
      </div>
      <div style={boxStyle}>
        <span style={numberStyle}>{pad(remaining.minutes)}</span>
        <span style={labelStyle}>MIN</span>
      </div>
      <div style={boxStyle}>
        <span style={{ ...numberStyle, color: 'var(--primary)' }}>{pad(remaining.seconds)}</span>
        <span style={labelStyle}>SEC</span>
      </div>
    </div>
  );
};

const boxStyle: React.CSSProperties = {
  backgroundColor: 'var(--bg-secondary)',
  borderRadius: '8px',
  padding: '8px 12px',
  textAlign: 'center',
  minWidth: '54px',
  border: '1px solid var(--border-color)',
};

const numberStyle: React.CSSProperties = {
  display: 'block',
  fontSize: '20px',
  fontWeight: 800,
  fontFamily: 'var(--font-mono)',
  color: 'var(--text-main)',
  lineHeight: '1.1',
};

const labelStyle: React.CSSProperties = {
  fontSize: '9px',
  letterSpacing: '0.05em',
  color: 'var(--text-muted)',
  fontWeight: 600,
};
