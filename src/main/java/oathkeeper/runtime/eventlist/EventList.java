package oathkeeper.runtime.eventlist;

import oathkeeper.runtime.event.SemanticEvent;

public interface EventList<T> {

    public void add(T t);

    public int size();

    public T get(int index);
}
