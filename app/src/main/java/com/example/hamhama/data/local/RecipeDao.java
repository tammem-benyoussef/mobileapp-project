package com.example.hamhama.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.hamhama.data.model.Recipe;

import java.util.List;

@Dao
public interface RecipeDao {

    @Query("SELECT * FROM recipes WHERE (:query = '' OR title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR ingredients LIKE '%' || :query || '%') AND (:category = 'All' OR :category = '' OR category = :category) ORDER BY favorite DESC, updatedAt DESC")
    LiveData<List<Recipe>> observeRecipes(String query, String category);

    @Query("SELECT * FROM recipes WHERE favorite = 1 AND (:query = '' OR title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR ingredients LIKE '%' || :query || '%') AND (:category = 'All' OR :category = '' OR category = :category) ORDER BY updatedAt DESC")
    LiveData<List<Recipe>> observeFavorites(String query, String category);

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    LiveData<Recipe> observeRecipeById(String id);

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    Recipe getRecipeByIdSync(String id);

    @Query("SELECT COUNT(*) FROM recipes")
    int getCountSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Recipe recipe);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Recipe> recipes);

    @Update
    void update(Recipe recipe);

    @Delete
    void delete(Recipe recipe);

    @Query("UPDATE recipes SET favorite = :favorite, updatedAt = :updatedAt WHERE id = :id")
    void updateFavorite(String id, boolean favorite, long updatedAt);

    @Query("DELETE FROM recipes WHERE source = 'remote'")
    void deleteRemoteRecipes();
}