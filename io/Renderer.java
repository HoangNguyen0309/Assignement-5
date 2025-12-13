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
    void renderHeroStats(List<Hero> heroes, Map<Hero, String> heroCodes);
    void renderMonsterStats(List<Monster> monsters);
    void renderMonsterStats(List<Monster> monsters, Map<Monster, String> monsterCodes);
    void renderMessage(String message);
    // New overload for Legends of Valor
    void renderWorld(World world,
                     Map<Hero, core.Position> heroPositions,
                     Map<Monster, core.Position> monsterPositions,
                     Map<Hero, String> heroCodes,
                     Map<Monster, String> monsterCodes);
}
