/**
 * Local, client-side configuration for AutoDM.
 *
 * <p>These settings describe how the application behaves for a given user on a given device. They
 * are entirely local: they are persisted to {@code localStorage} and are never sent across the
 * network, so the settings screen works without any internet connectivity.</p>
 */
export interface AutoDMSettings {
  /**
   * Where the local data is stored.
   *
   * <p>By default the Spring Boot back-end writes its SQLite database into the user's home
   * directory. This value lets the operator point AutoDM at a different local storage location.
   * It is a plain path string and is never validated against the file system from the browser;
   * it is only stored and echoed back so it survives restarts.</p>
   */
  storagePath: string;

  /**
   * The colour theme applied to the user interface.
   *
   * <ul>
   *   <li>{@code system} follows the operating system's light/dark setting;</li>
   *   <li>{@code light} forces the light theme;</li>
   *   <li>{@code dark} forces the dark theme.</li>
   * </ul>
   */
  theme: 'system' | 'light' | 'dark';

  /**
   * The base text size scale for the interface.
   *
   * <p>Rendered as a CSS custom property on the root element so the whole layout scales
   * consistently.</p>
   */
  fontSize: 'small' | 'medium' | 'large';

  /**
   * When {@code true}, the interface uses tighter spacing and more information per screen.
   */
  compactMode: boolean;
}

/**
 * The default settings used before the operator has customised anything.
 */
export const DEFAULT_SETTINGS: AutoDMSettings = {
  storagePath: '',
  theme: 'system',
  fontSize: 'medium',
  compactMode: false,
};

/**
 * The {@code localStorage} key under which {@link AutoDMSettings} are serialised.
 */
export const SETTINGS_STORAGE_KEY = 'autodm.settings';
