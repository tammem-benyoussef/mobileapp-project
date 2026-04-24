package com.example.hamhama.data.repository;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.hamhama.CookBookApp;
import com.example.hamhama.BuildConfig;
import com.example.hamhama.data.firebase.FirebaseSyncManager;
import com.example.hamhama.data.model.Recipe;
import com.example.hamhama.data.remote.ApiService;
import com.example.hamhama.data.remote.RetrofitClient;
import com.example.hamhama.data.remote.dto.ApiRecipeDto;
import com.example.hamhama.data.remote.dto.AnalyzedInstructionDto;
import com.example.hamhama.data.remote.dto.IngredientDto;
import com.example.hamhama.data.remote.dto.RecipeSearchResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecipeRepository {

    private static volatile RecipeRepository instance;

    private final ApiService apiService;
    private final FirebaseSyncManager firebaseSyncManager;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Map<String, Recipe> recipesById = new HashMap<>();
    private final MutableLiveData<List<Recipe>> recipesLiveData = new MutableLiveData<>(new ArrayList<>());

    private RecipeRepository(Context context) {
        apiService = RetrofitClient.getApiService();
        firebaseSyncManager = ((CookBookApp) context.getApplicationContext()).getFirebaseSyncManager();
    }

    public static RecipeRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (RecipeRepository.class) {
                if (instance == null) {
                    instance = new RecipeRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public LiveData<List<Recipe>> observeRecipes(String query, String category) {
        return Transformations.map(recipesLiveData, recipes -> filterRecipes(recipes, normalizeQuery(query), normalizeCategory(category), false));
    }

    public LiveData<List<Recipe>> observeFavorites(String query, String category) {
        return Transformations.map(recipesLiveData, recipes -> filterRecipes(recipes, normalizeQuery(query), normalizeCategory(category), true));
    }

    public LiveData<Recipe> observeRecipeById(String id) {
        return Transformations.map(recipesLiveData, recipes -> {
            if (recipes == null) {
                return null;
            }
            for (Recipe recipe : recipes) {
                if (recipe.getId().equals(id)) {
                    return recipe;
                }
            }
            return null;
        });
    }

    public void seedInitialData() {
        executorService.execute(() -> {
            synchronized (recipesById) {
                if (!recipesById.isEmpty()) {
                    return;
                }
            }
            upsertAll(SampleRecipes.create());
        });
    }

    public void addRecipe(Recipe recipe) {
        executorService.execute(() -> {
            upsertRecipe(recipe);
            firebaseSyncManager.syncRecipe(recipe);
        });
    }

    public void toggleFavorite(Recipe recipe) {
        executorService.execute(() -> {
            Recipe current;
            synchronized (recipesById) {
                current = recipesById.get(recipe.getId());
            }
            Recipe target = current != null ? current : recipe;
            target.setFavorite(!target.isFavorite());
            target.setUpdatedAt(System.currentTimeMillis());
            upsertRecipe(target);
            firebaseSyncManager.syncRecipe(target);
        });
    }

    public void refreshRemoteRecipes(String query) {
        String apiKey = BuildConfig.SPOONACULAR_API_KEY;
        if (TextUtils.isEmpty(apiKey)) {
            return;
        }
        String normalizedQuery = TextUtils.isEmpty(query) ? "chicken" : query.trim();
        apiService.searchRecipes(normalizedQuery, 12, true, apiKey).enqueue(new Callback<RecipeSearchResponse>() {
            @Override
            public void onResponse(Call<RecipeSearchResponse> call, Response<RecipeSearchResponse> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().getResults() == null) {
                    return;
                }
                executorService.execute(() -> {
                    List<Recipe> recipes = new ArrayList<>();
                    for (ApiRecipeDto dto : response.body().getResults()) {
                        Recipe existing;
                        synchronized (recipesById) {
                            existing = recipesById.get(recipeKey(dto.getId()));
                        }
                        recipes.add(mapRemoteRecipe(dto, existing));
                    }
                    upsertAll(recipes);
                });
            }

            @Override
            public void onFailure(Call<RecipeSearchResponse> call, Throwable t) {
            }
        });
    }

    public void refreshRecipeDetails(String recipeId, long remoteId) {
        String apiKey = BuildConfig.SPOONACULAR_API_KEY;
        if (TextUtils.isEmpty(apiKey) || remoteId <= 0) {
            return;
        }
        apiService.getRecipeInformation(remoteId, apiKey).enqueue(new Callback<ApiRecipeDto>() {
            @Override
            public void onResponse(Call<ApiRecipeDto> call, Response<ApiRecipeDto> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }
                executorService.execute(() -> {
                    Recipe existing;
                    synchronized (recipesById) {
                        existing = recipesById.get(recipeId);
                    }
                    upsertRecipe(mapRemoteRecipe(response.body(), existing));
                });
            }

            @Override
            public void onFailure(Call<ApiRecipeDto> call, Throwable t) {
            }
        });
    }

    public void saveRemoteRecipes(List<ApiRecipeDto> recipes) {
        executorService.execute(() -> {
            List<Recipe> mapped = new ArrayList<>();
            for (ApiRecipeDto dto : recipes) {
                Recipe existing;
                synchronized (recipesById) {
                    existing = recipesById.get(recipeKey(dto.getId()));
                }
                mapped.add(mapRemoteRecipe(dto, existing));
            }
            upsertAll(mapped);
        });
    }

    public void syncFromCloud() {
        firebaseSyncManager.fetchUserRecipes(new FirebaseSyncManager.RecipeListCallback() {
            @Override
            public void onSuccess(@NonNull List<Recipe> recipes) {
                executorService.execute(() -> {
                    if (!recipes.isEmpty()) {
                        upsertAll(recipes);
                    }
                });
            }

            @Override
            public void onError(@NonNull String message) {
            }
        });
    }

    private Recipe mapRemoteRecipe(ApiRecipeDto dto, Recipe existing) {
        Recipe recipe = existing != null ? existing : new Recipe();
        recipe.setId(recipeKey(dto.getId()));
        recipe.setRemoteId(dto.getId());
        recipe.setTitle(dto.getTitle());
        recipe.setCategory("API");
        recipe.setImageUrl(dto.getImage());
        recipe.setSummary(dto.getSummary());
        recipe.setIngredients(joinIngredients(dto.getExtendedIngredients()));
        recipe.setSteps(joinSteps(dto.getInstructions(), dto.getAnalyzedInstructions()));
        recipe.setSource("remote");
        recipe.setFavorite(existing != null && existing.isFavorite());
        recipe.setCreatedAt(existing != null ? existing.getCreatedAt() : System.currentTimeMillis());
        recipe.setUpdatedAt(System.currentTimeMillis());
        return recipe;
    }

    private String joinIngredients(List<IngredientDto> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return "Loaded from API";
        }
        StringBuilder builder = new StringBuilder();
        for (IngredientDto ingredient : ingredients) {
            String text = ingredient.getOriginal();
            if (TextUtils.isEmpty(text)) {
                text = ingredient.getName();
            }
            if (!TextUtils.isEmpty(text)) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(text);
            }
        }
        return builder.toString();
    }

    private String joinSteps(String instructions, List<AnalyzedInstructionDto> analyzedInstructions) {
        if (!TextUtils.isEmpty(instructions)) {
            return stripHtml(instructions);
        }
        if (analyzedInstructions != null && !analyzedInstructions.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (AnalyzedInstructionDto instruction : analyzedInstructions) {
                if (instruction.getSteps() == null) {
                    continue;
                }
                for (int i = 0; i < instruction.getSteps().size(); i++) {
                    String step = instruction.getSteps().get(i).getStep();
                    if (!TextUtils.isEmpty(step)) {
                        if (builder.length() > 0) {
                            builder.append('\n');
                        }
                        builder.append(i + 1).append(". ").append(step);
                    }
                }
            }
            if (builder.length() > 0) {
                return builder.toString();
            }
        }
        return "Open the recipe in the app to explore the steps.";
    }

    private String stripHtml(String text) {
        return text.replaceAll("<[^>]+>", "").trim();
    }

    private List<Recipe> filterRecipes(List<Recipe> recipes, String query, String category, boolean favoritesOnly) {
        if (recipes == null || recipes.isEmpty()) {
            return Collections.emptyList();
        }
        String normalizedQuery = normalizeQuery(query).toLowerCase(Locale.US);
        String normalizedCategory = normalizeCategory(category);
        List<Recipe> filtered = new ArrayList<>();
        for (Recipe recipe : recipes) {
            if (favoritesOnly && !recipe.isFavorite()) {
                continue;
            }
            if (!matchesCategory(recipe, normalizedCategory)) {
                continue;
            }
            if (!matchesQuery(recipe, normalizedQuery)) {
                continue;
            }
            filtered.add(recipe);
        }
        filtered.sort(Comparator
            .comparing(Recipe::isFavorite)
            .reversed()
            .thenComparing(Recipe::getUpdatedAt, Comparator.reverseOrder()));
        return filtered;
    }

    private boolean matchesCategory(Recipe recipe, String category) {
        if (TextUtils.isEmpty(category) || "All".equalsIgnoreCase(category)) {
            return true;
        }
        return category.equalsIgnoreCase(recipe.getCategory());
    }

    private boolean matchesQuery(Recipe recipe, String query) {
        if (TextUtils.isEmpty(query)) {
            return true;
        }
        return containsIgnoreCase(recipe.getTitle(), query)
                || containsIgnoreCase(recipe.getCategory(), query)
                || containsIgnoreCase(recipe.getIngredients(), query)
                || containsIgnoreCase(recipe.getSummary(), query);
    }

    private boolean containsIgnoreCase(String value, String query) {
        return !TextUtils.isEmpty(value) && value.toLowerCase(Locale.US).contains(query);
    }

    private void upsertAll(List<Recipe> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return;
        }
        synchronized (recipesById) {
            for (Recipe recipe : recipes) {
                if (recipe != null && !TextUtils.isEmpty(recipe.getId())) {
                    recipesById.put(recipe.getId(), recipe);
                }
            }
        }
        publishRecipes();
    }

    private void upsertRecipe(Recipe recipe) {
        if (recipe == null || TextUtils.isEmpty(recipe.getId())) {
            return;
        }
        synchronized (recipesById) {
            recipesById.put(recipe.getId(), recipe);
        }
        publishRecipes();
    }

    private void publishRecipes() {
        List<Recipe> snapshot;
        synchronized (recipesById) {
            snapshot = new ArrayList<>(recipesById.values());
        }
        snapshot.sort(Comparator
            .comparing(Recipe::isFavorite)
            .reversed()
            .thenComparing(Recipe::getUpdatedAt, Comparator.reverseOrder()));
        recipesLiveData.postValue(snapshot);
    }

    private String normalizeQuery(String query) {
        return TextUtils.isEmpty(query) ? "" : query.trim();
    }

    private String normalizeCategory(String category) {
        return TextUtils.isEmpty(category) ? "All" : category.trim();
    }

    private String recipeKey(long remoteId) {
        return String.format(Locale.US, "remote_%d", remoteId);
    }
}