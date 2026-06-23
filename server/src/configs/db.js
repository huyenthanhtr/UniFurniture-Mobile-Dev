const mongoose = require('mongoose');
const dns = require('dns');

// Cấu hình Node.js sử dụng DNS của Google/Cloudflare để sửa lỗi querySrv ECONNREFUSED
dns.setServers(['8.8.8.8', '1.1.1.1']);
const connectDB = async () => {
    try {
        await mongoose.connect(process.env.MONGO_URI, {
            family: 4,
            serverSelectionTimeoutMS: 10000,
        });
        console.log("Đã kết nối MongoDB thành công vào database: ecommerce");
    } catch (error) {
        console.error("Lỗi kết nối MongoDB:", error.message);
        process.exit(1);
    }
};

module.exports = connectDB;
