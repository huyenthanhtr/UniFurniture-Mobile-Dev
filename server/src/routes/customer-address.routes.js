const express = require("express");
const {
  listCustomerAddresses,
  createCustomerAddress,
  updateCustomerAddress,
  deleteCustomerAddress,
} = require("../controllers/customer-address.controller");

const router = express.Router();

router.get("/", listCustomerAddresses);
router.post("/", createCustomerAddress);
router.patch("/:id", updateCustomerAddress);
router.delete("/:id", deleteCustomerAddress);

module.exports = router;
