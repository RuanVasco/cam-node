import { HttpClient, HttpContext, HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of, throwError } from 'rxjs';

import { API_ERROR_MESSAGE, CALLER_HANDLED_STATUSES } from '../../core/http/api-error.interceptor';
import { MESSAGES } from '../../core/i18n/messages';
import { Ring, RingPage } from './ring.model';

export const RINGS_URL = '/api/v1/rings';
export const PAGE_SIZE = 20;

/** Wire format of SPEC-002; dates arrive as ISO-8601 strings. */
export interface RingDto {
  readonly id: string;
  readonly ringedAt: string;
  readonly photoUrl: string;
}

interface RingPageDto {
  readonly content: readonly RingDto[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
}

export function toRing(dto: RingDto): Ring {
  return { id: dto.id, ringedAt: new Date(dto.ringedAt), photoUrl: dto.photoUrl };
}

@Injectable({ providedIn: 'root' })
export class RingApi {
  private readonly http = inject(HttpClient);

  fetchPage(page: number): Observable<RingPage> {
    return this.http
      .get<RingPageDto>(RINGS_URL, { params: { page, size: PAGE_SIZE } })
      .pipe(map((dto) => ({ rings: dto.content.map(toRing), totalElements: dto.totalElements })));
  }

  /**
   * Rule: SPEC-010 #6, #9. Public, no key. A single ring that is already gone (404) counts as removed: another
   * screen got there first, and this one only missed the event.
   */
  remove(ids: readonly string[]): Observable<void> {
    const single = ids.length === 1;
    const options = {
      context: new HttpContext()
        .set(API_ERROR_MESSAGE, MESSAGES.errors.removalFailed)
        .set(CALLER_HANDLED_STATUSES, single ? [HttpStatusCode.NotFound] : []),
    };
    const request = single
      ? this.http.delete<void>(`${RINGS_URL}/${ids[0]}`, options)
      : this.http.post<void>(`${RINGS_URL}/removal`, { ids }, options);
    return request.pipe(
      map(() => undefined),
      catchError((error: unknown) =>
        single && error instanceof HttpErrorResponse && error.status === HttpStatusCode.NotFound
          ? of(undefined)
          : throwError(() => error),
      ),
    );
  }
}
