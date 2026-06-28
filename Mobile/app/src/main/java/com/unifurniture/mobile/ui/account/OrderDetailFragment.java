package com.unifurniture.mobile.ui.account;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.unifurniture.mobile.BuildConfig;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.OrderDetailDto;
import com.unifurniture.mobile.data.model.OrderDetailResponse;
import com.unifurniture.mobile.data.model.OrderDto;
import com.unifurniture.mobile.data.model.OrderReviewItemDto;
import com.unifurniture.mobile.data.model.OrderReviewStatusDto;
import com.unifurniture.mobile.data.model.PaymentDto;
import com.unifurniture.mobile.data.model.PaymentSummaryDto;
import com.unifurniture.mobile.data.model.ReviewMediaConfigDto;
import com.unifurniture.mobile.data.model.ReviewMediaUploadResponseDto;
import com.unifurniture.mobile.data.model.ReviewSubmissionItemDto;
import com.unifurniture.mobile.data.model.ReviewSubmissionRequestDto;
import com.unifurniture.mobile.data.model.ReviewSubmissionResponseDto;
import com.unifurniture.mobile.data.remote.ApiClient;
import com.unifurniture.mobile.data.remote.ApiService;
import com.unifurniture.mobile.databinding.DialogSubmitReviewBinding;
import com.unifurniture.mobile.databinding.FragmentOrderDetailBinding;
import com.unifurniture.mobile.databinding.ItemOrderDetailBinding;
import com.unifurniture.mobile.ui.adapter.ReviewMediaPreviewAdapter;
import com.unifurniture.mobile.ui.common.ReviewImageViewerDialogFragment;
import com.unifurniture.mobile.ui.common.ReviewVideoViewerDialogFragment;
import com.unifurniture.mobile.util.CountingRequestBody;
import com.unifurniture.mobile.util.FormatUtil;
import com.unifurniture.mobile.util.OrderStatusUi;
import com.unifurniture.mobile.util.SessionManager;
import com.unifurniture.mobile.util.ToastUtil;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailFragment extends Fragment {

    private FragmentOrderDetailBinding binding;
    private ApiService apiService;
    private String orderId;
    private final Map<String, OrderReviewItemDto> reviewStatusMap = new HashMap<>();
    private List<OrderDetailDto> currentItems = new ArrayList<>();
    private OrderDto currentOrder;
    private ReviewMediaConfigDto reviewMediaConfig;

    private ActivityResultLauncher<String> reviewImagesPickerLauncher;
    private ActivityResultLauncher<String> reviewVideosPickerLauncher;
    private final List<Uri> pendingReviewImageUris = new ArrayList<>();
    private final List<Uri> pendingReviewVideoUris = new ArrayList<>();
    private DialogSubmitReviewBinding activeReviewDialogBinding;
    private AlertDialog activeReviewDialog;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrderDetailBinding.inflate(inflater, container, false);
        initMediaPickers();
        return binding.getRoot();
    }

    private void initMediaPickers() {
        reviewImagesPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    pendingReviewImageUris.clear();
                    if (uris != null) {
                        pendingReviewImageUris.addAll(limitUris(uris, true));
                    }
                    updateSelectedMediaLabels();
                }
        );

        reviewVideosPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    pendingReviewVideoUris.clear();
                    if (uris != null) {
                        pendingReviewVideoUris.addAll(limitUris(uris, false));
                    }
                    updateSelectedMediaLabels();
                }
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        apiService = ApiClient.getInstance();
        loadReviewMediaConfig();

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        if (getArguments() != null) {
            orderId = getArguments().getString("order_id");
        }

        if (orderId != null) {
            loadOrderDetails();
        } else {
            ToastUtil.error(requireContext(), R.string.error_unknown);
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        }
    }

    private void loadReviewMediaConfig() {
        apiService.getReviewMediaConfig().enqueue(new Callback<ReviewMediaConfigDto>() {
            @Override
            public void onResponse(@NonNull Call<ReviewMediaConfigDto> call, @NonNull Response<ReviewMediaConfigDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    reviewMediaConfig = response.body();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReviewMediaConfigDto> call, @NonNull Throwable t) {
                // fallback defaults stay in use
            }
        });
    }

    private List<Uri> limitUris(List<Uri> uris, boolean imageMode) {
        int max = imageMode ? getMaxImages() : getMaxVideos();
        if (uris == null) return Collections.emptyList();
        if (uris.size() <= max) return new ArrayList<>(uris);
        ToastUtil.show(requireContext(), getString(imageMode
                ? R.string.review_images_limit_reached
                : R.string.review_videos_limit_reached, max));
        return new ArrayList<>(uris.subList(0, max));
    }

    private void loadOrderDetails() {
        if (binding == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.contentScrollView.setVisibility(View.GONE);

        apiService.getOrderById(orderId).enqueue(new Callback<OrderDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<OrderDetailResponse> call, @NonNull Response<OrderDetailResponse> response) {
                if (!isAdded() || binding == null) return;

                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getOrder() != null) {
                    binding.contentScrollView.setVisibility(View.VISIBLE);
                    displayOrderInfo(response.body());
                } else {
                    ToastUtil.error(requireContext(), R.string.error_unknown);
                }
            }

            @Override
            public void onFailure(@NonNull Call<OrderDetailResponse> call, @NonNull Throwable t) {
                if (!isAdded() || binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                ToastUtil.error(requireContext(), getString(R.string.error_network, t.getMessage()));
            }
        });
    }

    private void displayOrderInfo(OrderDetailResponse detailResponse) {
        currentOrder = detailResponse.getOrder();
        String displayCode = currentOrder.getOrderCode();
        if (displayCode == null || displayCode.trim().isEmpty()) {
            String id = currentOrder.getId();
            displayCode = id != null && !id.isEmpty()
                    ? "#" + id.substring(Math.max(0, id.length() - 8))
                    : getString(R.string.order_details);
        }

        binding.tvOrderId.setText(getString(R.string.order_number_format, displayCode));
        OrderStatusUi.applyBadge(binding.tvOrderStatus, currentOrder.getStatus());
        binding.tvOrderDate.setText(getString(R.string.order_date_format, formatDate(currentOrder.getCreatedAt())));
        binding.tvShippingName.setText(safeText(currentOrder.getShippingName()));
        binding.tvShippingPhone.setText(safeText(currentOrder.getShippingPhone()));
        binding.tvShippingAddress.setText(safeText(currentOrder.getShippingAddress()));

        PaymentSummaryDto summary = currentOrder.getPaymentSummary() != null
                ? currentOrder.getPaymentSummary()
                : detailResponse.getPaymentSummary();
        PaymentDto latestPayment = detailResponse.getPayments() != null && !detailResponse.getPayments().isEmpty()
                ? detailResponse.getPayments().get(0)
                : null;

        String method = firstNonBlank(
                currentOrder.getPaymentMethod(),
                summary != null ? summary.getMethod() : null,
                latestPayment != null ? latestPayment.getMethod() : null
        );
        binding.tvPaymentMethod.setText(formatPaymentMethod(method));

        String paymentStatus = firstNonBlank(
                currentOrder.getPaymentStatus(),
                summary != null ? summary.getStatus() : null,
                latestPayment != null ? latestPayment.getStatus() : null
        );
        binding.tvPaymentStatus.setText(formatPaymentStatus(paymentStatus));
        binding.tvTotalAmount.setText(FormatUtil.formatCurrency(currentOrder.getTotalAmount()));

        currentItems = detailResponse.getItems() != null ? detailResponse.getItems() : currentOrder.getDetails();
        if (currentItems == null) currentItems = Collections.emptyList();

        renderOrderItems();
        updateTimeline(currentOrder.getStatus());
        loadOrderReviewStatus();
    }

    private void renderOrderItems() {
        binding.llOrderItems.removeAllViews();
        for (OrderDetailDto item : currentItems) {
            ItemOrderDetailBinding itemBinding = ItemOrderDetailBinding.inflate(getLayoutInflater(), binding.llOrderItems, true);
            bindOrderItemContent(itemBinding, item);
            bindReviewState(itemBinding, item);
        }
    }

    private void bindOrderItemContent(ItemOrderDetailBinding itemBinding, OrderDetailDto item) {
        String title = item.getProductName();
        if ((title == null || title.isEmpty()) && item.getProduct() != null) {
            title = item.getProduct().getTitle();
        }
        if (title == null || title.isEmpty()) {
            title = getString(R.string.product_default_name);
        }
        itemBinding.tvProductName.setText(title);

        String variantName = item.getVariantName();
        itemBinding.tvVariantName.setText(variantName != null && !variantName.trim().isEmpty()
                ? variantName
                : getString(R.string.variant_default_name));

        itemBinding.tvPrice.setText(FormatUtil.formatCurrency(item.getUnitPrice()));
        itemBinding.tvQuantity.setText(getString(R.string.quantity_format, item.getQuantity() != null ? item.getQuantity() : 1));

        String imageUrl = item.getImageUrl();
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(this).load(imageUrl).placeholder(R.drawable.placeholder_product).error(R.drawable.placeholder_product).into(itemBinding.ivProductImage);
        } else if (item.getProduct() != null && item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
            Glide.with(this).load(item.getProduct().getImages().get(0).getUrl()).placeholder(R.drawable.placeholder_product).error(R.drawable.placeholder_product).into(itemBinding.ivProductImage);
        } else {
            itemBinding.ivProductImage.setImageResource(R.drawable.placeholder_product);
        }
    }

    private void loadOrderReviewStatus() {
        if (orderId == null || orderId.trim().isEmpty()) return;

        apiService.getOrderReviewStatus(orderId).enqueue(new Callback<OrderReviewStatusDto>() {
            @Override
            public void onResponse(@NonNull Call<OrderReviewStatusDto> call, @NonNull Response<OrderReviewStatusDto> response) {
                reviewStatusMap.clear();
                if (response.isSuccessful() && response.body() != null && response.body().getItems() != null) {
                    for (OrderReviewItemDto reviewItem : response.body().getItems()) {
                        if (reviewItem == null) continue;
                        String detailId = reviewItem.getOrderDetailId();
                        if (detailId != null && !detailId.trim().isEmpty()) {
                            reviewStatusMap.put(detailId, reviewItem);
                        }
                    }
                }
                if (isAdded() && binding != null) {
                    renderOrderItems();
                }
            }

            @Override
            public void onFailure(@NonNull Call<OrderReviewStatusDto> call, @NonNull Throwable t) {
                reviewStatusMap.clear();
                if (isAdded() && binding != null) {
                    renderOrderItems();
                }
            }
        });
    }

    private void bindReviewState(ItemOrderDetailBinding itemBinding, OrderDetailDto item) {
        itemBinding.tvReviewState.setVisibility(View.GONE);
        itemBinding.tvReviewHint.setVisibility(View.GONE);
        itemBinding.btnWriteReview.setVisibility(View.GONE);
        itemBinding.btnWriteReview.setOnClickListener(null);

        String detailId = item != null ? item.getId() : null;
        if (detailId == null || detailId.trim().isEmpty()) return;

        OrderReviewItemDto reviewItem = reviewStatusMap.get(detailId);
        if (reviewItem != null) {
            showExistingReviewState(itemBinding, reviewItem);
            return;
        }

        if (!isReviewAllowedStatus(currentOrder != null ? currentOrder.getStatus() : null)) {
            itemBinding.tvReviewHint.setVisibility(View.VISIBLE);
            itemBinding.tvReviewHint.setText(R.string.review_available_after_delivery);
            return;
        }

        itemBinding.btnWriteReview.setVisibility(View.VISIBLE);
        itemBinding.btnWriteReview.setText(R.string.review_write);
        itemBinding.btnWriteReview.setOnClickListener(v -> showSubmitReviewDialog(item));
    }

    private void showExistingReviewState(ItemOrderDetailBinding itemBinding, OrderReviewItemDto reviewItem) {
        itemBinding.tvReviewState.setVisibility(View.VISIBLE);
        itemBinding.tvReviewHint.setVisibility(View.VISIBLE);

        String status = reviewItem.getStatus() != null ? reviewItem.getStatus().trim().toLowerCase() : "";
        int textColorRes = R.color.gray_700;
        int backgroundRes = R.drawable.bg_review_status_neutral;
        int labelRes = R.string.review_status_pending;
        int hintRes = R.string.review_submitted_waiting;

        if ("approved".equals(status)) {
            textColorRes = R.color.status_completed_text;
            backgroundRes = R.drawable.bg_review_status_approved;
            labelRes = R.string.review_status_approved;
            hintRes = R.string.review_already_visible;
        } else if ("rejected".equals(status)) {
            textColorRes = R.color.status_cancelled_text;
            backgroundRes = R.drawable.bg_review_status_rejected;
            labelRes = R.string.review_status_rejected;
            hintRes = R.string.review_rejected_notice;
        } else {
            textColorRes = R.color.status_pending_text;
            backgroundRes = R.drawable.bg_review_status_pending;
        }

        itemBinding.tvReviewState.setBackgroundResource(backgroundRes);
        itemBinding.tvReviewState.setText(labelRes);
        itemBinding.tvReviewState.setTextColor(ContextCompat.getColor(requireContext(), textColorRes));

        String shortContent = reviewItem.getContent() != null ? reviewItem.getContent().trim() : "";
        if (!shortContent.isEmpty()) {
            itemBinding.tvReviewHint.setText(getString(R.string.review_summary_with_rating,
                    reviewItem.getRating() != null ? reviewItem.getRating() : 0,
                    shortContent));
        } else {
            itemBinding.tvReviewHint.setText(hintRes);
        }
    }

    private void showSubmitReviewDialog(OrderDetailDto item) {
        if (!isAdded()) return;

        pendingReviewImageUris.clear();
        pendingReviewVideoUris.clear();

        DialogSubmitReviewBinding dialogBinding = DialogSubmitReviewBinding.inflate(getLayoutInflater());
        activeReviewDialogBinding = dialogBinding;
        dialogBinding.tvReviewProductName.setText(firstNonBlank(
                item.getProductName(),
                item.getProduct() != null ? item.getProduct().getTitle() : null,
                getString(R.string.product_default_name)
        ));
        dialogBinding.tvReviewMediaRules.setText(buildReviewMediaRulesText());

        dialogBinding.rvSelectedImages.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        dialogBinding.rvSelectedVideos.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        dialogBinding.rvSelectedImages.setAdapter(new ReviewMediaPreviewAdapter(
                pendingReviewImageUris,
                false,
                position -> {
                    if (position >= 0 && position < pendingReviewImageUris.size()) {
                        pendingReviewImageUris.remove(position);
                        updateSelectedMediaLabels();
                    }
                },
                position -> {
                    ArrayList<String> items = new ArrayList<>();
                    for (Uri uri : pendingReviewImageUris) items.add(uri.toString());
                    ReviewImageViewerDialogFragment.newInstance(items, position)
                            .show(getChildFragmentManager(), "selected_review_images");
                }
        ));

        dialogBinding.rvSelectedVideos.setAdapter(new ReviewMediaPreviewAdapter(
                pendingReviewVideoUris,
                true,
                position -> {
                    if (position >= 0 && position < pendingReviewVideoUris.size()) {
                        pendingReviewVideoUris.remove(position);
                        updateSelectedMediaLabels();
                    }
                },
                position -> {
                    if (position >= 0 && position < pendingReviewVideoUris.size()) {
                        ReviewVideoViewerDialogFragment.newInstance(pendingReviewVideoUris.get(position).toString())
                                .show(getChildFragmentManager(), "selected_review_video");
                    }
                }
        ));

        updateSelectedMediaLabels();
        dialogBinding.btnPickImages.setOnClickListener(v -> reviewImagesPickerLauncher.launch("image/*"));
        dialogBinding.btnPickVideos.setOnClickListener(v -> reviewVideosPickerLauncher.launch("video/*"));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.review_dialog_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.cancel, (d, which) -> d.dismiss())
                .setPositiveButton(R.string.review_submit, null)
                .create();
        activeReviewDialog = dialog;

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            RatingBar ratingBar = dialogBinding.ratingBarReview;
            TextInputLayout tilReviewContent = dialogBinding.tilReviewContent;
            TextInputEditText etReviewContent = dialogBinding.etReviewContent;

            int rating = Math.round(ratingBar.getRating());
            String content = etReviewContent.getText() != null ? etReviewContent.getText().toString().trim() : "";

            if (rating < 1) {
                tilReviewContent.setError(getString(R.string.review_rating_required));
                return;
            }
            tilReviewContent.setError(null);
            uploadMediaThenSubmitReview(item, rating, content, dialog);
        }));

        dialog.setOnDismissListener(d -> {
            activeReviewDialog = null;
            activeReviewDialogBinding = null;
            pendingReviewImageUris.clear();
            pendingReviewVideoUris.clear();
        });
        dialog.show();
    }

    private void updateSelectedMediaLabels() {
        if (activeReviewDialogBinding == null) return;

        activeReviewDialogBinding.tvSelectedImages.setText(
                pendingReviewImageUris.isEmpty()
                        ? getString(R.string.review_no_images_selected)
                        : getString(R.string.review_selected_images_count, pendingReviewImageUris.size())
        );
        activeReviewDialogBinding.rvSelectedImages.setVisibility(pendingReviewImageUris.isEmpty() ? View.GONE : View.VISIBLE);
        if (activeReviewDialogBinding.rvSelectedImages.getAdapter() != null) {
            activeReviewDialogBinding.rvSelectedImages.getAdapter().notifyDataSetChanged();
        }

        activeReviewDialogBinding.tvSelectedVideos.setText(
                pendingReviewVideoUris.isEmpty()
                        ? getString(R.string.review_no_videos_selected)
                        : getString(R.string.review_selected_videos_count, pendingReviewVideoUris.size())
        );
        activeReviewDialogBinding.rvSelectedVideos.setVisibility(pendingReviewVideoUris.isEmpty() ? View.GONE : View.VISIBLE);
        if (activeReviewDialogBinding.rvSelectedVideos.getAdapter() != null) {
            activeReviewDialogBinding.rvSelectedVideos.getAdapter().notifyDataSetChanged();
        }
    }

    private void uploadMediaThenSubmitReview(OrderDetailDto item, int rating, String content, AlertDialog dialog) {
        if (pendingReviewImageUris.isEmpty() && pendingReviewVideoUris.isEmpty()) {
            submitReview(item, rating, content, Collections.emptyList(), Collections.emptyList(), dialog);
            return;
        }

        MultipartBody multipartBody = buildReviewMediaBody();
        if (multipartBody == null) {
            ToastUtil.error(requireContext(), R.string.review_media_prepare_failed);
            return;
        }

        setDialogButtonsEnabled(dialog, false);
        setUploadProgressVisible(true);

        CountingRequestBody countingBody = new CountingRequestBody(multipartBody, (bytesWritten, contentLength) -> {
            int percent = contentLength > 0 ? (int) Math.min(100, Math.round((bytesWritten * 100f) / contentLength)) : 0;
            mainHandler.post(() -> updateUploadProgress(percent));
        });

        Request request = new Request.Builder()
                .url(BuildConfig.API_BASE_URL + "reviews/media")
                .post(countingBody)
                .build();

        OkHttpClient client = ApiClient.getRawClient();
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    setUploadProgressVisible(false);
                    setDialogButtonsEnabled(dialog, true);
                    ToastUtil.error(requireContext(), getString(R.string.error_network, e.getMessage()));
                });
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                String rawBody = response.body() != null ? response.body().string() : "";
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    if (response.isSuccessful()) {
                        ReviewMediaUploadResponseDto body = new Gson().fromJson(rawBody, ReviewMediaUploadResponseDto.class);
                        setUploadProgressVisible(false);
                        submitReview(
                                item,
                                rating,
                                content,
                                body != null && body.getImages() != null ? body.getImages() : Collections.emptyList(),
                                body != null && body.getVideos() != null ? body.getVideos() : Collections.emptyList(),
                                dialog
                        );
                        return;
                    }
                    setUploadProgressVisible(false);
                    setDialogButtonsEnabled(dialog, true);
                    ToastUtil.error(requireContext(), parseRawApiError(rawBody, R.string.review_media_upload_failed));
                });
            }
        });
    }

    private MultipartBody buildReviewMediaBody() {
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        int count = 0;

        for (Uri uri : pendingReviewImageUris) {
            PartWithName part = buildMultipartPart(uri, "review-image");
            if (part != null) {
                builder.addFormDataPart("files", part.fileName, part.requestBody);
                count++;
            }
        }
        for (Uri uri : pendingReviewVideoUris) {
            PartWithName part = buildMultipartPart(uri, "review-video");
            if (part != null) {
                builder.addFormDataPart("files", part.fileName, part.requestBody);
                count++;
            }
        }
        return count > 0 ? builder.build() : null;
    }

    private PartWithName buildMultipartPart(Uri uri, String fallbackBaseName) {
        if (!isAdded() || uri == null) return null;

        String mimeType = requireContext().getContentResolver().getType(uri);
        if (mimeType == null || mimeType.trim().isEmpty()) {
            mimeType = "application/octet-stream";
        }

        String extension = mimeType.contains("/") ? mimeType.substring(mimeType.indexOf('/') + 1) : "bin";
        String fileName = fallbackBaseName + "-" + System.currentTimeMillis() + "." + extension;

        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (inputStream == null) return null;

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            RequestBody requestBody = RequestBody.create(outputStream.toByteArray(), MediaType.parse(mimeType));
            return new PartWithName(fileName, requestBody);
        } catch (IOException e) {
            return null;
        }
    }

    private void submitReview(OrderDetailDto item, int rating, String content, List<String> images, List<String> videos, AlertDialog dialog) {
        String profileId = SessionManager.getInstance(requireContext()).getProfileId();
        ReviewSubmissionItemDto submissionItem = new ReviewSubmissionItemDto(
                item.getId(),
                rating,
                content,
                images,
                videos
        );
        ReviewSubmissionRequestDto request = new ReviewSubmissionRequestDto(
                orderId,
                profileId,
                Collections.singletonList(submissionItem)
        );

        setDialogButtonsEnabled(dialog, false);
        apiService.submitOrderReviews(request).enqueue(new Callback<ReviewSubmissionResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<ReviewSubmissionResponseDto> call, @NonNull Response<ReviewSubmissionResponseDto> response) {
                if (!isAdded()) return;
                setDialogButtonsEnabled(dialog, true);

                if (response.isSuccessful()) {
                    dialog.dismiss();
                    ReviewSubmissionResponseDto body = response.body();
                    if (body != null && body.getRewardedPoints() != null && body.getRewardedPoints() > 0) {
                        ToastUtil.show(requireContext(), getString(R.string.review_submit_success_with_points, body.getRewardedPoints()));
                    } else {
                        ToastUtil.show(requireContext(), R.string.review_submit_success);
                    }
                    loadOrderDetails();
                    return;
                }
                ToastUtil.error(requireContext(), parseApiError(response, R.string.review_submit_failed));
            }

            @Override
            public void onFailure(@NonNull Call<ReviewSubmissionResponseDto> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                setDialogButtonsEnabled(dialog, true);
                ToastUtil.error(requireContext(), getString(R.string.error_network, t.getMessage()));
            }
        });
    }

    private void setDialogButtonsEnabled(AlertDialog dialog, boolean enabled) {
        if (dialog == null) return;
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(enabled);
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(enabled);
        }
        if (activeReviewDialogBinding != null) {
            activeReviewDialogBinding.btnPickImages.setEnabled(enabled);
            activeReviewDialogBinding.btnPickVideos.setEnabled(enabled);
        }
    }

    private void setUploadProgressVisible(boolean visible) {
        if (activeReviewDialogBinding == null) return;
        activeReviewDialogBinding.progressUploadReview.setVisibility(visible ? View.VISIBLE : View.GONE);
        activeReviewDialogBinding.tvUploadProgress.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) {
            activeReviewDialogBinding.progressUploadReview.setProgress(0);
            activeReviewDialogBinding.tvUploadProgress.setText("");
        }
    }

    private void updateUploadProgress(int percent) {
        if (activeReviewDialogBinding == null) return;
        activeReviewDialogBinding.progressUploadReview.setProgressCompat(percent, true);
        activeReviewDialogBinding.tvUploadProgress.setText(getString(R.string.review_upload_progress, percent));
    }

    private String parseApiError(Response<?> response, int fallbackRes) {
        try {
            if (response != null && response.errorBody() != null) {
                String raw = response.errorBody().string();
                if (raw != null && !raw.trim().isEmpty()) {
                    JSONObject object = new JSONObject(raw);
                    String message = object.optString("message", "").trim();
                    if (!message.isEmpty()) return message;
                }
            }
        } catch (Exception ignored) {
        }
        return getString(fallbackRes);
    }

    private String parseRawApiError(String raw, int fallbackRes) {
        try {
            if (raw != null && !raw.trim().isEmpty()) {
                JSONObject object = new JSONObject(raw);
                String message = object.optString("message", "").trim();
                if (!message.isEmpty()) return message;
            }
        } catch (Exception ignored) {
        }
        return getString(fallbackRes);
    }

    private String buildReviewMediaRulesText() {
        return getString(R.string.review_media_rules, getMaxImages(), getMaxVideos(), getMaxFileSizeMb());
    }

    private int getMaxImages() {
        return reviewMediaConfig != null && reviewMediaConfig.getMaxImages() != null ? reviewMediaConfig.getMaxImages() : 5;
    }

    private int getMaxVideos() {
        return reviewMediaConfig != null && reviewMediaConfig.getMaxVideos() != null ? reviewMediaConfig.getMaxVideos() : 2;
    }

    private int getMaxFileSizeMb() {
        return reviewMediaConfig != null && reviewMediaConfig.getMaxFileSizeMb() != null ? reviewMediaConfig.getMaxFileSizeMb() : 30;
    }

    private String safeText(String value) {
        return value != null && !value.trim().isEmpty() ? value : "-";
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.trim().isEmpty() && !"-".equals(value.trim())) {
                return value;
            }
        }
        return null;
    }

    private String formatPaymentMethod(String method) {
        String normalized = method == null ? "" : method.trim().toLowerCase();
        switch (normalized) {
            case "cod":
                return getString(R.string.cod_short);
            case "bank_transfer":
            case "chuyen_khoan":
            case "transfer":
                return getString(R.string.bank_transfer);
            default:
                return safeText(method);
        }
    }

    private String formatPaymentStatus(String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase();
        switch (normalized) {
            case "paid":
            case "completed":
                return getString(R.string.payment_paid);
            case "pending":
            case "unpaid":
                return getString(R.string.payment_pending);
            case "failed":
            case "cancelled":
                return getString(R.string.payment_failed);
            default:
                return safeText(status);
        }
    }

    private String formatDate(String value) {
        return value != null && !value.trim().isEmpty()
                ? value.substring(0, Math.min(10, value.length()))
                : "-";
    }

    private boolean isReviewAllowedStatus(String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase();
        return "delivered".equals(normalized) || "completed".equals(normalized);
    }

    private void updateTimeline(String status) {
        OrderStatusUi.applyTimeline(
                status,
                binding.ivStep1, binding.tvStep1, binding.line1,
                binding.ivStep2, binding.tvStep2, binding.line2,
                binding.ivStep3, binding.tvStep3, binding.line3,
                binding.ivStep4, binding.tvStep4
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        activeReviewDialogBinding = null;
        activeReviewDialog = null;
        binding = null;
    }

    private static final class PartWithName {
        final String fileName;
        final RequestBody requestBody;

        PartWithName(String fileName, RequestBody requestBody) {
            this.fileName = fileName;
            this.requestBody = requestBody;
        }
    }
}
