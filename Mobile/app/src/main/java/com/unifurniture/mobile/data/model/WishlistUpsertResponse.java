package com.unifurniture.mobile.data.model;

/**
 * Response wrapper for POST /wishlist/profiles/:profileId/items
 * Server returns: { item: {...} }
 */
public class WishlistUpsertResponse {
    public WishlistItemDto item;
}
