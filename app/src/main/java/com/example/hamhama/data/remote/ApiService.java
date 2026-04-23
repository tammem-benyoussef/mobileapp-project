package com.example.hamhama.data.remote;

import com.example.hamhama.data.remote.dto.ApiRecipeDto;
import com.example.hamhama.data.remote.dto.RecipeSearchResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @GET("recipes/complexSearch")
    Call<RecipeSearchResponse> searchRecipes(
            @Query("query") String query,
            @Query("number") int number,
            @Query("addRecipeInformation") boolean addRecipeInformation,
            @Query("apiKey") String apiKey
    );

    @GET("recipes/{id}/information")
    Call<ApiRecipeDto> getRecipeInformation(
            @Path("id") long id,
            @Query("apiKey") String apiKey
    );
}