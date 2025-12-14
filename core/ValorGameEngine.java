package core;

import java.util.List;

import battle.Battle;
import characters.Hero;
import core.valor.ValorContext;
import core.valor.ValorGameLoop;
import core.valor.phases.CleanupPhase;
import core.valor.phases.EndOfRoundPhase;
import core.valor.phases.HeroPhase;
import core.valor.phases.MonsterPhase;
import core.valor.phases.SetupPhase;
import io.InputHandler;
import io.Renderer;
import world.World;

public class ValorGameEngine implements Battle {

    private final ValorGameLoop loop;

    public ValorGameEngine(World world,
                           List<Hero> heroes,
                           Renderer renderer,
                           InputHandler input,
                           int monsterWavePeriod) {

        ValorContext ctx = new ValorContext(world, heroes, renderer, input, monsterWavePeriod);

        this.loop = new ValorGameLoop(
                ctx,
                new SetupPhase(),
                new HeroPhase(),
                new MonsterPhase(),
                new CleanupPhase(),
                new EndOfRoundPhase()
        );
    }

    @Override
    public void start() {
        loop.run();
    }

    // optional compatibility if older code calls run()
    public void run() {
        start();
    }
}
