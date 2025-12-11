package io;

import java.util.List;
import java.util.Map;      // ✅ added

import characters.Hero;
import characters.Monster;
import core.Position;      // ✅ added
import world.World;


public interface Renderer {
    void renderWorld(World world);
    void renderHeroStats(List<Hero> heroes);
    void renderMonsterStats(List<Monster> monsters);
    void renderMessage(String message);
    // New overload for Legends of Valor
    void renderWorld(World world,
                     Map<Hero, core.Position> heroPositions,
                     Map<Monster, core.Position> monsterPositions);
}
