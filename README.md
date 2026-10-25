# SmartMenus

A powerful, fully-featured GUI plugin for Paper 1.21+ servers. Build complex interactive menus entirely through YAML configuration — no coding required.

## Features

- **Chest GUIs** — 1–6 row chest inventories with fully configurable items
- **Full-screen GUIs** — take over the player's inventory for up to 90 interactive slots
- **Conditions** — show/hide items based on permission, money, placeholder values, world, region, weather, XP, items, and more
- **Actions** — console commands, player commands, messages, sounds, economy transactions, open/close GUIs, and more
- **Click types** — LEFT, RIGHT, SHIFT_LEFT, SHIFT_RIGHT, NUMBER_KEY, DROP, MIDDLE, ANY, and more
- **GUI animations** — config-driven animated borders, loading indicators, blinking icons and frame-by-frame effects
- **Dynamic pagination** — automatically paginate player inventories, ender chests, or online players
- **Viewer/target menus** — staff can open a menu that displays another player's data (e.g. `/enderchestview <player>`)
- **Patterns** — reusable item templates applied to multiple GUIs
- **Navigation** — navigate between menus with full history support
- **Bedrock support** — auto-convert to Floodgate/Geyser forms for Bedrock players
- **In-game editor** — edit GUI items live with `/smartmenus editor`
- **Converters** — import from DeluxeMenus, CommandPanel, and ZMenus
- **Item level system** — upgrade items through configurable tiers
- **Script engine** — run custom scripts inside conditions and actions
- **NPC binding** — open GUIs when players interact with ModeledNPCs
- **Soft dependencies** — Vault, PlaceholderAPI, WorldGuard, LuckPerms, ItemsAdder, Nexo, OreoEssentials, ModeledNPCs, PacketEvents

## Commands

| Command | Description |
|---|---|
| `/smartmenus open <id> [player]` | Open a GUI |
| `/smartmenus reload` | Reload all GUIs |
| `/smartmenus editor` | Open the in-game editor |
| `/smartmenus convert <type>` | Convert menus from another plugin |
| `/smartmenus list` | List all loaded GUIs |

**Aliases:** `/sm`, `/smenu`, `/smenus`, `/smartmenu`, `/gui`

## Permissions

| Permission | Default | Description |
|---|---|---|
| `smartmenus.command` | true | Use `/smartmenus` |
| `smartmenus.open` | true | Open GUIs |
| `smartmenus.reload` | op | Reload GUIs |
| `smartmenus.editor` | op | In-game editor |
| `smartmenus.convert` | op | Convert menus |
| `smartmenus.command.*` | true | Use all GUI commands |
| `smartmenus.*` | op | All permissions |

## Installation

1. Drop `SmartMenus.jar` into your `plugins/` folder
2. Start or restart your server
3. Edit GUIs in `plugins/SmartMenus/guis/`
4. Run `/smartmenus reload`

**Optional:** Install [PacketEvents](https://github.com/retrooper/packetevents) to unlock `PACKET_EVENT` bottom inventory mode.

## Basic GUI Example

```yaml
my_shop:
  title: "&6&lItem Shop"
  rows: 3
  commands: [ "shop", "myshop" ]

  items:
    diamond_item:
      slot: 13
      material: DIAMOND
      name: "&bDiamond"
      lore:
        - "&7Click to buy!"
      actions:
        - "[console] give {player} diamond 1"
        - "[message] &aYou bought a Diamond!"
```

## Full-Screen GUI (Bottom Inventory)

```yaml
full_screen:
  title: "&5Full Screen"
  rows: 6
  commands: [ "fullscreen" ]
  use_bottom_inventory: true
  bottom_inventory_mode: DEFAULT

  bottom_items:
    hotbar_button:
      slot: 27
      material: EMERALD
      name: "&aClick me"
      actions:
        - "[message] &aYou clicked the hotbar slot!"
```

**`bottom_inventory_mode` values:**

| Value | Requires | Description |
|---|---|---|
| `DEFAULT` | Nothing | Saves and restores the player's real inventory on open/close |
| `PACKET_EVENT` | PacketEvents plugin | Shows GUI items client-side only — real inventory is never modified |

## Conditions Example

```yaml
vip_button:
  slot: 4
  material: NETHER_STAR
  name: "&eVIP Only"
  conditions:
    - type: PERMISSION
      value: "group.vip"
      deny-message: "&cYou need VIP!"
  actions:
    - "[message] &aWelcome, VIP!"
  view-requirements:
    - type: PERMISSION
      value: "group.vip"
  else-item:
    material: GRAY_STAINED_GLASS_PANE
    name: "&7VIP Required"
```

## GUI Animations

Animations change the items in a set of slots over time — moving borders, blinking icons,
loading indicators, colour cycles, and more. They are declared in an `animations:` block and
run on the **same per-player update lifecycle** SmartMenus already uses, so an animated menu
does **not** spawn any extra scheduler tasks (no task-per-animation, per-slot, or per-player).
Animation state is created when the menu opens and cleaned up automatically when it closes, so
nothing leaks. Each viewer gets an independent copy of the animation state.

Animated frame items reuse the **exact same item format** as normal items (materials, names,
lore, placeholders, glow, custom items, and even click actions), so there is nothing new to
learn per slot.

```yaml
animated_border:
  title: "&b&l✦ Animated Menu ✦"
  rows: 5
  commands: [ "animatedborder" ]

  # Normal, static clickable items keep working exactly as before.
  items:
    info:
      slot: 22
      material: NETHER_STAR
      name: "&eStatic clickable item"
      glow: true
    close:
      slot: 31
      material: BARRIER
      name: "&cClose"
      close: true

  animations:
    border:
      interval: 8          # ticks each frame is shown (20 ticks = 1 second)
      start-delay: 0       # ticks to wait before the first frame advance
      playback: LOOP       # LOOP | ONCE | REVERSE | PING_PONG
      autoplay: true       # start automatically when the menu opens (default true)
      loops: 0             # number of loops before stopping; 0 = infinite
      frames:
        - slots:
            "0-8,36-44": { material: RED_STAINED_GLASS_PANE,  name: "&8" }
        - slots:
            "0-8,36-44": { material: BLUE_STAINED_GLASS_PANE, name: "&8" }
        - slots:
            "0-8,36-44": { material: LIME_STAINED_GLASS_PANE, name: "&8" }
```

The bundled `/animatedborder` example uses two yellow/orange/red trails moving around
a dark glass border, with static center items and a Close button outside the animated slots.

**Playback modes:**

| Mode | Behaviour |
|---|---|
| `LOOP` | `0,1,…,n-1,0,1,…` — repeats from the start |
| `ONCE` | `0,1,…,n-1` — plays once and stops on the last frame |
| `REVERSE` | plays backwards and loops |
| `PING_PONG` | bounces back and forth between the first and last frame |

**Per-animation options:** `interval` (ticks per frame), `start-delay`, `playback`, `autoplay`,
and `loops`. Each frame may also override the global interval with its own `duration:` (ticks).

**Slot ownership:** while an animation runs it owns the union of every slot referenced across its
frames. Slots that a given frame does not define are cleared for that frame. If two animations
share a slot, the one applied last (registration order) wins — so avoid overlapping animated
regions. Multiple independent animations in one menu (e.g. a `border` plus a `loading` indicator)
run without interfering with each other.

Existing menus that use `update: true` / `update-interval` on individual items keep working
unchanged — animations are purely additive.

### For plugin developers (SmartInvs-level)

The animation engine lives in `com.oreo.animation` and operates on SmartInvs'
`InventoryContents`. If you build menus in Java you can drive it directly from your
`InventoryProvider`:

```java
GuiAnimation border = GuiAnimation.builder()
        .interval(2)
        .playback(AnimationPlaybackMode.LOOP)
        .frame(AnimationFrame.builder().set(0, redPane).build())
        .frame(AnimationFrame.builder().set(0, bluePane).build())
        .build();

// one controller per open menu; advance it from InventoryProvider#update(...)
AnimationController controller = new AnimationController();
controller.register("border", border);
// controller.tick(contents, renderer);  // called each update tick

controller.pause("border");
controller.resume("border");
controller.restart("border");
controller.stop("border");
```

`InventoryProvider#update(...)` continues to work exactly as before; animations are advanced from
that same call.

## Viewer / Target Menus

SmartMenus distinguishes between:

- **viewer** — the player who has the GUI open
- **target** — the player whose data the GUI displays

For normal menus these are the same player, so nothing changes. When a menu command is run with a
player argument, the viewer stays the command sender while data sources such as `PLAYER_ENDERCHEST`
and `PLAYER_INVENTORY` resolve against the target instead.

### Ender Chest Viewer

The bundled `enderchest_viewer` menu (source `PLAYER_ENDERCHEST`) supports this out of the box:

| Command | Result |
|---|---|
| `/enderchestview` | View your **own** ender chest (unchanged behaviour) |
| `/enderchestview <player>` | View **that player's** ender chest (requires the `.others` permission) |

Other players' inventory and ender chest items are read-only: their dynamic item clicks do not
run actions or transfer items, even when `give-item` / `take-item` are enabled. Your own
inventory retains the configured click behavior. `%inventory_click_hint%` supplies the
appropriate ender chest interaction hint.

Aliases: `/ecview`. The target must be **online**. This works for any menu command whose GUI uses a
player-scoped dynamic source — the ender chest is just the primary use case.

**Placeholders:** `%player%` / `{player}` always refer to the viewer (backwards compatible).
`%target_name%`, `%target_uuid%`, `%viewer_name%` and `%viewer_uuid%` are also available for menus
that need to distinguish the two.

**Permissions:**

| Permission | Default | Description |
|---|---|---|
| `smartmenus.command.<gui>` | true | Run a GUI's command for yourself |
| `smartmenus.command.<gui>.others` | op | Open that GUI targeting another player |
| `smartmenus.command.others` | op | Open **any** GUI command targeting another player |

## Requirements

- Paper 1.21.4+
- Java 21+

## Building

```bash
mvn clean package
```

Output: `target/SmartMenus-VERSION.jar`
