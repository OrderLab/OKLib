package traces;

import oathkeeper.runtime.EventTracer;
import oathkeeper.runtime.event.OpTriggerEvent;
import oathkeeper.runtime.event.StateUpdateEvent;

public class TraceForAfterOpAtomicStateUpdateTemplate extends TraceForTemplate{

    //1 -> 2 ^ 3
    public EventTracer getPatchedEventTracer()
    {
        EventTracer tracer = new EventTracer();
        //tracer.enqueue(new StateUpdateEvent("2","nouse",1));
        //tracer.enqueue(new StateUpdateEvent("2","nouse",1));
        tracer.enqueue(new OpTriggerEvent("4"));
        tracer.enqueue(new OpTriggerEvent("4"));
        tracer.enqueue(new OpTriggerEvent("5"));
        tracer.enqueue(new OpTriggerEvent("4"));
        tracer.enqueue(new OpTriggerEvent("1"));
        tracer.enqueue(new OpTriggerEvent("6"));
        tracer.enqueue(new StateUpdateEvent("2","nouse",1));
        tracer.enqueue(new OpTriggerEvent("4"));
        tracer.enqueue(new OpTriggerEvent("4"));
        tracer.enqueue(new OpTriggerEvent("6"));
        tracer.enqueue(new StateUpdateEvent("3","nouse",2));
        tracer.enqueue(new OpTriggerEvent("4"));
        tracer.enqueue(new OpTriggerEvent("1"));
        tracer.enqueue(new OpTriggerEvent("5"));
        tracer.enqueue(new StateUpdateEvent("2","nouse",1));
        tracer.enqueue(new StateUpdateEvent("3","nouse",2));
        tracer.enqueue(new OpTriggerEvent("4"));
        tracer.enqueue(new OpTriggerEvent("1"));
        tracer.assignSerialTimestamp(true,true);
        return tracer;
    }

    public EventTracer getUnpatchedEventTracer()
    {
        EventTracer tracer = new EventTracer();
        //tracer.enqueue(new StateUpdateEvent("2","nouse",1));
        //tracer.enqueue(new StateUpdateEvent("2","nouse",1));
        tracer.enqueue(new OpTriggerEvent("4"));
        tracer.enqueue(new OpTriggerEvent("4"));
        tracer.enqueue(new OpTriggerEvent("5"));
        tracer.enqueue(new OpTriggerEvent("4"));
        tracer.enqueue(new OpTriggerEvent("1"));
        tracer.enqueue(new OpTriggerEvent("6"));
        tracer.enqueue(new StateUpdateEvent("2","nouse",1));
        tracer.enqueue(new OpTriggerEvent("4"));
        tracer.enqueue(new OpTriggerEvent("4"));
        tracer.enqueue(new OpTriggerEvent("6"));
        tracer.enqueue(new OpTriggerEvent("4"));
        tracer.enqueue(new OpTriggerEvent("5"));
        tracer.enqueue(new StateUpdateEvent("2","nouse",1));
        tracer.enqueue(new OpTriggerEvent("4"));
        tracer.assignSerialTimestamp(true,true);
        return tracer;
    }
}
