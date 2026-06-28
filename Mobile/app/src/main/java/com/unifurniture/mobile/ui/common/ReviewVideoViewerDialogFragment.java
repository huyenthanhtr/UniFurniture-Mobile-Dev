package com.unifurniture.mobile.ui.common;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.MediaController;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.unifurniture.mobile.databinding.DialogReviewVideoViewerBinding;

public class ReviewVideoViewerDialogFragment extends DialogFragment {

    private static final String ARG_VIDEO_URL = "video_url";

    public static ReviewVideoViewerDialogFragment newInstance(String videoUrl) {
        ReviewVideoViewerDialogFragment fragment = new ReviewVideoViewerDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_VIDEO_URL, videoUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        DialogReviewVideoViewerBinding binding = DialogReviewVideoViewerBinding.inflate(LayoutInflater.from(requireContext()));
        String videoUrl = getArguments() != null ? getArguments().getString(ARG_VIDEO_URL, "") : "";
        MediaController mediaController = new MediaController(requireContext());
        mediaController.setAnchorView(binding.videoViewReview);
        binding.videoViewReview.setMediaController(mediaController);
        binding.videoViewReview.setVideoURI(Uri.parse(videoUrl));
        binding.videoViewReview.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            binding.videoViewReview.start();
        });
        binding.btnClose.setOnClickListener(v -> dismiss());

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(binding.getRoot())
                .create();
    }
}
