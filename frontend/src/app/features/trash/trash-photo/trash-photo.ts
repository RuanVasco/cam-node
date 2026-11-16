import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';

import { TrashService } from '../trash.service';
import { TrashedRing } from '../trash.model';

/** Shows a trash photo from a blob URL, released when the ring changes or the component goes away. */
@Component({
  selector: 'app-trash-photo',
  templateUrl: './trash-photo.html',
  styleUrl: './trash-photo.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrashPhoto {
  private readonly trash = inject(TrashService);

  readonly ring = input.required<TrashedRing>();
  readonly alt = input.required<string>();

  protected readonly src = signal<string | null>(null);

  constructor() {
    effect((onCleanup) => {
      let objectUrl: string | null = null;
      const subscription = this.trash.photo(this.ring()).subscribe({
        next: (blob) => {
          objectUrl = URL.createObjectURL(blob);
          this.src.set(objectUrl);
        },
        // The interceptor already told the user; the placeholder stays.
        error: () => undefined,
      });
      onCleanup(() => {
        subscription.unsubscribe();
        if (objectUrl !== null) {
          URL.revokeObjectURL(objectUrl);
        }
        this.src.set(null);
      });
    });
  }
}
