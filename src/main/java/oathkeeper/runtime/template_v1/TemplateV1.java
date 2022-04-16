package oathkeeper.runtime.template_v1;

import oathkeeper.runtime.ConfigManager;
import oathkeeper.runtime.event.SemanticEvent;
import oathkeeper.runtime.invariant.Context;
import oathkeeper.runtime.invariant.Invariant;
import io.herrmann.generator.Generator;
import oathkeeper.runtime.template.Template;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static oathkeeper.runtime.invariant.Invariant.ifStateNotFail;

public abstract class TemplateV1 {

    public abstract class Scanner {
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

    //how many objects are involved in the operator
    public abstract int getOperatorSize();

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
    public abstract Invariant genInv(Context context);

    //two sets of scanners
    //infer scanner: heavyweight, to infer missing variables in invariants
    public abstract Scanner getInferScanner(Context context);

    //verify scanner: lightweight, do quick check on given traces
    public abstract Scanner getVerifyScanner(Context context);

    //return true means the relation holds on given traces
    public Invariant.InvState infer(List<SemanticEvent> traces, Context context) {
        Scanner scanner = getInferScanner(context);
        scanner.prescan();
        int size = traces.size();
        //to avoid concurrentModificationEx, use index
        for (int i=0;i<size;++i)
            if (!scanner.scan(traces.get(i)))
                break;
        scanner.postscan();
        return scanner.getRetVal();
    }

    //return true means the relation holds on given traces
    public Invariant.InvState verify(List<SemanticEvent> traces, Context context) {
        Scanner scanner = getVerifyScanner(context);
        scanner.prescan();
        int size = traces.size();
        //to avoid concurrentModificationEx, use index
        for (int i=0;i<size;++i)
            if (!scanner.scan(traces.get(i)))
                break;
        scanner.postscan();
        return scanner.getRetVal();
    }

    //invert to opposite version of template
    public abstract TemplateV1 invert();

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

    public class ContextGenerator extends Generator<Context> {
        List<SemanticEvent> traces;
        Set<SemanticEvent> eventCandidates;

        public ContextGenerator(List<SemanticEvent> traces, Set<SemanticEvent> eventCandidates) {
            this.traces=traces;
            this.eventCandidates=eventCandidates;
        }

        @Override
        protected void run() throws InterruptedException {
            if (getOperatorSize() == 1) {
                for (SemanticEvent event : eventCandidates) {
                    if (checkLeftEventClass(event))
                        yield(new Context(event, null));
                }
            } else if (getOperatorSize() == 2) {

                Set<SemanticEvent> totalEvents = new HashSet<SemanticEvent>(traces);
                for (SemanticEvent event : eventCandidates) {
                    if (checkLeftEventClass(event))
                        for (SemanticEvent event2 : totalEvents) {
                            if (event.equals(event2)) continue;

                            if (checkRightEventClass(event2))
                            {
                                Context context = new Context(event, event2);
                                    yield(context);
                            }
                        }
                }
            } else if (getOperatorSize() == 3) {

                Set<SemanticEvent> totalEvents = new HashSet<SemanticEvent>(traces);
                for (SemanticEvent event : eventCandidates) {
                    if (checkLeftEventClass(event))
                        for (SemanticEvent event2 : totalEvents) {
                            if (event.equals(event2)) continue;

                            if (checkRightEventClass(event2)) {
                                for (SemanticEvent event3 : totalEvents) {
                                    if (event3.equals(event)) continue;

                                    if (event3.equals(event2)) continue;

                                    if (checkSecondRightEventClass(event3)) {
                                        Context context = new Context(event, event2, event3);
                                            yield(context);
                                    }
                                }
                            }
                        }
                }
            }
        }
    }

    public List<Context> generateCtxtCandidates(List<SemanticEvent> traces, Set<SemanticEvent> eventCandidates) {
        List<Context> contexts = new ArrayList<>();
        if (getOperatorSize() == 1) {
            for (SemanticEvent event : eventCandidates) {
                if (checkLeftEventClass(event))
                    contexts.add(new Context(event, null));
            }
        } else if (getOperatorSize() == 2) {

            Set<SemanticEvent> totalEvents = new HashSet<SemanticEvent>(traces);
            for (SemanticEvent event : eventCandidates) {
                if (checkLeftEventClass(event))
                    for (SemanticEvent event2 : totalEvents) {
                        if (event.equals(event2)) continue;

                        if (checkRightEventClass(event2))
                        {
                            Context context = new Context(event, event2);
                            if(checkTimeConstraints(context))
                                contexts.add(context);
                        }
                    }
            }
        } else if (getOperatorSize() == 3) {

            Set<SemanticEvent> totalEvents = new HashSet<SemanticEvent>(traces);
            for (SemanticEvent event : eventCandidates) {
                if (checkLeftEventClass(event))
                    for (SemanticEvent event2 : totalEvents) {
                        if (event.equals(event2)) continue;

                        if (checkRightEventClass(event2))
                        {
                            for (SemanticEvent event3 : totalEvents) {
                                if (event3.equals(event)) continue;

                                if (event3.equals(event2)) continue;

                                if(checkSecondRightEventClass(event3))
                                {
                                    Context context = new Context(event, event2, event3);
                                    if(checkTimeConstraints(context))
                                        contexts.add(context);
                                }
                            }
                        }
                    }
            }
        }

        System.out.println("Generate " + contexts.size() + " contexts");

        //TODO: fix me
        if(contexts.size()>100000)
        {
            System.err.println("Context size > threshold. Abort.");
            return new ArrayList<>();
        }
        return contexts;
    }

    public List<Invariant> infer(List<SemanticEvent> traces, Set<SemanticEvent> eventCandidates) {
        List<Invariant> invariants = new ArrayList<>();
        ContextGenerator contextGenerator = new ContextGenerator(traces, eventCandidates);
        //approach I:
        //for (Context context : generateCtxtCandidates(traces, eventCandidates)) {
        //approach II:
        for (Context context : contextGenerator) {
            if (ifStateNotFail(infer(traces, context)))
                invariants.add(genInv(context));
        }
        return invariants;
    }

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