import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <nav class="topbar">
      <a routerLink="/requests" class="brand">Architecture Portal</a>
      <a routerLink="/requests" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Requests</a>
      @if (auth.isReviewer()) {
        <a routerLink="/review-queue" routerLinkActive="active">Review queue</a>
      }
      <span class="spacer"></span>
      @if (auth.username(); as name) {
        <span class="user">{{ name }}</span>
        <button type="button" class="btn" (click)="auth.logout()">Sign out</button>
      }
    </nav>
    <main><router-outlet /></main>
  `,
})
export class App {
  readonly auth = inject(AuthService);
}
