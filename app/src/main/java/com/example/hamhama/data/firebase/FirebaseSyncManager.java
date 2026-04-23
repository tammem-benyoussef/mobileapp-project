package com.example.hamhama.data.firebase;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.example.hamhama.data.model.Recipe;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseSyncManager {

    public interface AuthCallback {
        void onSuccess(@NonNull FirebaseUser user);

        void onError(@NonNull String message);
    }

    public interface RecipeListCallback {
        void onSuccess(@NonNull List<Recipe> recipes);

        void onError(@NonNull String message);
    }

    private final boolean available;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    public FirebaseSyncManager(Context context) {
        boolean initialized;
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context);
            }
            initialized = !FirebaseApp.getApps(context).isEmpty();
        } catch (Exception e) {
            initialized = false;
        }
        available = initialized;
        if (available) {
            auth = FirebaseAuth.getInstance();
            firestore = FirebaseFirestore.getInstance();
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isLoggedIn() {
        return available && auth != null && auth.getCurrentUser() != null;
    }

    public void signIn(String email, String password, AuthCallback callback) {
        if (!available || auth == null) {
            callback.onError("Firebase is not configured. Add google-services.json to enable cloud login.");
            return;
        }
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        callback.onSuccess(user);
                    } else {
                        callback.onError("Login failed. Please try again.");
                    }
                })
                .addOnFailureListener(error -> callback.onError(error.getMessage() == null ? "Login failed." : error.getMessage()));
    }

    public void signOut() {
        if (available && auth != null) {
            auth.signOut();
        }
    }

    public void syncRecipe(Recipe recipe) {
        if (!available || firestore == null || auth == null || auth.getCurrentUser() == null || recipe == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", recipe.getId());
        payload.put("title", recipe.getTitle());
        payload.put("category", recipe.getCategory());
        payload.put("imageUrl", recipe.getImageUrl());
        payload.put("localImageUri", recipe.getLocalImageUri());
        payload.put("ingredients", recipe.getIngredients());
        payload.put("steps", recipe.getSteps());
        payload.put("summary", recipe.getSummary());
        payload.put("source", recipe.getSource());
        payload.put("remoteId", recipe.getRemoteId());
        payload.put("favorite", recipe.isFavorite());
        payload.put("createdAt", recipe.getCreatedAt());
        payload.put("updatedAt", recipe.getUpdatedAt());

        firestore.collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("recipes")
                .document(recipe.getId())
                .set(payload);
    }

    public String getUserLabel() {
        if (!isLoggedIn()) {
            return "CookBook User";
        }
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return "CookBook User";
        }
        if (!TextUtils.isEmpty(user.getDisplayName())) {
            return user.getDisplayName();
        }
        if (!TextUtils.isEmpty(user.getEmail())) {
            return user.getEmail();
        }
        return "CookBook User";
    }

    public void fetchUserRecipes(RecipeListCallback callback) {
        if (!available || firestore == null || auth == null || auth.getCurrentUser() == null) {
            callback.onError("Cloud sync is unavailable.");
            return;
        }
        firestore.collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("recipes")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Recipe> recipes = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        recipes.add(mapRecipe(doc.getData()));
                    }
                    callback.onSuccess(recipes);
                })
                .addOnFailureListener(error -> callback.onError(error.getMessage() == null ? "Cloud sync failed." : error.getMessage()));
    }

    private Recipe mapRecipe(Map<String, Object> data) {
        Recipe recipe = new Recipe();
        recipe.setId(stringValue(data.get("id"), "local_" + System.currentTimeMillis()));
        recipe.setTitle(stringValue(data.get("title"), "Untitled Recipe"));
        recipe.setCategory(stringValue(data.get("category"), "All"));
        recipe.setImageUrl(stringValue(data.get("imageUrl"), ""));
        recipe.setLocalImageUri(stringValue(data.get("localImageUri"), ""));
        recipe.setIngredients(stringValue(data.get("ingredients"), ""));
        recipe.setSteps(stringValue(data.get("steps"), ""));
        recipe.setSummary(stringValue(data.get("summary"), ""));
        recipe.setSource(stringValue(data.get("source"), "local"));
        recipe.setRemoteId(longValue(data.get("remoteId"), 0L));
        recipe.setFavorite(booleanValue(data.get("favorite"), false));
        recipe.setCreatedAt(longValue(data.get("createdAt"), System.currentTimeMillis()));
        recipe.setUpdatedAt(longValue(data.get("updatedAt"), System.currentTimeMillis()));
        return recipe;
    }

    private String stringValue(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private long longValue(Object value, long fallback) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return fallback;
    }

    private boolean booleanValue(Object value, boolean fallback) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return fallback;
    }
}