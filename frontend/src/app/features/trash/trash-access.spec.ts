import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { Observable, of, throwError } from 'rxjs';

import { AdminKeyDialogData } from './admin-key-dialog/admin-key-dialog';
import { AdminKeyStore } from './admin-key.store';
import { Keyed, TrashAccess } from './trash-access';
import { InvalidAdminKeyError } from './trash.model';

class FakeDialog {
  keyAnswers: (string | undefined)[] = [];
  readonly prompts: AdminKeyDialogData[] = [];

  open(_component: unknown, config: { data: AdminKeyDialogData }) {
    this.prompts.push(config.data);
    return { afterClosed: () => of(this.keyAnswers.shift()) };
  }
}

describe('TrashAccess', () => {
  let dialog: FakeDialog;
  let access: TrashAccess;
  let store: AdminKeyStore;
  let sentKeys: string[];

  beforeEach(() => {
    sessionStorage.clear();
    dialog = new FakeDialog();
    sentKeys = [];
    TestBed.configureTestingModule({ providers: [{ provide: MatDialog, useValue: dialog }] });
    access = TestBed.inject(TrashAccess);
    store = TestBed.inject(AdminKeyStore);
  });

  function run(): Keyed<string>[] {
    const results: Keyed<string>[] = [];
    const request = (key: string): Observable<string> => {
      sentKeys.push(key);
      return key === 'right' ? of('page') : throwError(() => new InvalidAdminKeyError());
    };
    access.withKey(request).subscribe((result) => results.push(result));
    return results;
  }

  it('SPEC-010 #7: asks for the key when the tab has none and keeps it after it works', () => {
    dialog.keyAnswers = ['right'];

    expect(run()).toEqual([{ kind: 'done', value: 'page' }]);
    expect(dialog.prompts).toEqual([{ error: null }]);
    expect(store.get()).toBe('right');
  });

  it('SPEC-010 CA-10 (#7): with the key kept in this tab it is not asked again', () => {
    store.set('right');

    expect(run()).toEqual([{ kind: 'done', value: 'page' }]);
    expect(dialog.prompts).toEqual([]);
  });

  it('SPEC-010 #7: a wrong key is forgotten and asked again with "Senha incorreta."', () => {
    store.set('stale');
    dialog.keyAnswers = ['wrong', 'right'];

    expect(run()).toEqual([{ kind: 'done', value: 'page' }]);
    expect(dialog.prompts).toEqual([{ error: 'Senha incorreta.' }, { error: 'Senha incorreta.' }]);
    expect(sentKeys).toEqual(['stale', 'wrong', 'right']);
    expect(store.get()).toBe('right');
  });

  it('SPEC-010 CA-8 (#7): cancelling the key dialog sends nothing and keeps no key', () => {
    dialog.keyAnswers = [undefined];

    expect(run()).toEqual([{ kind: 'cancelled' }]);
    expect(sentKeys).toEqual([]);
    expect(store.get()).toBeNull();
  });
});
