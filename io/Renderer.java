package io;

import java.util.List;

import characters.Hero;
import characters.Monster;
import world.World;

public interface Renderer {
    void renderWorld(World world);
    void renderHeroStats(List<Hero> heroes);
    void renderMonsterStats(List<Monster> monsters);
    void renderMessage(String message);
}
