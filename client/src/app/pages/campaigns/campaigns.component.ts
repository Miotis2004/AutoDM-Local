import { Component, inject, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import {
  ReactiveFormsModule,
  FormBuilder,
  Validators,
} from '@angular/forms';

import {
  Campaign,
  CampaignStatus,
  CreateCampaignRequest,
  UpdateCampaignRequest,
} from '../../models/campaign';
import { CampaignsService } from '../../services/campaigns.service';
import { CampaignStore } from '../../services/campaign-store.service';

/**
 * Campaigns: create, edit, archive, delete, and select campaigns.
 *
 * <p>This page is the single surface for the full campaign lifecycle. It lists every campaign,
 * provides a form for creating new ones, an inline editor for existing ones, destructive-action
 * confirmations for archiving and deleting, and a way to make a campaign the active one.</p>
 */
@Component({
  selector: 'app-campaigns',
  standalone: true,
  templateUrl: './campaigns.component.html',
  styleUrl: './campaigns.component.css',
  imports: [ReactiveFormsModule],
})
export class CampaignsComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly campaigns = inject(CampaignsService);
  private readonly store = inject(CampaignStore);
  private readonly title = inject(Title);

  /** Every campaign, most recently created first. */
  campaignList: Campaign[] = [];

  /** True while the campaign list is being fetched. */
  loading = true;

  /** True while any request is in flight, disabling interactive controls. */
  submitting = false;

  /** Error surfaced by the most recent request, if any. */
  error: string | null = null;

  /** The campaign being edited inline, if any. */
  editingId: number | null = null;

  /** The id whose destructive action (archive/delete) confirmation is shown. */
  pendingAction: { id: number; kind: 'archive' | 'delete'; title: string } | null = null;

  /** Form for creating a new campaign. */
  readonly createForm = this.formBuilder.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: [''],
    status: [CampaignStatus.DRAFT],
    notes: [''],
  });

  /** Form for editing an existing campaign. */
  readonly editForm = this.formBuilder.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: [''],
    status: [CampaignStatus.DRAFT],
    lastPlayedAt: [''],
    notes: [''],
  });

  /** The status values offered by the create and edit forms. */
  readonly statuses = Object.values(CampaignStatus);

  ngOnInit(): void {
    this.title.setTitle('AutoDM - Campaigns');
    this.load();
  }

  /**
   * Fetches the full campaign list and clears transient state.
   */
  load(): void {
    this.loading = true;
    this.error = null;
    this.campaigns.list().subscribe({
      next: (campaigns) => {
        this.campaignList = campaigns;
        this.loading = false;
      },
      error: (err: HttpErrorResponseLike) => {
        this.campaignList = [];
        this.loading = false;
        this.error = err?.message ?? 'Failed to load campaigns.';
      },
    });
  }

  /**
   * Opens the inline editor for the given campaign.
   *
   * @param campaign the campaign to edit
   */
  startEdit(campaign: Campaign): void {
    this.editingId = campaign.id;
    this.editForm.reset({
      title: campaign.title,
      description: campaign.description,
      status: campaign.status,
      lastPlayedAt: this.toLocalDateInput(campaign.lastPlayedAt),
      notes: campaign.notes,
    });
  }

  /**
   * Closes the inline editor without saving.
   */
  cancelEdit(): void {
    this.editingId = null;
    this.editForm.reset();
  }

  /**
   * Persists the changes made in the inline editor.
   *
   * @param campaign the campaign being edited
   */
  saveEdit(campaign: Campaign): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }
    this.setSubmitting(true);
    const value = this.editForm.getRawValue() as UpdateCampaignRequest;
    const request: UpdateCampaignRequest = {
      title: value.title ?? campaign.title,
      description: value.description ?? campaign.description,
      status: value.status as CampaignStatus,
      lastPlayedAt: value.lastPlayedAt ? this.toIsoDate(value.lastPlayedAt) : undefined,
      notes: value.notes ?? campaign.notes,
    };
    this.campaigns.update(campaign.id, request).subscribe({
      next: () => {
        this.editingId = null;
        this.editForm.reset();
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to save campaign.';
      },
    });
  }

  /**
   * Opens the confirmation dialog for the given destructive action.
   *
   * @param id the campaign targeted by the action
   * @param kind whether the action archives or deletes
   */
  confirmAction(id: number, kind: 'archive' | 'delete', title: string): void {
    this.pendingAction = { id, kind, title };
    this.error = null;
  }

  /**
   * Dismisses the confirmation dialog without performing the action.
   */
  cancelAction(): void {
    this.pendingAction = null;
  }

  /**
   * Executes the destructive action confirmed in {@link confirmAction}.
   */
  executeAction(): void {
    if (!this.pendingAction) {
      return;
    }
    const { id, kind } = this.pendingAction;
    this.setSubmitting(true);
    const handler = {
      next: () => {
        this.pendingAction = null;
        this.editingId = null;
        this.editForm.reset();
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? `Failed to ${kind} campaign.`;
      },
    };
    if (kind === 'archive') {
      this.campaigns.archive(id).subscribe(handler);
    } else {
      this.campaigns.delete(id).subscribe(handler);
    }
  }

  /**
   * Makes the given campaign the active campaign, loading its collections.
   *
   * @param campaign the campaign to activate
   */
  select(campaign: Campaign): void {
    this.store.select(campaign.id).subscribe({
      next: () => this.load(),
      error: (err: HttpErrorResponseLike) => {
        this.error = err?.message ?? 'Failed to activate campaign.';
      },
    });
  }

  /**
   * Submits the create campaign form.
   */
  createCampaign(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    this.setSubmitting(true);
    const value = this.createForm.getRawValue() as CreateCampaignRequest;
    const request: CreateCampaignRequest = {
      title: value.title ?? '',
      description: value.description || undefined,
      status: value.status as CampaignStatus,
      notes: value.notes || undefined,
    };
    this.campaigns.create(request).subscribe({
      next: () => {
        this.createForm.reset({ status: CampaignStatus.DRAFT });
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to create campaign.';
      },
    });
  }

  /**
   * @param value whether a request is in flight
   */
  private setSubmitting(value: boolean): void {
    this.submitting = value;
  }

  /**
   * @param value an ISO date string (possibly {@code undefined})
   * @return a value suitable for an {@code <input type="date">}, or {@code ''}
   */
  private toLocalDateInput(value: string | undefined): string {
    return value ? value.slice(0, 10) : '';
  }

  /**
   * @param value an {@code <input type="date">} value
   * @return an ISO {@code yyyy-MM-dd} date string, or {@code undefined} when empty
   */
  private toIsoDate(value: string): string {
    return value ? value.slice(0, 10) : '';
  }
}

interface HttpErrorResponseLike {
  message?: string;
}
