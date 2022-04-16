package oathkeeper.runtime.template_v1;

import oathkeeper.runtime.event.SemanticEvent;
import oathkeeper.runtime.invariant.Context;
import oathkeeper.runtime.invariant.Invariant;

public class EventProtectedByEventTemplateV1 extends TemplateV1 {
    public String getTemplateName() {
        return "EventProtectedByEventTemplateV1";
    }

    public int getOperatorSize() {
        return 2;
    }

    public Invariant genInv(Context context) {
        return new Invariant(new EventProtectedByEventTemplateV1(), context);
    }

    public class Scanner extends TemplateV1.Scanner
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
                state.ifActivated = true;

            } else if (event.equals(context.right)) {
                state.counter--;
                //state.ifActivated = true;

            }

            if(state.counter>0)
            {
                state.ifHold = false;
                return false;
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
        public Scanner(Context context) {
            this.context = context;
        }
    }

    public TemplateV1.Scanner getInferScanner(Context context)
    {
        return new Scanner(context);
    }
    public TemplateV1.Scanner getVerifyScanner(Context context)
    {
        return new Scanner(context);
    }

    public TemplateV1 invert()
    {
        throw new RuntimeException("IMPOSSIBLE");
    }
}
