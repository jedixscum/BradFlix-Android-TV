# BradFlix for Android TV

BradFlix is a customizable home for streaming apps on Android TV. The first Android version gives you a remote-friendly row of provider apps, editable names and package IDs, an accent color choice, and update checks against published GitHub Releases.

## What this version does

- Navigate with the Android TV remote's D-pad and Select button.
- Launch streaming apps already installed on the device.
- Add provider shortcuts with custom names and home groups. Long-press a shortcut to edit its name, package ID, or group, or remove it.
- Pick an accent color.
- Check for updates when the app opens or from **Check for updates**. The app downloads a release APK only after you publish it from a successful GitHub Actions build, then hands installation to Android's package installer.
- Build debug and, when signing is configured, signed APK artifacts in GitHub Actions after each push to `main`.

This is an early Android TV hub shell. It does not yet bring the Linux app's Stremio catalog browser or mpv playback to Android, nor does it include the streaming providers' licensed catalogs. Provider playback opens the provider's own installed app. Each service controls its own account, subscription, and playback.

## Build an APK

Push a change to `main`, or open **Actions → Android TV build → Run workflow**. A successful run attaches `bradflix-android-tv-debug`, a sideloadable debug APK. Debug builds are for testing and cannot receive updates from a signed release because Android requires the same signing key for updates.

## Enable signed in-app updates

Android only accepts an update APK when it is signed with the same key as the installed app. Create a private release key and add these four repository Actions secrets:

- `ANDROID_KEYSTORE_BASE64`: base64-encoded Java keystore file
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_PASSWORD`

Never commit the keystore or passwords. Losing the signing key prevents updates over an existing installation. If you first installed the debug APK, uninstall it before installing a signed release; uninstalling removes its app data.

## Publish an update from a successful build

Each successful build with the signing secrets configured uploads a `bradflix-android-tv-release` APK artifact, but does not publish it automatically. To choose which successful build becomes the update:

1. Open **Actions → Android TV build** and select the successful run you want.
2. Copy the numeric run ID from its page URL (the digits after `/runs/`).
3. Open **Actions → Publish Android TV update → Run workflow**.
4. Enter that run ID and start the workflow.

The publishing workflow checks that the selected run succeeded on `main` and contains a signed APK, then creates a GitHub Release. BradFlix checks the latest release when it opens and offers the update. Android may ask once to allow BradFlix to install updates; the final install is confirmed by Android's package installer.

Keep the keystore and passwords backed up securely. Losing the signing key prevents updates over an existing installation. Never commit the keystore or passwords to the repository.

## Customize provider shortcuts

On the TV, choose **Add app** and enter the app label, Android package name, and home group. Shortcuts sharing a group appear together. Long-press a shortcut to edit or remove it. Provider shortcuts use apps installed on the device; they do not add subscriptions or bypass provider access controls.

## Original Linux source

The source from the supplied BradFlix ZIP is preserved under [`legacy-linux/`](legacy-linux/). It is a separate Linux build and is not yet connected to the Android app. Its original README is retained there for reference.

## Roadmap

1. Bring the catalog, add-on, profile, and playback features from Linux to Android with TV-first focus navigation.
2. Add user-configurable groups and content catalogs, using provider APIs and add-ons the user is authorized to access.
3. Add playback through Android Media3 with resume, subtitles, and audio track selection.
4. Verify focus paths and installation/update flows on Android TV hardware and emulators.

BradFlix is an independent project. Provider names are used only to label shortcuts, and do not imply endorsement or affiliation.
