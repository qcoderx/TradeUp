import { useCallback, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { SearchX, SlidersHorizontal, X } from "lucide-react";
import { query } from "../lib/api";
import { useApi } from "../lib/useApi";
import { cx } from "../lib/format";
import type { ListingCard, PageResponse, ReferenceData } from "../lib/types";
import { TradeTicket, TradeTicketSkeleton } from "../components/TradeTicket";
import { Button, EmptyState, ErrorState, LinkButton, Select } from "../components/ui";

const SORTS = [
  { value: "NEWEST", label: "Newest first" },
  { value: "PRICE_LOW", label: "Price: low to high" },
  { value: "PRICE_HIGH", label: "Price: high to low" },
  { value: "POPULAR", label: "Most viewed" },
  { value: "BEST_CONDITION", label: "Best condition" },
];

const PAGE_SIZE = 24;

/**
 * The browse screen.
 *
 * Filter state lives in the URL rather than in component state, so a filtered
 * view can be shared, bookmarked, and reached with the back button — which is
 * how people actually use a marketplace.
 */
export function Browse() {
  const [params, setParams] = useSearchParams();
  const [filtersOpen, setFiltersOpen] = useState(false);

  const { data: reference } = useApi<ReferenceData>("/reference");

  const q = params.get("q") ?? "";
  const category = params.get("category") ?? "";
  const intent = params.get("intent") ?? "";
  const conditions = params.getAll("condition");
  const minPrice = params.get("minPrice") ?? "";
  const maxPrice = params.get("maxPrice") ?? "";
  const sort = params.get("sort") ?? "NEWEST";
  const page = Number(params.get("page") ?? "0");

  const path = useMemo(
    () =>
      `/listings${query({
        q,
        category,
        intent,
        condition: conditions,
        minPrice,
        maxPrice,
        sort,
        page,
        size: PAGE_SIZE,
      })}`,
    // conditions is a fresh array each render, so compare its contents.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [q, category, intent, conditions.join(","), minPrice, maxPrice, sort, page]
  );

  const { data, error, loading, reload } = useApi<PageResponse<ListingCard>>(path);

  /** Writes one filter into the URL and returns to the first page. */
  const update = useCallback(
    (key: string, value: string | string[] | null) => {
      const next = new URLSearchParams(params);
      next.delete(key);
      if (Array.isArray(value)) value.forEach((entry) => next.append(key, entry));
      else if (value) next.set(key, value);
      if (key !== "page") next.delete("page");
      setParams(next, { replace: true });
    },
    [params, setParams]
  );

  const clearAll = useCallback(() => setParams(new URLSearchParams(), { replace: true }), [setParams]);

  const activeCount =
    (category ? 1 : 0) + (intent ? 1 : 0) + conditions.length + (minPrice ? 1 : 0) + (maxPrice ? 1 : 0);

  // Changing a filter should bring the results back into view on mobile.
  useEffect(() => {
    if (page > 0) window.scrollTo({ top: 0, behavior: "smooth" });
  }, [page]);

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6">
      <header className="mb-8">
        <p className="eyebrow">The board</p>
        <h1 className="mt-2 text-[clamp(1.9rem,4vw,2.75rem)]">
          {q ? (
            <>
              Results for <span className="text-green">{q}</span>
            </>
          ) : (
            "Everything on offer"
          )}
        </h1>
        {data && (
          <p className="mt-2 text-sm text-muted">
            {data.totalItems === 0
              ? "Nothing matches yet"
              : `${data.totalItems} item${data.totalItems === 1 ? "" : "s"} available`}
          </p>
        )}
      </header>

      <div className="grid gap-8 lg:grid-cols-[16rem_1fr]">
        {/* Filters ---------------------------------------------------- */}
        <aside
          id="filters"
          className={cx(
            "lg:sticky lg:top-24 lg:block lg:h-fit",
            filtersOpen ? "block" : "hidden"
          )}
        >
          <div className="flex flex-col gap-6 rounded-xl bg-surface p-5 shadow-[var(--shadow-inset-line)] lg:bg-transparent lg:p-0 lg:shadow-none">
            <FilterGroup label="Category">
              <Select
                id="filter-category"
                value={category}
                onChange={(event) => update("category", event.target.value || null)}
              >
                <option value="">All categories</option>
                {reference?.categories.map((option) => (
                  <option key={option.slug} value={option.slug}>
                    {option.label} ({option.availableCount})
                  </option>
                ))}
              </Select>
            </FilterGroup>

            <FilterGroup label="Sale or swap">
              <div className="flex flex-wrap gap-2">
                {[
                  { value: "", label: "Either" },
                  { value: "SELL", label: "For sale" },
                  { value: "SWAP", label: "For swap" },
                ].map((option) => (
                  <Chip
                    key={option.value || "any"}
                    active={intent === option.value}
                    onClick={() => update("intent", option.value || null)}
                  >
                    {option.label}
                  </Chip>
                ))}
              </div>
            </FilterGroup>

            <FilterGroup label="Condition">
              <div className="flex flex-col gap-1.5">
                {reference?.conditions.map((option) => {
                  const checked = conditions.includes(option.name);
                  return (
                    <label
                      key={option.name}
                      className="flex cursor-pointer items-center gap-2.5 rounded-lg px-1 py-1.5 text-sm text-muted transition-colors duration-200 hover:text-ink"
                    >
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() =>
                          update(
                            "condition",
                            checked
                              ? conditions.filter((entry) => entry !== option.name)
                              : [...conditions, option.name]
                          )
                        }
                        className="h-4 w-4 cursor-pointer accent-[var(--tu-green)]"
                      />
                      {option.label}
                    </label>
                  );
                })}
              </div>
            </FilterGroup>

            <FilterGroup label="Price (₦)">
              <div className="flex items-center gap-2">
                <input
                  type="number"
                  inputMode="numeric"
                  min={0}
                  placeholder="Min"
                  defaultValue={minPrice}
                  onBlur={(event) => update("minPrice", event.target.value || null)}
                  aria-label="Lowest price in naira"
                  className="field tabular"
                />
                <span className="text-faint" aria-hidden="true">
                  –
                </span>
                <input
                  type="number"
                  inputMode="numeric"
                  min={0}
                  placeholder="Max"
                  defaultValue={maxPrice}
                  onBlur={(event) => update("maxPrice", event.target.value || null)}
                  aria-label="Highest price in naira"
                  className="field tabular"
                />
              </div>
              <p className="mt-1.5 text-xs text-muted">Swap-only items are always included.</p>
            </FilterGroup>

            {activeCount > 0 && (
              <Button tone="quiet" onClick={clearAll} className="justify-start px-1">
                <X className="h-4 w-4" aria-hidden="true" />
                Clear {activeCount} filter{activeCount === 1 ? "" : "s"}
              </Button>
            )}
          </div>
        </aside>

        {/* Results ---------------------------------------------------- */}
        <div>
          <div className="mb-5 flex items-center justify-between gap-3">
            <Button
              tone="outline"
              onClick={() => setFiltersOpen((open) => !open)}
              aria-expanded={filtersOpen}
              aria-controls="filters"
              className="lg:hidden"
            >
              <SlidersHorizontal className="h-4 w-4" aria-hidden="true" />
              Filters
              {activeCount > 0 && (
                <span className="ml-0.5 rounded-full bg-green px-1.5 font-mono text-[0.625rem] text-white">
                  {activeCount}
                </span>
              )}
            </Button>

            <div className="ml-auto flex items-center gap-2">
              <label htmlFor="sort" className="text-sm whitespace-nowrap text-muted">
                Sort
              </label>
              <Select
                id="sort"
                value={sort}
                onChange={(event) => update("sort", event.target.value)}
                className="w-auto min-w-44"
              >
                {SORTS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </Select>
            </div>
          </div>

          {error ? (
            <ErrorState message={error.message} onRetry={reload} />
          ) : loading ? (
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3">
              {Array.from({ length: 9 }, (_, index) => (
                <TradeTicketSkeleton key={index} />
              ))}
            </div>
          ) : data && data.items.length === 0 ? (
            <EmptyState
              icon={<SearchX className="h-8 w-8" aria-hidden="true" />}
              title="Nothing matches that yet"
              description={
                activeCount > 0 || q
                  ? "Try loosening a filter, or search for something broader. New items go up every week."
                  : "The board is empty right now. Be the first to list something."
              }
              action={
                activeCount > 0 || q ? (
                  <Button tone="outline" onClick={clearAll}>
                    Clear filters
                  </Button>
                ) : (
                  <LinkButton to="/sell">List an item</LinkButton>
                )
              }
            />
          ) : (
            <>
              <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3">
                {data?.items.map((listing, index) => (
                  <TradeTicket key={listing.id} listing={listing} priority={index < 6} />
                ))}
              </div>

              {data && data.totalPages > 1 && (
                <nav className="mt-10 flex items-center justify-center gap-3" aria-label="Pagination">
                  <Button
                    tone="outline"
                    disabled={!data.hasPrevious}
                    onClick={() => update("page", String(page - 1))}
                  >
                    Previous
                  </Button>
                  <span className="tabular px-2 text-sm text-muted">
                    Page {data.page + 1} of {data.totalPages}
                  </span>
                  <Button
                    tone="outline"
                    disabled={!data.hasNext}
                    onClick={() => update("page", String(page + 1))}
                  >
                    Next
                  </Button>
                </nav>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function FilterGroup({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <h2 className="eyebrow mb-2.5">{label}</h2>
      {children}
    </div>
  );
}

function Chip({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={cx(
        "cursor-pointer rounded-full px-3 py-1.5 text-xs font-semibold transition-colors duration-200",
        active
          ? "bg-ink text-paper"
          : "bg-sunk text-muted hover:bg-line hover:text-ink"
      )}
    >
      {children}
    </button>
  );
}
