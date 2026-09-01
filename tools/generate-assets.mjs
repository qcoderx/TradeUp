/**
 * TradeUp — brand & content asset generation.
 * Requires OPENAI_API_KEY in the environment. Never commit the key.
 *   OPENAI_API_KEY=sk-... node tools/generate-assets.mjs
 */
import fs from "node:fs";
import path from "node:path";

const KEY = process.env.OPENAI_API_KEY;
if (!KEY) {
  console.error("OPENAI_API_KEY is not set. Export it before running this script.");
  process.exit(1);
}

const OUT = path.resolve("frontend/public/brand");
fs.mkdirSync(OUT, { recursive: true });

// Shared art direction so every asset reads as one system.
const PALETTE =
  "Palette strictly limited to: deep indigo #16204A, mid indigo #2D3F8F, marigold #E9A13B, leaf green #2E9E6B, warm paper #F7F5F0.";
const PHOTO =
  "Editorial product photograph on a seamless warm paper #F7F5F0 background, soft directional daylight from the upper left, gentle natural shadow, shallow depth of field, centered with generous margin, muted realistic colour, no text, no logos, no people, no hands.";

const JOBS = [
  {
    file: "logo-mark.png",
    size: "1024x1024",
    quality: "high",
    background: "transparent",
    prompt:
      "A minimal flat vector app icon: two thick rounded arrows interlocking into a continuous upward-turning loop that reads as exchange and circulation. One arrow deep indigo #16204A, the other marigold #E9A13B. Pure geometry, thick confident strokes, generous negative space, perfectly centered, crisp vector edges, flat with no gradients and no shadows, no text and no letters anywhere. Premium fintech-grade icon design.",
  },
  {
    file: "hero-texture.png",
    size: "1536x1024",
    quality: "high",
    background: "opaque",
    prompt:
      "An abstract textile pattern inspired by Yoruba adire indigo resist-dyeing: irregular hand-tied circular resist motifs, fine crackle lines and soft organic bleed, deep indigo #16204A ground with paler indigo #2D3F8F and small warm paper #F7F5F0 highlights. Very low contrast, softly blurred and atmospheric so it works as a subtle background layer behind white text. Seamless all-over composition, no focal subject, no text.",
  },
  {
    file: "og-card.png",
    size: "1536x1024",
    quality: "high",
    background: "opaque",
    prompt:
      "A premium abstract social share graphic. Deep indigo #16204A field with a soft adire-inspired resist-dye texture, and a large centered geometric mark of two interlocking rounded arrows forming an upward loop in marigold #E9A13B and warm paper #F7F5F0. Cinematic soft lighting, generous negative space, no text and no letters anywhere. " +
      PALETTE,
  },
  // Seed listing photography — the real objects students trade on a Lagos campus.
  { file: "item-textbooks.png", prompt: "A neat stack of five used university textbooks with slightly worn spines and a few colourful sticky-note tabs. " + PHOTO },
  { file: "item-calculator.png", prompt: "A single used scientific calculator lying flat, dark grey casing, faintly worn keys. " + PHOTO },
  { file: "item-desklamp.png", prompt: "A small adjustable metal study desk lamp in muted mustard, arm folded, switched off. " + PHOTO },
  { file: "item-labcoat.png", prompt: "A clean folded white laboratory coat, neatly squared, soft fabric texture visible. " + PHOTO },
  { file: "item-minifridge.png", prompt: "A compact single-door mini refrigerator in soft off-white, door closed, gently used. " + PHOTO },
  { file: "item-bookshelf.png", prompt: "A small empty three-tier wooden bookshelf in warm light oak, shown at a slight three-quarter angle. " + PHOTO },
  { file: "item-drafting.png", prompt: "An open technical drawing set: metal compass, dividers and set squares arranged loosely. " + PHOTO },
  { file: "item-headphones.png", prompt: "A pair of over-ear wired headphones in matte charcoal, folded flat. " + PHOTO },
  { file: "item-backpack.png", prompt: "A used canvas laptop backpack in deep indigo, standing upright, zips closed. " + PHOTO },
  { file: "item-fan.png", prompt: "A small white desk fan with a round cage guard, seen straight on. " + PHOTO },
  { file: "item-whiteboard.png", prompt: "A small blank whiteboard with a slim aluminium frame, leaning at a slight angle. " + PHOTO },
  { file: "item-kettle.png", prompt: "A compact stainless steel electric kettle with a black handle, lid closed. " + PHOTO },
];

async function generate(job) {
  const body = {
    model: job.model ?? "gpt-image-1-mini",
    prompt: job.prompt,
    size: job.size ?? "1024x1024",
    quality: job.quality ?? "medium",
    background: job.background ?? "opaque",
    output_format: "png",
    n: 1,
  };

  const res = await fetch("https://api.openai.com/v1/images/generations", {
    method: "POST",
    headers: { Authorization: `Bearer ${KEY}`, "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  if (!res.ok) throw new Error(`${res.status} ${(await res.text()).slice(0, 300)}`);
  const json = await res.json();
  fs.writeFileSync(path.join(OUT, job.file), Buffer.from(json.data[0].b64_json, "base64"));
  console.log(`ok   ${job.file}`);
}

// Three at a time keeps us well inside the image rate limit.
const queue = [...JOBS];
const failures = [];
await Promise.all(
  Array.from({ length: 3 }, async () => {
    while (queue.length) {
      const job = queue.shift();
      if (fs.existsSync(path.join(OUT, job.file))) {
        console.log(`skip ${job.file} (already generated)`);
        continue;
      }
      try {
        await generate(job);
      } catch (err) {
        console.error(`FAIL ${job.file}: ${err.message}`);
        failures.push(job.file);
      }
    }
  })
);

console.log(failures.length ? `\nDone with ${failures.length} failure(s): ${failures.join(", ")}` : "\nAll assets generated.");
