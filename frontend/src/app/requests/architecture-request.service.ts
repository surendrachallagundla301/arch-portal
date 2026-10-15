import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { RUNTIME_CONFIG } from '../core/runtime-config';
import {
  ArchitectureRequest,
  NewArchitectureRequest,
  NewDecision,
  RequestStatus,
} from './architecture-request.model';

@Injectable({ providedIn: 'root' })
export class ArchitectureRequestService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(RUNTIME_CONFIG).apiUrl}/requests`;

  search(query: string, status: RequestStatus | ''): Observable<ArchitectureRequest[]> {
    let params = new HttpParams();
    if (query.trim()) params = params.set('q', query.trim());
    if (status) params = params.set('status', status);
    return this.http.get<ArchitectureRequest[]>(this.baseUrl, { params });
  }

  get(id: number): Observable<ArchitectureRequest> {
    return this.http.get<ArchitectureRequest>(`${this.baseUrl}/${id}`);
  }

  create(request: NewArchitectureRequest): Observable<ArchitectureRequest> {
    return this.http.post<ArchitectureRequest>(this.baseUrl, request);
  }

  decide(id: number, decision: NewDecision): Observable<ArchitectureRequest> {
    return this.http.post<ArchitectureRequest>(`${this.baseUrl}/${id}/decisions`, decision);
  }
}
