import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { MatIconButton } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogClose,
  MatDialogContent,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';

import { MESSAGES } from '../../../core/i18n/messages';
import { RingRemoval } from '../ring-removal';
import { dateTimeLabel } from '../ring-time';
import { Ring } from '../ring.model';
import { RingService } from '../ring.service';

@Component({
  selector: 'app-ring-photo-dialog',
  imports: [MatDialogContent, MatDialogClose, MatIconButton, MatIcon],
  templateUrl: './ring-photo-dialog.html',
  styleUrl: './ring-photo-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RingPhotoDialog {
  private readonly removal = inject(RingRemoval);
  private readonly rings = inject(RingService);
  private readonly dialogRef = inject(MatDialogRef);

  protected readonly ring = inject<Ring>(MAT_DIALOG_DATA);
  protected readonly messages = MESSAGES;
  protected readonly caption = dateTimeLabel(this.ring.ringedAt);

  constructor() {
    // Rule: SPEC-010 #4. Removed here or on another screen, the photo is gone, so the dialog goes too.
    effect(() => {
      if (!this.rings.rings().some((ring) => ring.id === this.ring.id)) {
        this.dialogRef.close();
      }
    });
  }

  protected remove(): void {
    this.removal.remove([this.ring.id]).subscribe();
  }
}
