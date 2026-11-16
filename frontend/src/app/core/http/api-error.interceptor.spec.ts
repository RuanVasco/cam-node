import { HttpClient, HttpContext, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';

import {
  API_ERROR_MESSAGE,
  CALLER_HANDLED_STATUSES,
  apiErrorInterceptor,
} from './api-error.interceptor';

describe('apiErrorInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let notices: string[];

  beforeEach(() => {
    notices = [];
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
        { provide: MatSnackBar, useValue: { open: (message: string) => notices.push(message) } },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  function fail(context: HttpContext, status: number): void {
    http.get('/api/v1/x', { context }).subscribe({ error: () => undefined });
    backend.expectOne('/api/v1/x').flush(null, { status, statusText: 'Error' });
  }

  it('shows the generic message by default', () => {
    fail(new HttpContext(), 500);

    expect(notices).toEqual(['Não foi possível carregar os dados. Tente novamente.']);
  });

  it('shows the message chosen by the call', () => {
    fail(new HttpContext().set(API_ERROR_MESSAGE, 'Outra mensagem.'), 500);

    expect(notices).toEqual(['Outra mensagem.']);
  });

  it('SPEC-010 #8: a 429 tells the user to wait', () => {
    fail(new HttpContext(), 429);

    expect(notices).toEqual(['Muitas tentativas. Tente de novo em alguns minutos.']);
  });

  it('SPEC-010 #7: a status the call handles itself shows nothing', () => {
    fail(new HttpContext().set(CALLER_HANDLED_STATUSES, [401]), 401);

    expect(notices).toEqual([]);
  });
});
