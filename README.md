# Better Armorstands

Armor stands, done right. The 13 poses from Bedrock Edition, arms on every new
stand, hold **R** and move the mouse to turn one, shears for the base plate,
and a cleaner look. Runs on the server: players without the mod still get the
poses, arms and base plate; with the mod on the client too they also get R,
the pose preview and the new texture.

![All 13 armor stand poses side by side](https://wiki.mycelium-gg.org/betterarmorstands/img/poses.png)

Built on [Mycel](https://github.com/MyCelium-GG/MyCel-Lib) (`my.celium.org`), the shared
core for the MyCelium ecosystem.

| | |
|---|---|
| Mod id | `betterarmorstands` |
| Minecraft | **26.3**, **26.2**, **26.1** (26.1-26.1.2) · Java **25** |
| Loaders | Fabric, Forge, NeoForge |
| Side | Server for poses, arms and base plate; client too for R, the preview and the texture |

## Behavior

- **Poses**: sneak + right-click a stand with an empty hand to switch to the
  next of the 13 Bedrock poses: Default, No pose, Solemn, Athena, Brandish,
  Honor, Entertain, Salute, Riposte, Zombie, Can-can A, Can-can B, Hero. The
  action bar names the new pose.
- **Redstone**: when the redstone signal next to a stand changes, the
  strength picks the pose (1 = No pose ... 12 = Hero, 13 and up wrap around).
  A steady signal never fights a pose you set by hand.
- **Arms**: every new armor stand has arms, like in Bedrock, whether placed or
  summoned. Stands that already exist keep what they had.
- **Base plate**: right-click a stand with shears to remove the base plate,
  again to bring it back.
- **Turning** (mod on the client): look at a stand, hold **R** and move the
  mouse left or right. The camera stays still while you turn; sneak to snap to
  15 degree steps. Rebind it under Controls, Gameplay.
- **Pose preview** (mod on the client): while sneaking and looking at a stand,
  the action bar shows its pose and the one a click gives.
- **Look** (mod on the client): a cleaner texture with oak grain, outlined
  parts and a polished stone base.

## Anonymous stats

Better Armorstands sends anonymous stats (which poses get used, how often stands
are turned, the settings, and errors in its own code) to its FastStats project
through Mycel. It follows Mycel's choice exactly: the one-time screen in your own
game, `shareStats` / `shareErrors` in `config/mycel.json` on servers.
Details: https://wiki.mycelium-gg.org/privacy#stats-betterarmorstands

![Three armor stands without base plates in diamond, netherite and gold armor](https://wiki.mycelium-gg.org/betterarmorstands/img/display.png)

## Configuration (`config/betterarmorstands.json`)

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch. |
| `sneakPoses` | `true` | Sneak + right-click switches poses. |
| `redstonePoses` | `true` | Redstone signals pick poses. |
| `armsByDefault` | `true` | New armor stands have arms. |
| `shearsBasePlate` | `true` | Shears toggle the base plate. |
| `allowTurning` | `true` | Players with the mod may turn stands with R. |

Reload live with `/mycel reload betterarmorstands`.

## Building

JDK 25, then:

```sh
./gradlew build            # all loaders + tests
./gradlew :common:test     # unit tests only
```
