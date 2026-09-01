import { useApi } from "../lib/useApi";
import { cx } from "../lib/format";
import type { TeamMember } from "../lib/types";
import { Avatar, ErrorState, Skeleton } from "../components/ui";

/**
 * The badge is small and set in uppercase mono, so long titles are trimmed to
 * something that fits. Anything unrecognised is shown as it comes, rather than
 * being silently mislabelled as one of the roles we happen to know about.
 */
function shortRole(role: string): string {
  if (role === "Team Captain") return "Captain";
  if (role === "Assistant Team Captain") return "Assistant";
  if (role === "Lead Developer") return "Lead dev";
  return role;
}

/** Initials from a full name, matching how the backend builds them. */
function initialsOf(fullName: string): string {
  const parts = fullName.trim().split(/\s+/);
  const joined = parts.map((part) => part[0]?.toUpperCase() ?? "").join("");
  return joined.length <= 2 ? joined : joined[0] + joined[joined.length - 1];
}

export function TeamPage() {
  const { data, error, loading, reload } = useApi<TeamMember[]>("/team");

  const leads = (data ?? []).filter((member) => member.role !== "Member");
  const members = (data ?? []).filter((member) => member.role === "Member");

  return (
    <>
      <section className="hero-panel">
        <div className="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:py-24">
          <p className="font-mono text-[0.6875rem] tracking-[0.16em] text-green uppercase">
            Group 15 · COS202 Computer Programming II
          </p>
          <h1 className="mt-5 max-w-3xl text-[clamp(2.2rem,5.4vw,3.5rem)] leading-[1.02]">
            Eleven students who got tired of watching good things go to waste
          </h1>
          <p className="mt-6 max-w-2xl text-[1.0625rem] leading-relaxed text-muted">
            We are second and third year students at the University of Lagos, from Data Science,
            Mathematics, Computer Science and Science Education. TradeUp is our answer to a problem we
            watch play out on our own campus every single semester.
          </p>
        </div>
      </section>

      {/* The initiative --------------------------------------------------- */}
      <section className="mx-auto max-w-7xl px-4 py-20 sm:px-6">
        <div className="grid gap-12 lg:grid-cols-[0.9fr_1.1fr]">
          <div>
            <p className="eyebrow">Our initiative</p>
            <h2 className="mt-2 text-[clamp(1.75rem,3.4vw,2.5rem)]">
              The waste is not a shortage of things. It is a shortage of matching.
            </h2>
          </div>

          <div className="flex flex-col gap-6 text-[0.9375rem] leading-relaxed text-muted">
            <p>
              Every semester, students here accumulate things they no longer need: textbooks from courses
              already passed, lab coats, calculators, kettles, desk lamps, furniture. At the same time,
              other students — especially first years and those on tight budgets — are struggling to
              afford those exact same items new.
            </p>
            <p>
              Both halves of the problem sit within walking distance of each other, and nothing connects
              them. General marketplace apps are too broad and too impersonal to feel safe for campus use,
              so usable things end up in a corner, or in a bin.
            </p>
            <p className="text-ink">
              TradeUp is a marketplace built only for this campus, so that an item finishing its life with
              one student can start a second life with another. That is{" "}
              <span className="font-semibold">
                UN Sustainable Development Goal 12, Responsible Consumption and Production
              </span>
              , applied to the thing we can actually reach.
            </p>
          </div>
        </div>

        <dl className="mt-16 grid gap-px overflow-hidden rounded-2xl bg-line sm:grid-cols-3">
          {[
            {
              term: "Environmental",
              detail: "Less waste from discarded textbooks, tools and hostel kit at the end of every session.",
            },
            {
              term: "Financial",
              detail: "Students get what they need second-hand, at prices a student budget can carry.",
            },
            {
              term: "Social",
              detail: "A culture of passing things on, and a reason for students to trust each other.",
            },
          ].map((item) => (
            <div key={item.term} className="bg-surface p-7">
              <dt className="font-display text-lg font-semibold text-ink">{item.term}</dt>
              <dd className="mt-2 text-sm leading-relaxed text-muted">{item.detail}</dd>
            </div>
          ))}
        </dl>
      </section>

      {/* The roster -------------------------------------------------------- */}
      <section className="mx-auto max-w-7xl px-4 pb-24 sm:px-6">
        <p className="eyebrow">The group</p>
        <h2 className="mt-2 text-[clamp(1.75rem,3.4vw,2.5rem)]">Who built it</h2>

        {error ? (
          <div className="mt-8">
            <ErrorState message={error.message} onRetry={reload} />
          </div>
        ) : loading ? (
          <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 11 }, (_, index) => (
              <Skeleton key={index} className="h-28 w-full rounded-xl" />
            ))}
          </div>
        ) : (
          <>
            <div className="mt-8 grid gap-4 sm:grid-cols-2">
              {leads.map((member) => (
                <MemberCard key={member.matricNumber} member={member} lead />
              ))}
            </div>
            <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {members.map((member) => (
                <MemberCard key={member.matricNumber} member={member} />
              ))}
            </div>
          </>
        )}

        <p className="mt-12 border-t border-line pt-6 text-sm text-muted">
          University of Lagos · Faculty of Science · COS202 Computer Programming II · SDG 12
        </p>
      </section>
    </>
  );
}

function MemberCard({ member, lead = false }: { member: TeamMember; lead?: boolean }) {
  return (
    <article
      className={cx(
        "flex items-center gap-4 rounded-xl p-5 transition-shadow duration-200",
        lead
          ? "bg-surface shadow-[0_0_0_1px_var(--tu-green-bright)]"
          : "bg-surface shadow-[var(--shadow-inset-line)] hover:shadow-[var(--shadow-ticket),var(--shadow-inset-line)]"
      )}
    >
      <Avatar initials={initialsOf(member.fullName)} size={48} />

      <div className="min-w-0 flex-1">
        <h3 className="font-display text-[1.0625rem] leading-tight font-semibold text-ink">
          {member.fullName}
        </h3>
        <p className="mt-0.5 text-sm text-muted">{member.department}</p>
        <p className="tabular mt-1.5 font-mono text-[0.6875rem] tracking-wide text-faint">
          {member.matricNumber}
        </p>
      </div>

      {lead && (
        <span className="shrink-0 self-start rounded-md bg-green-soft px-2 py-1 font-mono text-[0.625rem] tracking-wide text-green uppercase">
          {shortRole(member.role)}
        </span>
      )}
    </article>
  );
}
