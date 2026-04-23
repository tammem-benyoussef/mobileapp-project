package com.example.hamhama.ui.add;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.hamhama.R;
import com.example.hamhama.databinding.ActivityAddRecipeBinding;
import com.example.hamhama.ui.util.ActivityTransition;
import com.example.hamhama.ui.viewmodel.AddRecipeViewModel;

public class AddRecipeActivity extends AppCompatActivity {

    private ActivityAddRecipeBinding binding;
    private AddRecipeViewModel viewModel;
    private ActivityResultLauncher<String> imagePicker;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddRecipeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(AddRecipeViewModel.class);
        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                viewModel.setImageUri(uri.toString());
                renderPreview(uri);
            }
        });

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new String[]{"Breakfast", "Lunch", "Dinner", "Dessert", "Vegan", "Quick"});
        binding.categoryInput.setAdapter(categoryAdapter);

        binding.pickImageButton.setOnClickListener(v -> imagePicker.launch("image/*"));
        binding.saveButton.setOnClickListener(v -> {
            syncInputsToViewModel();
            viewModel.saveRecipe();
        });

        viewModel.getSaveResult().observe(this, message -> {
            if (!TextUtils.isEmpty(message)) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                if (message.toLowerCase().contains("success")) {
                    ActivityTransition.finish(this);
                }
            }
        });

        bindViewModel();
    }

    private void bindViewModel() {
        viewModel.getTitle().observe(this, value -> {
            if (!TextUtils.equals(binding.titleInput.getText(), value)) {
                binding.titleInput.setText(value);
            }
        });
        viewModel.getCategory().observe(this, value -> {
            if (!TextUtils.equals(binding.categoryInput.getText(), value)) {
                binding.categoryInput.setText(value, false);
            }
        });
        viewModel.getIngredients().observe(this, value -> {
            if (!TextUtils.equals(binding.ingredientsInput.getText(), value)) {
                binding.ingredientsInput.setText(value);
            }
        });
        viewModel.getSteps().observe(this, value -> {
            if (!TextUtils.equals(binding.stepsInput.getText(), value)) {
                binding.stepsInput.setText(value);
            }
        });
        viewModel.getSummary().observe(this, value -> {
            if (!TextUtils.equals(binding.summaryInput.getText(), value)) {
                binding.summaryInput.setText(value);
            }
        });
        viewModel.getImageUri().observe(this, value -> {
            if (!TextUtils.isEmpty(value)) {
                renderPreview(Uri.parse(value));
            }
        });
    }

    private void syncInputsToViewModel() {
        viewModel.setTitle(binding.titleInput.getText() == null ? "" : binding.titleInput.getText().toString());
        viewModel.setCategory(binding.categoryInput.getText() == null ? "Breakfast" : binding.categoryInput.getText().toString());
        viewModel.setIngredients(binding.ingredientsInput.getText() == null ? "" : binding.ingredientsInput.getText().toString());
        viewModel.setSteps(binding.stepsInput.getText() == null ? "" : binding.stepsInput.getText().toString());
        viewModel.setSummary(binding.summaryInput.getText() == null ? "" : binding.summaryInput.getText().toString());
    }

    private void renderPreview(Uri uri) {
        binding.imagePreviewCard.setVisibility(android.view.View.VISIBLE);
        Glide.with(this).load(uri).centerCrop().into(binding.recipeImagePreview);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}