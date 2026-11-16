import { Routes } from '@angular/router';

import { MESSAGES } from './core/i18n/messages';
import { RingHistory } from './features/ring/ring-history/ring-history';

export const TRASH_PATH = 'lixeira';

export const routes: Routes = [
  { path: '', component: RingHistory, title: MESSAGES.appTitle },
  {
    // Rule: SPEC-010 #10. Loaded on demand: visitors of the history never download it.
    path: TRASH_PATH,
    loadComponent: () => import('./features/trash/trash-page/trash-page').then((m) => m.TrashPage),
    title: MESSAGES.trash.title,
  },
  { path: '**', redirectTo: '' },
];
