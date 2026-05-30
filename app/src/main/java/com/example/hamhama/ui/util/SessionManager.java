package com.example.hamhama.ui.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFS = "cookbook_session";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME = "name";
    private static final String KEY_PHOTO_URL = "photo_url";
    private static final String KEY_HOME_QUERY = "home_query";
    private static final String KEY_HOME_CATEGORY = "home_category";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveSession(String email, String name) {
        saveSession(email, name, "");
    }

    public void saveSession(String email, String name, String photoUrl) {
        preferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_EMAIL, email)
                .putString(KEY_NAME, name)
                .putString(KEY_PHOTO_URL, photoUrl)
                .apply();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_LOGGED_IN, false);
    }

    public String getName() {
        return preferences.getString(KEY_NAME, "Chef");
    }

    public String getEmail() {
        return preferences.getString(KEY_EMAIL, "");
    }

    public String getPhotoUrl() {
        return preferences.getString(KEY_PHOTO_URL, "");
    }

    public void updateProfile(String name, String photoUrl) {
        preferences.edit()
                .putString(KEY_NAME, name)
                .putString(KEY_PHOTO_URL, photoUrl)
                .apply();
    }

    public void saveHomeState(String query, String category) {
        preferences.edit()
                .putString(KEY_HOME_QUERY, query)
                .putString(KEY_HOME_CATEGORY, category)
                .apply();
    }

    public String getHomeQuery() {
        return preferences.getString(KEY_HOME_QUERY, "");
    }

    public String getHomeCategory() {
        return preferences.getString(KEY_HOME_CATEGORY, "All");
    }

    public void clear() {
        preferences.edit().clear().apply();
    }
}