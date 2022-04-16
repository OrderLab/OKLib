package oathkeeper.runtime.event;

import java.util.Objects;

public class OpTriggerEvent extends SemanticEvent{
    public String opName;
    //public int hashCode;

    public OpTriggerEvent() {
        this.opName = "";
    }

    public OpTriggerEvent(String opName) {
        this.opName = opName;
        //this.hashCode = Objects.hash(opName);
    }

    public OpTriggerEvent(String opName, long sys_time) {
        this(opName);
        this.system_timestamp = sys_time;
    }

    public OpTriggerEvent(String opName, long sys_time, long log_time) {
        this(opName);
        this.system_timestamp = sys_time;
        this.logical_timestamp = log_time;
    }

    public String getMapKey()
    {
        return opName;
    }

    @Override
    public String toString() {
        return "OpTriggerEvent{" +
                "opName='" + opName + '\'' +
                ", system_timestamp=" + system_timestamp +
                ", logical_timestamp=" + logical_timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpTriggerEvent that = (OpTriggerEvent) o;
        //return hashCode == that.hashCode;
        return hashCode() == that.hashCode();
    }

    @Override
    public int hashCode() {
        //return hashCode;
        return Objects.hash(opName);
    }

    public SemanticEvent clone()
    {
        return new OpTriggerEvent(opName, system_timestamp,logical_timestamp);
    }

}
