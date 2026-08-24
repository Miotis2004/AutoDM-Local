import { Component, inject, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';

import {
  CreateCreatureTemplateRequest,
  CreatureTemplate,
} from '../../models/creature-template';
import { CreatureTemplatesService } from '../../services/creature-template.service';
import { CampaignStore } from '../../services/campaign-store.service';

/**
 * Creature templates: create, edit, and list blueprints for the active campaign.
 *
 * <p>This page lists the campaign's creature templates. Templates are created through a form, can
 * be edited, and can be removed. The back-end exposes create, list and delete endpoints for the
 * templates, so this screen implements those operations with validation feedback and empty
 * states.</p>
 */
@Component({
  selector: 'app-creature-templates',
  standalone: true,
  templateUrl: './creature-templates.component.html',
  styleUrls: ['./creature-templates.component.css', '../management-shared.css'],
  imports: [ReactiveFormsModule],
})
export class CreatureTemplatesComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly templates = inject(CreatureTemplatesService);
  private readonly store = inject(CampaignStore);
  private readonly title = inject(Title);

  templateList: CreatureTemplate[] = [];
  loading = true;
  submitting = false;
  error: string | null = null;
  editingId: number | null = null;
  deletingId: number | null = null;

  get hasCampaign(): boolean {
    return this.store.activeCampaign !== null;
  }

  readonly createForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    description: [''],
    health: [''],
    defense: [''],
    attack: [''],
    damage: [''],
    initiativeModifier: [''],
    behaviorNotes: [''],
  });

  readonly editForm = this.formBuilder.group({
    description: [''],
    health: [''],
    defense: [''],
    attack: [''],
    damage: [''],
    initiativeModifier: [''],
    behaviorNotes: [''],
  });

  ngOnInit(): void {
    this.title.setTitle('AutoDM - Creature Templates');
    this.store.activeCampaign$.subscribe((campaign) => {
      if (campaign) {
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
    this.templates.list(campaignId).subscribe({
      next: (list) => {
        this.templateList = list;
        this.loading = false;
      },
      error: (err: HttpErrorResponseLike) => {
        this.templateList = [];
        this.loading = false;
        this.error = err?.message ?? 'Failed to load creature templates.';
      },
    });
  }

  private campaignId(): number | null {
    return this.store.activeCampaign?.id ?? null;
  }

  createTemplate(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    this.setSubmitting(true);
    const value = this.createForm.getRawValue();
    const request: CreateCreatureTemplateRequest = {
      name: value.name ?? '',
      description: value.description || undefined,
      health: this.parseNumber(value.health),
      defense: this.parseNumber(value.defense),
      attack: this.parseNumber(value.attack),
      damage: this.parseNumber(value.damage),
      initiativeModifier: this.parseNumber(value.initiativeModifier),
      behaviorNotes: value.behaviorNotes || undefined,
    };
    this.templates.create(this.campaignId()!, request).subscribe({
      next: () => {
        this.createForm.reset();
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to create creature template.';
      },
    });
  }

  openEdit(template: CreatureTemplate): void {
    this.editForm.reset({
      description: template.description ?? '',
      health: template.health != null ? String(template.health) : '',
      defense: template.defense != null ? String(template.defense) : '',
      attack: template.attack != null ? String(template.attack) : '',
      damage: template.damage != null ? String(template.damage) : '',
      initiativeModifier: template.initiativeModifier != null
        ? String(template.initiativeModifier)
        : '',
      behaviorNotes: template.behaviorNotes ?? '',
    });
    this.editingId = template.id;
  }

  cancelEdit(): void {
    this.editingId = null;
  }

  saveEdit(): void {
    if (this.editForm.invalid || this.editingId === null) {
      this.editForm.markAllAsTouched();
      return;
    }
    this.setSubmitting(true);
    const value = this.editForm.getRawValue();
    const patch: Omit<CreateCreatureTemplateRequest, 'name'> = {
      description: value.description || undefined,
      health: this.parseNumber(value.health),
      defense: this.parseNumber(value.defense),
      attack: this.parseNumber(value.attack),
      damage: this.parseNumber(value.damage),
      initiativeModifier: this.parseNumber(value.initiativeModifier),
      behaviorNotes: value.behaviorNotes || undefined,
    };
    if (this.editingId === null) {
      return;
    }
    this.templates.update(this.campaignId()!, this.editingId, patch).subscribe({
        next: () => {
          this.editingId = null;
          this.setSubmitting(false);
          this.load();
        },
        error: (err: HttpErrorResponseLike) => {
          this.setSubmitting(false);
          this.error = err?.message ?? 'Failed to save creature template.';
        },
      });
  }

  confirmDelete(templateId: number): void {
    this.deletingId = templateId;
  }

  cancelDelete(): void {
    this.deletingId = null;
  }

  deleteTemplate(): void {
    const templateId = this.deletingId;
    if (templateId === null) {
      return;
    }
    this.setSubmitting(true);
    this.templates.delete(this.campaignId()!, templateId).subscribe({
      next: () => {
        this.deletingId = null;
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to delete creature template.';
      },
    });
  }

  field(name: string): import('@angular/forms').FormControl {
    return this.editForm.get(name) as import('@angular/forms').FormControl;
  }

  getError(controlName: string): string | null {
    const control = this.createForm.get(controlName);
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

  private parseNumber(value: string | null | undefined): number | undefined {
    if (value === undefined || value === null || value === '') {
      return undefined;
    }
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }

  private setSubmitting(value: boolean): void {
    this.submitting = value;
  }
}

interface HttpErrorResponseLike {
  message?: string;
}
