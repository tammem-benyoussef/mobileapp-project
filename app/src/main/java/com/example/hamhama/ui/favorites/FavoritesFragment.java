package com.example.hamhama.ui.favorites;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hamhama.data.model.Recipe;
import com.example.hamhama.databinding.FragmentFavoritesBinding;
import com.example.hamhama.ui.adapter.RecipeAdapter;
import com.example.hamhama.ui.detail.RecipeDetailActivity;
import com.example.hamhama.ui.util.ActivityTransition;
import com.example.hamhama.ui.util.Searchable;
import com.example.hamhama.ui.viewmodel.RecipeViewModel;

import java.util.List;

public class FavoritesFragment extends Fragment implements Searchable {

    private FragmentFavoritesBinding binding;
    private RecipeViewModel viewModel;
    private RecipeAdapter adapter;
    private android.os.Parcelable listState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(RecipeViewModel.class);
        viewModel.setFavoriteQuery(viewModel.getFavoriteQuery());
        binding.favoriteSearch.setText(viewModel.getFavoriteQuery());
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

        binding.favoriteRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.favoriteRecycler.setAdapter(adapter);
        binding.favoriteRecycler.setHasFixedSize(true);
        binding.favoritesSwipeRefresh.setColorSchemeResources(com.example.hamhama.R.color.primary, com.example.hamhama.R.color.secondary);
        binding.favoritesSwipeRefresh.setOnRefreshListener(() -> {
            CharSequence query = binding.favoriteSearch.getText();
            viewModel.setFavoriteQuery(query == null ? "" : query.toString());
            binding.favoritesSwipeRefresh.postDelayed(() -> {
                if (binding != null) {
                    binding.favoritesSwipeRefresh.setRefreshing(false);
                }
            }, 450);
        });
        binding.favoriteSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        binding.favoriteSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setFavoriteQuery(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        viewModel.getFavoriteRecipes().observe(getViewLifecycleOwner(), this::submitFavorites);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listState != null && binding != null && binding.favoriteRecycler.getLayoutManager() != null) {
            binding.favoriteRecycler.getLayoutManager().onRestoreInstanceState(listState);
            listState = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (binding != null && binding.favoriteRecycler.getLayoutManager() != null) {
            listState = binding.favoriteRecycler.getLayoutManager().onSaveInstanceState();
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
            binding.favoriteSearch.requestFocus();
            InputMethodManager manager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (manager != null) {
                manager.showSoftInput(binding.favoriteSearch, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    @Override
    public void onSearchQueryChanged(String query) {
        if (binding != null) {
            binding.favoriteSearch.setText(query);
            binding.favoriteSearch.setSelection(binding.favoriteSearch.getText() != null ? binding.favoriteSearch.getText().length() : 0);
        }
    }

    private void submitFavorites(List<Recipe> recipes) {
        if (binding == null) {
            return;
        }
        adapter.submitList(recipes);
        boolean empty = recipes == null || recipes.isEmpty();
        binding.emptyFavorites.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.favoriteRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.favoritesSwipeRefresh.setRefreshing(false);
    }
}