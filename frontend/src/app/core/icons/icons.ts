import { inject } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

import arrowBack from '@material-symbols/svg-400/outlined/arrow_back.svg';
import checklist from '@material-symbols/svg-400/outlined/checklist.svg';
import close from '@material-symbols/svg-400/outlined/close.svg';
import deleteIcon from '@material-symbols/svg-400/outlined/delete.svg';
import deleteForever from '@material-symbols/svg-400/outlined/delete_forever.svg';
import doorbell from '@material-symbols/svg-400/outlined/doorbell.svg';
import lock from '@material-symbols/svg-400/outlined/lock.svg';
import notificationsOff from '@material-symbols/svg-400/outlined/notifications_off.svg';
import restoreFromTrash from '@material-symbols/svg-400/outlined/restore_from_trash.svg';
import sensors from '@material-symbols/svg-400/outlined/sensors.svg';
import sync from '@material-symbols/svg-400/outlined/sync.svg';

// Inlined at build time (svg loader set to text) so icons need no extra request and work offline.
const ICONS: Record<string, string> = {
  'arrow-back': arrowBack,
  checklist,
  close,
  delete: deleteIcon,
  'delete-forever': deleteForever,
  doorbell,
  lock,
  'notifications-off': notificationsOff,
  'restore-from-trash': restoreFromTrash,
  sensors,
  sync,
};

export function registerIcons(): void {
  const registry = inject(MatIconRegistry);
  const sanitizer = inject(DomSanitizer);
  for (const [name, svg] of Object.entries(ICONS)) {
    registry.addSvgIconLiteral(name, sanitizer.bypassSecurityTrustHtml(svg));
  }
}
