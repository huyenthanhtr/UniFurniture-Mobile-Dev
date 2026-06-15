package com.unifurniture.mobile.data.model;

import java.util.List;

/**
 * Response wrapper for GET /wishlist/profiles/:profileId
 * Server returns: { items: [...] }
 */
public class WishlistListResponse {
    public List<WishlistItemDto> items;
}
