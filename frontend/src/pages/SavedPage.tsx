import { useState } from "react";
import { Bookmark } from "lucide-react";
import { useApi } from "../lib/useApi";
import type { ListingCard } from "../lib/types";
import { TradeTicket, TradeTicketSkeleton } from "../components/TradeTicket";
import { EmptyState, ErrorState, LinkButton } from "../components/ui";

export function SavedPage() {
  const { data, error, loading, reload } = useApi<ListingCard[]>("/me/saved");
  const [removed, setRemoved] = useState<Set<number>>(new Set());

  // Unsaving from this page should drop the card, not leave a saved item that
  // is no longer saved sitting in the grid.
  const visible = (data ?? []).filter((listing) => !removed.has(listing.id));

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6">
      <p className="eyebrow">Kept an eye on</p>
      <h1 className="mt-2 text-[clamp(1.9rem,4vw,2.75rem)]">Saved items</h1>

      <div className="mt-8">
        {error ? (
          <ErrorState message={error.message} onRetry={reload} />
        ) : loading ? (
          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {Array.from({ length: 4 }, (_, index) => (
              <TradeTicketSkeleton key={index} />
            ))}
          </div>
        ) : visible.length === 0 ? (
          <EmptyState
            icon={<Bookmark className="h-8 w-8" aria-hidden="true" />}
            title="Nothing saved yet"
            description="Tap the bookmark on any listing and it will wait for you here while you decide."
            action={<LinkButton to="/browse">Browse the board</LinkButton>}
          />
        ) : (
          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {visible.map((listing) => (
              <TradeTicket
                key={listing.id}
                listing={listing}
                onSaveChange={(listingId, saved) => {
                  if (!saved) setRemoved((current) => new Set(current).add(listingId));
                }}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
