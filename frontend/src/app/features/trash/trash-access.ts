import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable, catchError, map, of, switchMap, tap, throwError } from 'rxjs';

import { MESSAGES } from '../../core/i18n/messages';
import { AdminKeyDialog, AdminKeyDialogData } from './admin-key-dialog/admin-key-dialog';
import { AdminKeyStore } from './admin-key.store';
import { InvalidAdminKeyError } from './trash.model';

export type Keyed<T> =
  { readonly kind: 'done'; readonly value: T } | { readonly kind: 'cancelled' };

/**
 * Rule: SPEC-010 #7. Runs a trash request with the key kept for this tab, asking for it when there is none. The key
 * is kept only after a request accepts it; a rejected key is forgotten and asked again with "Senha incorreta.".
 */
@Injectable({ providedIn: 'root' })
export class TrashAccess {
  private readonly dialog = inject(MatDialog);
  private readonly keys = inject(AdminKeyStore);

  withKey<T>(request: (adminKey: string) => Observable<T>): Observable<Keyed<T>> {
    return this.attempt(request, this.keys.get(), null);
  }

  private attempt<T>(
    request: (adminKey: string) => Observable<T>,
    key: string | null,
    error: string | null,
  ): Observable<Keyed<T>> {
    if (key === null) {
      return this.askKey(error).pipe(
        switchMap((typed) =>
          typed ? this.attempt(request, typed, null) : of<Keyed<T>>({ kind: 'cancelled' }),
        ),
      );
    }
    return request(key).pipe(
      tap(() => this.keys.set(key)),
      map((value): Keyed<T> => ({ kind: 'done', value })),
      catchError((failure: unknown) => {
        if (failure instanceof InvalidAdminKeyError) {
          this.keys.clear();
          return this.attempt(request, null, MESSAGES.adminKey.wrong);
        }
        return throwError(() => failure);
      }),
    );
  }

  private askKey(error: string | null): Observable<string | undefined> {
    return this.dialog
      .open<AdminKeyDialog, AdminKeyDialogData, string>(AdminKeyDialog, { data: { error } })
      .afterClosed();
  }
}
