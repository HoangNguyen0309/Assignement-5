package items;

import characters.Hero;
import characters.Monster;
import config.GameBalance;

public class FireSpellEffect implements SpellEffect {
    @Override
    public void apply(Hero caster, Monster target, int rawDamage) {
        int defReduction = Math.max(1, target.getDefense() / GameBalance.SPELL_DEBUFF_DIVISOR);
        target.reduceDefense(defReduction);
    }

    @Override
    public String describe() {
        return "reduces target's defense";
    }
}
