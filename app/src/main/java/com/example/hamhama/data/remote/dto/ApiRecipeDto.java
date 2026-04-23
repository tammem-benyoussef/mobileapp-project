package com.example.hamhama.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ApiRecipeDto {

    private long id;
    private String title;
    private String image;
    private String summary;
    private String instructions;

    @SerializedName("readyInMinutes")
    private int readyInMinutes;

    @SerializedName("sourceUrl")
    private String sourceUrl;

    @SerializedName("extendedIngredients")
    private List<IngredientDto> extendedIngredients = new ArrayList<>();

    @SerializedName("analyzedInstructions")
    private List<AnalyzedInstructionDto> analyzedInstructions = new ArrayList<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public int getReadyInMinutes() {
        return readyInMinutes;
    }

    public void setReadyInMinutes(int readyInMinutes) {
        this.readyInMinutes = readyInMinutes;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public List<IngredientDto> getExtendedIngredients() {
        return extendedIngredients;
    }

    public void setExtendedIngredients(List<IngredientDto> extendedIngredients) {
        this.extendedIngredients = extendedIngredients;
    }

    public List<AnalyzedInstructionDto> getAnalyzedInstructions() {
        return analyzedInstructions;
    }

    public void setAnalyzedInstructions(List<AnalyzedInstructionDto> analyzedInstructions) {
        this.analyzedInstructions = analyzedInstructions;
    }
}