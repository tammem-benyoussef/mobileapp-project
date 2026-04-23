package com.example.hamhama.ui.viewmodel;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hamhama.CookBookApp;
import com.google.firebase.auth.FirebaseUser;

public class AuthViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Boolean> getLoginSuccess() {
        return loginSuccess;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void login(String email, String password) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            error.setValue("Enter your email and password.");
            return;
        }
        CookBookApp app = (CookBookApp) getApplication();
        app.getFirebaseSyncManager().signIn(email.trim(), password.trim(), new com.example.hamhama.data.firebase.FirebaseSyncManager.AuthCallback() {
            @Override
            public void onSuccess(@NonNull FirebaseUser user) {
                app.getSessionManager().saveSession(
                        user.getEmail() == null ? email.trim() : user.getEmail(),
                        app.getFirebaseSyncManager().getUserLabel()
                );
                app.getRecipeRepository().syncFromCloud();
                loginSuccess.postValue(true);
            }

            @Override
            public void onError(@NonNull String message) {
                if (!app.getFirebaseSyncManager().isAvailable()) {
                    app.getSessionManager().saveSession(email.trim(), "CookBook User");
                    loginSuccess.postValue(true);
                    error.postValue("Firebase is not configured. Signed in using local session mode.");
                } else {
                    error.postValue(message);
                }
            }
        });
    }
}