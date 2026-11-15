import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

import { MESSAGES } from '../../../core/i18n/messages';
import { RingPhotoDialog } from '../ring-photo-dialog/ring-photo-dialog';
import { groupByDay, timeLabel } from '../ring-time';
import { Ring } from '../ring.model';
import { RingService } from '../ring.service';

const CLOCK_TICK_MS = 60_000;

@Component({
  selector: 'app-ring-history',
  imports: [MatButton, MatIcon, MatProgressSpinner],
  templateUrl: './ring-history.html',
  styleUrl: './ring-history.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RingHistory implements OnInit {
  private readonly service = inject(RingService);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);
  // Ticks every minute so "Hoje" turns into "Ontem" after midnight without a reload.
  private readonly now = signal(new Date());

  protected readonly messages = MESSAGES;
  protected readonly loaded = this.service.loaded;
  protected readonly hasMore = this.service.hasMore;
  protected readonly loadingMore = this.service.loadingMore;
  protected readonly isEmpty = computed(
    () => this.service.loaded() && this.service.rings().length === 0,
  );
  protected readonly days = computed(() =>
    groupByDay(this.service.rings(), this.now()).map((day) => ({
      ...day,
      items: day.rings.map((ring) => ({
        ring,
        isoTime: ring.ringedAt.toISOString(),
        time: timeLabel(ring.ringedAt),
        openLabel: MESSAGES.history.openPhoto(timeLabel(ring.ringedAt)),
        highlighted: this.service.highlighted().has(ring.id),
      })),
    })),
  );

  ngOnInit(): void {
    this.service.start();
    const tick = setInterval(() => this.now.set(new Date()), CLOCK_TICK_MS);
    this.destroyRef.onDestroy(() => clearInterval(tick));
  }

  protected loadMore(): void {
    this.service.loadMore();
  }

  protected openPhoto(ring: Ring): void {
    this.dialog.open(RingPhotoDialog, { data: ring, maxWidth: '960px', width: '100%' });
  }
}
