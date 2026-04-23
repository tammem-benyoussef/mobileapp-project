package com.example.hamhama.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.hamhama.R;
import com.example.hamhama.data.model.Recipe;
import com.example.hamhama.databinding.ActivityRecipeDetailBinding;
import com.example.hamhama.ui.util.ActivityTransition;
import com.example.hamhama.ui.viewmodel.RecipeDetailViewModel;

public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";

    private ActivityRecipeDetailBinding binding;
    private RecipeDetailViewModel viewModel;

    public static Intent newIntent(Context context, String recipeId) {
        Intent intent = new Intent(context, RecipeDetailActivity.class);
        intent.putExtra(EXTRA_RECIPE_ID, recipeId);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecipeDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(RecipeDetailViewModel.class);
        String recipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);
        if (TextUtils.isEmpty(recipeId)) {
            finish();
            return;
        }

        viewModel.observeRecipe(recipeId).observe(this, recipe -> {
            if (recipe != null) {
                bindRecipe(recipe);
                viewModel.refreshDetails(recipe);
            }
        });
    }

    private void bindRecipe(Recipe recipe) {
        binding.title.setText(recipe.getTitle());
        binding.category.setText(recipe.getCategory());
        binding.summary.setText(TextUtils.isEmpty(recipe.getSummary()) ? "Curated recipe details with a polished preparation flow." : recipe.getSummary());
        binding.favoriteButton.setChecked(recipe.isFavorite());
        binding.favoriteButton.setIconResource(recipe.isFavorite() ? R.drawable.ic_favorite_24 : R.drawable.ic_favorite_border_24);
        binding.favoriteButton.setOnClickListener(v -> {
            boolean nextFavorite = !recipe.isFavorite();
            animateFavorite(v);
            viewModel.toggleFavorite(recipe);
            recipe.setFavorite(nextFavorite);
            binding.favoriteButton.setChecked(nextFavorite);
            binding.favoriteButton.setIconResource(nextFavorite ? R.drawable.ic_favorite_24 : R.drawable.ic_favorite_border_24);
        });
        Glide.with(this)
                .load(recipe.getDisplayImage())
                .placeholder(R.drawable.bg_image_placeholder)
                .into(binding.headerImage);

        binding.ingredientsText.setText(formatBlock(recipe.getIngredients(), "Ingredients are loading."));
        binding.stepsText.setText(formatBlock(recipe.getSteps(), "Steps are loading."));
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