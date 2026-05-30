package com.example.hamhama.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.hamhama.CookBookApp;
import com.example.hamhama.R;
import com.example.hamhama.databinding.ActivityMainBinding;
import com.example.hamhama.ui.add.AddRecipeActivity;
import com.example.hamhama.ui.chat.ChatActivity;
import com.example.hamhama.ui.favorites.FavoritesFragment;
import com.example.hamhama.ui.home.HomeFragment;
import com.example.hamhama.ui.profile.ProfileActivity;
import com.example.hamhama.ui.util.ActivityTransition;
import com.example.hamhama.ui.util.Searchable;
import com.example.hamhama.ui.viewmodel.RecipeViewModel;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private RecipeViewModel viewModel;
    private Searchable currentSearchable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        viewModel = new ViewModelProvider(this).get(RecipeViewModel.class);
        updateToolbarSubtitle(R.id.nav_home);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            int systemBarsType = WindowInsetsCompat.Type.systemBars();
            Insets systemBars = insets.getInsets(systemBarsType);
            int topSafeSpacing = getResources().getDimensionPixelSize(R.dimen.space_s);
            binding.toolbar.setPadding(
                    binding.toolbar.getPaddingLeft(),
                systemBars.top + topSafeSpacing,
                    binding.toolbar.getPaddingRight(),
                    binding.toolbar.getPaddingBottom()
            );
            binding.bottomNavigation.setPadding(
                    binding.bottomNavigation.getPaddingLeft(),
                    binding.bottomNavigation.getPaddingTop(),
                    binding.bottomNavigation.getPaddingRight(),
                    systemBars.bottom
            );
            return insets;
        });

        if (savedInstanceState == null) {
            showFragment(new HomeFragment());
            binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
        } else {
            Fragment existing = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
            if (existing instanceof Searchable) {
                currentSearchable = (Searchable) existing;
            }
            updateToolbarSubtitle(binding.bottomNavigation.getSelectedItemId());
        }

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                showFragment(new HomeFragment());
                updateToolbarSubtitle(R.id.nav_home);
                return true;
            }
            if (item.getItemId() == R.id.nav_favorites) {
                showFragment(new FavoritesFragment());
                updateToolbarSubtitle(R.id.nav_favorites);
                return true;
            }
            if (item.getItemId() == R.id.nav_add) {
                ActivityTransition.open(this, new Intent(this, AddRecipeActivity.class));
                return false;
            }
            return false;
        });

        binding.fabChat.setOnClickListener(v -> ActivityTransition.open(this, new Intent(this, ChatActivity.class)));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.bottomNavigation.getSelectedItemId() != R.id.nav_home) {
                    binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
                } else {
                    finishAffinity();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            if (currentSearchable != null) {
                currentSearchable.onSearchRequested();
            }
            return true;
        } else if (item.getItemId() == R.id.action_profile) {
            ActivityTransition.open(this, new Intent(this, ProfileActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_logout) {
            ((CookBookApp) getApplication()).getSessionManager().clear();
            ((CookBookApp) getApplication()).getFirebaseSyncManager().signOut();
            ActivityTransition.open(this, new Intent(this, com.example.hamhama.ui.auth.LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public RecipeViewModel getRecipeViewModel() {
        return viewModel;
    }

    public void setChromeVisible(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        binding.toolbar.setVisibility(visibility);
        binding.bottomNavigation.setVisibility(visibility);
    }

    private void showFragment(Fragment fragment) {
        if (fragment instanceof Searchable) {
            currentSearchable = (Searchable) fragment;
        } else {
            currentSearchable = null;
        }
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void updateToolbarSubtitle(int tabId) {
        CookBookApp app = (CookBookApp) getApplication();
        String sessionName = app.getSessionManager().getName();
        String user = !TextUtils.isEmpty(sessionName) ? sessionName : app.getFirebaseSyncManager().getUserLabel();
        if (tabId == R.id.nav_favorites) {
            binding.toolbar.setSubtitle(getString(R.string.favorites_subtitle));
        } else {
            binding.toolbar.setSubtitle(getString(R.string.welcome_toolbar, user));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (binding.bottomNavigation.getSelectedItemId() != R.id.nav_home) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
        } else {
            finishAffinity();
        }
        return true;
    }

}