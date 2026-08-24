import { Component, inject, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { forkJoin } from 'rxjs';

import {
  ReactiveFormsModule,
  FormBuilder,
  Validators,
} from '@angular/forms';

import {
  CreateLocationRequest,
  CreatePointOfInterestRequest,
  CreateRegionRequest,
  CreateSettlementRequest,
  Location,
  PointOfInterest,
  PointOfInterestCategory,
  Region,
  Settlement,
  SettlementType,
} from '../../models/world';
import { WorldService } from '../../services/world.service';
import { CampaignStore } from '../../services/campaign-store.service';

/**
 * The world: create and manage the places a campaign's world is made of.
 *
 * <p>This page is the hub for world setup. It lets the operator create the four kinds of world
 * data the back-end persists for a campaign - {@link Region}s, {@link Location}s,
 * {@link Settlement}s, and {@link PointOfInterest}s - and lists what already exists for each. Every
 * mutation is forwarded to the {@link WorldService}, which wraps the {@code /api/campaigns/{campaignId}/...}
 * world endpoints, so the data is stored in the campaign and reloads across sessions.</p>
 */
@Component({
  selector: 'app-world',
  standalone: true,
  templateUrl: './world.component.html',
  styleUrls: ['./world.component.css', '../management-shared.css'],
  imports: [ReactiveFormsModule],
})
export class WorldComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly world = inject(WorldService);
  private readonly store = inject(CampaignStore);
  private readonly title = inject(Title);

  /** Every region, for grouping locations and settlements. */
  regionList: Region[] = [];

  /** Every location in the campaign. */
  locationList: Location[] = [];

  /** Every settlement in the campaign. */
  settlementList: Settlement[] = [];

  /** Every point of interest in the campaign. */
  poiList: PointOfInterest[] = [];

  /** True while any list is being fetched. */
  loading = true;

  /** True while any request is in flight, disabling interactive controls. */
  submitting = false;

  /** Error surfaced by the most recent request, if any. */
  error: string | null = null;

  /** Form for creating a region. */
  readonly regionForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
  });

  /** Form for creating a location. */
  readonly locationForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    regionId: [''],
  });

  /** Form for creating a settlement. The back-end creates its backing location. */
  readonly settlementForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    type: [SettlementType.VILLAGE],
    population: ['100'],
    regionId: [''],
  });

  /** Form for creating a point of interest. The back-end creates its backing location. */
  readonly poiForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    category: [PointOfInterestCategory.LANDMARK],
    regionId: [''],
  });

  /** The settlement types offered by the settlement form. */
  readonly settlementTypes = Object.values(SettlementType);

  /** The point-of-interest categories offered by the point-of-interest form. */
  readonly categories = Object.values(PointOfInterestCategory);

  ngOnInit(): void {
    this.title.setTitle('AutoDM - World');
    this.store.activeCampaign$.subscribe((campaign) => {
      if (campaign) {
        this.load();
      }
    });
  }

  /**
   * Whether a campaign is active, so the world can be managed.
   */
  get hasCampaign(): boolean {
    return this.store.activeCampaign !== null;
  }

  /**
   * Loads every world collection for the active campaign.
   */
  load(): void {
    const campaignId = this.campaignId();
    if (campaignId === null) {
      return;
    }
    this.loading = true;
    this.error = null;
    forkJoin({
      regions: this.world.listRegions(campaignId),
      locations: this.world.listLocations(campaignId),
      settlements: this.world.listSettlements(campaignId),
      pois: this.world.listPointsOfInterest(campaignId),
    }).subscribe({
      next: ({ regions, locations, settlements, pois }) => {
        this.regionList = regions ?? [];
        this.locationList = locations ?? [];
        this.settlementList = settlements ?? [];
        this.poiList = pois ?? [];
        this.loading = false;
      },
      error: (err: HttpErrorResponseLike) => {
        this.loading = false;
        this.error = err?.message ?? 'Failed to load world.';
      },
    });
  }

  /**
   * The active campaign id, or {@code null} when none is selected.
   */
  private campaignId(): number | null {
    return this.store.activeCampaign?.id ?? null;
  }

  /**
   * Creates a new region.
   */
  createRegion(): void {
    if (this.regionForm.invalid) {
      this.regionForm.markAllAsTouched();
      return;
    }
    this.setSubmitting(true);
    const value = this.regionForm.getRawValue() as CreateRegionRequest;
    const request: CreateRegionRequest = {
      name: value.name ?? '',
      description: value.description || undefined,
    };
    this.world.createRegion(this.campaignId()!, request).subscribe({
      next: () => {
        this.regionForm.reset();
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to create region.';
      },
    });
  }

  /**
   * Creates a new location, optionally grouped under a region.
   */
  createLocation(): void {
    if (this.locationForm.invalid) {
      this.locationForm.markAllAsTouched();
      return;
    }
    this.setSubmitting(true);
    const value = this.locationForm.getRawValue();
    const request: CreateLocationRequest = {
      name: value.name ?? '',
      description: value.description || undefined,
      regionId: this.parseNumber(value.regionId as string | null | undefined),
    };
    this.world.createLocation(this.campaignId()!, request).subscribe({
      next: () => {
        this.locationForm.reset();
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to create location.';
      },
    });
  }

  /**
   * Creates a new settlement. The back-end creates the backing location for it.
   */
  createSettlement(): void {
    if (this.settlementForm.invalid) {
      this.settlementForm.markAllAsTouched();
      return;
    }
    this.setSubmitting(true);
    const value = this.settlementForm.getRawValue();
    const request: CreateSettlementRequest = {
      name: value.name ?? '',
      type: (value.type as SettlementType) ?? SettlementType.VILLAGE,
      population: this.parseNumber(value.population as string | null | undefined),
      regionId: this.parseNumber(value.regionId as string | null | undefined),
    };
    this.world.createSettlement(this.campaignId()!, request).subscribe({
      next: () => {
        this.settlementForm.reset();
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to create settlement.';
      },
    });
  }

  /**
   * Creates a new point of interest. The back-end creates the backing location for it.
   */
  createPointOfInterest(): void {
    if (this.poiForm.invalid) {
      this.poiForm.markAllAsTouched();
      return;
    }
    this.setSubmitting(true);
    const value = this.poiForm.getRawValue();
    const request: CreatePointOfInterestRequest = {
      name: value.name ?? '',
      category: (value.category as PointOfInterestCategory) ?? PointOfInterestCategory.LANDMARK,
      description: value.description || undefined,
    };
    this.world.createPointOfInterest(this.campaignId()!, request).subscribe({
      next: () => {
        this.poiForm.reset();
        this.setSubmitting(false);
        this.load();
      },
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to create point of interest.';
      },
    });
  }

  /**
   * Returns the validation message for a control, or {@code null} when it is valid.
   *
   * @param controlName the name of the control
   * @return a human-readable error, or {@code null}
   */
  getError(controlName: string): string | null {
    const control = this.regionForm.get(controlName)
      ?? this.locationForm.get(controlName)
      ?? this.settlementForm.get(controlName)
      ?? this.poiForm.get(controlName);
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
   * Converts a string control value to a number, or {@code undefined} when empty or invalid.
   *
   * @param value the raw control value
   * @return the parsed number, or {@code undefined}
   */
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
