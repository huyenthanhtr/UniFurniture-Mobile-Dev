const mongoose = require("mongoose");
const Review = require("../models/Review");

function buildRatingsAggregation(matchStage = {}) {
  return [
    { $match: { status: "approved", ...matchStage } },
    {
      $lookup: {
        from: "order_detail",
        localField: "order_detail_id",
        foreignField: "_id",
        as: "order_detail",
      },
    },
    { $unwind: "$order_detail" },
    {
      $lookup: {
        from: "product_variants",
        localField: "order_detail.variant_id",
        foreignField: "_id",
        as: "variant",
      },
    },
    { $unwind: "$variant" },
    {
      $group: {
        _id: "$variant.product_id",
        averageRating: { $avg: "$rating" },
        totalReviews: { $sum: 1 },
      },
    },
  ];
}

async function getRatingsMapForProductIds(productIds) {
  if (!Array.isArray(productIds) || !productIds.length) {
    return new Map();
  }

  const objectIds = productIds
    .filter((id) => mongoose.Types.ObjectId.isValid(String(id)))
    .map((id) => new mongoose.Types.ObjectId(String(id)));

  if (!objectIds.length) {
    return new Map();
  }

  const rows = await Review.aggregate([
    ...buildRatingsAggregation(),
    { $match: { _id: { $in: objectIds } } },
  ]);

  return new Map(
    rows.map((row) => [
      String(row._id),
      {
        averageRating: Number(Number(row.averageRating || 0).toFixed(1)),
        totalReviews: row.totalReviews || 0,
      },
    ])
  );
}

async function getProductIdsWithMinRating(minRating) {
  const threshold = Number(minRating);
  if (!Number.isFinite(threshold) || threshold <= 0) {
    return null;
  }

  const rows = await Review.aggregate(buildRatingsAggregation());
  return rows
    .filter((row) => Number(Number(row.averageRating || 0).toFixed(1)) >= threshold)
    .map((row) => row._id);
}

async function attachAverageRatings(items) {
  if (!Array.isArray(items) || !items.length) {
    return items;
  }

  const ratingsMap = await getRatingsMapForProductIds(items.map((item) => item._id));
  return items.map((item) => {
    const summary = ratingsMap.get(String(item._id));
    return {
      ...item,
      average_rating: summary ? summary.averageRating : 0,
    };
  });
}

module.exports = {
  attachAverageRatings,
  getProductIdsWithMinRating,
};
