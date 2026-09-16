import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Subscription, switchMap, takeWhile, timer } from 'rxjs';
import { InventoryItem, Reservation } from './models';
import { ReservationApiService } from './reservation-api.service';

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit, OnDestroy {
  inventory: InventoryItem[] = [];
  clientId = 'client-demo';
  sku = 'SKU-FLASH-A';
  quantity = 1;
  idempotencyKey = crypto.randomUUID();
  reservation: Reservation | null = null;
  errorMessage = '';
  infoMessage = '';
  eventStatus: 'CONFIRMED' | 'REJECTED' = 'CONFIRMED';
  eventSequence = 1;
  lastProviderOutcome = '';
  polling = false;

  private pollSub?: Subscription;

  constructor(private readonly api: ReservationApiService) {}

  ngOnInit(): void {
    this.refreshInventory();
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  refreshInventory(): void {
    this.api.listInventory().subscribe({
      next: (items) => (this.inventory = items),
      error: (err) => (this.errorMessage = this.describeError(err))
    });
  }

  newIdempotencyKey(): void {
    this.idempotencyKey = crypto.randomUUID();
  }

  createReservation(): void {
    this.errorMessage = '';
    this.infoMessage = '';
    this.api.createReservation(this.clientId, this.sku, this.quantity, this.idempotencyKey).subscribe({
      next: (reservation) => {
        this.reservation = reservation;
        this.infoMessage = 'Reserva creada. El estado se actualiza solo cada 2 segundos.';
        this.refreshInventory();
        this.startPolling(reservation.id);
      },
      error: (err) => (this.errorMessage = this.describeError(err))
    });
  }

  lookupReservation(): void {
    if (!this.reservation?.id) {
      this.errorMessage = 'Crea o indica una reserva primero.';
      return;
    }
    this.startPolling(this.reservation.id);
  }

  confirmWithProvider(): void {
    if (!this.reservation?.id) {
      this.errorMessage = 'No hay reserva para notificar.';
      return;
    }
    this.api.sendDemoProviderEvent({
      eventId: crypto.randomUUID(),
      reservationId: this.reservation.id,
      sequence: this.eventSequence,
      status: this.eventStatus,
      occurredAt: new Date().toISOString()
    }).subscribe({
      next: (response) => {
        this.lastProviderOutcome = response.outcome;
        this.infoMessage = `Evento del proveedor: ${response.outcome}`;
        this.startPolling(this.reservation!.id);
      },
      error: (err) => (this.errorMessage = this.describeError(err))
    });
  }

  private startPolling(id: string): void {
    this.pollSub?.unsubscribe();
    this.polling = true;
    this.pollSub = timer(0, 2000)
      .pipe(
        switchMap(() => this.api.getReservation(id)),
        takeWhile((reservation) => reservation.status === 'PENDING', true)
      )
      .subscribe({
        next: (reservation) => {
          this.reservation = reservation;
          if (reservation.status !== 'PENDING') {
            this.polling = false;
            this.refreshInventory();
          }
        },
        error: (err) => {
          this.polling = false;
          this.errorMessage = this.describeError(err);
        },
        complete: () => (this.polling = false)
      });
  }

  private describeError(err: unknown): string {
    if (err instanceof HttpErrorResponse && err.error?.message) {
      return `${err.error.code ?? 'ERROR'}: ${err.error.message}`;
    }
    return 'No se pudo contactar la API. ¿Está el backend en marcha?';
  }
}
