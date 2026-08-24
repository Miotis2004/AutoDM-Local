import { Component, inject, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';

import { CreateNpcRequest, Disposition, Npc } from '../../models/npc';
import { NpcsService } from '../../services/npcs.service';
import { CampaignStore } from '../../services/campaign-store.service';

/**
 * NPCs: create, edit, list, and delete non-player characters for the active campaign.
 *
 * <p>This page lists the campaign's NPCs and offers a form for creating NPCs, an inline editor for
 * their story fields, active/inactive toggling, and a destructive-action confirmation before
 * deleting one.</p>
 */
@Component({
  selector: 'app-npcs',
  standalone: true,
  templateUrl: './npcs.component.html',
  styleUrls: ['./npcs.component.css', '../management-shared.css'],
  imports: [ReactiveFormsModule],
})
export class NpcsComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly npcs = inject(NpcsService);
  private readonly store = inject(CampaignStore);
  private readonly title = inject(Title);

  npcList: Npc[] = [];
  loading = true;
  submitting = false;
  error: string | null = null;
  editingId: number | null = null;
  pendingDelete: { id: number; name: string } | null = null;

  get hasCampaign(): boolean {
    return this.store.activeCampaign !== null;
  }

  readonly createForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: [''],
    role: [''],
    disposition: [Disposition.NEUTRAL],
    faction: [''],
    notes: [''],
  });

  readonly editForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: [''],
    role: [''],
    disposition: [Disposition.NEUTRAL],
    faction: [''],
  });

  readonly dispositions = Object.values(Disposition);

  ngOnInit(): void {
    this.title.setTitle('AutoDM - NPCs');
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
    this.npcs.list(campaignId).subscribe({
      next: (npcs) => {
        this.npcList = npcs;
        this.loading = false;
      },
      error: (err: HttpErrorResponseLike) => {
        this.npcList = [];
        this.loading = false;
        this.error = err?.message ?? 'Failed to load NPCs.';
      },
    });
  }

  private campaignId(): number | null {
    return this.store.activeCampaign?.id ?? null;
  }

  startEdit(npc: Npc): void {
    this.editingId = npc.id;
    this.error = null;
    this.editForm.reset({
      name: npc.name,
      description: npc.description ?? '',
      role: npc.role ?? '',
      disposition: npc.disposition ?? Disposition.NEUTRAL,
      faction: npc.faction ?? '',
    });
  }

  cancelEdit(): void {
    this.editingId = null;
    this.editForm.reset();
  }

  saveEdit(npc: Npc): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }
    this.setSubmitting(true);
    const value = this.editForm.getRawValue();
    const request: Partial<CreateNpcRequest> = {
      name: value.name ?? npc.name,
      description: value.description || undefined,
      role: value.role || undefined,
      disposition: value.disposition ?? Disposition.NEUTRAL,
      faction: value.faction || undefined,
    };
    this.npcs.update(this.campaignId()!, npc.id, request).subscribe({
      next: () => {
        this.editingId = null;
        this.editForm.reset();
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to save NPC.';
      },
    });
  }

  /**
   * Toggles an NPC's active state.
   *
   * @param npc the NPC to toggle
   */
  toggleActive(npc: Npc): void {
    this.setSubmitting(true);
    this.npcs.setActive(this.campaignId()!, npc.id, !npc.active).subscribe({
      next: () => this.load(),
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to update NPC status.';
      },
    });
  }

  confirmDelete(npc: Npc): void {
    this.pendingDelete = { id: npc.id, name: npc.name };
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
    this.npcs.delete(this.campaignId()!, this.pendingDelete.id).subscribe({
      next: () => {
        this.pendingDelete = null;
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to delete NPC.';
      },
    });
  }

  createNpc(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    this.setSubmitting(true);
    const value = this.createForm.getRawValue();
    const request: CreateNpcRequest = {
      name: value.name ?? '',
      description: value.description || undefined,
      role: value.role || undefined,
      disposition: value.disposition ?? Disposition.NEUTRAL,
      faction: value.faction || undefined,
      notes: value.notes || undefined,
    };
    this.npcs.create(this.campaignId()!, request).subscribe({
      next: () => {
        this.createForm.reset({ disposition: Disposition.NEUTRAL });
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to create NPC.';
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

  /**
   * Joins the non-empty identity parts of an NPC (such as role and
   * disposition) into a single short line, so templates can render them
   * without relying on {@code Boolean} directly.
   *
   * @param values the candidate parts, in display order
   * @return the non-empty parts joined by a bullet, or an empty string
   */
  identityLine(values: Array<string | undefined>): string {
    return values
      .filter((value): value is string => Boolean(value))
      .join(' · ');
  }
}

interface HttpErrorResponseLike {
  message?: string;
}
