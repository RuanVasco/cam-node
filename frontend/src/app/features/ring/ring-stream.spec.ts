import { TestBed } from '@angular/core/testing';

import { EVENT_SOURCE_FACTORY, RingStream, STREAM_RETRY_MS } from './ring-stream';
import { RingStreamEvent } from './ring.model';

// jsdom has no EventSource; the adapter only reads its readyState constants.
vi.stubGlobal('EventSource', { CONNECTING: 0, OPEN: 1, CLOSED: 2 });

class FakeEventSource {
  readyState: number = EventSource.CONNECTING;
  closed = false;
  onopen: (() => void) | null = null;
  onerror: (() => void) | null = null;
  private readonly listeners = new Map<string, (event: MessageEvent<string>) => void>();

  constructor(readonly url: string) {}

  addEventListener(type: string, listener: (event: MessageEvent<string>) => void): void {
    this.listeners.set(type, listener);
  }

  close(): void {
    this.closed = true;
  }

  emit(type: string, data: string): void {
    this.listeners.get(type)?.(new MessageEvent(type, { data }));
  }
}

describe('RingStream', () => {
  let sources: FakeEventSource[];
  let stream: RingStream;

  beforeEach(() => {
    vi.useFakeTimers();
    sources = [];
    TestBed.configureTestingModule({
      providers: [
        {
          provide: EVENT_SOURCE_FACTORY,
          useValue: (url: string) => {
            const source = new FakeEventSource(url);
            sources.push(source);
            return source;
          },
        },
      ],
    });
    stream = TestBed.inject(RingStream);
  });

  afterEach(() => vi.useRealTimers());

  it('SPEC-002 #4: turns a ring event into a domain ring', () => {
    const events: RingStreamEvent[] = [];
    stream.connect().subscribe((event) => events.push(event));

    sources[0].emit(
      'ring',
      '{"id":"r1","ringedAt":"2026-11-23T19:42:10.123Z","photoUrl":"/api/v1/rings/r1/photo"}',
    );

    expect(sources[0].url).toBe('/api/v1/rings/stream');
    expect(events).toEqual([
      {
        kind: 'ring',
        ring: {
          id: 'r1',
          ringedAt: new Date('2026-11-23T19:42:10.123Z'),
          photoUrl: '/api/v1/rings/r1/photo',
        },
      },
    ]);
  });

  it('SPEC-010 #4: turns a ring-removed event into a domain event', () => {
    const events: RingStreamEvent[] = [];
    stream.connect().subscribe((event) => events.push(event));

    sources[0].emit('ring-removed', '{"id":"r1"}');

    expect(events).toEqual([{ kind: 'removed', id: 'r1' }]);
  });

  it('SPEC-010 #11: turns a ring-restored event into a domain event', () => {
    const events: RingStreamEvent[] = [];
    stream.connect().subscribe((event) => events.push(event));

    sources[0].emit(
      'ring-restored',
      '{"id":"r1","ringedAt":"2026-11-23T19:42:10.123Z","photoUrl":"/api/v1/rings/r1/photo"}',
    );

    expect(events).toEqual([
      {
        kind: 'restored',
        ring: {
          id: 'r1',
          ringedAt: new Date('2026-11-23T19:42:10.123Z'),
          photoUrl: '/api/v1/rings/r1/photo',
        },
      },
    ]);
  });

  it('SPEC-002 #9: opens a new connection when the browser gives up reconnecting', () => {
    const events: RingStreamEvent[] = [];
    stream.connect().subscribe((event) => events.push(event));

    sources[0].readyState = EventSource.CLOSED;
    sources[0].onerror?.();
    vi.advanceTimersByTime(STREAM_RETRY_MS);
    sources[1].onopen?.();

    expect(sources).toHaveLength(2);
    expect(sources[0].closed).toBe(true);
    expect(events.map((event) => event.kind)).toEqual(['error', 'open']);
  });

  it('SPEC-002 #9: leaves retrying to the browser while it is still reconnecting', () => {
    stream.connect().subscribe();

    sources[0].onerror?.();
    vi.advanceTimersByTime(STREAM_RETRY_MS);

    expect(sources).toHaveLength(1);
  });

  it('closes the connection when unsubscribed', () => {
    stream.connect().subscribe().unsubscribe();

    expect(sources[0].closed).toBe(true);
  });
});
