const mongoose = require("mongoose");
const Wishlist = require("../models/Wishlist");
const Product = require("../models/Product");
const ProductImage = require("../models/ProductImage");
const Customer = require("../models/Customer");

function asObjectId(value) {
  const raw = String(value || "").trim();
  if (!raw || !mongoose.Types.ObjectId.isValid(raw)) return null;
  return new mongoose.Types.ObjectId(raw);
}

async function resolveOrCreateCustomerId(idRaw) {
  const id = asObjectId(idRaw);
  if (!id) return null;

  // 1. Try to find if this is a Profile ID
  const profile = await mongoose.model("Profile").findById(id);
  if (profile) {
    if (profile.customer_id && mongoose.Types.ObjectId.isValid(String(profile.customer_id))) {
      return profile.customer_id;
    }
    // No Customer document linked to this Profile yet, create one!
    const CustomerModel = mongoose.model("Customer");
    const { generateCustomerCode } = require("../utils/code-generator");
    const customerCode = await generateCustomerCode();
    const customer = await CustomerModel.create({
      customer_code: customerCode,
      full_name: profile.full_name || "Member",
      phone: profile.phone || "",
      customer_type: "member",
      status: "active"
    });
    profile.customer_id = customer._id;
    await profile.save();
    return customer._id;
  }

  // 2. Otherwise, check if it's already a Customer ID
  const customerDoc = await mongoose.model("Customer").findById(id).select("_id").lean();
  if (customerDoc) {
    return customerDoc._id;
  }

  return null;
}

function toMoney(value, fallback = 0) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed < 0) return Math.max(0, Number(fallback || 0));
  return parsed;
}

function mapWishlistItem(doc) {
  return {
    _id: String(doc?._id || ""),
    customer_id: String(doc?.customer_id || ""),
    account_name: String(doc?.account_name || ""),
    account_phone: String(doc?.account_phone || ""),
    product_id: String(doc?.product_id || ""),
    product_slug: String(doc?.product_slug || ""),
    name: String(doc?.name || "Sản phẩm"),
    image_url: String(doc?.image_url || ""),
    sale_price: toMoney(doc?.sale_price, 0),
    price: toMoney(doc?.price, 0),
    createdAt: doc?.createdAt || null,
    updatedAt: doc?.updatedAt || null,
  };
}

async function resolveImageUrl(productId, preferredUrl) {
  const preferred = String(preferredUrl || "").trim();
  if (preferred) return preferred;

  const product = await Product.findById(productId)
    .select("thumbnail thumbnail_url")
    .lean();

  const fromProduct = String(product?.thumbnail_url || product?.thumbnail || "").trim();
  if (fromProduct) return fromProduct;

  const imageDoc = await ProductImage.findOne({ product_id: productId })
    .sort({ is_primary: -1, sort_order: 1, _id: 1 })
    .select("image_url")
    .lean();

  return String(imageDoc?.image_url || "").trim();
}

async function listWishlist(req, res) {
  try {
    const customerId = await resolveOrCreateCustomerId(req.query.customer_id);
    if (!customerId) {
      return res.status(400).json({ message: "customer_id is required." });
    }

    const items = await Wishlist.find({ customer_id: customerId }).sort({ createdAt: -1 }).lean();
    return res.json({
      items: items.map(mapWishlistItem),
    });
  } catch (error) {
    return res.status(500).json({ message: "Cannot load wishlist.", error: error.message });
  }
}

async function upsertWishlistItem(req, res) {
  try {
    const customerId = await resolveOrCreateCustomerId(req.body?.customer_id);
    const productId = asObjectId(req.body?.product_id);

    if (!customerId || !productId) {
      return res.status(400).json({ message: "customer_id and product_id are required." });
    }

    const [customer, product] = await Promise.all([
      Customer.findById(customerId).select("full_name phone").lean(),
      Product.findById(productId).select("name slug min_price").lean()
    ]);

    if (!customer) {
      return res.status(404).json({ message: "Customer not found." });
    }
    if (!product) {
      return res.status(404).json({ message: "Product not found." });
    }

    const salePrice = toMoney(req.body?.sale_price, product.min_price || 0);
    const listedPrice = toMoney(req.body?.price, Math.max(salePrice, product.min_price || 0));
    const imageUrl = await resolveImageUrl(productId, req.body?.image_url);

    const payload = {
      account_name: customer.full_name || "Guest",
      account_phone: customer.phone || "",
      product_slug: String(req.body?.product_slug || product.slug || "").trim(),
      name: String(req.body?.name || product.name || "Sản phẩm").trim(),
      image_url: imageUrl,
      sale_price: salePrice,
      price: listedPrice,
    };

    const item = await Wishlist.findOneAndUpdate(
      { customer_id: customerId, product_id: productId },
      { $set: payload },
      { upsert: true, new: true, setDefaultsOnInsert: true }
    ).lean();

    return res.status(201).json(mapWishlistItem(item));
  } catch (error) {
    console.error(error);
    return res.status(500).json({ message: "Cannot add to wishlist.", error: error.message });
  }
}

async function removeWishlistItem(req, res) {
  try {
    const { id } = req.params;
    const itemId = asObjectId(id);

    if (!itemId) {
      return res.status(400).json({ message: "Invalid item ID." });
    }

    const result = await Wishlist.findByIdAndDelete(itemId);
    if (!result) {
      return res.status(404).json({ message: "Item not found in wishlist." });
    }

    return res.json({ success: true });
  } catch (error) {
    return res.status(500).json({ message: "Cannot remove from wishlist.", error: error.message });
  }
}

module.exports = {
  listWishlist,
  upsertWishlistItem,
  removeWishlistItem,
};
