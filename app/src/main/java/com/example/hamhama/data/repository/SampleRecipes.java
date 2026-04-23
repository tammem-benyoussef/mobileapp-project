package com.example.hamhama.data.repository;

import com.example.hamhama.data.model.Recipe;

import java.util.ArrayList;
import java.util.List;

public final class SampleRecipes {

    private SampleRecipes() {
    }

    public static List<Recipe> create() {
        List<Recipe> recipes = new ArrayList<>();
        recipes.add(build("local_1", "Citrus Avocado Toast", "Breakfast",
                "https://picsum.photos/seed/citrus-toast/900/600",
                "2 slices sourdough\n1 avocado\n1 tsp lemon juice\nChili flakes\nSea salt",
                "Toast bread until golden. Mash avocado with lemon juice and salt. Spread, finish with chili flakes.",
                "A bright, simple breakfast with a clean modern feel."));
        recipes.add(build("local_2", "Creamy Tomato Pasta", "Dinner",
                "https://picsum.photos/seed/tomato-pasta/900/600",
                "200g pasta\n1 cup tomato sauce\n1 tbsp olive oil\n2 garlic cloves\nParmesan",
                "Cook pasta. Saute garlic in olive oil, add sauce, then fold in pasta and finish with parmesan.",
                "Fast comfort food with a silky texture."));
        recipes.add(build("local_3", "Mango Chia Pudding", "Dessert",
                "https://picsum.photos/seed/mango-chia/900/600",
                "3 tbsp chia seeds\n1 cup oat milk\n1 tbsp honey\n1 mango\nMint leaves",
                "Mix chia seeds with oat milk and honey. Chill overnight. Top with mango and mint.",
                "Light, fresh and ideal for meal prep."));
        recipes.add(build("local_4", "Green Buddha Bowl", "Vegan",
                "https://picsum.photos/seed/buddha-bowl/900/600",
                "Quinoa\nRoasted broccoli\nEdamame\nCucumber\nTahini dressing",
                "Assemble warm quinoa with vegetables. Drizzle tahini dressing and serve immediately.",
                "Balanced, vibrant and nutrient-rich."));
        recipes.add(build("local_5", "Honey Glazed Salmon", "Dinner",
                "https://picsum.photos/seed/salmon/900/600",
                "Salmon fillet\nHoney\nSoy sauce\nLime\nSesame seeds",
                "Bake salmon with honey-soy glaze until caramelized. Finish with lime and sesame seeds.",
                "Polished, quick and restaurant-style."));
        recipes.add(build("local_6", "Berry Yogurt Parfait", "Dessert",
                "https://picsum.photos/seed/parfait/900/600",
                "Greek yogurt\nMixed berries\nGranola\nMaple syrup",
                "Layer yogurt, berries and granola in a glass. Repeat and finish with maple syrup.",
                "A clean sweet option for breakfast or dessert."));
        return recipes;
    }

    private static Recipe build(String id, String title, String category, String imageUrl, String ingredients, String steps, String summary) {
        Recipe recipe = new Recipe();
        recipe.setId(id);
        recipe.setTitle(title);
        recipe.setCategory(category);
        recipe.setImageUrl(imageUrl);
        recipe.setIngredients(ingredients);
        recipe.setSteps(steps);
        recipe.setSummary(summary);
        recipe.setSource("local");
        recipe.setFavorite(false);
        recipe.setCreatedAt(System.currentTimeMillis());
        recipe.setUpdatedAt(System.currentTimeMillis());
        return recipe;
    }
}