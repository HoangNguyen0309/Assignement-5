package characters;

public enum HeroType {
    WARRIOR,
    SORCERER,
    PALADIN;

    public String getDisplayName() {
        switch (this) {
            case WARRIOR: return "Warrior";
            case SORCERER: return "Sorcerer";
            case PALADIN: return "Paladin";
            default: return name();
        }
    }
}
