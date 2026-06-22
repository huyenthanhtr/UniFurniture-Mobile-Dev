package com.unifurniture.mobile.util;

import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.CategoryDto;
import com.unifurniture.mobile.data.model.CollectionDto;
import java.util.Locale;
import java.text.Normalizer;

/**
 * Cung cấp ảnh nghệ thuật chất lượng cao cho danh mục và bộ sưu tập.
 */
public final class CategoryImageHelper {
    private CategoryImageHelper() {}

    private static String deAccent(String str) {
        if (str == null) return "";
        // Chuẩn hóa NFD để tách dấu và xóa dấu hoàn toàn
        String nfd = Normalizer.normalize(str.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        String out = nfd.replaceAll("\\p{M}", "");
        return out.replace("đ", "d").trim();
    }

    public static String resolveNetworkUrl(CategoryDto category, String serverHost) {
        if (category == null) return "https://images.unsplash.com/photo-1524758631624-e2822e304c36?w=500";
        
        String name = deAccent(category.name);
        String slug = (category.slug != null ? category.slug : "").toLowerCase(Locale.ROOT);
        
        // 1. Tủ Đầu Giường
        if (name.contains("dau giuong") || slug.contains("dau-giuong")) {
            return "https://images.unsplash.com/photo-1618221520382-3d68e64f58ff?w=500&h=500&fit=crop";
        }

        // 2. Combo / Phòng Ngủ
        if (name.contains("combo") || slug.contains("combo")) {
            return "https://images.unsplash.com/photo-1616594039964-ae9021a400a0?w=500&h=500&fit=crop";
        }

        // 3. Giường Ngủ (Sau khi loại trừ Tủ đầu giường và Combo)
        if (name.contains("giuong") || slug.contains("giuong-ngu") || name.contains("bed")) {
            return "https://images.unsplash.com/photo-1595526114035-0d45ed16cfbf?w=500&h=500&fit=crop";
        }

        // 4. Bàn Sofa / Cafe / Trà (Kiểm tra trước Bàn Ăn)
        if (name.contains("ban") && (name.contains("sofa") || name.contains("tra") || name.contains("cafe"))) {
            return "https://images.unsplash.com/photo-1538688525198-9b88f6f53126?w=500&h=500&fit=crop";
        }

        // 5. Bàn làm việc
        if (name.contains("lam viec") || name.contains("desk") || slug.contains("lam-viec")) {
            return "https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?w=500&h=500&fit=crop";
        }

        // 6. Bàn Ăn (Sau khi đã loại trừ các loại bàn khác)
        if (name.contains("ban") && (name.contains("an") || slug.contains("ban-an") || name.contains("dining"))) {
            // Góc nhìn từ trên, bố cục vuông — hiển thị trọn trong khung tròn centerCrop
            return "https://images.unsplash.com/photo-1617806118233-18e1de247200?w=500&h=500&fit=crop";
        }

        // 7. Ghế Sofa
        if (name.contains("ghe") || name.contains("sofa")) {
            return "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=500&h=500&fit=crop";
        }

        // 8. Tủ quần áo
        if (name.contains("quan ao") || name.contains("wardrobe") || slug.contains("quan-ao")) {
            return "https://images.unsplash.com/photo-1595428774223-ef52624120d2?w=500&h=500&fit=crop";
        }

        // 9. Tủ giày / Kệ / Trang trí
        if (name.contains("tu") || name.contains("ke") || name.contains("giay")) {
            return "https://images.unsplash.com/photo-1594026112284-02bb6f3352fe?w=500&h=500&fit=crop";
        }
        
        return "https://images.unsplash.com/photo-1524758631624-e2822e304c36?w=500&h=500&fit=crop";
    }

    public static String resolveCollectionUrl(CollectionDto collection, String serverHost) {
        if (collection == null) return null;
        String name = deAccent(collection.name);
        if (name.contains("phong khach")) return "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=800";
        if (name.contains("phong ngu")) return "https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?w=800";
        
        if (collection.bannerUrl != null && !collection.bannerUrl.isEmpty()) {
            return collection.bannerUrl.replace("http://localhost:3000", serverHost);
        }
        return "https://images.unsplash.com/photo-1493663284031-b7e3aefcae8e?w=800";
    }

    public static String resolveProductUrl(com.unifurniture.mobile.data.model.ProductDto product, String serverHost) {
        if (product == null) return null;
        String url = product.getImageUrl();
        if (url == null || url.isEmpty()) return null;
        if (serverHost != null && !serverHost.isEmpty()) {
            url = url.replace("http://localhost:3000", serverHost).replace("https://localhost:3000", serverHost);
        }
        return url;
    }

    public static int resolveDrawableRes(CategoryDto category) {
        return R.drawable.placeholder_category;
    }
}
