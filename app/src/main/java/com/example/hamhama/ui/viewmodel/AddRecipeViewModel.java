package com.example.hamhama.ui.viewmodel;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hamhama.CookBookApp;
import com.example.hamhama.data.model.Recipe;

public class AddRecipeViewModel extends AndroidViewModel {

    private final MutableLiveData<String> title = new MutableLiveData<>("");
    private final MutableLiveData<String> category = new MutableLiveData<>("Breakfast");
    private final MutableLiveData<String> ingredients = new MutableLiveData<>("");
    private final MutableLiveData<String> steps = new MutableLiveData<>("");
    private final MutableLiveData<String> summary = new MutableLiveData<>("");
    private final MutableLiveData<String> imageUri = new MutableLiveData<>("");
    private final MutableLiveData<String> saveResult = new MutableLiveData<>();

    public AddRecipeViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<String> getTitle() {
        return title;
    }

    public LiveData<String> getCategory() {
        return category;
    }

    public LiveData<String> getIngredients() {
        return ingredients;
    }

    public LiveData<String> getSteps() {
        return steps;
    }

    public LiveData<String> getSummary() {
        return summary;
    }

    public LiveData<String> getImageUri() {
        return imageUri;
    }

    public LiveData<String> getSaveResult() {
        return saveResult;
    }

    public void setTitle(String value) {
        title.setValue(value);
    }

    public void setCategory(String value) {
        category.setValue(value);
    }

    public void setIngredients(String value) {
        ingredients.setValue(value);
    }

    public void setSteps(String value) {
        steps.setValue(value);
    }

    public void setSummary(String value) {
        summary.setValue(value);
    }

    public void setImageUri(String value) {
        imageUri.setValue(value);
    }

    public void saveRecipe() {
        if (TextUtils.isEmpty(title.getValue()) || TextUtils.isEmpty(ingredients.getValue()) || TextUtils.isEmpty(steps.getValue())) {
            saveResult.setValue("Please complete the recipe title, ingredients, and steps.");
            return;
        }
        Recipe recipe = new Recipe();
        recipe.setId("local_" + System.currentTimeMillis());
        recipe.setTitle(title.getValue().trim());
        recipe.setCategory(category.getValue());
        recipe.setIngredients(ingredients.getValue().trim());
        recipe.setSteps(steps.getValue().trim());
        recipe.setSummary(summary.getValue());
        recipe.setSource("local");
        recipe.setImageUrl("");
        recipe.setLocalImageUri(imageUri.getValue());
        recipe.setFavorite(false);
        recipe.setCreatedAt(System.currentTimeMillis());
        recipe.setUpdatedAt(System.currentTimeMillis());
        ((CookBookApp) getApplication()).getRecipeRepository().addRecipe(recipe);
        saveResult.setValue("Recipe saved successfully.");
    }
}