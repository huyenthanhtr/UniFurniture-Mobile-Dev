const mongoose = require("mongoose");
const MONGO_URI = "mongodb+srv://dbUser:khaitbh123@cluster0.80kgvn4.mongodb.net/ecommerce?appName=Cluster0";

const ProductImage = require("./src/models/ProductImage");
const ProductVariant = require("./src/models/ProductVariant");

async function run() {
  try {
    await mongoose.connect(MONGO_URI);
    console.log("Connected to MongoDB!");

    const productId = "69a6c3ec435745d9034c497d";

    // Test images filter logic
    console.log(`\n--- Querying ProductImages directly with string product_id: ${productId} ---`);
    const imagesRaw = await ProductImage.find({ product_id: productId }).lean();
    console.log("Found direct images:", imagesRaw.length);

    console.log(`\n--- Querying ProductVariants directly with string product_id: ${productId} ---`);
    const variantsRaw = await ProductVariant.find({ product_id: productId }).lean();
    console.log("Found direct variants:", variantsRaw.length);

    // Let's test the route filter construction
    const reqQuery1 = { product_id: productId };
    const filter1 = {};
    for (const [k, v] of Object.entries(reqQuery1)) {
      filter1[k] = v;
    }
    const imagesFilter = await ProductImage.find(filter1).lean();
    console.log("\nUsing constructed filter (images):", JSON.stringify(filter1));
    console.log("Found with constructed filter:", imagesFilter.length);

    const reqQuery2 = { product_id: productId, variant_status: "active" };
    const filter2 = {};
    for (const [k, v] of Object.entries(reqQuery2)) {
      filter2[k] = v;
    }
    const variantsFilter = await ProductVariant.find(filter2).lean();
    console.log("\nUsing constructed filter (variants):", JSON.stringify(filter2));
    console.log("Found with constructed filter:", variantsFilter.length);

  } catch (err) {
    console.error("Error:", err);
  } finally {
    await mongoose.disconnect();
  }
}

run();
