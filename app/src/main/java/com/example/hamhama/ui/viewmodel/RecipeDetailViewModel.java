package com.example.hamhama.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.hamhama.CookBookApp;
import com.example.hamhama.data.model.Recipe;
import com.example.hamhama.data.repository.RecipeRepository;

public class RecipeDetailViewModel extends AndroidViewModel {

    private final RecipeRepository repository;

    public RecipeDetailViewModel(@NonNull Application application) {
        super(application);
        repository = ((CookBookApp) application).getRecipeRepository();
    }

    public LiveData<Recipe> observeRecipe(String id) {
        return repository.observeRecipeById(id);
    }

    public void refreshDetails(Recipe recipe) {
        if (recipe != null && recipe.getRemoteId() > 0) {
            repository.refreshRecipeDetails(recipe.getId(), recipe.getRemoteId());
        }
    }

    public void toggleFavorite(Recipe recipe) {
        repository.toggleFavorite(recipe);
    }
}