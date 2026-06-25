const mongoose = require("mongoose");
const Post = require("../models/Post");

const SUPPORTED_LANGS = ["vi", "en", "fr", "zh"];
const DEFAULT_LANG = "vi";
const DAY = 24 * 60 * 60 * 1000;

const DEFAULT_POSTS = [
  {
    slug: "5-cach-bo-tri-phong-khach-nho-van-thoang",
    thumbnail_url: "/assets/images/community1.jpg",
    translations: {
      vi: {
        title: "5 cách bố trí phòng khách nhỏ vẫn thoáng",
        caption: "Gợi ý tỉ lệ sofa, bàn trà và kệ trang trí để căn hộ gọn hơn.",
        post_category: "Không gian sống",
        content:
          "<p>Phòng khách nhỏ cần ưu tiên lối đi, ánh sáng và các món nội thất có tỉ lệ vừa đủ. Sofa chân cao, bàn trà bo cạnh và kệ sát tường giúp không gian nhẹ mắt hơn mà vẫn giữ đủ công năng.</p><p>Trước khi mua, bạn nên đo lại chiều ngang cửa, khoảng cách từ sofa đến bàn trà và lối đi chính. Một lối đi khoảng 70cm sẽ giúp sinh hoạt hằng ngày thoải mái hơn, nhất là khi nhà có trẻ nhỏ hoặc thường xuyên đón khách.</p>",
      },
      en: {
        title: "5 ways to keep a small living room airy",
        caption: "Sofa, coffee table, and shelving proportions for a neater apartment.",
        post_category: "Living spaces",
        content:
          "<p>A small living room works best when circulation, light, and scale come first. Raised sofas, rounded coffee tables, and wall-hugging shelves make the room feel lighter without losing everyday function.</p><p>Before buying, measure door widths, the distance between sofa and table, and the main walkway. Around 70cm of clear space helps daily movement feel easier, especially for families or homes that host guests often.</p>",
      },
      fr: {
        title: "5 idées pour garder un petit salon aéré",
        caption: "Proportions de canapé, table basse et étagères pour un appartement plus net.",
        post_category: "Espaces de vie",
        content:
          "<p>Dans un petit salon, la circulation, la lumière et les bonnes proportions passent avant tout. Un canapé sur pieds, une table basse aux angles doux et des rangements près du mur allègent visuellement la pièce.</p><p>Avant l'achat, mesurez la largeur des portes, la distance entre le canapé et la table, ainsi que le passage principal. Environ 70cm de dégagement rendent les gestes du quotidien plus fluides.</p>",
      },
      zh: {
        title: "小客厅保持通透的 5 个布置方法",
        caption: "用合适的沙发、茶几和收纳比例，让小户型更整洁。",
        post_category: "生活空间",
        content:
          "<p>小客厅最需要先考虑动线、采光和家具比例。高脚沙发、圆角茶几和靠墙收纳能减轻视觉负担，同时保留日常功能。</p><p>购买前建议测量门宽、沙发到茶几的距离以及主要通道。约 70cm 的通行空间会让日常活动更舒适，尤其适合家庭使用。</p>",
      },
    },
  },
  {
    slug: "chon-go-ben-vung-cho-noi-that-gia-dinh",
    thumbnail_url: "/assets/images/FSC Certification (Forest Stewardship Council).jpg",
    translations: {
      vi: {
        title: "Chọn gỗ bền vững cho nội thất gia đình",
        caption: "Những dấu hiệu giúp bạn nhận biết vật liệu gỗ có nguồn gốc tốt.",
        post_category: "Vật liệu",
        content:
          "<p>Gỗ bền vững không chỉ đẹp ở bề mặt mà còn cần có nguồn gốc rõ ràng, quy trình xử lý ổn định và khả năng sử dụng lâu dài. Khi xem sản phẩm, hãy chú ý chứng nhận vật liệu, độ hoàn thiện mép cạnh và mùi sơn phủ.</p><p>Một món đồ tốt nên có thông tin bảo hành minh bạch, hướng dẫn bảo quản cụ thể và cấu trúc đủ chắc cho nhu cầu hằng ngày. Điều này giúp bạn giảm chi phí thay mới và hạn chế lãng phí vật liệu.</p>",
      },
      en: {
        title: "Choosing sustainable wood for family furniture",
        caption: "Simple signs that help you recognize responsibly sourced wood.",
        post_category: "Materials",
        content:
          "<p>Sustainable wood is not only about the surface finish. It should have clear sourcing, stable treatment, and enough durability for long-term use. Check material certificates, edge finishing, and the smell of coating when viewing a product.</p><p>A good piece should also come with transparent warranty information, practical care instructions, and a structure strong enough for everyday routines. That reduces replacement costs and material waste.</p>",
      },
      fr: {
        title: "Choisir un bois durable pour la maison",
        caption: "Des signes simples pour reconnaître un bois d'origine responsable.",
        post_category: "Matériaux",
        content:
          "<p>Un bois durable ne se résume pas à une belle finition. Il doit avoir une origine claire, un traitement stable et une résistance adaptée à un usage quotidien. Vérifiez les certifications, la finition des chants et l'odeur du vernis.</p><p>Un bon meuble doit aussi proposer une garantie lisible, des conseils d'entretien concrets et une structure suffisamment solide. Vous limitez ainsi les remplacements et le gaspillage de matériaux.</p>",
      },
      zh: {
        title: "为家庭家具选择更可持续的木材",
        caption: "几个简单信号，帮助你判断木材来源是否可靠。",
        post_category: "材料",
        content:
          "<p>可持续木材不只看表面是否漂亮，还要关注来源、处理工艺和长期使用稳定性。挑选时可以留意材料认证、边缘收口以及表面涂装气味。</p><p>一件好的家具也应该有清楚的保修信息、具体的保养建议和足够稳固的结构。这样可以减少更换成本，也能降低材料浪费。</p>",
      },
    },
  },
  {
    slug: "can-bang-mau-sac-noi-that-theo-anh-sang",
    thumbnail_url: "/assets/images/community2.webp",
    translations: {
      vi: {
        title: "Cân bằng màu sắc nội thất theo ánh sáng tự nhiên",
        caption: "Cách phối sofa, thảm và tủ để nhà ấm mà không bị nặng.",
        post_category: "Cẩm nang",
        content:
          "<p>Ánh sáng tự nhiên quyết định rất nhiều đến cảm giác màu sắc trong nhà. Không gian nhiều nắng có thể dùng gỗ ấm, vải trung tính và điểm nhấn xanh lá để tạo chiều sâu mà không bị chói.</p><p>Với nhà ít sáng, hãy ưu tiên bề mặt sáng màu, rèm mỏng và ánh sáng phụ ấm vừa phải. Bạn có thể thêm một món nhấn màu đất hoặc vàng đồng để căn phòng có cảm giác ấm hơn.</p>",
      },
      en: {
        title: "Balancing interior colors with natural light",
        caption: "How to match sofas, rugs, and cabinets without making the room heavy.",
        post_category: "Guide",
        content:
          "<p>Natural light strongly changes how colors feel at home. Sunny rooms can handle warm wood, neutral fabrics, and green accents for depth without looking harsh.</p><p>For darker homes, choose brighter surfaces, lighter curtains, and gentle warm lighting. A small earthy or brass accent can make the room feel warmer without overwhelming it.</p>",
      },
      fr: {
        title: "Équilibrer les couleurs avec la lumière naturelle",
        caption: "Associer canapé, tapis et rangements sans alourdir la pièce.",
        post_category: "Guide",
        content:
          "<p>La lumière naturelle modifie fortement la perception des couleurs. Une pièce très lumineuse accepte le bois chaud, les tissus neutres et quelques touches de vert pour gagner en profondeur.</p><p>Dans un logement moins lumineux, privilégiez les surfaces claires, les voilages légers et une lumière d'appoint douce. Une touche terre cuite ou laiton peut réchauffer l'ensemble.</p>",
      },
      zh: {
        title: "根据自然光平衡家居配色",
        caption: "沙发、地毯和柜体这样搭配，温暖但不压抑。",
        post_category: "指南",
        content:
          "<p>自然光会明显影响家中颜色的观感。采光好的空间可以使用温暖木色、中性色布艺和绿色点缀，让空间更有层次但不刺眼。</p><p>采光不足的房间建议优先选择浅色表面、轻薄窗帘和柔和暖光。少量大地色或铜色点缀能增加温度感。</p>",
      },
    },
  },
  {
    slug: "checklist-truoc-ngay-giao-va-lap-dat",
    thumbnail_url: "/assets/images/community3.jpg",
    translations: {
      vi: {
        title: "Checklist trước ngày giao và lắp đặt",
        caption: "Những việc nên chuẩn bị để đội giao hàng thao tác nhanh hơn.",
        post_category: "Dịch vụ",
        content:
          "<p>Trước ngày giao hàng, bạn nên đo lại thang máy, hành lang, cửa ra vào và vị trí đặt sản phẩm. Những thông tin này giúp đội giao hàng chọn cách di chuyển phù hợp và hạn chế va chạm.</p><p>Hãy dọn sẵn khu vực lắp đặt, giữ lối đi thông thoáng và xác nhận khung giờ nhận hàng. Nếu cần đổi lịch, báo trước ít nhất 24 giờ sẽ giúp đơn hàng không bị gián đoạn.</p>",
      },
      en: {
        title: "Checklist before delivery and installation day",
        caption: "What to prepare so the delivery team can work faster.",
        post_category: "Service",
        content:
          "<p>Before delivery day, measure the elevator, hallway, doorways, and final product location. These details help the team choose the right route and reduce the risk of bumps or delays.</p><p>Clear the installation area, keep walkways open, and confirm your receiving time slot. If the schedule needs to change, informing the team at least 24 hours ahead keeps the order moving smoothly.</p>",
      },
      fr: {
        title: "Checklist avant la livraison et l'installation",
        caption: "Les préparatifs qui aident l'équipe à intervenir plus vite.",
        post_category: "Service",
        content:
          "<p>Avant la livraison, mesurez l'ascenseur, le couloir, les portes et l'emplacement final du meuble. Ces informations aident l'équipe à choisir le bon passage et à limiter les risques de choc.</p><p>Libérez la zone d'installation, gardez le passage dégagé et confirmez le créneau de réception. En cas de changement, prévenir au moins 24 heures à l'avance évite les interruptions.</p>",
      },
      zh: {
        title: "送货与安装前的准备清单",
        caption: "提前做好这些事，让配送安装更顺利。",
        post_category: "服务",
        content:
          "<p>送货前建议重新测量电梯、走廊、门口和最终摆放位置。这些信息能帮助配送团队选择合适路线，减少碰撞和延误。</p><p>请提前清理安装区域，保持通道顺畅，并确认收货时间。如果需要改期，至少提前 24 小时通知会更稳妥。</p>",
      },
    },
  },
  {
    slug: "goc-lam-viec-tai-nha-it-mon-hieu-qua",
    thumbnail_url: "/assets/images/about1.jpg",
    translations: {
      vi: {
        title: "Góc làm việc tại nhà ít món nhưng hiệu quả",
        caption: "Bàn, ghế và ánh sáng là ba điểm nên đầu tư đầu tiên.",
        post_category: "Làm việc",
        content:
          "<p>Một góc làm việc tốt không cần quá nhiều đồ. Bạn nên bắt đầu từ chiều cao bàn phù hợp, ghế có nâng đỡ lưng và nguồn sáng không tạo bóng trên mặt bàn.</p><p>Hộp lưu trữ nhỏ, khay dây điện và một kệ mỏng có thể giữ mặt bàn gọn suốt ngày. Khi không gian ít xao nhãng, việc học và làm việc tại nhà cũng dễ duy trì nhịp hơn.</p>",
      },
      en: {
        title: "A minimal but effective home workspace",
        caption: "Desk, chair, and lighting are the first three things to invest in.",
        post_category: "Work",
        content:
          "<p>A good home workspace does not need many items. Start with the right desk height, a chair that supports your back, and a light source that does not cast shadows across the surface.</p><p>Small storage boxes, cable trays, and a slim shelf can keep the desk tidy throughout the day. With fewer distractions, studying and working from home become easier to sustain.</p>",
      },
      fr: {
        title: "Un coin bureau simple mais efficace",
        caption: "Bureau, chaise et lumière sont les trois premiers investissements.",
        post_category: "Travail",
        content:
          "<p>Un bon coin bureau à la maison n'a pas besoin de beaucoup d'objets. Commencez par une hauteur de bureau adaptée, une chaise qui soutient le dos et une lumière qui ne projette pas d'ombre gênante.</p><p>Quelques boîtes, un range-câbles et une étagère fine suffisent à garder le plateau net. Moins de distractions aide à garder un bon rythme de travail.</p>",
      },
      zh: {
        title: "少而有效的居家工作角",
        caption: "桌子、椅子和灯光，是最值得先投入的三件事。",
        post_category: "工作",
        content:
          "<p>一个好的居家工作角不需要很多物品。先确认合适的桌高、能支撑腰背的椅子，以及不会在桌面形成阴影的光源。</p><p>小收纳盒、理线槽和薄层架可以让桌面一整天保持清爽。干扰减少后，在家学习和工作会更容易进入节奏。</p>",
      },
    },
  },
  {
    slug: "bao-quan-sofa-vai-trong-mua-mua",
    thumbnail_url: "/assets/images/community4.jpg",
    translations: {
      vi: {
        title: "Bảo quản sofa vải trong mùa mưa",
        caption: "Giảm ẩm mốc và mùi khó chịu bằng vài thói quen đơn giản.",
        post_category: "Bảo quản",
        content:
          "<p>Sofa vải dễ giữ được vẻ mới nếu được hút bụi định kỳ và đặt cách tường ẩm một khoảng nhỏ. Sau những ngày mưa kéo dài, bạn nên mở cửa thông gió hoặc dùng máy hút ẩm trong thời gian ngắn.</p><p>Vết đổ nên được xử lý sớm bằng khăn sạch thấm nhẹ, tránh chà mạnh làm chất bẩn đi sâu vào sợi vải. Với vết bẩn khó, hãy kiểm tra hướng dẫn vệ sinh của nhà sản xuất trước khi dùng dung dịch tẩy.</p>",
      },
      en: {
        title: "Caring for fabric sofas during the rainy season",
        caption: "Reduce moisture and odors with a few simple habits.",
        post_category: "Care",
        content:
          "<p>Fabric sofas stay fresh longer with regular vacuuming and a small gap from damp walls. After several rainy days, ventilate the room or use a dehumidifier for a short period.</p><p>Spills should be blotted early with a clean cloth instead of rubbed, which can push stains deeper into the fibers. For tougher marks, check the manufacturer's cleaning guidance before using any solution.</p>",
      },
      fr: {
        title: "Entretenir un canapé en tissu pendant la saison des pluies",
        caption: "Limiter l'humidité et les odeurs grâce à quelques habitudes simples.",
        post_category: "Entretien",
        content:
          "<p>Un canapé en tissu reste frais plus longtemps avec un aspirateur régulier et un léger écart avec les murs humides. Après plusieurs jours de pluie, aérez la pièce ou utilisez brièvement un déshumidificateur.</p><p>En cas de tache, tamponnez vite avec un chiffon propre au lieu de frotter. Pour les marques difficiles, consultez les conseils du fabricant avant d'utiliser un produit.</p>",
      },
      zh: {
        title: "雨季布艺沙发保养方法",
        caption: "用几个简单习惯减少潮湿和异味。",
        post_category: "保养",
        content:
          "<p>布艺沙发如果定期吸尘，并与潮湿墙面保持一点距离，就更容易保持清爽。连续下雨后，可以开窗通风或短时间使用除湿机。</p><p>液体泼洒后应尽快用干净毛巾轻轻吸干，避免用力摩擦让污渍进入纤维。顽固污渍建议先查看厂家清洁说明。</p>",
      },
    },
  },
  {
    slug: "vi-sao-nen-chon-noi-that-co-bao-hanh-ro-rang",
    thumbnail_url: "/assets/images/BIFMACertification.jpeg",
    translations: {
      vi: {
        title: "Vì sao nên chọn nội thất có bảo hành rõ ràng",
        caption: "Bảo hành minh bạch giúp bạn an tâm sau khi hoàn thiện nhà.",
        post_category: "Chính sách",
        content:
          "<p>Nội thất là khoản đầu tư sử dụng nhiều năm, vì vậy bảo hành rõ ràng giúp bạn hiểu sản phẩm được hỗ trợ trong trường hợp nào. Thời hạn, phạm vi và điều kiện loại trừ nên được đọc trước khi đặt mua.</p><p>Một chính sách tốt cũng cần hướng dẫn cách liên hệ, thời gian xử lý và phương án bảo trì sau bảo hành. Khi thông tin minh bạch, khách hàng dễ chủ động hơn nếu phát sinh sự cố.</p>",
      },
      en: {
        title: "Why clear furniture warranties matter",
        caption: "Transparent warranty terms help you feel secure after furnishing a home.",
        post_category: "Policy",
        content:
          "<p>Furniture is a long-term investment, so a clear warranty helps you understand when support applies. Duration, coverage, and exclusions should be reviewed before purchase.</p><p>A helpful policy also explains how to contact support, expected handling time, and maintenance options after warranty. Clear information makes it easier for customers to act when an issue appears.</p>",
      },
      fr: {
        title: "Pourquoi choisir des meubles avec une garantie claire",
        caption: "Une garantie transparente rassure après l'aménagement de la maison.",
        post_category: "Politique",
        content:
          "<p>Le mobilier est un investissement de long terme. Une garantie claire permet de savoir dans quels cas le produit est pris en charge. Durée, couverture et exclusions doivent être consultées avant l'achat.</p><p>Une bonne politique explique aussi le contact du service client, les délais de traitement et les options d'entretien après garantie. La transparence facilite les démarches en cas de problème.</p>",
      },
      zh: {
        title: "为什么家具保修政策要清楚",
        caption: "透明的保修说明，让装修完成后更安心。",
        post_category: "政策",
        content:
          "<p>家具通常会使用多年，因此清楚的保修政策能帮助你了解哪些情况可以获得支持。购买前应查看保修期限、范围和不适用条件。</p><p>好的政策还会说明联系方式、处理时间以及保修后的维护方案。信息越透明，遇到问题时客户就越容易主动处理。</p>",
      },
    },
  },
  {
    slug: "thiet-ke-nha-dau-tien-mua-gi-truoc-mua-gi-sau",
    thumbnail_url: "/assets/images/banner6.jpg",
    translations: {
      vi: {
        title: "Thiết kế nhà đầu tiên: mua gì trước, mua gì sau",
        caption: "Thứ tự ưu tiên giúp ngân sách nội thất dễ kiểm soát hơn.",
        post_category: "Mua sắm",
        content:
          "<p>Khi làm nhà đầu tiên, bạn nên ưu tiên các món ảnh hưởng trực tiếp đến sinh hoạt: giường, nệm, bàn ăn, sofa và hệ lưu trữ chính. Những món này quyết định nhịp sống hằng ngày và nên được đo đạc kỹ.</p><p>Đồ trang trí, đèn phụ và phụ kiện nên mua sau khi bạn đã sống thử vài tuần. Lúc đó nhu cầu thật sẽ rõ hơn, ngân sách cũng ít bị dàn trải vào các món chỉ đẹp lúc nhìn ban đầu.</p>",
      },
      en: {
        title: "First home setup: what to buy first",
        caption: "A priority order that keeps furniture spending easier to control.",
        post_category: "Shopping",
        content:
          "<p>For a first home, prioritize pieces that directly affect daily routines: bed, mattress, dining table, sofa, and core storage. These items shape how you live and should be measured carefully.</p><p>Decor, secondary lighting, and accessories are better purchased after a few weeks of living in the space. Real needs become clearer, and the budget is less likely to be spent on things that only look good at first glance.</p>",
      },
      fr: {
        title: "Premier logement : quoi acheter en premier",
        caption: "Un ordre de priorité pour mieux maîtriser le budget mobilier.",
        post_category: "Achat",
        content:
          "<p>Pour un premier logement, commencez par les meubles qui influencent directement le quotidien : lit, matelas, table à manger, canapé et rangements principaux. Ils structurent la vie dans la maison et doivent être bien mesurés.</p><p>La décoration, les lampes secondaires et les accessoires peuvent attendre quelques semaines. Les besoins réels deviennent alors plus clairs et le budget reste mieux contrôlé.</p>",
      },
      zh: {
        title: "第一次布置新家：先买什么",
        caption: "按优先级采购，更容易控制家具预算。",
        post_category: "购物",
        content:
          "<p>第一次布置新家时，建议先购买直接影响日常生活的家具：床、床垫、餐桌、沙发和主要收纳系统。这些决定生活节奏，需要认真测量。</p><p>装饰品、辅助灯具和配件可以在入住几周后再买。真正的需求会更清楚，预算也不容易花在只是一开始好看的物品上。</p>",
      },
    },
  },
];

const LEGACY_SEED_SLUGS = [
  "can-bang-mau-sac-noi-that-theo-anh-sang-tu-nhien",
  "goc-lam-viec-tai-nha-it-mon-nhung-hieu-qua",
];

function asString(value) {
  return String(value || "").trim();
}

function normalizeLang(value) {
  const raw = asString(value || DEFAULT_LANG).toLowerCase();
  if (raw.startsWith("zh")) return "zh";
  if (raw.startsWith("fr")) return "fr";
  if (raw.startsWith("en")) return "en";
  if (raw.startsWith("vi")) return "vi";
  return DEFAULT_LANG;
}

function firstTranslation(translations = {}, preferredLang = DEFAULT_LANG) {
  const ordered = [
    normalizeLang(preferredLang),
    DEFAULT_LANG,
    ...SUPPORTED_LANGS.filter((lang) => lang !== preferredLang && lang !== DEFAULT_LANG),
  ];

  for (const lang of ordered) {
    const translation = translations?.[lang];
    if (translation && asString(translation.title)) {
      return translation;
    }
  }

  return {};
}

function normalizeTranslations(source = {}, body = {}) {
  const translations = {};
  for (const lang of SUPPORTED_LANGS) {
    const raw = source?.[lang] || {};
    const translation = {
      title: asString(raw.title || body[`title_${lang}`]),
      caption: asString(raw.caption || raw.excerpt || body[`caption_${lang}`]),
      content: asString(raw.content || body[`content_${lang}`]),
      post_category: asString(raw.post_category || raw.category || body[`post_category_${lang}`] || body[`category_${lang}`]),
    };

    if (translation.title || translation.caption || translation.content || translation.post_category) {
      translations[lang] = translation;
    }
  }

  if (!translations.vi && (body.title || body.caption || body.content || body.post_category || body.category)) {
    translations.vi = {
      title: asString(body.title),
      caption: asString(body.caption || body.excerpt),
      content: asString(body.content),
      post_category: asString(body.post_category || body.category),
    };
  }

  return translations;
}

function normalizePostPayload(body = {}) {
  const translations = normalizeTranslations(body.translations, body);
  const primary = firstTranslation(translations, DEFAULT_LANG);
  const legacyTitle = asString(body.title);
  const title = asString(primary.title || legacyTitle);
  const status = ["draft", "published"].includes(asString(body.status).toLowerCase())
    ? asString(body.status).toLowerCase()
    : "draft";
  const requestedSlug = asString(body.slug);

  return {
    title,
    slug: requestedSlug || (title ? Post.buildSlug(title) : ""),
    caption: asString(primary.caption || body.caption || body.excerpt),
    content: asString(primary.content || body.content),
    thumbnail_url: asString(body.thumbnail_url || body.thumbnailUrl),
    post_category: asString(primary.post_category || body.post_category || body.category || "Blog") || "Blog",
    status,
    published_at: body.published_at || (status === "published" ? new Date() : null),
    translations,
  };
}

function localizePost(post, requestedLang = DEFAULT_LANG) {
  if (!post) return post;
  const lang = normalizeLang(requestedLang);
  const selected = firstTranslation(post.translations || {}, lang);
  return {
    ...post,
    title: asString(selected.title || post.title),
    caption: asString(selected.caption || post.caption),
    content: selected.content || post.content || "",
    post_category: asString(selected.post_category || post.post_category || "Blog"),
  };
}

async function ensureDefaultPosts() {
  await Post.deleteMany({
    slug: { $in: LEGACY_SEED_SLUGS },
    "translations.vi.title": { $exists: false },
  });

  const defaultSlugs = DEFAULT_POSTS.map((post) => post.slug);
  const translatedDefaults = await Post.countDocuments({
    slug: { $in: defaultSlugs },
    "translations.vi.title": { $exists: true, $ne: "" },
  });

  if (translatedDefaults === DEFAULT_POSTS.length) return;

  const now = new Date();
  await Post.bulkWrite(
    DEFAULT_POSTS.map((post, index) => {
      const primary = firstTranslation(post.translations, DEFAULT_LANG);
      const publishedAt = new Date(now.getTime() - index * DAY);

      return {
        updateOne: {
          filter: { slug: post.slug },
          update: {
            $set: {
              title: primary.title,
              caption: primary.caption,
              content: primary.content,
              thumbnail_url: post.thumbnail_url,
              post_category: primary.post_category || "Blog",
              translations: post.translations,
              status: "published",
              published_at: publishedAt,
              is_seed: true,
            },
            $setOnInsert: {
              slug: post.slug,
            },
          },
          upsert: true,
        },
      };
    }),
    { ordered: false }
  ).catch((error) => {
    if (error?.code !== 11000) throw error;
  });
}

function buildFilter(query = {}) {
  const filter = {};
  const status = asString(query.status).toLowerCase();
  if (["draft", "published"].includes(status)) {
    filter.status = status;
  }

  const slug = asString(query.slug);
  if (slug) {
    filter.slug = slug;
  }

  const q = asString(query.q || query.search);
  if (q) {
    const regex = { $regex: q, $options: "i" };
    filter.$or = [
      { title: regex },
      { caption: regex },
      { post_category: regex },
      { "translations.vi.title": regex },
      { "translations.vi.caption": regex },
      { "translations.en.title": regex },
      { "translations.en.caption": regex },
      { "translations.fr.title": regex },
      { "translations.fr.caption": regex },
      { "translations.zh.title": regex },
      { "translations.zh.caption": regex },
    ];
  }

  return filter;
}

function buildSort(sortQuery) {
  const raw = asString(sortQuery || "-published_at");
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
    const lang = normalizeLang(req.query.lang);

    const [items, total] = await Promise.all([
      Post.find(filter).sort(sort).skip(skip).limit(limit).lean(),
      Post.countDocuments(filter),
    ]);

    res.json({
      page,
      limit,
      total,
      totalPages: Math.ceil(total / limit) || 1,
      items: items.map((post) => localizePost(post, lang)),
    });
  } catch (err) {
    next(err);
  }
}

async function getPostById(req, res, next) {
  try {
    await ensureDefaultPosts();

    const identifier = asString(req.params.id);
    if (!identifier) {
      return res.status(400).json({ message: "Invalid id" });
    }

    const filter = mongoose.Types.ObjectId.isValid(identifier)
      ? { _id: identifier }
      : { slug: identifier };
    const doc = await Post.findOne(filter).lean();
    if (!doc) return res.status(404).json({ message: "Not found" });

    res.json(localizePost(doc, req.query.lang));
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
    res.status(201).json(localizePost(doc.toObject(), req.query.lang));
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
    }).lean();
    if (!doc) return res.status(404).json({ message: "Not found" });
    res.json(localizePost(doc, req.query.lang));
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
  getPostById,
  createPost,
  updatePost,
  deletePost,
};
