package traces;

import oathkeeper.runtime.EventTracer;

public abstract class TraceForTemplate {

    public abstract EventTracer getPatchedEventTracer();
    public abstract EventTracer getUnpatchedEventTracer();

}
