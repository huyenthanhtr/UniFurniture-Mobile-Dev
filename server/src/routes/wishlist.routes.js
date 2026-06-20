const express = require("express");
const {
  listWishlist,
  upsertWishlistItem,
  removeWishlistItem,
} = require("../controllers/wishlist.controller");

const router = express.Router();

// Match Mobile ApiService: @GET("wishlist") @Query("customer_id")
router.get('/', listWishlist);

// Match Mobile ApiService: @POST("wishlist") @Body { customer_id, product_id, ... }
router.post('/', upsertWishlistItem);

// Match Mobile ApiService: @DELETE("wishlist/:id")
router.delete('/:id', removeWishlistItem);

module.exports = router;
