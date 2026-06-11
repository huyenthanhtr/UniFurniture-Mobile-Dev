package com.unifurniture.mobile.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.unifurniture.mobile.data.model.VoucherDto;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class VoucherManager {

    private static final String PREFS_NAME = "unifurniture_vouchers";
    private static final String KEY_VOUCHERS = "vouchers_list";
    private static final String KEY_SELECTED = "selected_voucher_code";
    private static final String KEY_SEEDED = "vouchers_seeded";

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
            if (!prefs.getBoolean(KEY_SEEDED, false)) {
                return seedDefaultVouchers();
            }
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

    private void saveVouchers(List<VoucherDto> list) {
        prefs.edit().putString(KEY_VOUCHERS, gson.toJson(list)).apply();
    }

    private List<VoucherDto> seedDefaultVouchers() {
        List<VoucherDto> list = new ArrayList<>();

        list.add(new VoucherDto(
                "UNIFRESH50",
                "Giảm 50.000 ₫",
                "Áp dụng cho mọi đơn hàng có giá trị từ 500.000 ₫ trở lên khi mua đồ gỗ nội thất.",
                "fixed",
                50000,
                500000,
                0,
                false,
                "Hạn dùng: 31/12/2026"
        ));

        list.add(new VoucherDto(
                "UNISUPER10",
                "Giảm 10%",
                "Giảm ngay 10% tổng giá trị đơn hàng, tối đa 200.000 ₫ cho các đơn hàng từ 1.000.000 ₫.",
                "percent",
                10,
                1000000,
                200000,
                false,
                "Hạn dùng: 31/12/2026"
        ));

        list.add(new VoucherDto(
                "WELCOMENEW",
                "Giảm 15%",
                "Mã giảm giá chào mừng thành viên mới. Giảm 15% tối đa 100.000 ₫ không yêu cầu giá trị tối thiểu.",
                "percent",
                15,
                0,
                100000,
                false,
                "Hạn dùng: 31/12/2026"
        ));

        list.add(new VoucherDto(
                "FREESHIP30",
                "Giảm 30.000 ₫",
                "Áp dụng cho đơn hàng từ 300.000 ₫ trở lên. Hỗ trợ phí vận chuyển tận nhà.",
                "fixed",
                30000,
                300000,
                0,
                false,
                "Hạn dùng: 31/12/2026"
        ));

        saveVouchers(list);
        prefs.edit().putBoolean(KEY_SEEDED, true).apply();
        return list;
    }
}
