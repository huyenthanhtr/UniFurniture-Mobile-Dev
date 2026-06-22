const makeGenericRouter = require("./generic.routes");
const Profile = require("../models/Profile");
const { changePassword, uploadAvatar } = require("../controllers/profile.controller");

const router = makeGenericRouter(Profile);

router.post("/:id/change-password", changePassword);
router.post("/:id/avatar", uploadAvatar);

module.exports = router;
