package oathkeeper.runtime.template;

import oathkeeper.runtime.EventTracer;
import oathkeeper.runtime.event.OpTriggerEvent;
import oathkeeper.runtime.event.SemanticEvent;
import oathkeeper.runtime.event.StateUpdateEvent;
import oathkeeper.runtime.invariant.Context;
import oathkeeper.runtime.invariant.Invariant;

import java.util.*;

public class AfterOpAtomicStateUpdateTemplate extends Template {
    //this template is kinda like stateupdateatomic, but with an operation to constrain window
    //in other words, when you see op, you should expect state changes both happen or both not happen
    //Semantics: p ⇒ ⊙(s ↑,k ↑)
    public String getTemplateName() {
        return "AfterOpAtomicStateUpdateTemplate";
    }

    public int getOperatorSize() {
        return 3;
    }

    @Override
    public boolean checkLeftEventClass(SemanticEvent event)
    {
        return event instanceof OpTriggerEvent;
    }

    @Override
    public boolean checkRightEventClass(SemanticEvent event)
    {
        return event instanceof StateUpdateEvent;
    }

    @Override
    public boolean checkSecondRightEventClass(SemanticEvent event)
    {
        return event instanceof StateUpdateEvent;
    }

    public Invariant genInv(Context context) {
        return new Invariant(new AfterOpAtomicStateUpdateTemplate(), context);
    }

    public class InferScanner extends Template.InferScanner{
        static final int EVENT_WINDOW_THRESHOLD = 10;
        int stepCounter = 0;

        Map<SemanticEvent, Map<SemanticEvent, Integer>> counterMap = new HashMap<>();
        // S1, S2, OP, counter
        Map<SemanticEvent, List<SemanticEvent>> pairMap= new HashMap<>();
        List<SemanticEvent> opEvents = new ArrayList<>();

        public void prescan(Set<SemanticEvent> eventSet)
        {
        }

        //algorithm is like:
        //phase 1: find all the pairs of state changes
        //phase 2: include all ops with a limited window
        //e.g.  OP1 ⇒ ⊙(S1 ↑,S2 ↑)
        //OP1   OP2     OP3      OP2    OP1   S1    S2
        public void scan(SemanticEvent event) {
            if(!(event instanceof StateUpdateEvent))
                return;

            //this logic is borrowed from imply template
            if(counterMap.containsKey(event))
            {
                //appear before, so we update all counters
                //phase 1:
                for(Map.Entry<SemanticEvent, Integer> entry: counterMap.get(event).entrySet())
                {
                    if(entry.getValue()>=0)
                        entry.setValue(entry.getValue()+1);
                }

                //phase 2:
                for(SemanticEvent event1: counterMap.keySet())
                {
                    if(event.equals(event1))
                        continue;

                    Integer val = counterMap.get(event1).get(event);
                    if(val>0)
                        counterMap.get(event1).put(event, val-1);
                    else
                        //mark as failed
                        counterMap.get(event1).put(event, -1);
                }
            }
            else {
                //never appear before, so we add it
                counterMap.put(event, new HashMap<>());
                for(SemanticEvent event1: counterMap.keySet())
                {
                    if(!event.equals(event1))
                    {
                        //phase 1
                        counterMap.get(event).put(event1, 1);

                        //phase 2
                        counterMap.get(event1).put(event, 0);
                    }
                }

            }
        }

        public void scan2(SemanticEvent event)
        {
            if(pairMap.containsKey(event)) {
                //reset
                stepCounter = EVENT_WINDOW_THRESHOLD;
            }

            if(stepCounter>0) {
                stepCounter--;

                opEvents.add(event);
            }

        }

        public List<Invariant> postscan()
        {
            List<Invariant> lst= new ArrayList<>();
            for(Map.Entry<SemanticEvent, Map<SemanticEvent, Integer>> entry: counterMap.entrySet())
            {
                for(Map.Entry<SemanticEvent, Integer> subEntry: entry.getValue().entrySet())
                {
                    if(subEntry.getValue()==0)
                    {
                        pairMap.putIfAbsent(entry.getKey(), new ArrayList<>());
                        pairMap.get(entry.getKey()).add(subEntry.getKey());
                    }
                }
            }

            return lst;
        }

        public List<Invariant> postscan2()
        {
            List<Invariant> lst= new ArrayList<>();

            for(SemanticEvent opEvent: opEvents) {
                for(Map.Entry<SemanticEvent, List<SemanticEvent>> entry: pairMap.entrySet())
                {
                    for(SemanticEvent sEvent2: entry.getValue())
                    {
                        Invariant inv = genInv(new Context(
                                        opEvent,
                                        entry.getKey(),
                                        sEvent2
                                ));
                        if(inv!=null)
                            lst.add(inv);
                    }
                }

            }
            return lst;
        }
    }

    //this takes multiple passes, so override
    @Override
    public List<Invariant> infer(EventTracer tracer) {
        InferScanner scanner = (InferScanner)getInferScanner();
        scanner.prescan(tracer.getEventSet());
        for (SemanticEvent event:tracer)
            scanner.scan(event);
        scanner.postscan();
        ListIterator<SemanticEvent> it = tracer.iteratorAtTail();
        while(it.hasPrevious()) {
            scanner.scan2(it.previous());
        }
        return scanner.postscan2();
    }

    public class VerifyScanner extends Template.VerifyScanner {

        Context context;
        State state;

        class State {
            boolean ifHold = true;
            boolean ifActivated = false;
            boolean ifLeftAppear = false;
            boolean ifRightAppear = false;
            boolean ifSecondRightAppear = false;
        }

        public void prescan() {
            state = new State();
        }

        public boolean scan(SemanticEvent event) {
            if (event.equals(context.left))
            {
                state.ifLeftAppear = true;
                state.ifActivated = true;
            }
            if (event.equals(context.right))
            {
                state.ifRightAppear = true;
            }
            if (event.equals(context.secondright))
            {
                state.ifSecondRightAppear = true;
            }

            //always finish all traces
            return true;
        }

        public void postscan() {
            if(state.ifLeftAppear)
            {
                state.ifHold = state.ifSecondRightAppear == state.ifRightAppear;
            }
            else
                state.ifHold = true;
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

        public VerifyScanner(Context context) {
            this.context = context;
        }
    }

    public Template.InferScanner getInferScanner() {
        return new InferScanner();
    }

    public Template.VerifyScanner getVerifyScanner(Context context) {
        return new VerifyScanner(context);
    }

    public Template invert() {
        throw new RuntimeException("IMPOSSIBLE");
    }

}
