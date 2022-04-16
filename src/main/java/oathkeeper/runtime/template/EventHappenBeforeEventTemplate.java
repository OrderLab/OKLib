package oathkeeper.runtime.template;

import oathkeeper.runtime.OKHelper;
import oathkeeper.runtime.event.OpTriggerEvent;
import oathkeeper.runtime.event.SemanticEvent;
import oathkeeper.runtime.event.StateUpdateEvent;
import oathkeeper.runtime.invariant.Context;
import oathkeeper.runtime.invariant.Invariant;

import java.util.*;

public class EventHappenBeforeEventTemplate extends Template {
    public String getTemplateName() {
        return "EventHappenBeforeEventTemplate";
    }

    public Invariant genInv(Context context) {
        if(context.left instanceof OpTriggerEvent && context.right instanceof OpTriggerEvent)
            return new Invariant(new OpHappenBeforeOpTemplate(), context);
        else if(context.left instanceof StateUpdateEvent && context.right instanceof OpTriggerEvent)
            return new Invariant(new StateChangeHappenBeforeOpTemplate(), context);
        else if(context.left instanceof OpTriggerEvent && context.right instanceof StateUpdateEvent)
            return new Invariant(new OpHappenBeforeStateChangeTemplate(), context);
        else if(context.left instanceof StateUpdateEvent && context.right instanceof StateUpdateEvent)
            return new Invariant(new StateChangeHappenBeforeStateChangeTemplate(), context);
        else
            return null;
    }

    public class InferScanner extends Template.InferScanner{
        Map<SemanticEvent, Map<SemanticEvent, Integer>> counterMap = new HashMap<>();
        Set<SemanticEvent> appearSet = new HashSet<>();

        public void prescan(Set<SemanticEvent> eventSet)
        {
            //System.out.println(eventSet.size());
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

            appearSet.add(event);

            //appear before, so we update all counters
            //phase 1: update as left operator
            for (Map.Entry<SemanticEvent, Integer> entry : counterMap.get(event).entrySet()) {
                if (entry.getValue() > 0)
                    //mark as failed, no longer come down
                    continue;

                // 1-->2 (-1)
                //  -->3 (-1)
                // =>
                // 1-->2 (-2)
                //  -->3 (-2)
                entry.setValue(entry.getValue() - 1);
            }

            //phase 2: update as right operator
            for (SemanticEvent event1 : counterMap.keySet()) {
                if (event.equals(event1))
                    continue;

                // 2-->1 (-1)
                // =>
                // 2-->1 (0)
                if(appearSet.contains(event1))
                {
                    Integer val = counterMap.get(event1).get(event);
                    counterMap.get(event1).put(event, val + 1);
                }
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
            boolean ifLeftAppear = false;
            boolean ifRightAppear = false;
            int counter = 0;
        }

        public void prescan() {
            state = new State();
        }

        public boolean scan(SemanticEvent event) {
            if (event.equals(context.left)) {
                state.ifLeftAppear = true;
                state.counter--;
                OKHelper.debug("counter--");
            } else if (event.equals(context.right)) {
                state.ifRightAppear = true;

                if(state.ifLeftAppear)
                {
                    state.counter++;
                    OKHelper.debug("counter++");
                }
            }

            if(state.counter>0)
            {
                state.ifHold = false;
                return false;
            }

            return true;
        }


        public void postscan() {
            OKHelper.debug("counter:"+state.counter);
            state.ifActivated = (state.ifLeftAppear&&state.ifRightAppear);
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
