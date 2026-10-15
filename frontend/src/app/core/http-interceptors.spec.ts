import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { authInterceptor, errorInterceptor } from './http-interceptors';
import { RUNTIME_CONFIG } from './runtime-config';

describe('HTTP interceptors', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let auth: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['getToken', 'login']);
    auth.getToken.and.resolveTo('test-token');

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: auth },
        {
          provide: RUNTIME_CONFIG,
          useValue: { apiUrl: '/api', keycloak: { url: '', realm: '', clientId: '' } },
        },
        provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('adds the bearer token to API calls', fakeAsync(() => {
    http.get('/api/requests').subscribe();
    tick();
    const req = controller.expectOne('/api/requests');
    expect(req.request.headers.get('Authorization')).toBe('Bearer test-token');
    req.flush([]);
  }));

  it('does not send the token to third-party URLs', fakeAsync(() => {
    http.get('https://example.com/data').subscribe();
    tick();
    const req = controller.expectOne('https://example.com/data');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  }));

  it('retries a GET on 503 and succeeds', fakeAsync(() => {
    let result: unknown;
    http.get('/api/requests').subscribe((r) => (result = r));
    tick();
    controller.expectOne('/api/requests').flush(null, { status: 503, statusText: 'Unavailable' });
    tick(300);
    controller.expectOne('/api/requests').flush(['ok']);
    expect(result).toEqual(['ok']);
  }));

  it('does not retry a POST and surfaces the server detail', fakeAsync(() => {
    let message = '';
    http.post('/api/requests', {}).subscribe({ error: (e: Error) => (message = e.message) });
    tick();
    controller
      .expectOne('/api/requests')
      .flush({ detail: 'Title is required' }, { status: 400, statusText: 'Bad Request' });
    tick(1000);
    controller.expectNone('/api/requests');
    expect(message).toBe('Title is required');
  }));

  it('sends the user to login on 401', fakeAsync(() => {
    http.get('/api/requests').subscribe({ error: () => undefined });
    tick();
    controller.expectOne('/api/requests').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(auth.login).toHaveBeenCalled();
  }));
});
