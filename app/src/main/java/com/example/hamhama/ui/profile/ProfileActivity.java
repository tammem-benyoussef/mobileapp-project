package com.example.hamhama.ui.profile;

import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.hamhama.CookBookApp;
import com.example.hamhama.databinding.ActivityProfileBinding;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 31;

    private ActivityProfileBinding binding;
    private boolean editMode;
    private Uri selectedPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        bindProfileData();

        binding.choosePhotoButton.setOnClickListener(v -> {
            setEditMode(true);
            openImagePicker();
        });
        binding.editProfileButton.setOnClickListener(v -> {
            setEditMode(true);
            openImagePicker();
        });
        binding.saveProfileButton.setOnClickListener(v -> saveProfile());
    }

    private void bindProfileData() {
        CookBookApp app = (CookBookApp) getApplication();
        String name = app.getSessionManager().getName();
        String email = app.getSessionManager().getEmail();
        String photoUrl = app.getSessionManager().getPhotoUrl();

        if (selectedPhotoUri != null) {
            photoUrl = selectedPhotoUri.toString();
        } else if (TextUtils.isEmpty(photoUrl) && app.getFirebaseSyncManager().isLoggedIn()) {
            photoUrl = app.getFirebaseSyncManager().getUserPhotoUrl();
        }

        binding.nameInput.setText(name);
        binding.emailInput.setText(email);

        Glide.with(this)
                .load(photoUrl)
                .placeholder(com.example.hamhama.R.drawable.bg_image_placeholder)
                .error(com.example.hamhama.R.drawable.bg_image_placeholder)
                .circleCrop()
                .into(binding.profilePhoto);

        boolean hasPhoto = !TextUtils.isEmpty(photoUrl);
        binding.profilePhoto.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);
        binding.choosePhotoButton.setText(hasPhoto
            ? com.example.hamhama.R.string.edit_photo_button
            : com.example.hamhama.R.string.upload_photo_button);

        setEditMode(false);
    }

    private void setEditMode(boolean enabled) {
        editMode = enabled;
        binding.nameInput.setEnabled(enabled);
        binding.editProfileButton.setEnabled(!enabled);
        binding.saveProfileButton.setEnabled(enabled);
        binding.choosePhotoButton.setEnabled(true);
    }

    private void saveProfile() {
        String name = binding.nameInput.getText() == null ? "" : binding.nameInput.getText().toString().trim();
        String photoUrl = selectedPhotoUri != null ? selectedPhotoUri.toString() : ((CookBookApp) getApplication()).getSessionManager().getPhotoUrl();

        binding.nameInputLayout.setError(null);
        if (TextUtils.isEmpty(name)) {
            binding.nameInputLayout.setError(getString(com.example.hamhama.R.string.error_username_required));
            return;
        }

        CookBookApp app = (CookBookApp) getApplication();
        String email = app.getSessionManager().getEmail();
        app.getSessionManager().updateProfile(name, photoUrl);

        app.getFirebaseSyncManager().updateCurrentUserProfile(name, photoUrl, new com.example.hamhama.data.firebase.FirebaseSyncManager.ProfileCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    bindProfileData();
                    Toast.makeText(ProfileActivity.this, com.example.hamhama.R.string.profile_saved, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    bindProfileData();
                    Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Profile Photo"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedPhotoUri = data.getData();
            final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try {
                getContentResolver().takePersistableUriPermission(selectedPhotoUri, takeFlags);
            } catch (SecurityException ignored) {
                // Some providers do not allow persistable permissions; Glide can still load for this session.
            }
            binding.profilePhoto.setVisibility(View.VISIBLE);
            binding.choosePhotoButton.setText(com.example.hamhama.R.string.edit_photo_button);
            Glide.with(this)
                    .load(selectedPhotoUri)
                    .placeholder(com.example.hamhama.R.drawable.bg_image_placeholder)
                    .error(com.example.hamhama.R.drawable.bg_image_placeholder)
                    .circleCrop()
                    .into(binding.profilePhoto);
            setEditMode(true);
        }
    }
}
