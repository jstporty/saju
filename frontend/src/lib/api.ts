/**
 * API client for backend (api.saju.app or http://localhost:8080).
 * 세션 쿠키 사용 — credentials: 'include' 필수.
 */
const BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(public status: number, public code: string, message: string) {
    super(message);
  }
}

type Options = {
  method?: "GET" | "POST" | "DELETE";
  body?: unknown;
  headers?: Record<string, string>;
};

export async function api<T>(path: string, opts: Options = {}): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method: opts.method ?? "GET",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
      ...(opts.headers ?? {}),
    },
    body: opts.body ? JSON.stringify(opts.body) : undefined,
  });

  if (res.status === 204) return undefined as T;

  const text = await res.text();
  const json = text ? JSON.parse(text) : null;

  if (!res.ok) {
    const code = json?.code ?? "UNKNOWN";
    const msg = json?.message ?? json?.detail ?? res.statusText;
    throw new ApiError(res.status, code, msg);
  }

  return (json?.data ?? json) as T;
}

export const API_BASE = BASE;
