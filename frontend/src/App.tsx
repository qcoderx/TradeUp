import { useEffect } from "react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { Navbar } from "./components/Navbar";
import { Footer } from "./components/Footer";
import { useAuth } from "./lib/auth";
import { Landing } from "./pages/Landing";
import { Browse } from "./pages/Browse";
import { ListingPage } from "./pages/ListingPage";
import { SellPage } from "./pages/SellPage";
import { Dashboard } from "./pages/Dashboard";
import { Inbox } from "./pages/Inbox";
import { Thread } from "./pages/Thread";
import { SavedPage } from "./pages/SavedPage";
import { ProfilePage } from "./pages/ProfilePage";
import { SignIn, Join } from "./pages/Auth";
import { ImpactPage } from "./pages/ImpactPage";
import { TeamPage } from "./pages/TeamPage";
import { AboutPage } from "./pages/AboutPage";
import { Moderation } from "./pages/Moderation";
import { NotFound } from "./pages/NotFound";

export function App() {
  return (
    <div className="flex min-h-dvh flex-col">
      <SkipLink />
      <ScrollToTop />
      <Navbar />

      <main id="main" className="flex-1">
        <Routes>
          <Route path="/" element={<Landing />} />
          <Route path="/browse" element={<Browse />} />
          <Route path="/listings/:id" element={<ListingPage />} />
          <Route path="/students/:id" element={<ProfilePage />} />
          <Route path="/impact" element={<ImpactPage />} />
          <Route path="/team" element={<TeamPage />} />
          <Route path="/about" element={<AboutPage />} />

          <Route path="/signin" element={<SignIn />} />
          <Route path="/join" element={<Join />} />

          <Route
            path="/sell"
            element={
              <RequireAuth>
                <SellPage />
              </RequireAuth>
            }
          />
          <Route
            path="/listings/:id/edit"
            element={
              <RequireAuth>
                <SellPage />
              </RequireAuth>
            }
          />
          <Route
            path="/dashboard"
            element={
              <RequireAuth>
                <Dashboard />
              </RequireAuth>
            }
          />
          <Route
            path="/saved"
            element={
              <RequireAuth>
                <SavedPage />
              </RequireAuth>
            }
          />
          <Route
            path="/messages"
            element={
              <RequireAuth>
                <Inbox />
              </RequireAuth>
            }
          />
          <Route
            path="/messages/:id"
            element={
              <RequireAuth>
                <Thread />
              </RequireAuth>
            }
          />
          <Route
            path="/moderation"
            element={
              <RequireAuth adminOnly>
                <Moderation />
              </RequireAuth>
            }
          />

          <Route path="*" element={<NotFound />} />
        </Routes>
      </main>

      <Footer />
    </div>
  );
}

/**
 * Guards a route.
 *
 * While the stored token is still being checked, this renders nothing rather
 * than redirecting: bouncing a signed-in student to the sign-in screen for a
 * moment on every hard refresh is worse than a blank frame.
 */
function RequireAuth({ children, adminOnly = false }: { children: React.ReactNode; adminOnly?: boolean }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) return <div className="min-h-[60vh]" aria-busy="true" />;

  if (!user) {
    // Remember where they were headed so sign-in can send them back.
    return <Navigate to="/signin" state={{ from: location.pathname + location.search }} replace />;
  }
  if (adminOnly && !user.admin) return <Navigate to="/dashboard" replace />;

  return <>{children}</>;
}

/** A new page should start at the top, except when only the query changed. */
function ScrollToTop() {
  const { pathname } = useLocation();
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "instant" as ScrollBehavior });
  }, [pathname]);
  return null;
}

function SkipLink() {
  return (
    <a
      href="#main"
      className="sr-only focus:not-sr-only focus:absolute focus:top-3 focus:left-3 focus:z-50 focus:rounded-lg focus:bg-ink focus:px-4 focus:py-2.5 focus:text-sm focus:font-semibold focus:text-paper"
    >
      Skip to content
    </a>
  );
}
