package com.example.hamhama.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hamhama.R;
import com.example.hamhama.data.model.Recipe;
import com.example.hamhama.databinding.ItemRecipeCardBinding;

public class RecipeAdapter extends ListAdapter<Recipe, RecipeAdapter.RecipeViewHolder> {

    public interface Listener {
        void onRecipeClicked(Recipe recipe);

        void onFavoriteClicked(Recipe recipe);
    }

    private static final DiffUtil.ItemCallback<Recipe> DIFF_CALLBACK = new DiffUtil.ItemCallback<Recipe>() {
        @Override
        public boolean areItemsTheSame(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
            return oldItem.getTitle().equals(newItem.getTitle())
                    && TextUtils.equals(oldItem.getCategory(), newItem.getCategory())
                    && TextUtils.equals(oldItem.getSummary(), newItem.getSummary())
                    && oldItem.isFavorite() == newItem.isFavorite()
                    && oldItem.getUpdatedAt() == newItem.getUpdatedAt();
        }
    };

    private final Listener listener;

    public RecipeAdapter(Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRecipeCardBinding binding = ItemRecipeCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new RecipeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = getItem(position);
        holder.bind(recipe, listener);
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {

        private final ItemRecipeCardBinding binding;

        RecipeViewHolder(ItemRecipeCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Recipe recipe, Listener listener) {
            binding.recipeTitle.setText(recipe.getTitle());
            binding.recipeCategory.setText(recipe.getCategory());
            binding.recipeSummary.setText(getPreviewText(recipe));
            binding.favoriteIcon.setChecked(recipe.isFavorite());
            binding.favoriteIcon.setIconResource(recipe.isFavorite() ? R.drawable.ic_favorite_24 : R.drawable.ic_favorite_border_24);
            View.OnClickListener openDetail = v -> listener.onRecipeClicked(recipe);
            binding.getRoot().setOnClickListener(openDetail);
            binding.recipeImage.setOnClickListener(openDetail);
            binding.recipeTitle.setOnClickListener(openDetail);
            binding.recipeSummary.setOnClickListener(openDetail);
            binding.recipeCategory.setOnClickListener(openDetail);
            binding.favoriteIcon.setOnClickListener(v -> {
                v.animate().scaleX(1.18f).scaleY(1.18f).setDuration(120).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()).start();
                listener.onFavoriteClicked(recipe);
            });
            Glide.with(binding.recipeImage)
                    .load(recipe.getDisplayImage())
                    .placeholder(R.drawable.bg_image_placeholder)
                    .centerCrop()
                    .into(binding.recipeImage);
        }

        private String getPreviewText(Recipe recipe) {
            if (!TextUtils.isEmpty(recipe.getSummary())) {
                return recipe.getSummary().replaceAll("<[^>]+>", "").trim();
            }
            if (!TextUtils.isEmpty(recipe.getIngredients())) {
                String[] lines = recipe.getIngredients().split("\\n");
                if (lines.length > 0 && !TextUtils.isEmpty(lines[0])) {
                    return lines[0];
                }
            }
            return binding.getRoot().getContext().getString(R.string.recipe_preview_fallback);
        }
    }
}