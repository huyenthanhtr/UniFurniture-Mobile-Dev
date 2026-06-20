const mongoose = require("mongoose");

const ProductTranslationSchema = new mongoose.Schema(
  {
    product_id: { type: mongoose.Schema.Types.ObjectId, ref: "Product", required: true },
    language_code: { type: String, required: true, trim: true },
    name: { type: String, required: true, trim: true },
    short_description: { type: String, default: "" },
    description: { type: String, default: "" },
  },
  { timestamps: true, collection: "product_translations" }
);

// Ensure unique translation for each product per language
ProductTranslationSchema.index({ product_id: 1, language_code: 1 }, { unique: true });

module.exports = mongoose.model("ProductTranslation", ProductTranslationSchema, "product_translations");
