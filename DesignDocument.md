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
