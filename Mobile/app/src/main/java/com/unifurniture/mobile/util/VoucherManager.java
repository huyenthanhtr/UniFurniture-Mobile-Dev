package com.unifurniture.mobile.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.data.model.CouponDto;
import com.unifurniture.mobile.data.model.VoucherDto;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class VoucherManager {

    private static final String PREFS_NAME = "unifurniture_vouchers";
    private static final String KEY_VOUCHERS = "vouchers_list";
    private static final String KEY_SELECTED = "selected_voucher_code";

    private static VoucherManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    private VoucherManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized VoucherManager getInstance(Context context) {
        if (instance == null) {
            instance = new VoucherManager(context);
        }
        return instance;
    }

    public synchronized List<VoucherDto> getVouchers() {
        String json = prefs.getString(KEY_VOUCHERS, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<VoucherDto>>() {}.getType();
        List<VoucherDto> list = gson.fromJson(json, type);
        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public synchronized void setSelectedVoucherCode(String code) {
        prefs.edit().putString(KEY_SELECTED, code).apply();
    }

    public synchronized String getSelectedVoucherCode() {
        return prefs.getString(KEY_SELECTED, null);
    }

    public synchronized void clearSelectedVoucherCode() {
        prefs.edit().remove(KEY_SELECTED).apply();
    }

    public synchronized VoucherDto getSelectedVoucher() {
        String code = getSelectedVoucherCode();
        if (code == null) return null;
        for (VoucherDto v : getVouchers()) {
            if (v != null && code.equals(v.code) && !v.isUsed) {
                return v;
            }
        }
        return null;
    }

    public synchronized void markAsUsed(String code) {
        if (code == null) return;
        List<VoucherDto> list = getVouchers();
        boolean changed = false;
        for (VoucherDto v : list) {
            if (v != null && code.equalsIgnoreCase(v.code)) {
                v.isUsed = true;
                changed = true;
                break;
            }
        }
        if (changed) {
            saveVouchers(list);
        }
        if (code.equalsIgnoreCase(getSelectedVoucherCode())) {
            clearSelectedVoucherCode();
        }
    }

    public double calculateDiscount(VoucherDto voucher, double subtotal) {
        if (voucher == null || subtotal < voucher.minOrderValue) return 0;
        if ("fixed".equals(voucher.discountType)) {
            return Math.min(voucher.discountValue, subtotal);
        } else if ("percent".equals(voucher.discountType)) {
            double calculated = subtotal * (voucher.discountValue / 100.0);
            if (voucher.maxDiscountValue > 0) {
                calculated = Math.min(calculated, voucher.maxDiscountValue);
            }
            return Math.min(calculated, subtotal);
        }
        return 0;
    }

    public synchronized void saveVouchers(List<VoucherDto> list) {
        prefs.edit().putString(KEY_VOUCHERS, gson.toJson(list)).apply();
    }

    public static VoucherDto convertCouponToVoucher(Context context, CouponDto coupon) {
        if (coupon == null) return null;

        String name;
        String description;

        if ("fixed".equalsIgnoreCase(coupon.discountType)) {
            name = context.getString(R.string.voucher_name_fixed, FormatUtil.formatCurrency(coupon.discountValue));
            description = context.getString(R.string.voucher_desc_fixed, FormatUtil.formatCurrency(coupon.minOrderValue));
        } else {
            name = context.getString(R.string.voucher_name_percent, (int) coupon.discountValue);
            double maxAmount = (coupon.maxDiscountAmount != null) ? coupon.maxDiscountAmount : 0;
            if (maxAmount > 0) {
                description = context.getString(R.string.voucher_desc_percent_max,
                        (int) coupon.discountValue,
                        FormatUtil.formatCurrency(maxAmount),
                        FormatUtil.formatCurrency(coupon.minOrderValue));
            } else {
                description = context.getString(R.string.voucher_desc_percent,
                        (int) coupon.discountValue,
                        FormatUtil.formatCurrency(coupon.minOrderValue));
            }
        }

        String expiration = formatExpirationDate(context, coupon.endAt);
        boolean isUsed = "used".equalsIgnoreCase(coupon.status) || coupon.used >= coupon.totalLimit;

        return new VoucherDto(
                coupon.code,
                name,
                description,
                coupon.discountType,
                coupon.discountValue,
                coupon.minOrderValue,
                (coupon.maxDiscountAmount != null) ? coupon.maxDiscountAmount : 0,
                isUsed,
                expiration
        );
    }

    private static String formatExpirationDate(Context context, String endAt) {
        if (endAt == null || endAt.isEmpty()) {
            return "";
        }
        try {
            if (endAt.length() >= 10) {
                String datePart = endAt.substring(0, 10);
                String[] parts = datePart.split("-");
                if (parts.length == 3) {
                    String formattedDate = parts[2] + "/" + parts[1] + "/" + parts[0];
                    return context.getString(R.string.voucher_expiry_date, formattedDate);
                }
            }
        } catch (Exception e) {
            // fallback
        }
        return context.getString(R.string.voucher_expiry_date, endAt);
    }
}
