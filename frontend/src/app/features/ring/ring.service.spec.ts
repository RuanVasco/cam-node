import { TestBed } from '@angular/core/testing';
import { Observable, Subject, of, throwError } from 'rxjs';

import { RingApi } from './ring-api';
import { RingStream } from './ring-stream';
import { Ring, RingPage, RingStreamEvent } from './ring.model';
import { HIGHLIGHT_MS, RingService } from './ring.service';

function ringAt(id: string, iso: string): Ring {
  return { id, ringedAt: new Date(iso), photoUrl: `/api/v1/rings/${id}/photo` };
}

class FakeRingApi {
  readonly requestedPages: number[] = [];
  readonly removals: (readonly string[])[] = [];
  pages: RingPage[] = [];

  remove(ids: readonly string[]): Observable<void> {
    this.removals.push(ids);
    return of(undefined);
  }

  fetchPage(page: number): Observable<RingPage> {
    this.requestedPages.push(page);
    return of(this.pages.shift() ?? { rings: [], totalElements: 0 });
  }
}

class FakeRingStream {
  readonly events = new Subject<RingStreamEvent>();

  connect(): Observable<RingStreamEvent> {
    return this.events;
  }
}

describe('RingService', () => {
  let api: FakeRingApi;
  let stream: FakeRingStream;
  let service: RingService;

  beforeEach(() => {
    vi.useFakeTimers();
    api = new FakeRingApi();
    stream = new FakeRingStream();
    TestBed.configureTestingModule({
      providers: [
        { provide: RingApi, useValue: api },
        { provide: RingStream, useValue: stream },
      ],
    });
    service = TestBed.inject(RingService);
  });

  afterEach(() => vi.useRealTimers());

  it('SPEC-002 CA-8 (#8): a live ring goes to the top and is highlighted for 10 s', () => {
    api.pages = [{ rings: [ringAt('old', '2026-11-23T10:00:00Z')], totalElements: 1 }];
    service.start();

    stream.events.next({ kind: 'ring', ring: ringAt('new', '2026-11-23T11:00:00Z') });

    expect(service.rings().map((ring) => ring.id)).toEqual(['new', 'old']);
    expect(service.highlighted().has('new')).toBe(true);

    vi.advanceTimersByTime(HIGHLIGHT_MS - 1);
    expect(service.highlighted().has('new')).toBe(true);

    vi.advanceTimersByTime(1);
    expect(service.highlighted().has('new')).toBe(false);
  });

  it('SPEC-002 CA-9 (#9): reopening the stream after a drop reloads the first page', () => {
    api.pages = [
      { rings: [ringAt('a', '2026-11-23T10:00:00Z')], totalElements: 1 },
      {
        rings: [ringAt('during-drop', '2026-11-23T10:30:00Z'), ringAt('a', '2026-11-23T10:00:00Z')],
        totalElements: 2,
      },
    ];
    service.start();
    stream.events.next({ kind: 'open' });

    stream.events.next({ kind: 'error' });
    expect(service.connection()).toBe('reconnecting');
    stream.events.next({ kind: 'open' });

    expect(api.requestedPages).toEqual([0, 0]);
    expect(service.connection()).toBe('live');
    expect(service.rings().map((ring) => ring.id)).toEqual(['during-drop', 'a']);
  });

  it('SPEC-002 #9: a first page that failed to load is fetched again when the stream opens', () => {
    let calls = 0;
    api.fetchPage = (page: number) => {
      api.requestedPages.push(page);
      calls++;
      return calls === 1
        ? throwError(() => new Error('offline'))
        : of({ rings: [], totalElements: 0 });
    };
    service.start();
    expect(service.loaded()).toBe(false);

    stream.events.next({ kind: 'open' });

    expect(api.requestedPages).toEqual([0, 0]);
    expect(service.loaded()).toBe(true);
  });

  it('SPEC-002 #9: the first open of the stream does not reload the page', () => {
    service.start();

    stream.events.next({ kind: 'open' });

    expect(api.requestedPages).toEqual([0]);
  });

  it('SPEC-002 #2: load more asks for the next page and skips rings already shown', () => {
    api.pages = [
      { rings: [ringAt('b', '2026-11-23T10:00:00Z')], totalElements: 2 },
      {
        rings: [ringAt('b', '2026-11-23T10:00:00Z'), ringAt('a', '2026-11-23T09:00:00Z')],
        totalElements: 2,
      },
    ];
    service.start();
    expect(service.hasMore()).toBe(true);

    service.loadMore();

    expect(api.requestedPages).toEqual([0, 1]);
    expect(service.rings().map((ring) => ring.id)).toEqual(['b', 'a']);
    expect(service.hasMore()).toBe(false);
  });

  it('SPEC-010 #9: a removal drops the rings and lowers the total', () => {
    api.pages = [
      {
        rings: [ringAt('b', '2026-11-23T10:00:00Z'), ringAt('a', '2026-11-23T09:00:00Z')],
        totalElements: 3,
      },
    ];
    service.start();

    service.remove(['b']).subscribe();

    expect(api.removals).toEqual([['b']]);
    expect(service.rings().map((ring) => ring.id)).toEqual(['a']);
    expect(service.hasMore()).toBe(true);
  });

  it('SPEC-010 #4: the own removal and its stream event lower the total once, in either order', () => {
    api.pages = [
      {
        rings: [ringAt('b', '2026-11-23T10:00:00Z'), ringAt('a', '2026-11-23T09:00:00Z')],
        totalElements: 2,
      },
    ];
    service.start();

    stream.events.next({ kind: 'removed', id: 'b' });
    service.remove(['b']).subscribe();

    expect(service.rings().map((ring) => ring.id)).toEqual(['a']);
    expect(service.hasMore()).toBe(false);
  });

  it('SPEC-010 #4: a ring removed on another screen leaves the list', () => {
    api.pages = [{ rings: [ringAt('a', '2026-11-23T09:00:00Z')], totalElements: 1 }];
    service.start();

    stream.events.next({ kind: 'removed', id: 'a' });

    expect(service.rings()).toEqual([]);
  });

  it('SPEC-010 #2: a page fetched before the removal does not bring the ring back', () => {
    api.pages = [
      { rings: [ringAt('a', '2026-11-23T09:00:00Z')], totalElements: 1 },
      { rings: [ringAt('a', '2026-11-23T09:00:00Z')], totalElements: 1 },
    ];
    service.start();
    stream.events.next({ kind: 'removed', id: 'a' });

    stream.events.next({ kind: 'error' });
    stream.events.next({ kind: 'open' });

    expect(service.rings()).toEqual([]);
  });

  it('SPEC-010 #2: after removing a loaded ring, load more fetches from one page back', () => {
    api.pages = [
      {
        rings: Array.from({ length: 20 }, (_, i) =>
          ringAt('r' + i, new Date(Date.UTC(2026, 10, 23, 12, 0, 59 - i)).toISOString()),
        ),
        totalElements: 25,
      },
    ];
    service.start();

    service.remove(['r0']).subscribe();
    service.loadMore();
    service.loadMore();

    expect(api.requestedPages).toEqual([0, 0, 1]);
  });

  it('SPEC-010 #4: a ring removed elsewhere while the stream was down leaves the list on reconnect', () => {
    api.pages = [
      {
        rings: [ringAt('b', '2026-11-23T10:00:00Z'), ringAt('a', '2026-11-23T09:00:00Z')],
        totalElements: 2,
      },
      { rings: [ringAt('a', '2026-11-23T09:00:00Z')], totalElements: 1 },
    ];
    service.start();
    stream.events.next({ kind: 'open' });

    stream.events.next({ kind: 'error' });
    stream.events.next({ kind: 'open' });

    expect(service.rings().map((ring) => ring.id)).toEqual(['a']);
  });

  it('SPEC-010 #2: a failed load more keeps the shift from earlier removals', () => {
    api.pages = [
      {
        rings: Array.from({ length: 20 }, (_, i) =>
          ringAt('r' + i, new Date(Date.UTC(2026, 10, 23, 12, 0, 59 - i)).toISOString()),
        ),
        totalElements: 25,
      },
    ];
    service.start();
    service.remove(['r0']).subscribe();
    const fetchPage = api.fetchPage.bind(api);
    api.fetchPage = (page: number) => {
      api.requestedPages.push(page);
      return throwError(() => new Error('offline'));
    };
    service.loadMore();
    api.fetchPage = fetchPage;

    service.loadMore();

    expect(api.requestedPages).toEqual([0, 0, 0]);
  });

  it('SPEC-010 CA-4 (#11): a restored ring goes back to the place of its time, not highlighted', () => {
    api.pages = [
      {
        rings: [ringAt('c', '2026-11-23T11:00:00Z'), ringAt('a', '2026-11-23T09:00:00Z')],
        totalElements: 2,
      },
    ];
    service.start();

    stream.events.next({ kind: 'restored', ring: ringAt('b', '2026-11-23T10:00:00Z') });

    expect(service.rings().map((ring) => ring.id)).toEqual(['c', 'b', 'a']);
    expect(service.highlighted().size).toBe(0);
    expect(service.hasMore()).toBe(false);
  });

  it('SPEC-010 #11: a ring removed here and then restored elsewhere comes back once', () => {
    api.pages = [{ rings: [ringAt('a', '2026-11-23T09:00:00Z')], totalElements: 1 }];
    service.start();
    service.remove(['a']).subscribe();

    stream.events.next({ kind: 'restored', ring: ringAt('a', '2026-11-23T09:00:00Z') });
    stream.events.next({ kind: 'restored', ring: ringAt('a', '2026-11-23T09:00:00Z') });

    expect(service.rings().map((ring) => ring.id)).toEqual(['a']);
    expect(service.hasMore()).toBe(false);
  });

  it('SPEC-010 #11: a restored ring older than the loaded pages waits for load more', () => {
    api.pages = [
      { rings: [ringAt('b', '2026-11-23T10:00:00Z')], totalElements: 2 },
      {
        rings: [ringAt('a', '2026-11-23T09:00:00Z'), ringAt('old', '2026-11-20T09:00:00Z')],
        totalElements: 3,
      },
    ];
    service.start();

    stream.events.next({ kind: 'restored', ring: ringAt('old', '2026-11-20T09:00:00Z') });
    expect(service.rings().map((ring) => ring.id)).toEqual(['b']);

    service.loadMore();
    expect(service.rings().map((ring) => ring.id)).toEqual(['b', 'a', 'old']);
    expect(service.hasMore()).toBe(false);
  });

  it('SPEC-002 #4: the same ring arriving twice is shown once', () => {
    service.start();
    const ring = ringAt('x', '2026-11-23T10:00:00Z');

    stream.events.next({ kind: 'ring', ring });
    stream.events.next({ kind: 'ring', ring });

    expect(service.rings()).toHaveLength(1);
  });
});
