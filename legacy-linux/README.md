# BradFlix

A cinematic streaming app for Linux. It is written in Rust, has its own native
window, works with Stremio add-ons, and plays video with mpv (hardware
accelerated on your AMD graphics).

The look is inspired by Harbor (a Stremio client): a rotating banner, wide
"Continue watching" cards, Top 10 rows, rails you can scroll, a sidebar, and
themes. BradFlix is its own app. It contains no Harbor code or artwork, and it
is not made by the Harbor or WuPlay teams.

## Install (about 5 to 15 minutes)

1. Extract this folder somewhere, for example your Downloads folder.
2. Open the folder in your file manager.
3. Right-click an empty spot and choose **Open Terminal Here**
   (it may be called **Open in Terminal**).
4. Type this and press Enter:

       bash install.sh

5. Type your password when asked. Nothing appears while you type it. That is normal.
6. Wait until you see **All done!** The build step takes a while the first time.
7. The installer stops old BradFlix/WuPlay processes first. This build also uses a
   separate temporary local web-server port per process, so an old copy cannot
   silently serve its old interface to the updated window. The installer prints
   matching SHA-256 hashes to verify that the compiled binary was copied.

Then open your app menu and search for **BradFlix**.
Closing the window quits the app.

If you used the earlier "WuPlay Linux" version, your profiles, lists and
progress carry over automatically, and the installer removes the old launcher.

## Mixing genres (horror comedies and more)

1. Open **Discover** in the sidebar.
2. Tap several genre chips, for example **Horror** and **Comedy**.
3. With "Must match all selected genres (mix)" you only see titles that fit
   every genre you picked. Switch to "Match any selected genre" to see titles
   from any of them, mixed together.
4. Press **Load more** to dig deeper. Mixed searches scan many titles, so the
   first results can take a few seconds.

Shortcuts: the **Mix genres** buttons on Home, Movies and Shows (like
"Horror + Comedy"), and the genre chips on any title's page.

## Themes

Open **Settings, then Themes**.

- Pick one of the presets (BradFlix, Harbor, Nord, Dracula, Forest, Noir,
  Aurora, Royal).
- **Import a Harbor theme**: choose a `.harborstyle` file, or paste its text.
  BradFlix reads Harbor's front matter, `@tokens`, and `@css` sections. The
  `@js` section is deliberately ignored for safety and compatibility. Theme CSS
  selectors designed specifically for Harbor may not match BradFlix elements.
- Change the navigation layout (left sidebar, icon rail or top bar), card
  corners and font.
- Add your own CSS if you like tinkering.

Only import themes you trust.

Please note: Harbor's theme file format is not published where I could read
it, so the importer reads a theme file as best it can (JSON of any shape, or
plain CSS). If a theme comes in looking wrong or blank, send me the file and I
can teach BradFlix to read it properly.

## Features

- Profiles with separate settings, lists, history and add-ons, plus optional PINs
- Home with a rotating banner, Continue watching, Top 10 rows, custom named catalog sections and your catalogs
- Discover with multi-genre mixing, plus Movies, Shows and Anime rooms
- TV episode release calendar with week navigation (TVmaze schedule data)
- Search, including your add-ons that support search
- My Library: several watchlists (save search results straight to a list) and history
- Watch progress, resume, watched marks, autoplay of the next episode, preference-ranked stream selection, optional automatic playback, and configurable fixed opening skip for episodes
- Cast and crew, person pages, similar titles, trailers
- Preferred audio and subtitle languages
- Add-ons that update themselves, a metadata provider choice, catalog control, custom Home grouping and catalog ordering
- Custom regular-expression rules to prefer or exclude matching streams during automatic playback
- A short, skippable Netflix-inspired BradFlix opening animation
- Sync between computers through a file in a Syncthing, Nextcloud or Dropbox folder
- Backups, and no accounts, tracking or analytics

## Current limitations

BradFlix is an independent Linux discovery and playback client, not a source of every service's licensed catalog or subscription. The Anime page uses built-in Animation catalogs and anime-specific catalogs exposed by installed add-ons; a dedicated anime catalog requires a compatible add-on. The release calendar uses TVmaze schedule data and needs an internet connection. Skip intro currently skips a user-configured number of seconds at the beginning of a new episode; it does not automatically detect intro boundaries. Stream ranking is best-effort because add-ons may omit quality and seed metadata. BradFlix does not include Live TV/Multiview, manga reading, watch parties, casting, tracker sync, debrid integration, trickplay previews or Harbor’s Theme Studio.

## About add-ons (please read)

BradFlix comes with **no add-ons**. Without one you can browse and search, but
there are no videos to play. Add-ons are separate services run by other people.
Only add ones you trust and that are legal where you live.

Torrent-only results are shown greyed out and cannot be played. Only direct
web links work.

## Where your data lives

`~/.config/bradflix/state.json` holds all profiles, lists, add-ons, themes,
history and progress. Delete that file to start fresh. You can also export a
backup from Settings, Privacy and data.

A profile PIN keeps family members out, but it is not strong security because
the data file is plain text.

## If something goes wrong

- **"rustc: symbol lookup error ... LLVM"**: your system was only partly
  updated. Run `sudo pacman -Syu`, restart the computer, then run
  `bash install.sh` again.
- **Install stops with an error**: copy the last 10 lines and ask for help.
- **No window appears or it is blank**: run `bradflix` in a terminal and tell
  me what it prints.
- **"Could not start mpv"**: run `sudo pacman -S mpv`.
- **Video is choppy**: make sure your system is fully updated with
  `sudo pacman -Syu`, then restart.
- **A theme made things unreadable**: Settings, Themes, then pick BradFlix again.

## Remove it

    rm ~/.local/bin/bradflix ~/.local/share/applications/bradflix.desktop
    rm -r ~/.config/bradflix
