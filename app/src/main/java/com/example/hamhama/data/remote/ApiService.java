package com.example.hamhama.data.remote;

import com.example.hamhama.data.remote.dto.MealResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {

    @GET("search.php")
    Call<MealResponse> searchRecipes(@Query("s") String query);

    @GET("lookup.php")
    Call<MealResponse> getRecipeInformation(@Query("i") long id);

    @GET("random.php")
    Call<MealResponse> getRandomRecipe();

    @GET("filter.php")
    Call<MealResponse> getMealsByCategory(@Query("c") String category);
}