package com.unifurniture.mobile.ui.product;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.databinding.FragmentFilterBottomSheetBinding;
import java.util.ArrayList;

public class FilterBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_CAT_IDS   = "cat_ids";
    private static final String ARG_CAT_NAMES = "cat_names";
    private static final String ARG_SEL_CAT   = "sel_category";
    private static final String ARG_MIN_PRICE = "min_price";
    private static final String ARG_MAX_PRICE = "max_price";
    private static final String ARG_MIN_RATING = "min_rating";

    public interface OnFiltersAppliedListener {
        void onFiltersApplied(String categoryId, Double minPrice, Double maxPrice, int minRating);
        void onFiltersCleared();
    }

    private FragmentFilterBottomSheetBinding binding;
    private OnFiltersAppliedListener listener;

    public static FilterBottomSheetFragment newInstance(
            ArrayList<String> categoryIds,
            ArrayList<String> categoryNames,
            String selectedCategoryId,
            Double minPrice,
            Double maxPrice,
            int minRating) {
        FilterBottomSheetFragment f = new FilterBottomSheetFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_CAT_IDS, categoryIds);
        args.putStringArrayList(ARG_CAT_NAMES, categoryNames);
        args.putString(ARG_SEL_CAT, selectedCategoryId);
        if (minPrice != null) args.putDouble(ARG_MIN_PRICE, minPrice);
        if (maxPrice != null) args.putDouble(ARG_MAX_PRICE, maxPrice);
        args.putInt(ARG_MIN_RATING, minRating);
        f.setArguments(args);
        return f;
    }

    public void setOnFiltersAppliedListener(OnFiltersAppliedListener l) {
        this.listener = l;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFilterBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args == null) return;

        ArrayList<String> catIds   = args.getStringArrayList(ARG_CAT_IDS);
        ArrayList<String> catNames = args.getStringArrayList(ARG_CAT_NAMES);
        String selCategory = args.getString(ARG_SEL_CAT);
        double minPriceArg = args.containsKey(ARG_MIN_PRICE) ? args.getDouble(ARG_MIN_PRICE) : -1;
        double maxPriceArg = args.containsKey(ARG_MAX_PRICE) ? args.getDouble(ARG_MAX_PRICE) : -1;
        int minRating = args.getInt(ARG_MIN_RATING, 0);

        buildCategoryChips(catIds, catNames, selCategory);

        if (minPriceArg >= 0) binding.etMinPrice.setText(String.valueOf((long) minPriceArg));
        if (maxPriceArg >= 0) binding.etMaxPrice.setText(String.valueOf((long) maxPriceArg));

        switch (minRating) {
            case 3: binding.chipRating3.setChecked(true); break;
            case 4: binding.chipRating4.setChecked(true); break;
            case 5: binding.chipRating5.setChecked(true); break;
            default: binding.chipRatingAll.setChecked(true); break;
        }

        binding.btnApplyFilter.setOnClickListener(v -> applyAndDismiss());
        binding.btnClearFilter.setOnClickListener(v -> clearAndDismiss());
    }

    private void buildCategoryChips(ArrayList<String> ids, ArrayList<String> names, String selId) {
        if (ids == null || names == null) return;

        Chip allChip = makeChoiceChip(getString(R.string.filter_all), null);
        binding.chipGroupCategories.addView(allChip);
        if (selId == null) binding.chipGroupCategories.check(allChip.getId());

        for (int i = 0; i < ids.size() && i < names.size(); i++) {
            Chip chip = makeChoiceChip(names.get(i), ids.get(i));
            binding.chipGroupCategories.addView(chip);
            if (ids.get(i).equals(selId)) binding.chipGroupCategories.check(chip.getId());
        }
    }

    private Chip makeChoiceChip(String label, String tag) {
        int colorPrimary  = ContextCompat.getColor(requireContext(), R.color.primary);
        int colorWhite    = ContextCompat.getColor(requireContext(), R.color.white);
        int colorGray200  = ContextCompat.getColor(requireContext(), R.color.gray_200);
        int colorBlack    = ContextCompat.getColor(requireContext(), R.color.black);

        // text: white when checked, black when unchecked
        ColorStateList textColors = new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{colorWhite, colorBlack});

        // bg: primary when checked, gray_200 when unchecked
        ColorStateList bgColors = new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{colorPrimary, colorGray200});

        Chip chip = new Chip(requireContext());
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setTag(tag);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setChipBackgroundColor(bgColors);
        chip.setTextColor(textColors);
        chip.setChipStrokeWidth(0f);
        return chip;
    }

    private void applyAndDismiss() {
        if (listener == null) { dismiss(); return; }

        // Read selected category (null tag = "Tất cả" = no filter)
        String categoryId = null;
        int checkedId = binding.chipGroupCategories.getCheckedChipId();
        if (checkedId != View.NO_ID) {
            Chip chip = binding.chipGroupCategories.findViewById(checkedId);
            if (chip != null) categoryId = (String) chip.getTag();
        }

        // Read price inputs
        String minText = binding.etMinPrice.getText() != null ? binding.etMinPrice.getText().toString() : "";
        String maxText = binding.etMaxPrice.getText() != null ? binding.etMaxPrice.getText().toString() : "";
        Double minP = parsePrice(minText);
        Double maxP = parsePrice(maxText);
        // Swap if user entered min > max
        if (minP != null && maxP != null && minP > maxP) {
            Double tmp = minP; minP = maxP; maxP = tmp;
        }

        // Read rating selection
        int rating = 0;
        int ratingId = binding.chipGroupRating.getCheckedChipId();
        if (ratingId == R.id.chipRating3)      rating = 3;
        else if (ratingId == R.id.chipRating4) rating = 4;
        else if (ratingId == R.id.chipRating5) rating = 5;

        listener.onFiltersApplied(categoryId, minP, maxP, rating);
        dismiss();
    }

    private void clearAndDismiss() {
        if (listener != null) listener.onFiltersCleared();
        dismiss();
    }

    private Double parsePrice(String text) {
        if (TextUtils.isEmpty(text.trim())) return null;
        try {
            double val = Double.parseDouble(text.trim());
            return val > 0 ? val : null; // bỏ qua giá trị âm hoặc bằng 0
        } catch (NumberFormatException e) { return null; }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
