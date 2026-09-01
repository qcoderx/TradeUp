import { Link } from "react-router-dom";
import { MessagesSquare } from "lucide-react";
import { assetUrl } from "../lib/api";
import { useApi } from "../lib/useApi";
import { timeAgo } from "../lib/format";
import type { ConversationSummary } from "../lib/types";
import { Avatar, EmptyState, ErrorState, LinkButton, Skeleton } from "../components/ui";

export function Inbox() {
  const { data, error, loading, reload } = useApi<ConversationSummary[]>("/conversations");

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <p className="eyebrow">Messages</p>
      <h1 className="mt-2 text-[clamp(1.9rem,4vw,2.75rem)]">Your conversations</h1>

      <div className="mt-8">
        {error ? (
          <ErrorState message={error.message} onRetry={reload} />
        ) : loading ? (
          <div className="flex flex-col gap-2">
            {Array.from({ length: 4 }, (_, index) => (
              <Skeleton key={index} className="h-24 w-full rounded-xl" />
            ))}
          </div>
        ) : data && data.length === 0 ? (
          <EmptyState
            icon={<MessagesSquare className="h-8 w-8" aria-hidden="true" />}
            title="No conversations yet"
            description="When you message someone about an item, or they message you, the thread shows up here."
            action={<LinkButton to="/browse">Find something</LinkButton>}
          />
        ) : (
          <ul className="flex flex-col gap-2">
            {data?.map((conversation) => (
              <li key={conversation.id}>
                <Link
                  to={`/messages/${conversation.id}`}
                  className="flex cursor-pointer items-center gap-4 rounded-xl bg-surface p-4 shadow-[var(--shadow-inset-line)] transition-colors duration-200 hover:bg-sunk"
                >
                  {conversation.listingImageUrl ? (
                    <img
                      src={assetUrl(conversation.listingImageUrl)}
                      alt=""
                      className="h-14 w-14 shrink-0 rounded-lg object-cover"
                      loading="lazy"
                    />
                  ) : (
                    <Avatar initials={conversation.counterpart.initials} size={56} />
                  )}

                  <div className="min-w-0 flex-1">
                    <div className="flex items-baseline justify-between gap-3">
                      <p className="truncate font-semibold text-ink">{conversation.counterpart.fullName}</p>
                      <span className="shrink-0 text-xs text-faint">
                        {timeAgo(conversation.lastMessageAt)}
                      </span>
                    </div>

                    <p className="truncate text-xs text-muted">{conversation.listingTitle}</p>

                    {conversation.lastMessagePreview && (
                      <p className="mt-1 truncate text-sm text-muted">{conversation.lastMessagePreview}</p>
                    )}
                  </div>

                  {conversation.unreadCount > 0 && (
                    <span
                      className="grid h-6 min-w-6 shrink-0 place-items-center rounded-full bg-clay px-1.5 font-mono text-[0.625rem] font-bold text-white"
                      aria-label={`${conversation.unreadCount} unread`}
                    >
                      {conversation.unreadCount}
                    </span>
                  )}
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
