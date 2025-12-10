package items;

import characters.Hero;
import characters.Monster;
import config.GameBalance;

public class Spell extends AbstractItem implements Consumable {

    public enum SpellType {
        FIRE,
        ICE,
        LIGHTNING
    }

    private int baseDamage;
    private int manaCost;
    private SpellType type;
    private SpellEffect effect;

    public Spell(String name, int price, int requiredLevel,
                 int baseDamage, int manaCost, SpellType type, SpellEffect effect) {
        super(name, price, requiredLevel);
        this.baseDamage = baseDamage;
        this.manaCost = manaCost;
        this.type = type;
        this.effect = effect;
    }

    public int getBaseDamage() {
        return baseDamage;
    }

    public int getManaCost() {
        return manaCost;
    }

    public SpellType getType() {
        return type;
    }

    public SpellEffect getEffect() {
        return effect;
    }

    public int cast(Hero caster, Monster target) {
        int rawDamage = baseDamage + (int)(caster.getDexterity() / GameBalance.SPELL_DEX_DIVISOR);
        // Let Monster defense handle reduction; pass raw
        target.takeDamage(rawDamage);
        if (effect != null) {
            effect.apply(caster, target, rawDamage);
        }
        return rawDamage;
    }

    @Override
    public void consume(Hero hero) {
        // Spell consumption is handled in battle (spell is removed from inventory after cast).
        // No-op here.
    }
}
