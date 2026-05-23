# UniFurniture Mobile (Android)

Ứng dụng Android cho hệ thống bán đồ nội thất **UniFurniture**, xây dựng dựa trên web project cùng tên.

## 📦 Tech Stack

| Layer | Technology |
|---|---|
| Language | **Java** (Android) |
| Architecture | **MVVM** (ViewModel + LiveData) |
| HTTP Client | **Retrofit 2** + OkHttp |
| JSON | **Gson** |
| Image Loading | **Glide** |
| Navigation | **Navigation Component** |
| UI | **Material Design 3** |

## 🗂️ Cấu trúc

```
app/src/main/java/com/unifurniture/mobile/
├── data/
│   ├── model/          # DTO classes (mirrors web models)
│   ├── remote/         # ApiService + ApiClient (Retrofit)
│   └── repository/     # ProductRepository, CartRepository, OrderRepository, AuthRepository
├── ui/
│   ├── home/           # HomeFragment + HomeViewModel
│   ├── product/        # ProductListFragment, ProductDetailFragment + ViewModels
│   ├── cart/           # CartFragment + CartViewModel
│   ├── checkout/       # CheckoutFragment
│   ├── account/        # AccountFragment, OrderListFragment
│   ├── auth/           # AuthActivity, LoginFragment, RegisterFragment, OtpFragment
│   └── adapter/        # ProductCardAdapter, CartItemAdapter, CategoryAdapter, ...
└── util/
    ├── SessionManager  # JWT token + customer data (SharedPreferences)
    └── FormatUtil      # VND currency formatter
```

## 🌐 API Endpoints

Cấu hình địa chỉ kết nối API trong file `local.properties` (nằm ở thư mục gốc của dự án di động). File này sẽ không được đưa lên Git để tránh lộ/lệch IP cá nhân:

```properties
# local.properties
api.base.url=http://192.168.110.179:3000/api/
```

* Nếu dùng Emulator: Bạn có thể bỏ cấu hình này (mặc định sẽ dùng `http://10.0.2.2:3000/api/`).
* Nếu test trên điện thoại thật: Điền IP LAN của máy tính chạy server Node.js vào như ví dụ trên (đảm bảo điện thoại và máy tính kết nối chung mạng Wi-Fi).

### Các API đã tích hợp:
- `POST /auth/login` – Đăng nhập
- `POST /auth/register` – Đăng ký
- `POST /auth/verify-otp` – Xác thực OTP
- `GET /products` – Danh sách sản phẩm (filter, sort, search)
- `GET /products/:slug` – Chi tiết sản phẩm
- `GET /product-images` – Ảnh sản phẩm
- `GET /product-variants` – Biến thể sản phẩm
- `GET /categories` – Danh mục
- `GET /collections` – Bộ sưu tập
- `GET /cart/active` – Giỏ hàng
- `POST /cart/items/upsert` – Thêm vào giỏ
- `PATCH /cart/items/:id` – Cập nhật số lượng
- `DELETE /cart/items/:id` – Xóa sản phẩm
- `GET /orders` – Lịch sử đơn hàng
- `POST /orders` – Đặt hàng
- `GET /reviews/product/:id` – Đánh giá sản phẩm
- `GET /wishlist` – Danh sách yêu thích

## 🚀 Chạy dự án

1. Khởi động server: `cd server && npm start` (port 3000)
2. Mở `unifurniture-mobile/` bằng Android Studio
3. Tạo/Cập nhật file `local.properties` với `api.base.url` tương ứng
4. Sync Gradle
5. Run trên thiết bị hoặc emulator (API 26+)

