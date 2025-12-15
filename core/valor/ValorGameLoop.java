package core.valor;

import core.valor.phases.Phase;

public class ValorGameLoop {

    private final ValorContext ctx;

    private final Phase setup;
    private final Phase hero;
    private final Phase monster;
    private final Phase cleanup;
    private final Phase endRound;

    public ValorGameLoop(ValorContext ctx,
                         Phase setup,
                         Phase hero,
                         Phase monster,
                         Phase cleanup,
                         Phase endRound) {
        this.ctx = ctx;
        this.setup = setup;
        this.hero = hero;
        this.monster = monster;
        this.cleanup = cleanup;
        this.endRound = endRound;
    }

    public void run() {
        ctx.renderer.renderMessage("Starting Legends of Valor!");

        setup.execute(ctx);

        while (!ctx.gameOver) {
            ctx.roundLog.clear();

            hero.execute(ctx);
            if (ctx.gameOver) break;

            monster.execute(ctx);
            if (ctx.gameOver) break;

            cleanup.execute(ctx);

            endRound.execute(ctx);

            ctx.roundCount++;
        }

        ctx.renderer.renderMessage("Game over. Thanks for playing Legends of Valor!");
    }
}
