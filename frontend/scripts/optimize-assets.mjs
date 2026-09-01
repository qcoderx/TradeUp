/**
 * TradeUp — asset optimisation.
 *
 * The image model returns large lossless PNGs, which are the wrong thing to ship:
 * the raw set is over 20 MB. This resizes each asset to the largest size the UI
 * ever displays it at, converts it to WebP, and removes the original once the
 * WebP is on disk. Re-run tools/generate-assets.mjs if you need the originals.
 *
 *   npm run optimize:assets        (from the frontend directory)
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import sharp from "sharp";

const FRONTEND = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const PUBLIC = path.join(FRONTEND, "public");
const BRAND = path.join(PUBLIC, "brand");

/** The longest edge each asset is ever rendered at, plus headroom for 2x screens. */
const TARGETS = [
  { match: /^hero-texture\.png$/, width: 1800, quality: 72 },
  { match: /^og-card\.png$/, width: 1200, quality: 82 },
  { match: /^item-.*\.png$/, width: 900, quality: 78 },
];

if (!fs.existsSync(BRAND)) {
  console.error(`No brand directory at ${BRAND}. Run tools/generate-assets.mjs first.`);
  process.exit(1);
}

let savedBytes = 0;
let converted = 0;

for (const file of fs.readdirSync(BRAND)) {
  if (!file.endsWith(".png")) continue;

  const target = TARGETS.find((t) => t.match.test(file));
  if (!target) {
    // The generated logo mark is superseded by the hand-authored SVG.
    fs.rmSync(path.join(BRAND, file));
    console.log(`drop ${file}`);
    continue;
  }

  const source = path.join(BRAND, file);
  const destination = source.replace(/\.png$/, ".webp");
  const before = fs.statSync(source).size;

  await sharp(source)
    .resize({ width: target.width, withoutEnlargement: true })
    .webp({ quality: target.quality, effort: 6 })
    .toFile(destination);

  const after = fs.statSync(destination).size;
  fs.rmSync(source);

  savedBytes += before - after;
  converted++;
  console.log(`ok   ${file} -> ${path.basename(destination)}  ${kb(before)} -> ${kb(after)}`);
}

// Raster icons are rendered from the hand-authored mark so they stay crisp.
const faviconSource = path.join(PUBLIC, "favicon.svg");
if (fs.existsSync(faviconSource)) {
  for (const [name, size] of [
    ["apple-touch-icon.png", 180],
    ["icon-512.png", 512],
  ]) {
    await sharp(faviconSource, { density: 384 })
      .resize(size, size)
      .png({ compressionLevel: 9 })
      .toFile(path.join(PUBLIC, name));
    console.log(`ok   ${name}`);
  }
}

console.log(`\nConverted ${converted} images, saved ${kb(savedBytes)}.`);

function kb(bytes) {
  return bytes > 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)} MB` : `${Math.round(bytes / 1024)} KB`;
}
