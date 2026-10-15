import { DatePipe } from '@angular/common';
import { Component, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, finalize, of, switchMap } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { ArchitectureRequest, DECISION_OUTCOMES, DecisionOutcome } from './architecture-request.model';
import { ArchitectureRequestService } from './architecture-request.service';
import { StatusBadgeComponent } from './status-badge.component';

@Component({
  selector: 'app-request-detail',
  imports: [ReactiveFormsModule, RouterLink, DatePipe, StatusBadgeComponent],
  template: `
    <a routerLink="/requests" class="back">← All requests</a>

    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    }

    @if (request(); as r) {
      <header class="page-header">
        <h1>{{ r.title }}</h1>
        <app-status-badge [status]="r.status" />
      </header>

      <dl class="meta">
        <dt>Application</dt><dd>{{ r.applicationName }}</dd>
        <dt>Owner (CMDB)</dt><dd>{{ r.applicationOwner ?? 'Not found in CMDB' }}</dd>
        <dt>Submitted</dt><dd>{{ r.createdAt | date: 'medium' }} by {{ r.submittedBy }}</dd>
      </dl>
      <p class="description">{{ r.description }}</p>

      <h2>Review board decisions</h2>
      @for (d of r.decisions; track d.id) {
        <article class="card decision">
          <header><app-status-badge [status]="d.outcome" /> {{ d.decidedBy }} · {{ d.decidedAt | date: 'medium' }}</header>
          <p>{{ d.notes }}</p>
          @if (d.actionItems.length) {
            <ul>
              @for (item of d.actionItems; track $index) { <li>{{ item }}</li> }
            </ul>
          }
        </article>
      } @empty {
        <p class="muted">No decisions recorded yet.</p>
      }

      @if (auth.isReviewer()) {
        <form [formGroup]="decisionForm" (ngSubmit)="recordDecision(r.id)" class="card">
          <h3>Record ARB decision</h3>
          <label>
            Outcome
            <select formControlName="outcome">
              @for (o of outcomes; track o) { <option [value]="o">{{ o.replace('_', ' ') }}</option> }
            </select>
          </label>
          <label>
            Notes
            <textarea formControlName="notes" rows="3"></textarea>
          </label>
          <label>
            Action items (one per line)
            <textarea formControlName="actionItems" rows="3"></textarea>
          </label>
          <div class="actions">
            <button type="submit" class="btn primary" [disabled]="saving() || decisionForm.invalid">
              {{ saving() ? 'Saving…' : 'Save decision' }}
            </button>
          </div>
        </form>
      }
    } @else if (!error()) {
      <p class="muted">Loading…</p>
    }
  `,
})
export class RequestDetailComponent {
  private readonly service = inject(ArchitectureRequestService);
  readonly auth = inject(AuthService);

  /** Bound from the :id route parameter (withComponentInputBinding). */
  readonly id = input.required<string>();

  readonly request = signal<ArchitectureRequest | null>(null);
  readonly error = signal<string | null>(null);
  readonly saving = signal(false);
  readonly outcomes = DECISION_OUTCOMES;

  readonly decisionForm = inject(FormBuilder).nonNullable.group({
    outcome: ['APPROVED' as DecisionOutcome, Validators.required],
    notes: ['', [Validators.required, Validators.maxLength(2000)]],
    actionItems: [''],
  });

  constructor() {
    toObservable(this.id)
      .pipe(
        switchMap((id) =>
          this.service.get(Number(id)).pipe(
            catchError((err: Error) => {
              this.error.set(err.message);
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((r) => this.request.set(r));
  }

  recordDecision(id: number): void {
    const { outcome, notes, actionItems } = this.decisionForm.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    this.service
      .decide(id, {
        outcome,
        notes,
        actionItems: actionItems.split('\n').map((s) => s.trim()).filter(Boolean),
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (updated) => {
          this.request.set(updated);
          this.decisionForm.reset();
        },
        error: (err: Error) => this.error.set(err.message),
      });
  }
}
