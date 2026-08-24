import { Injectable, inject } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpErrorResponse,
  HttpRequest,
  HttpInterceptor,
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { NotificationsService } from '../services/notifications.service';

/**
 * A normalized HTTP error surfaced to the front-end.
 *
 * <p>The {@link HttpErrorInterceptor} replaces the raw {@link HttpErrorResponse} with this shape so
 * every subscriber sees a clear, consistent {@link message} regardless of whether the failure was a
 * network failure, an HTTP status error, or an unexpected exception.</p>
 */
export interface NormalizedHttpError {
  /** A human-readable description of what went wrong. */
  message: string;
  /** The HTTP status code, or {@code 0} when the request never reached the back-end. */
  status: number;
  /** Whether the failure is because the back-end could not be reached at all. */
  unavailable: boolean;
  /** The request URL that failed, when known. */
  url?: string;
}

/**
 * Interceptor that makes HTTP failures visible to the user.
 *
 * <p>Every request that reaches the back-end flows through this interceptor. It classifies the
 * failure and guarantees that backend failures are never silently ignored:</p>
 *
 * <ul>
 *   <li><b>API unavailable</b> - when the request never reaches the back-end (network error or
 *       status {@code 0}), the user is told the application is unavailable.</li>
 *   <li><b>Server errors</b> - HTTP {@code 5xx} responses are logged and surfaced, so a failing
 *       back-end is reported rather than swallowed.</li>
 *   <li><b>Client/invalid operations</b> - HTTP {@code 4xx} responses surface the back-end's own
 *       message (falling back to a clear default), so invalid user operations and invalid game
 *       actions report the real problem.</li>
 * </ul>
 *
 * <p>The interceptor normalizes the error and re-emits it, so existing per-component error handling
 * ({@code error: (err) => err?.message}) keeps working while receiving a clearer message. API
 * unavailable and server errors are additionally pushed to the global {@link NotificationsService}
 * so they are shown even on pages that do not report per-action errors.</p>
 */
@Injectable()
export class HttpErrorInterceptor implements HttpInterceptor {
  private readonly notifications = inject(NotificationsService);

  intercept(
    req: HttpRequest<unknown>,
    next: HttpHandler,
  ): Observable<HttpEvent<unknown>> {
    return next.handle(req).pipe(
      catchError((error: unknown): Observable<never> => {
        const normalized = this.normalize(error);
        // Backend failures are never silently ignored: anything that is not a plain client
        // validation/operation error is surfaced through the global notification region.
        if (normalized.unavailable || normalized.status >= 500) {
          this.notifications.add(normalized.message, 'error');
        }
        return throwError(() => normalized);
      }),
    );
  }

  /**
   * Converts a raw HTTP failure into a normalized error with a clear message.
   *
   * @param error the raw failure (an {@link HttpErrorResponse} or an unexpected value)
   * @return the normalized error
   */
  private normalize(error: unknown): NormalizedHttpError {
    if (error instanceof HttpErrorResponse) {
      const status = error.status;
      const url = error.url ?? undefined;
      const bodyMessage = this.extractBodyMessage(error.error);

      // A network failure (status 0) means the back-end could not be reached.
      if (status === 0) {
        return {
          message:
            'The application is unavailable. Please make sure the back-end server is running and try again.',
          status,
          unavailable: true,
          url,
        };
      }

      // Server-side failures (5xx) surface the back-end message or a clear default.
      if (status >= 500) {
        return {
          message:
            bodyMessage ??
            `The back-end failed to process your request (server error ${status}).`,
          status,
          unavailable: false,
          url,
        };
      }

      // Client failures (4xx): invalid operations, invalid actions, failed saves. Surface the
      // back-end's own validation message so the user sees the real reason.
      if (status >= 400) {
        return {
          message:
            bodyMessage ??
            `Your request could not be processed (error ${status}).`,
          status,
          unavailable: false,
          url,
        };
      }

      return {
        message: bodyMessage ?? `An unexpected error occurred (${status}).`,
        status,
        unavailable: false,
        url,
      };
    }

    // A non-HTTP error (for example an exception thrown while handling a response).
    if (error instanceof Error) {
      return {
        message: error.message || 'An unexpected error occurred.',
        status: 0,
        unavailable: false,
      };
    }

    return {
      message: 'An unexpected error occurred. Please try again.',
      status: 0,
      unavailable: false,
    };
  }

  /**
   * Extracts a human-readable message from a parsed error body, tolerating the shapes the back-end
   * may return (a plain string, an {@code error} field, or a {@code message} field).
   *
   * @param error the response body
   * @return the first non-empty message found, or {@code null} when none is present
   */
  private extractBodyMessage(error: unknown): string | null {
    if (typeof error === 'string' && error.trim().length > 0) {
      return error.trim();
    }
    if (error && typeof error === 'object') {
      const candidate = error as Record<string, unknown>;
      for (const key of ['message', 'error', 'errorMessage', 'detail']) {
        const value = candidate[key];
        if (typeof value === 'string' && value.trim().length > 0) {
          return value.trim();
        }
      }
    }
    return null;
  }
}
