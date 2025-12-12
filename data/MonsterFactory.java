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
     * Spawn monsters with level targeting the provided level (or the closest available).
     */
    public List<Monster> spawnMonstersForBattle(int count, int targetLevel) {
        ensureLoaded();

        int desiredLevel = Math.max(1, targetLevel);

        // First try exact level matches
        List<MonsterTemplate> candidates = new ArrayList<MonsterTemplate>();
        for (MonsterTemplate t : templates) {
            if (t.level == desiredLevel) {
                candidates.add(t);
            }
        }

        // If none, choose the closest level band
        if (candidates.isEmpty()) {
            int bestDiff = Integer.MAX_VALUE;
            for (MonsterTemplate t : templates) {
                int diff = Math.abs(t.level - desiredLevel);
                if (diff < bestDiff) {
                    bestDiff = diff;
                }
            }
            for (MonsterTemplate t : templates) {
                if (Math.abs(t.level - desiredLevel) == bestDiff) {
                    candidates.add(t);
                }
            }
        }

        if (candidates.isEmpty()) {
            return new ArrayList<Monster>();
        }

        List<Monster> monsters = new ArrayList<Monster>();
        for (int i = 0; i < count; i++) {
            MonsterTemplate t = candidates.get(random.nextInt(candidates.size()));
            monsters.add(instantiateMonster(t));
        }
        return monsters;
    }

    private Monster instantiateMonster(MonsterTemplate t) {
        return new Monster(t.name, t.level, t.damage, t.defense, t.dodgeChance);
    }

    /**
     * Simple HP formula ¨C you can tune it.
     * Here: base 100 + 50 * level
     */
    private int computeHp(int level) {
        return GameBalance.monsterHpForLevel(level);
    }

}
