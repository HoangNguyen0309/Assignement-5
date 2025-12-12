package battle;

import characters.Hero;
import characters.Monster;
import config.GameBalance;
import items.Spell;
import world.TileType;

/**
 * Lightweight combat helper for Legends of Valor.
 * Wraps existing damage/dodge/armor logic and only adds simple terrain buffs.
 */
public final class CombatResolver {

    private CombatResolver() {}

    /**
     * Hero basic attack against a monster, with terrain bonus applied before monster defense.
     * @return actual HP lost by the target
     */
    public static int computeHeroAttackDamage(Hero hero, Monster target, TileType tileType) {
        if (hero == null || target == null) return 0;

        double multiplier = terrainAttackBonus(tileType, true);
        int baseDamage = hero.basicAttackDamage();
        int raw = (int) Math.round(baseDamage * multiplier);

        int hpBefore = target.getHP();
        target.takeDamage(raw); // Monster handles its own defense
        return Math.max(0, hpBefore - target.getHP());
    }

    /**
     * Hero casts a spell on a monster, terrain bonus applied on top of the spell's raw damage.
     * @return actual HP lost by the target
     */
    public static int computeSpellDamage(Hero hero, Monster target, Spell spell, TileType tileType) {
        if (hero == null || target == null || spell == null) return 0;

        double multiplier = terrainAttackBonus(tileType, true);
        int hpBefore = target.getHP();

        // spell.cast already applies base damage + dex bonus and any spell effect
        int rawDealt = spell.cast(hero, target);

        // Apply extra terrain-based damage, if any
        int bonusDamage = 0;
        if (multiplier > 1.0) {
            bonusDamage = (int) Math.round(rawDealt * (multiplier - 1.0));
            target.takeDamage(bonusDamage); // Monster defense still applies
        }

        return Math.max(0, hpBefore - target.getHP());
    }

    /**
     * Monster attack against a hero, considering terrain buffs and hero armor/dodge.
     * @return actual HP lost by the hero
     */
    public static int computeMonsterAttackDamage(Monster monster, Hero target, TileType tileType) {
        if (monster == null || target == null) return 0;

        // Monster gains attack in Koulou; hero gains protection in Bush/Cave by reducing final damage
        double offenseMul = terrainAttackBonus(tileType, false);
        double defenseMul = terrainDefenseBonus(tileType);

        int raw = (int) Math.round(monster.getDamage() * offenseMul);

        if (target.tryDodge()) {
            return 0;
        }

        int reduced = raw - target.getArmorReduction();
        if (reduced < 0) reduced = 0;

        reduced = (int) Math.round(reduced * defenseMul);

        int hpBefore = target.getHP();
        target.takeDamage(reduced);
        return Math.max(0, hpBefore - target.getHP());
    }

    // -------------------------------------------------------------
    // Terrain helpers
    // -------------------------------------------------------------

    private static double terrainAttackBonus(TileType tileType, boolean isHeroAttacker) {
        if (tileType == null) return 1.0;
        switch (tileType) {
            case KOULOU:
                // Strength/attack boost
                return GameBalance.TERRAIN_BUFF_MULTIPLIER;
            case BUSH:
                // Agility/dex related (help heroes hit/cast effectively)
                return isHeroAttacker ? GameBalance.TERRAIN_BUFF_MULTIPLIER : 1.0;
            default:
                return 1.0;
        }
    }

    private static double terrainDefenseBonus(TileType tileType) {
        if (tileType == null) return 1.0;
        switch (tileType) {
            case BUSH:
            case CAVE:
                // Evasion / defense flavor: reduce incoming damage a bit
                return GameBalance.TERRAIN_DEFENSE_MULTIPLIER;
            default:
                return 1.0;
        }
    }
}
