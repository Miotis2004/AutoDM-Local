import { Injectable, signal } from '@angular/core';

import {
  AutoDMSettings,
  DEFAULT_SETTINGS,
  SETTINGS_STORAGE_KEY,
} from '../models/settings';

/**
 * Stores and applies the local AutoDM settings.
 *
 * <p>Everything here is local-only. Settings are read from and written to {@code localStorage}
 * and are never transmitted to the back-end, which keeps the settings screen fully usable while
 * offline. The service also applies the {@link AutoDMSettings.theme theme} and
 * {@link AutoDMSettings.fontSize font size} to the document so their effect is visible
 * immediately.</p>
 */
@Injectable({ providedIn: 'root' })
export class SettingsService {
  private readonly settingsSignal = signal<AutoDMSettings>(this.load());

  /** A read-only snapshot of the current settings, updated whenever they change. */
  readonly settings = this.settingsSignal.asReadonly();

  /**
   * Returns the current settings.
   *
   * @return the current {@link AutoDMSettings}
   */
  get(): AutoDMSettings {
    return this.settingsSignal();
  }

  /**
   * Persists a full set of settings and reapplies the visual ones.
   *
   * @param settings the settings to store
   */
  save(settings: AutoDMSettings): void {
    this.settingsSignal.set(settings);
    try {
      localStorage.setItem(SETTINGS_STORAGE_KEY, JSON.stringify(settings));
    } catch {
      // Storage may be unavailable (e.g. private mode). Persisting is best-effort;
      // the in-memory settings remain authoritative for the current session.
    }
    this.applyTheme(settings.theme);
    this.applyFontSize(settings.fontSize);
  }

  /**
   * Applies a single setting by key. Handy for toggles and selects that change one value at a
   * time.
   *
   * @param key the setting to update
   * @param value the new value for that setting
   */
  update<K extends keyof AutoDMSettings>(key: K, value: AutoDMSettings[K]): void {
    this.save({ ...this.get(), [key]: value });
  }

  /**
   * Resets every setting back to {@link DEFAULT_SETTINGS}.
   */
  reset(): void {
    this.save({ ...DEFAULT_SETTINGS });
  }

  private load(): AutoDMSettings {
    try {
      const raw = localStorage.getItem(SETTINGS_STORAGE_KEY);
      if (!raw) {
        return { ...DEFAULT_SETTINGS };
      }
      const parsed = JSON.parse(raw) as Partial<AutoDMSettings>;
      return { ...DEFAULT_SETTINGS, ...parsed };
    } catch {
      return { ...DEFAULT_SETTINGS };
    }
  }

  private applyTheme(theme: AutoDMSettings['theme']): void {
    const root = document.documentElement;
    if (theme === 'system') {
      root.removeAttribute('data-theme');
    } else {
      root.setAttribute('data-theme', theme);
    }
  }

  private applyFontSize(fontSize: AutoDMSettings['fontSize']): void {
    const root = document.documentElement;
    const scale: Record<AutoDMSettings['fontSize'], number> = {
      small: 0.9,
      medium: 1,
      large: 1.125,
    };
    root.style.setProperty('--font-scale', String(scale[fontSize]));
  }
}
