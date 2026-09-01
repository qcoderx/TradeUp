import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  AlertTriangle,
  Bookmark,
  Check,
  Eye,
  Leaf,
  MapPin,
  MessageSquare,
  Pencil,
  Repeat2,
  Trash2,
} from "lucide-react";
import { ApiError, request } from "../lib/api";
import { useAuth } from "../lib/auth";
import { useApi } from "../lib/useApi";
import { cx, formatDate, formatOwnerGeneration, formatPrice, timeAgo } from "../lib/format";
import type { ListingCard, ListingDetail, OfferView } from "../lib/types";
import { TradeTicket } from "../components/TradeTicket";
import { Avatar, Badge, Button, Card, ErrorState, Field, LinkButton, Select, Skeleton, TextArea, TextInput } from "../components/ui";
import { Modal } from "../components/Modal";

export function ListingPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();

  const { data: listing, error, loading, reload } = useApi<ListingDetail>(id ? `/listings/${id}` : null);
  const { data: similar } = useApi<ListingCard[]>(id ? `/listings/${id}/similar?limit=4` : null);

  const [activeImage, setActiveImage] = useState(0);
  const [saved, setSaved] = useState(false);
  const [savedKnown, setSavedKnown] = useState(false);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [showOffer, setShowOffer] = useState(false);
  const [showReport, setShowReport] = useState(false);
  const [showMessage, setShowMessage] = useState(false);

  // The server is the source of truth on first load; local state takes over
  // once the student has actually toggled it.
  const isSaved = savedKnown ? saved : (listing?.savedByViewer ?? false);

  if (loading) return <ListingSkeleton />;
  if (error) return <ErrorState message={error.message} onRetry={reload} />;
  if (!listing) return null;

  const isOwner = listing.ownedByViewer;
  const gone = listing.statusName === "COMPLETED" || listing.statusName === "REMOVED";

  async function act(action: () => Promise<unknown>, message: string) {
    setBusy(true);
    setNotice(null);
    try {
      await action();
      setNotice(message);
      reload();
    } catch (cause) {
      setNotice(cause instanceof ApiError ? cause.message : "That did not work. Try again.");
    } finally {
      setBusy(false);
    }
  }

  async function toggleSave() {
    if (!user) return navigate("/signin");
    setBusy(true);
    try {
      const result = await request<{ saved: boolean }>(`/listings/${listing!.id}/save`, { method: "POST" });
      setSaved(result.saved);
      setSavedKnown(true);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
      <nav className="mb-6 flex items-center gap-2 text-sm text-muted" aria-label="Breadcrumb">
        <Link to="/browse" className="cursor-pointer hover:text-ink">
          Browse
        </Link>
        <span aria-hidden="true">/</span>
        <Link
          to={`/browse?category=${listing.categorySlug}`}
          className="cursor-pointer hover:text-ink"
        >
          {listing.categoryLabel}
        </Link>
      </nav>

      <div className="grid gap-10 lg:grid-cols-[1.1fr_0.9fr]">
        {/* Gallery ---------------------------------------------------- */}
        <div>
          <div className="aspect-item overflow-hidden rounded-2xl bg-sunk shadow-[var(--shadow-inset-line)]">
            {listing.imageUrls.length > 0 ? (
              <img
                src={listing.imageUrls[activeImage]}
                alt={`${listing.title}, photo ${activeImage + 1} of ${listing.imageUrls.length}`}
                className="h-full w-full object-cover"
              />
            ) : (
              <div className="grid h-full place-items-center text-sm text-faint">No photo provided</div>
            )}
          </div>

          {listing.imageUrls.length > 1 && (
            <div className="mt-3 flex gap-2.5" role="tablist" aria-label="Photos">
              {listing.imageUrls.map((url, index) => (
                <button
                  key={url}
                  type="button"
                  role="tab"
                  aria-selected={index === activeImage}
                  onClick={() => setActiveImage(index)}
                  className={cx(
                    "h-16 w-20 shrink-0 cursor-pointer overflow-hidden rounded-lg transition-shadow duration-200",
                    index === activeImage
                      ? "shadow-[0_0_0_2px_var(--tu-ink)]"
                      : "opacity-70 hover:opacity-100"
                  )}
                >
                  <img src={url} alt="" className="h-full w-full object-cover" />
                </button>
              ))}
            </div>
          )}

          <section className="mt-9">
            <h2 className="font-display text-lg font-semibold">About this item</h2>
            <p className="mt-3 leading-relaxed whitespace-pre-line text-muted">{listing.description}</p>
          </section>

          <dl className="mt-8 grid grid-cols-2 gap-px overflow-hidden rounded-xl bg-line sm:grid-cols-4">
            <Fact label="Condition" value={listing.conditionLabel} hint={listing.conditionDescription} />
            <Fact label="Provenance" value={formatOwnerGeneration(listing.ownerGeneration)} />
            <Fact label="Listed" value={formatDate(listing.createdAt)} />
            <Fact label="Reference" value={listing.reference} mono />
          </dl>
        </div>

        {/* Buy box ---------------------------------------------------- */}
        <div className="lg:sticky lg:top-24 lg:h-fit">
          <div className="flex flex-wrap items-center gap-2">
            <Badge tone={listing.intentName === "SELL" ? "neutral" : "marigold"}>
              {listing.intentName !== "SELL" && <Repeat2 className="h-3 w-3" aria-hidden="true" />}
              {listing.intentLabel}
            </Badge>
            {listing.statusName === "RESERVED" && <Badge tone="indigo">Reserved</Badge>}
            {listing.statusName === "COMPLETED" && <Badge tone="leaf">Traded</Badge>}
            {listing.statusName === "FLAGGED" && <Badge tone="clay">Under review</Badge>}
          </div>

          <h1 className="mt-3 text-[clamp(1.6rem,3.4vw,2.35rem)]">{listing.title}</h1>

          <p className="tabular mt-4 font-display text-4xl leading-none font-bold text-ink">
            {formatPrice(listing.priceKobo)}
          </p>

          {listing.acceptsSwap && listing.swapWanted && (
            <div className="mt-4 rounded-xl bg-marigold-soft p-4">
              <p className="eyebrow">Would swap for</p>
              <p className="mt-1.5 text-sm leading-relaxed text-ink">{listing.swapWanted}</p>
            </div>
          )}

          <div className="mt-5 flex flex-wrap items-center gap-x-5 gap-y-2 text-sm text-muted">
            {listing.pickupLocation && (
              <span className="inline-flex items-center gap-1.5">
                <MapPin className="h-4 w-4" aria-hidden="true" />
                {listing.pickupLocation}
              </span>
            )}
            <span className="inline-flex items-center gap-1.5">
              <Eye className="h-4 w-4" aria-hidden="true" />
              {listing.viewCount} views
            </span>
            <span className="inline-flex items-center gap-1.5 text-leaf">
              <Leaf className="h-4 w-4" aria-hidden="true" />
              Saves {listing.co2SavedKg.toFixed(1)} kg CO₂e
            </span>
          </div>

          {notice && (
            <p role="status" className="mt-5 rounded-lg bg-leaf-soft px-4 py-3 text-sm text-leaf">
              {notice}
            </p>
          )}

          {/* Actions */}
          <div className="mt-6 flex flex-col gap-2.5">
            {isOwner ? (
              <OwnerControls listing={listing} busy={busy} act={act} navigate={navigate} />
            ) : gone ? (
              <p className="rounded-xl bg-sunk px-4 py-4 text-sm text-muted">
                This item has already found its next owner.
              </p>
            ) : (
              <>
                <Button onClick={() => (user ? setShowMessage(true) : navigate("/signin"))} full>
                  <MessageSquare className="h-4 w-4" aria-hidden="true" />
                  Message the lister
                </Button>
                <Button
                  tone="outline"
                  onClick={() => (user ? setShowOffer(true) : navigate("/signin"))}
                  full
                >
                  Make an offer
                </Button>
                <div className="flex gap-2.5">
                  <Button tone="quiet" onClick={toggleSave} busy={busy} className="flex-1">
                    <Bookmark className="h-4 w-4" fill={isSaved ? "currentColor" : "none"} aria-hidden="true" />
                    {isSaved ? "Saved" : "Save"}
                  </Button>
                  <Button
                    tone="quiet"
                    onClick={() => (user ? setShowReport(true) : navigate("/signin"))}
                    className="flex-1"
                  >
                    <AlertTriangle className="h-4 w-4" aria-hidden="true" />
                    Report
                  </Button>
                </div>
              </>
            )}
          </div>

          {/* Seller */}
          <Card className="mt-6">
            <p className="eyebrow">Listed by</p>
            <Link
              to={`/students/${listing.owner.id}`}
              className="mt-3 flex cursor-pointer items-center gap-3 transition-opacity duration-200 hover:opacity-80"
            >
              <Avatar initials={listing.owner.initials} size={44} />
              <span>
                <span className="block font-semibold text-ink">{listing.owner.fullName}</span>
                <span className="block text-xs text-muted">
                  {listing.owner.department}
                  {listing.owner.campusLocation ? ` · ${listing.owner.campusLocation}` : ""}
                </span>
              </span>
            </Link>
            {/* The card carries the seller's trade record. Their join date is not
                on this payload, so it is left to the profile page rather than
                inferred from when the listing happened to go up. */}
            <p className="mt-3 border-t border-line pt-3 text-xs text-muted">
              {listing.owner.completedTrades === 0
                ? "No completed trades yet"
                : `${listing.owner.completedTrades} completed trade${listing.owner.completedTrades === 1 ? "" : "s"}`}
              {" · listed "}
              {timeAgo(listing.createdAt)}
            </p>
          </Card>
        </div>
      </div>

      {similar && similar.length > 0 && (
        <section className="mt-20">
          <h2 className="text-[clamp(1.4rem,2.6vw,1.9rem)]">More in {listing.categoryLabel}</h2>
          <div className="mt-6 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {similar.map((other) => (
              <TradeTicket key={other.id} listing={other} />
            ))}
          </div>
        </section>
      )}

      {showMessage && (
        <MessageModal listing={listing} onClose={() => setShowMessage(false)} />
      )}
      {showOffer && <OfferModal listing={listing} onClose={() => setShowOffer(false)} onDone={reload} />}
      {showReport && <ReportModal listingId={listing.id} onClose={() => setShowReport(false)} />}
    </div>
  );
}

/* -------------------------------------------------------------------------
   Owner controls
   ------------------------------------------------------------------------- */

function OwnerControls({
  listing,
  busy,
  act,
  navigate,
}: {
  listing: ListingDetail;
  busy: boolean;
  act: (action: () => Promise<unknown>, message: string) => Promise<void>;
  navigate: ReturnType<typeof useNavigate>;
}) {
  const status = listing.statusName;

  return (
    <>
      <p className="rounded-xl bg-indigo-soft px-4 py-3 text-sm text-indigo">
        This is your listing.
        {listing.pendingOfferCount > 0 &&
          ` ${listing.pendingOfferCount} offer${listing.pendingOfferCount === 1 ? "" : "s"} waiting on you.`}
      </p>

      {status !== "COMPLETED" && status !== "REMOVED" && (
        <>
          <LinkButton to={`/listings/${listing.id}/edit`} tone="outline" full>
            <Pencil className="h-4 w-4" aria-hidden="true" />
            Edit listing
          </LinkButton>

          {status === "ACTIVE" && (
            <Button
              tone="outline"
              busy={busy}
              full
              onClick={() =>
                act(
                  () => request(`/listings/${listing.id}/reserve`, { method: "POST" }),
                  "Held. It will not show as available while it is reserved."
                )
              }
            >
              Hold for someone
            </Button>
          )}

          {status === "RESERVED" && (
            <Button
              tone="outline"
              busy={busy}
              full
              onClick={() =>
                act(
                  () => request(`/listings/${listing.id}/release`, { method: "POST" }),
                  "Back on the board."
                )
              }
            >
              Put back on the board
            </Button>
          )}

          <Button
            busy={busy}
            full
            onClick={() =>
              act(
                () => request(`/listings/${listing.id}/complete`, { method: "POST" }),
                "Marked as traded. That is one more item kept in use."
              )
            }
          >
            <Check className="h-4 w-4" aria-hidden="true" />
            Mark as traded
          </Button>

          <Button
            tone="quiet"
            busy={busy}
            full
            onClick={() => {
              if (!confirm("Take this listing down? Anyone you are already talking to keeps the thread.")) return;
              void act(async () => {
                await request(`/listings/${listing.id}`, { method: "DELETE" });
                navigate("/dashboard");
              }, "Taken down.");
            }}
          >
            <Trash2 className="h-4 w-4" aria-hidden="true" />
            Take it down
          </Button>
        </>
      )}
    </>
  );
}

/* -------------------------------------------------------------------------
   Modals
   ------------------------------------------------------------------------- */

function MessageModal({ listing, onClose }: { listing: ListingDetail; onClose: () => void }) {
  const navigate = useNavigate();
  const [body, setBody] = useState(`Hi, is "${listing.title}" still available?`);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function send(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const conversation = await request<{ id: number }>(`/conversations/listing/${listing.id}`, {
        method: "POST",
        body: { body },
      });
      navigate(`/messages/${conversation.id}`);
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : "Could not send that.");
      setBusy(false);
    }
  }

  return (
    <Modal title={`Message ${listing.owner.fullName}`} onClose={onClose}>
      <form onSubmit={send} className="flex flex-col gap-4">
        <Field label="Your message" htmlFor="message-body" error={error ?? undefined}>
          <TextArea
            id="message-body"
            rows={4}
            value={body}
            onChange={(event) => setBody(event.target.value)}
            maxLength={2000}
            required
            autoFocus
          />
        </Field>
        <div className="flex justify-end gap-2.5">
          <Button type="button" tone="quiet" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" busy={busy}>
            Send message
          </Button>
        </div>
      </form>
    </Modal>
  );
}

function OfferModal({
  listing,
  onClose,
  onDone,
}: {
  listing: ListingDetail;
  onClose: () => void;
  onDone: () => void;
}) {
  const [kind, setKind] = useState<"CASH" | "SWAP">(listing.acceptsCash ? "CASH" : "SWAP");
  const [amount, setAmount] = useState("");
  const [offeredListingId, setOfferedListingId] = useState("");
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  // Only the student's own available items can be put up in a swap.
  const { data: mine } = useApi<{ items: ListingCard[] }>(kind === "SWAP" ? "/me/listings?size=48" : null);
  const swappable = (mine?.items ?? []).filter(
    (item) => item.statusName === "ACTIVE" && item.id !== listing.id
  );

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await request<OfferView>(`/listings/${listing.id}/offers`, {
        method: "POST",
        body: {
          kind,
          amountKobo: kind === "CASH" ? Math.round(Number(amount) * 100) : null,
          offeredListingId: kind === "SWAP" ? Number(offeredListingId) : null,
          note: note || null,
        },
      });
      setDone(true);
      onDone();
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : "Could not send that offer.");
    } finally {
      setBusy(false);
    }
  }

  if (done) {
    return (
      <Modal title="Offer sent" onClose={onClose}>
        <p className="text-sm leading-relaxed text-muted">
          The lister will see it on their dashboard. You can track it under the offers you have made.
        </p>
        <div className="mt-6 flex justify-end">
          <Button onClick={onClose}>Done</Button>
        </div>
      </Modal>
    );
  }

  return (
    <Modal title="Make an offer" onClose={onClose}>
      <form onSubmit={submit} className="flex flex-col gap-4">
        {listing.acceptsCash && listing.acceptsSwap && (
          <div className="flex gap-2">
            {(["CASH", "SWAP"] as const).map((option) => (
              <button
                key={option}
                type="button"
                onClick={() => setKind(option)}
                aria-pressed={kind === option}
                className={cx(
                  "flex-1 cursor-pointer rounded-lg px-3 py-2.5 text-sm font-semibold transition-colors duration-200",
                  kind === option ? "bg-ink text-paper" : "bg-sunk text-muted hover:text-ink"
                )}
              >
                {option === "CASH" ? "Offer cash" : "Offer a swap"}
              </button>
            ))}
          </div>
        )}

        {kind === "CASH" ? (
          <Field
            label="How much are you offering?"
            htmlFor="offer-amount"
            hint={`Asking price is ${formatPrice(listing.priceKobo)}.`}
            required
          >
            <TextInput
              id="offer-amount"
              type="number"
              inputMode="numeric"
              min={1}
              value={amount}
              onChange={(event) => setAmount(event.target.value)}
              placeholder="₦"
              required
              autoFocus
              className="tabular"
            />
          </Field>
        ) : (
          <Field
            label="Which of your items are you offering?"
            htmlFor="offer-listing"
            hint={
              swappable.length === 0 ? "You need an available listing of your own to offer a swap." : undefined
            }
            required
          >
            <Select
              id="offer-listing"
              value={offeredListingId}
              onChange={(event) => setOfferedListingId(event.target.value)}
              required
            >
              <option value="">Choose an item</option>
              {swappable.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.title}
                </option>
              ))}
            </Select>
          </Field>
        )}

        <Field label="Add a note" htmlFor="offer-note" hint="Optional.">
          <TextArea
            id="offer-note"
            rows={3}
            value={note}
            onChange={(event) => setNote(event.target.value)}
            maxLength={500}
            placeholder="When you could meet, anything else worth saying."
          />
        </Field>

        {error && (
          <p role="alert" className="rounded-lg bg-clay-soft px-3.5 py-2.5 text-sm text-clay">
            {error}
          </p>
        )}

        <div className="flex justify-end gap-2.5">
          <Button type="button" tone="quiet" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" busy={busy} disabled={kind === "SWAP" && swappable.length === 0}>
            Send offer
          </Button>
        </div>
      </form>
    </Modal>
  );
}

const REPORT_REASONS = [
  { value: "SPAM", label: "Spam or repeated posting" },
  { value: "PROHIBITED", label: "Prohibited item" },
  { value: "MISLEADING", label: "Misleading description or photos" },
  { value: "OFFENSIVE", label: "Offensive content" },
  { value: "SCAM", label: "Suspected scam" },
  { value: "OTHER", label: "Something else" },
];

function ReportModal({ listingId, onClose }: { listingId: number; onClose: () => void }) {
  const [reason, setReason] = useState("MISLEADING");
  const [details, setDetails] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await request(`/listings/${listingId}/report`, { method: "POST", body: { reason, details } });
      setDone(true);
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : "Could not send that report.");
    } finally {
      setBusy(false);
    }
  }

  if (done) {
    return (
      <Modal title="Report sent" onClose={onClose}>
        <p className="text-sm leading-relaxed text-muted">
          A moderator will look at this listing. Thanks for keeping the board usable.
        </p>
        <div className="mt-6 flex justify-end">
          <Button onClick={onClose}>Done</Button>
        </div>
      </Modal>
    );
  }

  return (
    <Modal title="Report this listing" onClose={onClose}>
      <form onSubmit={submit} className="flex flex-col gap-4">
        <Field label="What is wrong with it?" htmlFor="report-reason" required>
          <Select id="report-reason" value={reason} onChange={(event) => setReason(event.target.value)}>
            {REPORT_REASONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </Select>
        </Field>

        <Field label="Anything else a moderator should know?" htmlFor="report-details" hint="Optional.">
          <TextArea
            id="report-details"
            rows={3}
            value={details}
            onChange={(event) => setDetails(event.target.value)}
            maxLength={700}
          />
        </Field>

        {error && (
          <p role="alert" className="rounded-lg bg-clay-soft px-3.5 py-2.5 text-sm text-clay">
            {error}
          </p>
        )}

        <div className="flex justify-end gap-2.5">
          <Button type="button" tone="quiet" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" busy={busy}>
            Send report
          </Button>
        </div>
      </form>
    </Modal>
  );
}

/* -------------------------------------------------------------------------
   Bits
   ------------------------------------------------------------------------- */

function Fact({
  label,
  value,
  hint,
  mono,
}: {
  label: string;
  value: string;
  hint?: string;
  mono?: boolean;
}) {
  return (
    <div className="bg-surface p-4">
      <dt className="eyebrow">{label}</dt>
      <dd className={cx("mt-1.5 text-sm font-semibold text-ink", mono && "font-mono")}>{value}</dd>
      {hint && <p className="mt-1 text-xs leading-snug text-muted">{hint}</p>}
    </div>
  );
}

function ListingSkeleton() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
      <div className="grid gap-10 lg:grid-cols-[1.1fr_0.9fr]">
        <Skeleton className="aspect-item w-full rounded-2xl" />
        <div className="flex flex-col gap-4">
          <Skeleton className="h-5 w-28" />
          <Skeleton className="h-10 w-3/4" />
          <Skeleton className="h-9 w-40" />
          <Skeleton className="mt-4 h-11 w-full" />
          <Skeleton className="h-11 w-full" />
          <Skeleton className="mt-6 h-28 w-full rounded-xl" />
        </div>
      </div>
    </div>
  );
}
