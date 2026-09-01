import { useParams } from "react-router-dom";
import { MapPin, PackageOpen } from "lucide-react";
import { useApi } from "../lib/useApi";
import { formatDate } from "../lib/format";
import type { UserProfile } from "../lib/types";
import { TradeTicket, TradeTicketSkeleton } from "../components/TradeTicket";
import { Avatar, EmptyState, ErrorState, Skeleton } from "../components/ui";

export function ProfilePage() {
  const { id } = useParams<{ id: string }>();
  const { data, error, loading, reload } = useApi<UserProfile>(id ? `/users/${id}/profile` : null);

  if (error) return <ErrorState message={error.message} onRetry={reload} />;

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6">
      <header className="flex flex-wrap items-start gap-6 border-b border-line pb-9">
        {loading || !data ? (
          <>
            <Skeleton className="h-20 w-20 rounded-full" />
            <div className="flex flex-col gap-2">
              <Skeleton className="h-8 w-56" />
              <Skeleton className="h-4 w-40" />
            </div>
          </>
        ) : (
          <>
            <Avatar initials={data.initials} size={80} />
            <div className="min-w-0 flex-1">
              <h1 className="text-[clamp(1.6rem,3.4vw,2.35rem)]">{data.fullName}</h1>
              <p className="mt-1.5 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-muted">
                {data.department && <span>{data.department}</span>}
                {data.campusLocation && (
                  <span className="inline-flex items-center gap-1.5">
                    <MapPin className="h-4 w-4" aria-hidden="true" />
                    {data.campusLocation}
                  </span>
                )}
                <span>Joined {formatDate(data.joinedAt)}</span>
              </p>
              {data.bio && <p className="mt-4 max-w-xl leading-relaxed text-muted">{data.bio}</p>}
            </div>

            <dl className="flex gap-8">
              <div>
                <dd className="tabular font-display text-2xl leading-none font-bold text-ink">
                  {data.completedTrades}
                </dd>
                <dt className="mt-1.5 text-xs text-muted">completed trades</dt>
              </div>
              <div>
                <dd className="tabular font-display text-2xl leading-none font-bold text-ink">
                  {data.activeListings.length}
                </dd>
                <dt className="mt-1.5 text-xs text-muted">on the board</dt>
              </div>
            </dl>
          </>
        )}
      </header>

      <section className="mt-10">
        <h2 className="text-[clamp(1.3rem,2.4vw,1.65rem)]">Currently listed</h2>

        <div className="mt-6">
          {loading ? (
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
              {Array.from({ length: 4 }, (_, index) => (
                <TradeTicketSkeleton key={index} />
              ))}
            </div>
          ) : data && data.activeListings.length === 0 ? (
            <EmptyState
              icon={<PackageOpen className="h-8 w-8" aria-hidden="true" />}
              title="Nothing on the board right now"
              description={`${data.fullName} has no items available at the moment.`}
            />
          ) : (
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
              {data?.activeListings.map((listing) => (
                <TradeTicket key={listing.id} listing={listing} />
              ))}
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
