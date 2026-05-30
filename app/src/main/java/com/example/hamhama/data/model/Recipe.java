package com.example.hamhama.data.model;

import androidx.annotation.NonNull;

public class Recipe {

    @NonNull
    private String id = "";
    private String title;
    private String imageUrl;
    private String summary;
    private String instructions;
    private String category;
    private boolean isFavorite;
    private float rating;

    // Legacy fields kept to preserve current app and Firestore sync compatibility.
    private long remoteId;
    private String ingredients;
    private String steps;
    private String source;
    private String localImageUri;
    private long createdAt;
    private long updatedAt;

    public Recipe() {
    }

    public Recipe(
            @NonNull String id,
            String title,
            String imageUrl,
            String summary,
            String instructions,
            String category,
            boolean isFavorite,
            float rating
    ) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.summary = summary;
        this.instructions = instructions;
        this.category = category;
        this.isFavorite = isFavorite;
        this.rating = rating;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public long getRemoteId() {
        return remoteId;
    }

    public void setRemoteId(long remoteId) {
        this.remoteId = remoteId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
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

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getLocalImageUri() {
        return localImageUri;
    }

    public void setLocalImageUri(String localImageUri) {
        this.localImageUri = localImageUri;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDisplayImage() {
        return imageUrl;
    }

    public Recipe copy() {
        Recipe copy = new Recipe();
        copy.id = this.id;
        copy.title = this.title;
        copy.imageUrl = this.imageUrl;
        copy.summary = this.summary;
        copy.instructions = this.instructions;
        copy.category = this.category;
        copy.isFavorite = this.isFavorite;
        copy.rating = this.rating;
        copy.remoteId = this.remoteId;
        copy.ingredients = this.ingredients;
        copy.steps = this.steps;
        copy.source = this.source;
        copy.localImageUri = this.localImageUri;
        copy.createdAt = this.createdAt;
        copy.updatedAt = this.updatedAt;
        return copy;
    }
}