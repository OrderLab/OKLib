package oathkeeper.runtime.invariant;

import oathkeeper.runtime.event.SemanticEvent;

import java.util.Objects;

public class Context {
    public SemanticEvent left=null;
    public SemanticEvent right=null;
    public SemanticEvent secondright=null;

    public String stateId;

    public long constant;

    public long timeInterval;

    // update: we discard this triggering design to be periodical
    //this should satisfy two properties
    //1) accuracy: not trigger at an intermediate state, e.g. for A invoke B, should not immediately check after A
    //2) completeness: should guarantee a violation should always be checked
    //public SemanticEvent triggering_point;

    public Context() {
        //empty
    }

    public Context(SemanticEvent left, SemanticEvent right) {
        this.left = left;
        this.right = right;
    }

    public Context(SemanticEvent left, SemanticEvent right, SemanticEvent secondright) {
        this.left = left;
        this.right = right;
        this.secondright = secondright;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Context context = (Context) o;
        return Objects.equals(left, context.left) &&
                Objects.equals(right, context.right) &&
                Objects.equals(secondright, context.secondright);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right, secondright);
    }

    @Override
    public String toString() {
        return "Context{" +
                "left=" + left +
                ", right=" + right +
                ", secondright=" + secondright +
                ", stateId='" + stateId + '\'' +
                ", constant=" + constant +
                ", timeInterval=" + timeInterval +
                '}';
    }
}
