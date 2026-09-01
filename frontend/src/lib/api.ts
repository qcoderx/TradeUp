import type { ApiErrorBody } from "./types";

const TOKEN_KEY = "tradeup-token";

/**
 * A failed request, carrying the structured body the Java side sent.
 *
 * `fieldErrors` is what lets a form highlight the exact input that was
 * rejected instead of showing one generic message at the top.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, body: Partial<ApiErrorBody>) {
    super(body.message ?? "Something went wrong. Please try again.");
    this.name = "ApiError";
    this.status = status;
    this.code = body.code ?? "unknown";
    this.fieldErrors = body.fieldErrors ?? {};
  }
}

/* -------------------------------------------------------------------------
   Token storage
   ------------------------------------------------------------------------- */

export function getToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    // Private browsing can refuse storage entirely; a signed-out session is
    // a perfectly valid state to fall back to.
    return null;
  }
}

export function setToken(token: string | null): void {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch {
    /* Nothing useful to do; the session simply will not survive a reload. */
  }
}

/* -------------------------------------------------------------------------
   Request
   ------------------------------------------------------------------------- */

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  signal?: AbortSignal;
}

/**
 * Calls the API and unwraps the response.
 *
 * Every non-2xx becomes an {@link ApiError}, so callers only ever handle one
 * failure type. A 401 clears the stored token, which is what makes an expired
 * session drop cleanly back to signed-out rather than looping on retries.
 */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, signal } = options;

  const headers: Record<string, string> = {};
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers["Content-Type"] = "application/json";

  let response: Response;
  try {
    response = await fetch(`/api${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (cause) {
    if ((cause as Error).name === "AbortError") throw cause;
    throw new ApiError(0, {
      code: "network",
      message: "Could not reach the server. Check that the backend is running.",
    });
  }

  if (response.status === 204) return undefined as T;

  const text = await response.text();
  const payload = text ? safeParse(text) : {};

  if (!response.ok) {
    if (response.status === 401) setToken(null);
    throw new ApiError(response.status, payload as Partial<ApiErrorBody>);
  }

  return payload as T;
}

/** Multipart upload, which needs the browser to set its own content type. */
export async function uploadFiles(files: File[]): Promise<string[]> {
  const form = new FormData();
  files.forEach((file) => form.append("files", file));

  const token = getToken();
  const response = await fetch("/api/uploads", {
    method: "POST",
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: form,
  });

  const text = await response.text();
  const payload = text ? safeParse(text) : {};

  if (!response.ok) throw new ApiError(response.status, payload as Partial<ApiErrorBody>);
  return (payload as { urls: string[] }).urls;
}

function safeParse(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return { code: "bad_response", message: "The server sent something unreadable." };
  }
}

/** Builds a query string, dropping anything the user did not set. */
export function query(params: Record<string, string | number | boolean | string[] | null | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value === null || value === undefined || value === "") continue;
    if (Array.isArray(value)) {
      value.forEach((entry) => search.append(key, entry));
    } else {
      search.set(key, String(value));
    }
  }
  const encoded = search.toString();
  return encoded ? `?${encoded}` : "";
}
