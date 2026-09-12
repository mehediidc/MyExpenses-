# MyExpenses — GitHub-ready Android project

This project keeps the offline SQLite app and adds a small native Android Cordova plugin for:

- **Native Android printing** for Summary and Ledger (opens the real Android Print Service).
- **Easy backup** directly to `Downloads/MyExpenses/` as a JSON file.
- Restore still uses the app's file picker.
- No server, login, or internet dependency for app data.

## GitHub Actions

Upload the whole project to a GitHub repository. The included workflow builds a release APK automatically.

Workflow: `.github/workflows/build-apk.yml`

The APK is uploaded as a workflow artifact.
