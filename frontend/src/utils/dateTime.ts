export const IST_TIMEZONE = 'Asia/Kolkata';
export const IST_LABEL = 'IST (UTC+05:30)';

/**
 * Returns YYYY-MM-DDTHH:mm string in IST timezone for an HTML datetime-local input,
 * offset by a given number of minutes from current time (defaults to +10 mins).
 */
export function getNowInIstInputString(offsetMinutes: number = 10): string {
  const target = new Date(Date.now() + offsetMinutes * 60 * 1000);
  return formatForDateTimeLocal(target);
}

/**
 * Returns YYYY-MM-DDTHH:mm string in IST for tomorrow at a specific hour and minute.
 */
export function getTomorrowInIstInputString(hour: number = 10, minute: number = 0): string {
  // Get current IST date parts
  const now = new Date();
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: IST_TIMEZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(now);

  const get = (type: string) => parts.find((p) => p.type === type)?.value || '01';
  const year = parseInt(get('year'), 10);
  const month = parseInt(get('month'), 10) - 1;
  const day = parseInt(get('day'), 10);

  // Tomorrow in UTC date matching the IST calendar day + 1
  const tomorrow = new Date(Date.UTC(year, month, day + 1, hour, minute));
  // Adjust from IST to UTC: IST is UTC+5:30, so hour:minute in IST is hour-5, minute-30 in UTC
  const targetUtcMs = tomorrow.getTime() - 5.5 * 60 * 60 * 1000;
  return formatForDateTimeLocal(new Date(targetUtcMs));
}

function formatForDateTimeLocal(date: Date): string {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: IST_TIMEZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).formatToParts(date);

  const get = (type: string) => parts.find((p) => p.type === type)?.value || '00';
  return `${get('year')}-${get('month')}-${get('day')}T${get('hour')}:${get('minute')}`;
}

/**
 * Converts a datetime-local input value (YYYY-MM-DDTHH:mm) entered in IST to an ISO-8601 UTC string.
 */
export function istDateTimeInputToIso(val: string): string {
  if (!val) return '';
  const cleanVal = val.length === 16 ? `${val}:00` : val;
  const istDateString = `${cleanVal}+05:30`;
  return new Date(istDateString).toISOString();
}

/**
 * Checks whether the chosen IST datetime string is strictly in the future.
 */
export function isFutureIstDateTime(val: string): boolean {
  if (!val) return false;
  try {
    const iso = istDateTimeInputToIso(val);
    return new Date(iso).getTime() > Date.now();
  } catch {
    return false;
  }
}

/**
 * Formats an ISO date string / Date into a human-readable IST string:
 * e.g. "05 Sep 2026, 04:30 PM IST"
 */
export function formatToIstDateTime(date: string | Date | number): string {
  const d = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date;
  if (isNaN(d.getTime())) return 'Invalid Date';

  return (
    new Intl.DateTimeFormat('en-IN', {
      timeZone: IST_TIMEZONE,
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    }).format(d) + ' IST'
  );
}

/**
 * Formats an ISO date string into a compact IST time:
 * e.g. "04:30 PM IST"
 */
export function formatToIstTime(date: string | Date | number): string {
  const d = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date;
  if (isNaN(d.getTime())) return 'Invalid Date';

  return (
    new Intl.DateTimeFormat('en-IN', {
      timeZone: IST_TIMEZONE,
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    }).format(d) + ' IST'
  );
}

/**
 * Calculates human-readable relative time from now to a target IST datetime string.
 */
export function getRelativeTimeFromIstInput(val: string): string {
  if (!val) return '';
  try {
    const iso = istDateTimeInputToIso(val);
    const targetMs = new Date(iso).getTime();
    const diffMs = targetMs - Date.now();
    if (diffMs <= 0) return 'in the past';

    const diffMins = Math.round(diffMs / 60000);
    if (diffMins < 60) return `in ${diffMins} min${diffMins === 1 ? '' : 's'}`;

    const diffHours = Math.floor(diffMins / 60);
    const remMins = diffMins % 60;
    if (remMins === 0) return `in ${diffHours} hour${diffHours === 1 ? '' : 's'}`;
    return `in ${diffHours}h ${remMins}m`;
  } catch {
    return '';
  }
}
