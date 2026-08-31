# Minimalist

Hide the scenery you can't interact with. Curated, per-content toggles remove decorative
objects, wandering NPCs, projectiles, and HUD clutter so only the things that matter stay
on screen.

No ID inputs, every toggle maps to hardcoded, human-reviewed ID sets sourced from the
game cache, and nothing is ever removed from the scene: hiding happens at the renderer,
so hidden objects stay hoverable, clickable, and visible to other plugins.

## Supported content

### Guardians of the Rift

| Toggle | Hides |
|---|---|
| Abyss scenery | Whale-fall, kelp, lace, fossils, statues, and the rest of the abyss backdrop |
| Inactive guardian statues | All guardian statues except the two active ones and any whose portal talisman you hold, visibility follows the altar rotation instantly, and hidden statues also lose their menu entries so you can't misclick them |
| Guardian remains | Small guardian parts and depleted remains; Large/Huge/Fallen (mineable) always show |
| Essence piles | Elemental and catalytic essence piles |
| Barriers and cells | Barriers (both width variants), the weak cells table, and the guides |
| Barrier hitsplats | The hitsplats and health bars drawn on the barriers |
| Altar scenery | Decoration inside all twelve runecrafting altars, pillars, glows, rubble, plants, corpses, ghosts, region-gated so these shared world IDs never disappear anywhere else |
| Entrance scenery | Lobby pillars, rubble, skeleton, cart, and fountain (never the agility shortcut) |
| Rain | The rain effect inside the temple |
| Abyssal creatures | Abyssal guardians, walkers, and leeches |
| Summoned guardians | Catalytic and elemental guardians summoned by players |
| Apprentices | Apprentices Tamara, Cordelia, and Felix |
| Rick | Rick |
| Projectiles | Projectiles from creatures attacking the barriers and guardian |
| HUD elements | Portal timer, guardian counter, portal location text |
| Other players | Other players, their 2D elements, and their pets, with a Show friends exemption |

### Blast Furnace

| Toggle | Hides |
|---|---|
| Operator dwarves | Dumpy, Stumpy, Pumpy, Numpty, and Thumpy working the machinery |
| Merchants | Ordan and Jorzik; they stay clickable where they stand |
| Delivery miners | The Dwarven Miners restocking Ordan's shop |
| Machinery | Cogs, pipes, gear box, and drive belt; broken machinery always shows so repairs are never missed |
| Smoke | Smoke from the furnace machinery |
| Manual equipment | Pedals, pump, stove, coke, and the temperature gauge, needed on non-official worlds and used for niche Strength, Agility, and Firemaking training |
| Coffer | The coins coffer, only needed on official worlds |
| Sink | The Fill-bucket sink, unnecessary with ice or smiths gloves |
| Other players | Other players, their 2D elements, and their pets, with a Show friends exemption |

The conveyor belt, bar dispenser, bars, melting pot, and the Blast Furnace Foreman are
never hideable.

## How it works

- **A GPU renderer is required for scenery hiding**, the GPU plugin (default), GPU with
  region locker, or 117HD all work; the software renderer never consults the render
  callback, so scenery cannot be hidden on it (the plugin tells you in chat if this
  applies). NPC, player, projectile, statue, and HUD hiding work on any renderer.
- All hiding goes through RuneLite's `RenderCallback`: static scenery is filtered when the
  scene uploads (toggle changes apply with one quick reload), and animated objects, the
  guardian statues, are filtered per frame, so active/inactive changes show instantly.
- Active altars are read from the game's own HUD update script, the same source the
  in-game HUD uses.
- Everything functional is deliberately excluded: mineable remains, the passable barrier,
  agility shortcuts, ladders, altars, portals, and exit markers always render.

## Troubleshooting

Something hidden that should not be, or the other way around? Type `::minimalist` in the
chat and attach `.runelite/minimalist-scene-report.txt` to a GitHub issue. It lists every
object in the loaded scene and what the plugin decided about it, and it never leaves your
computer unless you send it.

## Code layout

- `MinimalistPlugin`, a content-agnostic dispatcher over the supported areas
- `content/`, the `ContentArea` contract plus the shared helpers for player hiding,
  ID set assembly, and menu stripping
- `content/gotr/`, arena scenery, NPCs, HUD components, and guardian statue data
- `content/altars/`, one file per runecrafting altar, plus shared altar decoration and
  the scene-to-altar resolution
- `content/blastfurnace/`, the Blast Furnace room data and rules
- `diagnostics/`, the `::minimalist` scene report

## License

BSD 2-Clause
