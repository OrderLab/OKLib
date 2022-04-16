package traces;

import oathkeeper.runtime.EventTracer;
import oathkeeper.runtime.event.OpTriggerEvent;

public class TraceForEventHappenBeforeEventTemplate2 extends TraceForTemplate{

    //1->2
    public EventTracer getPatchedEventTracer()
    {
        EventTracer tracer = new EventTracer();
        tracer.enqueue(new OpTriggerEvent("2"));
        tracer.enqueue(new OpTriggerEvent("2"));
        tracer.enqueue(new OpTriggerEvent("2"));
        tracer.enqueue(new OpTriggerEvent("2"));
        tracer.enqueue(new OpTriggerEvent("2"));
        tracer.enqueue(new OpTriggerEvent("2"));
        tracer.enqueue(new OpTriggerEvent("2"));
        tracer.enqueue(new OpTriggerEvent("2"));
        tracer.assignSerialTimestamp(true,true);
        return tracer;
    }

    public EventTracer getUnpatchedEventTracer()
    {
        EventTracer tracer = new EventTracer();
        tracer.enqueue(new OpTriggerEvent("1"));
        tracer.enqueue(new OpTriggerEvent("2"));
        tracer.enqueue(new OpTriggerEvent("3"));
        tracer.enqueue(new OpTriggerEvent("3"));
        tracer.enqueue(new OpTriggerEvent("2"));
        tracer.enqueue(new OpTriggerEvent("1"));
        tracer.enqueue(new OpTriggerEvent("3"));
        tracer.enqueue(new OpTriggerEvent("4"));
        tracer.enqueue(new OpTriggerEvent("3"));
        tracer.enqueue(new OpTriggerEvent("1"));
        tracer.enqueue(new OpTriggerEvent("2"));
        tracer.assignSerialTimestamp(true,true);
        return tracer;
    }
}
