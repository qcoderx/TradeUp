import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ImagePlus, Loader2, X } from "lucide-react";
import { ApiError, assetUrl, request, uploadFiles } from "../lib/api";
import { useApi } from "../lib/useApi";
import { cx, formatPrice } from "../lib/format";
import type { ListingDetail, ReferenceData, TradeIntent } from "../lib/types";
import { Button, Field, Select, TextArea, TextInput } from "../components/ui";

const MAX_PHOTOS = 6;

interface FormState {
  title: string;
  description: string;
  category: string;
  itemCondition: string;
  intent: TradeIntent;
  price: string;
  swapWanted: string;
  pickupLocation: string;
  imageUrls: string[];
}

const BLANK: FormState = {
  title: "",
  description: "",
  category: "TEXTBOOKS",
  itemCondition: "GOOD",
  intent: "SELL",
  price: "",
  swapWanted: "",
  pickupLocation: "",
  imageUrls: [],
};

/**
 * Creating a listing, and editing one.
 *
 * Both use the same form because they are the same shape; the only difference
 * is whether it starts blank or loaded. What the form shows changes with the
 * trade intent, so a swap-only listing never asks for a price it will not use.
 */
export function SellPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const editing = Boolean(id);

  const { data: reference } = useApi<ReferenceData>("/reference");
  const { data: existing } = useApi<ListingDetail>(id ? `/listings/${id}` : null);

  const [form, setForm] = useState<FormState>(BLANK);
  const [busy, setBusy] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  // Fill the form once the listing being edited arrives.
  useEffect(() => {
    if (!existing) return;
    setForm({
      title: existing.title,
      description: existing.description,
      category: existing.categoryName,
      itemCondition: existing.conditionName,
      intent: existing.intentName,
      price: existing.priceKobo === null ? "" : String(existing.priceKobo / 100),
      swapWanted: existing.swapWanted ?? "",
      pickupLocation: existing.pickupLocation ?? "",
      imageUrls: existing.imageUrls,
    });
  }, [existing]);

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  const wantsCash = form.intent === "SELL" || form.intent === "BOTH";
  const wantsSwap = form.intent === "SWAP" || form.intent === "BOTH";

  async function onPickFiles(event: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(event.target.files ?? []);
    event.target.value = ""; // Allow re-picking the same file after a removal.
    if (files.length === 0) return;

    const room = MAX_PHOTOS - form.imageUrls.length;
    if (room <= 0) {
      setError(`You can add up to ${MAX_PHOTOS} photos.`);
      return;
    }

    setUploading(true);
    setError(null);
    try {
      const urls = await uploadFiles(files.slice(0, room));
      set("imageUrls", [...form.imageUrls, ...urls]);
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : "Those photos would not upload.");
    } finally {
      setUploading(false);
    }
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setFieldErrors({});

    const body = {
      title: form.title,
      description: form.description,
      category: form.category,
      itemCondition: form.itemCondition,
      intent: form.intent,
      priceKobo: wantsCash && form.price ? Math.round(Number(form.price) * 100) : null,
      swapWanted: wantsSwap ? form.swapWanted || null : null,
      pickupLocation: form.pickupLocation || null,
      imageUrls: form.imageUrls,
    };

    try {
      const saved = await request<ListingDetail>(editing ? `/listings/${id}` : "/listings", {
        method: editing ? "PUT" : "POST",
        body,
      });
      navigate(`/listings/${saved.id}`, { replace: true });
    } catch (cause) {
      if (cause instanceof ApiError) {
        setFieldErrors(cause.fieldErrors);
        setError(Object.keys(cause.fieldErrors).length > 0 ? null : cause.message);
      } else {
        setError("Could not save that listing.");
      }
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <p className="eyebrow">{editing ? "Editing" : "New listing"}</p>
      <h1 className="mt-2 text-[clamp(1.9rem,4vw,2.75rem)]">
        {editing ? "Update your listing" : "What are you passing on?"}
      </h1>
      <p className="mt-3 max-w-lg text-[0.9375rem] leading-relaxed text-muted">
        Be honest about the wear. A description that matches the item is what makes a handover go smoothly.
      </p>

      <form onSubmit={submit} className="mt-10 flex flex-col gap-6">
        {/* Photos ------------------------------------------------------ */}
        <div>
          <h2 className="text-sm font-semibold text-ink">Photos</h2>
          <p className="mt-1 mb-3 text-xs text-muted">
            Up to {MAX_PHOTOS}. The first one is what people see on the board.
          </p>

          <div className="flex flex-wrap gap-3">
            {form.imageUrls.map((url, index) => (
              <div key={url} className="group relative h-24 w-28 overflow-hidden rounded-lg bg-sunk">
                <img src={assetUrl(url)} alt={`Photo ${index + 1}`} className="h-full w-full object-cover" />
                {index === 0 && (
                  <span className="absolute bottom-1 left-1 rounded bg-ink/80 px-1.5 py-0.5 font-mono text-[0.5625rem] tracking-wide text-paper uppercase">
                    Cover
                  </span>
                )}
                <button
                  type="button"
                  onClick={() => set("imageUrls", form.imageUrls.filter((entry) => entry !== url))}
                  aria-label={`Remove photo ${index + 1}`}
                  className="absolute top-1 right-1 grid h-7 w-7 cursor-pointer place-items-center rounded-full bg-surface/92 text-muted transition-colors duration-200 hover:text-clay"
                >
                  <X className="h-3.5 w-3.5" aria-hidden="true" />
                </button>
              </div>
            ))}

            {form.imageUrls.length < MAX_PHOTOS && (
              <label
                className={cx(
                  "flex h-24 w-28 cursor-pointer flex-col items-center justify-center gap-1.5 rounded-lg",
                  "border border-dashed border-line-strong text-muted transition-colors duration-200",
                  "hover:border-ink hover:text-ink"
                )}
              >
                {uploading ? (
                  <Loader2 className="h-5 w-5 animate-spin" aria-hidden="true" />
                ) : (
                  <ImagePlus className="h-5 w-5" aria-hidden="true" />
                )}
                <span className="text-xs">{uploading ? "Uploading" : "Add photo"}</span>
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  multiple
                  onChange={onPickFiles}
                  disabled={uploading}
                  className="sr-only"
                />
              </label>
            )}
          </div>
        </div>

        <Field label="Title" htmlFor="title" error={fieldErrors.title} required>
          <TextInput
            id="title"
            value={form.title}
            onChange={(event) => set("title", event.target.value)}
            error={fieldErrors.title}
            maxLength={120}
            placeholder="MTH 201 and MTH 202 textbook set"
            required
          />
        </Field>

        <Field
          label="Description"
          htmlFor="description"
          error={fieldErrors.description}
          hint="What it is, how it has been used, and anything a buyer would want to know before meeting you."
          required
        >
          <TextArea
            id="description"
            rows={5}
            value={form.description}
            onChange={(event) => set("description", event.target.value)}
            error={fieldErrors.description}
            maxLength={2000}
            required
          />
        </Field>

        <div className="grid gap-6 sm:grid-cols-2">
          <Field label="Category" htmlFor="category" error={fieldErrors.category} required>
            <Select
              id="category"
              value={form.category}
              onChange={(event) => set("category", event.target.value)}
            >
              {reference?.categories.map((option) => (
                <option key={option.name} value={option.name}>
                  {option.label}
                </option>
              ))}
            </Select>
          </Field>

          <Field
            label="Condition"
            htmlFor="itemCondition"
            error={fieldErrors.itemCondition}
            hint={reference?.conditions.find((c) => c.name === form.itemCondition)?.description ?? undefined}
            required
          >
            <Select
              id="itemCondition"
              value={form.itemCondition}
              onChange={(event) => set("itemCondition", event.target.value)}
            >
              {reference?.conditions.map((option) => (
                <option key={option.name} value={option.name}>
                  {option.label}
                </option>
              ))}
            </Select>
          </Field>
        </div>

        {/* Trade terms ------------------------------------------------- */}
        <div>
          <h2 className="mb-2.5 text-sm font-semibold text-ink">
            Are you selling it, swapping it, or open to either?
          </h2>
          <div className="grid gap-2 sm:grid-cols-3">
            {(
              [
                { value: "SELL", label: "For sale", hint: "Cash only" },
                { value: "SWAP", label: "For swap", hint: "Trade only" },
                { value: "BOTH", label: "Either", hint: "Cash or trade" },
              ] as const
            ).map((option) => (
              <button
                key={option.value}
                type="button"
                onClick={() => set("intent", option.value)}
                aria-pressed={form.intent === option.value}
                className={cx(
                  "cursor-pointer rounded-xl px-4 py-3 text-left transition-colors duration-200",
                  form.intent === option.value
                    ? "bg-ink text-paper"
                    : "bg-sunk text-muted hover:text-ink"
                )}
              >
                <span className="block text-sm font-semibold">{option.label}</span>
                <span
                  className={cx(
                    "mt-0.5 block text-xs",
                    form.intent === option.value ? "text-paper/65" : "text-faint"
                  )}
                >
                  {option.hint}
                </span>
              </button>
            ))}
          </div>
        </div>

        {wantsCash && (
          <Field
            label="Asking price"
            htmlFor="price"
            error={fieldErrors.priceKobo}
            hint={form.price ? `Shown as ${formatPrice(Math.round(Number(form.price) * 100))}.` : "In naira."}
            required
          >
            <TextInput
              id="price"
              type="number"
              inputMode="numeric"
              min={0}
              value={form.price}
              onChange={(event) => set("price", event.target.value)}
              error={fieldErrors.priceKobo}
              placeholder="3500"
              required
              className="tabular"
            />
          </Field>
        )}

        {wantsSwap && (
          <Field
            label="What would you take in exchange?"
            htmlFor="swapWanted"
            error={fieldErrors.swapWanted}
            hint="Be specific. It is the fastest way to get the right offer."
            required={form.intent === "SWAP"}
          >
            <TextInput
              id="swapWanted"
              value={form.swapWanted}
              onChange={(event) => set("swapWanted", event.target.value)}
              error={fieldErrors.swapWanted}
              maxLength={300}
              placeholder="A scientific calculator, or any second year stats text"
              required={form.intent === "SWAP"}
            />
          </Field>
        )}

        <Field
          label="Where can you meet?"
          htmlFor="pickupLocation"
          error={fieldErrors.pickupLocation}
          hint="A hall, a faculty, somewhere public on campus."
        >
          <TextInput
            id="pickupLocation"
            value={form.pickupLocation}
            onChange={(event) => set("pickupLocation", event.target.value)}
            error={fieldErrors.pickupLocation}
            maxLength={80}
            placeholder="Faculty of Science car park"
          />
        </Field>

        {error && (
          <p role="alert" className="rounded-lg bg-clay-soft px-3.5 py-2.5 text-sm text-clay">
            {error}
          </p>
        )}

        <div className="flex flex-wrap gap-3 border-t border-line pt-6">
          <Button type="submit" busy={busy}>
            {editing ? "Save changes" : "Put it on the board"}
          </Button>
          <Button type="button" tone="quiet" onClick={() => navigate(-1)}>
            Cancel
          </Button>
        </div>
      </form>
    </div>
  );
}
