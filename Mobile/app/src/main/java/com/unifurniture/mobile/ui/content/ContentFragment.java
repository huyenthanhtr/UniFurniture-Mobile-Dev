package com.unifurniture.mobile.ui.content;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.unifurniture.mobile.BuildConfig;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.ApiListResponse;
import com.unifurniture.mobile.data.model.PostDto;
import com.unifurniture.mobile.data.remote.ApiClient;
import com.unifurniture.mobile.data.remote.ApiService;
import com.unifurniture.mobile.databinding.FragmentContentBinding;
import com.unifurniture.mobile.util.LanguageHelper;
import com.unifurniture.mobile.util.ScrollStateHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                    handleBlogClick(url.replace("blog://", ""));
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
            return;
        }

        String localHtml = buildLocalContent(title, url);
        if (!localHtml.isEmpty()) {
            binding.webView.loadDataWithBaseURL(getWebBaseUrl() + "/", localHtml, "text/html", "UTF-8", null);
        } else if (url != null && !url.isEmpty()) {
            binding.webView.loadUrl(url);
        } else {
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    private void fetchBlogsAndRender() {
        if (binding == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);

        ApiService apiService = ApiClient.getInstance();
        apiService.getPosts("published", 8, getCurrentLanguage()).enqueue(new Callback<ApiListResponse<PostDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiListResponse<PostDto>> call,
                                   @NonNull Response<ApiListResponse<PostDto>> response) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);

                List<PostDto> posts = null;
                if (response.isSuccessful() && response.body() != null) {
                    posts = response.body().getData();
                }

                binding.webView.loadDataWithBaseURL(
                        getWebBaseUrl() + "/",
                        buildAboutPageHtml(posts),
                        "text/html",
                        "UTF-8",
                        null
                );
            }

            @Override
            public void onFailure(@NonNull Call<ApiListResponse<PostDto>> call, @NonNull Throwable t) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                binding.webView.loadDataWithBaseURL(
                        getWebBaseUrl() + "/",
                        buildAboutPageHtml(null),
                        "text/html",
                        "UTF-8",
                        null
                );
            }
        });
    }

    private String buildAboutPageHtml(List<PostDto> posts) {
        Map<String, String> categoryKeys = new LinkedHashMap<>();
        StringBuilder blogSection = new StringBuilder();
        blogSection.append("<div class='section-title'>Blogs</div>");
        blogSection.append("<div class='filter-row blog-filter-row'>");
        blogSection.append("<button class='filter-chip active' type='button' onclick=\"filterBlogs('all',this)\">")
                .append(html(getString(R.string.filter_all)))
                .append("</button>");

        if (posts != null && !posts.isEmpty()) {
            for (PostDto post : posts) {
                if (post == null || isBlank(post.getId())) continue;
                String category = isBlank(post.getCategory()) ? "Blog" : post.getCategory().trim();
                if (!categoryKeys.containsKey(category)) {
                    String key = "cat-" + categoryKeys.size();
                    categoryKeys.put(category, key);
                    blogSection.append("<button class='filter-chip' type='button' onclick=\"filterBlogs('")
                            .append(attr(key))
                            .append("',this)\">")
                            .append(html(category))
                            .append("</button>");
                }
            }
        }
        blogSection.append("</div>");
        blogSection.append("<div class='blog-carousel'>");

        if (posts != null && !posts.isEmpty()) {
            for (PostDto post : posts) {
                if (post == null || isBlank(post.getId())) continue;

                String thumb = resolveMediaUrl(post.getThumbnailUrl());
                String caption = isBlank(post.getCaption()) ? post.getCategory() : post.getCaption();
                String category = isBlank(post.getCategory()) ? "Blog" : post.getCategory().trim();
                String categoryKey = categoryKeys.containsKey(category) ? categoryKeys.get(category) : "cat-0";
                blogSection.append("<button class='blog-card' data-category='")
                        .append(attr(categoryKey))
                        .append("' onclick='location.href=\"blog://")
                        .append(attr(post.getId()))
                        .append("\"'>")
                        .append("<span class='blog-img' style='background-image:url(\"")
                        .append(attr(thumb))
                        .append("\")'></span>")
                        .append("<span class='blog-info'>")
                        .append("<span class='blog-title'>").append(html(post.getTitle())).append("</span>")
                        .append("<span class='blog-caption'>").append(html(caption)).append("</span>")
                        .append("</span>")
                        .append("</button>");
            }
        }

        if (blogSection.toString().endsWith("<div class='blog-carousel'>")) {
            blogSection.append("<div class='empty-blog'>").append(html(getString(R.string.blog_empty_message))).append("</div>");
        }
        blogSection.append("</div>");

        return "<!doctype html><html><head><meta name='viewport' content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no'/>"
                + "<style>"
                + "body{margin:0;background:#fff;color:#1A1A1A;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;line-height:1.5;}"
                + ".hero{min-height:250px;background:linear-gradient(rgba(27,67,50,.48),rgba(27,67,50,.62)),url('https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?auto=format&fit=crop&w=1000&q=80');background-size:cover;background-position:center;display:flex;flex-direction:column;justify-content:flex-end;padding:28px 20px;color:#fff;}"
                + ".hero h1{font-size:28px;line-height:1.15;margin:0 0 8px;font-weight:800;letter-spacing:0;}"
                + ".hero p{font-size:14px;margin:0;max-width:320px;opacity:.94;}"
                + ".intro{padding:22px 20px 4px;background:#fff;}"
                + ".intro p{font-size:15px;color:#46554D;margin:0;line-height:1.7;}"
                + ".value-row{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px;padding:18px 20px 6px;}"
                + ".blog-carousel::-webkit-scrollbar,.filter-row::-webkit-scrollbar{display:none;}"
                + ".value-chip{display:block;min-height:42px;border:1px solid #DDE9E2;border-left:4px solid #1B4332;background:#F7FBF8;color:#1B4332;border-radius:10px;padding:10px 12px;font-size:12px;font-weight:800;line-height:1.32;box-sizing:border-box;}"
                + ".value-chip:nth-child(2){border-left-color:#245FA5;background:#F4F7FC;color:#173D68;}"
                + ".value-chip:nth-child(3){border-left-color:#C96F2D;background:#FFF7EF;color:#7B3E14;}"
                + ".value-chip:nth-child(4){border-left-color:#6B4E9B;background:#F8F5FC;color:#4D3478;}"
                + ".section-title{font-size:18px;font-weight:800;margin:26px 20px 14px;color:#1B4332;display:flex;align-items:center;}"
                + ".section-title:after{content:'';height:1px;background:#E8EFEA;flex:1;margin-left:14px;}"
                + ".filter-row{display:flex;gap:8px;overflow-x:auto;padding:0 20px 14px;scrollbar-width:none;}"
                + ".filter-chip{flex:0 0 auto;border:1px solid #D6E3DD;background:#fff;color:#456257;border-radius:999px;padding:8px 11px;font-size:12px;font-weight:800;}"
                + ".filter-chip.active{background:#1B4332;color:#fff;border-color:#1B4332;box-shadow:0 6px 16px rgba(27,67,50,.20);}"
                + ".blog-carousel{display:flex;gap:14px;overflow-x:auto;padding:0 20px 30px;}"
                + ".blog-card{flex:0 0 238px;border:1px solid #EDF2EF;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 8px 22px rgba(27,67,50,.08);padding:0;text-align:left;}"
                + ".blog-card.is-hidden{display:none;}"
                + ".blog-img{display:block;width:100%;height:136px;background:#EDF2EF;background-size:cover;background-position:center;}"
                + ".blog-info{display:block;padding:12px;}"
                + ".blog-title{display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;font-size:14px;font-weight:800;color:#1B4332;line-height:1.35;}"
                + ".blog-caption{display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;margin-top:6px;font-size:12px;color:#6B746F;line-height:1.45;}"
                + ".empty-blog{min-width:260px;border:1px dashed #C9D7D0;border-radius:12px;padding:22px;color:#6B746F;background:#FAFCFB;font-size:13px;}"
                + "</style></head><body>"
                + "<section class='hero'><h1>" + html(getString(R.string.account_about_title)) + "</h1><p>" + html(getString(R.string.about_uni_subtitle)) + "</p></section>"
                + "<section class='intro'><p>" + html(getString(R.string.about_uni_intro)) + "</p></section>"
                + "<div class='value-row'>"
                + "<span class='value-chip'>" + html(getString(R.string.home_value_quality)) + "</span>"
                + "<span class='value-chip'>" + html(getString(R.string.home_value_elegant)) + "</span>"
                + "<span class='value-chip'>" + html(getString(R.string.home_value_dedicated)) + "</span>"
                + "<span class='value-chip'>" + html(getString(R.string.about_uni_value_sustainable)) + "</span>"
                + "</div>"
                + blogSection
                + "<script>"
                + "function each(s,f){var n=document.querySelectorAll(s);for(var i=0;i<n.length;i++){f(n[i],i);}}"
                + "function activate(btn){var p=btn&&btn.parentNode;if(!p)return;var c=p.querySelectorAll('button');for(var i=0;i<c.length;i++){c[i].classList.remove('active');}btn.classList.add('active');}"
                + "function filterBlogs(category,btn){activate(btn);each('.blog-card',function(card){var show=category==='all'||card.getAttribute('data-category')===category;card.classList.toggle('is-hidden',!show);});}"
                + "</script>"
                + "</body></html>";
    }

    private void handleBlogClick(String blogId) {
        if (isBlank(blogId) || binding == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);
        ApiClient.getInstance().getPostById(blogId, getCurrentLanguage()).enqueue(new Callback<PostDto>() {
            @Override
            public void onResponse(@NonNull Call<PostDto> call, @NonNull Response<PostDto> response) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    PostDto post = response.body();
                    Bundle bundle = new Bundle();
                    bundle.putString("title", post.getTitle());
                    bundle.putString("content", buildBlogDetailContent(post));
                    androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.contentFragment, bundle);
                }
            }

            @Override
            public void onFailure(@NonNull Call<PostDto> call, @NonNull Throwable t) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }

    private String buildBlogDetailContent(PostDto post) {
        StringBuilder html = new StringBuilder();
        String thumb = resolveMediaUrl(post.getThumbnailUrl());
        if (!isBlank(thumb)) {
            html.append("<img src='").append(attr(thumb)).append("'/>");
        }
        if (!isBlank(post.getCaption())) {
            html.append("<p class='lead'>").append(html(post.getCaption())).append("</p>");
        }
        String content = post.getContent();
        if (isBlank(content)) {
            content = post.getCaption();
        }
        html.append(renderRichText(content));
        return html.toString();
    }

    private void displayDirectContent(String content) {
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no'/>"
                + "<style>"
                + "body{margin:0;padding:24px 20px 40px;background:#fff;color:#1A1A1A;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;line-height:1.7;}"
                + "h1,h2,h3{color:#1B4332;line-height:1.3;}h3{font-size:22px;margin:0 0 16px;}"
                + "p{margin:0 0 16px;font-size:15px;color:#444;}.lead{font-size:16px;color:#56645E;font-weight:600;}"
                + "img{max-width:100%;height:auto;border-radius:12px;margin:0 0 18px;display:block;}"
                + "ul{padding-left:20px;}li{margin-bottom:8px;}"
                + "</style></head><body>"
                + content
                + "</body></html>";
        binding.webView.loadDataWithBaseURL(getWebBaseUrl() + "/", html, "text/html", "UTF-8", null);
    }

    private String buildLocalContent(String title, String url) {
        String normalized = (url == null ? "" : url).toLowerCase();
        if (!normalized.contains("chinh-sach") && !normalized.contains("policy")) {
            return "";
        }

        PolicyContent policy = resolvePolicy(normalized, title);
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width, initial-scale=1'/>"
                + "<style>"
                + "html{scroll-behavior:smooth;}"
                + "body{margin:0;padding:22px 18px 34px;background:#FAFAF8;color:#1A1A1A;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;line-height:1.6;}"
                + "h1{font-size:24px;line-height:1.25;margin:0 0 16px;color:#1B4332;}"
                + ".chips{display:flex;flex-wrap:wrap;gap:8px;margin:0 0 18px;}"
                + ".chip{display:inline-flex;align-items:center;background:#EEF6F2;color:#1B4332;border:1px solid #CFE1D8;border-radius:999px;padding:7px 10px;font-size:12px;font-weight:800;text-decoration:none;}"
                + ".chip:nth-child(2n){background:#EEF4FF;color:#16427D;border-color:#CADBFF;}"
                + ".chip:nth-child(3n){background:#FFF7E8;color:#8A4B05;border-color:#F5D6A1;}"
                + ".chip:nth-child(4n){background:#FDF0F5;color:#8B2553;border-color:#F3C6D8;}"
                + ".chip.active{background:#1B4332!important;color:#fff!important;border-color:#1B4332!important;box-shadow:0 6px 16px rgba(27,67,50,.22);}"
                + "section{background:#fff;border:1px solid #ECEFEA;border-radius:12px;padding:16px;margin:0 0 12px;box-shadow:0 4px 16px rgba(27,67,50,.04);scroll-margin-top:14px;}"
                + "section.is-focused{border-color:#1B4332;box-shadow:0 8px 22px rgba(27,67,50,.14);}"
                + "section.is-hidden{display:none;}"
                + "p{font-size:14px;margin:0;color:#424242;}strong{color:#1B4332;}"
                + "</style></head><body><h1>"
                + html(policy.title)
                + "</h1>"
                + renderHighlights(policy.highlights, policy.paragraphs.length)
                + renderParagraphs(policy.paragraphs)
                + "<script>"
                + "function each(s,f){var n=document.querySelectorAll(s);for(var i=0;i<n.length;i++){f(n[i],i);}}"
                + "function setPolicyFilter(index,btn){each('.chip',function(chip){chip.classList.remove('active');});if(btn){btn.classList.add('active');}each('.policy-section',function(section,i){var show=index<0||i===index;section.classList.toggle('is-hidden',!show);section.classList.toggle('is-focused',index===i);});if(index>=0){var target=document.getElementById('policy-section-'+index);if(target){target.scrollIntoView({behavior:'smooth',block:'start'});}}}"
                + "</script>"
                + "</body></html>";
    }

    private PolicyContent resolvePolicy(String normalizedUrl, String fallbackTitle) {
        if (normalizedUrl.contains("giao-hang-lap-dat")) {
            return policyFromResources(
                    getString(R.string.policy_shipping),
                    R.array.policy_shipping_paragraphs,
                    R.array.policy_shipping_highlights
            );
        }
        if (normalizedUrl.contains("bao-hanh-bao-tri")) {
            return policyFromResources(
                    getString(R.string.policy_warranty),
                    R.array.policy_warranty_paragraphs,
                    R.array.policy_warranty_highlights
            );
        }
        if (normalizedUrl.contains("doi-tra")) {
            return policyFromResources(
                    getString(R.string.policy_return),
                    R.array.policy_return_paragraphs,
                    R.array.policy_return_highlights
            );
        }
        if (normalizedUrl.contains("khach-hang-than-thiet")) {
            return policyFromResources(
                    getString(R.string.policy_loyalty),
                    R.array.policy_loyalty_paragraphs,
                    R.array.policy_loyalty_highlights
            );
        }
        if (normalizedUrl.contains("doi-tac-ban-hang")) {
            return policyFromResources(
                    getString(R.string.policy_partner),
                    R.array.policy_partner_paragraphs,
                    R.array.policy_partner_highlights
            );
        }
        return policyFromResources(
                !isBlank(fallbackTitle) ? fallbackTitle : getString(R.string.policy_sales),
                R.array.policy_sales_paragraphs,
                R.array.policy_sales_highlights
        );
    }

    private PolicyContent policyFromResources(String title, int paragraphsRes, int highlightsRes) {
        return new PolicyContent(
                title,
                getResources().getStringArray(paragraphsRes),
                getResources().getStringArray(highlightsRes)
        );
    }

    private String renderHighlights(String[] highlights, int sectionCount) {
        if (highlights == null || highlights.length == 0 || sectionCount == 0) return "";
        StringBuilder builder = new StringBuilder("<div class='chips'>");
        builder.append("<button class='chip active' type='button' onclick='setPolicyFilter(-1,this)'>")
                .append(html(getString(R.string.filter_all)))
                .append("</button>");
        for (int i = 0; i < highlights.length; i++) {
            int targetIndex = Math.min(i, sectionCount - 1);
            builder.append("<button class='chip' type='button' onclick='setPolicyFilter(")
                    .append(targetIndex)
                    .append(",this)'>")
                    .append(html(highlights[i]))
                    .append("</button>");
        }
        builder.append("</div>");
        return builder.toString();
    }

    private String renderParagraphs(String[] paragraphs) {
        StringBuilder builder = new StringBuilder();
        if (paragraphs == null) return "";
        for (int i = 0; i < paragraphs.length; i++) {
            builder.append("<section class='policy-section' id='policy-section-")
                    .append(i)
                    .append("'><p>")
                    .append(html(paragraphs[i]))
                    .append("</p></section>");
        }
        return builder.toString();
    }

    private String renderRichText(String content) {
        if (content == null) return "";
        String value = content.trim();
        if (value.contains("<p") || value.contains("<ul") || value.contains("<ol") || value.contains("<br")) {
            return value;
        }
        if (value.isEmpty()) return "";
        return "<p>" + html(value).replace("\n\n", "</p><p>").replace("\n", "<br/>") + "</p>";
    }

    private String getCurrentLanguage() {
        return LanguageHelper.getLanguage(requireContext());
    }

    private String getWebBaseUrl() {
        String api = BuildConfig.API_BASE_URL == null ? "" : BuildConfig.API_BASE_URL.trim();
        if (api.endsWith("/api/")) {
            api = api.substring(0, api.length() - 5);
        } else if (api.endsWith("/api")) {
            api = api.substring(0, api.length() - 4);
        }
        while (api.endsWith("/")) {
            api = api.substring(0, api.length() - 1);
        }
        return api;
    }

    private String resolveMediaUrl(String url) {
        if (isBlank(url)) return "";
        String value = url.trim();
        if (value.startsWith("http://") || value.startsWith("https://")) return value;
        if (value.startsWith("//")) return "https:" + value;
        if (value.startsWith("/")) return getWebBaseUrl() + value;
        return getWebBaseUrl() + "/" + value;
    }

    private String html(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String attr(String value) {
        return html(value).replace("'", "&#39;");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class PolicyContent {
        final String title;
        final String[] paragraphs;
        final String[] highlights;

        PolicyContent(String title, String[] paragraphs, String[] highlights) {
            this.title = title;
            this.paragraphs = paragraphs;
            this.highlights = highlights;
        }
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
