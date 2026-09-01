import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { ApiError } from "../lib/api";
import { useAuth } from "../lib/auth";
import { LogoMark } from "../components/Logo";
import { Button, Field, TextInput } from "../components/ui";

/** Shared two-column shell: form on the left, the argument for joining on the right. */
function AuthShell({ title, lead, children }: { title: string; lead: string; children: React.ReactNode }) {
  return (
    <div className="grid min-h-[calc(100dvh-4rem)] lg:grid-cols-2">
      <div className="flex items-center justify-center px-4 py-12 sm:px-8">
        <div className="w-full max-w-sm">
          <LogoMark className="h-10 w-10" />
          <h1 className="mt-6 text-[clamp(1.75rem,4vw,2.25rem)]">{title}</h1>
          <p className="mt-2.5 text-sm leading-relaxed text-muted">{lead}</p>
          <div className="mt-8">{children}</div>
        </div>
      </div>

      <aside className="adire hidden flex-col justify-center px-12 lg:flex">
        <blockquote className="max-w-md">
          <p className="font-display text-[2rem] leading-[1.15] font-semibold text-white">
            The textbook you finished with in June is the one a first year cannot afford in October.
          </p>
          <footer className="mt-6 font-mono text-[0.6875rem] tracking-[0.14em] text-marigold-bright uppercase">
            TradeUp · Group 15 · COS202
          </footer>
        </blockquote>
      </aside>
    </div>
  );
}

/* -------------------------------------------------------------------------
   Sign in
   ------------------------------------------------------------------------- */

export function SignIn() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const returnTo = (location.state as { from?: string } | null)?.from ?? "/dashboard";

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await signIn(identifier, password);
      navigate(returnTo, { replace: true });
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : "Could not sign you in.");
      setBusy(false);
    }
  }

  return (
    <AuthShell title="Welcome back" lead="Sign in with your email or your matric number.">
      <form onSubmit={submit} className="flex flex-col gap-4">
        <Field label="Email or matric number" htmlFor="identifier" required>
          <TextInput
            id="identifier"
            value={identifier}
            onChange={(event) => setIdentifier(event.target.value)}
            autoComplete="username"
            required
            autoFocus
          />
        </Field>

        <Field label="Password" htmlFor="password" required>
          <TextInput
            id="password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
            required
          />
        </Field>

        {error && (
          <p role="alert" className="rounded-lg bg-clay-soft px-3.5 py-2.5 text-sm text-clay">
            {error}
          </p>
        )}

        <Button type="submit" busy={busy} full className="mt-1">
          Sign in
        </Button>
      </form>

      <p className="mt-6 text-sm text-muted">
        No account yet?{" "}
        <Link to="/join" className="cursor-pointer font-semibold text-ink underline underline-offset-2">
          Create one
        </Link>
      </p>
    </AuthShell>
  );
}

/* -------------------------------------------------------------------------
   Register
   ------------------------------------------------------------------------- */

export function Join() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    fullName: "",
    email: "",
    matricNumber: "",
    password: "",
    department: "",
    campusLocation: "",
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  function set(key: keyof typeof form) {
    return (event: React.ChangeEvent<HTMLInputElement>) =>
      setForm((current) => ({ ...current, [key]: event.target.value }));
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setFieldErrors({});
    try {
      await register(form);
      navigate("/dashboard", { replace: true });
    } catch (cause) {
      if (cause instanceof ApiError) {
        setFieldErrors(cause.fieldErrors);
        // A field-level message is already shown against its input.
        setError(Object.keys(cause.fieldErrors).length > 0 ? null : cause.message);
      } else {
        setError("Could not create your account.");
      }
      setBusy(false);
    }
  }

  return (
    <AuthShell
      title="Join TradeUp"
      lead="Free, and only for students. Your matric number keeps the board to people on campus."
    >
      <form onSubmit={submit} className="flex flex-col gap-4">
        <Field label="Full name" htmlFor="fullName" error={fieldErrors.fullName} required>
          <TextInput
            id="fullName"
            value={form.fullName}
            onChange={set("fullName")}
            error={fieldErrors.fullName}
            autoComplete="name"
            required
            autoFocus
          />
        </Field>

        <Field label="Email" htmlFor="email" error={fieldErrors.email} required>
          <TextInput
            id="email"
            type="email"
            value={form.email}
            onChange={set("email")}
            error={fieldErrors.email}
            autoComplete="email"
            placeholder="you@live.unilag.edu.ng"
            required
          />
        </Field>

        <Field
          label="Matric number"
          htmlFor="matricNumber"
          error={fieldErrors.matricNumber}
          hint="Nine digits, e.g. 240817017."
          required
        >
          <TextInput
            id="matricNumber"
            inputMode="numeric"
            value={form.matricNumber}
            onChange={set("matricNumber")}
            error={fieldErrors.matricNumber}
            maxLength={9}
            required
            className="tabular"
          />
        </Field>

        <Field
          label="Password"
          htmlFor="new-password"
          error={fieldErrors.password}
          hint="At least 8 characters."
          required
        >
          <TextInput
            id="new-password"
            type="password"
            value={form.password}
            onChange={set("password")}
            error={fieldErrors.password}
            autoComplete="new-password"
            minLength={8}
            required
          />
        </Field>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Department" htmlFor="department" error={fieldErrors.department}>
            <TextInput
              id="department"
              value={form.department}
              onChange={set("department")}
              placeholder="Computer Science"
            />
          </Field>

          <Field label="Hall or area" htmlFor="campusLocation" error={fieldErrors.campusLocation}>
            <TextInput
              id="campusLocation"
              value={form.campusLocation}
              onChange={set("campusLocation")}
              placeholder="Moremi Hall"
            />
          </Field>
        </div>

        {error && (
          <p role="alert" className="rounded-lg bg-clay-soft px-3.5 py-2.5 text-sm text-clay">
            {error}
          </p>
        )}

        <Button type="submit" busy={busy} full className="mt-1">
          Create account
        </Button>
      </form>

      <p className="mt-6 text-sm text-muted">
        Already have one?{" "}
        <Link to="/signin" className="cursor-pointer font-semibold text-ink underline underline-offset-2">
          Sign in
        </Link>
      </p>
    </AuthShell>
  );
}
