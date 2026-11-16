import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, of, throwError } from 'rxjs';

import { TrashApi } from './trash-api';
import { Keyed, TrashAccess } from './trash-access';
import { TrashPage, TrashedRing } from './trash.model';
import { TrashService } from './trash.service';

function trashed(id: string, removedIso: string): TrashedRing {
  return {
    id,
    ringedAt: new Date('2026-11-23T09:00:00Z'),
    removedAt: new Date(removedIso),
    photoUrl: `/api/v1/trash/${id}/photo`,
  };
}

class FakeTrashApi {
  readonly requestedPages: number[] = [];
  readonly restored: string[] = [];
  readonly purged: string[] = [];
  pages: TrashPage[] = [];

  fetchPage(page: number): Observable<TrashPage> {
    this.requestedPages.push(page);
    return of(this.pages.shift() ?? { rings: [], totalElements: 0 });
  }

  restore(id: string): Observable<void> {
    this.restored.push(id);
    return of(undefined);
  }

  purge(id: string): Observable<void> {
    this.purged.push(id);
    return of(undefined);
  }
}

/** Runs requests with a fixed key, or reports the key dialog as cancelled. */
class FakeAccess {
  cancelled = false;

  withKey<T>(request: (key: string) => Observable<T>): Observable<Keyed<T>> {
    if (this.cancelled) {
      return of({ kind: 'cancelled' });
    }
    return new Observable<Keyed<T>>((subscriber) =>
      request('right').subscribe({
        next: (value) => subscriber.next({ kind: 'done', value }),
        error: (error: unknown) => subscriber.error(error),
        complete: () => subscriber.complete(),
      }),
    );
  }
}

describe('TrashService', () => {
  let api: FakeTrashApi;
  let access: FakeAccess;
  let confirmAnswers: boolean[];
  let notices: string[];
  let service: TrashService;

  beforeEach(() => {
    api = new FakeTrashApi();
    access = new FakeAccess();
    confirmAnswers = [];
    notices = [];
    TestBed.configureTestingModule({
      providers: [
        { provide: TrashApi, useValue: api },
        { provide: TrashAccess, useValue: access },
        {
          provide: MatDialog,
          useValue: { open: () => ({ afterClosed: () => of(confirmAnswers.shift()) }) },
        },
        { provide: MatSnackBar, useValue: { open: (message: string) => notices.push(message) } },
      ],
    });
    service = TestBed.inject(TrashService);
  });

  it('SPEC-010 CA-11 (#10): opening loads the first page in the order the API gives', () => {
    api.pages = [
      {
        rings: [trashed('b', '2026-11-23T20:05:00Z'), trashed('a', '2026-11-23T20:01:00Z')],
        totalElements: 2,
      },
    ];

    service.open();

    expect(service.state()).toBe('ready');
    expect(service.rings().map((ring) => ring.id)).toEqual(['b', 'a']);
    expect(service.hasMore()).toBe(false);
  });

  it('SPEC-010 #7: a cancelled key dialog leaves the trash locked', () => {
    access.cancelled = true;

    service.open();

    expect(service.state()).toBe('locked');
    expect(api.requestedPages).toEqual([]);
  });

  it('SPEC-010 #8: a refused first page (for example a lockout) shows the failed state', () => {
    api.fetchPage = () => throwError(() => new Error('429'));

    service.open();

    expect(service.state()).toBe('failed');
  });

  it('SPEC-010 #11: restoring drops the ring from the trash and tells "Toque restaurado."', () => {
    api.pages = [{ rings: [trashed('a', '2026-11-23T20:01:00Z')], totalElements: 1 }];
    service.open();
    const results: boolean[] = [];

    service.restore(service.rings()[0]).subscribe((done) => results.push(done));

    expect(results).toEqual([true]);
    expect(api.restored).toEqual(['a']);
    expect(service.rings()).toEqual([]);
    expect(notices).toEqual(['Toque restaurado.']);
  });

  it('SPEC-010 CA-5 (#12): a cancelled purge confirmation deletes nothing', () => {
    api.pages = [{ rings: [trashed('a', '2026-11-23T20:01:00Z')], totalElements: 1 }];
    service.open();
    confirmAnswers = [false];

    service.purge(service.rings()[0]).subscribe();

    expect(api.purged).toEqual([]);
    expect(service.rings()).toHaveLength(1);
  });

  it('SPEC-010 #12: a confirmed purge drops the ring and tells "Toque apagado."', () => {
    api.pages = [{ rings: [trashed('a', '2026-11-23T20:01:00Z')], totalElements: 1 }];
    service.open();
    confirmAnswers = [true];

    service.purge(service.rings()[0]).subscribe();

    expect(api.purged).toEqual(['a']);
    expect(service.rings()).toEqual([]);
    expect(notices).toEqual(['Toque apagado.']);
  });

  it('SPEC-010 #11: a failed restore keeps the ring and shows no success notice', () => {
    api.pages = [{ rings: [trashed('a', '2026-11-23T20:01:00Z')], totalElements: 1 }];
    service.open();
    api.restore = () => throwError(() => new Error('down'));
    const results: boolean[] = [];

    service.restore(service.rings()[0]).subscribe((done) => results.push(done));

    expect(results).toEqual([false]);
    expect(service.rings()).toHaveLength(1);
    expect(notices).toEqual([]);
    expect(service.pending().size).toBe(0);
  });

  it('SPEC-010 #10: after a ring leaves the trash, load more fetches from one page back', () => {
    api.pages = [
      {
        rings: Array.from({ length: 20 }, (_, i) =>
          trashed('t' + i, new Date(Date.UTC(2026, 10, 23, 20, 0, 59 - i)).toISOString()),
        ),
        totalElements: 25,
      },
    ];
    service.open();

    service.restore(service.rings()[0]).subscribe();
    service.loadMore();
    service.loadMore();

    expect(api.requestedPages).toEqual([0, 0, 1]);
  });
});
