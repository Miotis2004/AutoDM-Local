import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

/**
 * Application shell navigation bar.
 *
 * <p>Renders the primary navigation linking to every major application area. The active link
 * highlights against the current route via {@link RouterLinkActive}, and the routed page content
 * renders below the bar through {@link RouterOutlet}.</p>
 */
@Component({
  selector: 'app-navigation',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './navigation.component.html',
  styleUrl: './navigation.component.css',
})
export class NavigationComponent {}
