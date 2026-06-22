package com.unifurniture.mobile.ui.content;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.ApiListResponse;
import com.unifurniture.mobile.data.model.PostDto;
import com.unifurniture.mobile.data.remote.ApiClient;
import com.unifurniture.mobile.data.remote.ApiService;
import com.unifurniture.mobile.databinding.FragmentContentBinding;
import com.unifurniture.mobile.util.ScrollStateHelper;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ContentFragment extends Fragment {

    private FragmentContentBinding binding;
    private final ScrollStateHelper scrollState = new ScrollStateHelper("content");
    private boolean contentLoaded;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentContentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        scrollState.read(savedInstanceState);

        String title = getArguments() != null ? getArguments().getString("title", "") : "";
        String url = getArguments() != null ? getArguments().getString("url", "") : "";
        String directContent = getArguments() != null ? getArguments().getString("content", "") : "";

        binding.toolbar.setTitle(title);
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        binding.webView.getSettings().setJavaScriptEnabled(true);
        binding.webView.getSettings().setDomStorageEnabled(true);
        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (binding != null) {
                    binding.progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && url.startsWith("blog://")) {
                    String blogId = url.replace("blog://", "");
                    handleBlogClick(blogId);
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }
        });

        if (savedInstanceState != null) {
            binding.webView.restoreState(savedInstanceState);
            contentLoaded = true;
            binding.progressBar.setVisibility(View.GONE);
        } else if (!directContent.isEmpty()) {
            displayDirectContent(directContent);
        } else {
            loadContent(title, url);
        }

        scrollState.restore(binding.webView);
    }

    private void loadContent(String title, String url) {
        if (contentLoaded || binding == null) return;
        contentLoaded = true;

        String normalized = (url == null ? "" : url).toLowerCase();
        if (normalized.contains("ve-unifurniture")) {
            fetchBlogsAndRender();
        } else {
            String localHtml = buildLocalContent(title, url);
            if (!localHtml.isEmpty()) {
                binding.webView.loadDataWithBaseURL(null, localHtml, "text/html", "UTF-8", null);
            } else if (!url.isEmpty()) {
                binding.webView.loadUrl(url);
            } else {
                binding.progressBar.setVisibility(View.GONE);
            }
        }
    }

    private void fetchBlogsAndRender() {
        if (binding == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);

        ApiService apiService = ApiClient.getInstance();
        apiService.getPosts("published", 8).enqueue(new Callback<ApiListResponse<PostDto>>() {
            @Override
            public void onResponse(Call<ApiListResponse<PostDto>> call, Response<ApiListResponse<PostDto>> response) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                
                List<PostDto> posts = null;
                if (response.isSuccessful() && response.body() != null) {
                    posts = response.body().getData();
                }
                
                String html = buildAboutPageHtml(posts);
                binding.webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            }

            @Override
            public void onFailure(Call<ApiListResponse<PostDto>> call, Throwable t) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                String html = buildAboutPageHtml(null);
                binding.webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            }
        });
    }

    private String buildAboutPageHtml(List<PostDto> posts) {
        String lang = com.unifurniture.mobile.util.LanguageHelper.getLanguage(requireContext());
        boolean isVi = "vi".equalsIgnoreCase(lang);

        String aboutTitle = isVi ? "Về UniFurniture" : "About UniFurniture";
        String aboutSub = isVi ? "Kiến tạo không gian sống hiện đại" : "Crafting modern living spaces";

        StringBuilder blogSection = new StringBuilder();
        blogSection.append("<div class='section-title'>Blogs & News</div>");
        blogSection.append("<div class='blog-carousel'>");

        if (posts != null && !posts.isEmpty()) {
            for (PostDto post : posts) {
                String thumb = post.getThumbnailUrl();
                if (thumb == null || thumb.isEmpty()) thumb = "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=400&q=80";
                
                String postId = post.getId() != null ? post.getId() : "";
                blogSection.append("<div class='blog-card' onclick='location.href=\"blog://").append(postId).append("\"'>")
                        .append("<div class='blog-img' style='background-image: url(\"").append(thumb).append("\")'></div>")
                        .append("<div class='blog-info'>")
                        .append("<h3>").append(post.getTitle()).append("</h3>")
                        .append("<p>").append(post.getCaption() != null ? post.getCaption() : "").append("</p>")
                        .append("</div>")
                        .append("</div>");
            }
        } else {
            String[] sampleTitles = {
                "Xu hướng nội thất tối giản 2024", 
                "Cách chọn sofa cho phòng khách nhỏ", 
                "5 loại gỗ bền bỉ nhất hiện nay",
                "Mẹo bảo quản đồ gỗ luôn mới",
                "Phối màu phòng ngủ chuẩn chuyên gia",
                "Tại sao nên dùng tủ bếp thông minh?",
                "Không gian làm việc tại gia lý tưởng",
                "UniFurniture: Hành trình kiến tạo"
            };
            String[] sampleImages = {
                "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1583847268964-b28dc8f51f92?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1524758631624-e2822e304c36?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1505691938895-1758d7eaa511?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1540518614846-7eded433c457?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1556911220-e15b29be8c8f?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1593062096033-9a26b09da705?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1538688525198-9b88f6f53126?auto=format&fit=crop&w=400&q=80"
            };

            for(int i=0; i<8; i++) {
                blogSection.append("<div class='blog-card' onclick='location.href=\"blog://sample_").append(i).append("\"'>")
                    .append("<div class='blog-img' style='background-image: url(\"").append(sampleImages[i]).append("\")'></div>")
                    .append("<div class='blog-info'>")
                    .append("<h3>").append(sampleTitles[i]).append("</h3>")
                    .append("<p>Khám phá giải pháp tối ưu cho không gian sống hiện đại cùng UniFurniture...</p>")
                    .append("</div>")
                    .append("</div>");
            }
        }
        blogSection.append("</div>");

        return "<!doctype html><html><head><meta name='viewport' content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no'/>"
                + "<style>"
                + "body{margin:0;padding:0;background:#fff;color:#1A1A1A;font-family:-apple-system,system-ui,sans-serif;line-height:1.5;}"
                + ".hero{background:linear-gradient(rgba(0,0,0,0.4),rgba(0,0,0,0.4)), url('https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?auto=format&fit=crop&w=800&q=80');"
                + "background-size:cover;background-position:center;padding:60px 20px;text-align:center;color:#fff;}"
                + ".hero h1{font-size:26px;margin:0;font-weight:800;text-transform:uppercase;letter-spacing:1px;}"
                + ".hero p{font-size:14px;margin:10px 0 0;opacity:0.9;}"
                + ".intro{padding:24px 20px;background:#F9F9F9;text-align:center;}"
                + ".intro p{font-size:15px;color:#444;margin:0;font-style:italic;line-height:1.7;}"
                + ".section-title{font-size:18px;font-weight:700;margin:30px 20px 15px;color:#1B4332;display:flex;align-items:center;}"
                + ".section-title::after{content:'';flex:1;height:1px;background:#EEE;margin-left:15px;}"
                + ".blog-carousel{display:flex;overflow-x:auto;padding:0 20px 30px;gap:15px;scrollbar-width:none;}"
                + ".blog-carousel::-webkit-scrollbar{display:none;}"
                + ".blog-card{flex:0 0 240px;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 15px rgba(0,0,0,0.06);}"
                + ".blog-img{width:100%;height:140px;background-size:cover;background-position:center;}"
                + ".blog-info{padding:12px;}"
                + ".blog-info h3{font-size:14px;margin:0 0 6px;color:#1B4332;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;}"
                + ".blog-info p{font-size:12px;color:#777;margin:0;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;}"
                + ".values{display:grid;grid-template-columns:1fr 1fr;gap:15px;padding:0 20px 20px;}"
                + ".val-item{background:#FFF;padding:15px;border-radius:10px;border:1px solid #F0F0F0;text-align:center;}"
                + ".val-icon{font-size:24px;margin-bottom:8px;display:block;}"
                + ".val-item span{font-size:12px;font-weight:600;color:#1B4332;}"
                + "</style></head><body>"
                + "<div class='hero'><h1>" + aboutTitle + "</h1><p>" + aboutSub + "</p></div>"
                + "<div class='intro'><p>\"" + (isVi ? "Kiến tạo không gian sống bền vững, mang lại sự ấm áp và phong cách cho ngôi nhà của bạn." : "Creating sustainable living spaces, bringing warmth and style to your home.") + "\"</p></div>"
                + blogSection.toString()
                + "<div class='section-title'>" + (isVi ? "Giá trị cốt lõi" : "Core Values") + "</div>"
                + "<div class='values'>"
                + "<div class='val-item'><i class='val-icon'>🛋️</i><span>" + getString(R.string.home_value_quality) + "</span></div>"
                + "<div class='val-item'><i class='val-icon'>📐</i><span>" + getString(R.string.home_value_elegant) + "</span></div>"
                + "<div class='val-item'><i class='val-icon'>🌟</i><span>" + getString(R.string.home_value_dedicated) + "</span></div>"
                + "<div class='val-item'><i class='val-icon'>🛡️</i><span>" + (isVi ? "Bền vững" : "Sustainable") + "</span></div>"
                + "</div>"
                + "</body></html>";
    }

    private void handleBlogClick(String blogId) {
        if (blogId.startsWith("sample_")) {
            int index = Integer.parseInt(blogId.replace("sample_", ""));
            String[] titles = {
                "Xu hướng nội thất tối giản 2024", 
                "Cách chọn sofa cho phòng khách nhỏ", 
                "5 loại gỗ bền bỉ nhất hiện nay",
                "Mẹo bảo quản đồ gỗ luôn mới",
                "Phối màu phòng ngủ chuẩn chuyên gia",
                "Tại sao nên dùng tủ bếp thông minh?",
                "Không gian làm việc tại gia lý tưởng",
                "UniFurniture: Hành trình kiến tạo"
            };
            String sampleContent = "<h3>" + titles[index] + "</h3>" +
                "<p>Đây là nội dung chi tiết bài viết về <b>" + titles[index] + "</b>. " +
                "Trong thiết kế hiện đại, việc tối ưu hóa không gian và sử dụng vật liệu bền bỉ là yếu tố tiên quyết. " +
                "UniFurniture cam kết mang đến những giải pháp nội thất thông minh nhất cho gia đình bạn.</p>" +
                "<img src='https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=800&q=80' style='width:100%; border-radius:12px; margin:20px 0;'/>" +
                "<p>Nội thất không chỉ là đồ dùng, mà còn là tâm hồn của ngôi nhà. Hãy để chúng tôi giúp bạn kiến tạo không gian sống mơ ước.</p>";
            
            Bundle bundle = new Bundle();
            bundle.putString("title", titles[index]);
            bundle.putString("content", sampleContent);
            androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.contentFragment, bundle);
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        ApiClient.getInstance().getPostById(blogId).enqueue(new Callback<PostDto>() {
            @Override
            public void onResponse(Call<PostDto> call, Response<PostDto> response) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    PostDto post = response.body();
                    Bundle bundle = new Bundle();
                    bundle.putString("title", post.getTitle());
                    bundle.putString("content", post.getContent() != null ? post.getContent() : post.getCaption());
                    androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.contentFragment, bundle);
                }
            }

            @Override
            public void onFailure(Call<PostDto> call, Throwable t) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void displayDirectContent(String content) {
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no'/>"
                + "<style>"
                + "body{margin:0;padding:24px 20px 40px;background:#fff;color:#1A1A1A;font-family:-apple-system,system-ui,sans-serif;line-height:1.7;}"
                + "h3{font-size:22px;color:#1B4332;margin:0 0 16px;line-height:1.3;}"
                + "p{margin-bottom:16px;font-size:15px;color:#444;}"
                + "img{max-width:100%;height:auto;border-radius:12px;}"
                + "</style></head><body>"
                + content
                + "</body></html>";
        binding.webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private String buildLocalContent(String title, String url) {
        String normalized = (url == null ? "" : url).toLowerCase();
        String body;
        if (normalized.contains("chinh-sach") || normalized.contains("policy")) {
            String lang = com.unifurniture.mobile.util.LanguageHelper.getLanguage(requireContext());
            boolean isVi = "vi".equalsIgnoreCase(lang);
            
            body = section(getString(R.string.policy_sales),
                    isVi ? "• Đặt cọc 10% cho đơn hàng trên 10,000,000đ.<br/>" +
                           "• Thanh toán phần còn lại ngay khi nhận hàng.<br/>" +
                           "• Hủy đơn sau 24h xác nhận sẽ không được hoàn cọc.<br/>" +
                           "• Hỗ trợ xuất hóa đơn VAT nếu yêu cầu khi đặt hàng."
                         : "• 10% deposit for orders over 10,000,000 VND.<br/>" +
                           "• Pay the balance upon receipt.<br/>" +
                           "• Non-refundable deposit if canceled 24h after confirmation.<br/>" +
                           "• VAT invoice support upon request.")
                    + section(getString(R.string.policy_shipping),
                    isVi ? "• Miễn phí giao hàng & lắp đặt khu vực TP.HCM (trừ Cần Giờ) và lân cận.<br/>" +
                           "• Thời gian giao hàng dự kiến trong 3 ngày làm việc.<br/>" +
                           "• Hỗ trợ dời lịch giao tối đa 1 lần (báo trước 24h)."
                         : "• Free delivery & installation in HCM City and nearby areas.<br/>" +
                           "• Expected delivery within 3 working days.<br/>" +
                           "• One-time delivery rescheduling supported (24h notice).")
                    + section(getString(R.string.policy_warranty),
                    isVi ? "• Bảo hành 5 năm lỗi kỹ thuật/chất liệu.<br/>" +
                           "• Bảo trì trọn đời với chi phí hợp lý sau bảo hành.<br/>" +
                           "• Không bảo hành lỗi do người dùng hoặc hao mòn tự nhiên."
                         : "• 5-year warranty for technical/material defects.<br/>" +
                           "• Lifetime maintenance at reasonable cost after warranty.<br/>" +
                           "• No warranty for user errors or natural wear and tear.")
                    + section(getString(R.string.policy_return),
                    isVi ? "• Đổi hàng miễn phí trong 3 ngày nếu có lỗi kỹ thuật.<br/>" +
                           "• Trả hàng tại thời điểm giao nếu không đúng thông tin đặt hàng (phí 300,000đ)."
                         : "• Free exchange within 3 days for technical errors.<br/>" +
                           "• Return at delivery if info is incorrect (300,000 VND fee).")
                    + section(getString(R.string.policy_loyalty),
                    isVi ? "• Tích điểm: 100,000đ = 1 điểm.<br/>" +
                           "• Hạng thành viên: Bronze, Silver (giảm 3%), Gold (5%), Diamond (7%).<br/>" +
                           "• Điểm tích lũy có giá trị trong 365 ngày."
                         : "• Points: 100,000 VND = 1 point.<br/>" +
                           "• Membership: Bronze, Silver (3% off), Gold (5%), Diamond (7%).<br/>" +
                           "• Points expire after 365 days.")
                    + section(getString(R.string.policy_partner),
                    isVi ? "• Áp dụng cho cá nhân/doanh nghiệp thiết kế, thi công nội thất.<br/>" +
                           "• Hỗ trợ chính sách giá, truyền thông và sản phẩm trưng bày."
                         : "• Applicable to interior design/construction businesses.<br/>" +
                           "• Support for pricing, media, and display products.");
        } else {
            return "";
        }

        String pageTitle = title == null || title.trim().isEmpty() ? "UniFurniture" : title.trim();
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width, initial-scale=1'/>"
                + "<style>"
                + "body{margin:0;padding:24px 18px 32px;background:#FAFAF8;color:#1A1A1A;font-family:sans-serif;line-height:1.55;}"
                + "h1{font-size:24px;line-height:1.2;margin:0 0 18px;color:#1B4332;}"
                + "section{background:#fff;border:1px solid #eee;border-radius:12px;padding:16px;margin:0 0 14px;}"
                + "h2{font-size:16px;margin:0 0 8px;color:#1B4332;}p{font-size:14px;margin:0;color:#424242;}"
                + "</style></head><body><h1>" + pageTitle + "</h1>" + body + "</body></html>";
    }

    private String section(String heading, String text) {
        return "<section><h2>" + heading + "</h2><p>" + text + "</p></section>";
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) {
            binding.webView.saveState(outState);
            scrollState.save(outState, binding.webView);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        contentLoaded = false;
    }
}
