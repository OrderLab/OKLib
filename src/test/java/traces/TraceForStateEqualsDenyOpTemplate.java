package traces;

import oathkeeper.runtime.EventTracer;
import oathkeeper.runtime.event.OpTriggerEvent;
import oathkeeper.runtime.event.SemanticEvent;
import oathkeeper.runtime.event.StateUpdateEvent;

import java.util.List;

public class TraceForStateEqualsDenyOpTemplate extends TraceForTemplate{

    // constant = 5, state 1 denies op 2
    public EventTracer getPatchedEventTracer()
    {
        EventTracer tracer = new EventTracer();
        tracer.enqueue(new StateUpdateEvent("1","nouse",0));
        tracer.enqueue(new OpTriggerEvent("2"));
        tracer.enqueue(new OpTriggerEvent("3"));
        tracer.enqueue(new OpTriggerEvent("3"));
        tracer.enqueue(new StateUpdateEvent("1","nouse",1));
        tracer.enqueue(new OpTriggerEvent("2"));
        tracer.enqueue(new OpTriggerEvent("2"));
        tracer.enqueue(new StateUpdateEvent("1","nouse",2));
        tracer.enqueue(new OpTriggerEvent("2"));
        tracer.enqueue(new OpTriggerEvent("3"));
        tracer.enqueue(new OpTriggerEvent("4"));
        tracer.enqueue(new StateUpdateEvent("1","nouse",5));
        tracer.enqueue(new OpTriggerEvent("3"));
        tracer.enqueue(new StateUpdateEvent("1","nouse",0));
        tracer.enqueue(new OpTriggerEvent("2"));
        tracer.assignSerialTimestamp(true,true);

        return tracer;
    }

    public EventTracer getUnpatchedEventTracer()
    {
        EventTracer tracer = new EventTracer();
        tracer.enqueue(new StateUpdateEvent("1","nouse",0));
        tracer.enqueue(new OpTriggerEvent("2"));
        tracer.enqueue(new OpTriggerEvent("3"));
        tracer.enqueue(new OpTriggerEvent("3"));
        tracer.enqueue(new StateUpdateEvent("1","nouse",1));
        tracer.enqueue(new OpTriggerEvent("2"));
        tracer.enqueue(new OpTriggerEvent("2"));
        tracer.enqueue(new StateUpdateEvent("1","nouse",2));
        tracer.enqueue(new OpTriggerEvent("3"));
        tracer.enqueue(new OpTriggerEvent("4"));
        tracer.enqueue(new StateUpdateEvent("1","nouse",5));
        tracer.enqueue(new OpTriggerEvent("3"));
        tracer.enqueue(new OpTriggerEvent("2"));
        tracer.enqueue(new StateUpdateEvent("1","nouse",0));
        tracer.enqueue(new OpTriggerEvent("2"));
        tracer.assignSerialTimestamp(true,true);

        return tracer;
    }
}
