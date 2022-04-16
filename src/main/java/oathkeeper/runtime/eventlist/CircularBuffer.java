package oathkeeper.runtime.eventlist;

import oathkeeper.runtime.event.MarkerEvent;
import oathkeeper.runtime.event.OpTriggerEvent;
import oathkeeper.runtime.event.StateUpdateEvent;

import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

//Modified based on https://github.com/asgeirn/circular-buffer/blob/master/src/main/java/no/twingine/CircularBuffer.java
/**
 * A lock-free thread safe circular fixed length buffer.
 *
 * Uses an AtomicLong as index counter and an AtomicReferenceArray to hold the references to the values.
 *
 * When the buffer is full, the oldest item is overwritten.
 *
 */
public class CircularBuffer<T> implements EventList<T> {

    private final AtomicInteger index = new AtomicInteger(0);
    private final AtomicReferenceArray<T> buffer;
    private final int size;

    private Class<T> clazz;

    public CircularBuffer(Class<T> clazz, int size) throws InstantiationException, IllegalAccessException {
        assert size > 0 : "Size must be positive";
        this.size = size;
        buffer = new AtomicReferenceArray<T>(this.size);
        for(int i=0;i<size;++i)
        {
            buffer.set(i, clazz.newInstance());
        }
    }

    public int size() {
        return size;
    }

    public void add(T item) {
        assert item != null : "Item must be non-null";
        T t = buffer.get((int) (index.getAndIncrement() % size));

        if(t instanceof OpTriggerEvent)
        {
            ((OpTriggerEvent) t).opName = ((OpTriggerEvent) item).opName;
            ((OpTriggerEvent) t).logical_timestamp = ((OpTriggerEvent) item).logical_timestamp;
            ((OpTriggerEvent) t).system_timestamp = ((OpTriggerEvent) item).system_timestamp;
        }
        else if(t instanceof StateUpdateEvent)
        {
            ((StateUpdateEvent) t).stateName = ((StateUpdateEvent) item).stateName;
            ((StateUpdateEvent) t).sourceMethodName = ((StateUpdateEvent) item).sourceMethodName;
            ((StateUpdateEvent) t).updatedValue = ((StateUpdateEvent) item).updatedValue;
            ((StateUpdateEvent) t).logical_timestamp = ((StateUpdateEvent) item).logical_timestamp;
            ((StateUpdateEvent) t).system_timestamp = ((StateUpdateEvent) item).system_timestamp;
        }
        else if(t instanceof MarkerEvent)
        {
            ((MarkerEvent) t).marker = ((MarkerEvent) item).marker;
            ((MarkerEvent) t).logical_timestamp = ((MarkerEvent) item).logical_timestamp;
            ((MarkerEvent) t).system_timestamp = ((MarkerEvent) item).system_timestamp;
        }


        //buffer.set((int) (index.getAndIncrement() % size), item);
    }

    public T get() {
        return buffer.get((int) (index.get() % size));
    }

    public T get(int i) {
        return buffer.get((int) (i % size));
    }

    public T take(AtomicInteger idx) {
        if (idx.get() >= index.get())
            return null;
        if (index.get() - idx.get() > size) {
            idx.lazySet(index.get());
            throw new BufferOverflowException();
        }
        return get(idx.getAndIncrement());
    }

    public List<T> drain(AtomicInteger idx) {
        if (index.get() - idx.get() > size) {
            idx.set(index.get());
            throw new BufferOverflowException();
        }
        List<T> result = new ArrayList<T>((int) (index.get()-idx.get()));
        while (idx.get() < index.get())
            result.add(get(idx.getAndIncrement()));
        return result;
    }

    public AtomicInteger index() {
        return new AtomicInteger(index.get());
    }
}