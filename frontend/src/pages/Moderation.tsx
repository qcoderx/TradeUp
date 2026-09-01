import { useState } from "react";
import { Link } from "react-router-dom";
import { Check, Download, FileDown, ShieldCheck, X } from "lucide-react";
import { ApiError, request } from "../lib/api";
import { getToken } from "../lib/api";
import { useApi } from "../lib/useApi";
import { cx, timeAgo } from "../lib/format";
import type { ReportView } from "../lib/types";
import { Badge, Button, Card, EmptyState, ErrorState, Skeleton, TextArea } from "../components/ui";

type Queue = "OPEN" | "UPHELD" | "DISMISSED";

const TABS: Array<{ value: Queue; label: string }> = [
  { value: "OPEN", label: "Open" },
  { value: "UPHELD", label: "Upheld" },
  { value: "DISMISSED", label: "Dismissed" },
];

export function Moderation() {
  const [queue, setQueue] = useState<Queue>("OPEN");
  const { data, error, loading, reload } = useApi<ReportView[]>(`/admin/reports?status=${queue}`, [queue]);

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="eyebrow">Moderation</p>
          <h1 className="mt-2 text-[clamp(1.9rem,4vw,2.75rem)]">The report queue</h1>
        </div>
        <ExportControls />
      </header>

      <div className="mt-8 flex gap-2" role="tablist" aria-label="Report status">
        {TABS.map((tab) => (
          <button
            key={tab.value}
            type="button"
            role="tab"
            aria-selected={queue === tab.value}
            onClick={() => setQueue(tab.value)}
            className={cx(
              "cursor-pointer rounded-lg px-4 py-2 text-sm font-semibold transition-colors duration-200",
              queue === tab.value ? "bg-ink text-paper" : "bg-sunk text-muted hover:text-ink"
            )}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="mt-6">
        {error ? (
          <ErrorState message={error.message} onRetry={reload} />
        ) : loading ? (
          <div className="flex flex-col gap-3">
            {Array.from({ length: 3 }, (_, index) => (
              <Skeleton key={index} className="h-36 w-full rounded-xl" />
            ))}
          </div>
        ) : data && data.length === 0 ? (
          <EmptyState
            icon={<ShieldCheck className="h-8 w-8" aria-hidden="true" />}
            title={queue === "OPEN" ? "Nothing waiting" : "Nothing here"}
            description={
              queue === "OPEN"
                ? "No listings have been flagged. The board is clear."
                : "No reports have been resolved this way yet."
            }
          />
        ) : (
          <div className="flex flex-col gap-3">
            {data?.map((report) => (
              <ReportRow key={report.id} report={report} onResolved={reload} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function ReportRow({ report, onResolved }: { report: ReportView; onResolved: () => void }) {
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState<"uphold" | "dismiss" | null>(null);
  const [error, setError] = useState<string | null>(null);

  const open = report.status === "OPEN";

  async function decide(action: "uphold" | "dismiss") {
    setBusy(action);
    setError(null);
    try {
      await request(`/admin/reports/${report.id}/${action}`, { method: "POST", body: { note } });
      onResolved();
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : "That did not work.");
      setBusy(null);
    }
  }

  return (
    <Card>
      <div className="flex flex-wrap items-start gap-4">
        {report.listingImageUrl && (
          <img
            src={report.listingImageUrl}
            alt=""
            className="h-16 w-16 shrink-0 rounded-lg object-cover"
            loading="lazy"
          />
        )}

        <div className="min-w-48 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <Badge tone="clay">{report.reasonLabel}</Badge>
            <Badge tone={report.listingStatus === "FLAGGED" ? "clay" : "neutral"}>
              Listing {report.listingStatus.toLowerCase()}
            </Badge>
            <span className="text-xs text-muted">{timeAgo(report.createdAt)}</span>
          </div>

          <p className="mt-2">
            <Link
              to={`/listings/${report.listingId}`}
              className="cursor-pointer font-semibold text-ink hover:underline"
            >
              {report.listingTitle}
            </Link>
            <span className="ml-2 font-mono text-xs text-faint">{report.listingReference}</span>
          </p>

          <p className="mt-1.5 text-xs text-muted">
            Listed by {report.listingOwner.fullName} · reported by {report.reporter.fullName}
          </p>

          {report.details && (
            <p className="mt-3 rounded-lg bg-sunk px-3.5 py-2.5 text-sm leading-relaxed text-muted">
              {report.details}
            </p>
          )}

          {!open && report.moderatorNote && (
            <p className="mt-3 border-l-2 border-line pl-3 text-sm leading-relaxed text-muted italic">
              “{report.moderatorNote}”
            </p>
          )}
        </div>
      </div>

      {open && (
        <div className="mt-5 flex flex-col gap-3 border-t border-line pt-4">
          <label htmlFor={`note-${report.id}`} className="sr-only">
            Note on this decision
          </label>
          <TextArea
            id={`note-${report.id}`}
            rows={2}
            value={note}
            onChange={(event) => setNote(event.target.value)}
            maxLength={500}
            placeholder="Optional note explaining the decision."
          />

          {error && (
            <p role="alert" className="text-sm text-clay">
              {error}
            </p>
          )}

          <div className="flex flex-wrap gap-2.5">
            <Button
              tone="outline"
              busy={busy === "dismiss"}
              disabled={busy !== null}
              onClick={() => decide("dismiss")}
            >
              <X className="h-4 w-4" aria-hidden="true" />
              Dismiss, listing is fine
            </Button>
            <Button busy={busy === "uphold"} disabled={busy !== null} onClick={() => decide("uphold")}>
              <Check className="h-4 w-4" aria-hidden="true" />
              Uphold and pull it down
            </Button>
          </div>
        </div>
      )}
    </Card>
  );
}

/**
 * The file exports.
 *
 * Downloading needs the bearer token on the request, so the file is fetched as
 * a blob and handed to a temporary link rather than being opened directly.
 */
function ExportControls() {
  const [busy, setBusy] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  async function exportAs(format: "csv" | "json") {
    setBusy(format);
    setMessage(null);
    try {
      const result = await request<{ file: string }>(`/admin/archives/${format}`, { method: "POST" });
      await download(result.file);
      setMessage(`Wrote ${result.file}`);
    } catch (cause) {
      setMessage(cause instanceof ApiError ? cause.message : "The export failed.");
    } finally {
      setBusy(null);
    }
  }

  async function download(fileName: string) {
    const token = getToken();
    const response = await fetch(`/api/admin/archives/${encodeURIComponent(fileName)}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    if (!response.ok) return;

    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  }

  return (
    <div className="flex flex-col items-end gap-1.5">
      <div className="flex gap-2">
        <Button tone="outline" busy={busy === "csv"} onClick={() => exportAs("csv")}>
          <FileDown className="h-4 w-4" aria-hidden="true" />
          Export CSV
        </Button>
        <Button tone="outline" busy={busy === "json"} onClick={() => exportAs("json")}>
          <Download className="h-4 w-4" aria-hidden="true" />
          Export JSON
        </Button>
      </div>
      {message && (
        <p role="status" className="font-mono text-xs text-muted">
          {message}
        </p>
      )}
    </div>
  );
}
