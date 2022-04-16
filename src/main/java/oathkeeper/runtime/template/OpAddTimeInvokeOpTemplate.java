package oathkeeper.runtime.template;

import oathkeeper.runtime.event.OpTriggerEvent;
import oathkeeper.runtime.event.SemanticEvent;
import oathkeeper.runtime.invariant.Context;
import oathkeeper.runtime.invariant.Invariant;

import java.util.*;

public class OpAddTimeInvokeOpTemplate extends Template {
    //Semantics: p + ∆t ⇒ q

    final long DELTA_INTERVAL_MS = 200;

    //it's meaningless to have invariants like after 1 ms another event happens, so we exclude that
    final long THRESHOLD_INTERVAL_MS = 500;

    public String getTemplateName() {
        return "OpAddTimeInvokeOpTemplate";
    }

    public int getOperatorSize() {
        return 2;
    }

    @Override
    public boolean checkLeftEventClass(SemanticEvent event)
    {
        return event instanceof OpTriggerEvent;
    }

    @Override
    public boolean checkRightEventClass(SemanticEvent event)
    {
        return event instanceof OpTriggerEvent;
    }

    public Invariant genInv(Context context) {
        return new Invariant(new OpAddTimeInvokeOpTemplate(), context);
    }

    public class InferScanner extends Template.InferScanner{
        Map<SemanticEvent, Long> lastAccessTimes = new HashMap<>();
        //for op B comes in, it updates <B, <xx, interval>>
        Map<SemanticEvent, Map<SemanticEvent, Long>> hyposisMap = new HashMap<>();
        Map<SemanticEvent, Map<SemanticEvent, Integer>> confidenceMap = new HashMap<>();

        public InferScanner() {

        }

        public void prescan(Set<SemanticEvent> eventSet)
        {
        }

        //the algorithm here is to maintain a list of queue for each op, when op comes it goes to the corresponding
        //queue and enqueue, and **attempt** to resolve the other queues
        public void scan(SemanticEvent event) {
            if(!(event instanceof OpTriggerEvent))
            {
                return;
            }

            lastAccessTimes.put(event, event.system_timestamp);
            hyposisMap.putIfAbsent(event, new HashMap<>());
            confidenceMap.putIfAbsent(event, new HashMap<>());
            for(SemanticEvent event1:lastAccessTimes.keySet())
            {
                Map<SemanticEvent, Long> map = hyposisMap.get(event);
                if(map.containsKey(event1))
                {
                    if(ifWithinRange(lastAccessTimes.get(event)-lastAccessTimes.get(event1),
                            map.get(event1)))
                    {
                        Integer confidence = confidenceMap.get(event).get(event1);
                        confidenceMap.get(event).put(event1, confidence+1);
                        map.put(event1, (map.get(event1)+lastAccessTimes.get(event)-lastAccessTimes.get(event1))/2);
                    }
                }
                else {
                    hyposisMap.get(event).put(event1, lastAccessTimes.get(event)-lastAccessTimes.get(event1));
                    confidenceMap.get(event).put(event1,0);
                }
            }

        }

        public List<Invariant> postscan() {
            List<Invariant> invs = new ArrayList<>();
            for(Map.Entry<SemanticEvent, Map<SemanticEvent, Integer>> entry: confidenceMap.entrySet())
            {
                for(Map.Entry<SemanticEvent, Integer> entry1 : entry.getValue().entrySet())
                {
                    //if we have quite some confidence about this rule
                    if(entry1.getValue()>=2)
                    {
                        Context context = new Context(entry1.getKey(), entry.getKey());
                        context.timeInterval = hyposisMap.get(entry.getKey()).get(entry1.getKey());
                        invs.add(genInv(context));
                        Invariant inv = genInv(context);
                        if(inv!=null)
                            invs.add(inv);
                    }
                }

            }

            return invs;
        }

        private boolean ifWithinRange(long calculatedTime, long actualTime)
        {
            if(calculatedTime<THRESHOLD_INTERVAL_MS || actualTime < THRESHOLD_INTERVAL_MS)
                return false;

            long abs = Math.abs(calculatedTime-actualTime);
            return abs < DELTA_INTERVAL_MS;
        }
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
            List<Long> queue = new ArrayList<>();
        }

        public void prescan() {
            state = new State();
        }

        public boolean scan(SemanticEvent event) {
            if (event.equals(context.left)) {
                state.ifActivated = true;
                state.queue.add(event.system_timestamp);
                return true;
            }

            if(state.queue.isEmpty())
            {
                return true;
            }

            //scenario 1: too late
            if (event.system_timestamp - state.queue.get(state.queue.size()-1) >= context.timeInterval + DELTA_INTERVAL_MS) {
                state.ifHold = false;
                return false;
            }

            if(event.equals(context.right))
            {
                //state.ifActivated = true;
                state.queue.remove(0);
            }

            return true;
        }

        public void postscan() {
            //scenario 2: never show up
            if(!state.queue.isEmpty())
            {
                state.ifHold = false;
            }
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
        throw new RuntimeException("IMPOSSIBLE!");
        //return new OpAddTimeDenyOpTemplate();
    }
}
