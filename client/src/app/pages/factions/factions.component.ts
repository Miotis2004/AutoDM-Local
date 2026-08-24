import { Component, inject, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';

import {
  CreateFactionRequest,
  Disposition,
  Faction,
  NpcRelationship,
} from '../../models/faction';
import { FactionsService } from '../../services/faction.service';
import { CampaignStore } from '../../services/campaign-store.service';

/**
 * Factions: create, edit, list, and delete organizations and powers for the active campaign.
 *
 * <p>This page lists the campaign's factions, provides a form for creating factions, an inline
 * editor for their story fields, and a destructive-action confirmation before deleting one.</p>
 */
@Component({
  selector: 'app-factions',
  standalone: true,
  templateUrl: './factions.component.html',
  styleUrls: ['./factions.component.css', '../management-shared.css'],
  imports: [ReactiveFormsModule],
})
export class FactionsComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly factions = inject(FactionsService);
  private readonly store = inject(CampaignStore);
  private readonly title = inject(Title);

  factionList: Faction[] = [];
  loading = true;
  submitting = false;
  error: string | null = null;
  editingId: number | null = null;
  pendingDelete: { id: number; name: string } | null = null;

  readonly dispositions = Object.values(Disposition);
  readonly reputations = Object.values(NpcRelationship);

  get hasCampaign(): boolean {
    return this.store.activeCampaign !== null;
  }

  readonly createForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: [''],
    disposition: [Disposition.NEUTRAL],
    reputation: [NpcRelationship.NEUTRAL],
  });

  readonly editForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: [''],
    disposition: [Disposition.NEUTRAL],
    reputation: [NpcRelationship.NEUTRAL],
  });

  ngOnInit(): void {
    this.title.setTitle('AutoDM - Factions');
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
    this.factions.list(campaignId).subscribe({
      next: (factions) => {
        this.factionList = factions;
        this.loading = false;
      },
      error: (err: HttpErrorResponseLike) => {
        this.factionList = [];
        this.loading = false;
        this.error = err?.message ?? 'Failed to load factions.';
      },
    });
  }

  private campaignId(): number | null {
    return this.store.activeCampaign?.id ?? null;
  }

  startEdit(faction: Faction): void {
    this.editingId = faction.id;
    this.error = null;
    this.editForm.reset({
      name: faction.name,
      description: faction.description ?? '',
      disposition: faction.disposition ?? Disposition.NEUTRAL,
      reputation: faction.reputation ?? NpcRelationship.NEUTRAL,
    });
  }

  cancelEdit(): void {
    this.editingId = null;
    this.editForm.reset();
  }

  saveEdit(faction: Faction): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }
    this.setSubmitting(true);
    const value = this.editForm.getRawValue();
    const request: Partial<CreateFactionRequest> = {
      name: value.name ?? faction.name,
      description: value.description || undefined,
      disposition: value.disposition ?? Disposition.NEUTRAL,
      reputation: value.reputation ?? NpcRelationship.NEUTRAL,
    };
    this.factions.update(this.campaignId()!, faction.id, request).subscribe({
      next: () => {
        this.editingId = null;
        this.editForm.reset();
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to save faction.';
      },
    });
  }

  confirmDelete(faction: Faction): void {
    this.pendingDelete = { id: faction.id, name: faction.name };
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
    this.factions.delete(this.campaignId()!, this.pendingDelete.id).subscribe({
      next: () => {
        this.pendingDelete = null;
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to delete faction.';
      },
    });
  }

  createFaction(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    this.setSubmitting(true);
    const value = this.createForm.getRawValue();
    const request: CreateFactionRequest = {
      name: value.name ?? '',
      description: value.description || undefined,
      disposition: value.disposition ?? Disposition.NEUTRAL,
      reputation: value.reputation ?? NpcRelationship.NEUTRAL,
    };
    this.factions.create(this.campaignId()!, request).subscribe({
      next: () => {
        this.createForm.reset({ disposition: Disposition.NEUTRAL, reputation: NpcRelationship.NEUTRAL });
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to create faction.';
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
      return 'Name is too long.';
    }
    return 'Invalid value.';
  }
}

interface HttpErrorResponseLike {
  message?: string;
}
