# mobileapp-project

Mobile app project with two parts:

- Android recipe app with Firebase auth, recipe browsing, favorites, profile, and chat UI
- Python FastAPI backend for the chef assistant

## Project Structure

- `app/` Android app source code
- `chefbot/` Python backend source code

## Requirements

- Android Studio for the mobile app
- Python 3.10+ for the backend
- A Groq API key for the chatbot
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
```

### Windows batch

```bat
cd chefbot
start.bat
```

## Notes

- Do not commit `.env`, `local.properties`, or IDE cache folders.
- The backend listens on port `8000` by default.
