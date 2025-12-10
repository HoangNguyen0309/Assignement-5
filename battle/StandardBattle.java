package battle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;

import characters.Hero;
import characters.Monster;
import config.GameBalance;
import data.ItemFactory;
import io.InputHandler;
import io.Renderer;
import items.Armor;
import items.Item;
import items.Potion;
import items.Spell;
import items.Weapon;
import items.Inventory;

public class StandardBattle implements Battle {

    public static class BattleContext {
        private List<Monster> monsters;
        private Map<Hero, HeroContribution> contributions;
        private StandardBattle parent;

        public BattleContext(StandardBattle parent,
                             List<Monster> monsters,
                             Map<Hero, HeroContribution> contributions) {
            this.parent = parent;
            this.monsters = monsters;
            this.contributions = contributions;
        }

        public void addDamageDealt(Hero hero, int amount) {
            HeroContribution hc = contributions.get(hero);
            if (hc != null) hc.damageDealt += amount;
        }

        public void addDamageTaken(Hero hero, int amount) {
            HeroContribution hc = contributions.get(hero);
            if (hc != null) hc.damageTaken += amount;
        }

        public void addDamageDodged(Hero hero, int amount) {
            HeroContribution hc = contributions.get(hero);
            if (hc != null) hc.damageDodged += amount;
        }

        public void removeDeadMonsters() {
            parent.removeDeadMonsters();
        }
    }

    private static class HeroContribution {
        int damageDealt;
        int damageTaken;
        int damageDodged;
    }

    private List<Hero> heroes;
    private List<Monster> monsters;
    private Renderer renderer;
    private InputHandler input;
    private Random rand;
    private ItemFactory itemFactory;

    private Map<Hero, HeroContribution> contributions;
    private Map<Hero, Integer> xpGained;

    public StandardBattle(List<Hero> heroes,
                          List<Monster> monsters,
                          Renderer renderer,
                          InputHandler input,
                          ItemFactory itemFactory) {
        this.heroes = heroes;
        this.monsters = monsters;
        this.renderer = renderer;
        this.input = input;
        this.itemFactory = itemFactory;
        this.rand = new Random();
        this.contributions = new HashMap<Hero, HeroContribution>();
        this.xpGained = new HashMap<Hero, Integer>();

        for (Hero h : heroes) {
            contributions.put(h, new HeroContribution());
            xpGained.put(h, 0);
        }
    }

    public void start() {
        renderer.renderMessage("A battle begins!");

        BattleContext ctx = new BattleContext(this, monsters, contributions);

        while (hasLivingHeroes() && hasLivingMonsters()) {
            // Heroes' turn
            for (Hero hero : heroes) {
                if (hero.isFainted()) continue;
                heroTurn(hero, ctx);
                if (!hasLivingMonsters()) break;
            }

            if (!hasLivingMonsters()) break;

            // Monsters' turn
            monstersTurn(ctx);
        }

        if (hasLivingHeroes()) {
            renderer.renderMessage("Heroes have won the battle!");
            rewardHeroes();
        } else {
            renderer.renderMessage("Heroes have been defeated...");
        }
    }

    private void heroTurn(Hero hero, BattleContext ctx) {
        renderer.renderHeroStats(heroes);
        renderer.renderMonsterStats(monsters);

        List<HeroAction> actions = new ArrayList<HeroAction>();
        actions.add(new AttackAction());
        actions.add(new CastSpellAction());
        actions.add(new SkipAction());

        renderer.renderMessage(hero.getName() + ", choose action:");
        for (int i = 0; i < actions.size(); i++) {
            renderer.renderMessage("  " + (i+1) + ") " + actions.get(i).getName());
        }

        int choice = input.readInt();
        if (choice < 1 || choice > actions.size()) {
            renderer.renderMessage("Invalid choice, skipping turn.");
            return;
        }

        HeroAction action = actions.get(choice - 1);
        action.execute(hero, heroes, monsters, renderer, input, ctx);
    }

    private void monstersTurn(BattleContext ctx) {
        for (Monster monster : monsters) {
            if (monster.isFainted()) continue;
            Hero target = chooseRandomLivingHero();
            if (target == null) break;

            int rawDamage = monster.getDamage();
            int reducedDamage = rawDamage - target.getArmorReduction();
            if (reducedDamage < 0) reducedDamage = 0;

            if (target.tryDodge()) {
                ctx.addDamageDodged(target, reducedDamage);
                renderer.renderMessage(target.getName() +
                        " dodged the attack from " + monster.getName() + "!");
                continue;
            }

            target.takeDamage(reducedDamage);
            ctx.addDamageTaken(target, reducedDamage);

            renderer.renderMessage(monster.getName() + " attacked " +
                    target.getName() + " for " + reducedDamage + " damage.");
        }
    }

    // ---------------------- Rewards & summary ----------------------

    private void rewardHeroes() {
        int totalXp = 0;
        for (Monster m : monsters) {
            totalXp += m.getLevel() * GameBalance.XP_PER_MONSTER_LEVEL;
        }
        if (totalXp <= 0) {
            totalXp = GameBalance.XP_FALLBACK_PER_MONSTER * monsters.size();
        }

        double totalScore = 0.0;
        Map<Hero, Double> scores = new HashMap<Hero, Double>();
        for (Hero h : heroes) {
            if (h.isFainted()) continue;
            HeroContribution hc = contributions.get(h);
            double score = hc.damageDealt
                    + GameBalance.CONTRIBUTION_TAKEN_WEIGHT * hc.damageTaken
                    + GameBalance.CONTRIBUTION_DODGED_WEIGHT * hc.damageDodged;
            if (score <= 0.0) score = 1.0;
            scores.put(h, score);
            totalScore += score;
        }

        if (totalScore <= 0.0) {
            int livingCount = 0;
            for (Hero h : heroes) {
                if (!h.isFainted()) livingCount++;
            }
            if (livingCount == 0) return;
            int perHero = totalXp / livingCount;
            for (Hero h : heroes) {
                if (!h.isFainted()) {
                    h.gainExperience(perHero);
                    xpGained.put(h, xpGained.get(h) + perHero);
                    renderer.renderMessage(h.getName() + " gains " + perHero + " XP.");
                }
            }
        } else {
            for (Hero h : heroes) {
                if (h.isFainted()) continue;
                double frac = scores.get(h) / totalScore;
                int xp = (int)Math.round(totalXp * frac);
                if (xp < 1) xp = 1;
                h.gainExperience(xp);
                xpGained.put(h, xpGained.get(h) + xp);
                HeroContribution hc = contributions.get(h);
                renderer.renderMessage(h.getName() +
                        " dealt " + hc.damageDealt +
                        ", tanked " + hc.damageTaken +
                        ", dodged " + hc.damageDodged +
                        " -> gains " + xp + " XP.");
            }
        }

        maybeDropLoot(scores);
        printBattleSummary();
        reviveFaintedHeroes();
    }

    private void maybeDropLoot(Map<Hero, Double> scores) {
        if (scores.isEmpty()) return;

        int roll = rand.nextInt(100);
        if (roll >= GameBalance.LOOT_ITEM_THRESHOLD) {
            return;
        }

        boolean dropPotion = (roll < GameBalance.LOOT_POTION_THRESHOLD);

        Hero bestHero = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Map.Entry<Hero, Double> e : scores.entrySet()) {
            if (e.getValue() > bestScore) {
                bestScore = e.getValue();
                bestHero = e.getKey();
            }
        }
        if (bestHero == null) return;

        int avgLevel = averageHeroLevel();

        if (dropPotion) {
            Potion p = itemFactory.getRandomPotionForLevel(avgLevel);
            if (p != null) {
                bestHero.getInventory().add(p);
                renderer.renderMessage(bestHero.getName() +
                        " receives a potion: " + p.getName());
            }
        } else {
            Item item = getRandomWeaponOrArmor(avgLevel);
            if (item != null) {
                bestHero.getInventory().add(item);
                renderer.renderMessage(bestHero.getName() +
                        " receives an item: " + item.getName());
            }
        }
    }

    private Item getRandomWeaponOrArmor(int avgLevel) {
        int which = rand.nextInt(2);
        if (which == 0) {
            return itemFactory.getRandomWeaponForLevel(avgLevel);
        } else {
            return itemFactory.getRandomArmorForLevel(avgLevel);
        }
    }

    private int averageHeroLevel() {
        if (heroes.isEmpty()) return 1;
        int sum = 0;
        for (Hero h : heroes) sum += h.getLevel();
        return sum / heroes.size();
    }

    private void printBattleSummary() {
        renderer.renderMessage("=== Battle Summary ===");
        String header = String.format("%-12s %7s %7s %7s %9s",
                "Hero", "Dealt", "Tanked", "Dodged", "XP Gain");
        renderer.renderMessage(header);
        renderer.renderMessage("-----------------------------------------------");

        for (Hero h : heroes) {
            HeroContribution hc = contributions.get(h);
            int gained = xpGained.containsKey(h) ? xpGained.get(h) : 0;
            String row = String.format("%-12s %7d %7d %7d %9d",
                    h.getName(),
                    hc.damageDealt,
                    hc.damageTaken,
                    hc.damageDodged,
                    gained);
            renderer.renderMessage(row);
        }
    }

    private void reviveFaintedHeroes() {
        for (Hero h : heroes) {
            if (h.isFainted()) {
                int revivalHp = (int)(h.getMaxHP() * GameBalance.REVIVE_HP_FRACTION);
                h.heal(revivalHp);
                renderer.renderMessage(h.getName() +
                        " is revived to " + h.getHP() + " HP!");
            }
        }
    }

    // ---------------------- Helpers ----------------------

    private boolean hasLivingHeroes() {
        for (Hero hero : heroes) {
            if (!hero.isFainted()) return true;
        }
        return false;
    }

    private boolean hasLivingMonsters() {
        for (Monster monster : monsters) {
            if (!monster.isFainted()) return true;
        }
        return false;
    }

    private Hero chooseRandomLivingHero() {
        List<Hero> living = new ArrayList<Hero>();
        for (Hero h : heroes) {
            if (!h.isFainted()) living.add(h);
        }
        if (living.isEmpty()) return null;
        return living.get(rand.nextInt(living.size()));
    }

    private void removeDeadMonsters() {
        Iterator<Monster> it = monsters.iterator();
        while (it.hasNext()) {
            Monster m = it.next();
            if (m.isFainted()) {
                it.remove();
            }
        }
    }
}
