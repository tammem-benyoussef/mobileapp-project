package com.example.hamhama.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hamhama.R;
import com.example.hamhama.data.model.Recipe;
import com.example.hamhama.databinding.FragmentHomeBinding;
import com.example.hamhama.ui.adapter.RecipeAdapter;
import com.example.hamhama.ui.detail.RecipeDetailActivity;
import com.example.hamhama.ui.main.MainActivity;
import com.example.hamhama.ui.util.ActivityTransition;
import com.example.hamhama.ui.util.Searchable;
import com.example.hamhama.ui.viewmodel.RecipeViewModel;

import java.util.List;

public class HomeFragment extends Fragment implements Searchable {

    private FragmentHomeBinding binding;
    private RecipeViewModel viewModel;
    private RecipeAdapter adapter;
    private String currentCategory = "All";
    private String currentQuery = "";
    private android.os.Parcelable listState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(RecipeViewModel.class);
        currentQuery = viewModel.getHomeQuery();
        currentCategory = viewModel.getHomeCategory();
        adapter = new RecipeAdapter(new RecipeAdapter.Listener() {
            @Override
            public void onRecipeClicked(Recipe recipe) {
                ActivityTransition.open(requireActivity(), RecipeDetailActivity.newIntent(requireContext(), recipe.getId()));
            }

            @Override
            public void onFavoriteClicked(Recipe recipe) {
                viewModel.toggleFavorite(recipe);
            }
        });

        binding.recipeRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recipeRecycler.setAdapter(adapter);
        binding.recipeRecycler.setHasFixedSize(true);
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        binding.homeSwipeRefresh.setColorSchemeResources(R.color.primary, R.color.secondary);
        binding.homeSwipeRefresh.setOnRefreshListener(() -> {
            viewModel.refreshRemoteRecipes(currentQuery);
            binding.homeSwipeRefresh.postDelayed(() -> {
                if (binding != null) {
                    binding.homeSwipeRefresh.setRefreshing(false);
                }
            }, 700);
        });

        if (!TextUtils.isEmpty(currentQuery)) {
            binding.searchInput.setText(currentQuery);
        }

        if ("Dessert".equals(currentCategory)) {
            binding.chipDessert.setChecked(true);
        } else if ("Vegan".equals(currentCategory)) {
            binding.chipVegan.setChecked(true);
        } else if ("Quick".equals(currentCategory)) {
            binding.chipQuick.setChecked(true);
        } else if ("Breakfast".equals(currentCategory)) {
            binding.chipBreakfast.setChecked(true);
        } else {
            binding.chipAll.setChecked(true);
        }

        binding.searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        binding.searchInput.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                currentQuery = textView.getText() == null ? "" : textView.getText().toString();
                viewModel.refreshRemoteRecipes(currentQuery);
                hideKeyboard();
                return true;
            }
            return false;
        });

        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s == null ? "" : s.toString();
                viewModel.setHomeQuery(currentQuery);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.chipAll.setOnClickListener(v -> setCategory("All"));
        binding.chipDessert.setOnClickListener(v -> setCategory("Dessert"));
        binding.chipVegan.setOnClickListener(v -> setCategory("Vegan"));
        binding.chipQuick.setOnClickListener(v -> setCategory("Quick"));
        binding.chipBreakfast.setOnClickListener(v -> setCategory("Breakfast"));

        viewModel.getHomeRecipes().observe(getViewLifecycleOwner(), this::submitRecipes);
        viewModel.seedInitialData();
        viewModel.setHomeCategory(currentCategory);
        viewModel.setHomeQuery(currentQuery);

        binding.loadingIndicator.setIndeterminateTintList(ContextCompat.getColorStateList(requireContext(), R.color.primary));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listState != null) {
            binding.recipeRecycler.getLayoutManager().onRestoreInstanceState(listState);
            listState = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (binding.recipeRecycler.getLayoutManager() != null) {
            listState = binding.recipeRecycler.getLayoutManager().onSaveInstanceState();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onSearchRequested() {
        if (binding != null) {
            binding.searchInput.requestFocus();
            binding.searchInput.post(() -> {
                InputMethodManager manager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (manager != null) {
                    manager.showSoftInput(binding.searchInput, InputMethodManager.SHOW_IMPLICIT);
                }
            });
        }
    }

    @Override
    public void onSearchQueryChanged(String query) {
        if (binding != null) {
            binding.searchInput.setText(query);
            binding.searchInput.setSelection(binding.searchInput.getText() != null ? binding.searchInput.getText().length() : 0);
        }
    }

    private void setCategory(String category) {
        currentCategory = category;
        if (binding != null) {
            binding.chipAll.setChecked("All".equals(category));
            binding.chipBreakfast.setChecked("Breakfast".equals(category));
            binding.chipDessert.setChecked("Dessert".equals(category));
            binding.chipVegan.setChecked("Vegan".equals(category));
            binding.chipQuick.setChecked("Quick".equals(category));
        }
        viewModel.setHomeCategory(category);
    }

    private void submitRecipes(List<Recipe> recipes) {
        if (binding == null) {
            return;
        }
        adapter.submitList(recipes);
        boolean empty = recipes == null || recipes.isEmpty();
        binding.emptyStateGroup.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recipeRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.loadingIndicator.setVisibility(View.GONE);
        binding.homeSwipeRefresh.setRefreshing(false);
    }

    private void hideKeyboard() {
        if (getActivity() == null) {
            return;
        }
        InputMethodManager manager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(binding.searchInput.getWindowToken(), 0);
        }
    }
}