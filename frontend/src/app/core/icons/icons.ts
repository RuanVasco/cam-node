import { inject } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

import checklist from '@material-symbols/svg-400/outlined/checklist.svg';
import close from '@material-symbols/svg-400/outlined/close.svg';
import deleteIcon from '@material-symbols/svg-400/outlined/delete.svg';
import doorbell from '@material-symbols/svg-400/outlined/doorbell.svg';
import notificationsOff from '@material-symbols/svg-400/outlined/notifications_off.svg';
import sensors from '@material-symbols/svg-400/outlined/sensors.svg';
import sync from '@material-symbols/svg-400/outlined/sync.svg';

// Inlined at build time (svg loader set to text) so icons need no extra request and work offline.
const ICONS: Record<string, string> = {
  checklist,
  close,
  delete: deleteIcon,
  doorbell,
  'notifications-off': notificationsOff,
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
