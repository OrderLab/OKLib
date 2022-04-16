//package oathkeeper.runtime.template;
//
//import oathkeeper.runtime.event.OpTriggerEvent;
//import oathkeeper.runtime.event.SemanticEvent;
//import oathkeeper.runtime.invariant.Context;
//import oathkeeper.runtime.invariant.Invariant;
//
//@Deprecated
//public class OpPeriodicTemplate extends Template {
//
//    //time that can be tolerated when checking
//    final long DELTA_INTERVAL_MS = 1000;
//
//    public String getTemplateName() {
//        return "OpPeriodicTemplate";
//    }
//
//    public int getOperatorSize() {
//        return 1;
//    }
//
//    public boolean checkLeftEventClass(SemanticEvent event)
//    {
//        return event instanceof OpTriggerEvent;
//    }
//
//    public boolean checkRightEventClass(SemanticEvent event)
//    {
//        throw new RuntimeException("Impossible");
//    }
//
//    public Invariant genInv(Context context) {
//        return new Invariant(new OpPeriodicTemplate(), context);
//    }
//
//    public class InferScanner extends Scanner {
//        Context context;
//        State state;
//
//        class State {
//            boolean ifHold = false;
//            boolean ifActivated = false;
//            long lastTime = Long.MAX_VALUE - 1;
//            long minInterval = Long.MAX_VALUE - 1;
//            long maxInterval = -1;
//            long totalInterval = 0;
//            int count = 0;
//        }
//
//        public InferScanner(Context context) {
//            this.context = context;
//        }
//
//        public void prescan() {
//            state = new State();
//        }
//
//        public boolean scan(SemanticEvent event) {
//            if (event.equals(context.left)) {
//                if (state.count != 0) {
//                    long interval = event.system_timestamp - state.lastTime;
//                    if (interval > state.maxInterval)
//                        state.maxInterval = interval;
//                    if (interval < state.minInterval)
//                        state.minInterval = interval;
//
//                    state.totalInterval += interval;
//                }
//
//                state.lastTime = event.system_timestamp;
//                state.count++;
//            }
//
//            return true;
//        }
//
//        public void postscan() {
//            if (state.count >= 3) {
//                if (state.maxInterval - state.minInterval <= 2 * DELTA_INTERVAL_MS) {
//                    context.timeInterval = (state.maxInterval+state.minInterval)/2;
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
//            boolean ifHold = false;
//            boolean ifActivated = false;
//            long lastTime = Long.MAX_VALUE - 1;
//            int count = 0;
//        }
//
//        public void prescan() {
//            state = new State();
//        }
//
//        public boolean scan(SemanticEvent event) {
//            if (event.system_timestamp - state.lastTime >= context.timeInterval + DELTA_INTERVAL_MS) {
//                state.ifHold = false;
//                return false;
//            }
//
//            if (event.equals(context.left)) {
//                state.lastTime = event.system_timestamp;
//                state.count++;
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
//        //omit for now
//        return this;
//    }
//}
