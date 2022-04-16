import oathkeeper.engine.InferEngine;
import oathkeeper.runtime.EventTracer;
import oathkeeper.runtime.FileLayoutManager;
import oathkeeper.runtime.invariant.Invariant;
import traces.*;

import java.util.List;

public class TestUtils {
    static public void generateAll()
    {
        System.setProperty("ok.ok_root_abs_path", System.getProperty("user.dir"));

        TestUtils.generateTrace(new TraceForOpAddTimeInvokeOpTemplate(),"testOpAddTimeInvokeOpTemplate");
        TestUtils.generateTrace(new TraceForEventImplyEventTemplate(),"testEventImplyEventTemplate");
        TestUtils.generateTrace(new TraceForEventHappenBeforeEventTemplate(),"testEventHappenBeforeEventTemplate");
        TestUtils.generateTrace(new TraceForEventHappenBeforeEventTemplate2(),"testEventHappenBeforeEventTemplate2");
        TestUtils.generateTrace(new TraceForEventProtectedByEventTemplate(),"testEventProtectedByEventTemplate");
        TestUtils.generateTrace(new TraceForStateEqualsDenyOpTemplate(),"testStateEqualsDenyOpTemplate");
        TestUtils.generateTrace(new TraceForAfterOpAtomicStateUpdateTemplate(),"testAfterOpAtomicStateUpdateTemplate");
    }

    public static void generateTrace(TraceForTemplate trace, String traceFilePrefix) {
        EventTracer.dumpToFile(FileLayoutManager.getPathForTestTraceDir(), traceFilePrefix + EventTracer.PATCHED_SUFFIX,
                trace.getPatchedEventTracer());
        EventTracer.dumpToFile(FileLayoutManager.getPathForTestTraceDir(), traceFilePrefix + EventTracer.UNPATCHED_SUFFIX,
                trace.getUnpatchedEventTracer());
    }

    public static List<Invariant> loadAndInfer(String traceFilePrefix) {
        return InferEngine.processTrace(traceFilePrefix,false);
    }

    public static List<Invariant> loadAndInfer_v1(String traceFilePrefix) {
        return InferEngine.processTrace_v1(traceFilePrefix,false);
    }

    public static Invariant.InvState loadAndVerifyPatched(String traceFilePrefix, Invariant inv)
    {
        EventTracer patchedTracer = EventTracer.loadFromFile(traceFilePrefix + EventTracer.PATCHED_SUFFIX);
        return inv.verify(patchedTracer);
    }

    public static Invariant.InvState loadAndVerifyUnpatched(String traceFilePrefix, Invariant inv)
    {
        EventTracer unpatchedTracer = EventTracer.loadFromFile(traceFilePrefix + EventTracer.UNPATCHED_SUFFIX);
        return inv.verify(unpatchedTracer);
    }

    public static Invariant.InvState loadAndVerifyPatched_v1(String traceFilePrefix, Invariant inv)
    {
        EventTracer patchedTracer = EventTracer.loadFromFile(traceFilePrefix + EventTracer.PATCHED_SUFFIX);
        return inv.verify_v1(patchedTracer.eventQueue);
    }

    public static Invariant.InvState loadAndVerifyUnpatched_v1(String traceFilePrefix, Invariant inv)
    {
        EventTracer unpatchedTracer = EventTracer.loadFromFile(traceFilePrefix + EventTracer.UNPATCHED_SUFFIX);
        return inv.verify_v1(unpatchedTracer.eventQueue);
    }

    public static boolean containsInvariantType(List<Invariant> lst, String templateName) {
        for (Invariant inv : lst) {
            if (inv.template.getTemplateName().equals(templateName))
                return true;
        }
        return false;
    }

    public static boolean containsInvariant(List<Invariant> lst, Invariant targetInv) {
        for (Invariant inv : lst) {
            if (inv.template.getTemplateName().equals(targetInv.template.getTemplateName())) {
                if (inv.context.equals(targetInv.context))
                    return true;
            }
        }
        return false;
    }

    public static boolean containsInvariant_v1(List<Invariant> lst, Invariant targetInv) {
        for (Invariant inv : lst) {
            if (inv.template_v1.getTemplateName().equals(targetInv.template_v1.getTemplateName())) {
                if (inv.context.equals(targetInv.context))
                    return true;
            }
        }
        return false;
    }

    public static void debugInvariant(List<Invariant> lst) {
        for(Invariant inv:lst)
            System.out.println(inv.toString());
    }

}
