import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButton } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
} from '@angular/material/dialog';

import { MESSAGES } from '../../../core/i18n/messages';

export interface RingRemovalConfirmData {
  readonly count: number;
}

/** Rule: SPEC-010 #5. Closes with true only when the user confirms. */
@Component({
  selector: 'app-ring-removal-confirm-dialog',
  imports: [MatDialogContent, MatDialogActions, MatDialogClose, MatButton],
  templateUrl: './ring-removal-confirm-dialog.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RingRemovalConfirmDialog {
  private readonly data = inject<RingRemovalConfirmData>(MAT_DIALOG_DATA);

  protected readonly messages = MESSAGES;
  protected readonly question =
    this.data.count === 1
      ? MESSAGES.removal.confirmOne
      : MESSAGES.removal.confirmMany(this.data.count);
}
