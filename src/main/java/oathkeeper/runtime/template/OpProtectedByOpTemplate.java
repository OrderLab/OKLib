package oathkeeper.runtime.template;

import oathkeeper.runtime.event.OpTriggerEvent;
import oathkeeper.runtime.event.SemanticEvent;
import oathkeeper.runtime.invariant.Context;
import oathkeeper.runtime.invariant.Invariant;

public class OpProtectedByOpTemplate extends EventProtectedByEventTemplate {
    @Override
    public String getTemplateName() {
        return "OpProtectedByOpTemplate";
    }

    @Override
    public boolean checkLeftEventClass(SemanticEvent event)
    {
        return event instanceof OpTriggerEvent;
    }

    @Override
    public boolean checkRightEventClass(SemanticEvent event)
    {
        return event instanceof OpTriggerEvent;
    }

    @Override
    public Invariant genInv(Context context) {
        return new Invariant(new OpProtectedByOpTemplate(), context);
    }

}
