package oathkeeper.runtime.template_v1;

import oathkeeper.runtime.event.SemanticEvent;
import oathkeeper.runtime.invariant.Context;
import oathkeeper.runtime.invariant.Invariant;

public class EventHappenBeforeEventTemplateV1 extends TemplateV1 {
    public String getTemplateName() {
        return "EventHappenBeforeEventTemplateV1";
    }

    public int getOperatorSize() {
        return 2;
    }

    public Invariant genInv(Context context) {
        return new Invariant(new EventHappenBeforeEventTemplateV1(), context);
    }

    public class Scanner extends TemplateV1.Scanner
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

        //consider only right operator exists, we should start count after seeing first left
        public boolean scan(SemanticEvent event) {
            if (event.equals(context.left)) {
                state.ifLeftAppear = true;

                state.counter--;
            } else if (event.equals(context.right)) {
                state.ifRightAppear = true;

                if(state.ifLeftAppear)
                    state.counter++;
            }

            if(state.counter>0)
            {
                state.ifHold = false;
                return false;
            }

            return true;
        }

        public void postscan() {
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