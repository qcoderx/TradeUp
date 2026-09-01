/** Formatting helpers. Naira, dates, and the ordinals on the trade ticket. */

const naira = new Intl.NumberFormat("en-NG", {
  style: "currency",
  currency: "NGN",
  maximumFractionDigits: 0,
});

/**
 * Renders a price held in kobo.
 *
 * A null price is not zero: it means the item is swap-only, and saying so is
 * more useful than printing a currency symbol next to nothing.
 */
export function formatPrice(kobo: number | null): string {
  if (kobo === null || kobo === undefined) return "Swap only";
  return naira.format(kobo / 100);
}

/** The same value with no currency symbol, for tight spaces like offer rows. */
export function formatAmount(kobo: number | null): string {
  if (kobo === null || kobo === undefined) return "—";
  return new Intl.NumberFormat("en-NG", { maximumFractionDigits: 0 }).format(kobo / 100);
}

/** "3rd owner" — the provenance stamp printed on every ticket. */
export function formatOwnerGeneration(generation: number): string {
  if (generation <= 1) return "First owner";
  return `${ordinal(generation)} owner`;
}

export function ordinal(value: number): string {
  const remainderTen = value % 10;
  const remainderHundred = value % 100;
  if (remainderTen === 1 && remainderHundred !== 11) return `${value}st`;
  if (remainderTen === 2 && remainderHundred !== 12) return `${value}nd`;
  if (remainderTen === 3 && remainderHundred !== 13) return `${value}rd`;
  return `${value}th`;
}

/** Compact relative time: "just now", "4h ago", "12 Mar". */
export function timeAgo(iso: string): string {
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) return "";

  const seconds = Math.round((Date.now() - then) / 1000);
  if (seconds < 60) return "just now";

  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;

  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h ago`;

  const days = Math.round(hours / 24);
  if (days < 7) return `${days}d ago`;
  if (days < 365) {
    return new Date(iso).toLocaleDateString("en-NG", { day: "numeric", month: "short" });
  }
  return new Date(iso).toLocaleDateString("en-NG", { day: "numeric", month: "short", year: "numeric" });
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-NG", {
    day: "numeric",
    month: "long",
    year: "numeric",
  });
}

export function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString("en-NG", { hour: "numeric", minute: "2-digit" });
}

/**
 * Rounds a CO2 figure to something a person can hold in their head, and keeps
 * the unit attached so it can never be read as naira.
 */
export function formatCo2(kg: number): string {
  if (kg >= 1000) return `${(kg / 1000).toFixed(1)} tonnes`;
  if (kg >= 100) return `${Math.round(kg)} kg`;
  return `${kg.toFixed(1)} kg`;
}

export function formatWeight(kg: number): string {
  if (kg >= 1000) return `${(kg / 1000).toFixed(1)} t`;
  return `${kg.toFixed(1)} kg`;
}

/** Joins class names, dropping anything falsy. */
export function cx(...parts: Array<string | false | null | undefined>): string {
  return parts.filter(Boolean).join(" ");
}
