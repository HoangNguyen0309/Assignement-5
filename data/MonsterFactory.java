package data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import characters.Monster;
import config.GameBalance;


public class MonsterFactory {

    // Folder where your text files live (as in your screenshot)
    private static final String DATA_DIR = "Legends_Monsters_and_Heroes/";

    private static final String DRAGONS_FILE      = DATA_DIR + "Dragons.txt";
    private static final String EXOSKELETONS_FILE = DATA_DIR + "Exoskeletons.txt";
    private static final String SPIRITS_FILE      = DATA_DIR + "Spirits.txt";

    private final Random random = new Random();

    private boolean loaded = false;

    private final List<MonsterTemplate> templates = new ArrayList<MonsterTemplate>();

    // Simple template so we can spawn fresh Monster instances each battle
    private static class MonsterTemplate {
        String name;
        int level;
        int damage;
        int defense;
        int dodgeChance;

        MonsterTemplate(String name, int level, int damage,
                        int defense, int dodgeChance) {
            this.name = name;
            this.level = level;
            this.damage = damage;
            this.defense = defense;
            this.dodgeChance = dodgeChance;
        }
    }

    private void ensureLoaded() {
        if (loaded) return;
        loadFile(DRAGONS_FILE);
        loadFile(EXOSKELETONS_FILE);
        loadFile(SPIRITS_FILE);
        loaded = true;
    }

    /**
     * Load monsters from a file with header:
     * Name/level/damage/defense/dodge chance
     */
    private void loadFile(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line = br.readLine(); // header
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length < 5) continue;

                String name = parts[0];
                int level = Integer.parseInt(parts[1]);
                int damage = Integer.parseInt(parts[2]);
                int defense = Integer.parseInt(parts[3]);
                int dodgeChance = Integer.parseInt(parts[4]);

                templates.add(new MonsterTemplate(name, level, damage, defense, dodgeChance));
            }
        } catch (IOException e) {
            System.err.println("Could not load monsters from file: " + fileName);
            e.printStackTrace();
        }
    }

    /**
     * Spawn monsters for a battle in a way that is "fair":
     *  - Use monsters whose level is close to the party's average level.
     *  - Start with range [avg-1, avg+1]; if empty, widen gradually.
     */
    public List<Monster> spawnMonstersForBattle(int count, int targetLevel) {
        ensureLoaded();

        /**
        if (templates.isEmpty()) {
            // Fallback – shouldn't happen unless files are missing
            List<Monster> fallback = new ArrayList<Monster>();
            for (int i = 0; i < count; i++) {
                fallback.add(new Monster("Slime", partyAverageLevel,
                        computeHp(partyAverageLevel), 20, 5, 5));
            }
            return fallback;
        }
         **/

        int avg = Math.max(1, targetLevel);

        // Gradually widen the allowed level band until we have candidates.
        List<MonsterTemplate> candidates = new ArrayList<MonsterTemplate>();
        int band = 1;
        while (candidates.isEmpty() && band <= 10) {
            int minLevel = Math.max(1, avg - band);
            int maxLevel = avg + band;

            candidates.clear();
            for (MonsterTemplate t : templates) {
                if (t.level >= minLevel && t.level <= maxLevel) {
                    candidates.add(t);
                }
            }
            band++;
        }

        // Still empty? Use everything.
        if (candidates.isEmpty()) {
            candidates.addAll(templates);
        }

        List<Monster> monsters = new ArrayList<Monster>();
        for (int i = 0; i < count; i++) {
            MonsterTemplate t = candidates.get(random.nextInt(candidates.size()));
            monsters.add(instantiateMonster(t, targetLevel));
        }
        return monsters;
    }

    private Monster instantiateMonster(MonsterTemplate t) {
        return instantiateMonster(t, t.level);
    }

    /**
     * Create a monster based on a template, but force the level to targetLevel
     * and scale its stats proportionally.
     */
    private Monster instantiateMonster(MonsterTemplate t, int targetLevel) {
        int baseLevel = Math.max(1, t.level);
        int level = Math.max(1, targetLevel);
        double scale = level / (double) baseLevel;

        int scaledDamage  = Math.max(1, (int) Math.round(t.damage * scale));
        int scaledDefense = Math.max(0, (int) Math.round(t.defense * scale));
        int scaledDodge   = Math.min(90, Math.max(0, (int) Math.round(t.dodgeChance * scale)));

        Monster m = new Monster(t.name, level, scaledDamage, scaledDefense, scaledDodge);
        // HP is derived from GameBalance using the forced level inside Monster constructor
        return m;
    }

}
