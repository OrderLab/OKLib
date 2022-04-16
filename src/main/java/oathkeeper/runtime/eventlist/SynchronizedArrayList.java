package oathkeeper.runtime.eventlist;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SynchronizedArrayList<T> implements EventList<T> {

    List<T> list;

    public SynchronizedArrayList() {
        this.list = Collections.synchronizedList(new ArrayList<T>());
    }

    public void add(T t) {
        list.add(t);
    }

    public int size()
    {
        return list.size();
    }

    public T get(int index)
    {
        return list.get(index);
    }
}
