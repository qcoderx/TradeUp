import { useState } from "react";
import { Link } from "react-router-dom";
import { Bookmark, ImageOff, Leaf, Repeat2 } from "lucide-react";
import { request } from "../lib/api";
import { useAuth } from "../lib/auth";
import { cx, formatOwnerGeneration, formatPrice } from "../lib/format";
import type { ListingCard } from "../lib/types";
import { Badge } from "./ui";

/**
 * The trade ticket — the one element this marketplace is remembered by.
 *
 * A second-hand item is not a product in a catalogue; it is an object with a
 * history. So a listing is drawn as a ticket rather than a product card: a
 * torn stub along the bottom carrying the reference code, which owner this is,
 * and what reusing it saves. The provenance is the point, so it gets the
 * typographic treatment usually reserved for the price.
 */
export function TradeTicket({
  listing,
  onSaveChange,
  priority = false,
}: {
  listing: ListingCard;
  onSaveChange?: (listingId: number, saved: boolean) => void;
  priority?: boolean;
}) {
  const { user } = useAuth();
  const [saved, setSaved] = useState(listing.savedByViewer);
  const [saving, setSaving] = useState(false);
  const [imageFailed, setImageFailed] = useState(false);

  const isOwn = user?.id === listing.owner.id;
  const reserved = listing.statusName === "RESERVED";

  async function toggleSave(event: React.MouseEvent) {
    // The button sits inside the card link, so stop the navigation.
    event.preventDefault();
    event.stopPropagation();

    if (!user || isOwn || saving) return;

    const next = !saved;
    setSaved(next); // Optimistic: the tap should feel instant.
    setSaving(true);
    try {
      const result = await request<{ saved: boolean }>(`/listings/${listing.id}/save`, { method: "POST" });
      setSaved(result.saved);
      onSaveChange?.(listing.id, result.saved);
    } catch {
      setSaved(!next); // Put it back the way it was.
    } finally {
      setSaving(false);
    }
  }

  return (
    <article className="ticket group flex h-full flex-col overflow-hidden">
      <Link
        to={`/listings/${listing.id}`}
        className="flex h-full cursor-pointer flex-col focus-visible:outline-none"
        aria-label={`${listing.title}, ${formatPrice(listing.priceKobo)}`}
      >
        {/* Photo ------------------------------------------------------- */}
        <div className="aspect-item relative overflow-hidden bg-sunk">
          {listing.primaryImageUrl && !imageFailed ? (
            <img
              src={listing.primaryImageUrl}
              alt={listing.title}
              loading={priority ? "eager" : "lazy"}
              decoding="async"
              onError={() => setImageFailed(true)}
              className="h-full w-full object-cover transition-[filter] duration-300 group-hover:brightness-[1.03]"
            />
          ) : (
            <div className="flex h-full w-full flex-col items-center justify-center gap-1.5 text-faint">
              <ImageOff className="h-6 w-6" aria-hidden="true" />
              <span className="text-xs">No photo yet</span>
            </div>
          )}

          {/* Intent and status sit on the photo so the body stays for words. */}
          <div className="absolute top-2.5 left-2.5 flex flex-wrap gap-1.5">
            {listing.intentName !== "SELL" && (
              <Badge tone="marigold" className="shadow-sm backdrop-blur">
                <Repeat2 className="h-3 w-3" aria-hidden="true" />
                {listing.intentName === "SWAP" ? "Swap" : "Sale or swap"}
              </Badge>
            )}
            {reserved && (
              <Badge tone="indigo" className="shadow-sm backdrop-blur">
                Reserved
              </Badge>
            )}
          </div>

          {user && !isOwn && (
            <button
              type="button"
              onClick={toggleSave}
              aria-pressed={saved}
              aria-label={saved ? `Remove ${listing.title} from saved` : `Save ${listing.title}`}
              className={cx(
                "absolute top-2 right-2 grid h-9 w-9 cursor-pointer place-items-center rounded-full",
                "bg-surface/92 backdrop-blur transition-colors duration-200",
                saved ? "text-marigold" : "text-muted hover:text-ink"
              )}
            >
              <Bookmark className="h-4 w-4" fill={saved ? "currentColor" : "none"} aria-hidden="true" />
            </button>
          )}
        </div>

        {/* Body -------------------------------------------------------- */}
        <div className="flex flex-1 flex-col gap-2 px-4 pt-3.5 pb-4">
          <p className="eyebrow">{listing.categoryLabel}</p>

          <h3 className="line-clamp-2 font-display text-[1.0625rem] leading-tight font-semibold text-ink">
            {listing.title}
          </h3>

          {/* The swap note sits above the price so that the price is always the
              last row, and prices line up across every card in a grid row. */}
          <div className="mt-auto flex flex-col gap-2 pt-1">
            {listing.intentName !== "SELL" && listing.swapWanted && (
              <p className="line-clamp-1 text-xs text-muted">
                <span className="font-medium text-ink">Wants:</span> {listing.swapWanted}
              </p>
            )}
            <div className="flex items-end justify-between gap-3">
              <p className="tabular font-display text-xl leading-none font-bold text-ink">
                {formatPrice(listing.priceKobo)}
              </p>
              <span className="text-right text-xs leading-tight text-muted">{listing.conditionLabel}</span>
            </div>
          </div>
        </div>

        {/* Stub — the provenance stamp --------------------------------- */}
        <div className="ticket-stub mx-3 flex items-center justify-between gap-2 py-2.5">
          <span className="font-mono text-[0.6875rem] tracking-wide text-faint">{listing.reference}</span>
          <span className="flex items-center gap-2 font-mono text-[0.6875rem] tracking-wide">
            <span className="text-muted">{formatOwnerGeneration(listing.ownerGeneration)}</span>
            <span className="inline-flex items-center gap-1 text-leaf">
              <Leaf className="h-3 w-3" aria-hidden="true" />
              {listing.co2SavedKg.toFixed(1)}kg
            </span>
          </span>
        </div>
      </Link>
    </article>
  );
}

/** Placeholder with the same footprint, so the grid does not jump on load. */
export function TradeTicketSkeleton() {
  return (
    <div className="ticket flex h-full flex-col overflow-hidden">
      <div className="aspect-item skeleton rounded-none" />
      <div className="flex flex-col gap-2.5 px-4 pt-4 pb-5">
        <div className="skeleton h-2.5 w-20" />
        <div className="skeleton h-4 w-full" />
        <div className="skeleton h-4 w-2/3" />
        <div className="skeleton mt-2 h-6 w-28" />
      </div>
      <div className="ticket-stub mx-3 flex justify-between py-3">
        <div className="skeleton h-2.5 w-16" />
        <div className="skeleton h-2.5 w-24" />
      </div>
    </div>
  );
}
