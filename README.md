<p align="center">
  <img src="https://raw.githubusercontent.com/MyCelium-GG/Advancements/main/docs/img/banners/header.svg" alt="Advancements: know exactly what an advancement still needs" width="100%">
</p>

<p align="center">
  <a href="https://www.curseforge.com/minecraft/mc-mods/advancement"><img src="https://img.shields.io/curseforge/dt/1785352?style=flat-square&logo=curseforge&logoColor=white&label=downloads&labelColor=2a2427&color=e0483c" alt="CurseForge downloads"></a>
  <a href="https://www.curseforge.com/minecraft/mc-mods/advancement/files"><img src="https://img.shields.io/badge/Minecraft-26.1%20%E2%80%93%2026.3-655a60?style=flat-square&labelColor=2a2427" alt="Minecraft 26.1 to 26.3"></a>
  <img src="https://img.shields.io/badge/loaders-Fabric%20%C2%B7%20Forge%20%C2%B7%20NeoForge-655a60?style=flat-square&labelColor=2a2427" alt="Fabric, Forge, NeoForge">
  <img src="https://img.shields.io/badge/side-client%20only-655a60?style=flat-square&labelColor=2a2427" alt="Client only">
  <img src="https://img.shields.io/badge/Java-25-655a60?style=flat-square&logo=openjdk&logoColor=white&labelColor=2a2427" alt="Java 25">
  <a href="https://www.curseforge.com/minecraft/mc-mods/mycel"><img src="https://img.shields.io/badge/requires-Mycel-e0483c?style=flat-square&logo=curseforge&logoColor=white&labelColor=2a2427" alt="Requires Mycel"></a>
  <a href="https://wiki.mycelium-gg.org/advancements/"><img src="https://img.shields.io/badge/docs-wiki-655a60?style=flat-square&labelColor=2a2427" alt="Docs on the wiki"></a>
  <a href="https://github.com/MyCelium-GG/Advancements/blob/main/LICENSE"><img src="https://img.shields.io/github/license/MyCelium-GG/Advancements?style=flat-square&labelColor=2a2427&color=655a60" alt="MIT license"></a>
</p>

![About the mod](https://raw.githubusercontent.com/MyCelium-GG/Advancements/main/docs/img/banners/about-the-mod.svg)

![Vanilla won't tell you which biomes you are missing. Click any advancement and a small window shows how to get it, how far along you are and exactly what is still missing. Then track it above your hotbar.](https://raw.githubusercontent.com/MyCelium-GG/Advancements/main/docs/img/banners/about.svg)

![How it works](https://raw.githubusercontent.com/MyCelium-GG/Advancements/main/docs/img/banners/how-it-works.svg)

![Click any advancement (L) to open its window. How to get it: written help for all 126 vanilla advancements. Still needed: every missing biome, food, mob and variant. Overlay: track one advancement per world above your hotbar or in a top corner. Animations and sounds for every new part. Works anywhere: client only, any server.](https://raw.githubusercontent.com/MyCelium-GG/Advancements/main/docs/img/banners/features.svg)

![Screenshots](https://raw.githubusercontent.com/MyCelium-GG/Advancements/main/docs/img/banners/screenshots.svg)

![Screenshots: the How to get it window for Zombie Doctor, the overlay above the hotbar tracking A Balanced Diet, the animation after eating a melon slice, and the corner layout for Monsters Hunted](https://raw.githubusercontent.com/MyCelium-GG/Advancements/main/docs/img/banners/gallery.svg)

![Configuration](https://raw.githubusercontent.com/MyCelium-GG/Advancements/main/docs/img/banners/configuration.svg)

`config/advancements.json`, reload live with `/mycel reload advancements`.

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch. |
| `showTracker` | `true` | Show the overlay. |
| `trackerPosition` | `above_hotbar` | `above_hotbar`, `top_left` or `top_right`. |
| `untrackWhenDone` | `true` | Drop the overlay a few seconds after the advancement is done. |
| `clickForDetails` | `true` | Clicking an advancement opens its window. |
| `sounds` | `true` | Sounds for progress, finishing and opening the window. |

Which advancement has the overlay is saved per world and server in `config/advancements-tracked.json`.

![Anonymous stats](https://raw.githubusercontent.com/MyCelium-GG/Advancements/main/docs/img/banners/anonymous-stats.svg)

Anonymous usage stats (windows opened, which advancements get the overlay, settings, and errors in this mod's
code) go to its FastStats project through Mycel, following Mycel's one-time choice in your game.
[Details](https://wiki.mycelium-gg.org/privacy#stats-advancements).

![Building](https://raw.githubusercontent.com/MyCelium-GG/Advancements/main/docs/img/banners/building.svg)

JDK 25, then:

```sh
./gradlew build            # all loaders + tests
./gradlew :common:test     # unit tests only
./gradlew build -Pmc=26.1  # the 26.1 or 26.2 line
```
