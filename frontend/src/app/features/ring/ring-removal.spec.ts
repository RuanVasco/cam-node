import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, of, throwError } from 'rxjs';

import { AdminKeyDialog, AdminKeyDialogData } from './admin-key-dialog/admin-key-dialog';
import { AdminKeyStore } from './admin-key.store';
import { RingRemovalConfirmDialog } from './ring-removal-confirm-dialog/ring-removal-confirm-dialog';
import { RingRemoval } from './ring-removal';
import { InvalidAdminKeyError } from './ring.model';
import { RingService } from './ring.service';

/** Answers each dialog with the next scripted value, by dialog type. */
class FakeDialog {
  confirmAnswers: boolean[] = [];
  keyAnswers: (string | undefined)[] = [];
  readonly keyPrompts: AdminKeyDialogData[] = [];
  confirmations = 0;

  open(
    component: unknown,
    config: { data: AdminKeyDialogData },
  ): { afterClosed: () => Observable<unknown> } {
    if (component === RingRemovalConfirmDialog) {
      this.confirmations++;
      return { afterClosed: () => of(this.confirmAnswers.shift()) };
    }
    if (component === AdminKeyDialog) {
      this.keyPrompts.push(config.data);
      return { afterClosed: () => of(this.keyAnswers.shift()) };
    }
    throw new Error('unexpected dialog');
  }
}

class FakeRingService {
  readonly calls: { ids: readonly string[]; adminKey: string }[] = [];
  rightKey = 'right';

  remove(ids: readonly string[], adminKey: string): Observable<void> {
    this.calls.push({ ids, adminKey });
    return adminKey === this.rightKey
      ? of(undefined)
      : throwError(() => new InvalidAdminKeyError());
  }
}

describe('RingRemoval', () => {
  let dialog: FakeDialog;
  let rings: FakeRingService;
  let notices: string[];
  let removal: RingRemoval;
  let store: AdminKeyStore;

  beforeEach(() => {
    sessionStorage.clear();
    dialog = new FakeDialog();
    rings = new FakeRingService();
    notices = [];
    TestBed.configureTestingModule({
      providers: [
        { provide: MatDialog, useValue: dialog },
        { provide: RingService, useValue: rings },
        { provide: MatSnackBar, useValue: { open: (message: string) => notices.push(message) } },
      ],
    });
    removal = TestBed.inject(RingRemoval);
    store = TestBed.inject(AdminKeyStore);
  });

  function run(ids: readonly string[]): boolean[] {
    const results: boolean[] = [];
    removal.remove(ids).subscribe((removed) => results.push(removed));
    return results;
  }

  it('SPEC-010 CA-5 (#5): a cancelled confirmation removes nothing', () => {
    dialog.confirmAnswers = [false];

    expect(run(['a'])).toEqual([false]);
    expect(rings.calls).toEqual([]);
    expect(dialog.keyPrompts).toEqual([]);
  });

  it('SPEC-010 #7: the first removal asks for the key and keeps it after it works', () => {
    dialog.confirmAnswers = [true];
    dialog.keyAnswers = ['right'];

    expect(run(['a'])).toEqual([true]);
    expect(dialog.keyPrompts).toEqual([{ error: null }]);
    expect(store.get()).toBe('right');
    expect(notices).toEqual(['Toque removido.']);
  });

  it('SPEC-010 CA-10 (#7): with the key kept in this tab it is not asked again', () => {
    store.set('right');
    dialog.confirmAnswers = [true];

    expect(run(['a', 'b', 'c'])).toEqual([true]);
    expect(dialog.keyPrompts).toEqual([]);
    expect(rings.calls).toEqual([{ ids: ['a', 'b', 'c'], adminKey: 'right' }]);
    expect(notices).toEqual(['3 toques removidos.']);
  });

  it('SPEC-010 #7: a wrong key is forgotten and asked again with "Senha incorreta."', () => {
    store.set('stale');
    dialog.confirmAnswers = [true];
    dialog.keyAnswers = ['wrong', 'right'];

    expect(run(['a'])).toEqual([true]);
    expect(dialog.keyPrompts).toEqual([
      { error: 'Senha incorreta.' },
      { error: 'Senha incorreta.' },
    ]);
    expect(rings.calls.map((call) => call.adminKey)).toEqual(['stale', 'wrong', 'right']);
    expect(store.get()).toBe('right');
  });

  it('SPEC-010 #5: a second removal while one flow is open is ignored', () => {
    store.set('right');
    dialog.confirmAnswers = [true];
    const pending = dialog.open;
    let close: ((value: boolean) => void) | undefined;
    dialog.open = () => ({
      afterClosed: () =>
        new Observable<unknown>((subscriber) => {
          close = (value) => {
            subscriber.next(value);
            subscriber.complete();
          };
        }),
    });
    const first = run(['a']);
    dialog.open = pending;

    expect(run(['a'])).toEqual([false]);
    close?.(true);
    expect(first).toEqual([true]);
    expect(rings.calls).toHaveLength(1);
    expect(removal.busy()).toBe(false);
  });

  it('SPEC-010 #7: cancelling the key dialog removes nothing and keeps no key', () => {
    dialog.confirmAnswers = [true];
    dialog.keyAnswers = [undefined];

    expect(run(['a'])).toEqual([false]);
    expect(rings.calls).toEqual([]);
    expect(store.get()).toBeNull();
  });
});
