const mongoose = require("mongoose");
const slugify = require("slugify");

function buildSlug(value) {
  return slugify(String(value || "").trim(), {
    lower: true,
    strict: true,
    locale: "vi",
  });
}

const TranslationSchema = new mongoose.Schema(
  {
    title: { type: String, default: "", trim: true },
    caption: { type: String, default: "", trim: true },
    content: { type: String, default: "" },
    post_category: { type: String, default: "Blog", trim: true },
  },
  { _id: false }
);

const PostSchema = new mongoose.Schema(
  {
    title: { type: String, required: true, trim: true },
    slug: { type: String, trim: true, unique: true, index: true },
    caption: { type: String, default: "", trim: true },
    content: { type: String, default: "" },
    thumbnail_url: { type: String, default: "", trim: true },
    post_category: { type: String, default: "Blog", trim: true },
    status: {
      type: String,
      enum: ["draft", "published"],
      default: "draft",
      index: true,
    },
    published_at: { type: Date, default: null },
    translations: {
      vi: { type: TranslationSchema, default: undefined },
      en: { type: TranslationSchema, default: undefined },
      fr: { type: TranslationSchema, default: undefined },
      zh: { type: TranslationSchema, default: undefined },
    },
    is_seed: { type: Boolean, default: false },
  },
  { timestamps: true, collection: "posts" }
);

PostSchema.pre("validate", function preValidate() {
  if (!this.slug && this.title) {
    this.slug = buildSlug(this.title);
  } else if (this.slug) {
    this.slug = buildSlug(this.slug);
  }

  if (this.status === "published" && !this.published_at) {
    this.published_at = new Date();
  }
});

PostSchema.statics.buildSlug = buildSlug;

module.exports = mongoose.model("Post", PostSchema, "posts");
