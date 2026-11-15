import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatIconButton } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogClose, MatDialogContent } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';

import { MESSAGES } from '../../../core/i18n/messages';
import { dateTimeLabel } from '../ring-time';
import { Ring } from '../ring.model';

@Component({
  selector: 'app-ring-photo-dialog',
  imports: [MatDialogContent, MatDialogClose, MatIconButton, MatIcon],
  templateUrl: './ring-photo-dialog.html',
  styleUrl: './ring-photo-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RingPhotoDialog {
  protected readonly ring = inject<Ring>(MAT_DIALOG_DATA);
  protected readonly messages = MESSAGES;
  protected readonly caption = dateTimeLabel(this.ring.ringedAt);
}
