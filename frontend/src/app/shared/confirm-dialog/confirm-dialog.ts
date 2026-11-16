import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButton } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
} from '@angular/material/dialog';

export interface ConfirmDialogData {
  readonly question: string;
  readonly cancel: string;
  readonly confirm: string;
}

/** Closes with true only when the user confirms. */
@Component({
  selector: 'app-confirm-dialog',
  imports: [MatDialogContent, MatDialogActions, MatDialogClose, MatButton],
  templateUrl: './confirm-dialog.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmDialog {
  protected readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);
}
