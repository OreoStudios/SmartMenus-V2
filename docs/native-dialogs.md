# Native Paper dialogs

SmartMenus supports `type: DIALOG` in the normal `guis/` directory, alongside inventory menus. Paper API 1.21.8 is the existing compile target. Java clients need native dialog support; Bedrock translation depends on the installed Geyser version.

Try `/dialogdemo` or open `dialog_demo` using the existing menu commands. See [the bundled example](../src/main/resources/guis/dialog_demo/dialog_demo.yml).

The dialog block supports MULTI_ACTION and CONFIRMATION (exactly two buttons), columns (1–10), close-on-escape, title, external-title, text and item bodies, TEXT, MULTILINE_TEXT, BOOLEAN, SINGLE_OPTION, NUMBER_RANGE, and legacy input aliases. Inputs and buttons can be named maps or lists with explicit keys. Buttons use existing SmartMenus action and condition maps; use OPEN_GUI with gui: another_menu, or BACK for navigation. Input values are available as %key% to actions. Titles and labels support MiniMessage or legacy formatting and PlaceholderAPI.

Open requirements are enforced on open and rechecked on click. Existing action conditions are rechecked on the server, and callbacks are single-use per displayed dialog. Responses are validated for type, length, range and allowed choices. Do not interpolate unrestricted text into privileged console commands: validate it for the exact command and use trusted fixed values for purchases and permissions. The demo performs no purchases.

Inventory-only features (slots, animations, inventory editor, inventory close/open event hooks) do not apply to dialogs. Dialog close via Escape is not a server-side submission event. A full in-game dialog editor is not included.
