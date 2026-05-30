package com.example.hamhama.ui.viewmodel;

import android.app.Application;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hamhama.CookBookApp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

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
                        app.getFirebaseSyncManager().getUserLabel(),
                        user.getPhotoUrl() == null ? "" : user.getPhotoUrl().toString()
                );
                app.getRecipeRepository().syncFromCloud();
                loginSuccess.postValue(true);
            }

            @Override
            public void onError(@NonNull String message) {
                if (!app.getFirebaseSyncManager().isAvailable()) {
                    app.getSessionManager().saveSession(email.trim(), "CookBook User", "");
                    loginSuccess.postValue(true);
                    error.postValue("Firebase is not configured. Signed in using local session mode.");
                } else {
                    error.postValue(message);
                }
            }
        });
    }

    public void createAccount(String email, String username, String password, String photoUrl) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            error.setValue("Enter your email, username, and password.");
            return;
        }
        if (password.trim().length() < 6) {
            error.setValue("Password must be at least 6 characters.");
            return;
        }
        CookBookApp app = (CookBookApp) getApplication();
        app.getFirebaseSyncManager().createAccount(email.trim(), password.trim(), new com.example.hamhama.data.firebase.FirebaseSyncManager.AuthCallback() {
            @Override
            public void onSuccess(@NonNull FirebaseUser user) {
                UserProfileChangeRequest.Builder profileBuilder = new UserProfileChangeRequest.Builder()
                        .setDisplayName(username.trim());
                if (!TextUtils.isEmpty(photoUrl)) {
                    profileBuilder.setPhotoUri(Uri.parse(photoUrl.trim()));
                }
                UserProfileChangeRequest profileUpdates = profileBuilder.build();
                user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                    app.getSessionManager().saveSession(
                            user.getEmail() == null ? email.trim() : user.getEmail(),
                            username.trim(),
                            !TextUtils.isEmpty(photoUrl) ? photoUrl.trim() : ""
                    );
                    app.getRecipeRepository().syncFromCloud();
                    loginSuccess.postValue(true);
                });
            }

            @Override
            public void onError(@NonNull String message) {
                error.postValue(message);
            }
        });
    }
}