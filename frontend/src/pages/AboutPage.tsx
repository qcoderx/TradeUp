import { LinkButton } from "../components/ui";

const STACK = [
  { label: "Language", value: "Java 21" },
  { label: "API", value: "Spring Boot 3.5, Spring Security, JWT" },
  { label: "Persistence", value: "Spring Data JPA over an embedded JDBC database" },
  { label: "File handling", value: "CSV and JSON exports written with java.nio" },
  { label: "Interface", value: "React, TypeScript, Tailwind" },
];

const CONCEPTS = [
  {
    title: "Object-oriented design",
    body: "Listings, offers, conversations and reports are real objects with real behaviour. A listing decides for itself which state changes are legal, so no caller can put it into a state that does not make sense.",
  },
  {
    title: "Collections",
    body: "ArrayList, HashSet, EnumMap and the Stream API do the work throughout — grouping trades by category for the impact figures, folding search filters together, counting unread messages in a thread.",
  },
  {
    title: "Exception handling",
    body: "A small hierarchy under AppException carries its own HTTP status and a sentence written for a student. One handler turns any of them into the same JSON shape, so the interface only has to understand one kind of failure.",
  },
  {
    title: "File input and output",
    body: "The catalogue can be written out to timestamped CSV and JSON on disk, with proper quoting and try-with-resources — the file-handling half of the original proposal, kept alongside the database rather than replaced by it.",
  },
  {
    title: "Database connectivity",
    body: "An embedded JDBC database holds everything, reached through JPA. A MySQL profile is included for when a server is available, which was the proposal's stretch goal.",
  },
];

export function AboutPage() {
  return (
    <>
      <section className="adire">
        <div className="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:py-24">
          <p className="font-mono text-[0.6875rem] tracking-[0.16em] text-marigold-bright uppercase">
            About the project
          </p>
          <h1 className="mt-5 max-w-3xl text-[clamp(2.2rem,5.4vw,3.5rem)] leading-[1.02]">
            A course project we would actually use
          </h1>
          <p className="mt-6 max-w-2xl text-[1.0625rem] leading-relaxed text-white/78">
            TradeUp was submitted for COS202 Computer Programming II at the University of Lagos. The brief
            asked for a demonstration of core Java. We wanted something that would still be worth running
            after it had been marked.
          </p>
        </div>
      </section>

      <div className="mx-auto max-w-7xl px-4 py-20 sm:px-6">
        <section className="grid gap-12 lg:grid-cols-[0.85fr_1.15fr]">
          <div>
            <p className="eyebrow">The problem</p>
            <h2 className="mt-2 text-[clamp(1.6rem,3vw,2.25rem)]">
              Two halves of the same problem, a hundred metres apart
            </h2>
          </div>
          <div className="flex flex-col gap-5 text-[0.9375rem] leading-relaxed text-muted">
            <p>
              Students here hold onto old textbooks, calculators, lab equipment and furniture they no
              longer use. Other students — especially first years and those on tight budgets — struggle to
              afford those same items new.
            </p>
            <p>
              There is no trusted, dedicated place for students on this campus to exchange goods with one
              another. General marketplace apps are too broad and too impersonal to feel safe for campus
              use, so usable items go to waste.
            </p>
            <p className="text-ink">
              TradeUp exists to close that gap, and to make the closing of it visible: every completed
              trade adds to a figure the whole campus can see.
            </p>
          </div>
        </section>

        {/* What it demonstrates ------------------------------------------- */}
        <section className="mt-24">
          <p className="eyebrow">What it demonstrates</p>
          <h2 className="mt-2 max-w-2xl text-[clamp(1.6rem,3vw,2.25rem)]">
            The course requirements, met by things the app genuinely needs
          </h2>

          <div className="mt-10 grid gap-px overflow-hidden rounded-2xl bg-line md:grid-cols-2 lg:grid-cols-3">
            {CONCEPTS.map((concept) => (
              <article key={concept.title} className="bg-surface p-7">
                <h3 className="font-display text-lg leading-snug font-semibold text-ink">
                  {concept.title}
                </h3>
                <p className="mt-2.5 text-sm leading-relaxed text-muted">{concept.body}</p>
              </article>
            ))}
          </div>
        </section>

        {/* Stack ------------------------------------------------------------ */}
        <section className="mt-24">
          <p className="eyebrow">How it is built</p>
          <h2 className="mt-2 text-[clamp(1.6rem,3vw,2.25rem)]">The stack</h2>

          <dl className="mt-8 max-w-3xl">
            {STACK.map((row) => (
              <div
                key={row.label}
                className="flex flex-col gap-1 border-b border-line py-4 sm:flex-row sm:gap-8"
              >
                <dt className="w-40 shrink-0 text-sm font-semibold text-ink">{row.label}</dt>
                <dd className="text-sm text-muted">{row.value}</dd>
              </div>
            ))}
          </dl>

          <p className="mt-8 max-w-2xl text-sm leading-relaxed text-muted">
            The original proposal specified a Swing or JavaFX desktop application. We kept every Java
            concept it promised and moved the interface to the browser, so the marketplace can be opened
            on the phone a student already has in their pocket rather than only on a lab machine.
          </p>
        </section>

        <div className="mt-20 rounded-2xl bg-sunk px-8 py-12 text-center">
          <h2 className="mx-auto max-w-xl text-[clamp(1.5rem,3vw,2.1rem)]">
            Have a look at what is on the board
          </h2>
          <div className="mt-7 flex flex-wrap justify-center gap-3">
            <LinkButton to="/browse">Browse listings</LinkButton>
            <LinkButton to="/team" tone="outline">
              Meet Group 15
            </LinkButton>
          </div>
        </div>
      </div>
    </>
  );
}
