package items;

import characters.Hero;
import characters.Monster;

public interface SpellEffect {
    void apply(Hero caster, Monster target, int rawDamage);
    String describe();
}
