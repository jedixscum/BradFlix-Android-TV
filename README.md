# BradFlix for Android TV

BradFlix is a customizable movie and series discovery app for Android TV. Its Android interface is based on the catalog browser included in the supplied BradFlix source, with profiles, configurable catalogs, search, watchlists, add-ons, themes, and TV remote navigation.

## What BradFlix does

- Browse and search movie and series catalogs through the built-in catalog provider and Stremio-compatible catalog add-ons you choose.
- Arrange and enable catalogs on Home and Discover, create profiles and watchlists, and keep history and settings on the TV.
- Use the Android TV remote's directional pad and Select button across the app. The TV layout has larger artwork, text, and focus highlights.
- Play supported HTTP(S) video sources inside BradFlix with the built-in Android Media3 player. The player provides fullscreen playback and Android TV remote controls, supports common progressive video, HLS and DASH sources, applies source request headers, and can load supported external subtitles.
- Check for a published update when the app opens. Updates appear only after a signed APK from a successful GitHub Actions build has been selected and published using **Publish Android TV update**. Installation is confirmed by Android.
- Build a debug APK in GitHub Actions after a push to `main`, or with **Actions → Android TV build → Run workflow**.

BradFlix is a discovery and playback client. It does not include every streaming service's licensed catalog or subscription, and it cannot unlock paid services. Catalog coverage depends on the metadata catalogs and add-ons you install. For a provider's licensed video, use that provider's official app and account.

## Build and install

A successful **Android TV build** run uploads `bradflix-android-tv-debug` for testing. Debug APKs are signed differently and cannot update a signed installation. For in-app updates, configure signing once, then use the release publishing flow below.

## Configure signing for in-app updates

Android accepts an update only when it is signed with the same key as the installed app. Add these four repository Actions secrets:

- `ANDROID_KEYSTORE_BASE64`: base64-encoded Java keystore file
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_PASSWORD`

Never commit the keystore or passwords. Keep a secure backup of the signing key. If you initially installed the debug APK, uninstall it before installing a signed release; uninstalling removes app data.

## Publish an update after a successful build

The build workflow uploads a signed APK artifact when all four signing secrets are set. It does not publish every build automatically. To choose a successful build as the update:

1. Open **Actions → Android TV build** and select the successful run you want.
2. Copy the numeric run ID from that run's URL (the digits after `/runs/`).
3. Open **Actions → Publish Android TV update → Run workflow**.
4. Enter that run ID and start the workflow.

The publish workflow verifies that the selected run succeeded on `main` and includes a signed APK before creating the public GitHub Release. BradFlix checks the latest published release when it opens and prompts to install a newer version. Android may ask once to allow BradFlix to install updates; the Android package installer then confirms the installation.

## Android TV limitations

- Built-in playback supports common direct HTTP(S) video streams; DRM-protected provider streams and sources that require a provider's official app are not unlocked. Playback also depends on the stream format, add-on, and your access rights. Linux mpv playback and its progress polling are not part of this Android build.
- Device profiles, lists, themes, add-ons, and history are stored locally. Linux file-based backup and cross-computer sync are not available in the Android TV interface yet.
- Catalogs describe and organize titles. Availability and complete coverage for every service varies by region and by the add-ons or provider catalogs available to you.
- Test D-pad behavior, add-on compatibility, playback, and update installation on your own Android TV device. GitHub Actions builds the APK but cannot certify every TV/player combination.

## Original Linux source

The supplied Linux source is preserved under [`legacy-linux/`](legacy-linux/). Its Rust/mpv desktop backend is separate from the Android TV client.

BradFlix is an independent project. Service names and add-ons do not imply endorsement or affiliation.
