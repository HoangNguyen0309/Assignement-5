package items;

import characters.Hero;
import characters.Monster;
import config.GameBalance;

public class IceSpellEffect implements SpellEffect {

    @Override
    public void apply(Hero caster, Monster target, int rawDamage) {
        int dmgReduction = Math.max(1, target.getDamage() / GameBalance.SPELL_DEBUFF_DIVISOR);
        target.reduceDamage(dmgReduction);
    }

    @Override
    public String describe() {
        return "reduces target's damage";
    }
}
