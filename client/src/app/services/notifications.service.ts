import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * A single user-visible notification.
 *
 * <p>Notifications are the app-wide, always-visible error surface. They are used to surface
 * failures that are not tied to a single on-screen action - most importantly an unavailable
 * back-end (the application cannot reach the API at all) and server-side failures - so they are
 * never silently ignored even on pages that do not otherwise report per-action errors.</p>
 */
export interface Notification {
  /** The unique identifier for the notification. */
  id: number;
  /**
   * The kind of notification.
   *
   * <ul>
   *   <li>{@code error} for failures that must be shown to the user;</li>
   *   <li>{@code info} for benign notices.</li>
   * </ul>
   */
  level: 'error' | 'info';
  /** The message shown to the user. */
  message: string;
}

/**
 * The app-wide notification store.
 *
 * <p>This is the single owner of the notifications shown in the global toast region
 * ({@link NotificationsComponent}). Both the {@link HttpErrorInterceptor} and individual
 * components push notifications through {@link add}, and the toast region renders whatever is
 * currently active.</p>
 *
 * <p>Errors are surfaced visibly: an unavailable back-end, a failed save, an invalid action, or an
 * encounter error each becomes a {@code level: 'error'} notification that the user can see, rather
 * than a swallowed exception.</p>
 */
@Injectable({ providedIn: 'root' })
export class NotificationsService {
  private readonly subject = new BehaviorSubject<Notification[]>([]);

  /**
   * @return an observable of the current notifications, emitting whenever the set changes
   */
  get notifications$(): Observable<Notification[]> {
    return this.subject.asObservable();
  }

  /**
   * Adds a notification and auto-dismisses it after a short delay.
   *
   * @param message the message to show
   * @param level the severity of the notification (defaults to {@code error})
   * @param timeoutMs the auto-dismiss delay in milliseconds (defaults to 6000)
   */
  add(message: string, level: 'error' | 'info' = 'error', timeoutMs = 6000): void {
    const id = ++this.nextId;
    const notification: Notification = { id, message, level };
    this.subject.next([...this.subject.value, notification]);
    if (timeoutMs > 0) {
      setTimeout(() => this.dismiss(id), timeoutMs);
    }
  }

  /**
   * Dismisses the notification with the given id.
   *
   * @param id the notification to dismiss
   */
  dismiss(id: number): void {
    this.subject.next(this.subject.value.filter((notification) => notification.id !== id));
  }

  private nextId = 0;
}
