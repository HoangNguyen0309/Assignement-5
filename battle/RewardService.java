package battle;

import java.util.List;

import characters.Hero;
import characters.Monster;
import config.GameBalance;

/**
 * Reward distribution helpers for Legends of Valor.
 */
public final class RewardService {

    private RewardService() {}

    /**
     * Distribute rewards to the entire party for killing a monster.
     */
    public static void rewardForMonsterKill(List<Hero> heroes, Monster monster) {
        if (heroes == null || monster == null) return;

        int goldReward = Math.max(GameBalance.GOLD_PER_MONSTER_LEVEL * monster.getLevel(),
                                  GameBalance.GOLD_FALLBACK_PER_MONSTER);
        int xpReward   = Math.max(GameBalance.XP_PER_MONSTER_LEVEL * monster.getLevel(),
                                  GameBalance.XP_FALLBACK_PER_MONSTER);

        for (Hero h : heroes) {
            if (h == null) continue;
            h.addGold(goldReward);
            h.gainExperience(xpReward);
        }
    }

    /**
     * Lightweight end-of-wave reward (optional small bonus).
     */
    public static void rewardEndOfWave(List<Hero> heroes) {
        if (heroes == null) return;
        for (Hero h : heroes) {
            if (h == null) continue;
            h.addGold(GameBalance.END_OF_WAVE_GOLD);
            h.gainExperience(GameBalance.END_OF_WAVE_XP);
        }
    }
}
