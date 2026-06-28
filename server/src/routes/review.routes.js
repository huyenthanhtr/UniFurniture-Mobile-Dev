const express = require('express');
const multer = require('multer');

const router = express.Router();
const reviewController = require('../controllers/review.controller');
const { uploadReviewMedia, REVIEW_MEDIA_RULES } = require('../middlewares/upload-review-media');

router.get('/media-config', reviewController.getReviewMediaConfig);
router.post('/media', (req, res, next) => {
  uploadReviewMedia.array('files', REVIEW_MEDIA_RULES.maxTotalFiles)(req, res, (error) => {
    if (!error) {
      next();
      return;
    }

    if (error instanceof multer.MulterError) {
      if (error.code === 'LIMIT_FILE_SIZE') {
        return res.status(400).json({ message: `Each file must be <= ${REVIEW_MEDIA_RULES.maxFileSizeMb} MB.` });
      }
      if (error.code === 'LIMIT_FILE_COUNT') {
        return res.status(400).json({ message: `Maximum ${REVIEW_MEDIA_RULES.maxTotalFiles} files allowed.` });
      }
      return res.status(400).json({ message: error.message || 'Upload failed.' });
    }

    return res.status(400).json({ message: error.message || 'Upload failed.' });
  });
}, reviewController.uploadReviewMedia);
router.post('/', reviewController.createOrderReviews);
router.get('/order/:orderId/status', reviewController.getOrderReviewStatus);
router.get('/featured', reviewController.getFeaturedReviews);
router.get('/product/:productId', reviewController.getApprovedReviewsByProduct);
router.get('/', reviewController.getAllReviews);
router.patch('/:id/status', reviewController.updateReviewStatus);
router.post('/:id/reply', reviewController.replyToReview);

module.exports = router;

