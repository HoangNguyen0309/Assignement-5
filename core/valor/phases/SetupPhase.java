package core.valor.phases;

import core.valor.ValorContext;
import core.valor.ValorSupport;

public class SetupPhase implements Phase {
    @Override
    public void execute(ValorContext ctx) {
        ValorSupport.initializeHeroPositions(ctx);
        ValorSupport.updateLaneMaxLevels(ctx);
        ValorSupport.assignHeroCodes(ctx);
        ValorSupport.spawnInitialMonsters(ctx);
    }
}
