package com.example.hamhama.ui.util;

import android.text.TextUtils;

public final class TextFormatters {

    private TextFormatters() {
    }

    public static String clean(String value) {
        return TextUtils.isEmpty(value) ? "" : value.trim();
    }

    public static String displayEmpty(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }
}