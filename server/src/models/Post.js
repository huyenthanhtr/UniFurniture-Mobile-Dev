const mongoose = require("mongoose");
const slugify = require("slugify");

function buildSlug(value) {
  return slugify(String(value || "").trim(), {
    lower: true,
    strict: true,
    locale: "vi",
  });
}

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
  },
  { timestamps: true, collection: "posts" }
);

PostSchema.pre("validate", function preValidate(next) {
  if (!this.slug && this.title) {
    this.slug = buildSlug(this.title);
  } else if (this.slug) {
    this.slug = buildSlug(this.slug);
  }

  if (this.status === "published" && !this.published_at) {
    this.published_at = new Date();
  }

  next();
});

PostSchema.statics.buildSlug = buildSlug;

module.exports = mongoose.model("Post", PostSchema, "posts");
