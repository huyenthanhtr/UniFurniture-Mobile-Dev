const multer = require('multer');
const path = require('path');
const fs = require('fs');

const uploadDir = path.join(process.cwd(), 'uploads', 'reviews');

const REVIEW_MEDIA_RULES = Object.freeze({
  maxImages: 5,
  maxVideos: 2,
  maxTotalFiles: 7,
  maxFileSizeBytes: 30 * 1024 * 1024,
  maxFileSizeMb: 30,
  acceptedTypes: ['image/*', 'video/*'],
});

const storage = multer.diskStorage({
  destination(req, file, cb) {
    fs.mkdirSync(uploadDir, { recursive: true });
    cb(null, uploadDir);
  },
  filename(req, file, cb) {
    const ext = path.extname(file.originalname).toLowerCase();
    const base = file.mimetype.startsWith('video/') ? 'review-video' : 'review-image';
    const unique = `${Date.now()}-${Math.round(Math.random() * 1e9)}`;
    cb(null, `${base}-${unique}${ext}`);
  },
});

function ensureUploadCounters(req) {
  if (!req._reviewUploadCounters) {
    req._reviewUploadCounters = { images: 0, videos: 0 };
  }
  return req._reviewUploadCounters;
}

const fileFilter = (req, file, cb) => {
  const mime = String(file?.mimetype || '').toLowerCase();
  const counters = ensureUploadCounters(req);

  if (mime.startsWith('image/')) {
    counters.images += 1;
    if (counters.images > REVIEW_MEDIA_RULES.maxImages) {
      cb(new Error(`Maximum ${REVIEW_MEDIA_RULES.maxImages} images allowed.`), false);
      return;
    }
    cb(null, true);
    return;
  }

  if (mime.startsWith('video/')) {
    counters.videos += 1;
    if (counters.videos > REVIEW_MEDIA_RULES.maxVideos) {
      cb(new Error(`Maximum ${REVIEW_MEDIA_RULES.maxVideos} videos allowed.`), false);
      return;
    }
    cb(null, true);
    return;
  }

  cb(new Error('Only image/video files are allowed.'), false);
};

const uploadReviewMedia = multer({
  storage,
  fileFilter,
  limits: {
    fileSize: REVIEW_MEDIA_RULES.maxFileSizeBytes,
    files: REVIEW_MEDIA_RULES.maxTotalFiles,
  },
});

module.exports = {
  uploadReviewMedia,
  REVIEW_MEDIA_RULES,
};
