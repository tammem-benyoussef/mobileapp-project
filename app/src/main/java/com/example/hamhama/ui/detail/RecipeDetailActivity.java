package com.example.hamhama.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.hamhama.R;
import com.example.hamhama.data.model.Recipe;
import com.example.hamhama.databinding.ActivityRecipeDetailBinding;
import com.example.hamhama.ui.viewmodel.RecipeDetailViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_IMAGE_URL = "extra_image_url";
    public static final String EXTRA_SUMMARY = "extra_summary";
    public static final String EXTRA_INSTRUCTIONS = "extra_instructions";
    public static final String EXTRA_CATEGORY = "extra_category";
    public static final String EXTRA_FROM_SEARCH = "extra_from_search";

    private ActivityRecipeDetailBinding binding;
    private RecipeDetailViewModel viewModel;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private Recipe currentRecipe;

    public static Intent newIntent(Context context, String recipeId) {
        Intent intent = new Intent(context, RecipeDetailActivity.class);
        intent.putExtra(EXTRA_RECIPE_ID, recipeId);
        return intent;
    }

    public static Intent newIntentFromSearch(Context context, Recipe recipe) {
        Intent intent = new Intent(context, RecipeDetailActivity.class);
        intent.putExtra(EXTRA_FROM_SEARCH, true);
        intent.putExtra(EXTRA_RECIPE_ID, recipe.getId());
        intent.putExtra(EXTRA_TITLE, recipe.getTitle());
        intent.putExtra(EXTRA_IMAGE_URL, recipe.getImageUrl());
        intent.putExtra(EXTRA_SUMMARY, recipe.getSummary());
        intent.putExtra(EXTRA_INSTRUCTIONS, recipe.getInstructions());
        intent.putExtra(EXTRA_CATEGORY, recipe.getCategory());
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecipeDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(RecipeDetailViewModel.class);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        binding.ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            if (!fromUser || currentRecipe == null) {
                return;
            }
            currentRecipe.setRating(rating);
            persistRating(currentRecipe);
            if (currentRecipe.isFavorite()) {
                persistFavorite(currentRecipe, true);
            }
        });

        Recipe fromSearch = buildRecipeFromIntent();
        if (fromSearch != null) {
            currentRecipe = fromSearch;
            bindRecipe(fromSearch);
            loadRatingFromFirestore(fromSearch.getId());
            loadFavoriteFromFirestore(fromSearch.getId(), true);
            return;
        }

        String recipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);
        if (TextUtils.isEmpty(recipeId)) {
            finish();
            return;
        }
        loadRatingFromFirestore(recipeId);
        loadFavoriteFromFirestore(recipeId, false);
    }

    private Recipe buildRecipeFromIntent() {
        if (!getIntent().getBooleanExtra(EXTRA_FROM_SEARCH, false)) {
            return null;
        }
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (TextUtils.isEmpty(title)) {
            return null;
        }
        String id = getIntent().getStringExtra(EXTRA_RECIPE_ID);
        Recipe recipe = new Recipe();
        recipe.setId(TextUtils.isEmpty(id) ? "search_" + System.currentTimeMillis() : id);
        recipe.setTitle(title);
        recipe.setImageUrl(getIntent().getStringExtra(EXTRA_IMAGE_URL));
        recipe.setSummary(getIntent().getStringExtra(EXTRA_SUMMARY));
        recipe.setInstructions(getIntent().getStringExtra(EXTRA_INSTRUCTIONS));
        recipe.setCategory(getIntent().getStringExtra(EXTRA_CATEGORY));
        recipe.setFavorite(false);
        recipe.setRating(0f);
        return recipe;
    }

    private void loadFavoriteFromFirestore(String recipeId, boolean keepCurrentOnMissing) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (TextUtils.isEmpty(uid) || TextUtils.isEmpty(recipeId)) {
            if (!keepCurrentOnMissing) {
                observeLocalRecipe(recipeId);
            }
            return;
        }
        firestore.collection("users")
                .document(uid)
                .collection("favorites")
                .document(recipeId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Recipe recipe = mapFavoriteRecipe(snapshot);
                        currentRecipe = recipe;
                        bindRecipe(recipe);
                    } else if (!keepCurrentOnMissing) {
                        observeLocalRecipe(recipeId);
                    }
                })
                .addOnFailureListener(error -> {
                    Toast.makeText(this, getString(R.string.favorite_load_failed), Toast.LENGTH_SHORT).show();
                    if (!keepCurrentOnMissing) {
                        observeLocalRecipe(recipeId);
                    }
                });
    }

    private void observeLocalRecipe(String recipeId) {
        if (TextUtils.isEmpty(recipeId)) {
            return;
        }
        viewModel.observeRecipe(recipeId).observe(this, recipe -> {
            if (recipe != null) {
                currentRecipe = recipe;
                bindRecipe(recipe);
                viewModel.refreshDetails(recipe);
            }
        });
    }

    private void bindRecipe(Recipe recipe) {
        binding.title.setText(recipe.getTitle());
        binding.category.setText(TextUtils.isEmpty(recipe.getCategory()) ? "All" : recipe.getCategory());
        binding.summary.setText(TextUtils.isEmpty(recipe.getSummary()) ? "Curated recipe details with a polished preparation flow." : recipe.getSummary());
        binding.ratingBar.setRating(recipe.getRating());
        updateFavoriteButton(recipe.isFavorite());
        binding.favoriteButton.setOnClickListener(v -> {
            boolean nextFavorite = !recipe.isFavorite();
            animateFavorite(v);
            recipe.setFavorite(nextFavorite);
            updateFavoriteButton(nextFavorite);
            persistFavorite(recipe, nextFavorite);
        });

        Glide.with(this)
                .load(recipe.getDisplayImage())
                .placeholder(R.drawable.bg_image_placeholder)
                .into(binding.headerImage);

        String instructions = !TextUtils.isEmpty(recipe.getInstructions()) ? recipe.getInstructions() : recipe.getSteps();
        binding.stepsText.setText(formatBlock(instructions, getString(R.string.instructions_loading)));
    }

    private void updateFavoriteButton(boolean favorite) {
        binding.favoriteButton.setChecked(favorite);
        binding.favoriteButton.setIconResource(favorite ? R.drawable.ic_favorite_24 : R.drawable.ic_favorite_border_24);
        binding.favoriteButton.setText(favorite ? getString(R.string.remove_from_favorites) : getString(R.string.add_to_favorites));
    }

    private void persistRating(Recipe recipe) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (TextUtils.isEmpty(uid) || TextUtils.isEmpty(recipe.getId())) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", recipe.getId());
        payload.put("rating", recipe.getRating());
        payload.put("updatedAt", System.currentTimeMillis());
        firestore.collection("users")
                .document(uid)
                .collection("ratings")
                .document(recipe.getId())
                .set(payload);
    }

    private void loadRatingFromFirestore(String recipeId) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (TextUtils.isEmpty(uid) || TextUtils.isEmpty(recipeId)) {
            return;
        }
        firestore.collection("users")
                .document(uid)
                .collection("ratings")
                .document(recipeId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        return;
                    }
                    float rating = floatValue(snapshot.get("rating"), 0f);
                    if (currentRecipe != null) {
                        currentRecipe.setRating(rating);
                    }
                    binding.ratingBar.setRating(rating);
                });
    }

    private void persistFavorite(Recipe recipe, boolean favorite) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (TextUtils.isEmpty(uid)) {
            Toast.makeText(this, getString(R.string.login_required_favorites), Toast.LENGTH_SHORT).show();
            return;
        }

        String recipeId = recipe.getId();
        if (TextUtils.isEmpty(recipeId)) {
            recipeId = "recipe_" + System.currentTimeMillis();
            recipe.setId(recipeId);
        }

        if (favorite) {
            firestore.collection("users")
                    .document(uid)
                    .collection("favorites")
                    .document(recipeId)
                    .set(buildFavoritePayload(recipe), SetOptions.merge())
                    .addOnFailureListener(error -> Toast.makeText(this, getString(R.string.favorite_save_failed), Toast.LENGTH_SHORT).show());
            return;
        }

        firestore.collection("users")
                .document(uid)
                .collection("favorites")
                .document(recipeId)
                .delete()
                .addOnFailureListener(error -> Toast.makeText(this, getString(R.string.favorite_remove_failed), Toast.LENGTH_SHORT).show());
    }

    private Map<String, Object> buildFavoritePayload(Recipe recipe) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", recipe.getId());
        payload.put("title", recipe.getTitle());
        payload.put("category", recipe.getCategory());
        payload.put("imageUrl", recipe.getImageUrl());
        payload.put("summary", recipe.getSummary());
        payload.put("instructions", recipe.getInstructions());
        payload.put("favorite", true);
        payload.put("rating", recipe.getRating());
        payload.put("updatedAt", System.currentTimeMillis());
        return payload;
    }

    private Recipe mapFavoriteRecipe(DocumentSnapshot doc) {
        Recipe recipe = new Recipe();
        recipe.setId(stringValue(doc.get("id"), doc.getId()));
        recipe.setTitle(stringValue(doc.get("title"), "Untitled Recipe"));
        recipe.setCategory(stringValue(doc.get("category"), "All"));
        recipe.setImageUrl(stringValue(doc.get("imageUrl"), ""));
        recipe.setSummary(stringValue(doc.get("summary"), ""));
        recipe.setInstructions(stringValue(doc.get("instructions"), ""));
        recipe.setFavorite(booleanValue(doc.get("favorite"), true));
        recipe.setRating(floatValue(doc.get("rating"), 0f));
        return recipe;
    }

    private String stringValue(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private boolean booleanValue(Object value, boolean fallback) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return fallback;
    }

    private float floatValue(Object value, float fallback) {
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return fallback;
    }

    private CharSequence formatBlock(String value, String fallback) {
        String text = TextUtils.isEmpty(value) ? fallback : value;
        return HtmlCompat.fromHtml(text.replace("\n", "<br/>").replace("•", "&#8226;"), HtmlCompat.FROM_HTML_MODE_LEGACY);
    }

    private void animateFavorite(View view) {
        view.animate().scaleX(1.18f).scaleY(1.18f).setDuration(120).withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(120).start()).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
