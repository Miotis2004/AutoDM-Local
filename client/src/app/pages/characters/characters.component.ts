import { Component, inject, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';

import {
  Character,
  CreateCharacterRequest,
} from '../../models/character';
import { CharactersService } from '../../services/characters.service';
import { CampaignStore } from '../../services/campaign-store.service';

/**
 * Characters: create, edit, list, and delete player characters for the active campaign.
 *
 * <p>This page lists the campaign roster and offers a form for creating characters, an inline
 * editor for their identity fields, and a destructive-action confirmation before deleting one. It
 * only becomes usable once a campaign has been selected through the campaigns screen.</p>
 */
@Component({
  selector: 'app-characters',
  standalone: true,
  templateUrl: './characters.component.html',
  styleUrls: ['./characters.component.css', '../management-shared.css'],
  imports: [ReactiveFormsModule],
})
export class CharactersComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly characters = inject(CharactersService);
  private readonly store = inject(CampaignStore);
  private readonly title = inject(Title);

  /** Every character in the campaign. */
  characterList: Character[] = [];

  /** True while the roster is being fetched. */
  loading = true;

  /** True while any request is in flight, disabling interactive controls. */
  submitting = false;

  /** Error surfaced by the most recent request, if any. */
  error: string | null = null;

  /** The character being edited inline, if any. */
  editingId: number | null = null;

  /** The id whose destructive action (delete) confirmation is shown. */
  pendingDelete: { id: number; name: string } | null = null;

  /** True once a campaign has been selected; false while it is still pending. */
  get hasCampaign(): boolean {
    return this.store.activeCampaign !== null;
  }

  /** Form for creating a new character. */
  readonly createForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    ancestry: [''],
    characterClass: [''],
    level: [1, [Validators.required, Validators.min(1), Validators.max(20)]],
    background: [''],
    alignment: [''],
  });

  /** Form for editing an existing character's identity. */
  readonly editForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    ancestry: [''],
    characterClass: [''],
    level: [1, [Validators.required, Validators.min(1), Validators.max(20)]],
    background: [''],
    alignment: [''],
  });

  ngOnInit(): void {
    this.title.setTitle('AutoDM - Characters');
    this.store.activeCampaign$.subscribe((campaign) => {
      if (campaign) {
        this.editingId = null;
        this.load();
      }
    });
  }

  /**
   * Fetches the current roster and clears transient state.
   */
  load(): void {
    const campaignId = this.campaignId();
    if (campaignId === null) {
      return;
    }
    this.loading = true;
    this.error = null;
    this.characters.list(campaignId).subscribe({
      next: (characters) => {
        this.characterList = characters;
        this.loading = false;
      },
      error: (err: HttpErrorResponseLike) => {
        this.characterList = [];
        this.loading = false;
        this.error = err?.message ?? 'Failed to load characters.';
      },
    });
  }

  /**
   * @return the active campaign id, or {@code null} while no campaign is selected
   */
  private campaignId(): number | null {
    return this.store.activeCampaign?.id ?? null;
  }

  /**
   * Opens the inline editor for the given character.
   *
   * @param character the character to edit
   */
  startEdit(character: Character): void {
    this.editingId = character.id;
    this.error = null;
    this.editForm.reset({
      name: character.name,
      ancestry: character.ancestry ?? '',
      characterClass: character.characterClass ?? '',
      level: character.level,
      background: character.background ?? '',
      alignment: character.alignment ?? '',
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
   * Persists the identity changes made in the inline editor.
   *
   * @param character the character being edited
   */
  saveEdit(character: Character): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }
    this.setSubmitting(true);
    const value = this.editForm.getRawValue();
    const request: Partial<CreateCharacterRequest> = {
      name: value.name ?? character.name,
      ancestry: value.ancestry || undefined,
      characterClass: value.characterClass || undefined,
      level: value.level ?? character.level,
      background: value.background || undefined,
      alignment: value.alignment || undefined,
    };
    this.characters.updateIdentity(this.campaignId()!, character.id, request).subscribe({
      next: () => {
        this.editingId = null;
        this.editForm.reset();
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to save character.';
      },
    });
  }

  /**
   * Opens the confirmation dialog for deleting a character.
   *
   * @param character the character to delete
   */
  confirmDelete(character: Character): void {
    this.pendingDelete = { id: character.id, name: character.name };
    this.error = null;
  }

  /**
   * Dismisses the confirmation dialog without deleting.
   */
  cancelDelete(): void {
    this.pendingDelete = null;
  }

  /**
   * Deletes the character confirmed in {@link confirmDelete}.
   */
  executeDelete(): void {
    if (!this.pendingDelete) {
      return;
    }
    this.setSubmitting(true);
    this.characters
      .delete(this.campaignId()!, this.pendingDelete.id)
      .subscribe({
        next: () => {
          this.pendingDelete = null;
          this.setSubmitting(false);
          this.load();
        },
        error: (err: HttpErrorResponseLike) => {
          this.setSubmitting(false);
          this.error = err?.message ?? 'Failed to delete character.';
        },
      });
  }

  /**
   * Submits the create character form.
   */
  createCharacter(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    this.setSubmitting(true);
    const value = this.createForm.getRawValue();
    const request: CreateCharacterRequest = {
      name: value.name ?? '',
      ancestry: value.ancestry || undefined,
      characterClass: value.characterClass || undefined,
      level: value.level ?? 1,
      background: value.background || undefined,
      alignment: value.alignment || undefined,
    };
    this.characters.create(this.campaignId()!, request).subscribe({
      next: () => {
        this.createForm.reset({ level: 1 });
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to create character.';
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
    const errors = control.errors;
    if (errors['required']) {
      return 'This field is required.';
    }
    if (errors['min']) {
      return 'Level must be at least 1.';
    }
    if (errors['max']) {
      return 'Level must be 20 or lower.';
    }
    if (errors['maxLength']) {
      return 'Name is too long.';
    }
    return 'Invalid value.';
  }

  /**
   * @param character the character to describe
   * @return a short badge label for the character's level
   */
  levelText(character: Character): string {
    return `Level ${character.level}`;
  }

  /**
   * Joins the non-empty identity parts of a character (such as ancestry and
   * class) into a single short line, so templates can render them without
   * relying on {@code Boolean} directly.
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
