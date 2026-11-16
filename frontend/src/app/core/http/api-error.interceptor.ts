import {
  HttpContextToken,
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpStatusCode,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';

import { MESSAGES } from '../i18n/messages';

/** What the user is told when this request fails. */
export const API_ERROR_MESSAGE = new HttpContextToken<string>(() => MESSAGES.errors.generic);

/** Statuses the caller turns into its own feedback, so no generic notice is shown for them. */
export const CALLER_HANDLED_STATUSES = new HttpContextToken<readonly number[]>(() => []);

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const snackBar = inject(MatSnackBar);
  return next(request).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        !request.context.get(CALLER_HANDLED_STATUSES).includes(error.status)
      ) {
        // Rule: SPEC-010 #8.
        const message =
          error.status === HttpStatusCode.TooManyRequests
            ? MESSAGES.errors.tooManyAttempts
            : request.context.get(API_ERROR_MESSAGE);
        snackBar.open(message, MESSAGES.errors.dismiss, { duration: 5000 });
      }
      return throwError(() => error);
    }),
  );
};
