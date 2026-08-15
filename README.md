# Eight Finger Challenge

A rhythm game for computer keyboards. Notes fall down the screen, and you hit the matching keys when they reach the line. Higher difficulties use more keys until it becomes a full 8 finger challenge.

## Why I made it

These days I am into rhythm games such as Colorful Stage, so I wanted to test if I could make something similar. But most levels on Colorful Stage are just too easy, so I wanted to make something harder and something people can play on computers instead of phones.

The goal is simple: make an 8 fingers challenge.

WITNESS!

## Tools

Main version: Java 21 + JavaFX. Uses Maven.

There is also an HTML version in `index.html` with plain HTML/CSS/JS. No install needed.

## Run the Java version

Make sure Java 21 is installed. Then in the project folder:

- Mac/Linux: `./mvnw javafx:run`
- Windows: `mvnw.cmd javafx:run`

You can also run `Launcher.main()` from IntelliJ.

## Run the HTML version

Just open `index.html` in any browser.

## Controls

Menu:

- `UP` / `DOWN` moves through menu options
- `LEFT` / `RIGHT` changes the selected option
- `ENTER` starts the game
- `S` toggles sound in the Java version

Gameplay:

- Level 1: `J`
- Level 2: `F J`
- Level 3: `D F J K`
- Level 4: `S D F J K L`
- Level 5: `A S D F H J K L`
- `R` restarts
- `ENTER` or `ESC` returns to menu

## How to rebuild it yourself

1. Pick a set of keyboard keys for each difficulty.
2. Make a chart with notes, beats, lanes, and optional hold lengths.
3. Convert beats into seconds using the song BPM.
4. Spawn notes above the screen early enough so they reach the hit line on time.
5. Move every note down each frame.
6. When the player presses a key, find the closest note in that lane.
7. Judge the hit based on distance from the hit line.
8. Give score for good timing and reset combo on misses.
9. Add hold notes by making the player keep the key pressed until the tail reaches the line.
10. Add difficulty by increasing note speed, lane count, note density, and chords.
11. Add a menu so players can choose song, difficulty, and mode.

In the Java version, the code is split into song data, models, chart transforming, sound, note view objects, and the main game loop. The HTML version keeps the same idea in one file so it is easier to open and share.


<img width="1857" height="923" alt="Screenshot 2026-08-14 at 23-13-12 Eight Finger Challenge" src="https://github.com/user-attachments/assets/235f9dfc-6eab-42b1-bf1b-d5d8dfb0dfa9" />
