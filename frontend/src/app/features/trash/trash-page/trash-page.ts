import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';

import { MESSAGES } from '../../../core/i18n/messages';
import { dateTimeLabel } from '../../ring/ring-time';
import { TrashPhoto } from '../trash-photo/trash-photo';
import { TrashService } from '../trash.service';
import { TrashedRing } from '../trash.model';

/** Rule: SPEC-010 #10. Reached only by its own address; the history has no link to it. */
@Component({
  selector: 'app-trash-page',
  imports: [MatButton, MatIconButton, MatIcon, MatProgressSpinner, RouterLink, TrashPhoto],
  templateUrl: './trash-page.html',
  styleUrl: './trash-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrashPage implements OnInit {
  private readonly trash = inject(TrashService);

  protected readonly messages = MESSAGES;
  protected readonly state = this.trash.state;
  protected readonly hasMore = this.trash.hasMore;
  protected readonly loadingMore = this.trash.loadingMore;
  protected readonly isEmpty = computed(
    () => this.trash.state() === 'ready' && this.trash.rings().length === 0,
  );
  protected readonly items = computed(() =>
    this.trash.rings().map((ring) => {
      const ringedAt = dateTimeLabel(ring.ringedAt);
      return {
        ring,
        ringedLabel: MESSAGES.trash.ringedAt(ringedAt),
        removedLabel: MESSAGES.trash.removedAt(dateTimeLabel(ring.removedAt)),
        restoreLabel: MESSAGES.trash.restoreRing(ringedAt),
        purgeLabel: MESSAGES.trash.purgeRing(ringedAt),
        busy: this.trash.pending().has(ring.id),
      };
    }),
  );

  ngOnInit(): void {
    this.trash.open();
  }

  protected retry(): void {
    this.trash.open();
  }

  protected loadMore(): void {
    this.trash.loadMore();
  }

  protected restore(ring: TrashedRing): void {
    this.trash.restore(ring).subscribe();
  }

  protected purge(ring: TrashedRing): void {
    this.trash.purge(ring).subscribe();
  }
}
