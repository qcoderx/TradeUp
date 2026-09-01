import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Send } from "lucide-react";
import { ApiError, assetUrl, request } from "../lib/api";
import { useApi } from "../lib/useApi";
import { cx, formatPrice, formatTime, timeAgo } from "../lib/format";
import type { ConversationDetail, MessageView } from "../lib/types";
import { Avatar, Button, ErrorState, Skeleton } from "../components/ui";

export function Thread() {
  const { id } = useParams<{ id: string }>();
  const { data, error, loading, reload } = useApi<ConversationDetail>(id ? `/conversations/${id}` : null);

  const [draft, setDraft] = useState("");
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState<string | null>(null);
  const [pending, setPending] = useState<MessageView[]>([]);

  const endRef = useRef<HTMLDivElement>(null);

  const messages = [...(data?.messages ?? []), ...pending];

  // Land at the newest message, the way a chat should open.
  useEffect(() => {
    endRef.current?.scrollIntoView({ block: "end" });
  }, [messages.length]);

  async function send(event: React.FormEvent) {
    event.preventDefault();
    const body = draft.trim();
    if (!body || sending) return;

    setSending(true);
    setSendError(null);
    setDraft("");

    try {
      const sent = await request<MessageView>(`/conversations/${id}/messages`, {
        method: "POST",
        body: { body },
      });
      // Append locally so the thread does not blink while it refetches.
      setPending((current) => [...current, sent]);
    } catch (cause) {
      setSendError(cause instanceof ApiError ? cause.message : "That message did not send.");
      setDraft(body); // Give the text back rather than losing it.
    } finally {
      setSending(false);
    }
  }

  if (error) return <ErrorState message={error.message} onRetry={reload} />;

  if (loading || !data) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-10 sm:px-6">
        <Skeleton className="h-20 w-full rounded-xl" />
        <div className="mt-6 flex flex-col gap-3">
          {Array.from({ length: 5 }, (_, index) => (
            <Skeleton key={index} className={cx("h-12 rounded-2xl", index % 2 ? "w-2/3 self-end" : "w-1/2")} />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto flex min-h-[calc(100dvh-4rem)] max-w-2xl flex-col px-4 py-6 sm:px-6">
      <Link
        to="/messages"
        className="mb-4 inline-flex w-fit cursor-pointer items-center gap-1.5 text-sm text-muted transition-colors duration-200 hover:text-ink"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        All messages
      </Link>

      {/* What the conversation is about, kept in view. */}
      <Link
        to={`/listings/${data.listing.id}`}
        className="flex cursor-pointer items-center gap-4 rounded-xl bg-surface p-4 shadow-[var(--shadow-inset-line)] transition-colors duration-200 hover:bg-sunk"
      >
        {data.listing.primaryImageUrl && (
          <img
            src={assetUrl(data.listing.primaryImageUrl)}
            alt=""
            className="h-14 w-14 shrink-0 rounded-lg object-cover"
          />
        )}
        <div className="min-w-0 flex-1">
          <p className="truncate font-semibold text-ink">{data.listing.title}</p>
          <p className="tabular text-sm text-muted">{formatPrice(data.listing.priceKobo)}</p>
        </div>
        <span className="shrink-0 text-xs text-faint">{data.listing.statusLabel}</span>
      </Link>

      <div className="mt-4 mb-2 flex items-center gap-2.5 border-b border-line pb-4">
        <Avatar initials={data.counterpart.initials} size={32} />
        <div>
          <p className="text-sm font-semibold text-ink">{data.counterpart.fullName}</p>
          <p className="text-xs text-muted">
            {data.counterpart.department}
            {data.counterpart.campusLocation ? ` · ${data.counterpart.campusLocation}` : ""}
          </p>
        </div>
      </div>

      {/* Messages */}
      <div className="flex flex-1 flex-col gap-2.5 overflow-y-auto py-4">
        {messages.map((message) => (
          <div
            key={message.id}
            className={cx("flex max-w-[80%] flex-col gap-1", message.mine ? "self-end items-end" : "self-start")}
          >
            <div
              className={cx(
                "rounded-2xl px-4 py-2.5 text-sm leading-relaxed",
                message.mine
                  ? "rounded-br-sm bg-ink text-paper"
                  : "rounded-bl-sm bg-surface text-ink shadow-[var(--shadow-inset-line)]"
              )}
            >
              {message.body}
            </div>
            <span className="px-1 text-[0.6875rem] text-faint">
              {formatTime(message.sentAt)} · {timeAgo(message.sentAt)}
            </span>
          </div>
        ))}
        <div ref={endRef} />
      </div>

      {/* Composer */}
      <form onSubmit={send} className="sticky bottom-0 flex flex-col gap-2 bg-paper pt-3 pb-4">
        {sendError && (
          <p role="alert" className="rounded-lg bg-clay-soft px-3.5 py-2 text-sm text-clay">
            {sendError}
          </p>
        )}
        <div className="flex items-end gap-2">
          <label htmlFor="draft" className="sr-only">
            Your message
          </label>
          <textarea
            id="draft"
            rows={1}
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            onKeyDown={(event) => {
              // Enter sends; Shift+Enter is a new line.
              if (event.key === "Enter" && !event.shiftKey) {
                event.preventDefault();
                void send(event as unknown as React.FormEvent);
              }
            }}
            placeholder="Write a message…"
            maxLength={2000}
            className="field max-h-32 min-h-11 flex-1 resize-none py-2.5"
          />
          <Button type="submit" busy={sending} disabled={!draft.trim()} aria-label="Send message">
            <Send className="h-4 w-4" aria-hidden="true" />
          </Button>
        </div>
      </form>
    </div>
  );
}
