package com.example.hamhama.ui.auth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.hamhama.databinding.ActivityRegisterBinding;
import com.example.hamhama.ui.main.MainActivity;
import com.example.hamhama.ui.util.ActivityTransition;
import com.example.hamhama.ui.viewmodel.AuthViewModel;

import java.io.IOException;

public class RegisterActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ActivityRegisterBinding binding;
    private AuthViewModel viewModel;
    private Uri selectedPhotoUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.choosePhotoButton.setOnClickListener(v -> openImagePicker());

        binding.createAccountButton.setOnClickListener(v -> createAccount());

        viewModel.getLoginSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "Account created successfully.", Toast.LENGTH_SHORT).show();
                ActivityTransition.open(this, new Intent(this, MainActivity.class));
                finish();
            }
        });

        viewModel.getError().observe(this, message -> {
            if (!TextUtils.isEmpty(message)) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Photo"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedPhotoUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedPhotoUri);
                binding.photoPreview.setImageBitmap(bitmap);
                binding.photoPreview.setVisibility(View.VISIBLE);
                binding.photoPlaceholder.setVisibility(View.GONE);
            } catch (IOException e) {
                Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void createAccount() {
        String email = binding.emailInput.getText() == null ? "" : binding.emailInput.getText().toString().trim();
        String username = binding.usernameInput.getText() == null ? "" : binding.usernameInput.getText().toString().trim();
        String password = binding.passwordInput.getText() == null ? "" : binding.passwordInput.getText().toString().trim();
        String confirm = binding.confirmPasswordInput.getText() == null ? "" : binding.confirmPasswordInput.getText().toString().trim();

        binding.usernameInputLayout.setError(null);
        binding.emailInputLayout.setError(null);
        binding.passwordInputLayout.setError(null);
        binding.confirmPasswordInputLayout.setError(null);

        boolean valid = true;
        if (TextUtils.isEmpty(username)) {
            binding.usernameInputLayout.setError(getString(com.example.hamhama.R.string.error_username_required));
            valid = false;
        }

        if (TextUtils.isEmpty(email)) {
            binding.emailInputLayout.setError(getString(com.example.hamhama.R.string.error_email_required));
            valid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.setError(getString(com.example.hamhama.R.string.error_email_invalid));
            valid = false;
        }

        if (TextUtils.isEmpty(password)) {
            binding.passwordInputLayout.setError(getString(com.example.hamhama.R.string.error_password_required));
            valid = false;
        } else if (password.length() < 6) {
            binding.passwordInputLayout.setError(getString(com.example.hamhama.R.string.error_password_short));
            valid = false;
        }

        if (!TextUtils.equals(password, confirm)) {
            binding.confirmPasswordInputLayout.setError(getString(com.example.hamhama.R.string.error_password_mismatch));
            valid = false;
        }

        if (!valid) {
            return;
        }

        String photoUrl = selectedPhotoUri != null ? selectedPhotoUri.toString() : "";
        viewModel.createAccount(email, username, password, photoUrl);
    }
}
