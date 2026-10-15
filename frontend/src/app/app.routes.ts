import { Routes } from '@angular/router';
import { roleGuard } from './core/role.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'requests' },
  {
    path: 'requests',
    loadComponent: () => import('./requests/request-list.component').then((m) => m.RequestListComponent),
  },
  {
    path: 'requests/new',
    loadComponent: () => import('./requests/request-form.component').then((m) => m.RequestFormComponent),
  },
  {
    path: 'requests/:id',
    loadComponent: () => import('./requests/request-detail.component').then((m) => m.RequestDetailComponent),
  },
  {
    path: 'review-queue',
    canActivate: [roleGuard('ARB_REVIEWER')],
    data: { initialStatus: 'SUBMITTED', heading: 'ARB review queue' },
    loadComponent: () => import('./requests/request-list.component').then((m) => m.RequestListComponent),
  },
  { path: '**', redirectTo: 'requests' },
];
