import { Component, inject, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';

import { CreateQuestRequest, Quest, QuestStatus } from '../../models/quest';
import { QuestsService } from '../../services/quests.service';
import { CampaignStore } from '../../services/campaign-store.service';

/**
 * Quests: create, edit, list, complete, fail, and delete quests for the active campaign.
 *
 * <p>This page lists the campaign's quests, provides a form for creating quests, an inline editor
 * for their identity and status, and destructive-action confirmations before deleting one.</p>
 */
@Component({
  selector: 'app-quests',
  standalone: true,
  templateUrl: './quests.component.html',
  styleUrls: ['./quests.component.css', '../management-shared.css'],
  imports: [ReactiveFormsModule],
})
export class QuestsComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly quests = inject(QuestsService);
  private readonly store = inject(CampaignStore);
  private readonly title = inject(Title);

  questList: Quest[] = [];
  loading = true;
  submitting = false;
  error: string | null = null;
  editingId: number | null = null;
  pendingDelete: { id: number; title: string } | null = null;

  readonly statuses = Object.values(QuestStatus);

  get hasCampaign(): boolean {
    return this.store.activeCampaign !== null;
  }

  readonly createForm = this.formBuilder.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: [''],
    giver: [''],
    rewards: [''],
    notes: [''],
  });

  readonly editForm = this.formBuilder.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: [''],
    giver: [''],
    rewards: [''],
    status: [QuestStatus.ACTIVE],
  });

  ngOnInit(): void {
    this.title.setTitle('AutoDM - Quests');
    this.store.activeCampaign$.subscribe((campaign) => {
      if (campaign) {
        this.editingId = null;
        this.load();
      }
    });
  }

  load(): void {
    const campaignId = this.campaignId();
    if (campaignId === null) {
      return;
    }
    this.loading = true;
    this.error = null;
    this.quests.list(campaignId).subscribe({
      next: (quests) => {
        this.questList = quests;
        this.loading = false;
      },
      error: (err: HttpErrorResponseLike) => {
        this.questList = [];
        this.loading = false;
        this.error = err?.message ?? 'Failed to load quests.';
      },
    });
  }

  private campaignId(): number | null {
    return this.store.activeCampaign?.id ?? null;
  }

  startEdit(quest: Quest): void {
    this.editingId = quest.id;
    this.error = null;
    this.editForm.reset({
      title: quest.title,
      description: quest.description ?? '',
      giver: quest.giver ?? '',
      rewards: quest.rewards ?? '',
      status: quest.status ?? QuestStatus.ACTIVE,
    });
  }

  cancelEdit(): void {
    this.editingId = null;
    this.editForm.reset();
  }

  saveEdit(quest: Quest): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }
    this.setSubmitting(true);
    const value = this.editForm.getRawValue();
    const request: Partial<{
      title: string;
      description: string;
      giver: string;
      rewards: string;
      status: QuestStatus;
    }> = {
      title: value.title ?? quest.title,
      description: value.description || undefined,
      giver: value.giver || undefined,
      rewards: value.rewards || undefined,
      status: value.status ?? QuestStatus.ACTIVE,
    };
    this.quests.update(this.campaignId()!, quest.id, request).subscribe({
      next: () => {
        this.editingId = null;
        this.editForm.reset();
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to save quest.';
      },
    });
  }

  complete(quest: Quest): void {
    this.setSubmitting(true);
    this.quests.complete(this.campaignId()!, quest.id).subscribe({
      next: () => this.load(),
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to complete quest.';
      },
    });
  }

  fail(quest: Quest): void {
    this.setSubmitting(true);
    this.quests.fail(this.campaignId()!, quest.id).subscribe({
      next: () => this.load(),
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to fail quest.';
      },
    });
  }

  confirmDelete(quest: Quest): void {
    this.pendingDelete = { id: quest.id, title: quest.title };
    this.error = null;
  }

  cancelDelete(): void {
    this.pendingDelete = null;
  }

  executeDelete(): void {
    if (!this.pendingDelete) {
      return;
    }
    this.setSubmitting(true);
    this.quests.delete(this.campaignId()!, this.pendingDelete.id).subscribe({
      next: () => {
        this.pendingDelete = null;
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to delete quest.';
      },
    });
  }

  createQuest(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    this.setSubmitting(true);
    const value = this.createForm.getRawValue();
    const request: CreateQuestRequest = {
      title: value.title ?? '',
      description: value.description || undefined,
      giver: value.giver || undefined,
      rewards: value.rewards || undefined,
      notes: value.notes || undefined,
    };
    this.quests.create(this.campaignId()!, request).subscribe({
      next: () => {
        this.createForm.reset();
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to create quest.';
      },
    });
  }

  private setSubmitting(value: boolean): void {
    this.submitting = value;
  }

  /**
   * Returns the validation message for a control, or {@code null} when it is valid.
   *
   * @param controlName the name of the control
   * @return a human-readable error, or {@code null}
   */
  getError(controlName: string): string | null {
    const control = this.createForm.get(controlName) ?? this.editForm.get(controlName);
    if (!control?.errors) {
      return null;
    }
    if (control.errors['required']) {
      return 'This field is required.';
    }
    if (control.errors['maxLength']) {
      return 'Title is too long.';
    }
    return 'Invalid value.';
  }
}

interface HttpErrorResponseLike {
  message?: string;
}
