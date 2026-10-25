# SmartMenus

A powerful, fully-featured GUI plugin for Paper 1.21+ servers. Build complex interactive menus entirely through YAML configuration — no coding required.

## Features

- **Chest GUIs** — 1–6 row chest inventories with fully configurable items
- **Full-screen GUIs** — take over the player's inventory for up to 90 interactive slots
- **Conditions** — show/hide items based on permission, money, placeholder values, world, region, weather, XP, items, and more
- **Actions** — console commands, player commands, messages, sounds, economy transactions, open/close GUIs, and more
- **Click types** — LEFT, RIGHT, SHIFT_LEFT, SHIFT_RIGHT, NUMBER_KEY, DROP, MIDDLE, ANY, and more
- **Dynamic pagination** — automatically paginate player inventories, ender chests, or online players
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

## Requirements

- Paper 1.21.4+
- Java 21+

## Building

```bash
mvn clean package
```

Output: `target/SmartMenus-VERSION.jar`
