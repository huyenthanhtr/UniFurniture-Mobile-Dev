const path = require("path");
require("dotenv").config({ path: path.resolve(__dirname, "../../.env") });
const mongoose = require("mongoose");
const connectDB = require("../configs/db");
const Profile = require("../models/Profile");
const PointTransaction = require("../models/PointTransaction");

async function run() {
  await connectDB();
  
  // 1. Find a customer profile that has points
  const profile = await Profile.findOne({ loyalty_points_lifetime: { $gt: 0 } }).lean();
  if (!profile) {
    console.log("❌ No profile found with points > 0");
    process.exit(0);
  }

  console.log("👤 FOUND PROFILE:");
  console.log(`- ID: ${profile._id}`);
  console.log(`- Name: ${profile.full_name}`);
  console.log(`- Phone: ${profile.phone}`);
  console.log(`- Points: ${profile.loyalty_points_lifetime}`);
  console.log(`- Tier: ${profile.membership_tier}`);

  // 2. Find PointTransactions for this profile
  const txs = await PointTransaction.find({ profile_id: profile._id }).lean();
  console.log(`\n📊 POINT TRANSACTIONS COUNT: ${txs.length}`);
  
  if (txs.length > 0) {
    txs.forEach((tx, index) => {
      console.log(`[${index + 1}] Type: ${tx.type}, Points: ${tx.points}, Note: ${tx.note}, OrderId: ${tx.order_id}`);
    });
  } else {
    console.log("⚠️ No transaction history found for this profile in point_transactions collection!");
  }

  // 3. Find any PointTransactions in the DB to see if they are linked to customer_id instead of profile_id
  const anyTx = await PointTransaction.findOne().lean();
  if (anyTx) {
    console.log("\n🔍 INSPECTING AN ARBITRARY TRANSACTION:");
    console.log(anyTx);
  } else {
    console.log("\n❌ The point_transactions collection is completely empty!");
  }

  process.exit(0);
}

run().catch(err => {
  console.error(err);
  process.exit(1);
});
