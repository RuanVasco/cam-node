import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { MatToolbar } from '@angular/material/toolbar';

import { MESSAGES } from './core/i18n/messages';
import { RingHistory } from './features/ring/ring-history/ring-history';
import { RingService } from './features/ring/ring.service';

@Component({
  selector: 'app-root',
  imports: [MatToolbar, MatIcon, RingHistory],
  templateUrl: './app.html',
  styleUrl: './app.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  private readonly rings = inject(RingService);

  protected readonly messages = MESSAGES;
  protected readonly isLive = computed(() => this.rings.connection() === 'live');
  protected readonly connectionLabel = computed(() => MESSAGES.connection[this.rings.connection()]);
}
