/**
 * variant-i18n-stats.js — read-only analysis of product_variants i18n coverage.
 * Run: node scripts/variant-i18n-stats.js
 */
const fs = require("fs");
const path = require("path");
const mongoose = require("mongoose");

function readMongoUri() {
  const env = fs.readFileSync(path.join(__dirname, "..", ".env"), "utf8");
  let uri = (env.match(/MONGO_URI\s*=\s*(.*)/) || [])[1] || "";
  return uri.trim().replace(/\r$/, "").replace(/^["']|["']$/g, "");
}

(async () => {
  await mongoose.connect(readMongoUri());
  const col = mongoose.connection.collection("product_variants");

  const total = await col.countDocuments({});
  const withColorI18n = await col.countDocuments({ color_i18n: { $exists: true, $ne: null } });
  const withName = await col.countDocuments({ name: { $exists: true, $nin: ["", null] } });
  const withVariantName = await col.countDocuments({ variant_name: { $exists: true, $nin: ["", null] } });

  console.log("===== TỔNG QUAN =====");
  console.log("Tổng variant:        ", total);
  console.log("Có color_i18n:       ", withColorI18n, `(${((withColorI18n/total)*100).toFixed(0)}%)`);
  console.log("Có name (≠rỗng):     ", withName);
  console.log("Có variant_name:     ", withVariantName);

  // Distinct spec values (name) and whether each looks "color-only" or has product-spec words.
  const names = (await col.distinct("name")).filter((n) => n && String(n).trim());
  const variantNames = (await col.distinct("variant_name")).filter((n) => n && String(n).trim());

  console.log("\n===== DISTINCT `name` (" + names.length + ") =====");
  names.sort().forEach((n) => console.log("  •", JSON.stringify(n)));

  console.log("\n===== DISTINCT `variant_name` (" + variantNames.length + ") =====");
  variantNames.sort().forEach((n) => console.log("  •", JSON.stringify(n)));

  // Sample a few full docs to see the real shape.
  console.log("\n===== 5 VARIANT MẪU =====");
  const sample = await col.find({}).limit(5).toArray();
  sample.forEach((d) => {
    console.log("  -", JSON.stringify({
      name: d.name, variant_name: d.variant_name, color: d.color,
      color_i18n: d.color_i18n ? Object.keys(d.color_i18n) : null,
      name_i18n: d.name_i18n ? "HAS" : null,
    }));
  });

  await mongoose.disconnect();
})().catch((e) => { console.error("Lỗi:", e.message); process.exit(1); });
