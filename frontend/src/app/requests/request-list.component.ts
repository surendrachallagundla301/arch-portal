import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  catchError,
  combineLatest,
  debounceTime,
  distinctUntilChanged,
  map,
  of,
  startWith,
  switchMap,
} from 'rxjs';
import { REQUEST_STATUSES, RequestStatus, ArchitectureRequest } from './architecture-request.model';
import { ArchitectureRequestService } from './architecture-request.service';
import { StatusBadgeComponent } from './status-badge.component';

interface ListState {
  loading: boolean;
  items: ArchitectureRequest[];
  error: string | null;
}

@Component({
  selector: 'app-request-list',
  imports: [ReactiveFormsModule, RouterLink, DatePipe, StatusBadgeComponent],
  template: `
    <header class="page-header">
      <h1>{{ heading() }}</h1>
      <a routerLink="/requests/new" class="btn primary">New request</a>
    </header>

    <div class="filters">
      <input
        type="search"
        [formControl]="query"
        placeholder="Search by title or application"
        aria-label="Search requests"
      />
      <select [formControl]="status" aria-label="Filter by status">
        <option value="">All statuses</option>
        @for (s of statuses; track s) {
          <option [value]="s">{{ s.replace('_', ' ') }}</option>
        }
      </select>
    </div>

    @let s = state();
    @if (s.error) {
      <p class="error" role="alert">{{ s.error }}</p>
    } @else if (s.loading) {
      <p class="muted">Loading…</p>
    } @else if (s.items.length === 0) {
      <p class="muted">No requests match.</p>
    } @else {
      <table>
        <thead>
          <tr><th>Title</th><th>Application</th><th>Status</th><th>Submitted</th></tr>
        </thead>
        <tbody>
          @for (r of s.items; track r.id) {
            <tr>
              <td><a [routerLink]="['/requests', r.id]">{{ r.title }}</a></td>
              <td>{{ r.applicationName }}</td>
              <td><app-status-badge [status]="r.status" /></td>
              <td>{{ r.createdAt | date: 'mediumDate' }} · {{ r.submittedBy }}</td>
            </tr>
          }
        </tbody>
      </table>
    }
  `,
})
export class RequestListComponent implements OnInit {
  private readonly service = inject(ArchitectureRequestService);
  private readonly destroyRef = inject(DestroyRef);

  /** Set from route data, e.g. the review queue pre-filters to SUBMITTED. */
  readonly initialStatus = input<RequestStatus | ''>('');
  readonly heading = input('Architecture requests');

  readonly statuses = REQUEST_STATUSES;
  readonly query = new FormControl('', { nonNullable: true });
  readonly status = new FormControl<RequestStatus | ''>('', { nonNullable: true });
  readonly state = signal<ListState>({ loading: true, items: [], error: null });

  ngOnInit(): void {
    this.status.setValue(this.initialStatus(), { emitEvent: false });

    const query$ = this.query.valueChanges.pipe(
      startWith(this.query.value),
      debounceTime(300),
      map((q) => q.trim()),
      distinctUntilChanged(),
    );
    const status$ = this.status.valueChanges.pipe(startWith(this.status.value));

    // switchMap cancels the in-flight search whenever the filters change.
    combineLatest([query$, status$])
      .pipe(
        switchMap(([q, status]) =>
          this.service.search(q, status).pipe(
            map((items): ListState => ({ loading: false, items, error: null })),
            catchError((err: Error) => of<ListState>({ loading: false, items: [], error: err.message })),
            startWith<ListState>({ loading: true, items: [], error: null }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((state) => this.state.set(state));
  }
}
