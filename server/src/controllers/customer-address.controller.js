const mongoose = require("mongoose");
const CustomerAddress = require("../models/CustomerAddress");
const Customer = require("../models/Customer");
const Profile = require("../models/Profile");
const { generateCustomerCode } = require("../utils/code-generator");

function asObjectId(value) {
  const raw = String(value || "").trim();
  if (!raw || !mongoose.Types.ObjectId.isValid(raw)) return null;
  return new mongoose.Types.ObjectId(raw);
}

async function resolveOrCreateCustomerId(idRaw) {
  const id = asObjectId(idRaw);
  if (!id) return null;

  const profile = await Profile.findById(id);
  if (profile) {
    if (profile.customer_id && mongoose.Types.ObjectId.isValid(String(profile.customer_id))) {
      return profile.customer_id;
    }

    const customerCode = await generateCustomerCode();
    const customer = await Customer.create({
      customer_code: customerCode,
      full_name: profile.full_name || "Member",
      phone: profile.phone || "",
      customer_type: "member",
      status: "active",
    });

    profile.customer_id = customer._id;
    await profile.save();
    return customer._id;
  }

  const customerDoc = await Customer.findById(id).select("_id").lean();
  return customerDoc?._id || null;
}

function normalizeText(value) {
  return String(value || "").trim();
}

function mapAddressPayload(body, customerId) {
  return {
    customer_id: customerId,
    customer_address_name: normalizeText(body?.customer_address_name),
    address_phone: normalizeText(body?.address_phone),
    address_line: normalizeText(body?.address_line),
    ward: normalizeText(body?.ward),
    district: normalizeText(body?.district),
    province: normalizeText(body?.province),
    is_default: !!body?.is_default,
    status: normalizeText(body?.status) || "active",
  };
}

function validateAddressPayload(payload) {
  if (!payload.customer_id) return "Customer id is required.";
  if (!payload.customer_address_name) return "Recipient name is required.";
  if (!payload.address_phone) return "Phone number is required.";
  if (!payload.address_line) return "Address line is required.";
  if (!payload.district) return "District is required.";
  if (!payload.province) return "Province is required.";
  return "";
}

async function ensureSingleDefault(customerId, addressId) {
  if (!customerId) return;
  await CustomerAddress.updateMany(
    {
      customer_id: customerId,
      status: "active",
      _id: { $ne: addressId },
    },
    { $set: { is_default: false } }
  );
}

async function promoteFallbackDefault(customerId) {
  if (!customerId) return;

  const existingDefault = await CustomerAddress.findOne({
    customer_id: customerId,
    status: "active",
    is_default: true,
  }).lean();
  if (existingDefault) return;

  const firstActive = await CustomerAddress.findOne({
    customer_id: customerId,
    status: "active",
  }).sort({ updatedAt: -1, _id: -1 });

  if (firstActive) {
    firstActive.is_default = true;
    await firstActive.save();
  }
}

async function listCustomerAddresses(req, res) {
  try {
    const page = Math.max(parseInt(req.query.page || "1", 10), 1);
    const limit = Math.min(Math.max(parseInt(req.query.limit || "20", 10), 1), 200);
    const skip = (page - 1) * limit;

    const resolvedCustomerId = await resolveOrCreateCustomerId(req.query.customer_id);
    if (!resolvedCustomerId) {
      return res.json({ page, limit, total: 0, items: [] });
    }

    const filter = {
      customer_id: resolvedCustomerId,
      status: req.query.status ? String(req.query.status).trim() : "active",
    };

    const [items, total] = await Promise.all([
      CustomerAddress.find(filter)
        .sort({ is_default: -1, updatedAt: -1, _id: -1 })
        .skip(skip)
        .limit(limit),
      CustomerAddress.countDocuments(filter),
    ]);

    return res.json({ page, limit, total, items });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ message: "Server error" });
  }
}

async function createCustomerAddress(req, res) {
  try {
    const resolvedCustomerId = await resolveOrCreateCustomerId(req.body?.customer_id);
    const payload = mapAddressPayload(req.body, resolvedCustomerId);
    const validationError = validateAddressPayload(payload);
    if (validationError) {
      return res.status(400).json({ message: validationError });
    }

    const hasActiveAddress = await CustomerAddress.exists({
      customer_id: resolvedCustomerId,
      status: "active",
    });
    if (!hasActiveAddress) {
      payload.is_default = true;
    }

    const doc = await CustomerAddress.create(payload);
    if (doc.is_default) {
      await ensureSingleDefault(resolvedCustomerId, doc._id);
    }

    return res.status(201).json(doc);
  } catch (err) {
    console.error(err);
    return res.status(400).json({ message: "Create failed" });
  }
}

async function updateCustomerAddress(req, res) {
  try {
    const { id } = req.params;
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({ message: "Invalid id" });
    }

    const current = await CustomerAddress.findById(id);
    if (!current) {
      return res.status(404).json({ message: "Not found" });
    }

    const resolvedCustomerId = await resolveOrCreateCustomerId(req.body?.customer_id || current.customer_id);
    const payload = mapAddressPayload(
      {
        ...current.toObject(),
        ...req.body,
        status: req.body?.status || current.status,
      },
      resolvedCustomerId
    );

    const validationError = validateAddressPayload(payload);
    if (validationError) {
      return res.status(400).json({ message: validationError });
    }

    current.customer_id = payload.customer_id;
    current.customer_address_name = payload.customer_address_name;
    current.address_phone = payload.address_phone;
    current.address_line = payload.address_line;
    current.ward = payload.ward;
    current.district = payload.district;
    current.province = payload.province;
    current.status = payload.status || "active";
    current.is_default = payload.status === "inactive" ? false : payload.is_default;

    await current.save();

    if (current.status === "active" && current.is_default) {
      await ensureSingleDefault(current.customer_id, current._id);
    } else {
      await promoteFallbackDefault(current.customer_id);
    }

    return res.json(current);
  } catch (err) {
    console.error(err);
    return res.status(400).json({ message: "Update failed" });
  }
}

async function deleteCustomerAddress(req, res) {
  try {
    const { id } = req.params;
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({ message: "Invalid id" });
    }

    const current = await CustomerAddress.findById(id);
    if (!current) {
      return res.status(404).json({ message: "Not found" });
    }

    current.status = "inactive";
    current.is_default = false;
    await current.save();
    await promoteFallbackDefault(current.customer_id);

    return res.json({ success: true, deleted: current });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ message: "Delete failed" });
  }
}

module.exports = {
  listCustomerAddresses,
  createCustomerAddress,
  updateCustomerAddress,
  deleteCustomerAddress,
};
