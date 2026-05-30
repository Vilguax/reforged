# Reforged

A Fabric mod for **Minecraft 1.21.4** that adds new enchantments. No new visuals — only new
enchanted books and enchantments obtainable from the enchanting table, anvil, and loot.

## Status

Theme: **tools / mining quality-of-life.** Working enchantments:

| Enchantment | Type      | Effect                                                       | Max level |
| ----------- | --------- | ------------------------------------------------------------ | --------- |
| Lifesteal   | Sword/Axe | Heals 10% of damage dealt per level on each hit, plus a 1-heart-per-level burst on kill. Exclusive with the tool enchants below (weapon vs tool). | 3 |
| Auto-Smelt  | Mining tools | Smelts mined drops as they fall (ore→ingot, sand→glass…) using real furnace recipes, grants smelting XP. Exclusive with Silk Touch; stacks with Fortune. | 1 |
| Vein Miner  | Mining tools | Breaks connected ore blocks of the same type in one go. Total mined = 2^level (lvl 1 = 2 … lvl 5 = 32). Ores only (tag `#c:ores`). Durability cost is diminishing per extra block. | 5 |
| Magnet      | Armor     | Sends mined-block drops and killed-mob loot straight to your inventory (overflow drops on the ground). | 1 |
| Auto-Replant| Hoe       | Replants the crop you break (wheat/carrot/potato/beetroot), reserving one seed from the harvest before Auto-Smelt can cook it. | 1 |

The mining enchants **compose**: a hoe/pickaxe with Vein Miner + Auto-Smelt + Fortune, worn with a
Magnet chestplate, will vein-mine a deposit, smelt every drop, and pull the ingots into your
inventory in one swing. The drop pipeline runs in a fixed order each tick:
**Auto-Replant (reserve seed) → Auto-Smelt (smelt the rest) → Magnet (collect)**.

## Tech stack

| Component    | Version          |
| ------------ | ---------------- |
| Minecraft    | 1.21.4           |
| Mod loader   | Fabric Loader 0.16.10 |
| Fabric API   | 0.119.4+1.21.4   |
| Yarn mappings| 1.21.4+build.8   |
| Loom         | 1.9-SNAPSHOT     |
| Gradle       | 8.12 (wrapper)   |
| **JDK**      | **21** (required)|

## Architecture

Since Minecraft 1.21 the enchantment system is **data-driven**. Enchantments are no longer
registered in Java — they are JSON files. Reforged splits responsibilities accordingly:

- **Data (JSON)** — defines each enchantment: cost, weight, anvil cost, supported items,
  equipment slots, and table/loot availability via tags.
  - `src/main/resources/data/reforged/enchantment/<name>.json` — the enchantment definition
  - `src/main/resources/data/minecraft/tags/enchantment/in_enchanting_table.json` — makes it
    appear in the enchanting table
  - `src/main/resources/data/minecraft/tags/enchantment/non_treasure.json` — makes it a normal
    (non-treasure) enchant so it shows up in books and random loot
- **Java** — only the *custom behaviour* that vanilla effect components can't express.
  - `ModEnchantments` — holds a `RegistryKey<Enchantment>` per enchant so code can look it up
  - `effect/LifestealHandler` — listens to Fabric's `ServerLivingEntityEvents.AFTER_DAMAGE` (heal
    per hit) and `AFTER_DEATH` (kill-blow burst) and heals the attacker by the weapon's level
  - `effect/AutoSmeltHandler` — listens to `PlayerBlockBreakEvents.AFTER`, then **defers to the end
    of the tick via `server.execute`** before scanning for fresh drops. This matters: the AFTER
    event fires *before* the block's item drops are spawned into the world, so scanning immediately
    finds nothing. Deferring lets the drops exist; we filter to `getItemAge() == 0` to touch only
    the just-dropped stacks, then run each through the smelting recipe.
  - `effect/VeinMinerHandler` — flood-fills connected same-type ores (26-neighbour) up to `2^level`
    blocks, drops each via `Block.dropStacks` (Fortune-correct) **regrouped at the origin** so the
    drops land in Auto-Smelt/Magnet's scan box and compose with them.
  - `effect/MagnetHandler` — same deferred-scan pattern on block break, plus an `AFTER_DEATH` hook
    for mob loot; inserts fresh drops into the player's inventory.
  - `effect/AutoReplantHandler` — replants any broken crop and reserves one seed from the harvest
    drops in a deferred task **registered before Auto-Smelt** so the seed is claimed before it is
    cooked.
  - `util/EnchantUtil` — shared helper to read an enchant's level off an `ItemStack`.

  The deferred handlers run in registration order (`Reforged#onInitialize`):
  Auto-Replant → Auto-Smelt → Magnet, which is what gives the pipeline its priority.

### Tuning the cost

Cost (the level requirement and lapis price in the enchanting table) lives entirely in the JSON:

- `weight` — how often it's offered (higher = more common; vanilla common enchants ~10, rare ~1–2)
- `min_cost` / `max_cost` — `{ base, per_level_above_first }`, the enchanting-power range in
  which each level can appear
- `anvil_cost` — base cost multiplier when combining in an anvil
- `max_level` — highest level obtainable

## Build & run

> **PATH gotcha:** the machine has Java 8 first on `PATH`. Gradle uses `JAVA_HOME`, which points
> to JDK 21, so the build works regardless. If you build in a fresh shell, make sure
> `JAVA_HOME` points to a JDK 21 install.

```powershell
# build the mod jar -> build/libs/reforged-0.1.0.jar
.\gradlew.bat build

# launch a dev Minecraft client with the mod loaded
.\gradlew.bat runClient
```

## Try it in-game

1. `runClient`, create a creative world.
2. Get an enchanted book or enchant a sword: `/enchant @s reforged:lifesteal 3`.
3. Hit a mob while injured — your health goes up by 30% of the damage dealt (level 3).

## Adding a new enchantment

1. Add a `RegistryKey<Enchantment>` in `ModEnchantments`.
2. Create `data/reforged/enchantment/<name>.json` (cost, items, slots).
3. Add the id to the relevant `data/minecraft/tags/enchantment/*.json` tags.
4. Add a translation key in `assets/reforged/lang/en_us.json`.
5. If it needs custom behaviour, add a handler under `effect/` and register it in `Reforged`.
