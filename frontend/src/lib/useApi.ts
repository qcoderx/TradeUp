import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError, request } from "./api";

interface QueryState<T> {
  data: T | null;
  error: ApiError | null;
  loading: boolean;
  /** Re-runs the request, e.g. after a mutation elsewhere on the page. */
  reload: () => void;
}

/**
 * Fetches a path and tracks the three states a screen actually has to draw:
 * loading, failed, and loaded.
 *
 * The abort controller matters more than it looks: without it, navigating away
 * mid-request lands a setState on an unmounted screen, and a slow response can
 * overwrite a newer one when filters change quickly.
 */
export function useApi<T>(path: string | null, deps: unknown[] = []): QueryState<T> {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [loading, setLoading] = useState(path !== null);
  const [nonce, setNonce] = useState(0);

  const latest = useRef(0);

  const reload = useCallback(() => setNonce((value) => value + 1), []);

  useEffect(() => {
    if (path === null) {
      setData(null);
      setLoading(false);
      return;
    }

    const controller = new AbortController();
    const ticket = ++latest.current;

    setLoading(true);
    setError(null);

    request<T>(path, { signal: controller.signal })
      .then((result) => {
        if (ticket === latest.current) {
          setData(result);
          setLoading(false);
        }
      })
      .catch((cause: unknown) => {
        if ((cause as Error)?.name === "AbortError") return;
        if (ticket === latest.current) {
          setError(cause instanceof ApiError ? cause : new ApiError(0, { message: String(cause) }));
          setLoading(false);
        }
      });

    return () => controller.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [path, nonce, ...deps]);

  return { data, error, loading, reload };
}
