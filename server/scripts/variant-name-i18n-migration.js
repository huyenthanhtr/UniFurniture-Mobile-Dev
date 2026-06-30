/**
 * variant-name-i18n-migration.js — seed product_variants.name_i18n (Cách 3, spec part)
 * ----------------------------------------------------------------------------------
 * Adds a localized {vi,en,fr,zh} object next to each variant's spec string (the NON-color part of
 * the label: combos, bed sizes, cabinet configs, chair sets, ...) so the mobile app can show the
 * full variant label in the selected language. The raw `name` / `variant_name` are NEVER modified.
 *
 * The "spec source" per document mirrors the mobile app (FormatUtil.getVariantLabel):
 *     specRaw = variant_name || name   (then the redundant leading "Combo Bàn Ăn " is stripped)
 * so a translation produced here lines up exactly with what the app displays.
 *
 * Workflow (auto-translate + human review) — identical to color-i18n-migration.js:
 *   1) node scripts/variant-name-i18n-migration.js              # DRY RUN -> variant-name-i18n-review.json
 *   2) review / edit variant-name-i18n-review.json by hand      # fix wording; clear "_needs_review"
 *   3) node scripts/variant-name-i18n-migration.js --apply --from variant-name-i18n-review.json
 *      node scripts/variant-name-i18n-migration.js --apply       # apply auto-proposals directly
 *
 * Idempotent: re-running --apply rewrites name_i18n to the same values.
 * Connection: reads MONGO_URI from server/.env (same as the app backend).
 */
const fs = require("fs");
const path = require("path");
const mongoose = require("mongoose");

const REVIEW_FILE = path.join(__dirname, "variant-name-i18n-review.json");
const COLLECTION = "product_variants";
const COMBO_PREFIX = "Combo Bàn Ăn "; // matches FormatUtil's strip

// --- Furniture vocabulary -------------------------------------------------------------------
// Keys are lowercased WITH diacritics (đơn vs đôn must stay distinct). Phrases (multi-word) are
// matched before single words. Output is { en, fr, zh }; Vietnamese keeps the (corrected) source.
const PHRASES = {
  "ghế băng dài":      { en: "Long bench",      fr: "Banc long",          zh: "长凳" },
  "ghế băng tựa":      { en: "Backrest bench",  fr: "Banc à dossier",     zh: "靠背长凳" },
  "băng dài":          { en: "Long bench",      fr: "Banc long",          zh: "长凳" },
  "băng tựa":          { en: "Backrest bench",  fr: "Banc à dossier",     zh: "靠背长凳" },
  "ghế ăn":            { en: "Dining chair",    fr: "Chaise à manger",    zh: "餐椅" },
  "ghế đơn":           { en: "Single chair",    fr: "Chaise simple",      zh: "单椅" },
  "ghế đôn":           { en: "Stool",           fr: "Tabouret",           zh: "凳子" },
  "ghế bành":          { en: "Armchair",        fr: "Fauteuil",           zh: "扶手椅" },
  "ghế sofa góc":      { en: "Corner sofa",     fr: "Canapé d'angle",     zh: "转角沙发" },
  "ghế sofa":          { en: "Sofa chair",      fr: "Fauteuil",           zh: "沙发椅" },
  "sofa góc":          { en: "Corner sofa",     fr: "Canapé d'angle",     zh: "转角沙发" },
  "không tay tựa":     { en: "armless",         fr: "sans accoudoir",     zh: "无扶手" },
  "tay tựa":           { en: "with armrests",   fr: "avec accoudoirs",    zh: "扶手" },
  "bàn ăn":            { en: "Dining table",    fr: "Table à manger",     zh: "餐桌" },
  "bàn cafe":          { en: "Coffee table",    fr: "Table basse",        zh: "咖啡桌" },
  "bàn cà phê":        { en: "Coffee table",    fr: "Table basse",        zh: "咖啡桌" },
  "tủ tv":             { en: "TV cabinet",      fr: "Meuble TV",          zh: "电视柜" },
  "tủ quần áo":        { en: "Wardrobe",        fr: "Armoire",            zh: "衣柜" },
  "tủ đầu giường":     { en: "Nightstand",      fr: "Table de chevet",    zh: "床头柜" },
  "ngăn kệ":           { en: "shelf",           fr: "étagère",            zh: "搁板" },
  "thanh treo":        { en: "hanging rail",    fr: "tringle",            zh: "挂衣杆" },
  "hộc ngăn kéo":      { en: "drawer",          fr: "tiroir",             zh: "抽屉" },
  "hộc kéo":           { en: "drawer",          fr: "tiroir",             zh: "抽屉" },
  "ngăn kéo":          { en: "drawer",          fr: "tiroir",             zh: "抽屉" },
  "kệ góc":            { en: "corner shelf",    fr: "étagère d'angle",    zh: "转角架" },
  "tấm phản":          { en: "platform",        fr: "plateau",            zh: "床板" },
  "đầu giường":        { en: "bedside",         fr: "chevet",             zh: "床头" },
  "quần áo":           { en: "clothes",         fr: "vêtements",          zh: "衣物" },
  "cao thông thường":  { en: "Standard height", fr: "Hauteur standard",   zh: "标准高度" },
  "đầy đủ":            { en: "Full",            fr: "Complet",            zh: "全套" },
  "tự nhiên":          { en: "Natural",         fr: "Naturel",            zh: "原木色" },
  "tu nhien":          { en: "Natural",         fr: "Naturel",            zh: "原木色" }, // no-diacritic alias
  "xanh dương":        { en: "Blue",            fr: "Bleu",               zh: "蓝色" },
  "xanh lá":           { en: "Green",           fr: "Vert",               zh: "绿色" },
  "vải xanh":          { en: "Blue fabric",     fr: "Tissu bleu",         zh: "蓝色布艺" },
  "ô liu":             { en: "Olive",           fr: "Olive",              zh: "橄榄色" },
  "default title":     { en: "Default",         fr: "Défaut",             zh: "默认" },
};

const WORDS = {
  "giường":  { en: "Bed",       fr: "Lit",        zh: "床" },
  "tủ":      { en: "Cabinet",   fr: "Meuble",     zh: "柜" },
  "ghế":     { en: "chair",     fr: "chaise",     zh: "椅" },
  "băng":    { en: "bench",     fr: "banc",       zh: "长凳" },
  "cánh":    { en: "door",      fr: "porte",      zh: "门" },
  "kệ":      { en: "shelf",     fr: "étagère",    zh: "架" },
  "hộc":     { en: "drawer",    fr: "tiroir",     zh: "抽屉" },
  "ngăn":    { en: "compartment", fr: "compartiment", zh: "格" },
  "sofa":    { en: "Sofa",      fr: "Canapé",     zh: "沙发" },
  "đệm":     { en: "cushion",   fr: "coussin",    zh: "坐垫" },
  "nệm":     { en: "cushion",   fr: "coussin",    zh: "坐垫" },
  "bộ":      { en: "Set of",    fr: "Lot de",     zh: "套" },
  "set":     { en: "Set",       fr: "Ensemble",   zh: "套" },
  "combo":   { en: "Combo",     fr: "Combo",      zh: "组合" },
  "full":    { en: "Full",      fr: "Complet",    zh: "全套" },
  "basic":   { en: "Basic",     fr: "Basic",      zh: "基础" },
  "mix":     { en: "Mix",       fr: "Mix",        zh: "混搭" },
  "nat":     { en: "Natural",   fr: "Naturel",    zh: "原木色" }, // "Combo Nat" abbreviation
  "cao":     { en: "Tall",      fr: "Haut",       zh: "高" },
  "gỗ":      { en: "Wood",      fr: "Bois",       zh: "木色" },
  "tv":      { en: "TV",        fr: "TV",         zh: "电视" },
  "đôi":     { en: "double",    fr: "double",     zh: "双" },
  "đôn":     { en: "stool",     fr: "tabouret",   zh: "凳子" },
  "kèm":     { en: "with",      fr: "avec",       zh: "配" },
  "vải":     { en: "fabric",    fr: "tissu",      zh: "布艺" },
  "trái":    { en: "left",      fr: "gauche",     zh: "左" },
  "phải":    { en: "right",     fr: "droite",     zh: "右" },
  "ver":     { en: "Version",   fr: "Version",    zh: "版本" },
  // colors (single word)
  "trắng":   { en: "White",     fr: "Blanc",      zh: "白色" },
  "đen":     { en: "Black",     fr: "Noir",       zh: "黑色" },
  "xám":     { en: "Gray",      fr: "Gris",       zh: "灰色" },
  "ghi":     { en: "Gray",      fr: "Gris",       zh: "灰色" },
  "nâu":     { en: "Brown",     fr: "Marron",     zh: "棕色" },
  "nau":     { en: "Brown",     fr: "Marron",     zh: "棕色" }, // no-diacritic alias
  "be":      { en: "Beige",     fr: "Beige",      zh: "米色" },
  "kem":     { en: "Cream",     fr: "Crème",      zh: "奶油色" },
  "cam":     { en: "Orange",    fr: "Orange",     zh: "橙色" },
  "vàng":    { en: "Yellow",    fr: "Jaune",      zh: "黄色" },
  "xanh":    { en: "Blue",      fr: "Bleu",       zh: "蓝色" },
  "đỏ":      { en: "Red",       fr: "Rouge",      zh: "红色" },
  "hồng":    { en: "Pink",      fr: "Rose",       zh: "粉色" },
  "tím":     { en: "Purple",    fr: "Violet",     zh: "紫色" },
  "navy":    { en: "Navy",      fr: "Marine",     zh: "藏青色" },
  "camel":   { en: "Camel",     fr: "Camel",      zh: "驼色" },
  "olive":   { en: "Olive",     fr: "Olive",      zh: "橄榄色" },
  "trơn":    { en: "plain",     fr: "uni",        zh: "纯色" },
  "nhạt":    { en: "light",     fr: "clair",      zh: "浅" },
  "đậm":     { en: "dark",      fr: "foncé",      zh: "深" },
};

// Countable nouns: when preceded by a number we pluralize (en/fr) and add a Chinese measure word.
// Forms: en/fr = [singular, plural]; zh = noun, c = zh measure word ("" if none).
const COUNTABLE = {
  // Multi-word chair/bench types (checked before the bare "ghế"/"băng" so "4 Ghế Đơn" pluralizes whole).
  "ghế băng dài": { en: ["long bench", "long benches"], fr: ["banc long", "bancs longs"], zh: "长凳", c: "条" },
  "ghế băng tựa": { en: ["backrest bench", "backrest benches"], fr: ["banc à dossier", "bancs à dossier"], zh: "靠背长凳", c: "条" },
  "ghế ăn":   { en: ["dining chair", "dining chairs"], fr: ["chaise à manger", "chaises à manger"], zh: "餐椅", c: "把" },
  "ghế đơn":  { en: ["single chair", "single chairs"], fr: ["chaise simple", "chaises simples"], zh: "单椅", c: "把" },
  "ghế đôn":  { en: ["stool", "stools"], fr: ["tabouret", "tabourets"], zh: "凳子", c: "个" },
  "ghế bành": { en: ["armchair", "armchairs"], fr: ["fauteuil", "fauteuils"], zh: "扶手椅", c: "把" },
  "băng dài": { en: ["long bench", "long benches"], fr: ["banc long", "bancs longs"], zh: "长凳", c: "条" },
  "băng tựa": { en: ["backrest bench", "backrest benches"], fr: ["banc à dossier", "bancs à dossier"], zh: "靠背长凳", c: "条" },
  "ngăn kéo": { en: ["drawer", "drawers"], fr: ["tiroir", "tiroirs"], zh: "抽屉", c: "个" },
  "hộc kéo":  { en: ["drawer", "drawers"], fr: ["tiroir", "tiroirs"], zh: "抽屉", c: "个" },
  "ghế":      { en: ["chair", "chairs"], fr: ["chaise", "chaises"], zh: "椅子", c: "把" },
  "cánh":     { en: ["door", "doors"],   fr: ["porte", "portes"],   zh: "门",   c: "扇" },
  "món":      { en: ["item", "items"],   fr: ["pièce", "pièces"],   zh: "件",   c: "" },
  "băng":     { en: ["bench", "benches"],fr: ["banc", "bancs"],     zh: "长凳", c: "条" },
};

// Dropped (no product meaning), mirroring ColorUi. "mau" = no-diacritic "màu".
const FILLER = new Set(["màu", "phối", "mau"]);

// Known proper names / brand tokens — kept verbatim, NOT flagged for review.
const MODEL_NAMES = new Set([
  "oslo", "milan", "fyn", "nexo", "odessa", "soro", "malaga", "hobro",
  "verona", "viborg", "vline", "plank", "fingal", "lyh", "olly", "lounge", "chair",
  "u-home", "furni", "signature",
]);

const lc = (s) => String(s).toLowerCase();

// "1m6"->"1.6m", "1m25"->"1.25m", "1m"->"1m" ; returns null if not a size token.
function sizeToken(tok) {
  const m = /^(\d+)m(\d*)$/i.exec(tok);
  if (!m) return null;
  return m[2] ? `${m[1]}.${m[2]}m` : `${m[1]}m`;
}

const isNumberLike = (tok) => /^\d+([.,]\d+)?(cm|mm|m)?$/i.test(tok);
// Internal alphanumeric code kept verbatim (BS, MS, WR120, V1, PN, SMH, H60, S60...).
const isCode = (tok) => /^[A-Z]{1,4}\d*$/.test(tok) && !MODEL_NAMES.has(lc(tok));

// Render "<n> <countable>" in one language with plural/measure-word handling.
function renderCount(n, cnt, lang) {
  if (lang === "zh") return `${n} ${cnt.c}${cnt.zh}`.replace(/\s+/g, " ").trim();
  const forms = cnt[lang];
  return `${n} ${n === "1" ? forms[0] : forms[1]}`;
}

// Translate one segment (no '/','+',' - ' separators inside) into one target language.
// Returns { text, unknown:[...] }. `unknown` lists tokens we could not confidently map.
function translateSegment(seg, lang) {
  // Tokenize: words (letters/digits/hyphen) and bracket/comma/colon punctuation as separate tokens.
  const tokens = seg.match(/[\p{L}\p{N}][\p{L}\p{N}-]*|[(),:]/gu) || [];
  const out = [];
  const unknown = [];
  for (let i = 0; i < tokens.length; i++) {
    const tok = tokens[i];
    const key1 = lc(tok);

    if (/^[(),:]$/.test(tok)) { out.push(tok); continue; } // punctuation, fixed up later
    if (FILLER.has(key1)) continue;

    // A plain integer: try to attach a following countable noun ("6 Ghế" -> "6 chairs").
    if (/^\d+$/.test(tok)) {
      let cnt = null, consumed = 0;
      for (let span = 3; span >= 1; span--) {
        const k = tokens.slice(i + 1, i + 1 + span).map(lc).join(" ");
        if (COUNTABLE[k]) { cnt = COUNTABLE[k]; consumed = span; break; }
      }
      if (cnt) { out.push(renderCount(tok, cnt, lang)); i += consumed; continue; }
      out.push(tok);
      continue;
    }

    // 3- then 2-word phrase lookahead.
    let matched = false;
    for (let span = Math.min(3, tokens.length - i); span >= 2; span--) {
      const phraseKey = tokens.slice(i, i + span).map(lc).join(" ");
      if (PHRASES[phraseKey]) {
        out.push(PHRASES[phraseKey][lang]);
        i += span - 1;
        matched = true;
        break;
      }
    }
    if (matched) continue;

    if (WORDS[key1]) { out.push(WORDS[key1][lang]); continue; }

    const size = sizeToken(tok);
    if (size) { out.push(size); continue; }
    if (isNumberLike(tok)) { out.push(tok); continue; }
    if (MODEL_NAMES.has(key1)) { out.push(tok); continue; } // proper name verbatim
    if (isCode(tok)) { out.push(tok); continue; }           // internal code verbatim
    out.push(tok);
    if (/\p{L}/u.test(tok)) unknown.push(tok);              // unrecognized word -> flag
  }
  // Re-attach punctuation: ") , :" hug the previous word; "(" hugs the next.
  const text = out.join(" ")
    .replace(/\s+([),:])/g, "$1")
    .replace(/\(\s+/g, "(")
    .replace(/\s{2,}/g, " ")
    .trim();
  return { text, unknown };
}

// Translate a full spec string (with '/','+',' - ' separators preserved) into one language.
function translateSpec(raw, lang) {
  const parts = String(raw).split(/(\s*[\/+]\s*|\s+-\s+)/); // keep separators as tokens
  let unknown = [];
  const text = parts
    .map((seg) => {
      if (/^\s*[\/+]\s*$/.test(seg)) return " / ";
      if (/^\s+-\s+$/.test(seg)) return " - ";
      const r = translateSegment(seg, lang);
      unknown = unknown.concat(r.unknown);
      return r.text;
    })
    .join("")
    .replace(/\s*\/\s*/g, " / ")
    .replace(/\s{2,}/g, " ")
    .trim();
  return { text, unknown };
}

function proposeFor(specRaw) {
  const en = translateSpec(specRaw, "en");
  const fr = translateSpec(specRaw, "fr");
  const zh = translateSpec(specRaw, "zh");
  const unknown = [...new Set([...en.unknown, ...fr.unknown, ...zh.unknown])];
  const entry = { vi: specRaw, en: en.text, fr: fr.text, zh: zh.text };
  if (unknown.length) entry._needs_review = unknown; // human should confirm these tokens
  return entry;
}

// specRaw for one document — must mirror FormatUtil.getVariantLabel.
function specSource(doc) {
  let s = (doc.variant_name && String(doc.variant_name).trim())
       || (doc.name && String(doc.name).trim())
       || "";
  if (s.startsWith(COMBO_PREFIX)) s = s.slice(COMBO_PREFIX.length);
  return s.trim();
}

function readMongoUri() {
  const env = fs.readFileSync(path.join(__dirname, "..", ".env"), "utf8");
  let uri = (env.match(/MONGO_URI\s*=\s*(.*)/) || [])[1] || "";
  return uri.trim().replace(/\r$/, "").replace(/^["']|["']$/g, "");
}

async function main() {
  const apply = process.argv.includes("--apply");
  const fromIdx = process.argv.indexOf("--from");
  const fromFile = fromIdx !== -1 ? process.argv[fromIdx + 1] : null;

  await mongoose.connect(readMongoUri());
  const col = mongoose.connection.collection(COLLECTION);

  let mapping;
  if (apply && fromFile) {
    mapping = JSON.parse(fs.readFileSync(fromFile, "utf8"));
    console.log(`Loaded ${Object.keys(mapping).length} entries from ${fromFile}`);
  } else {
    // Build distinct spec sources across all docs (handles variant_name vs name per document).
    const docs = await col.find({}, { projection: { name: 1, variant_name: 1 } }).toArray();
    const specs = new Set();
    for (const d of docs) {
      const s = specSource(d);
      if (s) specs.add(s);
    }
    mapping = {};
    for (const s of [...specs].sort()) mapping[s] = proposeFor(s);
  }

  if (!apply) {
    fs.writeFileSync(REVIEW_FILE, JSON.stringify(mapping, null, 2), "utf8");
    const needsReview = Object.values(mapping).filter((m) => m._needs_review).length;
    console.log(`DRY RUN. Wrote ${Object.keys(mapping).length} spec entries to ${REVIEW_FILE}`);
    if (needsReview) {
      console.log(`⚠️  ${needsReview} entry(ies) contain tokens (model codes / unknown words) flagged in "_needs_review".`);
      console.log("    Edit those, remove the _needs_review key, then run with --apply.");
    }
    await mongoose.disconnect();
    return;
  }

  // Apply: walk every doc, compute its spec source, set name_i18n from the mapping.
  const docs = await col.find({}, { projection: { name: 1, variant_name: 1 } }).toArray();
  let updated = 0, missing = 0;
  for (const d of docs) {
    const s = specSource(d);
    if (!s) continue;
    const t = mapping[s];
    if (!t) { missing++; continue; }
    const name_i18n = { vi: t.vi || s, en: t.en || s, fr: t.fr || s, zh: t.zh || s };
    const res = await col.updateOne({ _id: d._id }, { $set: { name_i18n } });
    updated += res.modifiedCount;
  }
  console.log(`APPLIED. Updated ${updated} variant document(s).`);
  if (missing) console.log(`(${missing} doc(s) had a spec with no mapping entry — re-run dry run if the data changed.)`);
  await mongoose.disconnect();
}

main().catch((e) => { console.error("Migration failed:", e.message); process.exit(1); });
