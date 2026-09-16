export interface Reservation {
  id: string;
  clientId: string;
  sku: string;
  quantity: number;
  status: 'PENDING' | 'CONFIRMED' | 'REJECTED';
  lastSequence: number;
  createdAt: string;
  updatedAt: string;
}

export interface InventoryItem {
  sku: string;
  name: string;
  available: number;
}

export interface ApiError {
  code: string;
  message: string;
}

export interface ProviderEventResponse {
  eventId: string;
  outcome: string;
  reservationId: string | null;
  status: string | null;
}
