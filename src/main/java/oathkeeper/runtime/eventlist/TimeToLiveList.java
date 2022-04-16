package oathkeeper.runtime.eventlist;

import oathkeeper.runtime.event.SemanticEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A high-concurrent and gc-optimized implementation for event list, however, this design still suffers from gc memory overhead issue,
 * thus we move to ring buffer
 */
@Deprecated
public class TimeToLiveList{

    private Map<Integer, ConcurrentLinkedQueue<SemanticEvent>> queueMap = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<SemanticEvent>>();
    private final int timeToLive;
    private long lastCycledTime = 0;
    private AtomicInteger queueCursor = new AtomicInteger(0);
    private AtomicInteger queueSize = new AtomicInteger(0);
    public int totalSizeForTraceGen = 0;
    private int currentWaterMark = 0;
    private Map<Integer, Long> lastElemTimeOfEachQueue = new ConcurrentHashMap<Integer, Long>();

    private final int MAX_QUEUE_SIZE = 1000;

    public TimeToLiveList(int timeToLive)
    {
        this.timeToLive = timeToLive;
    }

    public void garbageCollect()
    {
        //means this is in the test
        if(timeToLive == Integer.MAX_VALUE)
            return;

        long currentTime = System.currentTimeMillis();

        //we don't want to recycle to exccessive
        if(currentTime - lastCycledTime < timeToLive)
            return;

        while(lastElemTimeOfEachQueue.containsKey(currentWaterMark) && currentTime - lastElemTimeOfEachQueue.get(currentWaterMark) > timeToLive)
        {
            queueMap.remove(currentWaterMark);
            lastElemTimeOfEachQueue.remove(currentWaterMark);
            currentWaterMark++;
        }
        lastCycledTime = currentTime;
    }

    public void add(SemanticEvent event)
    {
        int i = queueCursor.get();
        if(!queueMap.containsKey(i))
            queueMap.put(i, new ConcurrentLinkedQueue<>());

        queueMap.get(i).add(event);
        if(queueSize.incrementAndGet()>MAX_QUEUE_SIZE)
        {
            lastElemTimeOfEachQueue.put(i,event.system_timestamp);
            queueCursor.incrementAndGet();
            queueSize.set(0);
        }
        totalSizeForTraceGen++;
    }

    //public SemanticEvent get(int i)
    //{
    //    return contents.get(i);
    //}

    public List<SemanticEvent> clone()
    {
        List<SemanticEvent> events = new ArrayList<>();
        for(int i=currentWaterMark;i<=queueCursor.get();++i)
        {
            if(queueMap.containsKey(i))
            {
                ConcurrentLinkedQueue<SemanticEvent> queue = queueMap.get(i);
                events.addAll(new ArrayList<>(queue));
            }
        }
        return events;
//
//        //because we no longer make access to list synchronized, need to reorder here
//        //lst.contents.sort(new Comparator<SemanticEvent>() {
//        //    public int compare(SemanticEvent left, SemanticEvent right) {
//        //        return Long.compare(left.logical_timestamp, right.logical_timestamp);
//        //    }
//        //});
//
    }

}
