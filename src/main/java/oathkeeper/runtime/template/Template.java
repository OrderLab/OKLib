package oathkeeper.runtime.template;

import oathkeeper.runtime.ConfigManager;
import oathkeeper.runtime.EventTracer;
import oathkeeper.runtime.OKHelper;
import oathkeeper.runtime.event.SemanticEvent;
import oathkeeper.runtime.event.StateUpdateEvent;
import oathkeeper.runtime.invariant.Context;
import oathkeeper.runtime.invariant.Invariant;
import io.herrmann.generator.Generator;

import java.util.*;

import static oathkeeper.runtime.invariant.Invariant.ifStateNotFail;

public abstract class Template {

    public abstract class InferScanner {
        //init some state vars here
        public abstract void prescan(Set<SemanticEvent> eventSet);

        //always need to go through the whole traces
        public abstract void scan(SemanticEvent event);

        //check the after scan state, and judge
        public abstract List<Invariant> postscan();
    }

    public abstract class VerifyScanner {
        //init some state vars here
        public abstract void prescan();

        //return true means continue, otherwise break
        public abstract boolean scan(SemanticEvent event);

        //check the after scan state, and judge
        public abstract void postscan();

        //return your customized state
        public abstract Invariant.InvState getRetVal();
    }

    public abstract String getTemplateName();

    public boolean checkLeftEventClass(SemanticEvent event){
        return true;
    }

    public boolean checkRightEventClass(SemanticEvent event){
        return true;
    }

    public boolean checkSecondRightEventClass(SemanticEvent event){
        return true;
    }

    //construct invariant with given template and context
    //can return null if the context is illegal, needs to check
    public abstract Invariant genInv(Context context);

    //two sets of scanners
    //infer scanner: heavyweight, to infer missing variables in invariants
    public abstract InferScanner getInferScanner();

    //verify scanner: lightweight, do quick check on given traces
    public abstract VerifyScanner getVerifyScanner(Context context);

    //return list of context
    public List<Invariant> infer(EventTracer tracer) {
        InferScanner scanner = getInferScanner();
        scanner.prescan(tracer.getEventSet());
        for (SemanticEvent event:tracer)
            scanner.scan(event);
        return scanner.postscan();
    }

    //return true means the relation holds on given traces
    public Invariant.InvState verify(EventTracer tracer, Context context) {
        VerifyScanner scanner = getVerifyScanner(context);
        scanner.prescan();
        //to avoid concurrentModificationEx, use index
        Iterator<SemanticEvent> it = tracer.iterator(context);
        while (it.hasNext()) {
            SemanticEvent event = it.next();
            //we may encounter hasNext() returns true but next() returns null,
            // it should be due to concurrency updates and fine, just skip it
            if(event==null)
            {
                continue;
            }
            //OKHelper.debug(event.toString());
            if (!scanner.scan(event)) {
                break;
            }
        }
        scanner.postscan();
        return scanner.getRetVal();
    }

    //invert to opposite version of template
    public abstract Template invert();

    private long getLongDistance(long a, long b)
    {
        return a>=b?(a-b):(b-a);
    }

    //if two events are two
    private boolean checkTimeConstraints(Context context)
    {
        long TIME_CONSTRAINT = ConfigManager.config.getLong(ConfigManager.TIME_WINDOW_LENGTH_IN_MILLIS_KEY);
        if(context.secondright!=null)
        {
            return getLongDistance(context.left.system_timestamp,context.right.system_timestamp) <= TIME_CONSTRAINT
                    && getLongDistance(context.left.system_timestamp,context.secondright.system_timestamp) <= TIME_CONSTRAINT
                    && getLongDistance(context.right.system_timestamp,context.secondright.system_timestamp) <= TIME_CONSTRAINT;
        }

        if(context.right!=null)
        {
            return getLongDistance(context.left.system_timestamp,context.right.system_timestamp) <= TIME_CONSTRAINT;
        }

        throw new RuntimeException("IMPOSSIBLE");
    }

    @Deprecated
    //previously we use this approach to infer rules, however the two phases are too expensive (see analysis in week22 report)
    //now we move to 1-phase approach
//    public List<Invariant> infer_two_phase(List<SemanticEvent> traces, Set<SemanticEvent> eventCandidates) {
//        List<Invariant> invariants = new ArrayList<>();
//        ContextGenerator contextGenerator = new ContextGenerator(traces, eventCandidates);
//        //approach I:
//        //for (Context context : generateCtxtCandidates(traces, eventCandidates)) {
//        //approach II:
//        for (Context context : contextGenerator) {
//            if (ifStateNotFail(infer(traces, context)))
//                invariants.add(genInv(context));
//        }
//        return invariants;
//    }

    @Override
    public String toString() {
        return getClass().getName();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        return true;
    }
}
