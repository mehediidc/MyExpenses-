# MyExpenses v5 — GitHub APK Builder

This project builds the MyExpenses Android APK automatically with GitHub Actions.

## Build the APK

1. Create a new GitHub repository, for example `MyExpenses-v5`.
2. Upload **all files and folders in this project** to the repository root.
3. Make sure `.github/workflows/build-apk.yml` is uploaded too.
4. Commit/push to the `main` branch.
5. Open the repository → **Actions** → **Build MyExpenses APK**.
6. Open the completed workflow run.
7. Under **Artifacts**, download **MyExpenses-v5-APK**.
8. Extract the ZIP and install the APK on Android.

You can also build manually from Actions → Build MyExpenses APK → Run workflow.

## Important

The generated APK is a **debug APK**. It is suitable for direct installation/testing. It is not signed with your personal release key, so it should not be expected to update an already-installed APK signed with a different key.

The Cordova File plugin is explicitly installed during the GitHub build so the MyExpenses backup function can write a backup file to Android external storage/Downloads where supported by the Android version.
