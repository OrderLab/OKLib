package oathkeeper.runtime.event;

import java.util.Objects;

public class StateUpdateEvent extends SemanticEvent{

    public String stateName;
    public String sourceMethodName;
    public long updatedValue;

    //public int hashCode;

    public StateUpdateEvent() {
        this.stateName = "";
        this.sourceMethodName = "";
        this.updatedValue = -1;
        //this.hashCode = Objects.hash(stateName, sourceMethodName);
    }

    public StateUpdateEvent(String stateName, String sourceMethodName, long updatedValue) {
        this.stateName = stateName;
        this.sourceMethodName = sourceMethodName;
        this.updatedValue = updatedValue;
        //this.hashCode = Objects.hash(stateName, sourceMethodName);
    }

    public StateUpdateEvent(String stateName, String sourceMethodName, long updatedValue, long sys_time, long log_time) {
        this(stateName,sourceMethodName,updatedValue);
        this.system_timestamp = sys_time;
        this.logical_timestamp = log_time;
    }

    public String getMapKey()
    {
        return stateName+":"+sourceMethodName;
    }

    @Override
    public String toString() {
        return "StateUpdateEvent{" +
                "stateName='" + stateName + '\'' +
                ", system_timestamp=" + system_timestamp +
                ", logical_timestamp=" + logical_timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateUpdateEvent that = (StateUpdateEvent) o;
        //return hashCode == that.hashCode;
        return hashCode() == that.hashCode();

    }

    @Override
    public int hashCode() {
        // return hashCode;
        return Objects.hash(stateName, sourceMethodName);
    }

    public SemanticEvent clone()
    {
        return new StateUpdateEvent(stateName, sourceMethodName, updatedValue, system_timestamp, logical_timestamp);
    }
}
