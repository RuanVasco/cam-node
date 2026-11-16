import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { PAGE_SIZE, RingApi } from './ring-api';
import { RingStream } from './ring-stream';
import { ConnectionStatus, Ring, RingPage } from './ring.model';

/** Rule: SPEC-002 #8. */
export const HIGHLIGHT_MS = 10_000;

/**
 * History state for the screen: pages from the API merged with rings pushed by the stream.
 *
 * Rings are kept unique by id and newest first. A live ring shifts the server pages by one, so the next page may
 * repeat rings already shown; deduplication absorbs that without leaving gaps.
 *
 * A removal reaches this client twice, as the answer to its own request and as a stream event, in either order.
 * Removed ids are remembered so the total drops once and a page fetched before the removal does not bring it back.
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
  private readonly removedIds = new Set<string>();
  private nextPage = 0;
  // Loaded rings removed since the last page fetch; each one moves later rings one place back on the server.
  private removedSinceFetch = 0;
  private readonly liveDuringFetch = new Set<string>();
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
        case 'removed':
          this.drop([event.id]);
          break;
        case 'restored':
          this.restore(event.ring);
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
    // Rule: SPEC-010 #2. Fetching from as many pages back as removals shifted keeps a ring from falling between pages.
    const shift = this.removedSinceFetch;
    const page = Math.max(0, this.nextPage - Math.ceil(shift / PAGE_SIZE));
    this.removedSinceFetch = 0;
    this.api.fetchPage(page).subscribe({
      next: (result) => {
        this.nextPage = page + 1;
        this.merge(result);
      },
      complete: () => this.fetchingMore.set(false),
      error: () => {
        this.removedSinceFetch += shift;
        this.fetchingMore.set(false);
      },
    });
  }

  // Rule: SPEC-010 #9.
  remove(ids: readonly string[]): Observable<void> {
    return this.api.remove(ids).pipe(tap(() => this.drop(ids)));
  }

  /**
   * Starts the list over from the first page. After a stream drop this is the only way to learn about rings removed
   * meanwhile (SPEC-010 #4), and it also resets the paging, which removals may have shifted. Rings pushed live
   * while the page was in flight are kept, since the server may have answered before they existed.
   */
  private loadFirstPage(): void {
    this.liveDuringFetch.clear();
    this.api.fetchPage(0).subscribe({
      next: (page) => {
        const kept = page.rings.filter((ring) => !this.removedIds.has(ring.id));
        const newer = this.ringList().filter((ring) => this.liveDuringFetch.has(ring.id));
        this.nextPage = 1;
        this.removedSinceFetch = 0;
        this.total.set(page.totalElements);
        this.ringList.set(mergeNewestFirst(newer, kept));
        this.firstPageLoaded.set(true);
      },
      // The interceptor already told the user; the next stream open retries the page.
      error: () => (this.droppedSinceOpen = true),
    });
  }

  private merge(page: RingPage): void {
    this.total.set(page.totalElements);
    const kept = page.rings.filter((ring) => !this.removedIds.has(ring.id));
    this.ringList.update((current) => mergeNewestFirst(current, kept));
  }

  // Rule: SPEC-010 #4.
  private drop(ids: readonly string[]): void {
    const fresh = ids.filter((id) => !this.removedIds.has(id));
    if (fresh.length === 0) {
      return;
    }
    fresh.forEach((id) => this.removedIds.add(id));
    const gone = new Set(fresh);
    const before = this.ringList();
    const after = before.filter((ring) => !gone.has(ring.id));
    this.removedSinceFetch += before.length - after.length;
    this.ringList.set(after);
    this.total.update((total) => Math.max(0, total - fresh.length));
  }

  /**
   * Rule: SPEC-010 #11. Goes back to the place of its ring time, not to the top, and is not highlighted: it is not a
   * new ring. Its id leaves the removed set so later pages show it again. A ring older than everything loaded,
   * with more pages to fetch, is left for "Carregar mais", which reaches it in order.
   */
  private restore(ring: Ring): void {
    this.removedIds.delete(ring.id);
    const current = this.ringList();
    if (current.some((existing) => existing.id === ring.id)) {
      return;
    }
    const oldest = current.at(-1);
    const beyondLoaded = this.hasMore() && oldest !== undefined && ring.ringedAt < oldest.ringedAt;
    this.total.update((total) => total + 1);
    if (!beyondLoaded) {
      this.ringList.set(mergeNewestFirst(current, [ring]));
    }
  }

  private addLive(ring: Ring): void {
    if (
      this.removedIds.has(ring.id) ||
      this.ringList().some((existing) => existing.id === ring.id)
    ) {
      return;
    }
    this.liveDuringFetch.add(ring.id);
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
