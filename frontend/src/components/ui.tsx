import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from "react";
import { Link } from "react-router-dom";
import { Loader2 } from "lucide-react";
import { cx } from "../lib/format";

/* -------------------------------------------------------------------------
   Buttons
   ------------------------------------------------------------------------- */

type ButtonTone = "primary" | "ink" | "outline" | "quiet";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  tone?: ButtonTone;
  /** Shows a spinner and blocks further clicks while a request is in flight. */
  busy?: boolean;
  full?: boolean;
}

export function Button({ tone = "primary", busy, full, className, children, disabled, ...rest }: ButtonProps) {
  return (
    <button
      {...rest}
      disabled={disabled || busy}
      aria-busy={busy || undefined}
      className={cx("btn", `btn-${tone}`, full && "w-full", className)}
    >
      {busy && <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />}
      {children}
    </button>
  );
}

export function LinkButton({
  to,
  tone = "primary",
  full,
  className,
  children,
}: {
  to: string;
  tone?: ButtonTone;
  full?: boolean;
  className?: string;
  children: ReactNode;
}) {
  return (
    <Link to={to} className={cx("btn", `btn-${tone}`, full && "w-full", className)}>
      {children}
    </Link>
  );
}

/* -------------------------------------------------------------------------
   Form fields
   ------------------------------------------------------------------------- */

interface FieldShellProps {
  label: string;
  htmlFor: string;
  error?: string;
  hint?: string;
  required?: boolean;
  children: ReactNode;
}

/** Label, control, hint and error in the one order on every form. */
export function Field({ label, htmlFor, error, hint, required, children }: FieldShellProps) {
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={htmlFor} className="text-sm font-semibold text-ink">
        {label}
        {required && (
          <span className="ml-1 text-red" aria-hidden="true">
            *
          </span>
        )}
      </label>
      {children}
      {hint && !error && <p className="text-xs text-muted">{hint}</p>}
      {error && (
        <p id={`${htmlFor}-error`} role="alert" className="text-xs font-medium text-red">
          {error}
        </p>
      )}
    </div>
  );
}

export function TextInput({
  error,
  className,
  id,
  ...rest
}: InputHTMLAttributes<HTMLInputElement> & { error?: string }) {
  return (
    <input
      {...rest}
      id={id}
      aria-invalid={error ? true : undefined}
      aria-describedby={error ? `${id}-error` : undefined}
      className={cx("field", className)}
    />
  );
}

export function TextArea({
  error,
  className,
  id,
  ...rest
}: TextareaHTMLAttributes<HTMLTextAreaElement> & { error?: string }) {
  return (
    <textarea
      {...rest}
      id={id}
      aria-invalid={error ? true : undefined}
      aria-describedby={error ? `${id}-error` : undefined}
      className={cx("field resize-y leading-relaxed", className)}
    />
  );
}

export function Select({
  error,
  className,
  id,
  children,
  ...rest
}: SelectHTMLAttributes<HTMLSelectElement> & { error?: string }) {
  return (
    <select
      {...rest}
      id={id}
      aria-invalid={error ? true : undefined}
      aria-describedby={error ? `${id}-error` : undefined}
      className={cx("field cursor-pointer appearance-none pr-9", className)}
      style={{
        backgroundImage:
          "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16' fill='none' stroke='%237d8899' stroke-width='1.8' stroke-linecap='round'%3E%3Cpath d='M4 6.5 8 10.5 12 6.5'/%3E%3C/svg%3E\")",
        backgroundRepeat: "no-repeat",
        backgroundPosition: "right 0.7rem center",
        backgroundSize: "1rem",
      }}
    >
      {children}
    </select>
  );
}

/* -------------------------------------------------------------------------
   Badges, cards, states
   ------------------------------------------------------------------------- */

/**
 * Three hues, three jobs. The crest has no fourth colour, so a badge that does
 * not mean "good", "informational" or "wrong" stays neutral rather than
 * inventing one.
 */
type BadgeTone = "neutral" | "green" | "blue" | "red";

const BADGE_TONES: Record<BadgeTone, string> = {
  neutral: "bg-sunk text-muted",
  green: "bg-green-soft text-green",
  blue: "bg-blue-soft text-blue",
  red: "bg-red-soft text-red",
};

export function Badge({
  tone = "neutral",
  className,
  children,
}: {
  tone?: BadgeTone;
  className?: string;
  children: ReactNode;
}) {
  return <span className={cx("badge", BADGE_TONES[tone], className)}>{children}</span>;
}

export function Card({ className, children }: { className?: string; children: ReactNode }) {
  return (
    <div className={cx("rounded-xl bg-surface p-5 shadow-[var(--shadow-inset-line)]", className)}>{children}</div>
  );
}

/** The shared empty state: says what is missing, and offers the way out. */
export function EmptyState({
  icon,
  title,
  description,
  action,
}: {
  icon?: ReactNode;
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-line-strong px-6 py-14 text-center">
      {icon && <div className="text-faint">{icon}</div>}
      <h3 className="font-display text-xl text-ink">{title}</h3>
      <p className="max-w-sm text-sm text-muted">{description}</p>
      {action && <div className="mt-2">{action}</div>}
    </div>
  );
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-xl border border-red/30 bg-red-soft px-6 py-10 text-center">
      <h3 className="font-display text-lg text-ink">That did not load</h3>
      <p className="max-w-sm text-sm text-muted">{message}</p>
      {onRetry && (
        <Button tone="outline" onClick={onRetry} className="mt-1">
          Try again
        </Button>
      )}
    </div>
  );
}

export function Skeleton({ className }: { className?: string }) {
  return <div className={cx("skeleton", className)} aria-hidden="true" />;
}

/** The avatar fallback. Initials come from the API so both sides agree. */
export function Avatar({ initials, size = 36 }: { initials: string; size?: number }) {
  return (
    <span
      className="inline-flex shrink-0 items-center justify-center rounded-full bg-blue-soft font-mono font-medium text-blue"
      style={{ width: size, height: size, fontSize: size * 0.36 }}
      aria-hidden="true"
    >
      {initials}
    </span>
  );
}
