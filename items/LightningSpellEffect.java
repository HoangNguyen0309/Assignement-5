package items;

import characters.Hero;
import characters.Monster;
import config.GameBalance;

public class LightningSpellEffect implements SpellEffect {
    @Override
    public void apply(Hero caster, Monster target, int rawDamage) {
        int dodgeReduction = Math.max(1, target.getDodgeChance() / GameBalance.SPELL_DEBUFF_DIVISOR);
        target.reduceDodgeChance(dodgeReduction);
    }

    @Override
    public String describe() {
        return "reduces target's dodge chance";
    }
}
