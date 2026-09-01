# TradeUp

**A Student Marketplace for Smarter Living**

University of Lagos · COS202 Computer Programming II · **Group 15**
Aligned to **UN Sustainable Development Goal 12 — Responsible Consumption and Production**

Every semester students accumulate textbooks, calculators, lab coats and hostel kit they
have finished with, while other students — especially first years and those on tight
budgets — struggle to afford the same things new. Both halves of that problem sit within
walking distance of each other on the Akoka campus, and nothing connects them.

TradeUp is a marketplace built only for this campus, so an item finishing its life with one
student can start a second life with another — and so that the saving is **visible**, not
invisible.

---

## The team

| Matric | Name | Department | Role |
|---|---|---|---|
| 240817017 | Adebowale Okiki David | Data Science | Team Captain |
| 240806153 | Bakare Deborah Oluwatosin | Mathematics | Assistant Team Captain |
| 252605503 | Bello Trust Osereme | Mathematics | Member |
| 252609502 | Fatoyinbo Victor Ayomikun | Data Science | Member |
| 240805034 | Obi Omasirichukwu Joan | Computer Science | Member |
| 240313022 | Adebayo Mistura Temitope | Science Education | Member |
| 240805036 | Adeniran Abdurrahman Adebolaji | Computer Science | Member |
| 240817008 | Lasisi Quadri Toluwalase | Data Science | Member |
| 240805111 | Harrison Blessing Idoreyin | Computer Science | Member |
| 252609512 | Olawunmi Afolabi Olajumoke | Data Science | Member |
| 240817013 | Salami Abdulmalik Ayobami | Data Science | Member |

The roster is served from the API (`GET /api/team`) and rendered at `/team`, so the names on
the site and the names in the proposal come from one place.

---

## Running it

You need **JDK 21+** and **Node 20+**. The Maven wrapper downloads Maven on first use.

### 1. Configure the database

Credentials live in `backend/.env`, which is gitignored and must never be committed.

```bash
cd backend
cp .env.example .env
```

Fill in your PostgreSQL connection (we use [Neon](https://neon.tech)) and a JWT signing key.
The connection string Neon hands you is in libpq form, so rewrite it for JDBC:

```
postgresql://USER:PASSWORD@HOST/neondb?sslmode=require   ->   jdbc:postgresql://HOST/neondb?sslmode=require
```

with the user and password as separate entries. Drop `channel_binding` — it is a libpq
option the JDBC driver does not accept.

No database to hand? Run entirely offline against an embedded file database instead:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

### 2. Start the Java API

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

It comes up on **http://localhost:8080** and, on an empty database, seeds itself with the
eleven Group 15 accounts, sixteen listings, some completed trades, a couple of live
conversations, a pending offer and one open moderation report.

### 3. Start the interface

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**. The dev server proxies `/api` to the backend, so there is
nothing to configure.

### 4. Sign in

On first run the seeder creates the eleven Group 15 accounts and **prints the password it
generated** to the console:

```
Seeded 11 students and 16 listings. Sign in with any seeded email and the password: <generated>
```

Pin it to something you choose with `TRADEUP_SEED_PASSWORD` in `.env`. No password is written
into this repository, so a checkout can never be used to sign in to somebody else's deployment.

Any seeded address works, for example `joan.obi@live.unilag.edu.ng`.

### Becoming a moderator

There is no shared moderator account. Register normally, then name yourself:

```
TRADEUP_ADMIN_EMAILS=your.name@live.unilag.edu.ng
```

You are promoted the next time the application starts, and demoted again if your address is
removed — so the environment variable stays the single source of truth. Moderation covers the
report queue and the CSV/JSON exports, and those exports contain real names and matric numbers,
which is exactly why it is not something you can sign up for.

### A note on secrets

Nothing secret is in this repository. `backend/.env` is gitignored; `backend/.env.example`
shows the shape with placeholder values. `application.yml` reads every credential through
`${...}` with **no fallback**, so a missing value stops the application rather than letting
it quietly start against the wrong database. In a deployment, set the same names as real
environment variables and skip the `.env` entirely.

---

## What is in it

**For students** — register with a matric number, list an item for sale or swap with up to
six photos, browse with keyword, category, condition, intent and price filters, save items,
message a lister, make cash or swap offers, and see your own impact on your dashboard.

**For moderators** — a report queue with uphold and dismiss decisions, and one-click CSV and
JSON exports of the whole catalogue.

**Throughout** — the SDG 12 figures on the landing page and at `/impact` are computed from
real completed trades, not hard-coded.

### The trade ticket

Every listing is drawn as a perforated ticket rather than a product card, with a torn stub
carrying its reference code, **which owner this is** (`3rd owner`), and the CO₂e its reuse
saves. A second-hand item is an object with a history, and the interface says so.

---

## How it maps to the course requirements

The proposal promised a demonstration of core Java. Each requirement is met by something the
application genuinely needs, not by a bolted-on example.

| Requirement | Where it lives |
|---|---|
| **Object-oriented design** | `domain/` — `Listing` owns its own lifecycle rules, so no caller can drive it into an illegal state. `BaseEntity` shares identity and audit stamps by inheritance. `Offer` uses static factories to make an invalid offer unconstructable. |
| **Collections** | `ImpactService` groups trades with `EnumMap`; `ListingService` folds filters over a `List<Specification>`; `Conversation` counts unread messages with the Stream API; `ListingSpecifications` builds predicates from `Collection<Condition>`. |
| **Exception handling** | `web/error/` — a small hierarchy under `AppException`, each carrying its own HTTP status and a sentence written for a student. `GlobalExceptionHandler` turns any of them into one JSON shape. |
| **File I/O** | `ArchiveService` writes timestamped CSV and JSON with `java.nio.file`, try-with-resources, and correct CSV quoting. This is Phase 1 of the proposal, kept alongside the database rather than replaced by it. |
| **Database connectivity** | Spring Data JPA over **PostgreSQL** via JDBC, hosted on Neon (Phase 2 of the proposal, and past it — a real networked server rather than a local file). An `h2` profile keeps an embedded database available for offline work, and the tests run against in-memory H2 so they never touch the shared database. |
| **CRUD** | Create, browse/search, update and delete listings — plus the reserve → complete lifecycle that a real handover needs. |

### Deviation from the proposal, stated plainly

The proposal specified a **Swing or JavaFX desktop application**. This implementation keeps
every Java concept it promised and moves the interface to the browser, so the marketplace
opens on the phone a student already carries rather than only on a lab machine. The Java is
the same; the presentation layer is HTTP instead of Swing.

---

## Deploying to Render

Render has **no native Java runtime** — JVM services deploy from a Dockerfile. `backend/Dockerfile`
and `render.yaml` are set up for this.

**Easiest path:** Render Dashboard → New → Blueprint → point at this repo. `render.yaml` creates
the service and prompts for the secrets. Or configure by hand:

| Setting | Value |
|---|---|
| Language | **Docker** |
| Root Directory | `backend` |
| Dockerfile Path | `./Dockerfile` |
| Health Check Path | `/actuator/health` |

Then set these environment variables in the Render dashboard — never in the repo:

```
TRADEUP_DB_URL          jdbc:postgresql://HOST/neondb?sslmode=require
TRADEUP_DB_USER         your_db_user
TRADEUP_DB_PASSWORD     your_db_password
TRADEUP_JWT_SECRET      (let Render generate one)
TRADEUP_CORS_ORIGINS    https://your-frontend-url
TRADEUP_SEED_ENABLED    false
TRADEUP_ADMIN_EMAILS    your.name@live.unilag.edu.ng
```

The app binds `$PORT`, which Render sets, and falls back to 8080 locally. Because
`spring.config.import` marks the `.env` as optional, the same configuration works from real
environment variables with no file present.

### Two things to know before you rely on it

**Uploaded photos do not survive a restart.** `StorageService` writes to local disk, and Render's
filesystem is ephemeral — free instances cannot mount a persistent disk at all. Listing photos
uploaded through the site vanish on every redeploy, restart, or spin-down. The seeded imagery is
fine because it is served by the frontend as a static asset. To fix properly, either attach a
Render persistent disk on a paid instance and point `tradeup.storage.upload-dir` at it, or move
uploads to object storage. The same applies to the CSV/JSON exports in `ArchiveService` — download
them promptly rather than treating them as durable.

**The free tier sleeps.** Render spins a free service down after 15 minutes without traffic, and
waking it takes about a minute on top of the roughly 20 seconds Spring Boot needs to start. Fine
for a demo you can warm up beforehand; worth knowing before a live presentation.

---

## Layout

```
backend/    Java 21 · Spring Boot 3.5 · Spring Security · JPA · PostgreSQL
  domain/       entities, and the rules that belong to them
  repository/   Spring Data interfaces + the search Specifications
  service/      business logic, file exports, impact arithmetic
  web/          controllers, DTO records, the error handler
  security/     JWT issuing, the auth filter, current-user access
  bootstrap/    the seed data
frontend/   React 19 · TypeScript · Tailwind 4 · Vite
  components/   the trade ticket, navbar, modal, UI primitives
  pages/        landing, browse, listing, sell, dashboard, inbox, impact, team, moderation
  lib/          typed API client, auth and theme context, formatting
tools/      one-off asset generation
```

## Testing

```bash
cd backend && ./mvnw test
```

29 tests: the listing lifecycle unit-tested directly on the entity, plus end-to-end HTTP
tests covering registration, validation, ownership rules, the offer flow, private messaging,
moderation access control, and that a completed trade actually moves the impact figures.

## API documentation

With the backend running, the full interactive API is at
**http://localhost:8080/swagger-ui.html**.

---

## Design

The identity is **"Adire & Ticket"**. The ground is Yoruba *adire* indigo — the resist-dyed
cloth this campus grew up around — rather than the usual marketplace blue; marigold marks
anything moving up, and leaf green marks a completed trade. Type is Bricolage Grotesque over
Instrument Sans, with JetBrains Mono for reference codes and provenance stamps.

The interface supports light and dark themes, respects `prefers-reduced-motion`, keeps every
control at a 44px touch target, and is responsive from 375px up.

### Regenerating the imagery

The listing photography, hero texture and social card were generated once with OpenAI's
image model and are committed to the repository — you do **not** need an API key to run the
project. To regenerate them:

```bash
OPENAI_API_KEY=sk-... node tools/generate-assets.mjs   # generate
cd frontend && npm run optimize:assets                 # resize + convert to WebP
```

The optimiser takes the raw set from ~23 MB down to ~370 KB. The brand mark itself is
hand-authored SVG (`frontend/public/favicon.svg`), because a logo is exact geometry that has
to stay crisp at 16px.
