package oathkeeper.runtime.template;

import oathkeeper.runtime.EventTracer;
import oathkeeper.runtime.event.OpTriggerEvent;
import oathkeeper.runtime.event.SemanticEvent;
import oathkeeper.runtime.event.StateUpdateEvent;
import oathkeeper.runtime.invariant.Context;
import oathkeeper.runtime.invariant.Invariant;

import java.util.*;

public class StateEqualsDenyOpTemplate extends Template {
    // semantics: (s = c)⊕q

    public String getTemplateName() {
        return "StateEqualsDenyOpTemplate";
    }

    public int getOperatorSize() {
        return 2;
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

    public Invariant genInv(Context context) {
        return new Invariant(new StateEqualsDenyOpTemplate(), context);
    }

    public class InferScanner extends Template.InferScanner{
        SemanticEvent stateEvent;
        State state;

        public InferScanner() {

        }

        class State {
            //between current state value to next state value update, there is a window, so we need to compare these windows
            //and find what are missing
            long currentStateValue = 0;
            Map<Long, Set<SemanticEvent>> opListInDifferentWindows = new HashMap<>();
            //a map records that how many times those operations appear, we would like to include
            //those missed once
            Map<SemanticEvent, List<Long>> counters = new HashMap<>();
        }

        public void prescan(Set<SemanticEvent> eventSet)
        {
            state = new State();
        }

        public void scan(SemanticEvent event) {
            if (event.equals(stateEvent) && event instanceof StateUpdateEvent) {

                StateUpdateEvent event1 = (StateUpdateEvent) event;
                state.currentStateValue = event1.updatedValue;

            }  else if (event instanceof OpTriggerEvent)
            {
                state.opListInDifferentWindows.putIfAbsent(state.currentStateValue, new HashSet<>());
                if(!state.opListInDifferentWindows.get(state.currentStateValue).contains(event))
                {
                    state.opListInDifferentWindows.get(state.currentStateValue).add(event);
                    state.counters.putIfAbsent(event,new ArrayList<>());
                    state.counters.get(event).add(state.currentStateValue);
                }
            }

        }

        //the algorithm here is to compare op sets in different windows and return those operations missed in single window
        public List<Invariant> postscan() {
            List<Invariant> invs = new ArrayList<>();

            for(Map.Entry<SemanticEvent, List<Long>> entry:state.counters.entrySet())
            {
                if(entry.getValue().size()==state.opListInDifferentWindows.keySet().size()-1)
                {
                    Context context = new Context();
                    context.left = stateEvent;
                    context.right = entry.getKey();
                    HashSet<Long> set = new HashSet<>(state.opListInDifferentWindows.keySet());
                    set.removeAll(entry.getValue());
                    if(set.isEmpty())
                        throw new RuntimeException("IMPOSSIBLE!");
                    else
                        context.constant = set.iterator().next();
                    Invariant inv = genInv(context);
                    if(inv!=null)
                        invs.add(inv);
                }
            }

            return invs;
        }

    }

    private Set<SemanticEvent> getStateSet(EventTracer tracer)
    {
        Set<SemanticEvent> set = new HashSet<>();
        for (SemanticEvent event:tracer)
            set.add(event);
        return set;
    }

    //this takes multiple passes, so override
    @Override
    public List<Invariant> infer(EventTracer tracer) {
        InferScanner scanner = (InferScanner)getInferScanner();
        List<Invariant> lst = new ArrayList<>();
        Set<SemanticEvent> stateSet = getStateSet(tracer);

        for(SemanticEvent state: stateSet)
        {
            scanner.prescan(null);
            scanner.stateEvent= state;
            for (SemanticEvent event:tracer)
                scanner.scan(event);
            lst.addAll(scanner.postscan());
        }
        return lst;
    }

    public class VerifyScanner extends Template.VerifyScanner {
        Context context;
        State state;

        public VerifyScanner(Context context) {
            this.context = context;
        }

        class State {
            boolean ifHold = true;
            boolean ifActivated = false;
            boolean ifShouldDeny = false;
        }

        public void prescan() {
            state = new State();
        }

        public boolean scan(SemanticEvent event) {
            if (event.equals(context.left) && event instanceof StateUpdateEvent) {
                StateUpdateEvent event1 = (StateUpdateEvent) event;
                state.ifShouldDeny = event1.updatedValue == context.constant;
                state.ifActivated = true;

            } else if (event.equals(context.right)) {
                //state.ifActivated = true;

                if(state.ifShouldDeny)
                {
                    state.ifHold = false;
                    return false;
                }
            }

            return true;
        }

        public void postscan() {
        }

        public Invariant.InvState getRetVal() {
            if(!state.ifHold)
                return Invariant.InvState.FAIL;
            else{
                if(state.ifActivated)
                    return Invariant.InvState.PASS;
                else return Invariant.InvState.INACTIVE;
            }
        }
    }

    public Template.InferScanner getInferScanner() {
        return new InferScanner();
    }

    public Template.VerifyScanner getVerifyScanner(Context context) {
        return new VerifyScanner(context);
    }


    public Template invert() {
        return this;
    }
}
