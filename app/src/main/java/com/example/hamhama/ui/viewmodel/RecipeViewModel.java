package com.example.hamhama.ui.viewmodel;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.hamhama.CookBookApp;
import com.example.hamhama.data.model.Recipe;
import com.example.hamhama.data.repository.RecipeRepository;

import java.util.List;

public class RecipeViewModel extends AndroidViewModel {

    private final RecipeRepository repository;
    private final MediatorLiveData<List<Recipe>> homeRecipes = new MediatorLiveData<>();
    private final MediatorLiveData<List<Recipe>> favoriteRecipes = new MediatorLiveData<>();

    private LiveData<List<Recipe>> homeSource;
    private LiveData<List<Recipe>> favoriteSource;
    private String homeQuery = "";
    private String homeCategory = "All";
    private String favoriteQuery = "";
    private String favoriteCategory = "All";

    public RecipeViewModel(@NonNull Application application) {
        super(application);
        repository = ((CookBookApp) application).getRecipeRepository();
        bindHomeSource();
        bindFavoriteSource();
    }

    public LiveData<List<Recipe>> getHomeRecipes() {
        return homeRecipes;
    }

    public LiveData<List<Recipe>> getFavoriteRecipes() {
        return favoriteRecipes;
    }

    public void setHomeQuery(String query) {
        homeQuery = normalize(query);
        bindHomeSource();
    }

    public void setHomeCategory(String category) {
        homeCategory = normalizeCategory(category);
        bindHomeSource();
    }

    public void setFavoriteQuery(String query) {
        favoriteQuery = normalize(query);
        bindFavoriteSource();
    }

    public void setFavoriteCategory(String category) {
        favoriteCategory = normalizeCategory(category);
        bindFavoriteSource();
    }

    public void refreshRemoteRecipes(String query) {
        repository.refreshRemoteRecipes(query);
    }

    public void toggleFavorite(Recipe recipe) {
        repository.toggleFavorite(recipe);
    }

    public void seedInitialData() {
        repository.seedInitialData();
    }

    public String getHomeQuery() {
        return homeQuery;
    }

    public String getHomeCategory() {
        return homeCategory;
    }

    public String getFavoriteQuery() {
        return favoriteQuery;
    }

    public String getFavoriteCategory() {
        return favoriteCategory;
    }

    private void bindHomeSource() {
        if (homeSource != null) {
            homeRecipes.removeSource(homeSource);
        }
        homeSource = repository.observeRecipes(homeQuery, homeCategory);
        homeRecipes.addSource(homeSource, homeRecipes::setValue);
    }

    private void bindFavoriteSource() {
        if (favoriteSource != null) {
            favoriteRecipes.removeSource(favoriteSource);
        }
        favoriteSource = repository.observeFavorites(favoriteQuery, favoriteCategory);
        favoriteRecipes.addSource(favoriteSource, favoriteRecipes::setValue);
    }

    private String normalize(String value) {
        return TextUtils.isEmpty(value) ? "" : value.trim();
    }

    private String normalizeCategory(String value) {
        return TextUtils.isEmpty(value) ? "All" : value.trim();
    }
}