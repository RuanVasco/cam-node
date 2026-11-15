import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

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
}
