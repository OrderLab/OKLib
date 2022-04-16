package oathkeeper.runtime.template;

import oathkeeper.runtime.OKHelper;
import oathkeeper.runtime.event.OpTriggerEvent;
import oathkeeper.runtime.event.SemanticEvent;
import oathkeeper.runtime.event.StateUpdateEvent;
import oathkeeper.runtime.invariant.Context;
import oathkeeper.runtime.invariant.Invariant;

import java.util.*;

public class EventProtectedByEventTemplate extends Template {
    public String getTemplateName() {
        return "EventProtectedByEventTemplate";
    }

    public int getOperatorSize() {
        return 2;
    }

    public Invariant genInv(Context context) {
        if(context.left instanceof OpTriggerEvent && context.right instanceof OpTriggerEvent)
            return new Invariant(new OpProtectedByOpTemplate(), context);
        else if(context.left instanceof StateUpdateEvent && context.right instanceof OpTriggerEvent)
            return new Invariant(new StateChangeProtectedByOpTemplate(), context);
        else if(context.left instanceof OpTriggerEvent && context.right instanceof StateUpdateEvent)
            return new Invariant(new OpProtectedByStateChangeTemplate(), context);
        else if(context.left instanceof StateUpdateEvent && context.right instanceof StateUpdateEvent)
            return new Invariant(new StateChangeProtectedByStateChangeTemplate(), context);
        else
            return null;
    }

    public class InferScanner extends Template.InferScanner {
        Map<SemanticEvent, Map<SemanticEvent, Integer>> counterMap = new HashMap<>();

        public void prescan(Set<SemanticEvent> eventSet)
        {
            for(SemanticEvent event:eventSet)
            {
                counterMap.put(event, new HashMap<>());
            }

            for(SemanticEvent event:eventSet)
            {
                for(SemanticEvent event1: eventSet)
                {
                    if(!event.equals(event1))
                    {
                        counterMap.get(event).put(event1, 0);
                        counterMap.get(event1).put(event, 0);
                    }
                }
            }
        }

        //structure look like
        //counterMap -------->  1 ---------> 2
        //                                   3
        //                                  ...
        //                      2 ---------> 1
        //                                   3
        //                                  ...
        //                      3 ---------->...
        public void scan(SemanticEvent event) {
            //appear before, so we update all counters
            //phase 1:
            for (Map.Entry<SemanticEvent, Integer> entry : counterMap.get(event).entrySet()) {
                entry.setValue(entry.getValue() + 1);

                if (entry.getValue() > 0)
                    //mark as failed, no longer come down
                    entry.setValue(Integer.MAX_VALUE-1);
            }

            //phase 2:
            for (SemanticEvent event1 : counterMap.keySet()) {
                if (event.equals(event1))
                    continue;

                Integer val = counterMap.get(event1).get(event);

                if(val == Integer.MAX_VALUE-1)
                    continue;

                counterMap.get(event1).put(event, val - 1);
            }


        }

        //check the after scan state, and judge
        public List<Invariant> postscan() {
            List<Invariant> lst= new ArrayList<>();
            for(Map.Entry<SemanticEvent, Map<SemanticEvent, Integer>> entry: counterMap.entrySet())
            {
                for(Map.Entry<SemanticEvent, Integer> subEntry: entry.getValue().entrySet())
                {
                    if(subEntry.getValue()<=0)
                    {
                        Invariant inv = genInv(new Context(
                                entry.getKey(),
                                subEntry.getKey()
                        ));
                        if(inv!=null)
                            lst.add(inv);
                    }
                }
            }

            return lst;
        }
    }

    public class VerifyScanner extends Template.VerifyScanner
    {
        Context context;
        State state;
        class State{
            boolean ifHold = true;
            boolean ifActivated = false;
            int counter = 0;
        }

        public void prescan() {
            state = new State();
        }

        public boolean scan(SemanticEvent event) {
            if (event.equals(context.left)) {
                state.counter++;
                OKHelper.debug("counter++");
                state.ifActivated = true;

                if(state.counter>0)
                {
                    state.ifHold = false;
                    return false;
                }
            } else if (event.equals(context.right)) {
                state.counter--;
                OKHelper.debug("counter--");
                //state.ifActivated = true;

            }

            return true;
        }

        public void postscan() {
            OKHelper.debug("counter:"+state.counter);
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

    public Template.InferScanner getInferScanner()
    {
        return new InferScanner();
    }
    public Template.VerifyScanner getVerifyScanner(Context context)
    {
        return new VerifyScanner(context);
    }

    public Template invert()
    {
        throw new RuntimeException("IMPOSSIBLE");
    }
}
