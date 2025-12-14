package data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import items.Armor;
import items.Item;
import items.Potion;
import items.Potion.StatType;
import items.Spell;
import items.Spell.SpellType;
import items.SpellEffect;
import items.FireSpellEffect;
import items.IceSpellEffect;
import items.LightningSpellEffect;
import items.Weapon;

public class ItemFactory {

    // Folder where your text files live
    private static final String DATA_DIR = "Legends_Monsters_and_Heroes/";

    private static final String ARMORY_FILE    = DATA_DIR + "Armory.txt";
    private static final String WEAPONRY_FILE  = DATA_DIR + "Weaponry.txt";
    private static final String POTIONS_FILE   = DATA_DIR + "Potions.txt";
    private static final String FIRE_SPELLS_FILE      = DATA_DIR + "FireSpells.txt";
    private static final String ICE_SPELLS_FILE       = DATA_DIR + "IceSpells.txt";
    private static final String LIGHTNING_SPELLS_FILE = DATA_DIR + "LightningSpells.txt";

    private boolean loaded = false;
    private Random random = new Random();

    private List<Armor> armors = new ArrayList<Armor>();
    private List<Weapon> weapons = new ArrayList<Weapon>();
    private List<Potion> potions = new ArrayList<Potion>();
    private List<Spell> spells = new ArrayList<Spell>();

    private void ensureLoaded() {
        if (loaded) return;
        loadArmors();
        loadWeapons();
        loadPotions();
        loadSpells();
        loaded = true;
    }

    // ------------------------------------------------------------
    // Loaders
    // ------------------------------------------------------------

    private void loadArmors() {
        armors.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(ARMORY_FILE))) {
            String line = br.readLine(); // header: Name/cost/required level/damage reduction
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length < 4) continue;

                String name = parts[0];
                int cost = Integer.parseInt(parts[1]);
                int level = Integer.parseInt(parts[2]);
                int reduction = Integer.parseInt(parts[3]);

                Armor armor = new Armor(name, cost, level, reduction);
                armors.add(armor);
            }
        } catch (IOException e) {
            System.err.println("Could not load armors from " + ARMORY_FILE);
            e.printStackTrace();
        }
    }

    private void loadWeapons() {
        weapons.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(WEAPONRY_FILE))) {
            String line = br.readLine(); // header: Name/cost/level/damage/required hands
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length < 5) continue;

                String name = parts[0];
                int cost = Integer.parseInt(parts[1]);
                int level = Integer.parseInt(parts[2]);
                int damage = Integer.parseInt(parts[3]);
                int hands = Integer.parseInt(parts[4]);

                Weapon weapon = new Weapon(name, cost, level, damage, hands);
                weapons.add(weapon);
            }
        } catch (IOException e) {
            System.err.println("Could not load weapons from " + WEAPONRY_FILE);
            e.printStackTrace();
        }
    }

    private void loadPotions() {
        potions.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(POTIONS_FILE))) {
            String line = br.readLine(); // header: Name/cost/required level/attribute increase/attribute affected
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length < 5) continue;

                String name = parts[0];
                int cost = Integer.parseInt(parts[1]);
                int level = Integer.parseInt(parts[2]);
                int amount = Integer.parseInt(parts[3]);

                // Attribute affected may have spaces (e.g. "All Health/Mana/...").
                // Join all remaining tokens.
                StringBuilder sb = new StringBuilder();
                for (int i = 4; i < parts.length; i++) {
                    if (i > 4) sb.append(' ');
                    sb.append(parts[i]);
                }
                String attrRaw = sb.toString().trim();
                StatType statType = mapAttributeToStatType(attrRaw);

                Potion potion = new Potion(name, cost, level, statType, amount);
                potions.add(potion);
            }
        } catch (IOException e) {
            System.err.println("Could not load potions from " + POTIONS_FILE);
            e.printStackTrace();
        }
    }

    private void loadSpells() {
        spells.clear();
        loadSpellFile(FIRE_SPELLS_FILE, SpellType.FIRE);
        loadSpellFile(ICE_SPELLS_FILE, SpellType.ICE);
        loadSpellFile(LIGHTNING_SPELLS_FILE, SpellType.LIGHTNING);
    }

    private void loadSpellFile(String fileName, SpellType type) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line = br.readLine(); // header: Name/cost/required level/damage/mana cost
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length < 5) continue;

                String name = parts[0];
                int cost = Integer.parseInt(parts[1]);
                int level = Integer.parseInt(parts[2]);
                int damage = Integer.parseInt(parts[3]);
                int manaCost = Integer.parseInt(parts[4]);

                Spell spell = new Spell(name, cost, level, damage, manaCost, type, createEffect(type));
                spells.add(spell);
            }
        } catch (IOException e) {
            System.err.println("Could not load spells from " + fileName);
            e.printStackTrace();
        }
    }

    private SpellEffect createEffect(SpellType type) {
        switch (type) {
            case FIRE: return new FireSpellEffect();
            case ICE: return new IceSpellEffect();
            case LIGHTNING: return new LightningSpellEffect();
            default: return null;
        }
    }

    private StatType mapAttributeToStatType(String attrRaw) {
        if (attrRaw == null) return StatType.ALL;
        String normalized = attrRaw.replaceAll("\\s+", "").toLowerCase();

        if (normalized.equals("health")) return StatType.HP;
        if (normalized.equals("mana")) return StatType.MANA;
        if (normalized.equals("strength")) return StatType.STRENGTH;
        if (normalized.equals("dexterity")) return StatType.DEXTERITY;
        if (normalized.equals("agility")) return StatType.AGILITY;

        // Things like "Health/Mana/Strength/Agility" or "All Health/Mana/Strength/..."
        return StatType.ALL;
    }

    // ------------------------------------------------------------
    // Public API for drops
    // ------------------------------------------------------------

    public Potion getRandomPotionForLevel(int approxLevel) {
        ensureLoaded();
        List<Potion> candidates = new ArrayList<Potion>();
        for (Potion p : potions) {
            if (p.getRequiredLevel() <= approxLevel + 1) {
                candidates.add(p);
            }
        }
        if (candidates.isEmpty()) {
            candidates.addAll(potions);
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(random.nextInt(candidates.size()));
    }

    public Weapon getRandomWeaponForLevel(int approxLevel) {
        ensureLoaded();
        List<Weapon> candidates = new ArrayList<Weapon>();
        for (Weapon w : weapons) {
            if (w.getRequiredLevel() <= approxLevel + 1) {
                candidates.add(w);
            }
        }
        if (candidates.isEmpty()) {
            candidates.addAll(weapons);
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(random.nextInt(candidates.size()));
    }

    public Armor getRandomArmorForLevel(int approxLevel) {
        ensureLoaded();
        List<Armor> candidates = new ArrayList<Armor>();
        for (Armor a : armors) {
            if (a.getRequiredLevel() <= approxLevel + 1) {
                candidates.add(a);
            }
        }
        if (candidates.isEmpty()) {
            candidates.addAll(armors);
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(random.nextInt(candidates.size()));
    }

    public Spell getRandomSpellForLevel(int approxLevel) {
        ensureLoaded();
        List<Spell> candidates = new ArrayList<Spell>();
        for (Spell s : spells) {
            if (s.getRequiredLevel() <= approxLevel + 1) {
                candidates.add(s);
            }
        }
        if (candidates.isEmpty()) {
            candidates.addAll(spells);
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(random.nextInt(candidates.size()));
    }
}
