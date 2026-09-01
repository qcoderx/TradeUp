import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { motion, useReducedMotion } from "framer-motion";
import { ArrowRight, Leaf, Search, Users } from "lucide-react";
import { useApi } from "../lib/useApi";
import { useAuth } from "../lib/auth";
import { formatCo2, formatPrice } from "../lib/format";
import type { CategoryOption, ImpactSnapshot, ListingCard, ReferenceData } from "../lib/types";
import { TradeTicket, TradeTicketSkeleton } from "../components/TradeTicket";
import { LinkButton } from "../components/ui";

/** How a trade actually goes. A real sequence, so the numbering earns its place. */
const STEPS = [
  {
    title: "List what you have finished with",
    body: "A photo, a price or what you would swap it for, and where on campus you can meet. Two minutes.",
  },
  {
    title: "Agree with the student who wants it",
    body: "They message you or make an offer. Accept one and the item is held for them while you sort out a time.",
  },
  {
    title: "Hand it over, and mark it traded",
    body: "The item keeps its history. The next person to list it sees it is on its third owner, not its first.",
  },
];

export function Landing() {
  const { user } = useAuth();
  const { data: impact } = useApi<ImpactSnapshot>("/impact");
  const { data: reference } = useApi<ReferenceData>("/reference");
  const { data: latest, loading: latestLoading } = useApi<ListingCard[]>("/listings/latest?limit=8");

  return (
    <>
      <Hero impact={impact} categories={reference?.categories ?? []} />
      <FreshOnTheBoard listings={latest ?? []} loading={latestLoading} />
      <HowItWorks />
      <ImpactBand impact={impact} />
      <JoinBand signedIn={Boolean(user)} />
    </>
  );
}

/* -------------------------------------------------------------------------
   Hero
   ------------------------------------------------------------------------- */

function Hero({ impact, categories }: { impact: ImpactSnapshot | null; categories: CategoryOption[] }) {
  const navigate = useNavigate();
  const [term, setTerm] = useState("");
  const reduceMotion = useReducedMotion();

  function onSearch(event: React.FormEvent) {
    event.preventDefault();
    navigate(`/browse${term.trim() ? `?q=${encodeURIComponent(term.trim())}` : ""}`);
  }

  // One orchestrated entrance rather than a dozen scattered effects.
  const rise = {
    hidden: { opacity: 0, y: reduceMotion ? 0 : 18 },
    show: (index: number) => ({
      opacity: 1,
      y: 0,
      transition: { delay: reduceMotion ? 0 : 0.06 * index, duration: 0.5, ease: [0.22, 1, 0.36, 1] as const },
    }),
  };

  const busiest = categories.filter((category) => category.availableCount > 0).slice(0, 6);

  return (
    <section className="hero-panel overflow-hidden">
      <div className="mx-auto grid max-w-7xl gap-14 px-4 pt-16 pb-16 sm:px-6 lg:grid-cols-[1.05fr_0.95fr] lg:items-center lg:pt-20 lg:pb-20">
        <div>
          {/* The three crest colours, in the order they read on the arms. */}
          <motion.div custom={0} initial="hidden" animate="show" variants={rise} className="crest-rule w-28" />

          <motion.p
            custom={1}
            initial="hidden"
            animate="show"
            variants={rise}
            className="mt-6 inline-flex items-center gap-2 rounded-full bg-green-soft px-3 py-1.5 font-mono text-[0.6875rem] tracking-[0.14em] text-green uppercase"
          >
            University of Lagos · SDG 12
          </motion.p>

          <motion.h1
            custom={2}
            initial="hidden"
            animate="show"
            variants={rise}
            className="mt-6 max-w-xl text-[clamp(2.6rem,6.2vw,4.25rem)] leading-[0.98] font-bold"
          >
            Nothing here is new.
            <br />
            <span className="text-green">That is the point.</span>
          </motion.h1>

          <motion.p
            custom={3}
            initial="hidden"
            animate="show"
            variants={rise}
            className="mt-5 max-w-lg text-[1.0625rem] leading-relaxed text-muted"
          >
            Textbooks, lab coats, kettles and desk lamps, passed from one student to the next instead of
            being bought twice. Find what you need on campus, at a price a student can actually pay.
          </motion.p>

          {/* Search is the hero CTA: the fastest way to find out whether the
              thing you need is already sitting in someone else's room. */}
          <motion.form
            custom={4}
            initial="hidden"
            animate="show"
            variants={rise}
            onSubmit={onSearch}
            role="search"
            className="mt-8 flex max-w-lg flex-col gap-2.5 sm:flex-row"
          >
            <label htmlFor="hero-search" className="sr-only">
              Search listings
            </label>
            <div className="relative flex-1">
              <Search
                className="pointer-events-none absolute top-1/2 left-4 h-[1.125rem] w-[1.125rem] -translate-y-1/2 text-faint"
                aria-hidden="true"
              />
              <input
                id="hero-search"
                type="search"
                value={term}
                onChange={(event) => setTerm(event.target.value)}
                placeholder="What are you looking for?"
                className="field h-13 min-h-13 rounded-xl pl-11 text-base shadow-lg"
              />
            </div>
            <button type="submit" className="btn btn-primary h-13 min-h-13 rounded-xl px-6 text-base">
              Search
            </button>
          </motion.form>

          {busiest.length > 0 && (
            <motion.div
              custom={5}
              initial="hidden"
              animate="show"
              variants={rise}
              className="mt-5 flex flex-wrap items-center gap-2"
            >
              <span className="mr-1 text-xs text-faint">Popular right now</span>
              {busiest.map((category) => (
                <Link
                  key={category.slug}
                  to={`/browse?category=${category.slug}`}
                  className="cursor-pointer rounded-full bg-sunk px-3 py-1.5 text-xs font-medium text-muted ring-1 ring-line transition-colors duration-200 hover:bg-blue-soft hover:text-white"
                >
                  {category.label}
                  <span className="ml-1.5 font-mono text-faint">{category.availableCount}</span>
                </Link>
              ))}
            </motion.div>
          )}

          {impact && (
            <motion.dl
              custom={6}
              initial="hidden"
              animate="show"
              variants={rise}
              className="mt-10 grid max-w-lg grid-cols-3 gap-6 border-t border-line pt-6"
            >
              <HeroStat value={String(impact.itemsRehomed)} label="items rehomed" />
              <HeroStat value={formatCo2(impact.co2SavedKg)} label="CO₂e avoided" />
              <HeroStat value={String(impact.studentsRegistered)} label="students on board" />
            </motion.dl>
          )}
        </div>

        {/* The signature element, introduced in the hero rather than described. */}
        <HeroTickets reduceMotion={Boolean(reduceMotion)} />
      </div>
    </section>
  );
}

function HeroStat({ value, label }: { value: string; label: string }) {
  return (
    <div>
      <dt className="sr-only">{label}</dt>
      <dd>
        <span className="tabular block font-display text-2xl leading-none font-bold text-ink">{value}</span>
        <span className="mt-1.5 block text-xs leading-snug text-muted">{label}</span>
      </dd>
    </div>
  );
}

/**
 * A real listing drawn as a ticket, with a second item stacked behind it.
 *
 * The card behind deliberately shows only its photograph. Two full tickets at
 * this size collide — the back one's title and price emerge from behind the
 * front one and read as a rendering fault rather than as depth.
 */
function HeroTickets({ reduceMotion }: { reduceMotion: boolean }) {
  const { data } = useApi<ListingCard[]>("/listings/trending?limit=2");
  const [front, back] = data ?? [];

  if (!front) {
    return (
      <div className="relative hidden h-[26rem] lg:block" aria-hidden="true">
        <div className="absolute top-10 right-10 w-64 rotate-[5deg] rounded-[14px] bg-sunk ring-1 ring-line backdrop-blur-sm">
          <div className="aspect-item" />
          <div className="h-24" />
        </div>
      </div>
    );
  }

  return (
    <div className="relative mx-auto hidden h-[26rem] w-full max-w-md lg:block" aria-hidden="true">
      {back?.primaryImageUrl && (
        <motion.div
          initial={{ opacity: 0, y: reduceMotion ? 0 : 22, rotate: 7 }}
          animate={{ opacity: 1, y: 0, rotate: 7 }}
          transition={{ delay: reduceMotion ? 0 : 0.34, duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
          className="absolute top-0 right-2 w-56 overflow-hidden rounded-[14px] shadow-[0_20px_50px_-24px_rgb(0_0_0/0.8)] ring-1 ring-line"
        >
          <img src={back.primaryImageUrl} alt="" className="aspect-item w-full object-cover" />
        </motion.div>
      )}

      <motion.div
        initial={{ opacity: 0, y: reduceMotion ? 0 : 30, rotate: -3.5 }}
        animate={{ opacity: 1, y: 0, rotate: -3.5 }}
        transition={{ delay: reduceMotion ? 0 : 0.2, duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
        className="absolute bottom-2 left-0 w-[19.5rem]"
      >
        <TradeTicket listing={front} priority />
      </motion.div>
    </div>
  );
}

/* -------------------------------------------------------------------------
   Fresh listings
   ------------------------------------------------------------------------- */

function FreshOnTheBoard({ listings, loading }: { listings: ListingCard[]; loading: boolean }) {
  return (
    <section className="mx-auto max-w-7xl px-4 pt-20 sm:px-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="eyebrow">Fresh on the board</p>
          <h2 className="mt-2 text-[clamp(1.75rem,3.4vw,2.5rem)]">Just listed by other students</h2>
        </div>
        <Link
          to="/browse"
          className="group inline-flex cursor-pointer items-center gap-1.5 text-sm font-semibold text-ink transition-colors duration-200 hover:text-green"
        >
          See everything
          <ArrowRight
            className="h-4 w-4 transition-transform duration-200 group-hover:translate-x-0.5"
            aria-hidden="true"
          />
        </Link>
      </div>

      <div className="mt-8 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
        {loading
          ? Array.from({ length: 8 }, (_, index) => <TradeTicketSkeleton key={index} />)
          : listings.map((listing, index) => (
              <TradeTicket key={listing.id} listing={listing} priority={index < 4} />
            ))}
      </div>
    </section>
  );
}

/* -------------------------------------------------------------------------
   How it works
   ------------------------------------------------------------------------- */

function HowItWorks() {
  return (
    <section className="mx-auto max-w-7xl px-4 pt-24 sm:px-6">
      <p className="eyebrow">How a trade goes</p>
      <h2 className="mt-2 max-w-2xl text-[clamp(1.75rem,3.4vw,2.5rem)]">
        Three steps, and the item keeps its story
      </h2>

      <ol className="mt-10 grid gap-px overflow-hidden rounded-2xl bg-line md:grid-cols-3">
        {STEPS.map((step, index) => (
          <li key={step.title} className="bg-surface p-7">
            <span className="font-mono text-sm font-medium text-green">
              {String(index + 1).padStart(2, "0")}
            </span>
            <h3 className="mt-3 font-display text-lg leading-snug font-semibold text-ink">{step.title}</h3>
            <p className="mt-2 text-sm leading-relaxed text-muted">{step.body}</p>
          </li>
        ))}
      </ol>
    </section>
  );
}

/* -------------------------------------------------------------------------
   Impact
   ------------------------------------------------------------------------- */

function ImpactBand({ impact }: { impact: ImpactSnapshot | null }) {
  if (!impact) return null;

  return (
    <section className="mx-auto max-w-7xl px-4 pt-24 sm:px-6">
      <div className="overflow-hidden rounded-2xl bg-surface shadow-[var(--shadow-inset-line)]">
        <div className="grid gap-10 p-8 sm:p-11 lg:grid-cols-[1fr_1.15fr] lg:items-center">
          <div>
            <p className="eyebrow">Sustainable Development Goal 12</p>
            <h2 className="mt-2 text-[clamp(1.6rem,3vw,2.25rem)]">
              Responsible consumption, counted item by item
            </h2>
            <p className="mt-4 max-w-md text-[0.9375rem] leading-relaxed text-muted">
              These are not decorative numbers. Every completed trade adds what its own category and
              condition actually imply, worked out on the server from the trades in the database.
            </p>
            <LinkButton to="/impact" tone="outline" className="mt-6">
              See the full breakdown
              <ArrowRight className="h-4 w-4" aria-hidden="true" />
            </LinkButton>
          </div>

          <dl className="grid grid-cols-2 gap-px overflow-hidden rounded-xl bg-line">
            <ImpactCell
              icon={<Leaf className="h-4 w-4" aria-hidden="true" />}
              value={formatCo2(impact.co2SavedKg)}
              label="CO₂e kept out of the air"
            />
            <ImpactCell value={`${impact.wasteDivertedKg.toFixed(1)} kg`} label="diverted from waste" />
            <ImpactCell
              value={formatPrice(impact.moneyKeptInPocketsKobo)}
              label="kept in student pockets"
            />
            <ImpactCell
              icon={<Users className="h-4 w-4" aria-hidden="true" />}
              value={String(impact.studentsRegistered)}
              label="students trading"
            />
          </dl>
        </div>
      </div>
    </section>
  );
}

function ImpactCell({ icon, value, label }: { icon?: React.ReactNode; value: string; label: string }) {
  return (
    <div className="bg-surface p-5">
      {icon && <span className="mb-2 inline-flex text-green">{icon}</span>}
      <dd className="tabular font-display text-2xl leading-none font-bold text-ink">{value}</dd>
      <dt className="mt-1.5 text-xs leading-snug text-muted">{label}</dt>
    </div>
  );
}

/* -------------------------------------------------------------------------
   Closing call
   ------------------------------------------------------------------------- */

function JoinBand({ signedIn }: { signedIn: boolean }) {
  return (
    <section className="mx-auto max-w-7xl px-4 pt-24 sm:px-6">
      <div className="rounded-2xl bg-sunk px-8 py-14 text-center sm:px-12">
        <h2 className="mx-auto max-w-2xl text-[clamp(1.75rem,3.6vw,2.6rem)]">
          You already own something another student is looking for
        </h2>
        <p className="mx-auto mt-4 max-w-lg text-[0.9375rem] leading-relaxed text-muted">
          The textbook you finished with in June is the one a first year cannot afford in October. Put it
          on the board.
        </p>
        <div className="mt-8 flex flex-wrap justify-center gap-3">
          <LinkButton to={signedIn ? "/sell" : "/join"}>
            {signedIn ? "List an item" : "Create your account"}
            <ArrowRight className="h-4 w-4" aria-hidden="true" />
          </LinkButton>
          <LinkButton to="/browse" tone="outline">
            Browse first
          </LinkButton>
        </div>
      </div>
    </section>
  );
}
