import { useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRight, Bookmark, Check, PackagePlus, X } from "lucide-react";
import { ApiError, assetUrl, request } from "../lib/api";
import { useAuth } from "../lib/auth";
import { useApi } from "../lib/useApi";
import { formatAmount, formatCo2, timeAgo } from "../lib/format";
import type { DashboardSummary, OfferView, PageResponse, ListingCard } from "../lib/types";
import { TradeTicket, TradeTicketSkeleton } from "../components/TradeTicket";
import { Avatar, Badge, Button, Card, EmptyState, ErrorState, LinkButton, Skeleton } from "../components/ui";

export function Dashboard() {
  const { user } = useAuth();
  const { data, error, loading, reload } = useApi<DashboardSummary>("/me/dashboard");
  const { data: mine, reload: reloadMine } = useApi<PageResponse<ListingCard>>("/me/listings?size=12");

  if (error) return <ErrorState message={error.message} onRetry={reload} />;

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="eyebrow">Your board</p>
          {/* The full name, not a guessed first name: many Nigerian names put the
              surname first, so splitting on the first token greets people wrongly. */}
          <h1 className="mt-2 text-[clamp(1.9rem,4vw,2.75rem)]">{user?.fullName ?? "Your dashboard"}</h1>
        </div>
        <LinkButton to="/sell">
          <PackagePlus className="h-4 w-4" aria-hidden="true" />
          List an item
        </LinkButton>
      </header>

      {/* Stats ---------------------------------------------------------- */}
      <dl className="mt-8 grid grid-cols-2 gap-px overflow-hidden rounded-xl bg-line lg:grid-cols-5">
        {loading || !data ? (
          Array.from({ length: 5 }, (_, index) => (
            <div key={index} className="bg-surface p-5">
              <Skeleton className="h-7 w-14" />
              <Skeleton className="mt-2 h-3 w-20" />
            </div>
          ))
        ) : (
          <>
            <Stat value={data.activeListings} label="on the board" />
            <Stat value={data.reservedListings} label="on hold" />
            <Stat value={data.completedTrades} label="traded" />
            <Stat value={data.unreadMessages} label="unread messages" to="/messages" />
            <Stat value={formatCo2(data.personalCo2SavedKg)} label="CO₂e you have saved" />
          </>
        )}
      </dl>

      {/* Offers waiting -------------------------------------------------- */}
      {data && data.offersAwaitingYou.length > 0 && (
        <section className="mt-12">
          <h2 className="text-[clamp(1.3rem,2.4vw,1.65rem)]">Offers waiting on you</h2>
          <div className="mt-5 flex flex-col gap-3">
            {data.offersAwaitingYou.map((offer) => (
              <OfferRow
                key={offer.id}
                offer={offer}
                onResolved={() => {
                  reload();
                  reloadMine();
                }}
              />
            ))}
          </div>
        </section>
      )}

      {/* Listings -------------------------------------------------------- */}
      <section className="mt-12">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <h2 className="text-[clamp(1.3rem,2.4vw,1.65rem)]">Everything you have listed</h2>
          <Link
            to="/saved"
            className="group inline-flex cursor-pointer items-center gap-1.5 text-sm font-semibold text-muted transition-colors duration-200 hover:text-ink"
          >
            <Bookmark className="h-4 w-4" aria-hidden="true" />
            Saved items
            <ArrowRight
              className="h-4 w-4 transition-transform duration-200 group-hover:translate-x-0.5"
              aria-hidden="true"
            />
          </Link>
        </div>

        <div className="mt-6">
          {!mine ? (
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
              {Array.from({ length: 4 }, (_, index) => (
                <TradeTicketSkeleton key={index} />
              ))}
            </div>
          ) : mine.items.length === 0 ? (
            <EmptyState
              icon={<PackagePlus className="h-8 w-8" aria-hidden="true" />}
              title="You have not listed anything yet"
              description="Start with the thing you have already stopped using. A photo and two lines is enough."
              action={<LinkButton to="/sell">List your first item</LinkButton>}
            />
          ) : (
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
              {mine.items.map((listing) => (
                <TradeTicket key={listing.id} listing={listing} />
              ))}
            </div>
          )}
        </div>
      </section>
    </div>
  );
}

function Stat({ value, label, to }: { value: number | string; label: string; to?: string }) {
  const body = (
    <>
      <dd className="tabular font-display text-2xl leading-none font-bold text-ink">{value}</dd>
      <dt className="mt-1.5 text-xs leading-snug text-muted">{label}</dt>
    </>
  );

  return to ? (
    <Link to={to} className="cursor-pointer bg-surface p-5 transition-colors duration-200 hover:bg-sunk">
      {body}
    </Link>
  ) : (
    <div className="bg-surface p-5">{body}</div>
  );
}

/** One pending offer, with the two decisions the lister can make. */
function OfferRow({ offer, onResolved }: { offer: OfferView; onResolved: () => void }) {
  const [busy, setBusy] = useState<"accept" | "decline" | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function respond(action: "accept" | "decline") {
    setBusy(action);
    setError(null);
    try {
      await request(`/offers/${offer.id}/${action}`, { method: "POST" });
      onResolved();
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : "That did not work.");
      setBusy(null);
    }
  }

  return (
    <Card className="flex flex-wrap items-center gap-4">
      {offer.listingImageUrl && (
        <img
          src={assetUrl(offer.listingImageUrl)}
          alt=""
          className="h-14 w-14 shrink-0 rounded-lg object-cover"
          loading="lazy"
        />
      )}

      <div className="min-w-48 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <Badge tone={offer.kind === "CASH" ? "green" : "blue"}>
            {offer.kind === "CASH" ? `₦${formatAmount(offer.amountKobo)}` : "Swap"}
          </Badge>
          <span className="text-xs text-muted">{timeAgo(offer.createdAt)}</span>
        </div>

        <p className="mt-1.5 text-sm text-ink">
          <Link to={`/listings/${offer.listingId}`} className="cursor-pointer font-semibold hover:underline">
            {offer.listingTitle}
          </Link>
        </p>

        <p className="mt-1 flex items-center gap-1.5 text-xs text-muted">
          <Avatar initials={offer.offeredBy.initials} size={20} />
          {offer.offeredBy.fullName}
          {offer.kind === "SWAP" && offer.offeredListingTitle && (
            <> · offering {offer.offeredListingTitle}</>
          )}
        </p>

        {offer.note && <p className="mt-2 text-sm leading-relaxed text-muted italic">“{offer.note}”</p>}
        {error && (
          <p role="alert" className="mt-2 text-xs text-red">
            {error}
          </p>
        )}
      </div>

      <div className="flex gap-2">
        <Button
          tone="quiet"
          busy={busy === "decline"}
          disabled={busy !== null}
          onClick={() => respond("decline")}
        >
          <X className="h-4 w-4" aria-hidden="true" />
          Decline
        </Button>
        <Button busy={busy === "accept"} disabled={busy !== null} onClick={() => respond("accept")}>
          <Check className="h-4 w-4" aria-hidden="true" />
          Accept
        </Button>
      </div>
    </Card>
  );
}
