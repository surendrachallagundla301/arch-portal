import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ArchitectureRequestService } from './architecture-request.service';

@Component({
  selector: 'app-request-form',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <header class="page-header"><h1>New architecture request</h1></header>

    <form [formGroup]="form" (ngSubmit)="submit()" class="card">
      <label>
        Title
        <input formControlName="title" maxlength="150" />
        @if (form.controls.title.touched && form.controls.title.invalid) {
          <small class="error">Title is required (5–150 characters).</small>
        }
      </label>

      <label>
        Application name (as registered in the CMDB)
        <input formControlName="applicationName" maxlength="100" />
        @if (form.controls.applicationName.touched && form.controls.applicationName.invalid) {
          <small class="error">Application name is required.</small>
        }
      </label>

      <label>
        Description
        <textarea formControlName="description" rows="6" maxlength="4000"></textarea>
        @if (form.controls.description.touched && form.controls.description.invalid) {
          <small class="error">Describe the change in at least 20 characters.</small>
        }
      </label>

      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }

      <div class="actions">
        <a routerLink="/requests" class="btn">Cancel</a>
        <button type="submit" class="btn primary" [disabled]="saving()">
          {{ saving() ? 'Submitting…' : 'Submit for review' }}
        </button>
      </div>
    </form>
  `,
})
export class RequestFormComponent {
  private readonly service = inject(ArchitectureRequestService);
  private readonly router = inject(Router);

  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = inject(FormBuilder).nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(150)]],
    applicationName: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(4000)]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.service
      .create(this.form.getRawValue())
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (created) => this.router.navigate(['/requests', created.id]),
        error: (err: Error) => this.error.set(err.message),
      });
  }
}
