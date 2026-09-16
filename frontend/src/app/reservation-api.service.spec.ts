import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReservationApiService } from './reservation-api.service';
import { Reservation } from './models';

describe('ReservationApiService', () => {
  let service: ReservationApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), ReservationApiService]
    });
    service = TestBed.inject(ReservationApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('polls a reservation by id without reloading the page', () => {
    const expected: Reservation = {
      id: 'res_1',
      clientId: 'c1',
      sku: 'SKU-FLASH-A',
      quantity: 2,
      status: 'CONFIRMED',
      lastSequence: 1,
      createdAt: '2026-09-16T12:00:00Z',
      updatedAt: '2026-09-16T12:00:01Z'
    };

    service.getReservation('res_1').subscribe((reservation) => {
      expect(reservation.status).toBe('CONFIRMED');
    });

    const req = http.expectOne('/api/reservations/res_1');
    expect(req.request.method).toBe('GET');
    req.flush(expected);
  });

  it('sends Idempotency-Key when creating a reservation', () => {
    service.createReservation('c1', 'SKU-FLASH-A', 2, 'key-1').subscribe();
    const req = http.expectOne('/api/reservations');
    expect(req.request.headers.get('Idempotency-Key')).toBe('key-1');
    expect(req.request.body).toEqual({ clientId: 'c1', sku: 'SKU-FLASH-A', quantity: 2 });
    req.flush({
      id: 'res_1',
      clientId: 'c1',
      sku: 'SKU-FLASH-A',
      quantity: 2,
      status: 'PENDING',
      lastSequence: 0,
      createdAt: '2026-09-16T12:00:00Z',
      updatedAt: '2026-09-16T12:00:00Z'
    });
  });

  it('sends demo provider events without an HMAC header', () => {
    service.sendDemoProviderEvent({
      eventId: 'evt-1',
      reservationId: 'res_1',
      sequence: 1,
      status: 'CONFIRMED',
      occurredAt: '2026-09-16T12:00:00Z'
    }).subscribe();
    const req = http.expectOne('/api/demo/provider-events');
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.has('X-Provider-Signature')).toBeFalse();
    req.flush({ eventId: 'evt-1', outcome: 'APPLIED', reservationId: 'res_1', status: 'CONFIRMED' });
  });
});
