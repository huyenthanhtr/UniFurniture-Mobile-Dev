package com.unifurniture.mobile.ui.checkout;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.UniFurnitureApp;
import com.unifurniture.mobile.data.repository.OrderRepository;
import com.unifurniture.mobile.databinding.FragmentPaymentQrBinding;
import com.unifurniture.mobile.util.FormatUtil;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class PaymentQrFragment extends Fragment {

    private FragmentPaymentQrBinding binding;
    private CountDownTimer countDownTimer;
    
    private String orderId;
    private String orderCode;
    private float totalAmount;
    private float depositAmount;
    private boolean requireDeposit;
    private String shippingPhone;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPaymentQrBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            orderId = getArguments().getString("order_id");
            orderCode = getArguments().getString("order_code");
            totalAmount = getArguments().getFloat("total_amount");
            depositAmount = getArguments().getFloat("deposit_amount");
            requireDeposit = getArguments().getBoolean("require_deposit");
            shippingPhone = getArguments().getString("shipping_phone");
        }

        // Amount to pay is deposit if requireDeposit, otherwise total
        float amountToPay = requireDeposit ? depositAmount : totalAmount;
        binding.tvTransferAmount.setText(FormatUtil.formatCurrency(amountToPay));
        binding.tvTransferMessage.setText("CK " + orderCode);

        // Generate VietQR dynamic link
        String memo = "CK " + orderCode;
        String accountName = "CONG TY TNHH NOI THAT U-HOME FURNI";
        String qrUrl = "";
        try {
            qrUrl = "https://img.vietqr.io/image/vietcombank-0011111222333-compact2.png"
                    + "?amount=" + (long) amountToPay
                    + "&addInfo=" + URLEncoder.encode(memo, "UTF-8")
                    + "&accountName=" + URLEncoder.encode(accountName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            qrUrl = "https://img.vietqr.io/image/vietcombank-0011111222333-compact2.png"
                    + "?amount=" + (long) amountToPay
                    + "&addInfo=" + memo;
        }

        // Load QR Code via Glide
        Glide.with(this)
                .load(qrUrl)
                .placeholder(android.R.drawable.progress_horizontal)
                .error(android.R.drawable.stat_notify_error)
                .into(binding.ivQrCode);

        // Set up Copy Buttons
        binding.btnCopyAccount.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) 
                    requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Account Number", "0011111222333");
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(requireContext(), getString(R.string.copied), Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnCopyMessage.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) 
                    requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Transfer Message", "CK " + orderCode);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(requireContext(), getString(R.string.copied), Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnBack.setOnClickListener(v -> handleCancelConfirm());
        binding.btnCancel.setOnClickListener(v -> handleCancelConfirm());

        // Start 1 minute CountDownTimer (60 seconds)
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (binding == null) return;
                long seconds = millisUntilFinished / 1000;
                long mins = seconds / 60;
                long secs = seconds % 60;
                binding.tvTimer.setText(String.format(java.util.Locale.US, "%02d:%02d", mins, secs));
            }

            @Override
            public void onFinish() {
                if (binding == null) return;
                binding.tvTimer.setText("00:00");
                completePaymentDemo();
            }
        }.start();
    }

    private void handleCancelConfirm() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.cancel_payment_confirm_title)
                .setMessage(R.string.cancel_payment_confirm_msg)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> cancelOrder())
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void cancelOrder() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnCancel.setEnabled(false);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        OrderRepository repo = new OrderRepository(UniFurnitureApp.getInstance().getApiService());
        repo.requestCancelOrder(orderId, "Khách hàng hủy thanh toán QR", "", shippingPhone)
                .observe(getViewLifecycleOwner(), success -> {
                    if (binding == null) return;
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Yêu cầu hủy đơn hàng thành công", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigate(R.id.homeFragment);
                });
    }

    private void completePaymentDemo() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnCancel.setEnabled(false);

        OrderRepository repo = new OrderRepository(UniFurnitureApp.getInstance().getApiService());
        repo.completeDemoTransfer(orderId).observe(getViewLifecycleOwner(), success -> {
            if (binding == null) return;
            binding.progressBar.setVisibility(View.GONE);
            
            Bundle bundle = new Bundle();
            bundle.putString("order_code", orderCode);
            Navigation.findNavController(requireView()).navigate(R.id.orderSuccessFragment, bundle);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        binding = null;
    }
}
