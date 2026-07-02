const mongoose = require("mongoose");
const Cart = require("../models/Cart");
const CartItem = require("../models/CartItem");
const ProductVariant = require("../models/ProductVariant");
const Product = require("../models/Product");
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

async function populateCartItem(item) {
  let doc = item.toObject ? item.toObject() : item;
  
  if (!doc.variant_id) return doc;

  const variant = await ProductVariant.findById(doc.variant_id).lean();
  let product = null;

  if (variant && variant.product_id) {
    product = await Product.findById(variant.product_id).lean();
    doc.variant_id = { ...variant, product_id: product };
  } else {
    product = await Product.findById(doc.variant_id).lean();
    if (product) {
      doc.variant_id = { _id: null, product_id: product };
    } else {
      doc.variant_id = null;
    }
  }

  return doc;
}

function getVariantStock(variant) {
  return Math.max(0, Number(variant?.stock_quantity || 0));
}

function getVariantDisplayName(variant) {
  return String(
    variant?.variant_name ||
    variant?.name ||
    variant?.sku ||
    "biến thể"
  ).trim();
}

function buildStockError(variant, requestedQty) {
  return `Số lượng vượt quá tồn kho của ${getVariantDisplayName(variant)}. Còn ${getVariantStock(variant)}, bạn chọn ${requestedQty}.`;
}

async function findActiveVariant(variantId) {
  if (!mongoose.Types.ObjectId.isValid(variantId)) {
    return { status: 400, message: "Invalid variant_id" };
  }

  const variant = await ProductVariant.findById(variantId).lean();
  if (!variant) {
    return { status: 404, message: "Variant not found" };
  }

  if (String(variant.variant_status || "").toLowerCase() !== "active") {
    return { status: 400, message: "Biến thể sản phẩm hiện không còn mở bán." };
  }

  return { variant };
}

async function validateVariantQuantity(variantId, quantity) {
  const result = await findActiveVariant(variantId);
  if (result.message) return result;

  const qty = Math.max(1, parseInt(quantity, 10) || 1);
  if (getVariantStock(result.variant) < qty) {
    return { status: 400, message: buildStockError(result.variant, qty) };
  }

  return { variant: result.variant, quantity: qty };
}

async function normalizeCartQuantities(cartId) {
  if (!cartId) return;

  const items = await CartItem.find({ cart_id: cartId });
  for (const item of items) {
    const variant = await ProductVariant.findById(item.variant_id).lean();
    if (!variant || String(variant.variant_status || "").toLowerCase() !== "active") {
      await CartItem.findByIdAndDelete(item._id);
      continue;
    }

    const stock = getVariantStock(variant);
    if (stock <= 0) {
      await CartItem.findByIdAndDelete(item._id);
      continue;
    }

    if (Number(item.quantity || 0) > stock) {
      item.quantity = stock;
      await item.save();
    }
  }
}

async function getActiveCart(req, res) {
  try {
    const { customer_id, cart_id } = req.query;

    let cart = null;
    const resolvedCustomerId = await resolveOrCreateCustomerId(customer_id);
    const hasValidCustomerId = resolvedCustomerId && mongoose.Types.ObjectId.isValid(resolvedCustomerId);
    const hasValidCartId = cart_id && mongoose.Types.ObjectId.isValid(cart_id);

    if (hasValidCartId) {
      cart = await Cart.findById(cart_id);
    }

    if (hasValidCustomerId) {
      let userCart = await Cart.findOne({ customer_id: resolvedCustomerId, status: "active" });
      if (cart && String(cart.customer_id) !== String(resolvedCustomerId)) {
        // We have a guest cart and a logged-in user cart, merge them
        if (userCart) {
          const guestItems = await CartItem.find({ cart_id: cart._id });
          for (const gItem of guestItems) {
            let uItem = await CartItem.findOne({ cart_id: userCart._id, variant_id: gItem.variant_id });
            if (uItem) {
              uItem.quantity += gItem.quantity;
              await uItem.save();
            } else {
              gItem.cart_id = userCart._id;
              await gItem.save();
            }
          }
          await normalizeCartQuantities(userCart._id);
          await Cart.findByIdAndDelete(cart._id);
          cart = userCart;
        } else {
          // Update the guest cart to belong to the logged-in user
          cart.customer_id = resolvedCustomerId;
          await cart.save();
        }
      } else if (!cart) {
        cart = userCart;
      }
    }

    if (!cart) {
      if (hasValidCustomerId) {
        cart = await Cart.create({ customer_id: resolvedCustomerId, status: "active" });
      } else {
        return res.status(200).json(null);
      }
    }

    const rawItems = await CartItem.find({ cart_id: cart._id }).sort({ createdAt: 1 });
    const items = await Promise.all(rawItems.map(populateCartItem));

    return res.json({
      ...cart.toObject(),
      items
    });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ message: "Server error" });
  }
}

async function upsertCartItem(req, res) {
  try {
    let { cart_id, variant_id, quantity, unit_price, customer_id } = req.body;

    if (!variant_id || !quantity) {
      return res.status(400).json({ message: "variant_id and quantity are required" });
    }

    if (!mongoose.Types.ObjectId.isValid(variant_id)) {
      return res.status(400).json({ message: "Invalid variant_id" });
    }

    let finalCart = null;
    const resolvedCustomerId = await resolveOrCreateCustomerId(customer_id);
    const hasValidCustomerId = resolvedCustomerId && mongoose.Types.ObjectId.isValid(resolvedCustomerId);

    // 1. Try to find cart by cart_id
    if (cart_id && mongoose.Types.ObjectId.isValid(cart_id)) {
      finalCart = await Cart.findById(cart_id);
    }

    // 2. Perform merge if both guest cart and logged-in user exist
    if (hasValidCustomerId) {
      let userCart = await Cart.findOne({ customer_id: resolvedCustomerId, status: "active" });
      if (finalCart && String(finalCart.customer_id) !== String(resolvedCustomerId)) {
        if (userCart) {
          // Merge items from finalCart (guest) into userCart
          const guestItems = await CartItem.find({ cart_id: finalCart._id });
          for (const gItem of guestItems) {
            let uItem = await CartItem.findOne({ cart_id: userCart._id, variant_id: gItem.variant_id });
            if (uItem) {
              uItem.quantity += gItem.quantity;
              await uItem.save();
            } else {
              gItem.cart_id = userCart._id;
              await gItem.save();
            }
          }
          await normalizeCartQuantities(userCart._id);
          await Cart.findByIdAndDelete(finalCart._id);
          finalCart = userCart;
        } else {
          // No user cart yet, update guest cart owner
          finalCart.customer_id = resolvedCustomerId;
          await finalCart.save();
        }
      } else if (!finalCart) {
        finalCart = userCart;
      }
    }

    // 3. If still no cart, but we have customer_id, create one
    if (!finalCart && hasValidCustomerId) {
      finalCart = await Cart.create({ customer_id: resolvedCustomerId, status: "active" });
    }

    // 4. If STILL no cart, create a guest customer and then a cart
    if (!finalCart) {
      const guestCustomer = await Customer.create({
        full_name: "Guest",
        customer_type: "guest",
        status: "active"
      });
      finalCart = await Cart.create({ customer_id: guestCustomer._id, status: "active" });
    }

    const validation = await validateVariantQuantity(variant_id, quantity);
    if (validation.message) {
      return res.status(validation.status).json({ message: validation.message });
    }
    const qty = validation.quantity;
    const variant = validation.variant;

    let item = await CartItem.findOne({ cart_id: finalCart._id, variant_id });
    if (item) {
      item.quantity = qty;
      if (unit_price !== undefined) {
        item.unit_price = unit_price;
      }
      await item.save();
    } else {
      item = await CartItem.create({
        cart_id: finalCart._id,
        variant_id,
        quantity: qty,
        unit_price: unit_price != null ? unit_price : variant.price || 0
      });
    }

    const rawItems = await CartItem.find({ cart_id: finalCart._id }).sort({ createdAt: 1 });
    const items = await Promise.all(rawItems.map(populateCartItem));

    return res.status(200).json({
      ...finalCart.toObject(),
      items
    });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ message: "Server error" });
  }
}

async function updateCartItem(req, res) {
  try {
    const { id } = req.params;
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({ message: "Invalid id" });
    }

    const item = await CartItem.findById(id);
    if (!item) return res.status(404).json({ message: "Cart item not found" });

    const { quantity, unit_price, variant_id } = req.body;

    const nextQuantity = quantity !== undefined
      ? Math.max(1, parseInt(quantity, 10) || 1)
      : Math.max(1, Number(item.quantity || 1));
    if (unit_price !== undefined) item.unit_price = unit_price;

    if (variant_id !== undefined) {
      if (!mongoose.Types.ObjectId.isValid(variant_id)) {
        return res.status(400).json({ message: "Invalid variant_id" });
      }

      const validation = await validateVariantQuantity(variant_id, nextQuantity);
      if (validation.message) {
        return res.status(validation.status).json({ message: validation.message });
      }

      // Check if another item with the same variant_id already exists in this cart
      const existingItem = await CartItem.findOne({
        cart_id: item.cart_id,
        variant_id,
        _id: { $ne: item._id }
      });
      if (existingItem) {
        // Merge quantities and delete this item
        const mergedQuantity = Math.max(1, Number(existingItem.quantity || 1)) + nextQuantity;
        if (getVariantStock(validation.variant) < mergedQuantity) {
          return res.status(400).json({ message: buildStockError(validation.variant, mergedQuantity) });
        }

        existingItem.quantity = mergedQuantity;
        await existingItem.save();
        await CartItem.findByIdAndDelete(item._id);
      } else {
        // Just swap the variant_id and update the unit price to match the new variant
        item.variant_id = variant_id;
        item.quantity = nextQuantity;
        item.unit_price = validation.variant.price || 0;
        await item.save();
      }
    } else {
      const validation = await validateVariantQuantity(item.variant_id, nextQuantity);
      if (validation.message) {
        return res.status(validation.status).json({ message: validation.message });
      }
      item.quantity = nextQuantity;
      await item.save();
    }

    const cartId = item.cart_id;
    const cart = await Cart.findById(cartId);
    const rawItems = await CartItem.find({ cart_id: cartId }).sort({ createdAt: 1 });
    const items = await Promise.all(rawItems.map(populateCartItem));

    return res.json({
      ...cart.toObject(),
      items
    });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ message: "Server error" });
  }
}

async function deleteCartItem(req, res) {
  try {
    const { id } = req.params;
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({ message: "Invalid id" });
    }
    const item = await CartItem.findById(id);
    if (!item) return res.status(404).json({ message: "Cart item not found" });

    const cartId = item.cart_id;
    await CartItem.findByIdAndDelete(id);

    const cart = await Cart.findById(cartId);
    const rawItems = await CartItem.find({ cart_id: cartId }).sort({ createdAt: 1 });
    const items = await Promise.all(rawItems.map(populateCartItem));

    return res.json({
      ...cart.toObject(),
      items
    });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ message: "Server error" });
  }
}

module.exports = { getActiveCart, upsertCartItem, updateCartItem, deleteCartItem };
