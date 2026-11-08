# Advancements

![About the mod](https://raw.githubusercontent.com/MyCelium-GG/Advancements/main/docs/img/banners/about-the-mod.svg)

Vanilla tells you "Discover every biome", but not which ones you are missing.
Advancements does. Click any advancement in the advancements screen and a small
window explains **how to get it**, shows how far along you are and lists
exactly **what is still missing**: every biome, food, mob, cat or wolf variant.
Turn on its **overlay** and follow it while you play, right above your hotbar.

![The Advancements window over vanilla's advancements screen: Adventuring Time, How to get it, 29 of 56 done, and the biomes still needed](docs/img/window.png)

Built on [Mycel](https://github.com/MyCelium-GG/MyCel-Lib) (`my.celium.org`), the shared
core for the MyCelium ecosystem.

| | |
|---|---|
| Mod id | `advancements` |
| Minecraft | **26.3**, **26.2**, **26.1** (26.1-26.1.2) · Java **25** |
| Loaders | Fabric, Forge, NeoForge |
| Side | Client only: works in singleplayer and on any server, vanilla ones included |

![How it works](https://raw.githubusercontent.com/MyCelium-GG/Advancements/main/docs/img/banners/how-it-works.svg)

- **Click an advancement** in the advancements screen (**L**) to open its
  window. Dragging the tree and switching category tabs work as before.
- **How to get it**: a written explanation for every one of Minecraft's 126
  advancements (what to do, where, and a tip where one helps). Advancements
  from other mods and data packs show their own description.
- **Still needed**: every missing part with its icon, then the parts you have
  in gold. Hover one you have to see when you got it.
- **Overlay**: turn it on for one advancement per world or server. It sits
  above your hearts and food with the advancement's frame in the middle and
  its progress bar to both sides, and steps aside for the held item's name and
  action bar messages. Or put it in a top corner as an advancement toast.
- **Animation and sounds**: every new part pops the frame, bursts a gold ring
  and sparks, fades in `+ Melon Slice` and plays a chime that climbs in pitch.
  Finishing gets a bigger celebration, then the overlay shrinks away.

![Screenshots](https://raw.githubusercontent.com/MyCelium-GG/Advancements/main/docs/img/banners/screenshots.svg)

![Zombie Doctor: how to get it](docs/img/explain.png)

![The overlay above the hotbar tracking A Balanced Diet, 23/40](docs/img/overlay.png)

![The overlay animating after eating a melon slice](docs/img/animation.png)

![The corner layout: Monsters Hunted with the next mobs to hunt](docs/img/corner.png)

![Anonymous stats](https://raw.githubusercontent.com/MyCelium-GG/Advancements/main/docs/img/banners/anonymous-stats.svg)

Advancements sends anonymous stats (how often windows are opened, which
advancements get the overlay, the settings, and errors in its own code) to its
FastStats project through Mycel. It follows Mycel's choice exactly: the
one-time screen in your own game. Details:
https://wiki.mycelium-gg.org/privacy#stats-advancements

![Configuration](https://raw.githubusercontent.com/MyCelium-GG/Advancements/main/docs/img/banners/configuration.svg)

Settings live in `config/advancements.json`.

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch. |
| `showTracker` | `true` | Show the overlay. |
| `trackerPosition` | `above_hotbar` | `above_hotbar`, `top_left` or `top_right`. |
| `untrackWhenDone` | `true` | Drop the overlay a few seconds after the advancement is done. |
| `clickForDetails` | `true` | Clicking an advancement opens its window. |
| `sounds` | `true` | Sounds for progress, finishing and opening the window. |

Reload live with `/mycel reload advancements`. Which advancement has the
overlay is saved per world and server in `config/advancements-tracked.json`.

![Building](https://raw.githubusercontent.com/MyCelium-GG/Advancements/main/docs/img/banners/building.svg)

JDK 25, then:

```sh
./gradlew build            # all loaders + tests
./gradlew :common:test     # unit tests only
./gradlew build -Pmc=26.1  # the 26.1 or 26.2 line
```
