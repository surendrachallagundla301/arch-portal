import { Component, computed, input } from '@angular/core';
import { RequestStatus } from './architecture-request.model';

@Component({
  selector: 'app-status-badge',
  template: `<span [class]="'badge ' + cssClass()">{{ label() }}</span>`,
})
export class StatusBadgeComponent {
  readonly status = input.required<RequestStatus>();
  readonly label = computed(() => this.status().replace('_', ' '));
  readonly cssClass = computed(() => this.status().toLowerCase().replace('_', '-'));
}
