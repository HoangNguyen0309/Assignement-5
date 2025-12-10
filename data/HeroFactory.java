package data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import characters.Hero;
import characters.HeroType;

public class HeroFactory {

    // 👇 Folder where your text files live (relative to project root / working dir)
    private static final String DATA_DIR = "src/Legends_Monsters_and_Heroes/";

    private static final String WARRIORS_FILE  = DATA_DIR + "Warriors.txt";
    private static final String PALADINS_FILE  = DATA_DIR + "Paladins.txt";
    private static final String SORCERERS_FILE = DATA_DIR + "Sorcerers.txt";

    public List<Hero> loadWarriors() {
        return loadHeroesFromFile(WARRIORS_FILE, HeroType.WARRIOR);
    }

    public List<Hero> loadPaladins() {
        return loadHeroesFromFile(PALADINS_FILE, HeroType.PALADIN);
    }

    public List<Hero> loadSorcerers() {
        return loadHeroesFromFile(SORCERERS_FILE, HeroType.SORCERER);
    }

    private List<Hero> loadHeroesFromFile(String fileName, HeroType type) {
        List<Hero> heroes = new ArrayList<Hero>();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line = br.readLine(); // header
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Name/mana/strength/agility/dexterity/starting money/starting experience
                String[] parts = line.split("\\s+");
                if (parts.length < 7) continue;

                String name = parts[0];
                int mana = Integer.parseInt(parts[1]);
                int strength = Integer.parseInt(parts[2]);
                int agility = Integer.parseInt(parts[3]);
                int dexterity = Integer.parseInt(parts[4]);
                int startingMoney = Integer.parseInt(parts[5]);
                int startingExp = Integer.parseInt(parts[6]);

                int baseHp = baseHpFor(type);

                Hero hero = new Hero(name, type, 1, baseHp, mana,
                        strength, dexterity, agility, startingMoney);

                hero.gainExperience(startingExp);
                heroes.add(hero);
            }
        } catch (IOException e) {
            System.err.println("Could not load heroes from file: " + fileName);
            e.printStackTrace();
        }

        return heroes;
    }

    private int baseHpFor(HeroType type) {
        switch (type) {
            case WARRIOR:  return 400;
            case SORCERER: return 300;
            case PALADIN:  return 350;
            default:       return 300;
        }
    }
}
