package com.example.hamhama.data.remote.dto;

import java.util.ArrayList;
import java.util.List;

public class RecipeSearchResponse {

    private List<ApiRecipeDto> results = new ArrayList<>();

    public List<ApiRecipeDto> getResults() {
        return results;
    }

    public void setResults(List<ApiRecipeDto> results) {
        this.results = results;
    }
}