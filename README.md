# BradFlix for Android TV

BradFlix is a customizable home for streaming apps on Android TV. The first Android version gives you a remote-friendly row of provider apps, editable names and package IDs, an accent color choice, and update checks against successful signed GitHub releases.

## What this version does

- Navigate with the Android TV remote's D-pad and Select button.
- Launch streaming apps already installed on the device.
- Add, rename, reorder by remove/re-add, or remove provider shortcuts. Long-press a shortcut to edit it.
- Pick an accent color.
- Check for updates when the app opens or from **Check for updates**. The app downloads a release APK only when GitHub Actions has published one, then hands installation to Android's package installer.
- Build an APK in GitHub Actions after each push to `main` and download it from the workflow run.

This is an early Android TV hub shell. It does not yet bring the Linux app's Stremio catalog browser or mpv playback to Android, nor does it include the streaming providers' licensed catalogs. Provider playback opens the provider's own installed app. Each service controls its own account, subscription, and playback.

## Build an APK

Open **Actions → Android TV build → Run workflow**, or push a change to `main`. A successful run attaches `bradflix-android-tv-debug` containing a sideloadable debug APK. Debug APKs are for first install and do not use a stable signing identity across GitHub runners, so they are not the in-app update channel.

## Enable signed in-app updates

Android only accepts an update APK when it is signed with the same key as the installed app. Create a private release key and add these four repository Actions secrets:

- `ANDROID_KEYSTORE_BASE64`: base64-encoded Java keystore file
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_PASSWORD`

After those secrets are set, a successful Actions run publishes a signed APK as a GitHub Release. BradFlix checks the latest release when it opens and offers the update. Android may ask you once to allow BradFlix to install app updates; the final install is always confirmed by Android's package installer.

Keep the keystore and passwords backed up securely. Losing the signing key prevents updates over an existing installation. Never commit the keystore or passwords to the repository.

## Customize provider shortcuts

On the TV, choose **Add app** and enter the app label and Android package name. Long-press a shortcut to edit or remove it. Provider shortcuts use apps installed on the device; they do not add subscriptions or bypass provider access controls.

## Original Linux source

The source from the supplied BradFlix ZIP is preserved under [`legacy-linux/`](legacy-linux/). It is a separate Linux build and is not yet connected to the Android app. Its original README is retained there for reference.

## Roadmap

1. Bring the catalog, add-on, profile, and playback features from Linux to Android with TV-first focus navigation.
2. Add user-configurable groups and content catalogs, using provider APIs and add-ons the user is authorized to access.
3. Add playback through Android Media3 with resume, subtitles, and audio track selection.
4. Verify focus paths and installation/update flows on Android TV hardware and emulators.

BradFlix is an independent project. Provider names are used only to label shortcuts, and do not imply endorsement or affiliation.
