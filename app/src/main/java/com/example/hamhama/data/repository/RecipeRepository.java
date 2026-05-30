package com.example.hamhama.data.repository;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.hamhama.CookBookApp;
import com.example.hamhama.data.firebase.FirebaseSyncManager;
import com.example.hamhama.data.model.Recipe;
import com.example.hamhama.data.remote.ApiService;
import com.example.hamhama.data.remote.RetrofitClient;
import com.example.hamhama.data.remote.dto.MealDto;
import com.example.hamhama.data.remote.dto.MealResponse;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecipeRepository {

    private static final String TAG = "RecipeRepository";

    private static volatile RecipeRepository instance;

    private final ApiService apiService;
    private final FirebaseSyncManager firebaseSyncManager;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Map<String, Recipe> recipesById = new HashMap<>();
    private final MutableLiveData<List<Recipe>> recipesLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Recipe>> favoritesLiveData = new MutableLiveData<>(new ArrayList<>());

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
        return Transformations.map(favoritesLiveData, recipes -> filterRecipes(recipes, normalizeQuery(query), normalizeCategory(category), true));
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
        // Intentionally no-op: app home feed is API-first (TheMealDB), with cloud favorites in Firestore.
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
            Recipe target = current != null ? current.copy() : recipe.copy();
            target.setFavorite(!target.isFavorite());
            target.setUpdatedAt(System.currentTimeMillis());
            upsertRecipe(target);
            if (target.isFavorite()) {
                firebaseSyncManager.saveFavoriteRecipe(target);
            } else {
                firebaseSyncManager.removeFavoriteRecipe(target.getId());
            }
            refreshFavoritesFromCloud();
        });
    }

    public void refreshFavoritesFromCloud() {
        firebaseSyncManager.fetchFavoriteRecipes(new FirebaseSyncManager.RecipeListCallback() {
            @Override
            public void onSuccess(@NonNull List<Recipe> recipes) {
                favoritesLiveData.postValue(recipes);
            }

            @Override
            public void onError(@NonNull String message) {
                favoritesLiveData.postValue(Collections.emptyList());
            }
        });
    }

    public void refreshRemoteRecipes(String query, String category) {
        String normalizedQuery = normalizeQuery(query);
        String normalizedCategory = normalizeCategory(category);
        if (!TextUtils.isEmpty(normalizedQuery)) {
            fetchMealsBySearch(normalizedQuery, null);
            return;
        }
        if ("Dessert".equalsIgnoreCase(normalizedCategory) || "Breakfast".equalsIgnoreCase(normalizedCategory)) {
            fetchMealsByCategory(normalizedCategory);
            return;
        }
        if ("Lunch".equalsIgnoreCase(normalizedCategory) || "Dinner".equalsIgnoreCase(normalizedCategory)) {
            fetchRandomMeals(12, normalizedCategory);
            return;
        }
        fetchRandomMeals(10, null);
    }

    private void fetchMealsByCategory(String category) {
        apiService.getMealsByCategory(category).enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(Call<MealResponse> call, Response<MealResponse> response) {
                MealResponse body = response.body();
                if (!response.isSuccessful() || body == null || body.getMeals() == null || body.getMeals().isEmpty()) {
                    fetchRandomMeals(10, category);
                    return;
                }
                executorService.execute(() -> {
                    List<Recipe> recipes = new ArrayList<>();
                    for (MealDto dto : body.getMeals()) {
                        Recipe existing;
                        synchronized (recipesById) {
                            existing = recipesById.get(recipeKey(dto.getIdMealAsLong()));
                        }
                        recipes.add(mapRemoteRecipe(dto, existing, category));
                    }
                    replaceRemoteRecipes(recipes);
                });
            }

            @Override
            public void onFailure(Call<MealResponse> call, Throwable t) {
                fetchRandomMeals(10, category);
            }
        });
    }

    private void fetchMealsBySearch(String query, String categoryOverride) {
        apiService.searchRecipes(query).enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(Call<MealResponse> call, Response<MealResponse> response) {
                MealResponse body = response.body();
                Log.d(TAG, "TheMealDB search response: success=" + response.isSuccessful()
                        + ", code=" + response.code()
                        + ", query=" + query
                        + ", body=" + (body == null ? "null" : new Gson().toJson(body)));
                if (!response.isSuccessful() || body == null || body.getMeals() == null) {
                    return;
                }
                executorService.execute(() -> {
                    List<Recipe> recipes = new ArrayList<>();
                    for (MealDto dto : body.getMeals()) {
                        Recipe existing;
                        synchronized (recipesById) {
                            existing = recipesById.get(recipeKey(dto.getIdMealAsLong()));
                        }
                        recipes.add(mapRemoteRecipe(dto, existing, categoryOverride));
                    }
                    replaceRemoteRecipes(recipes);
                });
            }

            @Override
            public void onFailure(Call<MealResponse> call, Throwable t) {
                Log.e(TAG, "TheMealDB search request failed for query=" + query, t);
            }
        });
    }

    private void fetchRandomMeals(int count, String categoryOverride) {
        List<Recipe> collectedRecipes = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger remaining = new AtomicInteger(count);
        for (int i = 0; i < count; i++) {
            apiService.getRandomRecipe().enqueue(new Callback<MealResponse>() {
                @Override
                public void onResponse(Call<MealResponse> call, Response<MealResponse> response) {
                    MealResponse body = response.body();
                    if (response.isSuccessful() && body != null && body.getMeals() != null && !body.getMeals().isEmpty()) {
                        MealDto dto = body.getMeals().get(0);
                        Recipe existing;
                        synchronized (recipesById) {
                            existing = recipesById.get(recipeKey(dto.getIdMealAsLong()));
                        }
                        collectedRecipes.add(mapRemoteRecipe(dto, existing, categoryOverride));
                    }
                    if (remaining.decrementAndGet() == 0 && !collectedRecipes.isEmpty()) {
                        executorService.execute(() -> replaceRemoteRecipes(new ArrayList<>(collectedRecipes)));
                    }
                }

                @Override
                public void onFailure(Call<MealResponse> call, Throwable t) {
                    if (remaining.decrementAndGet() == 0 && !collectedRecipes.isEmpty()) {
                        executorService.execute(() -> replaceRemoteRecipes(new ArrayList<>(collectedRecipes)));
                    }
                }
            });
        }
    }

    public void refreshRecipeDetails(String recipeId, long remoteId) {
        if (remoteId <= 0) {
            return;
        }
        apiService.getRecipeInformation(remoteId).enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(Call<MealResponse> call, Response<MealResponse> response) {
                MealResponse body = response.body();
                if (!response.isSuccessful() || body == null || body.getMeals() == null || body.getMeals().isEmpty()) {
                    return;
                }
                executorService.execute(() -> {
                    Recipe existing;
                    synchronized (recipesById) {
                        existing = recipesById.get(recipeId);
                    }
                    upsertRecipe(mapRemoteRecipe(body.getMeals().get(0), existing, null));
                });
            }

            @Override
            public void onFailure(Call<MealResponse> call, Throwable t) {
            }
        });
    }

    public void saveRemoteRecipes(List<MealDto> recipes) {
        executorService.execute(() -> {
            List<Recipe> mapped = new ArrayList<>();
            for (MealDto dto : recipes) {
                Recipe existing;
                synchronized (recipesById) {
                    existing = recipesById.get(recipeKey(dto.getIdMealAsLong()));
                }
                mapped.add(mapRemoteRecipe(dto, existing, null));
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

    private Recipe mapRemoteRecipe(MealDto dto, Recipe existing, String categoryOverride) {
        Recipe recipe = existing != null ? existing : new Recipe();
        recipe.setId(recipeKey(dto.getIdMealAsLong()));
        recipe.setRemoteId(dto.getIdMealAsLong());
        recipe.setTitle(dto.getStrMeal());
        recipe.setCategory(!TextUtils.isEmpty(categoryOverride) ? categoryOverride : inferCategory(dto.getStrCategory(), dto.getStrArea()));
        recipe.setImageUrl(dto.getStrMealThumb());
        recipe.setLocalImageUri("");
        recipe.setSummary(buildSummary(dto));
        recipe.setInstructions(joinSteps(dto.getStrInstructions()));
        recipe.setIngredients(joinIngredients(dto));
        recipe.setSteps(joinSteps(dto.getStrInstructions()));
        recipe.setRating(existing != null && existing.getRating() > 0f ? existing.getRating() : 3.5f);
        recipe.setSource("remote");
        recipe.setFavorite(existing != null && existing.isFavorite());
        recipe.setCreatedAt(existing != null ? existing.getCreatedAt() : System.currentTimeMillis());
        recipe.setUpdatedAt(System.currentTimeMillis());
        return recipe;
    }

    private String joinIngredients(MealDto meal) {
        List<String> ingredients = meal.getIngredients();
        if (ingredients.isEmpty()) {
            return "Loaded from API";
        }
        StringBuilder builder = new StringBuilder();
        for (String ingredient : ingredients) {
            if (!TextUtils.isEmpty(ingredient)) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(ingredient);
            }
        }
        return builder.toString();
    }

    private String joinSteps(String instructions) {
        if (!TextUtils.isEmpty(instructions)) {
            return stripHtml(instructions);
        }
        return "Open the recipe in the app to explore the steps.";
    }

    private String stripHtml(String text) {
        return text.replaceAll("<[^>]+>", "").trim();
    }

    private String inferCategory(String category, String area) {
        if (TextUtils.isEmpty(category) && TextUtils.isEmpty(area)) {
            return "All";
        }
        if (!TextUtils.isEmpty(category)) {
            return category.trim();
        }
        return area.trim();
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
            if (!matchesQuery(recipe, normalizedQuery, favoritesOnly)) {
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

    private boolean matchesQuery(Recipe recipe, String query, boolean favoritesOnly) {
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

    private void replaceRemoteRecipes(List<Recipe> remoteRecipes) {
        synchronized (recipesById) {
            List<String> keysToRemove = new ArrayList<>();
            for (Map.Entry<String, Recipe> entry : recipesById.entrySet()) {
                Recipe value = entry.getValue();
                if (value != null && "remote".equalsIgnoreCase(value.getSource())) {
                    keysToRemove.add(entry.getKey());
                }
            }
            for (String key : keysToRemove) {
                recipesById.remove(key);
            }
            if (remoteRecipes != null) {
                for (Recipe recipe : remoteRecipes) {
                    if (recipe != null && !TextUtils.isEmpty(recipe.getId())) {
                        recipesById.put(recipe.getId(), recipe);
                    }
                }
            }
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

    public void persistRating(Recipe recipe) {
        executorService.execute(() -> {
            Recipe current;
            synchronized (recipesById) {
                current = recipesById.get(recipe.getId());
            }
            if (current != null) {
                Recipe updated = current.copy();
                updated.setRating(recipe.getRating());
                updated.setUpdatedAt(System.currentTimeMillis());
                upsertRecipe(updated);
            }
            firebaseSyncManager.persistRating(recipe);
        });
    }

    private String buildSummary(MealDto meal) {
        StringBuilder builder = new StringBuilder();
        if (!TextUtils.isEmpty(meal.getStrCategory())) {
            builder.append(meal.getStrCategory().trim());
        }
        if (!TextUtils.isEmpty(meal.getStrArea())) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(meal.getStrArea().trim());
        }
        if (!TextUtils.isEmpty(meal.getStrTags())) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(meal.getStrTags().trim());
        }
        if (builder.length() > 0) {
            return builder.toString();
        }
        return "Curated recipe details from TheMealDB.";
    }
}