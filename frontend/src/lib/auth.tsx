import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { ApiError, getToken, request, setToken } from "./api";
import type { AuthResponse, UserSummary } from "./types";

interface AuthState {
  user: UserSummary | null;
  /** True until the stored token has been checked against the server. */
  loading: boolean;
  signIn: (identifier: string, password: string) => Promise<void>;
  register: (input: RegisterInput) => Promise<void>;
  signOut: () => void;
  refresh: () => Promise<void>;
}

export interface RegisterInput {
  fullName: string;
  email: string;
  matricNumber: string;
  password: string;
  department?: string;
  campusLocation?: string;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserSummary | null>(null);
  const [loading, setLoading] = useState(true);

  /**
   * A token in storage is only a claim. This confirms it with the server before
   * the app renders anything as signed in, so a revoked or expired token cannot
   * leave a stale name in the navbar.
   */
  useEffect(() => {
    let cancelled = false;

    async function restore() {
      if (!getToken()) {
        setLoading(false);
        return;
      }
      try {
        const me = await request<UserSummary>("/auth/me");
        if (!cancelled) setUser(me);
      } catch {
        setToken(null);
        if (!cancelled) setUser(null);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    void restore();
    return () => {
      cancelled = true;
    };
  }, []);

  const adopt = useCallback((response: AuthResponse) => {
    setToken(response.token);
    setUser(response.user);
  }, []);

  const signIn = useCallback(
    async (identifier: string, password: string) => {
      adopt(await request<AuthResponse>("/auth/login", { method: "POST", body: { identifier, password } }));
    },
    [adopt]
  );

  const register = useCallback(
    async (input: RegisterInput) => {
      adopt(await request<AuthResponse>("/auth/register", { method: "POST", body: input }));
    },
    [adopt]
  );

  const signOut = useCallback(() => {
    setToken(null);
    setUser(null);
  }, []);

  const refresh = useCallback(async () => {
    try {
      setUser(await request<UserSummary>("/auth/me"));
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) signOut();
    }
  }, [signOut]);

  const value = useMemo<AuthState>(
    () => ({ user, loading, signIn, register, signOut, refresh }),
    [user, loading, signIn, register, signOut, refresh]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside an AuthProvider.");
  return context;
}
