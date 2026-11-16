import { Injectable, computed, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EMPTY, Observable, catchError, defer, finalize, map, of, switchMap } from 'rxjs';

import { MESSAGES } from '../../core/i18n/messages';
import { ConfirmDialog, ConfirmDialogData } from '../../shared/confirm-dialog/confirm-dialog';
import { TRASH_PAGE_SIZE, TrashApi } from './trash-api';
import { TrashAccess } from './trash-access';
import { TrashPage, TrashState, TrashedRing } from './trash.model';

/**
 * State of the trash page (SPEC-010 #10) and its actions (#11, #12). The trash has no stream, so a ring that leaves
 * it is dropped here when this screen restores or purges it, and the paging is shifted back the same way the
 * history does.
 */
@Injectable({ providedIn: 'root' })
export class TrashService {
  private readonly api = inject(TrashApi);
  private readonly access = inject(TrashAccess);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  private readonly ringList = signal<readonly TrashedRing[]>([]);
  private readonly total = signal(0);
  private readonly currentState = signal<TrashState>('loading');
  private readonly fetchingMore = signal(false);
  private readonly pendingIds = signal<ReadonlySet<string>>(new Set());
  private nextPage = 0;
  private droppedSinceFetch = 0;

  readonly rings = this.ringList.asReadonly();
  readonly state = this.currentState.asReadonly();
  readonly loadingMore = this.fetchingMore.asReadonly();
  readonly pending = this.pendingIds.asReadonly();
  readonly hasMore = computed(() => this.ringList().length < this.total());

  /** Loads the first page, asking for the key if this tab has none. */
  open(): void {
    this.currentState.set('loading');
    this.access
      .withKey((key) => this.api.fetchPage(0, key))
      .subscribe({
        next: (result) => {
          if (result.kind === 'cancelled') {
            this.currentState.set('locked');
            return;
          }
          this.nextPage = 1;
          this.droppedSinceFetch = 0;
          this.ringList.set(result.value.rings);
          this.total.set(result.value.totalElements);
          this.currentState.set('ready');
        },
        // The interceptor already told the user what failed.
        error: () => this.currentState.set('failed'),
      });
  }

  loadMore(): void {
    if (this.fetchingMore()) {
      return;
    }
    this.fetchingMore.set(true);
    const shift = this.droppedSinceFetch;
    const page = Math.max(0, this.nextPage - Math.ceil(shift / TRASH_PAGE_SIZE));
    this.droppedSinceFetch = 0;
    this.access
      .withKey((key) => this.api.fetchPage(page, key))
      .pipe(finalize(() => this.fetchingMore.set(false)))
      .subscribe({
        next: (result) => {
          if (result.kind === 'done') {
            this.nextPage = page + 1;
            this.merge(result.value);
          } else {
            this.droppedSinceFetch += shift;
          }
        },
        error: () => (this.droppedSinceFetch += shift),
      });
  }

  /** The photo as a blob, since the key can not go in an image tag. Completes empty if the key is not given. */
  photo(ring: TrashedRing): Observable<Blob> {
    return this.access
      .withKey((key) => this.api.photo(ring.photoUrl, key))
      .pipe(switchMap((result) => (result.kind === 'done' ? of(result.value) : EMPTY)));
  }

  /** Rule: SPEC-010 #11. Emits true once the ring is back in the history. */
  restore(ring: TrashedRing): Observable<boolean> {
    return this.act(ring, (key) => this.api.restore(ring.id, key), MESSAGES.trash.restored);
  }

  /** Rule: SPEC-010 #12. Asks for confirmation first; emits true once the ring is gone for good. */
  purge(ring: TrashedRing): Observable<boolean> {
    return this.dialog
      .open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, {
        data: {
          question: MESSAGES.trash.confirmPurge,
          cancel: MESSAGES.trash.cancel,
          confirm: MESSAGES.trash.confirmPurgeAction,
        },
      })
      .afterClosed()
      .pipe(
        switchMap((confirmed) =>
          confirmed === true
            ? this.act(ring, (key) => this.api.purge(ring.id, key), MESSAGES.trash.purged)
            : of(false),
        ),
      );
  }

  // One action per ring at a time, so a double click does not send it twice.
  private act(
    ring: TrashedRing,
    request: (key: string) => Observable<void>,
    done: string,
  ): Observable<boolean> {
    return defer(() => {
      if (this.pendingIds().has(ring.id)) {
        return of(false);
      }
      this.pendingIds.update((ids) => new Set(ids).add(ring.id));
      return this.access.withKey(request).pipe(
        map((result) => {
          if (result.kind === 'cancelled') {
            return false;
          }
          this.drop(ring.id);
          this.snackBar.open(done, MESSAGES.errors.dismiss, { duration: 4000 });
          return true;
        }),
        // Any failure other than the key was already told to the user by the HTTP interceptor.
        catchError(() => of(false)),
        finalize(() =>
          this.pendingIds.update((ids) => {
            const next = new Set(ids);
            next.delete(ring.id);
            return next;
          }),
        ),
      );
    });
  }

  private merge(page: TrashPage): void {
    this.total.set(page.totalElements);
    this.ringList.update((current) => {
      const known = new Set(current.map((ring) => ring.id));
      return [...current, ...page.rings.filter((ring) => !known.has(ring.id))];
    });
  }

  private drop(id: string): void {
    const before = this.ringList();
    const after = before.filter((ring) => ring.id !== id);
    this.droppedSinceFetch += before.length - after.length;
    this.ringList.set(after);
    this.total.update((total) => Math.max(0, total - 1));
  }
}
