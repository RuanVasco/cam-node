import { dateTimeLabel, groupByDay, timeLabel } from './ring-time';
import { Ring } from './ring.model';

function ringAt(iso: string): Ring {
  return { id: iso, ringedAt: new Date(iso), photoUrl: `/api/v1/rings/${iso}/photo` };
}

describe('ring time', () => {
  it('SPEC-002 CA-7 (#6): 2026-11-23T01:30:00Z is shown as 22 de nov. de 2026, 22:30 in America/Sao_Paulo', () => {
    const ring = ringAt('2026-11-23T01:30:00Z');

    expect(timeLabel(ring.ringedAt)).toBe('22:30');
    expect(dateTimeLabel(ring.ringedAt)).toBe('22 de nov. de 2026, 22:30');
  });

  it('SPEC-002 CA-7 (#7): a ring after 21:00 in Sao Paulo belongs to that local day, not the UTC one', () => {
    const now = new Date('2026-12-01T15:00:00Z');

    const [day] = groupByDay([ringAt('2026-11-23T01:30:00Z')], now);

    expect(day.key).toBe('2026-11-22');
    expect(day.label).toBe('22 de novembro de 2026');
  });

  it('SPEC-002 #7: groups rings by day with Hoje and Ontem headers, newest first', () => {
    const now = new Date('2026-11-23T15:00:00Z');
    const rings = [
      ringAt('2026-11-23T14:00:00Z'),
      ringAt('2026-11-23T10:00:00Z'),
      ringAt('2026-11-22T20:00:00Z'),
      ringAt('2026-11-20T12:00:00Z'),
    ];

    const days = groupByDay(rings, now);

    expect(days.map((day) => day.label)).toEqual(['Hoje', 'Ontem', '20 de novembro de 2026']);
    expect(days.map((day) => day.rings.length)).toEqual([2, 1, 1]);
  });

  it('SPEC-002 #7: right after local midnight the previous evening is Ontem', () => {
    const now = new Date('2026-11-23T03:05:00Z');

    const [day] = groupByDay([ringAt('2026-11-23T02:55:00Z')], now);

    expect(day.label).toBe('Ontem');
  });
});
