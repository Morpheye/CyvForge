# <img src="src/main/resources/icon.png" width="80" align="center"> CyvForge

CyvForge is a parkour practice mod. It follows your jumps tick by tick and tells you what actually happened: how far from the edge you landed, which timing you used, how you turned, how fast you moved, and a lot more. All of it is available as a fully customizable HUD, as chat messages and through in-game GUIs. On top of that it ships a movement macro system, command hotkeys, singleplayer checkpoints, calculators and a movement simulator.

<p>
  <a href="https://modrinth.com/mod/cyv"><img src="images/Modrinth.png" alt="Modrinth" height="50"></a>&nbsp;&nbsp;
  <a href="https://discord.gg/a9qQZdVGZx"><img src="images/Discord.png" alt="Discord" height="50"></a>
</p>

Related Repos:
> CyvClient 1.8.9 (CyvForge mod but a client): [CyvClient 1.8.9](https://github.com/Morpheye/CyvClient-Extended) - unmaintained <br>
> CyvForge 1.12.2: [CyvForge 1.12.2](https://github.com/Morpheye/CyvForge-1.12) - unmaintained <br>
> Latest (plus a few earlier versions): [CyvFabric](https://github.com/Morpheye/CyvFabric) - maintained by [Zpiboo](https://github.com/zpiboo)

---

## Table of contents

- [Getting started](#getting-started)
- [Landing block](#landing-block)
- [Momentum block](#momentum-block)
- [HUD](#hud)
- [Last Timing](#last-timing)
- [Macros](#macros)
- [Command hotkeys (Chat Macros)](#command-hotkeys-chat-macros)
- [Checkpoints and Anti-Checkpoint](#checkpoints-and-anti-checkpoint)
- [Calculators and simulator](#calculators-and-simulator)
  - [Simulator syntax](#simulator-syntax)
- [Inertia listener](#inertia-listener)
- [Position checker](#position-checker)
- [Quality of life](#quality-of-life)
- [Command reference](#command-reference)
- [Settings reference](#settings-reference)
- [Keybinds](#keybinds)
- [Files](#files)
- [Community](#community)
- [Credits](#credits)

---

## Getting started

1. **Get the mod.** Pick one of the two ways:
- **Download it:** grab the latest `.jar` from [Modrinth](https://modrinth.com/mod/cyv).
- **Build it yourself:** install **JDK 8** (newer Java versions do not work with this ForgeGradle version), download or clone this repository, open a terminal in the project folder and run `gradlew build` (`./gradlew build` on Linux/macOS). You can also run the `build` task from your IDE's Gradle panel. The finished mod is `build/libs/cyvforge-<version>.jar` (not the `-sources` one).
2. Drop the `.jar` into the `mods` folder of your **Minecraft 1.8.9** installation with **Forge** and start the game.
3. Press **Right Shift** (or send `/cyv gui`) to open the main GUI.
4. Stand on the block you want to land on and send `/cyv setlb`.
5. Do the jump. Your offset shows up on the HUD (and optionally in chat).

Every command starts with **`/cyv`**. The aliases **`/mpk`** and **`/mm`** work the same way, so `/mpk setlb` and `/cyv setlb` are identical. All subcommands have tab completions, and `/cyv help <subcommand>` prints the usage, aliases and a short description.

---

## Landing block

The landing block is the core of the mod. Once you set one, CyvForge measures how far from the edge of that block you landed (the **offset**) on every jump, keeps your personal best (**PB**) and can show everything on the HUD, in chat and as a highlighted box in the world.

```
/cyv setlb [arguments]
```

By default the block **you are standing on** is used. Arguments can be combined in any order, for example `/cyv setlb target hit` or `/cyv setlb x tick5`.

| Argument | What it does |
|---|---|
| `target` | Use the block you are looking at instead of the one you stand on |
| `x` / `z` | Measure only the X or only the Z axis (default: both axes) |
| `land` / `landing` | Measure the offset on the tick you land (default) |
| `hit` | Measure the offset on the tick you hit the top of the block. Also works on water and lava, and on ice (distance from the middle, to get the boost) |
| `enter` | Measure the offset while you are inside the block's height, for ladders and vines |
| `zneo` / `xneo` | Neo modes: `zneo` uses the Z of the previous tick, `xneo` uses the X of the next tick. Useful to see where you were before you left a wall, e.g. on the last momentum block before a neo or triple |
| `box` | Treat the block as a player-sized box (ladders, vines, slime, ice) |
| `slime` / `ice` | Shortcut for `box hit` |
| `ladder` / `vine` | Shortcut for `box enter` |
| `tick<N>` / `tier<N>` | Check the offset on air tick *N* (a tier). Handy on water, lava, ladders and vines, or to artificially raise the tier of a normal jump |
| `ignorewall` (`nowall`, `iwh`) | Use your position as if you had not collided with a wall |

**Related commands**

| Command | Description |
|---|---|
| `/cyv lb` (`landingblock`, `landing`) | Opens the landing block GUI: landing mode, axis, bounding box visibility, condition visibility, target tick, ignore wall |
| `/cyv clearlb` | Removes the landing block |
| `/cyv clearpb` | Clears the stored offsets and PB of the landing block |
| `/cyv setcond <x1> <x2> <z1> <z2>` | Sets the detection zone. Offsets are only recorded when your position is inside it |
| `/cyv setbox <x1> <x2> <y1> <y2> <z1> <z2>` | Creates a landing zone with custom dimensions |

**Good to know**

- **Walls are detected for you.** Iron bars, fences, blocks above the landing block and similar obstacles are taken into account, so the offset reflects where you could really stand. The visual bounding box updates automatically when the surroundings change, so you never have to resend `setlb` after changing a block.
- Blocks with several hitboxes (stairs, iron bar crosses, ...) are handled box by box.
- Every new personal best is announced in chat (`New pb!`). Enable **Send Landing Offset** in the settings to also get the X and Z offset of every landing in chat.
- **Highlight Landing Blocks** and **Highlight Landing Conditions** draw the bounding box and the detection zone in the world.

---

## Momentum block

The momentum block works exactly like the landing block, but for the block you jump from or pass on the way (your *MM*). It uses the same arguments.

| Command | Description |
|---|---|
| `/cyv setmm [arguments]` | Sets the momentum block (same arguments as `setlb`) |
| `/cyv clearmm` | Removes the momentum block |
| `/cyv mm` (`momentumblock`, `momentum`) | Opens the momentum block GUI |

- The **MM X Offset** and **MM Z Offset** labels show the result on the HUD.
- **Send Momentum Offset** prints the momentum offset in chat.
- **Momentum PB Cancelling** ignores a landing (no PB, no chat offset) if the momentum block was missed.

---

## HUD

Open the main GUI with **Right Shift**. It lists every HUD element with a search bar, lets you enable or disable each one and gives access to its own settings through the **⋯** button. **Edit Positions** (default key **P**) lets you drag elements wherever you want, and keystrokes and input history can be resized right there. `/cyv resetgui` puts everything back at the default positions.

### Labels

| Group | Labels |
|---|---|
| Coordinates | FPS, X, Y, Z, Yaw (facing, optional axis), Pitch |
| Landing | PB, X Offset, Z Offset, Total Offset, X PB, Z PB, Last Landing X / Y / Z |
| Momentum | MM X Offset, MM Z Offset |
| Hit | Hit X / Y / Z, Hit Angle, Hit Vector |
| Jump | Jump X / Y / Z |
| Turning | Jump Angle, Second Turn, Preturn, Last Turning, Last 45 |
| Timings | Last Timing, Airtime, Stoptime, Waittime, Runtime, Tier |
| Inputs | Last Input, Last Sidestep |
| Speed | X Speed, Y Speed, Z Speed, Speeds, Speed Vector |
| Misc | Blips, Grinds, System Time |

**Good to know**
- **Yaw** can be rendered per frame instead of per tick, which is required for hard very jumps on some servers as proof.

### Other HUD elements

| Element | Description |
|---|---|
| **Keystrokes** | On-screen WASD, sprint, sneak and jump, with an adjustable size |
| **Input History** | A scrolling history of your inputs, see below |
| **Turn HUD** | Turning angles for ticks 1 to 12, with a configurable range, shown as one object or as separate labels |
| **Direction HUD** | A compass bar |
| **Togglesprint HUD** | Shows `[Sprint Toggled]` while toggle sprint is on |

### Input History

| Option | Description |
|---|---|
| Size / History Length | Size of the label and number of lines shown |
| Inputs Always Visible / Visibility Length | Keep lines forever, or let them disappear after a number of ticks |
| Text Color | White, `Color1 - Color2` or `Color2 - Color1` |
| Invert List | Choose the direction the lines grow in |
| Track Facing / Track Turn Amount | Show the facing or the turn amount for every change |
| Don't Split on Yaw Change | Only start a new line when the input changes, not the facing |
| Input Release (Jmp/Spr) | Sprint only appears when pressed, jump only when you really jump from the ground |
| Ground Jump Reset | Reset on landing. Sub-options: treat the landing tick as air, no line split on jump only, show press and release ticks (e.g. `1-4t`), air/ground indicator |
| Ignore Spr Until Input | Do not count ticks while only sprint is held |
| Reset History (Right Click) | Clear the label on right click |

---

## Last Timing

The **Last Timing** label classifies the way you started your last jump.

| Label text                                        | Meaning                                                                                                                        |
|---------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| `Jam`                                             | Jump pressed together with the first movement key                                                                              |
| `Burst n ticks`, `HH n ticks`, `Burstjam n ticks` | Jump after holding a movement key on the ground for *n* ticks                                                                  |
| `Pessi n ticks` / `Max Pessi`                     | Movement key pressed *n* ticks after the jump (`Max Pessi` = 1 tick)                                                           |
| `FMM n ticks` / `Max FMM`                         | Sprint pressed *n* ticks after the jump while holding W (`Max FMM` = 1 tick)                                                   |
| `Mark n ticks`                                    | W pressed in the air after jumping with A or D                                                                                 |
| `Wobble (key)`                                    | A key released and pressed again                                                                                               |
| `Strafejam n ticks`                               | A or D released while holding W in the air (shown after a `Jam` or `Mark`, or after any timing if *Strafejam Jam only* is off) |

**Options** (in the label's settings):

| Option | Description |
|---|---|
| Show Milliseconds | Append the real time difference, e.g. `Jam (12ms)` |
| Wobble Timing | Detect and display wobbles |
| Strafejam Timing | Detect and display strafejams (+ *Strafejam Jam only*) |
| Mark Display | Show marks on the **Last Sidestep** label (`Sidestep`) or on this label (`Timing`) |
| Keep Updating | See below |

**Keep Updating.** Normally the label stays on the first thing it recognized. For example, `Space + S` is shown as `Jam`, and a `W` pressed one tick later does not change it. With *Keep Updating* on, later inputs update the label: that jump becomes `Max Pessi`, and a sprint pressed afterwards (with W held) becomes `Max FMM `. With the option off, the label behaves exactly like before.

---

## Macros

Record, edit and replay movement tick by tick.

- Open the editor with `/cyv macro`. Every row is one tick with the columns **W, A, S, D, Jump, Sprint, Sneak, RMB, Yaw, Pitch**. You can add, duplicate and delete rows.
- **Record** with `/cyv macro rec start` and `/cyv macro rec stop`.
- **Clip** with `/cyv macro clip`: saves the last *N* ticks of your movement as a macro (up to 500 ticks = 25 s, enable *Macro Clip* in the settings first).
- **Run** with `/cyv macro run` or the *Run Macro* key (default **V**). A second key, *Stop Macro*, is unbound by default.
- The **Recent Macros** tab and the **List** window show your macros. You can load, rename (**R**), copy (**C**) and delete (**X**) them, and open the macro folder.
- Pressing Enter while typing a file name loads it.
- **Keyboard editing:** move around with the arrow keys or `TAB/ALT + WASD`. Shortcuts with `TAB/ALT`: `Z` undo, `X` delete tick, `C` copy tick, `V` paste tick, `B` add an empty tick. An option lets you use the letters without holding `TAB/ALT`.
- *Smooth Macro* and *Macro HUD Color* are available in the settings.

> Running macros only works in **singleplayer**. If you're caught using multiplayer macro on a server without permission you will get banned.

---

## Command hotkeys (Chat Macros)

Bind a key, a key combination or a mouse button (including side buttons) to a command or chat message. Open them from the main GUI (**Chat Macros**).

- Whether a command is client or server sided is detected automatically.
- **Chain mode** has two options:
  - **Step**: every press alternates between an *On* and an *Off* command.
  - **Link**: one press runs the primary and then the secondary command at the same time.

---

## Checkpoints and Anti-Checkpoint

**Singleplayer checkpoints.** Hold an item and send `/cyv checkpoint` to turn it into a checkpoint item. Items that already have a right click action (blocks, armor, food, buckets, bows, potions and similar) cannot be used. Right clicking with it teleports you to the saved position. While you hold it you are also protected from fall damage, fire and more.

```
/cyv checkpoint [x] [y] [z] [yaw] [pitch] [custom name]
```

Without arguments your current position is used. Aliases: `cp`, `setcheckpoint`, `setcp`.

`/cyv getcheckpointgenerator [x] [y] [z] [yaw] [pitch] [custom name]` (aliases `getcpgenerator`, `getcpgen`) gives you a command block that can create checkpoint items. Its dye color and hotbar slot can be set in the settings. Checkpoints can be disabled with *Custom Checkpoints Enabled*.

**Anti-Checkpoint.** After a successful landing (a new PB that is not negative) your right click is blocked for a configurable number of seconds (1 to 10), so you cannot trigger a checkpoint by accident right after a good jump.

---

## Calculators and simulator

| Command | Aliases | Description |
|---|---|---|
| `/cyv airtime <y> [ceiling_height]` | `duration` | Airtime in ticks for a given elevation change and optional ceiling |
| `/cyv height <airtime> [ceiling_height]` | | Elevation change for a given airtime and optional ceiling |
| `/cyv distance <x> <z>` | `dist`, `rawdistance`, `rawdist` | Raw distance and angle of a diagonal jump |
| `/cyv calculate <expression>` | `calc`, `=`, `==` | Evaluates a math expression |
| `/cyv simulate` | `sim`, `s`, `%` | Opens the movement simulator |
| `/cyv setsensitivity <0.0-1.0>` | `ss`, `setsens` | Sets your sensitivity (100% = 0.5) |
| `/cyv optimizesensitivity <angle>` | `os`, `optimizesens`, `optimisesens`, `optimisesensitivity` | Adjusts your sensitivity so a given angle is a whole number of pixels |

The **movement simulator** takes a string of movement functions (for example `sprint`/`s`, `sprintjump`/`sj`, `walk`/`w`, `stop`/`st`, `sneak`/`sn`) and prints the resulting position, velocity and speed vector in chat. See [Simulator syntax](#simulator-syntax) below.

### Simulator syntax

Open the simulator with `/cyv simulate`, type a string and press **Enter** (or click **Calculate**). **Up** and **Down** bring back your previous inputs. The result is printed in chat: `x` and `z` (distance travelled), `vx` and `vz` (velocity at the end) and the **Speed Vector** (length and angle). If the string is invalid you only get `Parsing failed.`

The string is a list of **functions separated by spaces**. Each function looks like this:

```
name(duration, facing)
```

- `duration` is the number of ticks (a whole number, default `1`). A negative number means the **S** key instead of **W**.
- `facing` is the yaw in degrees, like in F3 (default `0`).
- Both are optional, so `sj` is the same as `sj(1)` and `sj(12)` uses facing `0`.
- Function names are not case sensitive.

**Names** are built from a few parts:

| Part | Meaning | Examples |
|---|---|---|
| `w`, `s`, `sn` (`c`), `sns` (`cs`), `st` | Keys held: walk, sprint, sneak, sneak + sprint, no keys (stop) | `w(5)`, `s(5)`, `st(3)` |
| ending `j` | The first tick is the jump tick, the remaining ticks are in the air | `sj(12)`, `wj(12)`, `stj` (also `j`, `jump`) |
| ending `a` | All ticks are in the air | `sa(5)`, `wa(5)`, `sta(5)` |
| `45` | 45 strafe variant: W + A with the camera turned by 45° | `s45(5)`, `sj45(12)`, `s45a(5)` |
| prefix `l` / `r` | Jump tick with A / D held, the rest with W only | `lsj(12)`, `rsj(12)` |

The long names work too, for example `sprint`, `sprintjump`, `sprintair`, `walkjump`, `stopjump`, `sneaksprint`.

- A jump function with `duration` *n* is one jump tick followed by *n - 1* air ticks, so `sj(12)` is 12 ticks in total.
- In the `45` variants `facing` is the direction you **move** in. The function adds the 45° turn itself. In `sj45` the jump tick goes straight and only the following ticks are strafed.

**Key modifiers.** Add a dot and the keys you want after the function name: `.w`, `.a`, `.s`, `.d`.

- `s.wa(5)` holds W and A, `s.a(5)` holds only A. Without `w` or `s` the forward key is not held.
- `a` together with `d` cancel each other out.
- Modifiers are not allowed on the `45` and `l`/`r` variants, or together with a negative `duration`.

**Effects.** Put `name=value` inside the parentheses to change a single function:

| Effect | Meaning |
|---|---|
| `slip` | Slipperiness of the ground: `0.6` normal block (default), `0.8` slime, `0.98` ice |
| `blocking` | `true` / `false`: blocking with a sword |
| `swiftness`, `slowness` | Potion effect level (a whole number) |

```
s(10, slip=0.98)
sj(12, 45, slip=0.8)
```

**Global functions.** These take one value and apply to everything that comes after them.

| Function | Effect |
|---|---|
| `f(deg)` (`facing`, `face`) | Default facing for the following functions |
| `vx(v)`, `vz(v)` | Set the velocity on the X or Z axis |
| `posx(v)`, `posz(v)` | Set the position on the X or Z axis |
| `slip(v)` | Default slipperiness |
| `slow(n)`, `swift(n)` | Default slowness / swiftness level |

**Variables and distance helpers.**

- `var(name, value)` defines a variable that you can use as `duration`, `facing` or an effect value, e.g. `var(a,45) sj(12,a)`.
- `|` resets `x` and `z` to 0, so you can measure the distance from that point on.
- `b` adds 0.6 (the player's width) to the current distance on both axes.

**Examples**

```
sj(12)                   sprint jump: 1 jump tick + 11 air ticks
sprint(3) sj(12)         3 ticks of running first, then the jump
sj45(12)                 45 strafe jump
lsj(12)                  jump with A on the jump tick
j wa(11)                 pessi: jump without keys, W only after that
sj(12, 45, slip=0.8)     jump at yaw 45 from slime
```

---

## Inertia listener

Tells you whether you hit inertia on a chosen tick. At the chosen air tick the speed on the chosen axis is recorded, and if it falls inside your min/max range you get a `Hit inertia` or `Missed inertia` message.

`/cyv inertia [toggle | tick <1-12> | min <value> | max <value> | mode <x|z> | ground <normal|ice|slime>]`

The same options are available in the settings.

---

## Position checker

Sends your X and Z coordinates in chat on a chosen air tick, but only when you are inside the coordinate limits.

`/cyv positionchecker [toggle | mark | tick <1-12> | minx | maxx | minz | maxz <value> | zneo]` (aliases `pos`, `poscheck`, `poschecker`)

- `mark` (also `here`, `set`) sets the limits to your current position +/- the configured radius. The settings have a button.
- `zneo` uses the Z of the previous tick instead of the current one.

The same options are available in the settings.

---

## Quality of life

- **Sign commands.** Right click a sign whose text starts with `/` to run it as a command (client or server). Sneak to avoid it.
- **Togglesprint.** `/cyv togglesprint` (`ts`, `sprint`) or a keybind, with an on-screen indicator.
- **Fullbright.** `/cyv fullbright` (`fb`, `bright`).
- **Presets.** Save and switch between **Config presets** (label positions, mod settings) and **Keybind presets** (Minecraft keybinds) in the *Presets* section of the GUI.
- **Raw Input.** Reads raw mouse movement for turn tracking, bypassing OS mouse acceleration.
- **Appearance.** Four color themes (`CYVISPIRIA`, `VIRULIA`, `FLAXEN`, `VERDANT`), two accent colors (`/cyv color1`, `/cyv color2`, list with `/cyv colors`), a custom chat prefix, decimal precision (`/cyv df`) and trimming of trailing zeroes.
- **Discord Rich Presence** shows your CyvForge status in Discord.

---

## Command reference

All commands start with `/cyv` (aliases `/mpk`, `/mm`).

| Command | Description |
|---|---|
| `help [subcommand]` | Usage, aliases and description of a subcommand |
| `gui` | Opens the main GUI |
| `config` (`configuration`, `settings`) | Opens the settings |
| `resetgui` (`guireset`, `resetlabels`) | Resets all HUD elements to their default positions |
| `color1` / `color2 [color]`, `colors` | Sets the accent colors and lists the available ones |
| `df [precision]` (`decimalprecision`, `precision`, `decimals`) | Decimal precision from 0 to 16 |
| `fullbright` (`fb`, `bright`) | Toggles fullbright |
| `togglesprint` (`ts`, `sprint`) | Toggles auto-sprint |
| `setlb`, `clearlb`, `clearpb`, `lb` | Landing block commands |
| `setmm`, `clearmm`, `mm` | Momentum block commands |
| `setcond`, `setbox` | Detection zone and custom landing zone |
| `macro [run \| rec start \| rec stop \| clip]` | Macro editor, playback, recording and clipping |
| `checkpoint`, `getcheckpointgenerator` | Singleplayer checkpoints |
| `inertia`, `positionchecker` | Listeners |
| `airtime`, `height`, `distance`, `calculate`, `simulate`, `setsensitivity`, `optimizesensitivity` | Calculators |

---

## Settings reference

Open the settings with `/cyv config` or from the main GUI.

<details>
<summary><b>General</b></summary>

| Setting | Description |
|---|---|
| Color 1 / Color 2 / Color Theme | Colors used in chat and in the GUIs |
| Chat Prefix | The prefix of CyvForge chat messages (default `Cyv`) |
| Color2 always white in chat | Keep the second color white in chat |
| Decimal Precision / Trim Zeroes | Number formatting |
| Raw Input | Raw mouse input |

</details>

<details>
<summary><b>Landing and momentum</b></summary>

| Setting | Description                                           |
|---|-------------------------------------------------------|
| Send Landing Offset / Send Momentum Offset | Print the offsets in chat after every jump            |
| Highlight Landing Blocks / Highlight Landing Conditions | Render the landing box and the detection zone         |
| Momentum PB Cancelling | Ignore the landing when the momentum block was missed |
| Ignore Wall Hit | Default value of `ignorewall` for new blocks          |

</details>

<details>
<summary><b>Macros</b></summary>

| Setting | Description                                                                                         |
|---|-----------------------------------------------------------------------------------------------------|
| Smooth Macro | Spreads the rotation of each macro tick over the rendered frames instead of applying it all at once |
| Shortcuts Require TAB/ALT | Whether the editor shortcuts need `TAB/ALT`                                                         |
| Macro HUD Color | Color of the `MACRO` indicator: Default, Color1 or Color2                                           |
| Macro Clip / Number of ticks to clip | Enable clipping and set its length (1 to 500 ticks)                                                     |

</details>

<details>
<summary><b>Inertia listener and position checker</b></summary>

| Setting | Description                                                                                                       |
|---|-------------------------------------------------------------------------------------------------------------------|
| Inertia Listener Enabled | Master switch. <br> Options: air tick, min speed, max speed, axis, ground type                                    |
| Position Checker Enabled | Master switch. <br> Options: air tick, radius, *Copy current position with radius*, min and max X / Z, Z Neo Mode |

</details>

<details>
<summary><b>Checkpoints</b></summary>

| Setting | Description |
|---|---|
| Anti-Checkpoint / Anti-CP Delay (s) | Block right clicks for a number of seconds after a good landing |
| Custom Checkpoints Enabled | Enable singleplayer checkpoint items |
| Generator Dye Color / Generator Hotbar Slot | Look and slot of the checkpoint generator |

</details>

Label specific options (Last Timing, Yaw, Last Input, Runtime, Blips, Turn HUD, Keystrokes, Input History) are in each label's own settings (**⋯** in the main GUI).

---

## Keybinds

| Action | Default key |
|---|---|
| Open HUD Position GUI | P |
| Open MPK GUI | Right Shift |
| Run Macro | V |
| Stop Macro | not bound |
| Toggle Sprint | not bound |

All of them can be changed in Minecraft's controls menu under **CyvForge Keys**.

---

## Files

Everything is stored in `.minecraft/config/cyvforge/`.

| File / Folder | Description |
|---|---|
| `macros/` | Saved movement macros (`.json`). Can be opened directly with the **Open Folder** button in `/cyv macro` |
| `presets/configs/` | Saved HUD layout presets (`.json`) and configuration snapshots (`.cfg`) from the Presets menu |
| `presets/keybinds/` | Minecraft keybinding profiles (`.json`) saved in the Keybinds tab of the Presets menu |
| `config.txt` | Main configuration file storing settings, theme, decimal precision, and HUD positions |
| `config.txt.bak` | Automatic backup of `config.txt` created on game launch to prevent data loss |
| `hotkeys.json` | Stored command hotkeys, chain macros, and key combinations from the **Chat Macros** menu |

---

## Community

Questions, bug reports and suggestions are welcome on **[Discord](https://discord.gg/a9qQZdVGZx)**. You can also open an issue here on GitHub.

Release notes are posted on [Discord](https://discord.gg/a9qQZdVGZx) and on the [Modrinth page](https://modrinth.com/mod/cyv).

---

## Credits

- Original CyvForge created by [Morpheye](https://github.com/Morpheye).
- Maintained by [AanyaQ](https://github.com/AanyaQ).
- Based on CyvClient.
- The movement simulator is built on the `mcpk` movement parser included in this project.