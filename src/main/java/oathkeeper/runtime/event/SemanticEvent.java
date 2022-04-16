package oathkeeper.runtime.event;

public abstract class SemanticEvent {
    public long system_timestamp;
    //this will actually be used when traversing with eventMap, so need to be valid!
    public long logical_timestamp;

    abstract public String getMapKey();

    abstract public SemanticEvent clone();
}
