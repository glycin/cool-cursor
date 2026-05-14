# Smooth Caret Trail

A small IntelliJ plugin that leaves a glowing trail behind your caret. Think Kaneda's slide across Neo Tokyo, or a lightcycle carving a ribbon along the Grid.

## What it does

Every caret move spawns an animated streak from the old position to the new one. The head dashes ahead while the tail hangs back, stretching the line; the tail then catches up and the streak collapses into the caret. The dash rides the same easing curve as IntelliJ's native smooth caret movement.

## Customize

Configure under *Settings → Smooth Caret Trail*:

* **Trail shape** — straight line, bezier curve, sine wave (long moves only), or random per dash
* **Number of lines** — single, top + bottom, or top + middle + bottom
* **Head and tail colors** — set both for a gradient, or match them for a flat color
* **Glow** — optional halo with its own color
* **Thickness** — 1 to 50 px

## Requirements

IntelliJ Platform 2026.1 or newer. Pairs best with smooth caret movement enabled under *Settings → Editor → General → Appearance → "Use smooth caret movement"*.

## Build

* `./gradlew runIde` launches a sandbox IDE with the plugin installed
* `./gradlew buildPlugin` produces a distributable zip under `build/distributions/`
* `./gradlew verifyPlugin` checks compatibility against the configured target IDEs

## Tuning

The renderer reads two IntelliJ Registry keys at paint time, so edits apply on the next caret move:

* `editor.smooth.caret.duration` sets the per-caret slide duration in milliseconds
* `editor.smooth.caret.curve.parametric.factor` sets the easing shape

## Marketplace description

The text shown on the JetBrains Marketplace lives in [`MARKETPLACE_DESCRIPTION.html`](./MARKETPLACE_DESCRIPTION.html). It's read at build time by `pluginConfiguration.description` in `build.gradle.kts` and injected into the shipped `plugin.xml`.

## License

See [LICENSE](./LICENSE).
