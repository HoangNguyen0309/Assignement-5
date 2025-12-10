package party;

import java.util.ArrayList;
import java.util.List;

import characters.Hero;
import characters.HeroType;
import data.HeroFactory;
import io.InputHandler;
import io.Renderer;

public class PartyBuilder {

    private static final int MAX_HEROES = 3;

    public static List<Hero> buildParty(Renderer renderer,
                                        InputHandler input,
                                        HeroFactory heroFactory) {

        renderer.renderMessage("How many heroes will form your party? (1–" + MAX_HEROES + ")");
        int numHeroes = 0;
        while (numHeroes < 1 || numHeroes > MAX_HEROES) {
            numHeroes = input.readInt();
            if (numHeroes < 1 || numHeroes > MAX_HEROES) {
                renderer.renderMessage("Please enter a number between 1 and " + MAX_HEROES + ".");
            }
        }

        // Load heroes by class from the files
        List<Hero> warriors  = heroFactory.loadWarriors();
        List<Hero> sorcerers = heroFactory.loadSorcerers();
        List<Hero> paladins  = heroFactory.loadPaladins();

        List<Hero> party = new ArrayList<Hero>();

        for (int i = 1; i <= numHeroes; i++) {
            renderer.renderMessage("Choose class for Hero " + i + ":");
            renderer.renderMessage("  1) Warrior");
            renderer.renderMessage("  2) Sorcerer");
            renderer.renderMessage("  3) Paladin");

            int classChoice = 0;
            HeroType chosenType = null;
            List<Hero> pool = null;

            while (chosenType == null) {
                classChoice = input.readInt();
                switch (classChoice) {
                    case 1:
                        chosenType = HeroType.WARRIOR;
                        pool = warriors;
                        break;
                    case 2:
                        chosenType = HeroType.SORCERER;
                        pool = sorcerers;
                        break;
                    case 3:
                        chosenType = HeroType.PALADIN;
                        pool = paladins;
                        break;
                    default:
                        renderer.renderMessage("Please choose 1, 2, or 3.");
                        break;
                }
                if (chosenType != null && (pool == null || pool.isEmpty())) {
                    renderer.renderMessage("No more " + chosenType.getDisplayName() +
                            " heroes available. Choose another class.");
                    chosenType = null;
                }
            }

            // Pick a specific hero from that class
            Hero chosenHero = chooseHeroFromPool(renderer, input, pool, chosenType);
            if (chosenHero == null) {
                renderer.renderMessage("No hero chosen. Skipping slot " + i + ".");
                continue;
            }

            // Optional rename
            renderer.renderMessage("Do you want to rename this hero? (Y/N)");
            boolean rename = input.readYesNo();
            if (rename) {
                renderer.renderMessage("Enter new name:");
                String newName = input.readLine();
                if (newName != null && !newName.trim().isEmpty()) {
                    chosenHero.setName(newName.trim());
                }
            }

            // Add to party and remove from pool so the same hero isn't chosen twice
            party.add(chosenHero);
            pool.remove(chosenHero);
        }

        return party;
    }

    private static Hero chooseHeroFromPool(Renderer renderer,
                                           InputHandler input,
                                           List<Hero> pool,
                                           HeroType type) {
        while (true) {
            renderer.renderMessage("Choose a " + type.getDisplayName() + ":");
            for (int i = 0; i < pool.size(); i++) {
                Hero h = pool.get(i);
                renderer.renderMessage("  " + (i + 1) + ") " + h.getName() +
                        " | Lv " + h.getLevel() +
                        " | MP " + h.getMana() +
                        " | STR " + h.getStrength() +
                        " | DEX " + h.getDexterity() +
                        " | AGI " + h.getAgility() +
                        " | GOLD " + h.getGold());
            }
            renderer.renderMessage("  0) Back");

            int choice = input.readInt();
            if (choice == 0) {
                return null;
            }
            choice--;

            if (choice < 0 || choice >= pool.size()) {
                renderer.renderMessage("Invalid choice. Try again.");
                continue;
            }

            return pool.get(choice);
        }
    }
}
