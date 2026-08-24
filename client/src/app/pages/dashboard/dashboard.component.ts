import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { DatePipe, LowerCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { DashboardService } from '../../services/dashboard.service';
import { CampaignsService } from '../../services/campaigns.service';
import { CampaignStore } from '../../services/campaign-store.service';
import { DashboardState } from '../../models/dashboard';

/**
 * Dashboard: the entry point of the app shell. Summarises the current campaign state at a glance -
 * the active campaign, the party's current location, the active characters, the current quests, any
 * encounter in progress, and the most recent campaign events - and offers quick entry to the major
 * application areas.
 *
 * <p>The dashboard loads its snapshot from the back-end {@link DashboardService}. On entry it picks
 * the active campaign - whichever the user selected in the {@link CampaignStore}, falling back to
 * the campaign the back-end has marked active - and fetches the aggregated {@link DashboardState}.
 * If no campaign is active anywhere, it invites the visitor to open or start one.</p>
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DatePipe, LowerCasePipe, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent implements OnInit, OnDestroy {
  private readonly title = inject(Title);
  private readonly dashboardService = inject(DashboardService);
  private readonly campaignsService = inject(CampaignsService);
  private readonly store = inject(CampaignStore);

  /** The aggregated snapshot currently shown, or {@code null} while nothing has loaded. */
  state: DashboardState | null = null;
  /** Whether an initial load is in flight. */
  loading = true;
  /** A human-readable error when loading fails, or {@code null}. */
  error: string | null = null;

  private readonly subscriptions = new Subscription();

  /**
   * The number of setup dimensions that are complete: a dimension counts once its backing data
   * exists. The dashboard shows this as progress through the setup workflow.
   */
  get setupCompleteCount(): number {
    const progress = this.state?.setupProgress;
    if (!progress) {
      return 0;
    }
    let complete = 0;
    if (progress.characters > 0) {
      complete++;
    }
    if (progress.regions > 0 && progress.locations > 0) {
      complete++;
    }
    if (progress.npcs > 0) {
      complete++;
    }
    if (progress.quests > 0) {
      complete++;
    }
    if (progress.items > 0) {
      complete++;
    }
    return complete;
  }

  /**
   * @return {@code true} when every setup dimension has data, i.e. the workflow is finished
   */
  get isSetupComplete(): boolean {
    return this.setupCompleteCount === 5;
  }

  /**
   * @param label the label of the next step
   * @return the Angular route of the next step
   */
  nextRoute(step: { route: string }): string {
    return step.route;
  }

  ngOnInit(): void {
    this.title.setTitle('AutoDM - Dashboard');
    this.initialise();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  /**
   * Fetches the dashboard for a campaign. Exposed so the template can re-run the load when the
   * visitor switches campaigns by hand.
   *
   * @param campaignId the campaign to summarise
   */
  load(campaignId: number): void {
    this.loading = true;
    this.error = null;
    this.subscriptions.add(
      this.dashboardService.getDashboard(campaignId).subscribe({
        next: (snapshot) => {
          this.state = snapshot;
          this.loading = false;
        },
        error: (err: unknown) => {
          this.error = err instanceof Error
            ? err.message
            : 'Unable to load the dashboard. Is the back-end running on port 5150?';
          this.loading = false;
        },
      }),
    );
  }

  /**
   * Resolves and loads the campaign to summarise: the campaign the user has selected in the store
   * when one is active, otherwise the campaign the back-end has marked active, or leaves the view
   * prompting the visitor to open or start a campaign when neither exists yet.
   */
  private initialise(): void {
    const selectedId = this.store.activeCampaign?.id ?? null;
    if (selectedId != null) {
      this.load(selectedId);
      return;
    }
    // Fall back to the campaign the back-end has marked active so the dashboard reflects the
    // persistent selection even on a fresh visit before the store has loaded anything.
    this.subscriptions.add(
      this.campaignsService.getActive().subscribe({
        next: (active) => {
          const id = active?.id ?? null;
          if (id != null) {
            this.load(Number(id));
          } else {
            this.loading = false;
          }
        },
        error: () => {
          this.loading = false;
        },
      }),
    );
  }
}
