//package oathkeeper.runtime.template;
//
//import oathkeeper.runtime.event.OpTriggerEvent;
//import oathkeeper.runtime.event.SemanticEvent;
//import oathkeeper.runtime.invariant.Context;
//import oathkeeper.runtime.invariant.Invariant;
//
//@Deprecated
//public class OpMutualExclusiveTemplate extends Template {
//    public String getTemplateName() {
//        return "OpMutualExclusiveTemplate";
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
//        return new Invariant(new OpMutualExclusiveTemplate(), context);
//    }
//
//    public class Scanner extends Template.Scanner
//    {
//        Context context;
//        State state;
//
//        class State{
//            boolean ifHold = true;
//            boolean ifActivated = false;
//            boolean ifLeftAppear = false;
//            boolean ifRightAppear = false;
//        }
//
//        public void prescan() {
//            state = new State();
//        }
//
//        public boolean scan(SemanticEvent event) {
//            if (event.equals(context.left))
//                state.ifLeftAppear = true;
//            if (event.equals(context.right))
//                state.ifRightAppear = true;
//
//            if (state.ifLeftAppear && state.ifRightAppear) {
//                state.ifHold = false;
//                return false;
//            }
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
//
//        public Scanner(Context context) {
//            this.context = context;
//        }
//    }
//
//    public Template.Scanner getInferScanner(Context context)
//    {
//        return new Scanner(context);
//    }
//    public Template.Scanner getVerifyScanner(Context context)
//    {
//        return new Scanner(context);
//    }
//
//    public Template invert()
//    {
//        //omit for now
//        return new AfterOpAtomicStateUpdateTemplate();
//    }
//}
