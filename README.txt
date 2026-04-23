CookBookApp

Overview
CookBookApp is a Java Android recipe app built with MVVM, Room, Retrofit, Gson, and Material 3. It ships with a polished UI for login, home browsing, favorites, recipe details, and recipe creation.

Features
- Login screen with demo mode and optional Google Sign-In placeholder.
- Home feed with search, category chips, recipe cards, loading indicator, and remote API refresh.
- Recipe details with hero image, ingredients, steps, and animated favorite toggle.
- Add Recipe screen with image picker preview and form validation.
- Favorites screen with empty state animation.
- Local persistence with Room.
- Public API integration through Spoonacular.
- Smooth transitions between screens.

Project Structure
- com.example.hamhama.CookBookApp: Application singleton.
- data.local: Room database and DAO.
- data.remote: Retrofit service and DTOs.
- data.repository: Repository layer and sample data.
- ui.auth: Login screen.
- ui.main: Bottom navigation shell.
- ui.home: Recipe feed.
- ui.favorites: Favorite recipes.
- ui.add: Add recipe form.
- ui.detail: Recipe detail screen.
- ui.viewmodel: Shared ViewModels.

API Setup
1. Create a Spoonacular API key.
2. Add it to your Gradle properties as:
   SPOONACULAR_API_KEY=your_key_here
3. Sync the project.

If the key is missing, the app still works using local sample recipes.

Firebase Auth + Firestore Setup
1. Create a Firebase project for this app package: com.example.hamhama
2. Add an Android app in Firebase and download google-services.json
3. Place google-services.json in: app/google-services.json
4. In Firebase Console, enable Authentication with Email/Password
5. In Firebase Console, create a Firestore database (production or test mode)
6. Rebuild and run the app

Behavior
- If Firebase is configured, login uses Firebase Authentication.
- Added/updated recipes (including favorite toggles) are synced to:
   users/{uid}/recipes/{recipeId}
- If Firebase is not configured, login gracefully falls back to local session mode.

Build Notes
- Java 17
- Material 3 theme
- ViewBinding enabled
- Room database file: cookbook_app.db

Run
Open the project in Android Studio and run the app on an emulator or device.