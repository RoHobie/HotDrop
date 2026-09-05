export type Role = 'USER' | 'ADMIN';

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export type EventStatus = 'UPCOMING' | 'LIVE' | 'ENDED' | 'CANCELLED';

export interface EventItem {
  id: number;
  name: string;
  description: string;
  totalTickets: number;
  ticketsSold: number;
  remainingTickets: number;
  saleStartTime: string;
  waitingRoomOpenOffsetSeconds: number;
  status: EventStatus;
  createdAt: string;
}

export type QueueStatus = 'WAITING' | 'QUEUED' | 'ADMITTED' | 'EXPIRED' | 'COMPLETED';

export interface WaitingRoomStatusResponse {
  eventId: number;
  userId: number;
  status: QueueStatus;
  joinedAt: string;
  saleStartTime: string;
  secondsUntilSale: number;
}

export interface QueueStatusResponse {
  eventId: number;
  userId: number;
  status: QueueStatus;
  queuePosition: number | null;
  totalInQueue: number;
  admittedAt: string | null;
  admissionExpiresAt: string | null;
  secondsRemaining: number | null;
}

export interface Booking {
  bookingId: number;
  eventId: number;
  eventName: string;
  userId: number;
  status: 'CONFIRMED' | 'CANCELLED';
  bookedAt: string;
}

export interface EventSalesResponse {
  eventId: number;
  eventName: string;
  totalTickets: number;
  ticketsSold: number;
  remainingTickets: number;
  status: EventStatus;
  totalInQueue: number;
  completedBookings: number;
  percentSold: number;
}

export interface PlatformSalesSummaryResponse {
  totalEvents: number;
  totalTicketsAvailable: number;
  totalTicketsSold: number;
  totalBookingsConfirmed: number;
  activeEventsCount: number;
}
