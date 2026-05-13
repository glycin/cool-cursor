# Cool Caret

A small IntelliJ plugin that leaves a glowing violet trail behind your caret. Think Kaneda's slide across Neo Tokyo, or a lightcycle carving a ribbon along the Grid.

## What it does

When your caret jumps to a new position, a translucent wedge chases it across the editor. The trail rides the same easing curve as the platform's smooth caret, so it stays glued to the moving cursor rather than racing ahead to the destination. It fades out cleanly in about 350 ms.

Each dash carries a touch of random variation in length and angle, so motion never looks mechanical.

## Requirements

IntelliJ Platform 2024.1 or newer, with smooth caret movement enabled under Settings, Editor, General, Appearance, "Use smooth caret movement".

## Build

* `./gradlew runIde` launches a sandbox IDE with the plugin installed
* `./gradlew buildPlugin` produces a distributable zip under `build/distributions/`
* `./gradlew verifyPlugin` checks compatibility against the configured target IDEs

## Tuning

Two IntelliJ Registry keys shape the animation. The renderer reads both at paint time, so edits apply on the next caret move:

* `editor.smooth.caret.duration` sets how many milliseconds the caret takes to slide
* `editor.smooth.caret.curve.parametric.factor` sets the easing shape

## License

See [LICENSE](./LICENSE).
