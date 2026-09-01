import { Link } from "react-router-dom";
import { Leaf, Recycle, Users, Wallet } from "lucide-react";
import { useApi } from "../lib/useApi";
import { formatCo2, formatPrice, formatWeight } from "../lib/format";
import type { CategoryImpact, ImpactSnapshot } from "../lib/types";
import { ErrorState, LinkButton, Skeleton } from "../components/ui";

export function ImpactPage() {
  const { data, error, loading, reload } = useApi<ImpactSnapshot>("/impact");

  return (
    <>
      <section className="hero-panel">
        <div className="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:py-24">
          <p className="font-mono text-[0.6875rem] tracking-[0.16em] text-green uppercase">
            Sustainable Development Goal 12
          </p>
          <h1 className="mt-5 max-w-3xl text-[clamp(2.2rem,5.4vw,3.5rem)] leading-[1.02]">
            Responsible consumption, counted one item at a time
          </h1>
          <p className="mt-6 max-w-2xl text-[1.0625rem] leading-relaxed text-muted">
            Reuse is usually invisible. Nothing gets weighed and nobody is told what was avoided. These
            figures are the opposite: each one is worked out on the server from the trades that actually
            completed on this board.
          </p>
        </div>
      </section>

      <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6">
        {error ? (
          <ErrorState message={error.message} onRetry={reload} />
        ) : loading || !data ? (
          <div className="grid gap-px overflow-hidden rounded-2xl bg-line sm:grid-cols-2 lg:grid-cols-4">
            {Array.from({ length: 4 }, (_, index) => (
              <div key={index} className="bg-surface p-7">
                <Skeleton className="h-9 w-28" />
                <Skeleton className="mt-3 h-3 w-36" />
              </div>
            ))}
          </div>
        ) : (
          <>
            {/* Headline figures. A stat tile, not a chart: these are single
                numbers whose job is to be read, not compared. */}
            <dl className="grid gap-px overflow-hidden rounded-2xl bg-line sm:grid-cols-2 lg:grid-cols-4">
              <StatTile
                icon={<Leaf className="h-5 w-5" aria-hidden="true" />}
                value={formatCo2(data.co2SavedKg)}
                label="CO₂e kept out of the air"
                note="Estimated from each item's category and condition."
              />
              <StatTile
                icon={<Recycle className="h-5 w-5" aria-hidden="true" />}
                value={formatWeight(data.wasteDivertedKg)}
                label="diverted from waste"
                note={`Across ${data.itemsRehomed} completed trade${data.itemsRehomed === 1 ? "" : "s"}.`}
              />
              <StatTile
                icon={<Wallet className="h-5 w-5" aria-hidden="true" />}
                value={formatPrice(data.moneyKeptInPocketsKobo)}
                label="kept in student pockets"
                note="Total value of items bought second-hand."
              />
              <StatTile
                icon={<Users className="h-5 w-5" aria-hidden="true" />}
                value={String(data.studentsRegistered)}
                label="students on the board"
                note={`${data.itemsAvailableNow} items available right now.`}
              />
            </dl>

            <CategoryChart rows={data.byCategory} />

            {/* Method ------------------------------------------------------ */}
            <section className="mt-20 grid gap-10 lg:grid-cols-[0.85fr_1.15fr]">
              <div>
                <p className="eyebrow">How this is worked out</p>
                <h2 className="mt-2 text-[clamp(1.6rem,3vw,2.25rem)]">No number here is decorative</h2>
              </div>
              <div className="flex flex-col gap-5 text-[0.9375rem] leading-relaxed text-muted">
                <p>
                  Each category carries two constants: the average mass of an item in it, and the CO₂e
                  avoided when one is reused instead of bought new. A completed trade contributes its
                  category's figure, discounted for condition — a well-used item displaces less new
                  production than a nearly new one.
                </p>
                <p>
                  These are estimates, and they are deliberately conservative. What matters for the
                  project is that the arithmetic is real and visible: it runs in{" "}
                  <code className="font-mono text-ink">ImpactService.java</code> over the trades in the
                  database, and it changes the moment somebody marks an item as handed over.
                </p>
                <p className="text-ink">
                  Nothing counts until a trade is actually completed. Listing something does not move
                  these numbers.
                </p>
              </div>
            </section>

            <div className="mt-16 rounded-2xl bg-sunk px-8 py-12 text-center">
              <h2 className="mx-auto max-w-xl text-[clamp(1.5rem,3vw,2.1rem)]">
                Every one of these came from a student deciding not to bin something
              </h2>
              <div className="mt-7 flex flex-wrap justify-center gap-3">
                <LinkButton to="/sell">List an item</LinkButton>
                <LinkButton to="/team" tone="outline">
                  Meet the group behind it
                </LinkButton>
              </div>
            </div>
          </>
        )}
      </div>
    </>
  );
}

function StatTile({
  icon,
  value,
  label,
  note,
}: {
  icon: React.ReactNode;
  value: string;
  label: string;
  note: string;
}) {
  return (
    <div className="bg-surface p-7">
      <span className="inline-flex text-green">{icon}</span>
      <dd className="tabular mt-3 font-display text-[2rem] leading-none font-bold text-ink">{value}</dd>
      <dt className="mt-2 text-sm font-medium text-ink">{label}</dt>
      <p className="mt-1.5 text-xs leading-snug text-muted">{note}</p>
    </div>
  );
}

/**
 * CO₂e avoided, by category.
 *
 * One measure across several categories, so identity is carried by the row
 * labels and every bar takes the same hue. Giving each category its own colour
 * would encode what the labels already say, and would imply the categories are
 * a palette rather than a scale.
 *
 * Values are direct-labelled at the end of each bar, which removes the need for
 * an x-axis and its gridlines entirely.
 */
function CategoryChart({ rows }: { rows: CategoryImpact[] }) {
  if (rows.length === 0) {
    return (
      <section className="mt-16">
        <h2 className="text-[clamp(1.4rem,2.6vw,1.9rem)]">Where the savings come from</h2>
        <p className="mt-6 rounded-xl border border-dashed border-line-strong px-6 py-12 text-center text-sm text-muted">
          No trades have been completed yet. The moment one is, this fills in.
        </p>
      </section>
    );
  }

  const sorted = [...rows].sort((a, b) => b.co2SavedKg - a.co2SavedKg);
  const max = Math.max(...sorted.map((row) => row.co2SavedKg));

  return (
    <section className="mt-16">
      <h2 className="text-[clamp(1.4rem,2.6vw,1.9rem)]">CO₂e avoided, by category</h2>
      <p className="mt-2 text-sm text-muted">
        Kilograms of CO₂e kept out of the atmosphere by completed trades.
      </p>

      <div className="mt-8 rounded-2xl bg-surface p-6 shadow-[var(--shadow-inset-line)] sm:p-8">
        <ul className="flex flex-col gap-5">
          {sorted.map((row) => {
            const share = max === 0 ? 0 : (row.co2SavedKg / max) * 100;
            return (
              <li key={row.name}>
                <div className="flex items-baseline justify-between gap-4">
                  <Link
                    to={`/browse?category=${row.slug}`}
                    className="cursor-pointer text-sm font-medium text-ink transition-colors duration-200 hover:text-green"
                  >
                    {row.label}
                  </Link>
                  <span className="tabular shrink-0 font-mono text-xs text-muted">
                    {row.co2SavedKg.toFixed(1)} kg
                    <span className="ml-2 text-faint">
                      {row.itemsRehomed} item{row.itemsRehomed === 1 ? "" : "s"}
                    </span>
                  </span>
                </div>

                {/* The track is the sunk surface, so an empty bar still reads
                    as a measurable zero rather than as missing data. */}
                <div
                  className="mt-2 h-2.5 w-full overflow-hidden rounded-full bg-sunk"
                  role="img"
                  aria-label={`${row.label}: ${row.co2SavedKg.toFixed(1)} kilograms of CO2e from ${row.itemsRehomed} items`}
                >
                  <div
                    className="h-full rounded-full bg-green-bright transition-[width] duration-500 ease-out"
                    style={{ width: `${Math.max(share, 1.5)}%` }}
                  />
                </div>
              </li>
            );
          })}
        </ul>

        {/* The same data as a table, for anyone who cannot use the bars. */}
        <details className="mt-8 border-t border-line pt-5">
          <summary className="cursor-pointer text-sm font-medium text-muted transition-colors duration-200 hover:text-ink">
            View as a table
          </summary>
          <table className="mt-4 w-full text-sm">
            <caption className="sr-only">CO2e avoided by category</caption>
            <thead>
              <tr className="border-b border-line text-left">
                <th scope="col" className="py-2 pr-4 font-medium text-muted">
                  Category
                </th>
                <th scope="col" className="py-2 pr-4 text-right font-medium text-muted">
                  Items rehomed
                </th>
                <th scope="col" className="py-2 text-right font-medium text-muted">
                  CO₂e (kg)
                </th>
              </tr>
            </thead>
            <tbody>
              {sorted.map((row) => (
                <tr key={row.name} className="border-b border-line last:border-0">
                  <th scope="row" className="py-2 pr-4 text-left font-normal text-ink">
                    {row.label}
                  </th>
                  <td className="tabular py-2 pr-4 text-right text-muted">{row.itemsRehomed}</td>
                  <td className="tabular py-2 text-right text-ink">{row.co2SavedKg.toFixed(1)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </details>
      </div>
    </section>
  );
}
