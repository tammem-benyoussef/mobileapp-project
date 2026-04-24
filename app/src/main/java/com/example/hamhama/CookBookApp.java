package com.example.hamhama;

import android.app.Application;

import com.example.hamhama.data.firebase.FirebaseSyncManager;
import com.example.hamhama.data.repository.RecipeRepository;
import com.example.hamhama.ui.util.SessionManager;

public class CookBookApp extends Application {

    private RecipeRepository recipeRepository;
    private SessionManager sessionManager;
    private FirebaseSyncManager firebaseSyncManager;

    @Override
    public void onCreate() {
        super.onCreate();
        firebaseSyncManager = new FirebaseSyncManager(this);
        sessionManager = new SessionManager(this);
        recipeRepository = RecipeRepository.getInstance(this);
        recipeRepository.seedInitialData();
    }

    public RecipeRepository getRecipeRepository() {
        return recipeRepository;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public FirebaseSyncManager getFirebaseSyncManager() {
        return firebaseSyncManager;
    }
}