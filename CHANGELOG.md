# Changelog

All notable changes to Better Armorstands are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Better Armorstands is versioned semantically (`MAJOR.MINOR.PATCH`).
It runs on Minecraft 26.3, 26.2 and 26.1 (Fabric + Forge + NeoForge, Java 25) and requires
[Mycel](https://github.com/MyCelium-GG/MyCel-Lib) (`my.celium.org`).

## [1.0.1] - 2026-11-04

### Fixed
- With the mod on the client, armor stands without arms (vanilla ones, or summoned with `ShowArms:0b`) were drawn with arms anyway. Only how they looked was wrong; the stands themselves were fine.

## [1.0.0] - 2026-11-04

### Added
- The 13 armor stand poses of Bedrock Edition: sneak + right-click a stand with an empty hand for the next one.
- Redstone poses: a changing redstone signal next to a stand picks a pose by its strength.
- Arms on every new armor stand, placed or summoned. Saved stands keep what they had.
- Shears: right-click a stand to remove or bring back its base plate.
- Hold R while looking at a stand and move the mouse to turn it; sneak for 15 degree steps (mod on the client).
- While sneaking at a stand, the action bar shows its pose and the next one (mod on the client).
- A cleaner armor stand texture: warm oak with grain and outlines, a polished stone base (mod on the client).
- Anonymous stats (which poses get used and how stands get turned) so we know what to improve. They follow your answer on Mycel's stats screen; nothing else is asked. What is sent: https://wiki.mycelium-gg.org/privacy#stats-betterarmorstands
