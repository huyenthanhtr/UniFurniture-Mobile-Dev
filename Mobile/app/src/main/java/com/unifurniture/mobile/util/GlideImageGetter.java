package com.unifurniture.mobile.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.unifurniture.mobile.BuildConfig;

public class GlideImageGetter implements Html.ImageGetter {
    private final TextView textView;

    public GlideImageGetter(TextView textView) {
        this.textView = textView;
    }

    @Override
    public Drawable getDrawable(String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }

        String url = source;
        String baseUrl = BuildConfig.API_BASE_URL.replace("/api/", "");
        if (url.startsWith("//")) {
            url = "https:" + url;
        } else if (url.startsWith("/")) {
            url = baseUrl + url;
        } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = baseUrl + "/" + url;
        }

        final UrlDrawable urlDrawable = new UrlDrawable();

        Glide.with(textView.getContext())
                .asBitmap()
                .load(url)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        try {
                            int textViewWidth = textView.getWidth() - textView.getPaddingLeft() - textView.getPaddingRight();
                            if (textViewWidth <= 0) {
                                textViewWidth = textView.getResources().getDisplayMetrics().widthPixels 
                                        - textView.getPaddingLeft() - textView.getPaddingRight() 
                                        - (int) (40 * textView.getResources().getDisplayMetrics().density);
                            }
                            int width = textViewWidth > 0 ? textViewWidth : resource.getWidth();
                            
                            // Calculate height while maintaining aspect ratio
                            float aspectRatio = (float) resource.getHeight() / (float) resource.getWidth();
                            int height = Math.round(width * aspectRatio);

                            BitmapDrawable bitmapDrawable = new BitmapDrawable(textView.getResources(), resource);
                            bitmapDrawable.setBounds(0, 0, width, height);

                            // Add bottom padding (14dp) to prevent description text below from sticking to the image
                            int paddingBottom = (int) (14 * textView.getResources().getDisplayMetrics().density);
                            int totalHeight = height + paddingBottom;

                            urlDrawable.setBounds(0, 0, width, totalHeight);
                            urlDrawable.drawable = bitmapDrawable;

                            // Re-set text to trigger redraw and layout recalculation
                            textView.setText(textView.getText());
                            textView.invalidate();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Not implemented
                    }
                });

        return urlDrawable;
    }

    @SuppressWarnings("deprecation")
    public static class UrlDrawable extends BitmapDrawable {
        public Drawable drawable;

        public UrlDrawable() {
            super();
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            if (drawable != null) {
                drawable.draw(canvas);
            }
        }
    }
}
