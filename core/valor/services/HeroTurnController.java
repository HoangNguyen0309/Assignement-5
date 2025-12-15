package core.valor.ui;

import characters.Hero;
import core.valor.ValorContext;
import core.valor.ValorSupport;
import core.valor.services.HeroCombatService;
import core.valor.services.HeroInventoryService;
import core.valor.services.HeroMovementService;
import core.valor.services.MarketSystem;
import core.valor.services.TerrainSystem;

public class HeroTurnController {

    private final TerrainSystem terrain = new TerrainSystem();
    private final HeroMovementService movement = new HeroMovementService(terrain);
    private final HeroCombatService combat = new HeroCombatService();
    private final HeroInventoryService inventory = new HeroInventoryService();
    private final MarketSystem market = new MarketSystem();

    public void takeTurns(ValorContext ctx) {
        assignHeroCodes(ctx);

        for (Hero hero : ctx.heroes) {
            if (hero.isFainted()) continue;
            if (!ctx.heroPositions.containsKey(hero)) continue;

            boolean actionTaken = false;
            boolean showBoard = true;

            while (!actionTaken && !ctx.gameOver) {
                renderMenu(ctx, hero, showBoard);
                int choice = ctx.input.readInt();

                switch (choice) {
                    case 1: actionTaken = movement.move(ctx, hero); break;
                    case 2: actionTaken = combat.attack(ctx, hero); break;
                    case 3: actionTaken = combat.castSpell(ctx, hero); break;
                    case 4: actionTaken = inventory.openInventory(ctx, hero); break;
                    case 5: actionTaken = movement.recall(ctx, hero); break;
                    case 6: market.openShopIfAtHeroNexus(ctx, hero); break; // free
                    case 7: actionTaken = movement.teleport(ctx, hero); break;
                    case 8: actionTaken = movement.removeObstacle(ctx, hero); break;
                    case 9:
                        ctx.renderer.renderHeroStats(ctx.heroes, ctx.heroCodes);
                        ctx.renderer.renderMonsterStats(ctx.monsters, ctx.monsterCodes);
                        break;
                    case 10:
                        ctx.renderer.renderMessage(hero.getName() + " skips the turn.");
                        actionTaken = true;
                        break;
                    default:
                        ctx.renderer.renderMessage("Invalid choice.");
                        break;
                }

                if (!actionTaken) showBoard = false;
            }

            if (ctx.gameOver) return;
        }
    }

    private void renderMenu(ValorContext ctx, Hero hero, boolean showBoard) {
        ValorSupport.renderHeroTurnMenu(ctx, hero, showBoard);
    }

    private void assignHeroCodes(ValorContext ctx) {
        ctx.heroCodes.clear();
        int idx = 1;
        for (Hero h : ctx.heroes) {
            ctx.heroCodes.put(h, "h" + idx++);
        }
    }
}
