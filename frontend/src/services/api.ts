import {
  AuthResponse,
  Booking,
  EventItem,
  EventSalesResponse,
  EventStatus,
  PlatformSalesSummaryResponse,
  QueueStatusResponse,
  Role,
  User,
  WaitingRoomStatusResponse,
} from '../types';

const TOKEN_KEY = 'hotdrop_token';
const USER_KEY = 'hotdrop_user';

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function getStoredUser(): User | null {
  const data = localStorage.getItem(USER_KEY);
  if (!data) return null;
  try {
    return JSON.parse(data) as User;
  } catch {
    return null;
  }
}

export function setStoredAuth(token: string, user: User): void {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearStoredAuth(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export class ApiError extends Error {
  status: number;
  data?: any;

  constructor(status: number, message: string, data?: any) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getStoredToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(path, {
    ...options,
    headers,
  });

  const contentType = response.headers.get('content-type');
  const isJson = contentType && contentType.includes('application/json');
  const data = isJson ? await response.json() : await response.text();

  if (!response.ok) {
    let errorMessage = `HTTP Error ${response.status}`;
    if (typeof data === 'object' && data !== null) {
      errorMessage = data.message || data.error || JSON.stringify(data);
    } else if (typeof data === 'string' && data.length > 0) {
      errorMessage = data;
    }
    throw new ApiError(response.status, errorMessage, data);
  }

  return data as T;
}

export const api = {
  // Auth
  async login(email: string, password: string): Promise<AuthResponse> {
    const res = await request<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
    setStoredAuth(res.token, res.user);
    return res;
  },

  async signup(name: string, email: string, password: string, role: Role = 'USER'): Promise<User> {
    return request<User>('/auth/signup', {
      method: 'POST',
      body: JSON.stringify({ name, email, password, role }),
    });
  },

  async logout(): Promise<void> {
    try {
      await request<{ message: string }>('/auth/logout', { method: 'POST' });
    } finally {
      clearStoredAuth();
    }
  },

  // Events (Public)
  async listEvents(status?: EventStatus): Promise<EventItem[]> {
    const query = status ? `?status=${encodeURIComponent(status)}` : '';
    return request<EventItem[]>(`/events${query}`);
  },

  async getEvent(id: number): Promise<EventItem> {
    return request<EventItem>(`/events/${id}`);
  },

  // Waiting Room
  async joinWaitingRoom(eventId: number): Promise<{ eventId: number; userId: number; status: string; joinedAt: string }> {
    return request(`/events/${eventId}/waiting-room/join`, {
      method: 'POST',
    });
  },

  async getWaitingRoomStatus(eventId: number): Promise<WaitingRoomStatusResponse> {
    return request<WaitingRoomStatusResponse>(`/events/${eventId}/waiting-room/status`);
  },

  // Queue
  async getQueueStatus(eventId: number): Promise<QueueStatusResponse> {
    return request<QueueStatusResponse>(`/events/${eventId}/queue/status`);
  },

  // Booking
  async bookTicket(eventId: number): Promise<Booking> {
    return request<Booking>(`/events/${eventId}/book`, {
      method: 'POST',
    });
  },

  async getMyBookings(): Promise<Booking[]> {
    return request<Booking[]>('/users/me/bookings');
  },

  // Admin
  async createEvent(data: {
    name: string;
    description: string;
    totalTickets: number;
    saleStartTime: string;
    waitingRoomOpenOffsetSeconds: number;
  }): Promise<EventItem> {
    return request<EventItem>('/admin/events', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  async cancelEvent(eventId: number): Promise<{ eventId: number; status: string; message: string }> {
    return request(`/admin/events/${eventId}/cancel`, {
      method: 'PATCH',
    });
  },

  async getEventSales(eventId: number): Promise<EventSalesResponse> {
    return request<EventSalesResponse>(`/admin/events/${eventId}/sales`);
  },

  async getPlatformSalesSummary(): Promise<PlatformSalesSummaryResponse> {
    return request<PlatformSalesSummaryResponse>('/admin/sales/summary');
  },
};
