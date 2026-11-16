import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormField, form, required } from '@angular/forms/signals';
import { MatButton } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle,
} from '@angular/material/dialog';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';

import { MESSAGES } from '../../../core/i18n/messages';

export interface AdminKeyDialogData {
  /** Shown above the field, for example after a wrong key. */
  readonly error: string | null;
}

/** Rule: SPEC-010 #7. Closes with the typed key, or with nothing when cancelled. */
@Component({
  selector: 'app-admin-key-dialog',
  imports: [
    FormField,
    MatButton,
    MatDialogActions,
    MatDialogClose,
    MatDialogContent,
    MatDialogTitle,
    MatFormField,
    MatInput,
    MatLabel,
  ],
  templateUrl: './admin-key-dialog.html',
  styleUrl: './admin-key-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminKeyDialog {
  private readonly dialogRef = inject<MatDialogRef<AdminKeyDialog, string>>(MatDialogRef);

  protected readonly messages = MESSAGES;
  protected readonly error = inject<AdminKeyDialogData>(MAT_DIALOG_DATA).error;
  protected readonly keyForm = form(signal({ key: '' }), (path) => required(path.key));

  protected confirm(event: Event): void {
    event.preventDefault();
    if (this.keyForm().valid()) {
      this.dialogRef.close(this.keyForm.key().value());
    }
  }
}
