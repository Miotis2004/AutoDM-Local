import { Routes } from '@angular/router';

/**
 * The application routes.
 *
 * <p>Each major application area is a lazily loaded feature route so the initial bundle stays
 * small and every area has a clean, distinct URL. The empty path redirects to the dashboard,
 * which is the entry point of the shell.</p>
 */
export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./pages/dashboard/dashboard.component').then(
        (m) => m.DashboardComponent,
      ),
  },
  {
    path: 'campaigns',
    loadComponent: () =>
      import('./pages/campaigns/campaigns.component').then(
        (m) => m.CampaignsComponent,
      ),
  },
  {
    path: 'play',
    loadComponent: () =>
      import('./pages/play/play.component').then((m) => m.PlayComponent),
  },
  {
    path: 'sessions',
    loadComponent: () =>
      import('./pages/sessions/sessions.component').then(
        (m) => m.SessionsComponent,
      ),
  },
  {
    path: 'characters',
    loadComponent: () =>
      import('./pages/characters/characters.component').then(
        (m) => m.CharactersComponent,
      ),
  },
  {
    path: 'quests',
    loadComponent: () =>
      import('./pages/quests/quests.component').then((m) => m.QuestsComponent),
  },
  {
    path: 'world',
    loadComponent: () =>
      import('./pages/world/world.component').then((m) => m.WorldComponent),
  },
  {
    path: 'factions',
    loadComponent: () =>
      import('./pages/factions/factions.component').then(
        (m) => m.FactionsComponent,
      ),
  },
  {
    path: 'locations',
    loadComponent: () =>
      import('./pages/locations/locations.component').then(
        (m) => m.LocationsComponent,
      ),
  },
  {
    path: 'items',
    loadComponent: () =>
      import('./pages/items/items.component').then((m) => m.ItemsComponent),
  },
  {
    path: 'creature-templates',
    loadComponent: () =>
      import('./pages/creature-templates/creature-templates.component').then(
        (m) => m.CreatureTemplatesComponent,
      ),
  },
  {
    path: 'npcs',
    loadComponent: () =>
      import('./pages/npcs/npcs.component').then((m) => m.NpcsComponent),
  },
  {
    path: 'encounters',
    loadComponent: () =>
      import('./pages/encounters/encounters.component').then(
        (m) => m.EncountersComponent,
      ),
  },
  {
    path: 'history',
    loadComponent: () =>
      import('./pages/history/history.component').then(
        (m) => m.HistoryComponent,
      ),
  },
  {
    path: 'settings',
    loadComponent: () =>
      import('./pages/settings/settings.component').then(
        (m) => m.SettingsComponent,
      ),
  },
  { path: '**', redirectTo: 'dashboard' },
];
