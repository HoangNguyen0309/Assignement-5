package core.valor.phases;

import core.valor.ValorContext;
import core.valor.services.MonsterSystem;

public class MonsterPhase implements Phase {
    private final MonsterSystem monsters = new MonsterSystem();

    @Override
    public void execute(ValorContext ctx) {
        monsters.takeTurn(ctx);
    }
}
