package core.valor.phases;

import core.valor.ValorContext;
import core.valor.ValorSupport;

public class EndOfRoundPhase implements Phase {
    @Override
    public void execute(ValorContext ctx) {
        ValorSupport.endOfRound(ctx);
    }
}
