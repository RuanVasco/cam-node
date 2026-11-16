import { Injectable, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, catchError, defer, finalize, map, of, switchMap, tap } from 'rxjs';

import { MESSAGES } from '../../core/i18n/messages';
import { ConfirmDialog, ConfirmDialogData } from '../../shared/confirm-dialog/confirm-dialog';
import { RingService } from './ring.service';

/** Confirmation and removal, as one flow for the screens that offer "Excluir". No key: SPEC-010 #9. */
@Injectable({ providedIn: 'root' })
export class RingRemoval {
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly rings = inject(RingService);
  private readonly running = signal(false);

  /** True while a removal flow is open, from the confirmation to the answer. */
  readonly busy = this.running.asReadonly();

  /**
   * Emits true once the rings are removed, false when the user gives up or the removal fails. A second call while
   * one flow is open is ignored, so a double click does not stack dialogs.
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
        confirmed
          ? this.rings.remove(ids).pipe(
              map(() => true),
              // Any failure was already told to the user by the HTTP interceptor.
              catchError(() => of(false)),
            )
          : of(false),
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
      .open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, {
        data: {
          question: count === 1 ? MESSAGES.removal.confirmOne : MESSAGES.removal.confirmMany(count),
          cancel: MESSAGES.removal.cancel,
          confirm: MESSAGES.removal.confirm,
        },
      })
      .afterClosed()
      .pipe(map((confirmed) => confirmed === true));
  }
}
