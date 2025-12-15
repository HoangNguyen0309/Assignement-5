package core.valor.phases;

import core.valor.ValorContext;
import core.valor.ui.HeroTurnController;

public class HeroPhase implements Phase {

    private final HeroTurnController controller = new HeroTurnController();

    @Override
    public void execute(ValorContext ctx) {
        controller.takeTurns(ctx);
    }
}
