package com.example.hamhama.ui.util;

import android.app.Activity;
import android.content.Intent;

public final class ActivityTransition {

    private ActivityTransition() {
    }

    public static void open(Activity activity, Intent intent) {
        activity.startActivity(intent);
        activity.overridePendingTransition(com.example.hamhama.R.anim.slide_in_right, com.example.hamhama.R.anim.slide_out_left);
    }

    public static void finish(Activity activity) {
        activity.finish();
        activity.overridePendingTransition(com.example.hamhama.R.anim.slide_in_left, com.example.hamhama.R.anim.slide_out_right);
    }
}