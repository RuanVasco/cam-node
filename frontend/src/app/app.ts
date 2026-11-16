import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatIcon } from '@angular/material/icon';
import { MatToolbar } from '@angular/material/toolbar';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';

import { TRASH_PATH } from './app.routes';
import { MESSAGES } from './core/i18n/messages';
import { RingService } from './features/ring/ring.service';

@Component({
  selector: 'app-root',
  imports: [MatToolbar, MatIcon, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  private readonly rings = inject(RingService);
  private readonly router = inject(Router);
  private readonly url = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  protected readonly messages = MESSAGES;
  // The trash has no stream, so the live badge belongs to the history only.
  protected readonly inTrash = computed(() => this.url().startsWith(`/${TRASH_PATH}`));
  protected readonly title = computed(() =>
    this.inTrash() ? MESSAGES.trash.title : MESSAGES.appTitle,
  );
  protected readonly isLive = computed(() => this.rings.connection() === 'live');
  protected readonly connectionLabel = computed(() => MESSAGES.connection[this.rings.connection()]);
}
