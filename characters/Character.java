package characters;

public interface Character {
    String getName();
    int getLevel();
    int getHP();
    int getMaxHP();
    boolean isFainted();
    void takeDamage(int amount);
    void heal(int amount);
}
