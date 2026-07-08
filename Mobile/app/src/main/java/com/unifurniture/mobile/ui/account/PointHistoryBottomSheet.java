package com.unifurniture.mobile.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.PointTransactionDto;
import com.unifurniture.mobile.data.remote.ApiClient;
import com.unifurniture.mobile.data.remote.ApiService;
import com.unifurniture.mobile.ui.adapter.PointHistoryAdapter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PointHistoryBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PROFILE_ID = "profile_id";

    private String profileId;
    private RecyclerView recyclerView;
    private PointHistoryAdapter adapter;
    private ProgressBar progressBar;
    private View layoutEmpty;

    public PointHistoryBottomSheet() {
    }

    public static PointHistoryBottomSheet newInstance(String profileId) {
        PointHistoryBottomSheet fragment = new PointHistoryBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_PROFILE_ID, profileId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            profileId = getArguments().getString(ARG_PROFILE_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_point_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerPointHistory);
        progressBar = view.findViewById(R.id.progressLoading);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);

        view.findViewById(R.id.btnClose).setOnClickListener(v -> dismiss());

        adapter = new PointHistoryAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        loadPointHistory();
    }

    private void loadPointHistory() {
        if (profileId == null || profileId.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);

        ApiService apiService = ApiClient.getInstance();
        apiService.getPointTransactions(profileId).enqueue(new Callback<List<PointTransactionDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<PointTransactionDto>> call, @NonNull Response<List<PointTransactionDto>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    List<PointTransactionDto> data = response.body();
                    if (!data.isEmpty()) {
                        adapter.setData(data);
                        recyclerView.setVisibility(View.VISIBLE);
                        layoutEmpty.setVisibility(View.GONE);
                    } else {
                        recyclerView.setVisibility(View.GONE);
                        layoutEmpty.setVisibility(View.VISIBLE);
                    }
                } else {
                    recyclerView.setVisibility(View.GONE);
                    layoutEmpty.setVisibility(View.VISIBLE);
                    String errorMsg = "Lỗi tải: Code " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PointTransactionDto>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
