const express = require("express");
const { getProfileLoyalty, estimatePoints, getPointTransactions } = require("../controllers/loyalty.controller");

const router = express.Router();

router.get("/profiles/:profileId", getProfileLoyalty);
router.get("/estimate", estimatePoints);
router.get("/profiles/:profileId/transactions", getPointTransactions);

module.exports = router;

