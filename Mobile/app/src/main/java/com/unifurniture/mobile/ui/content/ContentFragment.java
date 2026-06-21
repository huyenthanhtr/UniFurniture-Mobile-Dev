package com.unifurniture.mobile.ui.content;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.unifurniture.mobile.databinding.FragmentContentBinding;
import com.unifurniture.mobile.util.ScrollStateHelper;

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
        });

        if (savedInstanceState != null) {
            binding.webView.restoreState(savedInstanceState);
            contentLoaded = true;
            binding.progressBar.setVisibility(View.GONE);
        } else {
            loadContent(title, url);
        }

        scrollState.restore(binding.webView);
    }

    private void loadContent(String title, String url) {
        if (contentLoaded || binding == null) return;
        contentLoaded = true;

        String localHtml = buildLocalContent(title, url);
        if (!localHtml.isEmpty()) {
            binding.webView.loadDataWithBaseURL(null, localHtml, "text/html", "UTF-8", null);
        } else if (!url.isEmpty()) {
            binding.webView.loadUrl(url);
        } else {
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    private String buildLocalContent(String title, String url) {
        String normalized = (url == null ? "" : url).toLowerCase();
        String body;
        if (normalized.contains("ve-unifurniture")) {
            body = section("Ve UniFurniture",
                    "UniFurniture la khong gian noi that hien dai danh cho nhung can nha tre, can bang giua tinh tham my, do ben va trai nghiem mua sam de chiu.")
                    + section("Gia tri chung toi theo duoi",
                    "San pham ro nguon goc, tu van gan voi nhu cau that, va dich vu hau mai minh bach tu luc dat hang den khi hoan thien khong gian song.")
                    + section("Showroom va ho tro",
                    "Khach hang co the xem san pham, theo doi don hang, luu dia chi giao hang va lien he cham soc khach hang truc tiep trong he thong UniFurniture.");
        } else if (normalized.contains("chinh-sach") || normalized.contains("bao-mat")) {
            body = section("Chinh sach bao mat",
                    "UniFurniture chi su dung thong tin tai khoan, so dien thoai, email va dia chi giao hang de xu ly don hang, cham soc khach hang va cai thien dich vu.")
                    + section("Du lieu don hang",
                    "Thong tin giao nhan va thanh toan duoc luu de doi soat, tra cuu van don, xu ly bao hanh va ho tro khi khach hang can kiem tra lai lich su mua hang.")
                    + section("Cam ket",
                    "Ung dung khong yeu cau nhung quyen khong can thiet. Nguoi dung co the cap nhat ho so va quan ly so dia chi trong trang tai khoan.");
        } else if (normalized.contains("cong-dong")) {
            body = section("Cong dong UniFurniture",
                    "UniFurniture huong toi mot cong dong yeu noi that, chia se cach bo tri nha cua gon gang, am ap va thuc te hon moi ngay.")
                    + section("Hoat dong",
                    "Khach hang co the theo doi cac chuong trinh uu dai, danh gia san pham sau khi mua va dong gop y kien de cua hang cai thien dich vu.")
                    + section("Ket noi",
                    "Moi phan hoi tu khach hang deu giup nhom hoan thien san pham, giao dien mobile va trai nghiem mua sam tot hon.");
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
