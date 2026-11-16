import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { AdminKeyDialog } from './admin-key-dialog';

describe('AdminKeyDialog', () => {
  let closedWith: (string | undefined)[];

  function render(error: string | null) {
    closedWith = [];
    TestBed.configureTestingModule({
      imports: [AdminKeyDialog],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: { error } },
        { provide: MatDialogRef, useValue: { close: (value?: string) => closedWith.push(value) } },
      ],
    });
    const fixture = TestBed.createComponent(AdminKeyDialog);
    fixture.detectChanges();
    return fixture;
  }

  it('SPEC-010 #7: closes with the typed key on submit', async () => {
    const fixture = render(null);
    const element: HTMLElement = fixture.nativeElement;
    const input = element.querySelector('input');
    if (!(input instanceof HTMLInputElement)) {
      throw new Error('password field not rendered');
    }

    input.value = 'segredo';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    element.querySelector('form')?.dispatchEvent(new Event('submit', { cancelable: true }));

    expect(closedWith).toEqual(['segredo']);
  });

  it('SPEC-010 #7: an empty key is not submitted', async () => {
    const fixture = render(null);
    const element: HTMLElement = fixture.nativeElement;

    element.querySelector('form')?.dispatchEvent(new Event('submit', { cancelable: true }));
    await fixture.whenStable();

    expect(closedWith).toEqual([]);
  });

  it('SPEC-010 #7: shows "Senha incorreta." after a wrong key', () => {
    const fixture = render('Senha incorreta.');

    expect(fixture.nativeElement.textContent).toContain('Senha incorreta.');
  });
});
