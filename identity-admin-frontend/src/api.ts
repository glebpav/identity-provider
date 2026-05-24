export const ROLES = ['TRADER', 'POSITIONER', 'ADMIN', 'AUDITOR'] as const;

export type UserRole = (typeof ROLES)[number];

export type User = {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
};

export type TokenResponse = {
  tokenType: string;
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
};

export type RegisterPayload = {
  email: string;
  firstName: string;
  lastName: string;
  password: string;
};

export type LoginPayload = {
  email: string;
  password: string;
};

export type Page<T> = {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type ApiErrorBody = {
  message?: string;
  error?: string;
  code?: string;
  path?: string;
  status?: number;
  fields?: Array<{ field: string; message: string }>;
};

const API_BASE_URL = import.meta.env.VITE_IDENTITY_API_BASE_PATH || '/identity-api';

export class ApiError extends Error {
  readonly status: number;
  readonly body?: ApiErrorBody;

  constructor(status: number, message: string, body?: ApiErrorBody) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

function authHeader(accessToken?: string): HeadersInit {
  return accessToken ? { Authorization: `Bearer ${accessToken}` } : {};
}

async function parseError(response: Response): Promise<ApiError> {
  const fallback = `${response.status} ${response.statusText}`;

  try {
    const body = (await response.json()) as ApiErrorBody;
    const fieldError = body.fields?.map((item) => `${item.field}: ${item.message}`).join(', ');
    return new ApiError(response.status, fieldError || body.message || body.error || fallback, body);
  } catch {
    return new ApiError(response.status, fallback);
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      Accept: 'application/json',
      ...(init.body ? { 'Content-Type': 'application/json' } : {}),
      ...init.headers
    }
  });

  if (!response.ok) {
    throw await parseError(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export const identityApi = {
  login(payload: LoginPayload) {
    return request<TokenResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  },

  register(payload: RegisterPayload) {
    return request<User>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  },

  refresh(refreshToken: string) {
    return request<TokenResponse>('/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken })
    });
  },

  me(accessToken: string) {
    return request<User>('/users/me', {
      headers: authHeader(accessToken)
    });
  },

  users(accessToken: string, page: number, size: number) {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
      sort: 'email,asc'
    });

    return request<Page<User>>(`/users?${params.toString()}`, {
      headers: authHeader(accessToken)
    });
  },

  updateRole(accessToken: string, userId: string, role: UserRole) {
    return request<User>(`/users/${userId}/role`, {
      method: 'PATCH',
      headers: authHeader(accessToken),
      body: JSON.stringify({ role })
    });
  }
};
