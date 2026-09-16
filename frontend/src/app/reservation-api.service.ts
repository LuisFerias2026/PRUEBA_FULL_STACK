import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { InventoryItem, ProviderEventResponse, Reservation } from './models';

export interface DemoProviderEvent {
  eventId: string;
  reservationId: string;
  sequence: number;
  status: 'CONFIRMED' | 'REJECTED';
  occurredAt: string;
}

@Injectable({ providedIn: 'root' })
export class ReservationApiService {
  constructor(private readonly http: HttpClient) {}

  listInventory(): Observable<InventoryItem[]> {
    return this.http.get<InventoryItem[]>('/api/inventory');
  }

  createReservation(
    clientId: string,
    sku: string,
    quantity: number,
    idempotencyKey: string
  ): Observable<Reservation> {
    const headers = new HttpHeaders({ 'Idempotency-Key': idempotencyKey });
    return this.http.post<Reservation>(
      '/api/reservations',
      { clientId, sku, quantity },
      { headers }
    );
  }

  getReservation(id: string): Observable<Reservation> {
    return this.http.get<Reservation>(`/api/reservations/${id}`);
  }

  sendDemoProviderEvent(event: DemoProviderEvent): Observable<ProviderEventResponse> {
    return this.http.post<ProviderEventResponse>('/api/demo/provider-events', event);
  }
}
