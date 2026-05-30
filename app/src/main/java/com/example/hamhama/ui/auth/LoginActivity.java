package com.example.hamhama.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.hamhama.CookBookApp;
import com.example.hamhama.databinding.ActivityLoginBinding;
import com.example.hamhama.ui.main.MainActivity;
import com.example.hamhama.ui.util.ActivityTransition;
import com.example.hamhama.ui.viewmodel.AuthViewModel;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;
    private boolean redirectToMain;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        redirectToMain = ((CookBookApp) getApplication()).getSessionManager().isLoggedIn();
        if (redirectToMain) {
            return;
        }

        binding.loginButton.setOnClickListener(v -> attemptLogin());

        binding.createAccountButton.setOnClickListener(v -> ActivityTransition.open(this, new Intent(this, RegisterActivity.class)));

        binding.googleButton.setOnClickListener(v -> Toast.makeText(this, "Google Sign-In can be connected through Firebase Auth.", Toast.LENGTH_SHORT).show());

        viewModel.getLoginSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                openMain();
            }
        });

        viewModel.getError().observe(this, message -> {
            if (!TextUtils.isEmpty(message)) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (redirectToMain) {
            redirectToMain = false;
            binding.getRoot().post(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    openMain();
                }
            });
        }
    }

    private void openMain() {
        ActivityTransition.open(this, new Intent(this, MainActivity.class));
        finish();
    }

    private void attemptLogin() {
        String email = TextUtils.isEmpty(binding.emailInput.getText()) ? "" : binding.emailInput.getText().toString().trim();
        String password = TextUtils.isEmpty(binding.passwordInput.getText()) ? "" : binding.passwordInput.getText().toString().trim();

        binding.emailInputLayout.setError(null);
        binding.passwordInputLayout.setError(null);

        boolean valid = true;
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
        }

        if (!valid) {
            return;
        }

        viewModel.login(email, password);
    }
}