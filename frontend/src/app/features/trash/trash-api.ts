import { HttpClient, HttpContext, HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of, throwError } from 'rxjs';

import { API_ERROR_MESSAGE, CALLER_HANDLED_STATUSES } from '../../core/http/api-error.interceptor';
import { MESSAGES } from '../../core/i18n/messages';
import { InvalidAdminKeyError, TrashPage, TrashedRing } from './trash.model';

export const TRASH_URL = '/api/v1/trash';
export const TRASH_PAGE_SIZE = 20;
const ADMIN_KEY_HEADER = 'X-Admin-Key';

/** Wire format of SPEC-010; dates arrive as ISO-8601 strings. */
interface TrashedRingDto {
  readonly id: string;
  readonly ringedAt: string;
  readonly removedAt: string;
  readonly photoUrl: string;
}

interface TrashPageDto {
  readonly content: readonly TrashedRingDto[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
}

function toTrashedRing(dto: TrashedRingDto): TrashedRing {
  return {
    id: dto.id,
    ringedAt: new Date(dto.ringedAt),
    removedAt: new Date(dto.removedAt),
    photoUrl: dto.photoUrl,
  };
}

/** Every call carries the admin key (SPEC-010 #7) and fails with InvalidAdminKeyError on a 401. */
@Injectable({ providedIn: 'root' })
export class TrashApi {
  private readonly http = inject(HttpClient);

  fetchPage(page: number, adminKey: string): Observable<TrashPage> {
    return this.http
      .get<TrashPageDto>(TRASH_URL, {
        params: { page, size: TRASH_PAGE_SIZE },
        ...this.options(adminKey, MESSAGES.errors.generic, []),
      })
      .pipe(
        map((dto) => ({ rings: dto.content.map(toTrashedRing), totalElements: dto.totalElements })),
        catchError(rejectInvalidKey),
      );
  }

  /** The key travels in a header, which an image tag can not send, so the photo comes as a blob. */
  photo(photoUrl: string, adminKey: string): Observable<Blob> {
    return this.http
      .get(photoUrl, {
        responseType: 'blob',
        ...this.options(adminKey, MESSAGES.errors.generic, []),
      })
      .pipe(catchError(rejectInvalidKey));
  }

  restore(id: string, adminKey: string): Observable<void> {
    return this.settle(
      this.http.post<void>(`${TRASH_URL}/${id}/restore`, null, this.actionOptions(adminKey)),
    );
  }

  purge(id: string, adminKey: string): Observable<void> {
    return this.settle(this.http.delete<void>(`${TRASH_URL}/${id}`, this.actionOptions(adminKey)));
  }

  // A ring already out of the trash (404) was restored or purged on another screen: the goal is met either way.
  private settle(request: Observable<void>): Observable<void> {
    return request.pipe(
      map(() => undefined),
      catchError((error: unknown) =>
        error instanceof HttpErrorResponse && error.status === HttpStatusCode.NotFound
          ? of(undefined)
          : rejectInvalidKey(error),
      ),
    );
  }

  private actionOptions(adminKey: string) {
    return this.options(adminKey, MESSAGES.errors.trashActionFailed, [HttpStatusCode.NotFound]);
  }

  private options(adminKey: string, failureMessage: string, alsoHandled: readonly number[]) {
    return {
      headers: { [ADMIN_KEY_HEADER]: adminKey },
      context: new HttpContext()
        .set(API_ERROR_MESSAGE, failureMessage)
        .set(CALLER_HANDLED_STATUSES, [HttpStatusCode.Unauthorized, ...alsoHandled]),
    };
  }
}

function rejectInvalidKey(error: unknown): Observable<never> {
  return throwError(() =>
    error instanceof HttpErrorResponse && error.status === HttpStatusCode.Unauthorized
      ? new InvalidAdminKeyError()
      : error,
  );
}
