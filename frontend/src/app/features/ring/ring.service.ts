import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';

import { RingApi } from './ring-api';
import { RingStream } from './ring-stream';
import { ConnectionStatus, Ring, RingPage } from './ring.model';

/** Rule: SPEC-002 #8. */
export const HIGHLIGHT_MS = 10_000;

/**
 * History state for the screen: pages from the API merged with rings pushed by the stream.
 *
 * Rings are kept unique by id and newest first. A live ring shifts the server pages by one, so the next page may
 * repeat rings already shown; deduplication absorbs that without leaving gaps.
 */
@Injectable({ providedIn: 'root' })
export class RingService {
  private readonly api = inject(RingApi);
  private readonly stream = inject(RingStream);
  private readonly destroyRef = inject(DestroyRef);

  private readonly ringList = signal<readonly Ring[]>([]);
  private readonly total = signal(0);
  private readonly highlightedIds = signal<ReadonlySet<string>>(new Set());
  private readonly status = signal<ConnectionStatus>('connecting');
  private readonly firstPageLoaded = signal(false);
  private readonly fetchingMore = signal(false);
  private nextPage = 0;
  private started = false;
  private droppedSinceOpen = false;

  readonly rings = this.ringList.asReadonly();
  readonly highlighted = this.highlightedIds.asReadonly();
  readonly connection = this.status.asReadonly();
  readonly loaded = this.firstPageLoaded.asReadonly();
  readonly loadingMore = this.fetchingMore.asReadonly();
  readonly hasMore = computed(() => this.ringList().length < this.total());

  start(): void {
    if (this.started) {
      return;
    }
    this.started = true;
    this.loadFirstPage();
    const subscription = this.stream.connect().subscribe((event) => {
      switch (event.kind) {
        case 'open':
          this.status.set('live');
          // Rule: SPEC-002 #9. Rings registered while the stream was down never reach this client as events.
          if (this.droppedSinceOpen) {
            this.droppedSinceOpen = false;
            this.loadFirstPage();
          }
          break;
        case 'error':
          this.status.set('reconnecting');
          this.droppedSinceOpen = true;
          break;
        case 'ring':
          this.addLive(event.ring);
          break;
      }
    });
    this.destroyRef.onDestroy(() => subscription.unsubscribe());
  }

  loadMore(): void {
    if (this.fetchingMore()) {
      return;
    }
    this.fetchingMore.set(true);
    this.api.fetchPage(this.nextPage).subscribe({
      next: (page) => {
        this.nextPage++;
        this.merge(page);
      },
      complete: () => this.fetchingMore.set(false),
      error: () => this.fetchingMore.set(false),
    });
  }

  private loadFirstPage(): void {
    this.api.fetchPage(0).subscribe({
      next: (page) => {
        this.nextPage = Math.max(this.nextPage, 1);
        this.merge(page);
        this.firstPageLoaded.set(true);
      },
      // The interceptor already told the user; the next stream open retries the page.
      error: () => (this.droppedSinceOpen = true),
    });
  }

  private merge(page: RingPage): void {
    this.total.set(page.totalElements);
    this.ringList.update((current) => mergeNewestFirst(current, page.rings));
  }

  private addLive(ring: Ring): void {
    if (this.ringList().some((existing) => existing.id === ring.id)) {
      return;
    }
    this.ringList.update((current) => mergeNewestFirst(current, [ring]));
    this.total.update((total) => total + 1);
    this.highlightedIds.update((ids) => new Set(ids).add(ring.id));
    setTimeout(() => {
      this.highlightedIds.update((ids) => {
        const next = new Set(ids);
        next.delete(ring.id);
        return next;
      });
    }, HIGHLIGHT_MS);
  }
}

function mergeNewestFirst(current: readonly Ring[], incoming: readonly Ring[]): Ring[] {
  const byId = new Map(current.map((ring) => [ring.id, ring]));
  for (const ring of incoming) {
    byId.set(ring.id, ring);
  }
  return [...byId.values()].sort((a, b) => b.ringedAt.getTime() - a.ringedAt.getTime());
}
