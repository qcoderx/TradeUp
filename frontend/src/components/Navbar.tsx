import { useEffect, useRef, useState } from "react";
import { Link, NavLink, useNavigate, useLocation } from "react-router-dom";
import {
  Bookmark,
  LayoutGrid,
  LogOut,
  MessageSquare,
  Monitor,
  Moon,
  Plus,
  Search,
  Shield,
  Sun,
  User as UserIcon,
  X,
  Menu,
} from "lucide-react";
import { useAuth } from "../lib/auth";
import { useTheme } from "../lib/theme";
import { useApi } from "../lib/useApi";
import { cx } from "../lib/format";
import { Logo } from "./Logo";
import { Avatar } from "./ui";

/**
 * The public sections, in one place.
 *
 * <p>The desktop bar and the mobile sheet both render from this, so a link can
 * no longer appear in one and be forgotten in the other.
 */
const PRIMARY_LINKS = [
  { to: "/browse", label: "Browse" },
  { to: "/impact", label: "Impact" },
  { to: "/team", label: "Team" },
];

export function Navbar() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [term, setTerm] = useState("");
  const [menuOpen, setMenuOpen] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  // Only signed-in students have an inbox to count.
  const { data: unread } = useApi<{ unread: number }>(user ? "/conversations/unread-count" : null, [
    user?.id,
    location.pathname,
  ]);

  // Any navigation closes whatever was open, so a menu never follows you.
  useEffect(() => {
    setMenuOpen(false);
    setMobileOpen(false);
  }, [location.pathname, location.search]);

  useEffect(() => {
    if (!menuOpen) return;
    function onPointerDown(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) setMenuOpen(false);
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setMenuOpen(false);
    }
    document.addEventListener("mousedown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("mousedown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [menuOpen]);

  function onSearch(event: React.FormEvent) {
    event.preventDefault();
    navigate(`/browse${term.trim() ? `?q=${encodeURIComponent(term.trim())}` : ""}`);
  }

  const unreadCount = unread?.unread ?? 0;

  return (
    <header className="sticky top-0 z-30 border-b border-line bg-paper/88 backdrop-blur-md">
      <nav className="mx-auto flex h-16 max-w-7xl items-center gap-3 px-4 sm:px-6" aria-label="Main">
        <Link to="/" className="shrink-0 cursor-pointer" aria-label="TradeUp home">
          <Logo />
        </Link>

        {/* Search is the primary action on a marketplace, so it sits in the
            navbar on every screen rather than only on the browse page. */}
        <form onSubmit={onSearch} role="search" className="ml-2 hidden max-w-md flex-1 md:block">
          <label htmlFor="nav-search" className="sr-only">
            Search listings
          </label>
          <div className="relative">
            <Search
              className="pointer-events-none absolute top-1/2 left-3 h-4 w-4 -translate-y-1/2 text-faint"
              aria-hidden="true"
            />
            <input
              id="nav-search"
              type="search"
              value={term}
              onChange={(event) => setTerm(event.target.value)}
              placeholder="Search textbooks, lab kit, hostel things…"
              className="field h-11 pl-9"
            />
          </div>
        </form>

        <div className="ml-auto flex items-center gap-1.5">
          {PRIMARY_LINKS.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                cx(
                  "hidden cursor-pointer rounded-lg px-3 py-2 text-sm font-semibold transition-colors duration-200 lg:block",
                  isActive ? "text-ink" : "text-muted hover:text-ink"
                )
              }
            >
              {link.label}
            </NavLink>
          ))}

          <ThemeToggle />

          {user ? (
            <>
              <Link
                to="/messages"
                className="relative hidden cursor-pointer rounded-lg p-2.5 text-muted transition-colors duration-200 hover:bg-sunk hover:text-ink sm:block"
                aria-label={unreadCount > 0 ? `Messages, ${unreadCount} unread` : "Messages"}
              >
                <MessageSquare className="h-5 w-5" aria-hidden="true" />
                {unreadCount > 0 && (
                  <span className="absolute top-1 right-1 grid h-4 min-w-4 place-items-center rounded-full bg-red px-1 font-mono text-[0.5625rem] font-bold text-white">
                    {unreadCount > 9 ? "9+" : unreadCount}
                  </span>
                )}
              </Link>

              <Link to="/sell" className="btn btn-primary hidden h-10 min-h-10 px-3.5 sm:inline-flex">
                <Plus className="h-4 w-4" aria-hidden="true" />
                <span className="hidden md:inline">List an item</span>
                <span className="md:hidden">List</span>
              </Link>

              <div className="relative" ref={menuRef}>
                <button
                  type="button"
                  onClick={() => setMenuOpen((open) => !open)}
                  aria-expanded={menuOpen}
                  aria-haspopup="menu"
                  className="cursor-pointer rounded-full p-0.5 transition-shadow duration-200 hover:shadow-[0_0_0_2px_var(--tu-line-strong)]"
                  aria-label="Your account"
                >
                  <Avatar initials={user.initials} size={34} />
                </button>

                {menuOpen && (
                  <div
                    role="menu"
                    className="absolute right-0 z-40 mt-2 w-60 overflow-hidden rounded-xl bg-surface py-1.5 shadow-[var(--shadow-lift),var(--shadow-inset-line)]"
                  >
                    <div className="border-b border-line px-3.5 pt-2 pb-3">
                      <p className="truncate text-sm font-semibold text-ink">{user.fullName}</p>
                      <p className="truncate text-xs text-muted">{user.department ?? "Student"}</p>
                    </div>
                    <MenuLink to="/dashboard" icon={<LayoutGrid className="h-4 w-4" />}>
                      Dashboard
                    </MenuLink>
                    <MenuLink to="/saved" icon={<Bookmark className="h-4 w-4" />}>
                      Saved items
                    </MenuLink>
                    <MenuLink to={`/students/${user.id}`} icon={<UserIcon className="h-4 w-4" />}>
                      Your profile
                    </MenuLink>
                    {user.admin && (
                      <MenuLink to="/moderation" icon={<Shield className="h-4 w-4" />}>
                        Moderation
                      </MenuLink>
                    )}
                    <button
                      type="button"
                      role="menuitem"
                      onClick={() => {
                        signOut();
                        navigate("/");
                      }}
                      className="flex w-full cursor-pointer items-center gap-2.5 border-t border-line px-3.5 py-2.5 text-sm text-muted transition-colors duration-200 hover:bg-sunk hover:text-ink"
                    >
                      <LogOut className="h-4 w-4" aria-hidden="true" />
                      Sign out
                    </button>
                  </div>
                )}
              </div>
            </>
          ) : (
            <>
              <Link
                to="/signin"
                className="hidden cursor-pointer rounded-lg px-3 py-2 text-sm font-semibold text-muted transition-colors duration-200 hover:text-ink sm:block"
              >
                Sign in
              </Link>
              <Link to="/join" className="btn btn-primary h-10 min-h-10 px-4">
                Join
              </Link>
            </>
          )}

          <button
            type="button"
            onClick={() => setMobileOpen((open) => !open)}
            className="cursor-pointer rounded-lg p-2.5 text-muted transition-colors duration-200 hover:bg-sunk hover:text-ink lg:hidden"
            aria-expanded={mobileOpen}
            aria-label={mobileOpen ? "Close menu" : "Open menu"}
          >
            {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>
      </nav>

      {mobileOpen && (
        <div className="border-t border-line bg-paper px-4 py-3 lg:hidden">
          <form onSubmit={onSearch} role="search" className="mb-3 md:hidden">
            <label htmlFor="mobile-search" className="sr-only">
              Search listings
            </label>
            <div className="relative">
              <Search
                className="pointer-events-none absolute top-1/2 left-3 h-4 w-4 -translate-y-1/2 text-faint"
                aria-hidden="true"
              />
              <input
                id="mobile-search"
                type="search"
                value={term}
                onChange={(event) => setTerm(event.target.value)}
                placeholder="Search listings"
                className="field pl-9"
              />
            </div>
          </form>
          <div className="flex flex-col">
            {PRIMARY_LINKS.map((link) => (
              <MobileLink key={link.to} to={link.to}>
                {link.label}
              </MobileLink>
            ))}
            {user ? (
              <>
                <MobileLink to="/sell">List an item</MobileLink>
                <MobileLink to="/messages">
                  Messages{unreadCount > 0 ? ` (${unreadCount})` : ""}
                </MobileLink>
                <MobileLink to="/dashboard">Dashboard</MobileLink>
              </>
            ) : (
              <MobileLink to="/signin">Sign in</MobileLink>
            )}
          </div>
        </div>
      )}
    </header>
  );
}

function MenuLink({ to, icon, children }: { to: string; icon: React.ReactNode; children: React.ReactNode }) {
  return (
    <Link
      to={to}
      role="menuitem"
      className="flex cursor-pointer items-center gap-2.5 px-3.5 py-2.5 text-sm text-muted transition-colors duration-200 hover:bg-sunk hover:text-ink"
    >
      <span aria-hidden="true">{icon}</span>
      {children}
    </Link>
  );
}

function MobileLink({ to, children }: { to: string; children: React.ReactNode }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        cx(
          "cursor-pointer rounded-lg px-3 py-3 text-sm font-semibold transition-colors duration-200",
          isActive ? "bg-sunk text-ink" : "text-muted hover:bg-sunk hover:text-ink"
        )
      }
    >
      {children}
    </NavLink>
  );
}

/** Cycles light, dark, and follow-the-system. */
function ThemeToggle() {
  const { theme, setTheme } = useTheme();

  const next = theme === "light" ? "dark" : theme === "dark" ? "system" : "light";
  const label = { light: "Light theme", dark: "Dark theme", system: "Matching your system" }[theme];

  return (
    <button
      type="button"
      onClick={() => setTheme(next)}
      className="cursor-pointer rounded-lg p-2.5 text-muted transition-colors duration-200 hover:bg-sunk hover:text-ink"
      aria-label={`${label}. Switch to ${next === "system" ? "system" : next}.`}
      title={label}
    >
      {theme === "light" && <Sun className="h-5 w-5" aria-hidden="true" />}
      {theme === "dark" && <Moon className="h-5 w-5" aria-hidden="true" />}
      {theme === "system" && <Monitor className="h-5 w-5" aria-hidden="true" />}
    </button>
  );
}
