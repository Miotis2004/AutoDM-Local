import { Component, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';

import { Notification, NotificationsService } from '../../services/notifications.service';

/**
 * The global toast region that surfaces app-wide notifications.
 *
 * <p>Rendered once in the application shell ({@code app.html}), this component shows the
 * notifications held by {@link NotificationsService}. It is how API-unavailable failures and
 * server-side errors become visible to the user even on pages that do not otherwise report
 * per-action errors.</p>
 */
@Component({
  selector: 'app-notifications',
  standalone: true,
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.css',
  imports: [CommonModule],
})
export class NotificationsComponent implements OnDestroy {
  private readonly notifications = inject(NotificationsService);

  private readonly subscription = new Subscription();

  /** The notifications currently shown. */
  items: Notification[] = [];

  ngOnInit(): void {
    this.subscription.add(
      this.notifications.notifications$.subscribe((list) => {
        this.items = list;
      }),
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  /**
   * Dismisses a notification when the user closes it.
   *
   * @param id the notification to dismiss
   */
  close(id: number): void {
    this.notifications.dismiss(id);
  }
}
