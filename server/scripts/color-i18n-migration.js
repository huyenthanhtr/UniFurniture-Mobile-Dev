/**
 * color-i18n-migration.js — seed product_variants.color_i18n (Cách 3)
 * -------------------------------------------------------------------
 * Adds a localized {vi,en,fr,zh} object next to each variant's raw `color` string so the mobile
 * app can show color names in the selected language. The raw `color` is NEVER modified, so any
 * descriptive info ("Giường Màu Trắng 1m6") is preserved.
 *
 * Workflow (auto-translate + human review):
 *   1) node scripts/color-i18n-migration.js            # DRY RUN: writes color-i18n-review.json
 *   2) review / edit color-i18n-review.json by hand     # fix wording, esp. descriptive values
 *   3) node scripts/color-i18n-migration.js --apply     # upsert color_i18n onto matching variants
 *      node scripts/color-i18n-migration.js --apply --from color-i18n-review.json
 *
 * Idempotent: re-running --apply just rewrites color_i18n to the same values.
 *
 * Connection: reads MONGO_URI from server/.env (same as the app backend).
 */
const fs = require("fs");
const path = require("path");
const mongoose = require("mongoose");

const REVIEW_FILE = path.join(__dirname, "color-i18n-review.json");
const COLLECTION = "product_variants";

// Curated "auto-translate" seed for the values currently in the DB. Edit the review file (not this
// table) for one-off corrections; extend this table for values that should always map a certain way.
// Keyed by the EXACT raw `color` string. vi = corrected Vietnamese spelling.
const TRANSLATIONS = {
  "Beige":                        { vi: "Be",            en: "Beige",                 fr: "Beige",                      zh: "米色" },
  "Cam":                          { vi: "Cam",           en: "Orange",                fr: "Orange",                     zh: "橙色" },
  "Camel":                        { vi: "Camel",         en: "Camel",                 fr: "Camel",                      zh: "驼色" },
  "Olive":                        { vi: "Ô liu",         en: "Olive",                 fr: "Olive",                      zh: "橄榄色" },
  "Trắng":                        { vi: "Trắng",         en: "White",                 fr: "Blanc",                      zh: "白色" },
  "Đen":                          { vi: "Đen",           en: "Black",                 fr: "Noir",                       zh: "黑色" },
  "Xám":                          { vi: "Xám",           en: "Gray",                  fr: "Gris",                       zh: "灰色" },
  "vàng":                         { vi: "Vàng",          en: "Yellow",                fr: "Jaune",                      zh: "黄色" },
  "Nâu":                          { vi: "Nâu",           en: "Brown",                 fr: "Marron",                     zh: "棕色" },
  "Nau":                          { vi: "Nâu",           en: "Brown",                 fr: "Marron",                     zh: "棕色" },
  "Màu Nâu":                      { vi: "Nâu",           en: "Brown",                 fr: "Marron",                     zh: "棕色" },
  "Màu nâu":                      { vi: "Nâu",           en: "Brown",                 fr: "Marron",                     zh: "棕色" },
  "Màu Trắng":                    { vi: "Trắng",         en: "White",                 fr: "Blanc",                      zh: "白色" },
  "Xanh Dương":                   { vi: "Xanh dương",    en: "Blue",                  fr: "Bleu",                       zh: "蓝色" },
  "Màu Tự Nhiên":                 { vi: "Tự nhiên",      en: "Natural",               fr: "Naturel",                    zh: "原木色" },
  "Màu Tự nhiên":                 { vi: "Tự nhiên",      en: "Natural",               fr: "Naturel",                    zh: "原木色" },
  "Màu tự nhiên":                 { vi: "Tự nhiên",      en: "Natural",               fr: "Naturel",                    zh: "原木色" },
  "Nâu Be":                       { vi: "Nâu - Be",      en: "Brown - Beige",         fr: "Marron - Beige",             zh: "棕色 - 米色" },
  "Trắng - Xám":                  { vi: "Trắng - Xám",   en: "White - Gray",          fr: "Blanc - Gris",               zh: "白色 - 灰色" },
  "Màu Nâu/Xám":                  { vi: "Nâu / Xám",     en: "Brown / Gray",          fr: "Marron / Gris",              zh: "棕色 / 灰色" },
  "Màu nâu/Nệm xám":              { vi: "Nâu / Nệm xám", en: "Brown / Gray cushion",  fr: "Marron / Coussin gris",      zh: "棕色 / 灰色坐垫" },
  "Nâu Phối Trắng":               { vi: "Nâu phối Trắng",en: "Brown & White",         fr: "Marron et blanc",            zh: "棕色拼白色" },
  "Gỗ Phối Trắng":                { vi: "Gỗ phối Trắng", en: "Wood & White",          fr: "Bois et blanc",              zh: "木色拼白色" },
  "Gỗ phối trắng":                { vi: "Gỗ phối Trắng", en: "Wood & White",          fr: "Bois et blanc",              zh: "木色拼白色" },
  "Combo Nâu":                    { vi: "Combo Nâu",     en: "Brown Combo",           fr: "Combo marron",               zh: "棕色组合" },
  "Combo Màu Tự Nhiên Đệm Be":    { vi: "Combo Tự nhiên Đệm Be", en: "Natural Combo, Beige Cushion", fr: "Combo naturel, coussin beige", zh: "原木色组合 米色坐垫" },
  "Sofa nệm be":                  { vi: "Sofa nệm Be",   en: "Beige Cushion Sofa",    fr: "Canapé coussin beige",       zh: "米色坐垫沙发" },
  "Sofa nệm xám":                 { vi: "Sofa nệm Xám",  en: "Gray Cushion Sofa",     fr: "Canapé coussin gris",        zh: "灰色坐垫沙发" },
  "Giường Màu Trắng 1m6":         { vi: "Giường Trắng 1m6", en: "White Bed 1.6m",     fr: "Lit blanc 1.6m",             zh: "白色床 1.6米" },
  "Giường Màu Trắng 1m8":         { vi: "Giường Trắng 1m8", en: "White Bed 1.8m",     fr: "Lit blanc 1.8m",             zh: "白色床 1.8米" },
  "Giường Trắng 1m6":             { vi: "Giường Trắng 1m6", en: "White Bed 1.6m",     fr: "Lit blanc 1.6m",             zh: "白色床 1.6米" },
  "Giường Trắng 1m8":             { vi: "Giường Trắng 1m8", en: "White Bed 1.8m",     fr: "Lit blanc 1.8m",             zh: "白色床 1.8米" },
  "Giường Tự Nhiên 1m6":          { vi: "Giường Tự nhiên 1m6", en: "Natural Bed 1.6m", fr: "Lit naturel 1.6m",          zh: "原木色床 1.6米" },
};

function readMongoUri() {
  const envPath = path.join(__dirname, "..", ".env");
  const env = fs.readFileSync(envPath, "utf8");
  let uri = (env.match(/MONGO_URI\s*=\s*(.*)/) || [])[1] || "";
  return uri.trim().replace(/\r$/, "").replace(/^["']|["']$/g, "");
}

// Seed proposal for a value not in TRANSLATIONS: vi = original, others = original (flag for review).
function proposeFor(raw) {
  if (TRANSLATIONS[raw]) return { ...TRANSLATIONS[raw], _reviewed: true };
  return { vi: raw, en: raw, fr: raw, zh: raw, _needs_review: true };
}

async function main() {
  const apply = process.argv.includes("--apply");
  const fromIdx = process.argv.indexOf("--from");
  const fromFile = fromIdx !== -1 ? process.argv[fromIdx + 1] : null;

  await mongoose.connect(readMongoUri());
  const col = mongoose.connection.collection(COLLECTION);

  // Build the map of raw color -> {vi,en,fr,zh}
  let mapping;
  if (apply && fromFile) {
    mapping = JSON.parse(fs.readFileSync(fromFile, "utf8"));
    console.log(`Loaded ${Object.keys(mapping).length} entries from ${fromFile}`);
  } else {
    const colors = (await col.distinct("color")).filter((c) => c && String(c).trim() !== "");
    mapping = {};
    for (const c of colors) mapping[c] = proposeFor(c);
  }

  if (!apply) {
    fs.writeFileSync(REVIEW_FILE, JSON.stringify(mapping, null, 2), "utf8");
    const needsReview = Object.values(mapping).filter((m) => m._needs_review).length;
    console.log(`DRY RUN. Wrote ${Object.keys(mapping).length} entries to ${REVIEW_FILE}`);
    if (needsReview) console.log(`⚠️  ${needsReview} value(s) have no curated translation — review them, then run with --apply.`);
    console.log("Re-run with --apply (optionally --from color-i18n-review.json) to write to the DB.");
    await mongoose.disconnect();
    return;
  }

  let updated = 0;
  for (const [raw, t] of Object.entries(mapping)) {
    const color_i18n = { vi: t.vi || raw, en: t.en || raw, fr: t.fr || raw, zh: t.zh || raw };
    const res = await col.updateMany({ color: raw }, { $set: { color_i18n } });
    updated += res.modifiedCount;
    console.log(`  "${raw}" -> ${JSON.stringify(color_i18n)}  (${res.matchedCount} matched)`);
  }
  console.log(`APPLIED. Updated ${updated} variant document(s).`);
  await mongoose.disconnect();
}

main().catch((e) => {
  console.error("Migration failed:", e.message);
  process.exit(1);
});
