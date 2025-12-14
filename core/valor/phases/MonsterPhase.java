package core.valor.phases;

import core.valor.ValorContext;
import core.valor.ValorSupport;

public class MonsterPhase implements Phase {
    @Override
    public void execute(ValorContext ctx) {
        ValorSupport.monsterPhase(ctx);
    }
}
