package config;

/**
 * Central place for all game formulas and tunable constants.
 */
public final class GameBalance {

    private GameBalance() {}

    // ----------------------------
    // Leveling / Experience
    // ----------------------------

    // XP_needed(level) = XP_BASE * n(n+1)/2, where n = level-1
    public static final int XP_BASE_PER_LEVEL_STEP = 10;

    public static long xpRequiredForLevel(int targetLevel) {
        if (targetLevel <= 1) return 0L;
        long n = targetLevel - 1;
        return XP_BASE_PER_LEVEL_STEP * n * (n + 1) / 2;
    }

    public static final int HERO_LEVELUP_HP_BONUS      = 50;
    public static final int HERO_LEVELUP_MANA_BONUS    = 10;
    public static final int HERO_LEVELUP_STAT_BONUS    = 5;
    public static final int HERO_LEVELUP_FAVORED_BONUS = 5;

    public static final int HERO_INITIAL_FAVORED_BONUS = 50;

    // ----------------------------
    // Combat – heroes
    // ----------------------------

    public static final double HERO_ATTACK_STRENGTH_FACTOR = 0.5;
    public static final double TWO_HAND_BONUS_MULTIPLIER    = 1.5;
    public static final double HERO_DODGE_AGILITY_DIVISOR   = 10.0;

    // ----------------------------
    // Combat – spells
    // ----------------------------

    public static final double SPELL_DEX_DIVISOR   = 10.0;
    public static final int    SPELL_DEBUFF_DIVISOR = 5; // 1/5 = 20%

    // ----------------------------
    // Monsters
    // ----------------------------

    public static final int MONSTER_BASE_HP      = 100;
    public static final int MONSTER_HP_PER_LEVEL = 50;

    public static int monsterHpForLevel(int level) {
        return MONSTER_BASE_HP + MONSTER_HP_PER_LEVEL * level;
    }

    // ----------------------------
    // XP rewards & contribution
    // ----------------------------

    public static final int XP_PER_MONSTER_LEVEL    = 20;
    public static final int XP_FALLBACK_PER_MONSTER = 10;

    public static final double CONTRIBUTION_TAKEN_WEIGHT  = 0.5;
    public static final double CONTRIBUTION_DODGED_WEIGHT = 0.5;

    public static final double REVIVE_HP_FRACTION = 0.5;

    // ----------------------------
    // Loot / market / economy
    // ----------------------------

    public static final int LOOT_POTION_THRESHOLD = 30; // 0–29 => potion
    public static final int LOOT_ITEM_THRESHOLD   = 40; // 30–39 => item

    public static final int MARKET_STOCK_SIZE = 10;

    public static final int MARKET_LEVEL_SAME_THRESHOLD  = 60;
    public static final int MARKET_LEVEL_LOWER_THRESHOLD = 90;

    public static final int SELL_PRICE_PERCENT = 80;

    // ----------------------------
    // World / exploration
    // ----------------------------

    public static final double MAP_MIN_REACHABLE_FRACTION = 0.7;
    public static final int    BATTLE_CHANCE_COMMON_TILE  = 30; // %

    public static final int MOVE_HEAL_AMOUNT = 40;
}
