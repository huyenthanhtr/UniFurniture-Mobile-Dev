package com.unifurniture.mobile.ui.common;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.databinding.DialogReviewImageViewerBinding;
import com.unifurniture.mobile.ui.adapter.FullscreenImagePagerAdapter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ReviewImageViewerDialogFragment extends DialogFragment {

    private static final String ARG_IMAGES_JSON = "images_json";
    private static final String ARG_START_INDEX = "start_index";

    public static ReviewImageViewerDialogFragment newInstance(List<String> images, int startIndex) {
        ReviewImageViewerDialogFragment fragment = new ReviewImageViewerDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGES_JSON, new Gson().toJson(images != null ? images : new ArrayList<>()));
        args.putInt(ARG_START_INDEX, Math.max(0, startIndex));
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        DialogReviewImageViewerBinding binding = DialogReviewImageViewerBinding.inflate(LayoutInflater.from(requireContext()));

        List<String> images = readImages();
        int startIndex = getArguments() != null ? getArguments().getInt(ARG_START_INDEX, 0) : 0;
        FullscreenImagePagerAdapter adapter = new FullscreenImagePagerAdapter(requireContext(), images);
        binding.viewPagerReviewImages.setAdapter(adapter);
        binding.viewPagerReviewImages.setCurrentItem(Math.min(startIndex, Math.max(images.size() - 1, 0)), false);
        updateCounter(binding, binding.viewPagerReviewImages.getCurrentItem(), images.size());
        binding.viewPagerReviewImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateCounter(binding, position, images.size());
            }
        });

        binding.btnClose.setOnClickListener(v -> dismiss());

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(binding.getRoot())
                .create();
    }

    private List<String> readImages() {
        String json = getArguments() != null ? getArguments().getString(ARG_IMAGES_JSON, "[]") : "[]";
        Type type = new TypeToken<List<String>>() {}.getType();
        List<String> parsed = new Gson().fromJson(json, type);
        return parsed != null ? parsed : new ArrayList<>();
    }

    private void updateCounter(DialogReviewImageViewerBinding binding, int position, int total) {
        binding.tvCounter.setText(getString(R.string.image_indicator_format, position + 1, Math.max(total, 1)));
    }
}
