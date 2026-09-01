import { Link } from "react-router-dom";
import { Logo } from "./Logo";

const COLUMNS = [
  {
    heading: "Marketplace",
    links: [
      { to: "/browse", label: "Browse everything" },
      { to: "/browse?category=textbooks", label: "Textbooks" },
      { to: "/browse?category=electronics", label: "Electronics" },
      { to: "/browse?intent=SWAP", label: "Open to swaps" },
      { to: "/sell", label: "List an item" },
    ],
  },
  {
    heading: "The project",
    links: [
      { to: "/impact", label: "Our SDG 12 impact" },
      { to: "/team", label: "Group 15" },
      { to: "/about", label: "Why we built this" },
    ],
  },
];

export function Footer() {
  return (
    <footer className="adire mt-24">
      <div className="mx-auto max-w-7xl px-4 pt-16 pb-10 sm:px-6">
        <div className="grid gap-12 md:grid-cols-[1.4fr_1fr_1fr]">
          <div>
            <Logo className="text-on-indigo" />
            <p className="mt-4 max-w-xs text-sm leading-relaxed text-white/70">
              A campus marketplace for the University of Lagos. Every item that changes hands here is one
              that did not have to be made again.
            </p>

            <p className="mt-6 flex items-center gap-2 font-mono text-[0.6875rem] tracking-[0.14em] text-marigold-bright uppercase">
              <span className="inline-block h-1.5 w-1.5 rounded-full bg-marigold-bright" aria-hidden="true" />
              SDG 12 · Responsible consumption
            </p>
          </div>

          {COLUMNS.map((column) => (
            <div key={column.heading}>
              <h2 className="font-mono text-[0.6875rem] font-medium tracking-[0.16em] text-white/50 uppercase">
                {column.heading}
              </h2>
              <ul className="mt-4 flex flex-col gap-2.5">
                {column.links.map((link) => (
                  <li key={link.to + link.label}>
                    <Link
                      to={link.to}
                      className="cursor-pointer text-sm text-white/75 transition-colors duration-200 hover:text-marigold-bright"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="mt-14 flex flex-col gap-3 border-t border-white/12 pt-6 text-xs text-white/55 sm:flex-row sm:items-center sm:justify-between">
          <p>
            Built by <span className="text-white/85">Group 15</span> · COS202 Computer Programming II ·
            University of Lagos
          </p>
          <p className="font-mono tracking-wide">Java · Spring Boot · React</p>
        </div>
      </div>
    </footer>
  );
}
