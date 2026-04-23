package com.example.hamhama.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        if (((CookBookApp) getApplication()).getSessionManager().isLoggedIn()) {
            openMain();
            return;
        }

        binding.loginButton.setOnClickListener(v -> viewModel.login(
                TextUtils.isEmpty(binding.emailInput.getText()) ? "" : binding.emailInput.getText().toString(),
                TextUtils.isEmpty(binding.passwordInput.getText()) ? "" : binding.passwordInput.getText().toString()
        ));

        binding.demoButton.setOnClickListener(v -> {
            ((CookBookApp) getApplication()).getSessionManager().saveSession("demo@cookbook.app", "Demo Chef");
            openMain();
        });

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

    private void openMain() {
        ActivityTransition.open(this, new Intent(this, MainActivity.class));
        finish();
    }
}