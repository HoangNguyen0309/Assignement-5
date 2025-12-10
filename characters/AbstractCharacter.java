package characters;


public abstract class AbstractCharacter implements Character {
    protected String name;
    protected int level;
    protected int hp;
    protected int maxHp;

    protected AbstractCharacter(String name, int level, int maxHp) {
        this.name = name;
        this.level = level;
        this.maxHp = maxHp;
        this.hp = maxHp;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public int getLevel() { return level; }
    public int getHP() { return hp; }
    public int getMaxHP() { return maxHp; }

    public boolean isFainted() {
        return hp <= 0;
    }

    public void takeDamage(int amount) {
        hp -= amount;
        if (hp < 0) hp = 0;
    }

    public void heal(int amount) {
        hp += amount;
        if (hp > maxHp) hp = maxHp;
    }
}

