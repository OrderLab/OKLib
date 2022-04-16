//package oathkeeper.runtime.template;
//
//import oathkeeper.runtime.event.OpTriggerEvent;
//import oathkeeper.runtime.event.SemanticEvent;
//import oathkeeper.runtime.invariant.Context;
//import oathkeeper.runtime.invariant.Invariant;
//
//@Deprecated
//public class OpAddTimeDenyOpTemplate extends Template {
//
//    final long DELTA_INTERVAL_MS = 1000;
//
//    public String getTemplateName() {
//        return "OpAddTimeDenyOpTemplate";
//    }
//
//    public int getOperatorSize() {
//        return 2;
//    }
//
//    public boolean checkLeftEventClass(SemanticEvent event)
//    {
//        return event instanceof OpTriggerEvent;
//    }
//
//    public boolean checkRightEventClass(SemanticEvent event)
//    {
//        return event instanceof OpTriggerEvent;
//    }
//
//    public Invariant genInv(Context context) {
//        return new Invariant(new OpAddTimeDenyOpTemplate(), context);
//    }
//
//    public class InferScanner extends Scanner {
//        Context context;
//        State state;
//
//        public InferScanner(Context context) {
//            this.context = context;
//        }
//
//        class State {
//            boolean ifHold = false;
//            boolean ifActivated = false;
//            int count = 0;
//            long startTime = Long.MAX_VALUE - 1;
//            long lastSecondRightEventTime = Long.MAX_VALUE - 1;
//            long lastRightEventTime = Long.MAX_VALUE - 1;
//            long lastAnyEventTime = Long.MAX_VALUE - 1;
//        }
//
//        public void prescan() {
//            state = new State();
//        }
//
//        public boolean scan(SemanticEvent event) {
//            if (event.equals(context.left)) {
//                state.startTime = event.system_timestamp;
//            } else if (event.equals(context.right)) {
//                state.lastSecondRightEventTime = state.lastRightEventTime;
//                state.lastRightEventTime = event.system_timestamp;
//                state.count++;
//            }
//            state.lastAnyEventTime = event.system_timestamp;
//
//            return true;
//        }
//
//        public void postscan() {
//            if (state.count >= 3) {
//                if((state.lastAnyEventTime - state.lastRightEventTime) > (state.lastRightEventTime- state.lastSecondRightEventTime))
//                {
//                    context.timeInterval = state.lastRightEventTime - state.startTime;
//                    state.ifHold = true;
//                    return;
//                }
//            }
//
//            state.ifHold = false;
//        }
//
//        public Invariant.InvState getRetVal() {
//            if(!state.ifHold)
//                return Invariant.InvState.FAIL;
//            else{
//                if(state.ifActivated)
//                    return Invariant.InvState.PASS;
//                else return Invariant.InvState.INACTIVE;
//            }
//        }
//    }
//
//    public class VerifyScanner extends Scanner {
//        Context context;
//        State state;
//
//        public VerifyScanner(Context context) {
//            this.context = context;
//        }
//
//        class State {
//            boolean ifHold = true;
//            boolean ifActivated = false;
//            long startTime = Long.MAX_VALUE - 1;
//        }
//
//        public void prescan() {
//            state = new State();
//        }
//
//        public boolean scan(SemanticEvent event) {
//            if (event.equals(context.left)) {
//                state.startTime = event.system_timestamp;
//            } else if (event.equals(context.right)) {
//                if(event.system_timestamp-state.startTime>=context.timeInterval+DELTA_INTERVAL_MS);
//                {
//                    state.ifHold = false;
//                    return false;
//                }
//            }
//
//            return true;
//        }
//
//        public void postscan() {
//        }
//
//        public Invariant.InvState getRetVal() {
//            if(!state.ifHold)
//                return Invariant.InvState.FAIL;
//            else{
//                if(state.ifActivated)
//                    return Invariant.InvState.PASS;
//                else return Invariant.InvState.INACTIVE;
//            }
//        }
//    }
//
//    public Scanner getInferScanner(Context context) {
//        return new InferScanner(context);
//    }
//
//    public Scanner getVerifyScanner(Context context) {
//        return new VerifyScanner(context);
//    }
//
//    public Template invert() {
//        return new OpAddTimeInvokeOpTemplate();
//    }
//}
