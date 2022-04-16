package traces;

import oathkeeper.runtime.EventTracer;
import oathkeeper.runtime.event.OpTriggerEvent;
import oathkeeper.runtime.event.SemanticEvent;

import java.util.List;

public class TraceForOpAddTimeInvokeOpTemplate extends TraceForTemplate {

    public EventTracer getPatchedEventTracer()
    {
        //1->2
        EventTracer tracer = new EventTracer();
        tracer.enqueue(new OpTriggerEvent("1", 0));
        tracer.enqueue(new OpTriggerEvent("2",999));
        tracer.enqueue(new OpTriggerEvent("1",1000));
        tracer.enqueue(new OpTriggerEvent("2",1999));
        tracer.enqueue(new OpTriggerEvent("3",2300));
        tracer.enqueue(new OpTriggerEvent("3",2350));
        tracer.enqueue(new OpTriggerEvent("1",2500));
        tracer.enqueue(new OpTriggerEvent("3",2600));
        tracer.enqueue(new OpTriggerEvent("4",2700));
        tracer.enqueue(new OpTriggerEvent("3",2800));
        tracer.enqueue(new OpTriggerEvent("2", 3500));
        tracer.enqueue(new OpTriggerEvent("1",4501));
        tracer.enqueue(new OpTriggerEvent("2",5504));
        tracer.assignSerialTimestamp(true,false);

        return tracer;
    }

    public EventTracer getUnpatchedEventTracer()
    {
        EventTracer tracer = new EventTracer();
        tracer.enqueue(new OpTriggerEvent("1", 0));
        tracer.enqueue(new OpTriggerEvent("1",200));
        tracer.enqueue(new OpTriggerEvent("2",999));
        tracer.enqueue(new OpTriggerEvent("3",1300));
        tracer.enqueue(new OpTriggerEvent("3",11350));
        tracer.enqueue(new OpTriggerEvent("1",21500));
        tracer.enqueue(new OpTriggerEvent("3",21600));
        tracer.enqueue(new OpTriggerEvent("4",21700));
        tracer.enqueue(new OpTriggerEvent("3",21800));
        tracer.enqueue(new OpTriggerEvent("1",32501));
        tracer.enqueue(new OpTriggerEvent("2",73504));
        tracer.assignSerialTimestamp(true,false);

        return tracer;
    }


}
