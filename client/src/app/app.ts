import { Component } from '@angular/core';
import { NavigationComponent } from './components/navigation/navigation.component';
import { NotificationsComponent } from './components/notifications/notifications.component';

@Component({
  imports: [NavigationComponent, NotificationsComponent],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {}
