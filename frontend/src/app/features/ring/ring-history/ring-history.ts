import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

import { MESSAGES } from '../../../core/i18n/messages';
import { RingPhotoDialog } from '../ring-photo-dialog/ring-photo-dialog';
import { RingRemoval } from '../ring-removal';
import { groupByDay, timeLabel } from '../ring-time';
import { Ring } from '../ring.model';
import { RingService } from '../ring.service';

const CLOCK_TICK_MS = 60_000;
/** Rule: SPEC-010 #6. */
export const MAX_SELECTION = 100;

@Component({
  selector: 'app-ring-history',
  imports: [MatButton, MatIconButton, MatCheckbox, MatIcon, MatProgressSpinner],
  templateUrl: './ring-history.html',
  styleUrl: './ring-history.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RingHistory implements OnInit {
  private readonly service = inject(RingService);
  private readonly removal = inject(RingRemoval);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);
  // Ticks every minute so "Hoje" turns into "Ontem" after midnight without a reload.
  private readonly now = signal(new Date());
  private readonly selectedIds = signal<ReadonlySet<string>>(new Set());

  protected readonly messages = MESSAGES;
  protected readonly loaded = this.service.loaded;
  protected readonly hasMore = this.service.hasMore;
  protected readonly loadingMore = this.service.loadingMore;
  protected readonly selecting = signal(false);
  protected readonly isEmpty = computed(
    () => this.service.loaded() && this.service.rings().length === 0,
  );
  // Rule: SPEC-010 #4. A ring removed elsewhere leaves the selection with it.
  private readonly selectedRings = computed(() =>
    this.service.rings().filter((ring) => this.selectedIds().has(ring.id)),
  );
  protected readonly removeSelectedLabel = computed(() =>
    MESSAGES.removal.removeSelected(this.selectedRings().length),
  );
  protected readonly cannotRemoveSelected = computed(
    () => this.removal.busy() || this.selectedRings().length === 0,
  );
  private readonly selectionFull = computed(() => this.selectedRings().length >= MAX_SELECTION);
  protected readonly days = computed(() => {
    const selecting = this.selecting();
    return groupByDay(this.service.rings(), this.now()).map((day) => ({
      ...day,
      items: day.rings.map((ring) => {
        const time = timeLabel(ring.ringedAt);
        const selected = this.selectedIds().has(ring.id);
        return {
          ring,
          time,
          selected,
          isoTime: ring.ringedAt.toISOString(),
          openLabel: MESSAGES.history.openPhoto(time),
          // While selecting, the checkbox is the accessible control; the thumbnail is a mouse shortcut to it.
          thumbHidden: selecting ? 'true' : null,
          thumbTabIndex: selecting ? -1 : 0,
          selectLabel: MESSAGES.removal.selectRing(time),
          selectDisabled: !selected && this.selectionFull(),
          removeLabel: MESSAGES.removal.removeRing(time),
          highlighted: this.service.highlighted().has(ring.id),
        };
      }),
    }));
  });

  ngOnInit(): void {
    this.service.start();
    const tick = setInterval(() => this.now.set(new Date()), CLOCK_TICK_MS);
    this.destroyRef.onDestroy(() => clearInterval(tick));
  }

  protected loadMore(): void {
    this.service.loadMore();
  }

  protected thumbClicked(ring: Ring): void {
    if (this.selecting()) {
      this.toggleSelected(ring);
    } else {
      this.dialog.open(RingPhotoDialog, { data: ring, maxWidth: '960px', width: '100%' });
    }
  }

  protected toggleSelecting(): void {
    this.selecting.update((selecting) => !selecting);
    this.selectedIds.set(new Set());
  }

  protected toggleSelected(ring: Ring): void {
    this.selectedIds.update((ids) => {
      const next = new Set(ids);
      if (!next.delete(ring.id) && !this.selectionFull()) {
        next.add(ring.id);
      }
      return next;
    });
  }

  protected removeOne(ring: Ring): void {
    this.removal.remove([ring.id]).subscribe();
  }

  // Rule: SPEC-010 #6.
  protected removeSelected(): void {
    const ids = this.selectedRings().map((ring) => ring.id);
    this.removal.remove(ids).subscribe((removed) => {
      if (removed) {
        this.selecting.set(false);
        this.selectedIds.set(new Set());
      }
    });
  }
}
