require("dotenv").config();
const connectDB = require("../configs/db");
const Product = require("../models/Product");
const ProductTranslation = require("../models/ProductTranslation");

// 1. Phrase replacements (order from longest/most-specific to shortest)
const phraseMap = [
  [/Bộ Bàn Ghế Ăn 4 - 6 Ghế Gỗ Tự Nhiên/gi, "Natural Wood Dining Table and 4-6 Chairs Set"],
  [/Bộ Bàn Ăn Gỗ 6 Ghế Cao Su/gi, "Rubber Wood Dining Table & 6 Chairs Set"],
  [/Bộ Bàn Ăn Gỗ 4 Ghế Cao Su/gi, "Rubber Wood Dining Table & 4 Chairs Set"],
  [/Bộ Bàn Ghế Ăn/gi, "Dining Table and Chairs Set"],
  [/Bộ Bàn Ăn Oslo Tròn/gi, "Oslo Round Dining Table Set"],
  [/Bộ Bàn Ăn Tròn Oslo/gi, "Oslo Round Dining Table Set"],
  [/Bộ Bàn Ăn 4 Ghế Gỗ Tự Nhiên/gi, "Natural Wood 4-Chair Dining Table Set"],
  [/Bộ Bàn Ăn 4 Ghế/gi, "4-Chair Dining Table Set"],
  [/Bộ Bàn Ăn 6 Ghế/gi, "6-Chair Dining Table Set"],
  [/Bộ Bàn Ăn/gi, "Dining Table Set"],
  
  [/Bàn Sofa – Bàn Cafe – Bàn Trà Tròn Cao/gi, "High Round Sofa / Coffee / Tea Table"],
  [/Bàn Sofa – Bàn Cafe – Bàn Trà Tròn/gi, "Round Sofa / Coffee / Tea Table"],
  [/Set 2 Bàn Sofa – Bàn Cafe – Bàn Trà/gi, "Set of 2 Sofa / Coffee / Tea Tables"],
  [/Bàn Sofa – Bàn Cafe – Bàn Trà/gi, "Sofa / Coffee / Tea Table"],
  [/Bàn Sofa - Bàn Cafe - Bàn Trà/gi, "Sofa / Coffee / Tea Table"],
  [/Bàn Sofa/gi, "Sofa Table"],
  [/Bàn Trà Tròn/gi, "Round Tea Table"],
  [/Bàn Trà/gi, "Tea Table"],
  [/Bàn Cafe/gi, "Coffee Table"],
  
  [/Ghế Ăn Gỗ Cao Su Tự Nhiên/gi, "Natural Rubber Wood Dining Chair"],
  [/Ghế Ăn Gỗ Cao Su/gi, "Rubber Wood Dining Chair"],
  [/Ghế Ăn Gỗ Tự Nhiên/gi, "Natural Wood Dining Chair"],
  [/Ghế Ăn Gỗ/gi, "Wooden Dining Chair"],
  [/Ghế Đôn Sofa Gỗ Cao Su Tự Nhiên/gi, "Natural Rubber Wood Sofa Ottoman"],
  [/Ghế Đôn Sofa/gi, "Sofa Ottoman"],
  [/Ghế Armchair Thư Giãn Kèm Đôn/gi, "Relaxing Armchair with Ottoman"],
  [/Ghế Armchair Thư Giãn/gi, "Relaxing Armchair"],
  [/Ghế Armchair/gi, "Armchair"],
  [/Ghế sofa góc/gi, "Corner Sofa Chair"],
  [/Ghế Sofa Góc Chữ L Gỗ Cao Su Tự Nhiên/gi, "Natural Rubber Wood L-Shaped Corner Sofa"],
  [/Ghế Sofa Góc Chữ L/gi, "L-Shaped Corner Sofa"],
  [/Ghế Sofa Băng/gi, "Sofa Bench"],
  [/Ghế Sofa Da/gi, "Leather Sofa"],
  [/Ghế Sofa Gỗ Cao Su Tự Nhiên/gi, "Natural Rubber Wood Sofa"],
  [/Ghế Sofa Gỗ/gi, "Wooden Sofa"],
  [/Ghế Sofa/gi, "Sofa"],
  [/Ghế Đôn/gi, "Ottoman"],
  [/Ghế Ăn Bọc Đệm/gi, "Cushion Upholstered Dining Chair"],
  [/Ghế Ăn/gi, "Dining Chair"],
  [/Ghế Gỗ/gi, "Wooden Chair"],
  
  [/Giường Ngủ Có Hộc & Ổ Điện/gi, "Bed with Drawers & Power Outlet"],
  [/Giường Ngủ Bọc Vải Phong Cách Ý/gi, "Italian Style Fabric Upholstered Bed"],
  [/Giường Ngủ Bọc Vải Cao Cấp/gi, "Premium Fabric Upholstered Bed"],
  [/Giường Ngủ Bọc Vải/gi, "Fabric Upholstered Bed"],
  [/Giường Ngủ Bọc Da Cao Cấp Hiện Đại/gi, "Modern Premium Leather Bed"],
  [/Giường Ngủ Bọc Da/gi, "Leather Bed"],
  [/Giường Ngủ/gi, "Bed"],
  [/Giường/gi, "Bed"],
  
  [/Bàn Trang Điểm Gỗ Đa Năng/gi, "Multipurpose Wooden Dressing Table"],
  [/Bàn Trang Điểm Gỗ/gi, "Wooden Dressing Table"],
  [/Bàn Trang Điểm/gi, "Dressing Table"],
  [/Bàn Làm Việc Gỗ/gi, "Wooden Desk"],
  [/Bàn Làm Việc/gi, "Desk"],
  [/Bàn Máy Tính Gỗ/gi, "Wooden Computer Desk"],
  [/Bàn Máy Tính/gi, "Computer Desk"],
  [/Bàn Ăn Gỗ Cao Su/gi, "Rubber Wood Dining Table"],
  [/Bàn Ăn Gỗ/gi, "Wooden Dining Table"],
  [/Bàn Ăn Tròn/gi, "Round Dining Table"],
  [/Bàn Ăn/gi, "Dining Table"],

  [/Tủ Quần Áo Cửa Lùa/gi, "Sliding Door Wardrobe"],
  [/Tủ Quần Áo Gỗ Kệ Ngăn Tay Nắm/gi, "Wooden Wardrobe with Shelves & Handles"],
  [/Tủ Quần Áo Gỗ Thanh Treo Tay Nắm/gi, "Wooden Wardrobe with Hanger Rod & Handles"],
  [/Tủ Quần Áo Gỗ Thanh Treo/gi, "Wooden Wardrobe with Hanger Rod"],
  [/Tủ Quần Áo Gỗ Ngăn Kệ/gi, "Wooden Wardrobe with Shelves"],
  [/Tủ Quần Áo Kèm Tủ Nóc/gi, "Wardrobe with Top Cabinet"],
  [/Tủ Quần Áo Gỗ/gi, "Wooden Wardrobe"],
  [/Tủ Quần Áo Nóc/gi, "Wardrobe Top Cabinet"],
  [/Tủ Quần Áo/gi, "Wardrobe"],
  
  [/Tủ Đầu Giường Gỗ/gi, "Wooden Bedside Table"],
  [/Tủ Đầu Giường/gi, "Bedside Table"],
  [/Tủ Giày – Tủ Trang Trí Gỗ/gi, "Wooden Shoe Cabinet - Decor Cabinet"],
  [/Tủ Giày/gi, "Shoe Cabinet"],
  [/Tủ Kệ Tivi Gỗ/gi, "Wooden TV Stand Cabinet"],
  [/Tủ Kệ Tivi/gi, "TV Stand Cabinet"],
  [/Tủ Kệ/gi, "Cabinet"],
  
  [/Combo Basic Phòng Khách/gi, "Basic Living Room Combo"],
  [/Full Combo Phòng Khách/gi, "Full Living Room Combo"],
  [/Combo Basic Phòng Ăn/gi, "Basic Dining Room Combo"],
  [/Combo Basic Phòng Ngủ/gi, "Basic Bedroom Combo"],
  [/Full Combo Phòng Ngủ/gi, "Full Bedroom Combo"],
  [/Combo Phòng Ăn/gi, "Dining Room Combo"],
  [/Combo Phòng Ngủ/gi, "Bedroom Combo"],
  [/Combo Phòng Khách/gi, "Living Room Combo"],
  [/Full Combo/gi, "Full Combo"],
  [/Combo Basic/gi, "Basic Combo"],
  [/Combo Cơ Bản/gi, "Basic Combo"],
  
  [/Vỏ Bọc Nệm Sofa/gi, "Sofa Cushion Cover"],
  [/Vỏ Bọc Nệm/gi, "Cushion Cover"],
  [/Đệm Lưng Tháo Rời/gi, "Detachable Back Cushion"],
  
  // Specs and compound materials
  [/Kích thước:/gi, "Dimensions:"],
  [/Chất liệu:/gi, "Material:"],
  [/Tiêu chuẩn:/gi, "Standard:"],
  [/Thân tủ:/gi, "Cabinet Body:"],
  [/Thân:/gi, "Body:"],
  [/Khung Sofa:/gi, "Sofa Frame:"],
  [/Khung:/gi, "Frame:"],
  [/Cánh tủ:/gi, "Cabinet Door:"],
  [/Ray trượt:/gi, "Drawer Slides:"],
  [/Mặt tủ:/gi, "Cabinet Surface:"],
  [/Bàn:/gi, "Table:"],
  [/Chân:/gi, "Legs:"],
  [/Mặt:/gi, "Surface:"],
  [/Đệm:/gi, "Cushion:"],
  [/Tựa:/gi, "Backrest:"],
  [/Vải:/gi, "Fabric:"],
  [/Hộc Kéo/gi, "Drawers"],
  [/Hộc/gi, "Drawers"],
  [/3 Cánh/gi, "3 Doors"],
  [/4 Cánh/gi, "4 Doors"],
  [/2 Cánh/gi, "2 Doors"],
  [/Thanh Treo/gi, "Hanger Rod"],
  [/Ngăn Kệ/gi, "Shelves"],
  [/Ổ Điện/gi, "Power Outlet"],
  [/Có Gương/gi, "with Mirror"],
  [/Giảm chấn IVANs cao cấp/gi, "Premium IVANs soft-closing"],
  [/Giảm chấn/gi, "Soft-closing"],
  [/Cao cấp/gi, "Premium"],
  [/cao cấp/gi, "premium"],
  
  // Colors & Styles
  [/Phong cách Hàn/gi, "Korean Style"],
  [/Phong Cách Hàn/gi, "Korean Style"],
  [/Phong cách Ý/gi, "Italian Style"],
  [/Phong Cách Ý/gi, "Italian Style"],
  [/Màu Tự Nhiên/gi, "Natural Color"],
  [/Màu tự nhiên/gi, "Natural color"],
  [/Màu Nâu Hạnh Nhân/gi, "Almond Brown"],
  [/Màu Nâu \/ Đen/gi, "Brown / Black"],
  [/Màu Nâu/gi, "Brown"],
  [/Màu Gỗ phối trắng/gi, "Wood color with white"],
  [/Màu gỗ phối trắng/gi, "Wood color with white"],
  [/Màu xám đậm/gi, "Dark Grey"],
  [/Màu Gỗ/gi, "Wood Color"],
  [/Màu Be/gi, "Beige"],
  [/màu be/gi, "beige"],
  [/Xám/gi, "Grey"],
  [/Trắng/gi, "White"],
  [/Đen/gi, "Black"],
  [/Mặt Vân Đá/gi, "Stone Veneer Surface"],
  [/mặt vân đá/gi, "stone pattern surface"],
  [/Nhiều Kích Thước/gi, "Multiple Sizes"],
  
  // Details
  [/Gỗ công nghiệp đạt chuẩn/gi, "Engineered wood complying with"],
  [/Gỗ công nghiệp/gi, "Engineered wood"],
  [/Gỗ cao su tự nhiên/gi, "Natural rubber wood"],
  [/Gỗ cao su/gi, "Rubber wood"],
  [/Gỗ tự nhiên/gi, "Natural wood"],
  [/Vải cao cấp/gi, "Premium fabric"],
  [/phủ veneer gỗ sồi/gi, "coated with oak veneer"],
  [/phủ melamine trắng nhám/gi, "coated with matte white melamine"],
  [/Không tay nắm/gi, "No handles"],
  [/Lò xo chữ S \+ dây đai đàn hồi/gi, "S-spring + elastic bands"],
  [/Chống bám bẩn/gi, "Stain-resistant"],
  [/2 chỗ/gi, "2-seater"],
  [/3 chỗ/gi, "3-seater"],
  [/gỗ phối trắng/gi, "wood mixed with white"],
  [/Dài (\d+)/gi, "L $1"],
  [/Rộng (\d+)/gi, "W $1"],
  [/Cao (\d+)/gi, "H $1"],
];

// 2. Standalone single words using Unicode-aware word boundaries
const wordTranslations = [
  ["có", "with"],
  ["và", "and"],
  ["kèm", "with"],
  ["hoặc", "or"],
  ["tủ", "Cabinet"],
  ["bàn", "Table"],
  ["ghế", "Chair"],
  ["giường", "Bed"],
  ["nệm", "Cushion"],
  ["đệm", "Cushion"],
  ["tựa", "Backrest"],
  ["vải", "Fabric"],
  ["da", "Leather"],
  ["gỗ", "Wood"],
  ["sắt", "Iron"],
  ["nhám", "matte"],
  ["màu", "color"],
  ["phủ", "coated with"],
  ["phòng", "room"],
  ["khách", "living/guest"],
  ["ăn", "dining"],
  ["ngủ", "sleeping"],
  ["chỗ", "seater"]
];

function translateVietnameseToEnglish(text) {
  if (!text) return "";
  let result = text;
  
  // Apply phrase replacements first
  for (const [regex, replacement] of phraseMap) {
    result = result.replace(regex, replacement);
  }
  
  // Apply word replacements with Unicode-safe boundaries
  for (const [word, replacement] of wordTranslations) {
    const regex = new RegExp(`(^|[^a-zA-Z0-9_À-ỹ])${word}($|[^a-zA-Z0-9_À-ỹ])`, 'gi');
    result = result.replace(regex, `$1${replacement}$2`);
  }
  
  return result;
}

async function main() {
  await connectDB();

  console.log("Fetching all products from DB...");
  const products = await Product.find({}).lean();
  console.log(`Found ${products.length} products to translate.`);

  let successCount = 0;

  for (const product of products) {
    // 1. Vi translation (original content)
    await ProductTranslation.findOneAndUpdate(
      { product_id: product._id, language_code: "vi" },
      {
        name: product.name,
        short_description: product.short_description || "",
        description: product.description || "",
      },
      { upsert: true }
    );

    // 2. En translation (translated content)
    const enName = translateVietnameseToEnglish(product.name);
    const enShortDesc = translateVietnameseToEnglish(product.short_description);
    const enDesc = translateVietnameseToEnglish(product.description);

    await ProductTranslation.findOneAndUpdate(
      { product_id: product._id, language_code: "en" },
      {
        name: enName,
        short_description: enShortDesc,
        description: enDesc,
      },
      { upsert: true }
    );

    successCount++;
    if (successCount % 10 === 0 || successCount === products.length) {
      console.log(`Processed translations for ${successCount}/${products.length} products.`);
    }
  }

  console.log("Migration completed successfully!");
  process.exit(0);
}

main().catch((error) => {
  console.error("Migration failed:", error);
  process.exit(1);
});
