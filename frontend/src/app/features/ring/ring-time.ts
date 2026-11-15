import { MESSAGES } from '../../core/i18n/messages';
import { Ring, RingDay } from './ring.model';

/** Rule: SPEC-002 #6. Every date on screen is shown in this zone, whatever the browser's own zone is. */
export const RING_TIME_ZONE = 'America/Sao_Paulo';
const LOCALE = 'pt-BR';

const dayKeyFormat = new Intl.DateTimeFormat('en-CA', {
  timeZone: RING_TIME_ZONE,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
});
const dayLabelFormat = new Intl.DateTimeFormat(LOCALE, {
  timeZone: RING_TIME_ZONE,
  day: 'numeric',
  month: 'long',
  year: 'numeric',
});
const timeFormat = new Intl.DateTimeFormat(LOCALE, {
  timeZone: RING_TIME_ZONE,
  hour: '2-digit',
  minute: '2-digit',
});
const dateTimeFormat = new Intl.DateTimeFormat(LOCALE, {
  timeZone: RING_TIME_ZONE,
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

/** Calendar day in the ring zone as YYYY-MM-DD (the en-CA locale formats dates that way). */
export function dayKey(date: Date): string {
  return dayKeyFormat.format(date);
}

export function timeLabel(date: Date): string {
  return timeFormat.format(date);
}

export function dateTimeLabel(date: Date): string {
  return dateTimeFormat.format(date);
}

function previousDayKey(now: Date): string {
  // Noon in the ring zone minus a day never crosses two calendar days, with or without daylight saving time.
  const [year, month, day] = dayKey(now).split('-').map(Number);
  return dayKey(new Date(Date.UTC(year, month - 1, day - 1, 15)));
}

/** Rule: SPEC-002 #7. Assumes rings are already newest first. */
export function groupByDay(rings: readonly Ring[], now: Date): RingDay[] {
  const today = dayKey(now);
  const yesterday = previousDayKey(now);
  const days: RingDay[] = [];
  for (const ring of rings) {
    const key = dayKey(ring.ringedAt);
    const current = days.at(-1);
    if (current?.key === key) {
      days[days.length - 1] = { ...current, rings: [...current.rings, ring] };
    } else {
      days.push({ key, label: dayLabel(key, ring.ringedAt, today, yesterday), rings: [ring] });
    }
  }
  return days;
}

function dayLabel(key: string, date: Date, today: string, yesterday: string): string {
  if (key === today) {
    return MESSAGES.history.today;
  }
  if (key === yesterday) {
    return MESSAGES.history.yesterday;
  }
  return dayLabelFormat.format(date);
}
