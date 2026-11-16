import { Injectable, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, catchError, defer, finalize, map, of, switchMap, tap } from 'rxjs';

import { MESSAGES } from '../../core/i18n/messages';
import { AdminKeyDialog, AdminKeyDialogData } from './admin-key-dialog/admin-key-dialog';
import { AdminKeyStore } from './admin-key.store';
import {
  RingRemovalConfirmData,
  RingRemovalConfirmDialog,
} from './ring-removal-confirm-dialog/ring-removal-confirm-dialog';
import { InvalidAdminKeyError } from './ring.model';
import { RingService } from './ring.service';

/** Confirmation, admin key and removal, as one flow for the screens that offer "Remover". */
@Injectable({ providedIn: 'root' })
export class RingRemoval {
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly rings = inject(RingService);
  private readonly adminKeys = inject(AdminKeyStore);
  private readonly running = signal(false);

  /** True while a removal flow is open, from the confirmation to the answer. */
  readonly busy = this.running.asReadonly();

  /**
   * Emits true once the rings are removed, false when the user gives up or the removal fails. A second call while
   * one flow is open is ignored, so a double click does not stack dialogs or send the key twice.
   */
  remove(ids: readonly string[]): Observable<boolean> {
    return defer(() => {
      if (this.running()) {
        return of(false);
      }
      this.running.set(true);
      return this.flow(ids).pipe(finalize(() => this.running.set(false)));
    });
  }

  private flow(ids: readonly string[]): Observable<boolean> {
    return this.confirm(ids.length).pipe(
      switchMap((confirmed) =>
        confirmed ? this.removeWithKey(ids, this.adminKeys.get(), null) : of(false),
      ),
      tap((removed) => {
        if (removed) {
          const message =
            ids.length === 1
              ? MESSAGES.removal.removedOne
              : MESSAGES.removal.removedMany(ids.length);
          this.snackBar.open(message, MESSAGES.errors.dismiss, { duration: 4000 });
        }
      }),
    );
  }

  // Rule: SPEC-010 #5.
  private confirm(count: number): Observable<boolean> {
    return this.dialog
      .open<RingRemovalConfirmDialog, RingRemovalConfirmData, boolean>(RingRemovalConfirmDialog, {
        data: { count },
      })
      .afterClosed()
      .pipe(map((confirmed) => confirmed === true));
  }

  // Rule: SPEC-010 #7. The key is asked only when none is kept for this tab, and kept only after it worked;
  // a rejected key is forgotten and asked again.
  private removeWithKey(
    ids: readonly string[],
    key: string | null,
    error: string | null,
  ): Observable<boolean> {
    if (key === null) {
      return this.askKey(error).pipe(
        switchMap((typed) => (typed ? this.removeWithKey(ids, typed, null) : of(false))),
      );
    }
    return this.rings.remove(ids, key).pipe(
      tap(() => this.adminKeys.set(key)),
      map(() => true),
      catchError((failure: unknown) => {
        if (failure instanceof InvalidAdminKeyError) {
          this.adminKeys.clear();
          return this.removeWithKey(ids, null, MESSAGES.adminKey.wrong);
        }
        // Any other failure was already told to the user by the HTTP interceptor.
        return of(false);
      }),
    );
  }

  private askKey(error: string | null): Observable<string | undefined> {
    return this.dialog
      .open<AdminKeyDialog, AdminKeyDialogData, string>(AdminKeyDialog, { data: { error } })
      .afterClosed();
  }
}
