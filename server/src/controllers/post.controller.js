const mongoose = require("mongoose");
const Post = require("../models/Post");

const DEFAULT_POSTS = [
  {
    title: "5 cach bo tri phong khach nho van thoang",
    caption: "Goi y ti le sofa, ban tra va ke trang tri de can ho gon hon.",
    content:
      "Phong khach nho can uu tien luu thong va anh sang. Hay chon sofa chan cao, ban tra gon, ke sat tuong va giu loi di chinh rong toi thieu 70cm de khong gian de tho hon.",
    thumbnail_url: "/assets/images/community1.jpg",
    post_category: "Khong gian song",
  },
  {
    title: "Chon go ben vung cho noi that gia dinh",
    caption: "Nhung dau hieu giup ban nhan biet vat lieu go co nguon goc tot.",
    content:
      "Vat lieu go ben vung nen co nguon goc ro, be mat hoan thien on dinh va duoc bao quan dung cach. Cac chung nhan nhu FSC giup khach hang yen tam hon khi dau tu dai han.",
    thumbnail_url: "/assets/images/FSC Certification (Forest Stewardship Council).jpg",
    post_category: "Vat lieu",
  },
  {
    title: "Can bang mau sac noi that theo anh sang tu nhien",
    caption: "Cach phoi mau sofa, tham va tu de nha am ma khong bi nang.",
    content:
      "Nha nhieu anh sang co the dung gam go am va vai trung tinh. Nha it sang nen tang be mat sang mau, them diem nhan xanh la hoac vang dong de khong gian co chieu sau.",
    thumbnail_url: "/assets/images/community2.webp",
    post_category: "Cam nang",
  },
  {
    title: "Checklist truoc ngay giao va lap dat",
    caption: "Nhung viec nen chuan bi de doi giao hang thao tac nhanh hon.",
    content:
      "Truoc ngay giao hang, hay do lai thang may, hanh lang, cua ra vao va don san khu vuc lap dat. Neu can doi lich, nen bao truoc 24 gio de don hang khong bi gian doan.",
    thumbnail_url: "/assets/images/community3.jpg",
    post_category: "Dich vu",
  },
  {
    title: "Goc lam viec tai nha it mon nhung hieu qua",
    caption: "Ban, ghe va anh sang la ba diem nen dau tu dau tien.",
    content:
      "Mot goc lam viec tot bat dau tu chieu cao ban phu hop, ghe co nang do lung va nguon sang khong tao bong tren mat ban. Them hop luu tru nho giup mat ban luon gon.",
    thumbnail_url: "/assets/images/about1.jpg",
    post_category: "Lam viec",
  },
  {
    title: "Bao quan sofa vai trong mua mua",
    caption: "Giam am moc va mui kho chiu bang vai thoi quen don gian.",
    content:
      "Sofa vai can duoc hut bui dinh ky, tranh dat sat tuong am va nen thong gio sau nhung ngay mua keo dai. Vet do nen xu ly som bang khan am sach truoc khi tham sau.",
    thumbnail_url: "/assets/images/community4.jpg",
    post_category: "Bao quan",
  },
  {
    title: "Vi sao nen chon noi that co bao hanh ro rang",
    caption: "Bao hanh minh bach giup ban an tam sau khi hoan thien nha.",
    content:
      "Noi that la khoan dau tu dai han. Chinh sach bao hanh ro ve thoi gian, pham vi va dieu kien loai tru giup khach hang chu dong khi can bao tri hoac sua chua.",
    thumbnail_url: "/assets/images/BIFMACertification.jpeg",
    post_category: "Chinh sach",
  },
  {
    title: "Thiet ke nha dau tien: mua gi truoc, mua gi sau",
    caption: "Thu tu uu tien giup ngan sach noi that de kiem soat hon.",
    content:
      "Hay dau tu truoc vao giuong, nem, ban an, sofa va he luu tru chinh. Do trang tri, den phu va phu kien nen mua sau khi da song thu vai tuan de biet nhu cau that.",
    thumbnail_url: "/assets/images/banner6.jpg",
    post_category: "Mua sam",
  },
];

function normalizePostPayload(body = {}) {
  const title = String(body.title || "").trim();
  const status = ["draft", "published"].includes(String(body.status || "").toLowerCase())
    ? String(body.status).toLowerCase()
    : "draft";

  return {
    title,
    slug: String(body.slug || "").trim(),
    caption: String(body.caption || body.excerpt || "").trim(),
    content: String(body.content || "").trim(),
    thumbnail_url: String(body.thumbnail_url || body.thumbnailUrl || "").trim(),
    post_category: String(body.post_category || body.category || "Blog").trim() || "Blog",
    status,
    published_at: body.published_at || (status === "published" ? new Date() : null),
  };
}

async function ensureDefaultPosts() {
  const count = await Post.estimatedDocumentCount();
  if (count > 0) return;

  const now = new Date();
  await Post.insertMany(
    DEFAULT_POSTS.map((post, index) => ({
      ...post,
      slug: Post.buildSlug(post.title),
      status: "published",
      published_at: new Date(now.getTime() - index * 24 * 60 * 60 * 1000),
    })),
    { ordered: false }
  ).catch((error) => {
    if (error?.code !== 11000) throw error;
  });
}

function buildFilter(query = {}) {
  const filter = {};
  const status = String(query.status || "").trim().toLowerCase();
  if (["draft", "published"].includes(status)) {
    filter.status = status;
  }

  const slug = String(query.slug || "").trim();
  if (slug) {
    filter.slug = slug;
  }

  const q = String(query.q || query.search || "").trim();
  if (q) {
    filter.$or = [
      { title: { $regex: q, $options: "i" } },
      { caption: { $regex: q, $options: "i" } },
      { post_category: { $regex: q, $options: "i" } },
    ];
  }

  return filter;
}

function buildSort(sortQuery) {
  const raw = String(sortQuery || "-published_at").trim();
  const key = raw.startsWith("-") ? raw.slice(1) : raw;
  const direction = raw.startsWith("-") ? -1 : 1;
  const allowed = new Set(["title", "createdAt", "updatedAt", "published_at", "status"]);
  return { [allowed.has(key) ? key : "published_at"]: direction, createdAt: -1 };
}

async function getPosts(req, res, next) {
  try {
    await ensureDefaultPosts();

    const page = Math.max(parseInt(req.query.page || "1", 10), 1);
    const limit = Math.min(Math.max(parseInt(req.query.limit || "20", 10), 1), 200);
    const skip = (page - 1) * limit;
    const filter = buildFilter(req.query);
    const sort = buildSort(req.query.sort);

    const [items, total] = await Promise.all([
      Post.find(filter).sort(sort).skip(skip).limit(limit).lean(),
      Post.countDocuments(filter),
    ]);

    res.json({
      page,
      limit,
      total,
      totalPages: Math.ceil(total / limit) || 1,
      items,
    });
  } catch (err) {
    next(err);
  }
}

async function createPost(req, res, next) {
  try {
    const payload = normalizePostPayload(req.body);
    if (!payload.title) {
      return res.status(400).json({ message: "Title is required" });
    }
    const doc = await Post.create(payload);
    res.status(201).json(doc);
  } catch (err) {
    if (err?.code === 11000) {
      return res.status(400).json({ message: "Slug already exists" });
    }
    next(err);
  }
}

async function updatePost(req, res, next) {
  try {
    const { id } = req.params;
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({ message: "Invalid id" });
    }

    const payload = normalizePostPayload(req.body);
    if (!payload.title) {
      return res.status(400).json({ message: "Title is required" });
    }

    const doc = await Post.findByIdAndUpdate(id, payload, {
      new: true,
      runValidators: true,
    });
    if (!doc) return res.status(404).json({ message: "Not found" });
    res.json(doc);
  } catch (err) {
    if (err?.code === 11000) {
      return res.status(400).json({ message: "Slug already exists" });
    }
    next(err);
  }
}

async function deletePost(req, res, next) {
  try {
    const { id } = req.params;
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({ message: "Invalid id" });
    }
    const doc = await Post.findByIdAndDelete(id);
    if (!doc) return res.status(404).json({ message: "Not found" });
    res.json({ success: true, deleted: doc });
  } catch (err) {
    next(err);
  }
}

module.exports = {
  getPosts,
  createPost,
  updatePost,
  deletePost,
};
