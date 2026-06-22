const crypto = require("crypto");
const fs = require("fs");
const path = require("path");
const mongoose = require("mongoose");
const Profile = require("../models/Profile");

function hashValue(value) {
  return crypto.createHash("sha256").update(String(value || "")).digest("hex");
}

function parseDataUrl(dataUrl) {
  const match = String(dataUrl || "").match(/^data:(image\/[a-zA-Z0-9.+-]+);base64,(.+)$/);
  if (!match) return null;
  return { mimeType: match[1], base64: match[2] };
}

function extensionFromMime(mimeType) {
  const map = {
    "image/jpeg": "jpg",
    "image/jpg": "jpg",
    "image/png": "png",
    "image/webp": "webp",
    "image/gif": "gif",
    "image/svg+xml": "svg",
  };
  return map[mimeType] || "jpg";
}

function slugify(value) {
  return String(value || "avatar")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/\u0111/g, "d")
    .replace(/\u0110/g, "D")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "")
    .replace(/_{2,}/g, "_");
}

async function changePassword(req, res, next) {
  try {
    const { id } = req.params;
    const oldPassword = String(req.body?.old_password || "").trim();
    const newPassword = String(req.body?.new_password || "").trim();

    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({ message: "ID tài khoản không hợp lệ." });
    }

    if (!oldPassword || !newPassword) {
      return res.status(400).json({ message: "Vui lòng nhập đủ mật khẩu cũ và mật khẩu mới." });
    }

    if (newPassword.length < 6) {
      return res.status(400).json({ message: "Mật khẩu mới phải có ít nhất 6 ký tự." });
    }

    const profile = await Profile.findById(id);
    if (!profile) {
      return res.status(404).json({ message: "Không tìm thấy tài khoản." });
    }

    const oldHash = hashValue(oldPassword);
    if (oldHash !== String(profile.password_hash || "")) {
      return res.status(400).json({ message: "Mật khẩu hiện tại không đúng." });
    }

    const nextHash = hashValue(newPassword);
    if (nextHash === String(profile.password_hash || "")) {
      return res.status(400).json({ message: "Mật khẩu mới phải khác mật khẩu hiện tại." });
    }

    profile.password_hash = nextHash;
    await profile.save();

    return res.status(200).json({ message: "Đổi mật khẩu thành công." });
  } catch (err) {
    next(err);
  }
}

async function uploadAvatar(req, res, next) {
  try {
    const { id } = req.params;
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({ message: "ID tai khoan khong hop le." });
    }

    const profile = await Profile.findById(id);
    if (!profile) {
      return res.status(404).json({ message: "Khong tim thay tai khoan." });
    }

    const parsed = parseDataUrl(req.body?.dataUrl);
    if (!parsed) {
      return res.status(400).json({ message: "Du lieu anh khong hop le." });
    }

    const buffer = Buffer.from(parsed.base64, "base64");
    if (!buffer.length) {
      return res.status(400).json({ message: "Anh tai len rong." });
    }

    const ext = extensionFromMime(parsed.mimeType);
    const randomHash = crypto.randomBytes(16).toString("hex");
    const profileSlug = slugify(profile.full_name || profile.phone || "avatar");
    const filename = `profile_${profileSlug}_${randomHash}.${ext}`;

    const uploadDir = path.resolve(__dirname, "..", "..", "..", "admin", "src", "assets", "upload");
    await fs.promises.mkdir(uploadDir, { recursive: true });
    await fs.promises.writeFile(path.join(uploadDir, filename), buffer);

    const origin = `${req.protocol}://${req.get("host")}`;
    const imageUrl = `${origin}/assets/upload/${filename}`;

    profile.avatar_url = imageUrl;
    await profile.save();

    return res.status(200).json(profile);
  } catch (err) {
    next(err);
  }
}

module.exports = {
  changePassword,
  uploadAvatar,
};
