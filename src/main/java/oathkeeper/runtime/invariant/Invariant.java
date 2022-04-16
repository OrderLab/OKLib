package oathkeeper.runtime.invariant;

import oathkeeper.runtime.EventTracer;
import oathkeeper.runtime.event.SemanticEvent;
import oathkeeper.runtime.template.Template;
import oathkeeper.runtime.template_v1.TemplateV1;

import java.util.List;
import java.util.Objects;

public class Invariant {
    //the relations between invariants
    public Template template;
    public TemplateV1 template_v1;
    //involved events
    public Context context;
    //verify stats
    public VerifyStats stats;

    public static class VerifyStats {
        public int passNum=-1;
        public int inactiveNum=-1;
        public int failNum=-1;

        public VerifyStats(int passNum, int inactiveNum, int failNum) {
            this.passNum = passNum;
            this.inactiveNum = inactiveNum;
            this.failNum = failNum;
        }

        @Override
        public String toString() {
            return "VerifyStats{" +
                    "passNum=" + passNum +
                    ", inactiveNum=" + inactiveNum +
                    ", failNum=" + failNum +
                    '}';
        }
    }

    public enum InvState {
        PASS,
        INACTIVE,
        FAIL,
        ILLEGAL;
    }

    public static boolean ifStateNotFail(InvState state)
    {
        return state.equals(InvState.PASS) || state.equals(InvState.INACTIVE);
    }

    public InvState verify(EventTracer tracer) {
        return template.verify(tracer, context);
    }

    public InvState verify_v1(List<SemanticEvent> queue) {
        return template_v1.verify(queue, context);
    }

    public void invertTemplate() {
        template = template.invert();
    }

        // check if a given trace satisfy the corresponding invariant
    /*
    public boolean check(List<SemanticEvent> traces) {
        for (Context realworkload : template.generateCtxtCandidates(traces, new HashSet<>(traces))) {
            if (realworkload.equals(context))
                if (!template.match(realworkload)) {
                    return false;
                }
        }

        return true;
    }
*/

    public Invariant() {
    }

    public Invariant(Template template, Context context) {
        this.template = template;
        this.context = context;
    }

    public Invariant(TemplateV1 template, Context context) {
        this.template_v1 = template;
        this.context = context;
    }

    @Override
    public String toString() {
        if(template!=null)
        return "Invariant{" +
                "template=" + template +
                ", context=" + context +
                '}';
        else if(template_v1!=null)
            return "Invariant{" +
                    "template=" + template_v1 +
                    ", context=" + context +
                    '}';
        else
            throw new RuntimeException("IMPOSSIBLE");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Invariant invariant = (Invariant) o;

        if(template != null)
        return Objects.equals(template, invariant.template) &&
                Objects.equals(context, invariant.context);
        else if(template_v1 != null)
            return Objects.equals(template_v1, invariant.template_v1) &&
                    Objects.equals(context, invariant.context);
        else
            throw new RuntimeException("IMPOSSIBLE");
    }

    @Override
    public int hashCode() {
        return Objects.hash(template, context);
    }

    public String toStringWithStats() {
        if(template!=null)
            return "Invariant{" +
                    "template=" + template +
                    ", template_v1=" + template_v1 +
                    ", context=" + context +
                    ", stats=" + stats +
                    '}';
        else if(template_v1!=null)
            return "Invariant{" +
                    "template=" + template_v1 +
                    ", context=" + context +
                    '}';
        else
            throw new RuntimeException("IMPOSSIBLE");
    }
}
