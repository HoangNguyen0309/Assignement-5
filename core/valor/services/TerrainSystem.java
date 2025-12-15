package core.valor.services;

import characters.Hero;
import core.Position;
import core.valor.ValorContext;
import world.TileType;

public class TerrainSystem {

    public void apply(ValorContext ctx, Hero hero, Position to) {
        clear(ctx, hero);
        if (to == null) return;

        TileType type = ctx.world.getTile(to.getRow(), to.getCol()).getType();
        int str = 0, dex = 0, agi = 0;

        switch (type) {
            case BUSH:
                dex = Math.max(1, hero.getDexterity() / 10);
                hero.adjustDexterity(dex);
                break;
            case CAVE:
                agi = Math.max(1, hero.getAgility() / 10);
                hero.adjustAgility(agi);
                break;
            case KOULOU:
                str = Math.max(1, hero.getStrength() / 10);
                hero.adjustStrength(str);
                break;
            default:
                return;
        }

        ctx.activeTerrainBuff.put(hero, type);
        ctx.terrainBuffDeltas.put(hero, new int[]{str, dex, agi});
    }

    private void clear(ValorContext ctx, Hero hero) {
        int[] delta = ctx.terrainBuffDeltas.remove(hero);
        if (delta != null) {
            hero.adjustStrength(-delta[0]);
            hero.adjustDexterity(-delta[1]);
            hero.adjustAgility(-delta[2]);
        }
        ctx.activeTerrainBuff.remove(hero);
    }
}
