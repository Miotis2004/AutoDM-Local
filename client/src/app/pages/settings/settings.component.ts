import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Title } from '@angular/platform-browser';

import { AutoDMSettings } from '../../models/settings';
import { SettingsService } from '../../services/settings.service';

/**
 * The settings screen.
 *
 * <p>Exposes the local, client-side configuration of the application - primarily the storage
 * location and display preferences. All values are persisted to {@code localStorage} and are never
 * sent to the back-end, so this screen works without any internet connectivity.</p>
 */
@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.css',
})
export class SettingsComponent implements OnInit {
  private readonly title = inject(Title);
  private readonly settingsService = inject(SettingsService);

  /** The current settings, surfaced reactively to the template. */
  readonly settings = this.settingsService.settings;

  /** The storage path being edited before it is saved. */
  storagePath = '';

  /** The colour theme currently selected. */
  theme: AutoDMSettings['theme'] = 'system';

  /** The base font size currently selected. */
  fontSize: AutoDMSettings['fontSize'] = 'medium';

  /** Whether compact display mode is on. */
  compactMode = false;

  /** Shown after the operator saves, then auto-dismissed. */
  savedMessage = '';

  /** How many settings were changed relative to the defaults, shown in the header. */
  changedCount = 0;

  ngOnInit(): void {
    this.title.setTitle('AutoDM - Settings');
    this.syncFromService();
  }

  /** Copies the service state into the local form fields. */
  private syncFromService(): void {
    const current = this.settingsService.get();
    this.storagePath = current.storagePath;
    this.theme = current.theme;
    this.fontSize = current.fontSize;
    this.compactMode = current.compactMode;
    this.changedCount = this.countChanged(current);
  }

  /** Counts how many values differ from the defaults. */
  private countChanged(settings: AutoDMSettings): number {
    let count = 0;
    if (settings.storagePath !== '') count++;
    if (settings.theme !== 'system') count++;
    if (settings.fontSize !== 'medium') count++;
    if (settings.compactMode !== false) count++;
    return count;
  }

  /** Persists the current storage path. */
  saveStoragePath(): void {
    this.settingsService.update('storagePath', this.storagePath);
    this.flashSaved();
  }

  /** Persists the selected theme. */
  onThemeChanged(): void {
    this.settingsService.update('theme', this.theme);
    this.flashSaved();
  }

  /** Persists the selected font size. */
  onFontSizeChanged(): void {
    this.settingsService.update('fontSize', this.fontSize);
    this.flashSaved();
  }

  /** Toggles compact display mode. */
  onCompactModeChanged(): void {
    this.settingsService.update('compactMode', this.compactMode);
    this.flashSaved();
  }

  /** Restores every setting to its default. */
  resetDefaults(): void {
    this.settingsService.reset();
    this.syncFromService();
    this.savedMessage = 'Settings restored to defaults.';
  }

  private flashSaved(): void {
    this.savedMessage = 'Settings saved.';
  }
}
