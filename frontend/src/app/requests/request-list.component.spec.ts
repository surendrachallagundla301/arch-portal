import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ArchitectureRequest } from './architecture-request.model';
import { ArchitectureRequestService } from './architecture-request.service';
import { RequestListComponent } from './request-list.component';

const sample: ArchitectureRequest = {
  id: 1,
  title: 'Move billing to EKS',
  applicationName: 'billing-service',
  applicationOwner: 'Payments Team',
  description: 'Containerize billing and run it on EKS.',
  status: 'SUBMITTED',
  submittedBy: 'alice',
  createdAt: '2026-10-01T10:00:00Z',
  updatedAt: '2026-10-01T10:00:00Z',
  decisions: [],
};

describe('RequestListComponent', () => {
  let fixture: ComponentFixture<RequestListComponent>;
  let service: jasmine.SpyObj<ArchitectureRequestService>;

  beforeEach(() => {
    service = jasmine.createSpyObj<ArchitectureRequestService>('ArchitectureRequestService', ['search']);
    service.search.and.returnValue(of([sample]));
    TestBed.configureTestingModule({
      imports: [RequestListComponent],
      providers: [provideRouter([]), { provide: ArchitectureRequestService, useValue: service }],
    });
    fixture = TestBed.createComponent(RequestListComponent);
  });

  it('loads and renders requests', fakeAsync(() => {
    fixture.detectChanges();
    tick(300);
    fixture.detectChanges();
    expect(service.search).toHaveBeenCalledWith('', '');
    expect(fixture.nativeElement.textContent).toContain('Move billing to EKS');
  }));

  it('debounces typing into a single search call', fakeAsync(() => {
    fixture.detectChanges();
    tick(300);
    service.search.calls.reset();

    const component = fixture.componentInstance;
    component.query.setValue('b');
    tick(100);
    component.query.setValue('bi');
    tick(100);
    component.query.setValue('billing');
    tick(300);

    expect(service.search).toHaveBeenCalledTimes(1);
    expect(service.search).toHaveBeenCalledWith('billing', '');
  }));

  it('applies the initial status from route data', fakeAsync(() => {
    fixture.componentRef.setInput('initialStatus', 'SUBMITTED');
    fixture.detectChanges();
    tick(300);
    expect(service.search).toHaveBeenCalledWith('', 'SUBMITTED');
  }));

  it('shows an error message when the search fails', fakeAsync(() => {
    service.search.and.returnValue(throwError(() => new Error('The server could not be reached.')));
    fixture.detectChanges();
    tick(300);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'could not be reached',
    );
  }));
});
