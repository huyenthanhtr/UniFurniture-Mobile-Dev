const mongoose = require("mongoose");
const Coupon = require("../models/Coupon");
const Order = require("../models/Order");
const Profile = require("../models/Profile");

function resolveLang(req) {
  const raw = String(req.query.lang || req.get("Accept-Language") || "en").toLowerCase();
  if (raw.includes("vi")) return "vi";
  if (raw.includes("zh")) return "zh";
  if (raw.includes("fr")) return "fr";
  return "en";
}

function formatCurrencyVnd(amount, lang) {
  try {
    const localeMap = {
      vi: "vi-VN",
      en: "en-US",
      fr: "fr-FR",
      zh: "zh-CN",
    };
    return new Intl.NumberFormat(localeMap[lang] || "en-US", {
      style: "currency",
      currency: "VND",
      maximumFractionDigits: 0,
    }).format(Number(amount || 0));
  } catch (error) {
    return `${Math.round(Number(amount || 0)).toLocaleString("en-US")} VND`;
  }
}

function formatCouponBenefit(coupon, lang) {
  if (!coupon) return "";
  const discountType = String(coupon.discount_type || "").toLowerCase();
  if (discountType === "fixed") {
    return formatCurrencyVnd(coupon.discount_value, lang);
  }
  return `${Math.round(Number(coupon.discount_value || 0))}%`;
}

function buildBirthdayCopy(lang, coupon) {
  const benefit = formatCouponBenefit(coupon, lang);
  const code = String(coupon?.code || "").trim();

  const hasCoupon = !!coupon && !!code;
  const copy = {
    vi: {
      title: "Chuc mung sinh nhat!",
      content: hasCoupon
        ? `UniFurniture gui tang ban voucher ${benefit} voi ma ${code} trong thang sinh nhat cua ban.`
        : "UniFurniture chuc ban mot thang sinh nhat that vui va am ap.",
    },
    en: {
      title: "Happy Birthday!",
      content: hasCoupon
        ? `UniFurniture prepared a ${benefit} birthday voucher for you this month. Code: ${code}.`
        : "UniFurniture wishes you a joyful and memorable birthday month.",
    },
    fr: {
      title: "Joyeux anniversaire !",
      content: hasCoupon
        ? `UniFurniture vous offre un bon anniversaire de ${benefit} ce mois-ci. Code : ${code}.`
        : "UniFurniture vous souhaite un mois d'anniversaire plein de joie.",
    },
    zh: {
      title: "生日快乐！",
      content: hasCoupon
        ? `UniFurniture 为您准备了本月生日优惠券，优惠 ${benefit}，代码：${code}。`
        : "UniFurniture 祝您生日月快乐温馨。",
    },
  };

  return copy[lang] || copy.en;
}

function buildOrderStatusLabel(status, lang) {
  const labels = {
    pending: { vi: "cho xac nhan", en: "pending confirmation", fr: "en attente de confirmation", zh: "待确认" },
    confirmed: { vi: "da xac nhan", en: "confirmed", fr: "confirmee", zh: "已确认" },
    processing: { vi: "dang xu ly", en: "processing", fr: "en cours de preparation", zh: "处理中" },
    shipping: { vi: "dang giao", en: "shipping", fr: "en cours de livraison", zh: "配送中" },
    delivered: { vi: "da giao", en: "delivered", fr: "livree", zh: "已送达" },
    completed: { vi: "hoan tat", en: "completed", fr: "terminee", zh: "已完成" },
    cancelled: { vi: "da huy", en: "cancelled", fr: "annulee", zh: "已取消" },
    cancel_pending: { vi: "cho huy", en: "cancellation pending", fr: "annulation en attente", zh: "待取消" },
    exchanged: { vi: "da doi hang", en: "exchanged", fr: "echangee", zh: "已换货" },
  };
  return labels[String(status || "").toLowerCase()]?.[lang] || labels.pending[lang] || labels.pending.en;
}

function buildOrderCopy(lang, orderCode, status) {
  const statusLabel = buildOrderStatusLabel(status, lang);
  const copy = {
    vi: {
      title: `Don hang ${orderCode}`,
      content: `Trang thai hien tai cua don hang la ${statusLabel}.`,
    },
    en: {
      title: `Order ${orderCode}`,
      content: `Your order is currently ${statusLabel}.`,
    },
    fr: {
      title: `Commande ${orderCode}`,
      content: `Votre commande est actuellement ${statusLabel}.`,
    },
    zh: {
      title: `订单 ${orderCode}`,
      content: `您的订单当前状态为${statusLabel}。`,
    },
  };
  return copy[lang] || copy.en;
}

function buildNotificationItem({
  id,
  title,
  content,
  type,
  timestamp,
  orderId = null,
  eventKey = "",
  couponCode = "",
}) {
  return {
    id,
    title,
    content,
    type,
    timestamp: new Date(timestamp || Date.now()).getTime(),
    isRead: false,
    orderId,
    eventKey,
    couponCode,
  };
}

async function getNotifications(req, res, next) {
  try {
    const lang = resolveLang(req);
    const limit = Math.min(Math.max(parseInt(req.query.limit || "20", 10) || 20, 1), 50);
    const customerId = String(req.query.customer_id || "").trim();

    const items = [];
    let profile = null;

    if (customerId && mongoose.Types.ObjectId.isValid(customerId)) {
      profile = await Profile.findOne({ customer_id: new mongoose.Types.ObjectId(customerId) }).lean();
    }

    if (profile?.date_of_birth) {
      const today = new Date();
      const dob = new Date(profile.date_of_birth);
      const sameBirthMonth = !Number.isNaN(dob.getTime()) && dob.getMonth() === today.getMonth();

      if (sameBirthMonth) {
        const now = new Date();
        const birthdayCoupon = await Coupon.findOne({
          status: "active",
          start_at: { $lte: now },
          end_at: { $gte: now },
        })
          .sort({ discount_value: -1, createdAt: -1 })
          .lean();

        const birthdayCopy = buildBirthdayCopy(lang, birthdayCoupon);
        items.push(
          buildNotificationItem({
            id: `birthday-${String(profile._id)}-${today.getFullYear()}-${today.getMonth() + 1}`,
            title: birthdayCopy.title,
            content: birthdayCopy.content,
            type: "account",
            timestamp: now,
            eventKey: "birthday_reward",
            couponCode: birthdayCoupon?.code || "",
          })
        );
      }
    }

    if (customerId && mongoose.Types.ObjectId.isValid(customerId)) {
      const orders = await Order.find({ customer_id: new mongoose.Types.ObjectId(customerId) })
        .sort({ updatedAt: -1, createdAt: -1, _id: -1 })
        .limit(10)
        .lean();

      for (const order of orders) {
        const orderCode = String(order.order_code || "").trim();
        if (!orderCode) continue;
        const copy = buildOrderCopy(lang, orderCode, order.status);
        items.push(
          buildNotificationItem({
            id: `order-${orderCode}`,
            title: copy.title,
            content: copy.content,
            type: "order",
            timestamp: order.updatedAt || order.createdAt || Date.now(),
            orderId: orderCode,
            eventKey: "order_status",
          })
        );
      }
    }

    items.sort((left, right) => Number(right.timestamp || 0) - Number(left.timestamp || 0));
    const pagedItems = items.slice(0, limit);

    return res.json({
      page: 1,
      limit,
      total: items.length,
      totalPages: 1,
      items: pagedItems,
    });
  } catch (error) {
    next(error);
  }
}

module.exports = {
  getNotifications,
};
