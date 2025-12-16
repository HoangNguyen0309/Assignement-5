# OOP Design + Class Relationships (Legends of Valor Refactor)

This document explains the OOP structure and how classes relate after refactoring 
`ValorGameEngine` into phases + controllers + services, and after integrating 
`battle.AttackAction` into `HeroCombatService`.

---

## 1) High-Level Architecture

The game follows a layered design:

- **Engine / Loop Runner**: runs phases in order each round  
- **Context (Model/State)**: stores all game state and shared dependencies  
- **Phases (Orchestration)**: hero phase, monster phase, cleanup/end-of-round, etc.  
- **Controllers (Workflow/UI)**: menus + turn flow  
- **Services/Systems (Domain Logic)**: movement, combat, inventory, market, terrain, 
  monster AI  
- **Rules (Pure Predicates)**: shared validity checks  

---

## 2) Key Classes and Responsibilities

### `core.ValorGameEngine` (or phase runner)

**Responsibility**
- Top-level loop: "each round, run HeroPhase then MonsterPhase then EndRound"
- Owns the phase sequence and the shared `ValorContext`

**Relationships**
- has-a `ValorContext`
- has-a `List<Phase>`
- calls `phase.execute(ctx)` for each phase

---

### `core.valor.ValorContext`

**Responsibility**
- Centralized state container ("single source of truth")
- Stores:
  - `World world`
  - `List<Hero> heroes`, `List<Monster> monsters`
  - `Map<Hero, Position> heroPositions`, `Map<Monster, Position> monsterPositions`
  - spawn positions, laneMaxLevels, terrain buff maps, codes, roundLog
  - shared dependencies: `Renderer`, `InputHandler`, `MarketController`, factories

**Relationships**
- Shared dependency used by phases/controllers/services
- Services/systems read and mutate state inside `ctx`

---

## 3) Phases (Polymorphic Loop Steps)

### `core.valor.phases.Phase`
```java
public interface Phase {
    void execute(ValorContext ctx);
}
```

### `HeroPhase` implements Phase

**Responsibility**
- Runs the hero turn portion of the round

**Relationships**
- has-a `HeroTurnController`
- delegates to `controller.takeTurns(ctx)`

### `MonsterPhase` implements Phase

**Responsibility**
- Runs monster AI portion of the round

**Relationships**
- has-a `MonsterSystem`
- delegates to `monsterSystem.takeTurn(ctx)` (all monsters act)

### `EndRoundPhase` / `RoundSystem`

**Responsibility**
- Respawn fainted heroes
- End-of-round regen (e.g., 10% HP/MP)
- Spawn wave every `monsterWavePeriod`
- Print Round Info summary from `ctx.roundLog`

---

## 4) Controller Layer (Workflow / UI Flow)

### `core.valor.controller.HeroTurnController`

**Responsibility**
- Menu loop per hero:
  - render menu
  - read input
  - decide which service to call

**Relationships**
- has-a services:
  - `HeroMovementService`
  - `HeroCombatService`
  - `HeroInventoryService`
  - `MarketSystem`
- calls service methods to perform actions

---

## 5) Services / Systems (Domain Logic)

### `core.valor.services.HeroCombatService`

**Responsibility**
- Orchestrates combat actions during hero turn:
  - determines valid targets (in-range)
  - delegates execution of attack to `battle.AttackAction`
  - applies Valor-specific rewards (XP/gold)

**Relationships**
- uses `ValorRules.isInRange(...)`
- uses `battle.AttackAction`
- reads `AttackAction.getLastTarget()` and `getLastEffectiveDamage()`
- mutates heroes (XP/gold) and logs to `ctx.log(...)`

### `battle.AttackAction` implements HeroAction

**Responsibility**
- Encapsulates the "Attack" action:
  - show target list
  - choose target via input
  - apply damage
  - render attack message
  - expose last result via getters

**Relationships**
- called by `HeroCombatService`
- uses `Renderer` and `InputHandler`
- mutates `Monster` HP

### `core.valor.services.MonsterSystem`

**Responsibility**
- Monster AI per monster phase:
  - attack if hero in range
  - else pathfind / sidestep around obstacles

**Relationships**
- uses `ValorRules` and `world.isAccessible(...)`
- mutates `ctx.monsterPositions`
- damages heroes

### `core.valor.services.HeroMovementService`

**Responsibility**
- Move / recall / teleport / remove obstacles

**Relationships**
- uses `ValorRules`
- uses `TerrainSystem`
- mutates `ctx.heroPositions` and `ctx.world`

### `core.valor.services.TerrainSystem`

**Responsibility**
- Applies/removes buffs based on tile type

**Relationships**
- mutates hero stats and terrain-buff maps

### `core.valor.services.MarketSystem`

**Responsibility**
- Allows shopping on Hero Nexus tiles

**Relationships**
- uses `MarketController`
- mutates market state

### `core.valor.services.HeroInventoryService`

**Responsibility**
- Equip weapon/armor, use potion

**Relationships**
- mutates hero inventory and equipment

---

## 6) Rules Layer (Shared Predicates)

### `core.valor.services.ValorRules`

**Responsibility**
- Stateless shared checks:
  - board bounds
  - occupancy
  - in-range definition
  - move-past-enemy restriction
  - teleport constraints

**Relationships**
- used by `HeroMovementService`, `HeroCombatService`, `MonsterSystem`

---

## 7) OOP Principles Used

- **Encapsulation**: logic split into focused classes
- **Single Responsibility Principle (SRP)**
- **Composition over inheritance**
- **Polymorphism** (Phase interface)
- **Command / Action pattern** (AttackAction)

---

## 8) Relationship Summary (Arrow Map)
```
ValorGameEngine
  ├── has ValorContext
  └── has List<Phase>
       └── calls execute(ctx)

HeroPhase
  └── has HeroTurnController
       └── has HeroCombatService
            └── uses AttackAction (implements HeroAction)

MonsterPhase
  └── has MonsterSystem

HeroTurnController
  ├── has HeroCombatService
  ├── has HeroMovementService
  ├── has HeroInventoryService
  └── has MarketSystem

Services/Systems
  ├── use ValorRules
  └── mutate state in ValorContext
```

---

## 9) Extensibility

- **New hero actions** → add service or new `HeroAction`
- **New monster AI** → replace or extend `MonsterSystem`
- **New phases** → add new `Phase`
- **New terrain types** → update `TerrainSystem`
- **New UI** → replace `Renderer` / `InputHandler`

---

## 10) LoV Combat Utility

- **Component:** `battle.CombatResolver`  

  **Responsibility:** Wrap existing hero/monster attack and 
  spell casting with terrain-aware damage calculation 
  (Bush/Cave/Koulou buffs). 
  Used by the LoV engine during hero and monster attacks.

## 11) Rewards & Recovery Services

- **Components:** `battle.RewardService`, `battle.BattleSupport`  

  **Responsibility:**  
  - `RewardService`: Distribute gold/XP for monster kills and end-of-wave events in LoV.  
  - `BattleSupport`: Provide revive and end-of-round recovery helpers (HP/MP restoration), invoked in the LoV end-of-round flow.

## 12) LoV Engine Integration

- **Component:** `core.ValorGameEngine`  

  **Responsibility:** Integrate `CombatResolver` for damage, `RewardService` for kill/wave rewards,
  and `BattleSupport` for revives/recovery within the Valor turn loop (hero phase, monster phase, cleanup, end-of-round).

## 13) Monster Selection Logic

- **Component:** `data.MonsterFactory` 

  **Responsibility:** Spawn monsters targeting a requested level (or nearest available level),
  aligning encounters with the heroes’ progression.

## 14) Retreat Action (LoV-only)

- **Components:** `ValorRules` / `HeroMovementService` / `MonsterSystem` / `ValorContext` (immunity tracking)  

  **Responsibility:** Engaged-only retreat option; hero falls back 1 tile, engaged monster advances 1 tile,
  hero heals 15% max HP and gains 1-turn immunity;
  monster attacks honor immunity and decrement it at the end of the monster phase.
  
  - Menu text: "Retreat (fall back one tile, engaged monster advances)".

```java
// core/valor/services/HeroMovementService.retreat (excerpt)
if (!ValorRules.isHeroEngaged(ctx, hero)) return false; // only while engaged
Monster engaged = ValorRules.getEngagedMonster(ctx, hero);

// move hero back one tile (validation omitted here)
ctx.heroPositions.put(hero, dest);
terrain.apply(ctx, hero, dest);
advanceMonster(ctx, pos, engaged); // try to advance the engaged monster

int healAmount = (int) Math.ceil(hero.getMaxHP() * 0.15);
hero.heal(healAmount);
ctx.grantHeroImmunity(hero, 1); // 1-turn immunity checked in MonsterSystem
```

## 15) UI/Prompt Fix

- **Component:** `party.PartyBuilder`  

  **Responsibility:** Correct classic hero selection prompt text for clarity (range 1–3).

## World / Map Layer

### `world.World`

**Responsibility**

- Owns the board grid: `Tile[][] tiles`, board `size`, and world `type` (`"Hero and Monster"` vs `"Valor"`).
- Initializes the map based on `type`:
  - **Hero and Monster**: sets `partyPosition` to bottom-left, randomly fills tiles, then runs a BFS connectivity check; retries up to `MAX_GENERATION_ATTEMPTS`, otherwise falls back to a fully connected map.
  - **Valor**: builds a fixed 3-lane 8×8 layout (walls at columns 2 and 5), places `MonsterNexus` on the top row and `HeroNexus` on the bottom row, then fills each lane with required terrain types.
- Supports classic movement with collision rules via `move(Direction)` (bounds + accessibility check).
- Provides Valor-specific helpers used by engine/services:
  - Bounds/accessibility: `isInside(Position)`, `isAccessible(Position)`
  - Lane logic: `laneIndexForCol(int)`, `sameLane(Position, Position)`, `isLaneWall(int)`
  - Nexus positions: `getHeroNexusForLane(int)`, `getMonsterNexusForLane(int)`, and “two-column per lane” helpers.

**Relationships**

- Stored and shared via `core.valor.ValorContext` as the central board state.
- Depends on `core.Position` and `core.Direction` for coordinates and movement.
- Uses `world.Tile` / `world.TileType` to encode tile semantics; concrete tile classes implement the behavior (`CommonTile`, `MarketTile`, `HeroNexusTile`, etc.).
- Queried by movement/AI/rules (e.g., services call `isAccessible`, `sameLane`, lane index helpers) and mutated when terrain/obstacles change.

```text
Valor lane columns (8×8):
[0,1] | wall(2) | [3,4] | wall(5) | [6,7]
Row 0: Monster Nexus tiles (two per lane)
Row 7: Hero Nexus tiles (two per lane)
```

------

### `world.Tile` (interface)

**Responsibility**

- Minimal contract for all tiles:
  - `boolean isAccessible()`
  - `TileType getType()`

**Relationships**

- Implemented by all tile classes; consumed by `World` and higher-level services to decide movement, terrain effects, and special locations.

------

### `world.TileType` (enum)

**Responsibility**

- Defines all tile categories across both modes:
  - Hero & Monster: `INACCESSIBLE`, `MARKET`, `COMMON`
  - Valor: `HERO_NEXUS`, `MONSTER_NEXUS`, `BUSH`, `CAVE`, `KOULOU`, `OBSTACLE`

**Relationships**

- Returned by `Tile.getType()` and used as the primary “switch key” for rules, rendering, terrain buffs, and special-tile checks.

------

### `world.AbstractTile`

**Responsibility**

- Shared base implementation for most tiles:
  - Stores `TileType type` and `boolean accessible`
  - Implements `isAccessible()` and `getType()`
  - Allows toggling accessibility via `setAccessible(boolean)`

**Relationships**

- Extended by tiles with simple behavior: `CommonTile`, `InaccessibleTile`, `MonsterNexusTile`, `ObstacleTile`.

------

### `world.CommonTile` extends `AbstractTile`

**Responsibility**

- Represents “normal/terrain” tiles.
- Supports types: `COMMON`, `BUSH`, `CAVE`, `KOULOU`, `OBSTACLE`.
- Encodes obstacle blocking rule: `OBSTACLE` tiles start as not accessible.

**Relationships**

- Heavily used by `World` generation (both classic and Valor lane filling).
- Tile type is used by terrain/buff systems (e.g., applying BUSH/CAVE/KOULOU effects).

------

### `world.InaccessibleTile` extends `AbstractTile`

**Responsibility**

- Permanent blocked tile: type `INACCESSIBLE`, not accessible.

**Relationships**

- Used in classic random generation and as fixed lane walls in Valor (columns 2 and 5).

------

### `world.MarketTile` implements `Tile`

**Responsibility**

- A passable market location (always accessible).
- Holds an optional `market.Market` instance that can be set later (`getMarket()`, `setMarket()`).

**Relationships**

- Used in classic mode as shop locations.
- Base class for `HeroNexusTile` in Valor (so nexus can also act as a shop location).

------

### `world.HeroNexusTile` extends `MarketTile`

**Responsibility**

- Valor hero base tile; overrides `getType()` to return `HERO_NEXUS` (while remaining accessible like a market tile).

**Relationships**

- Checked by Valor gameplay logic to gate shopping/recall behavior and other nexus-only actions.

------

### `world.MonsterNexusTile` extends `AbstractTile`

**Responsibility**

- Valor monster base tile: type `MONSTER_NEXUS`, accessible.

**Relationships**

- Placed by Valor world generator on the top row; used by win/advance conditions and AI/targeting logic at higher layers.

------

### `world.ObstacleTile` extends `AbstractTile`

**Responsibility**

- Explicit obstacle tile: type `OBSTACLE`, initially not accessible.
- Supports clearing: `removeObstacle()` sets the tile to accessible.

**Relationships**

- Mutated by movement/utility actions that remove obstacles; `World.isAccessible(...)` will reflect the updated passability.
