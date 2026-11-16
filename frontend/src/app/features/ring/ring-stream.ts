import { Injectable, InjectionToken, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { RINGS_URL, RingDto, toRing } from './ring-api';
import { RingStreamEvent } from './ring.model';

export const EVENT_SOURCE_FACTORY = new InjectionToken<(url: string) => EventSource>(
  'EVENT_SOURCE_FACTORY',
  {
    providedIn: 'root',
    factory: () => (url: string) => new EventSource(url),
  },
);

export const STREAM_RETRY_MS = 5000;

/** Wire format of the SPEC-010 ring-removed event. */
interface RingRemovedDto {
  readonly id: string;
}

@Injectable({ providedIn: 'root' })
export class RingStream {
  private readonly createEventSource = inject(EVENT_SOURCE_FACTORY);

  /**
   * The browser retries a dropped stream by itself, but gives up for good when a reconnect gets a non-200
   * answer (for example a 502 while the API restarts). In that case a new EventSource is opened after a pause.
   */
  connect(): Observable<RingStreamEvent> {
    return new Observable<RingStreamEvent>((subscriber) => {
      let source: EventSource | undefined;
      let retry: ReturnType<typeof setTimeout> | undefined;

      const open = (): void => {
        source = this.createEventSource(`${RINGS_URL}/stream`);
        source.onopen = () => subscriber.next({ kind: 'open' });
        source.onerror = () => {
          subscriber.next({ kind: 'error' });
          if (source?.readyState === EventSource.CLOSED) {
            source.close();
            retry = setTimeout(open, STREAM_RETRY_MS);
          }
        };
        source.addEventListener('ring', (event: MessageEvent<string>) => {
          const dto: RingDto = JSON.parse(event.data);
          subscriber.next({ kind: 'ring', ring: toRing(dto) });
        });
        source.addEventListener('ring-removed', (event: MessageEvent<string>) => {
          const dto: RingRemovedDto = JSON.parse(event.data);
          subscriber.next({ kind: 'removed', id: dto.id });
        });
      };

      open();
      return () => {
        clearTimeout(retry);
        source?.close();
      };
    });
  }
}
