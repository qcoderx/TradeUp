import { cx } from "../lib/format";

/**
 * The TradeUp mark: one arrow leaving, one arrow coming back.
 *
 * Hand-authored rather than generated, because a brand mark is exact geometry —
 * it has to stay crisp at 16px in a browser tab and at 200px in the footer, and
 * it has to be able to take its colour from whatever it is sitting on.
 */
export function LogoMark({ className, tone = "duo" }: { className?: string; tone?: "duo" | "mono" }) {
  const rising = tone === "mono" ? "currentColor" : "var(--tu-marigold-bright)";
  const returning = tone === "mono" ? "currentColor" : "var(--tu-indigo)";

  return (
    <svg viewBox="0 0 48 48" className={className} aria-hidden="true" focusable="false">
      <g fill="none" strokeWidth={5.5} strokeLinecap="round" strokeLinejoin="round">
        {/* Trading up. */}
        <g stroke={rising}>
          <path d="M13 28 L30 11" />
          <path d="M21.5 11 L30 11 L30 19.5" />
        </g>
        {/* And something coming back the other way. */}
        <g stroke={returning} opacity={tone === "mono" ? 0.5 : 1}>
          <path d="M35 20 L18 37" />
          <path d="M18 28.5 L18 37 L26.5 37" />
        </g>
      </g>
    </svg>
  );
}

/** The mark plus the wordmark, as used in the navbar and the footer. */
export function Logo({
  className,
  tone = "duo",
  showWord = true,
}: {
  className?: string;
  tone?: "duo" | "mono";
  showWord?: boolean;
}) {
  return (
    <span className={cx("inline-flex items-center gap-2", className)}>
      <LogoMark className="h-8 w-8 shrink-0" tone={tone} />
      {showWord && (
        <span className="font-display text-[1.35rem] leading-none font-bold tracking-[-0.03em]">
          Trade<span className="text-marigold">Up</span>
        </span>
      )}
    </span>
  );
}
