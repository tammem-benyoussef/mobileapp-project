package com.example.hamhama.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class IngredientDto {

    @SerializedName("original")
    private String original;

    @SerializedName("name")
    private String name;

    public String getOriginal() {
        return original;
    }

    public void setOriginal(String original) {
        this.original = original;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}