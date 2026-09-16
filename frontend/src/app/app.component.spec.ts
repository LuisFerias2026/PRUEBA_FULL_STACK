import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { Reservation } from './models';

describe('AppComponent', () => {
  let http: HttpTestingController;

  const pending: Reservation = {
    id: 'res_1',
    clientId: 'client-demo',
    sku: 'SKU-FLASH-A',
    quantity: 1,
    status: 'PENDING',
    lastSequence: 0,
    createdAt: '2026-09-16T12:00:00Z',
    updatedAt: '2026-09-16T12:00:00Z'
  };

  const confirmed: Reservation = {
    ...pending,
    status: 'CONFIRMED',
    lastSequence: 1,
    updatedAt: '2026-09-16T12:00:02Z'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should render FlashReserve heading', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    http.expectOne('/api/inventory').flush([]);
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('FlashReserve');
  });

  it('updates reservation status by polling every 2s without reload', fakeAsync(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    http.expectOne('/api/inventory').flush([
      { sku: 'SKU-FLASH-A', name: 'Flash campaign A', available: 10 }
    ]);

    component.createReservation();
    const created = http.expectOne('/api/reservations');
    expect(created.request.headers.get('Idempotency-Key')).toBeTruthy();
    created.flush(pending);
    http.expectOne('/api/inventory').flush([
      { sku: 'SKU-FLASH-A', name: 'Flash campaign A', available: 9 }
    ]);

    tick(0);
    http.expectOne('/api/reservations/res_1').flush(pending);
    expect(component.polling).toBeTrue();
    expect(component.reservation?.status).toBe('PENDING');

    tick(2000);
    http.expectOne('/api/reservations/res_1').flush(confirmed);
    expect(component.reservation?.status).toBe('CONFIRMED');
    expect(component.polling).toBeFalse();
    http.expectOne('/api/inventory').flush([
      { sku: 'SKU-FLASH-A', name: 'Flash campaign A', available: 9 }
    ]);
    discardPeriodicTasks();
  }));

  it('shows a comprehensible error when stock is insufficient', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    http.expectOne('/api/inventory').flush([
      { sku: 'SKU-FLASH-A', name: 'Flash campaign A', available: 10 }
    ]);

    component.createReservation();
    http.expectOne('/api/reservations').flush(
      { code: 'INSUFFICIENT_STOCK', message: 'Not enough inventory available for SKU SKU-FLASH-A.' },
      { status: 409, statusText: 'Conflict' }
    );
    expect(component.errorMessage).toContain('INSUFFICIENT_STOCK');
  });
});
