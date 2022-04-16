package oathkeeper.runtime.template;

import oathkeeper.runtime.event.OpTriggerEvent;
import oathkeeper.runtime.event.SemanticEvent;
import oathkeeper.runtime.event.StateUpdateEvent;
import oathkeeper.runtime.invariant.Context;
import oathkeeper.runtime.invariant.Invariant;

public class StateChangeHappenBeforeOpTemplate extends EventHappenBeforeEventTemplate {

    @Override
    public String getTemplateName() {
        return "StateChangeHappenBeforeOpTemplate";
    }

    @Override
    public boolean checkLeftEventClass(SemanticEvent event)
    {
        return event instanceof StateUpdateEvent;
    }

    @Override
    public boolean checkRightEventClass(SemanticEvent event)
    {
        return event instanceof OpTriggerEvent;
    }

    @Override
    public Invariant genInv(Context context) {
        return new Invariant(new StateChangeHappenBeforeOpTemplate(), context);
    }

}
