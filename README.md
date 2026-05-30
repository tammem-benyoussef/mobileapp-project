# mobileapp-project

University project developed by **Tammem Ben Youssef** and **Youssef Namouchi**.

Mobile app project with two parts:

- Android recipe app with Firebase auth, recipe browsing, favorites, profile, and chat UI
- Python FastAPI backend for the chef assistant

## ✨ Main Features

### 🔐 Authentication
- Sign up and login with **Firebase Authentication**
- Persistent session: the user stays logged in even after closing the app

### 🍽️ Recipe Catalog
- Display of recipes with image, title, and category
- Recipes fetched from **TheMealDB API**
- User-added recipes stored in **Firebase Firestore**

### 🔍 Search & Filtering
- Search bar to find a recipe by name
- Filter by **category** (Starter, Main Dish, Dessert, etc.)
- Real-time results displayed in a `RecyclerView`

### 📄 Recipe Details
- Full display: image, title, category, ingredients, preparation steps
- `RatingBar` to rate recipes
- Button to add or remove recipes from favorites

### ⭐ Favorites
- Dedicated section for saved recipes
- Each user sees only their own favorites

### ➕ Add Recipes
- Form to add a new custom recipe
- Fields: title, ingredients, steps, category, image
- Recipe saved in **Firebase Firestore**

### 🤖 Culinary Chatbot
- Integrated chatbot powered by **Groq**
- Users can ask questions about any recipe
- Natural language answers about ingredients, steps, substitutions, and more

## Project Structure

- `app/` Android app source code
- `chefbot/` Python backend source code

## Requirements

- Android Studio for the mobile app
- Python 3.10+ for the backend
- A Groq API key for the chatbot
- Firebase Authentication and **Firestore** for authentication and data storage
- A Firebase Android config file at `app/google-services.json` for Firebase features

## Android App Setup

1. Open the `MobileApp-1` folder in Android Studio.
2. Sync Gradle.
3. If you use Firebase, make sure `app/google-services.json` is configured for your project.
4. Run the app on an emulator or device.

## Backend Setup

1. Open a terminal in `chefbot/`.
2. Create and activate a virtual environment.
3. Install dependencies from `requirements.txt`.
4. Copy `.env.example` to `.env` and add your `GROQ_API_KEY`.
5. Start the server.

### PowerShell

```powershell
cd chefbot
.\start.ps1
