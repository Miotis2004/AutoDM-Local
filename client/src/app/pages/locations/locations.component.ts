import { Component, inject, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';

import { CreateLocationRequest, Location, Region } from '../../models/world';
import { WorldService } from '../../services/world.service';
import { CampaignStore } from '../../services/campaign-store.service';

/**
 * Locations: create, list, and discover places for the active campaign.
 *
 * <p>This page lists the campaign's locations. Locations are created through a form and can be
 * marked discovered as the party explores them. The back-end exposes create, list and discover
 * endpoints for locations, so this screen implements those operations with validation feedback and
 * empty states.</p>
 */
@Component({
  selector: 'app-locations',
  standalone: true,
  templateUrl: './locations.component.html',
  styleUrls: ['./locations.component.css', '../management-shared.css'],
  imports: [ReactiveFormsModule],
})
export class LocationsComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly world = inject(WorldService);
  private readonly store = inject(CampaignStore);
  private readonly title = inject(Title);

  locationList: Location[] = [];
  regionList: Region[] = [];
  loading = true;
  submitting = false;
  error: string | null = null;

  get hasCampaign(): boolean {
    return this.store.activeCampaign !== null;
  }

  readonly createForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: [''],
    regionId: [''],
    latitude: [''],
    longitude: [''],
  });

  ngOnInit(): void {
    this.title.setTitle('AutoDM - Locations');
    this.store.activeCampaign$.subscribe((campaign) => {
      if (campaign) {
        this.load();
        this.store.regions$.subscribe((regions) => {
          this.regionList = regions;
        });
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
    this.world.listLocations(campaignId).subscribe({
      next: (locations) => {
        this.locationList = locations;
        this.loading = false;
      },
      error: (err: HttpErrorResponseLike) => {
        this.locationList = [];
        this.loading = false;
        this.error = err?.message ?? 'Failed to load locations.';
      },
    });
  }

  private campaignId(): number | null {
    return this.store.activeCampaign?.id ?? null;
  }

  createLocation(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    this.setSubmitting(true);
    const value = this.createForm.getRawValue();
    const request: CreateLocationRequest = {
      name: value.name ?? '',
      description: value.description || undefined,
      regionId: this.parseNumber(value.regionId),
      latitude: this.parseNumber(value.latitude),
      longitude: this.parseNumber(value.longitude),
    };
    this.world.createLocation(this.campaignId()!, request).subscribe({
      next: () => {
        this.createForm.reset();
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
   * Marks a location as discovered.
   *
   * @param location the location to discover
   */
  discover(location: Location): void {
    if (location.discovered) {
      return;
    }
    this.setSubmitting(true);
    this.world.discover(this.campaignId()!, location.id).subscribe({
      next: () => this.load(),
      error: (err: HttpErrorResponseLike) => {
        this.setSubmitting(false);
        this.error = err?.message ?? 'Failed to discover location.';
      },
    });
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
