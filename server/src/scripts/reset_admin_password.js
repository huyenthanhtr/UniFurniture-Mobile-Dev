const mongoose = require("mongoose");
const crypto = require("crypto");
const Profile = require("../models/Profile");

const MONGO_URI = "mongodb+srv://dbUser:khaitbh123@cluster0.80kgvn4.mongodb.net/ecommerce?appName=Cluster0";

function hashValue(value) {
    return crypto.createHash('sha256').update(value).digest('hex');
}

function normalizePhone(phone) {
    if (!phone) return phone;
    let p = phone.replace(/\s+/g, '').replace('+', '');
    if (p.startsWith('84') && p.length >= 11) {
        return '0' + p.substring(2);
    }
    return p;
}

async function run() {
    try {
        await mongoose.connect(MONGO_URI);
        console.log("Connected to MongoDB!");

        // 1. Customer Profile: 0376215924 -> Khaitbh123@
        const custPhone = "0376215924";
        const custPass = "Khaitbh123@";
        const custHash = hashValue(custPass);
        
        let customer = await Profile.findOne({ phone: custPhone });
        if (!customer) {
            // Try with 84 format just in case
            customer = await Profile.findOne({ phone: "84" + custPhone.substring(1) });
        }

        if (customer) {
            console.log(`Found Customer: ${customer.phone} (${customer.full_name})`);
            customer.password_hash = custHash;
            await customer.save();
            console.log(`Updated Customer password hash to SHA-256 of: ${custPass}`);
        } else {
            console.log(`Customer profile for phone ${custPhone} NOT found in database. Creating a new one...`);
            // Create a test customer profile if not exists
            customer = new Profile({
                phone: custPhone,
                email: "customer_test@unifurniture.com",
                password_hash: custHash,
                full_name: "Test Customer",
                role: "customer",
                account_status: "active"
            });
            await customer.save();
            console.log("Successfully created test customer profile.");
        }

        // 2. Admin Profile: 0666666666 -> 12345678
        const adminPhone = "0666666666";
        const adminPass = "12345678";
        const adminHash = hashValue(adminPass);

        let admin = await Profile.findOne({ phone: adminPhone });
        if (!admin) {
            // Try with 84 format just in case
            admin = await Profile.findOne({ phone: "84" + adminPhone.substring(1) });
        }

        if (admin) {
            console.log(`Found Admin: ${admin.phone} (${admin.full_name})`);
            admin.password_hash = adminHash;
            await admin.save();
            console.log(`Updated Admin password hash to SHA-256 of: ${adminPass}`);
        } else {
            console.log(`Admin profile for phone ${adminPhone} NOT found in database. Creating a new one...`);
            // Create a test admin profile if not exists
            admin = new Profile({
                phone: adminPhone,
                email: "admin_test@unifurniture.com",
                password_hash: adminHash,
                full_name: "Test Admin",
                role: "admin",
                account_status: "active"
            });
            await admin.save();
            console.log("Successfully created test admin profile.");
        }

    } catch (err) {
        console.error("Error:", err);
    } finally {
        await mongoose.disconnect();
    }
}

run();
