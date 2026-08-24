import { Component, inject, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';

import {
  CreateInventoryItemRequest,
  InventoryItem,
  InventoryOwnerKind,
  ItemCategory,
} from '../../models/item';
import { Character } from '../../models/character';
import { ItemsService } from '../../services/items.service';
import { CampaignStore } from '../../services/campaign-store.service';

/**
 * Inventory items: create, edit, and list holdings for the active campaign.
 *
 * <p>This page lists the campaign's inventory holdings. Holdings are created through a form, can be
 * edited, and can be removed. The back-end exposes create, list and delete endpoints for the
 * inventory, so this screen implements those operations with validation feedback and empty
 * states.</p>
 */
@Component({
  selector: 'app-items',
  standalone: true,
  templateUrl: './items.component.html',
  styleUrls: ['./items.component.css', '../management-shared.css'],
  imports: [ReactiveFormsModule],
})
export class ItemsComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly items = inject(ItemsService);
  private readonly store = inject(CampaignStore);
  private readonly title = inject(Title);

  itemList: InventoryItem[] = [];
  characters: Character[] = [];
  loading = true;
  submitting = false;
  error: string | null = null;
  editingId: number | null = null;
  deletingId: number | null = null;

  get hasCampaign(): boolean {
    return this.store.activeCampaign !== null;
  }

  readonly categoryOptions = Object.values(ItemCategory);
  readonly ownerOptions: { kind: InventoryOwnerKind; label: string }[] = [
    { kind: InventoryOwnerKind.CAMPAIGN, label: 'Campaign stash' },
    { kind: InventoryOwnerKind.PLAYER, label: 'A character' },
  ];

  readonly createForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    category: [ItemCategory.MISCELLANEOUS, [Validators.required]],
    quantity: ['1', [Validators.required, Validators.min(1)]],
    value: [''],
    description: [''],
    ownerKind: [InventoryOwnerKind.CAMPAIGN, [Validators.required]],
    ownerId: [''],
  });

  readonly editForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    category: [ItemCategory.MISCELLANEOUS, [Validators.required]],
    quantity: ['', [Validators.required, Validators.min(1)]],
    value: [''],
    description: [''],
  });

  ngOnInit(): void {
    this.title.setTitle('AutoDM - Inventory');
    this.store.activeCampaign$.subscribe((campaign) => {
      if (campaign) {
        this.store.characters$.subscribe((chars) => {
          this.characters = chars;
        });
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
    this.items.list(campaignId).subscribe({
      next: (list) => {
        this.itemList = list;
        this.loading = false;
      },
      error: (err: HttpErrorResponseLike) => {
        this.itemList = [];
        this.loading = false;
        this.error = err?.message ?? 'Failed to load inventory.';
      },
    });
  }

  private campaignId(): number | null {
    return this.store.activeCampaign?.id ?? null;
  }

  createItem(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    this.setSubmitting(true);
    const value = this.createForm.getRawValue();
    const ownerKind = value.ownerKind ?? InventoryOwnerKind.CAMPAIGN;
    const ownerId = ownerKind === InventoryOwnerKind.PLAYER
      ? this.parseNumber(value.ownerId) ?? this.campaignId()!
      : this.campaignId()!;
    const request: CreateInventoryItemRequest = {
      name: value.name ?? '',
      category: value.category ?? ItemCategory.MISCELLANEOUS,
      quantity: this.parseNumber(value.quantity),
      value: this.parseNumber(value.value),
      description: value.description || undefined,
      ownerKind,
      ownerId,
    };
    this.items.create(this.campaignId()!, request).subscribe({
      next: () => {
        this.createForm.reset();
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to create item.';
      },
    });
  }

  openEdit(item: InventoryItem): void {
    this.editForm.reset({
      name: item.name,
      category: item.category,
      quantity: String(item.quantity),
      value: item.value ? String(item.value) : '',
      description: item.description ?? '',
    });
    this.editingId = item.id;
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
    this.items
      .update(
        this.campaignId()!,
        this.editingId,
        {
          name: value.name ?? '',
          category: value.category ?? ItemCategory.MISCELLANEOUS,
          quantity: this.parseNumber(value.quantity),
          value: this.parseNumber(value.value),
          description: value.description || undefined,
        },
      )
      .subscribe({
        next: () => {
          this.editingId = null;
          this.setSubmitting(false);
          this.load();
        },
        error: (err: HttpErrorResponseLike) => {
          this.setSubmitting(false);
          this.error = err?.message ?? 'Failed to save item.';
        },
      });
  }

  confirmDelete(itemId: number): void {
    this.deletingId = itemId;
  }

  cancelDelete(): void {
    this.deletingId = null;
  }

  deleteItem(): void {
    const itemId = this.deletingId;
    if (itemId === null) {
      return;
    }
    this.setSubmitting(true);
    this.items.delete(this.campaignId()!, itemId).subscribe({
      next: () => {
        this.deletingId = null;
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to delete item.';
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
    if (control.errors['min']) {
      return 'Quantity must be at least 1.';
    }
    return 'Invalid value.';
  }

  ownerLabel(item: InventoryItem): string {
    if (item.ownerKind === InventoryOwnerKind.PLAYER) {
      const character = this.characters.find((c) => c.id === item.ownerId);
      return character ? character.name : 'Unknown character';
    }
    return 'Campaign stash';
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
