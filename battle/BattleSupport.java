package battle;

import java.util.List;

import characters.Character;
import characters.Hero;

/**
 * Small utilities for revival and end-of-round recovery.
 */
public final class BattleSupport {

    private BattleSupport() {}

    public static boolean isDead(Character c) {
        return c == null || c.isFainted();
    }

    public static void fullyRestore(Hero hero) {
        if (hero == null) return;
        hero.heal(hero.getMaxHP());
        hero.restoreMana(hero.getMaxMana());
    }

    public static void recoverHeroesEndOfRound(List<Hero> heroes, double fraction) {
        if (heroes == null || fraction <= 0) return;
        for (Hero h : heroes) {
            if (h == null || h.isFainted()) continue;
            h.heal((int) (h.getMaxHP() * fraction));
            h.restoreMana((int) (h.getMaxMana() * fraction));
        }
    }
}
