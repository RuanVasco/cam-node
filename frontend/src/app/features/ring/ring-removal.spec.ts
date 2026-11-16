import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, of, throwError } from 'rxjs';

import { ConfirmDialog, ConfirmDialogData } from '../../shared/confirm-dialog/confirm-dialog';
import { RingRemoval } from './ring-removal';
import { RingService } from './ring.service';

class FakeDialog {
  confirmAnswers: boolean[] = [];
  readonly questions: ConfirmDialogData[] = [];

  open(
    component: unknown,
    config: { data: ConfirmDialogData },
  ): { afterClosed: () => Observable<unknown> } {
    if (component !== ConfirmDialog) {
      throw new Error('unexpected dialog');
    }
    this.questions.push(config.data);
    return { afterClosed: () => of(this.confirmAnswers.shift()) };
  }
}

class FakeRingService {
  readonly calls: (readonly string[])[] = [];
  fail = false;

  remove(ids: readonly string[]): Observable<void> {
    this.calls.push(ids);
    return this.fail ? throwError(() => new Error('down')) : of(undefined);
  }
}

describe('RingRemoval', () => {
  let dialog: FakeDialog;
  let rings: FakeRingService;
  let notices: string[];
  let removal: RingRemoval;

  beforeEach(() => {
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
  });

  it('SPEC-010 CA-1 (#9): a confirmed removal needs no key and tells "Toque excluído."', () => {
    dialog.confirmAnswers = [true];

    expect(run(['a'])).toEqual([true]);
    expect(rings.calls).toEqual([['a']]);
    expect(notices).toEqual(['Toque excluído.']);
  });

  it('SPEC-010 CA-13 (#9): neither the confirmation nor the notice mentions the trash', () => {
    dialog.confirmAnswers = [true];

    run(['a', 'b', 'c']);

    expect(dialog.questions).toEqual([
      { question: 'Excluir 3 toques?', cancel: 'Cancelar', confirm: 'Excluir' },
    ]);
    expect(notices).toEqual(['3 toques excluídos.']);
    const texts = [...notices, ...dialog.questions.map((data) => data.question)].join(' ');
    expect(texts.toLowerCase()).not.toContain('lixeira');
  });

  it('SPEC-010 #9: a failed removal emits false and shows no success notice', () => {
    dialog.confirmAnswers = [true];
    rings.fail = true;

    expect(run(['a'])).toEqual([false]);
    expect(notices).toEqual([]);
  });

  it('SPEC-010 #5: a second removal while one flow is open is ignored', () => {
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
});
