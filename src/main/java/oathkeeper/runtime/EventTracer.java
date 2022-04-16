package oathkeeper.runtime;

import oathkeeper.runtime.event.MarkerEvent;
import oathkeeper.runtime.event.OpTriggerEvent;
import oathkeeper.runtime.event.SemanticEvent;
import oathkeeper.runtime.event.StateUpdateEvent;
import oathkeeper.runtime.eventlist.EventList;
import oathkeeper.runtime.eventlist.EventListBuilder;
import oathkeeper.runtime.eventlist.TimeToLiveList;
import oathkeeper.runtime.gson.GsonUtils;
import oathkeeper.runtime.invariant.Context;
import oathkeeper.runtime.eventlist.CircularBuffer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static oathkeeper.runtime.ConfigManager.TIME_WINDOW_LENGTH_IN_MILLIS_KEY;

/**
 * Management unit for event traces
 */
//TODO: move to be per instance
public class EventTracer implements Iterable<SemanticEvent> {
    public enum TracerMode {
        TEST_PATCHED,
        TEST_UNPATCHED,
        PRODUCTION,
        ILLEGAL;
    }

    public final static String PATCHED_SUFFIX = "_patched";
    public final static String UNPATCHED_SUFFIX = "_unpatched";

    public static EventTracer instance = new EventTracer();
    static AtomicInteger eventCounter = new AtomicInteger(0);

    static int MAX_EVENT_QUEUE_SIZE = Integer.MAX_VALUE - 1;
    //public List<SemanticEvent> eventQueue = Collections.synchronizedList(new ArrayList());
    //we assume the events in this queue should be totally ordered based on system time
    //we use CopyOnWriteArrayList to avoid ConcurrentModificationException when serializing
    //TODO: this seems introduce significant performance overhead
    public List<SemanticEvent> eventQueue = new CopyOnWriteArrayList<SemanticEvent>();
    //to accelerate verifying, we would store each event based on their type, and would only use those involved in the context
    //to rebuild the queue (virtually)
    // mark transient to be excluded from serialization
    public transient Map<String, EventList> eventMap = new ConcurrentHashMap<>(16,0.75f,128);

    //TODO: double check OK if we use event as key here

    private int queueSize = 0;

    public String tracerName = "uninited";
    public TracerMode mode = TracerMode.ILLEGAL;

    //some cached config
    private boolean running_under_prod_mode = false;
    private boolean force_disable_enqueue_events = false;
    private int time_window_length_in_millis = -1;

    public EventTracer()
    {
        running_under_prod_mode = System.getProperty("ok.invmode") != null &&
                System.getProperty("ok.invmode").equals("prod");
        try{
            force_disable_enqueue_events = ConfigManager.config.getBoolean(ConfigManager.FORCE_DISABLE_ENQUEUE_EVENTS_KEY);
            time_window_length_in_millis = ConfigManager.config.getInt(TIME_WINDOW_LENGTH_IN_MILLIS_KEY);
        }
        catch (Exception e) {
            force_disable_enqueue_events = false;
            time_window_length_in_millis = Integer.MAX_VALUE;
        }
    }

    //TODO: check if internal sync is preserved so this sync is okay to disable
    //UPDATE: need to preserve to avoid java.util.ConcurrentModificationException from oathkeeper.runtime.EventTracer.serialize
    //synchronized
    public void enqueue(SemanticEvent event) {
        try{
            if(running_under_prod_mode && force_disable_enqueue_events)
            {
                return;
            }

            //eventQueue will not be used in the production as we only do check there
            if(!running_under_prod_mode)
            {
                //need to allocate new event in offline mode
                eventQueue.add(event.clone());
            }

            enqueueMap(event);
            queueSize++;

            //dequeueForExpiredEvent(event);
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
            //System.err.println("encounter OOME, here's some useful profiling info to disable some ops");
            //eventQueue.clear();
            //for(Map.Entry<SemanticEvent, TimeToLiveList> entry: eventMap.entrySet())
            //{
            //    System.err.println(entry.getKey().toString()+" "+entry.getValue().totalSizeForTraceGen);
            //}

        }
    }

    private void enqueueMap(SemanticEvent event) throws Throwable{
        if (!eventMap.containsKey(event.getMapKey())) {
            //fixme: add sync back
            //eventMap.put(event, Collections.synchronizedList(new ArrayList<>()));
            //eventMap.put(event, (new ArrayList<>()));
            //eventMap.put(event, (new TimeToLiveList(time_window_length_in_millis)));
            //eventMap.put(event.getMapKey(), ( new CircularBuffer(event.getClass(),100)));

            //ideally we should already pre-init for all types of events, still check missing in case
            eventMap.put(event.getMapKey(), EventListBuilder.buildEventList(event.getClass()));
            if(running_under_prod_mode)
                System.out.println("WARN: "+event.getMapKey()+" not pre-registered");
        }

        EventList lst = eventMap.get(event.getMapKey());
        //synchronized (lst)
        {
            lst.add(event);
        }
    }

//    private void dequeueForExpiredEvent(SemanticEvent lastestEvent) {
//        if(!running_under_prod_mode)
//            return;
//
//        //todo: I worry this is not efficient, there are several ways to do this window thing:
//        //1) delete when insert
//        //2) delete when check
//        //3) periodically delete in batch
//        List<SemanticEvent> subqueue = eventMap.get(lastestEvent);
//        if(subqueue.isEmpty())
//            return;
//        try
//        {
//            //synchronized (subqueue)
//            {
//                if(lastestEvent.system_timestamp - subqueue.get(0).system_timestamp > time_window_length_in_millis)
//                {
//                    subqueue.remove(0);
//                    queueSize--;
//                }
//            }
//        }
//        catch (Exception ex)
//        {
//            //fixme: remove this ignoring wrapper
//            //ignore
//        }
//
//    }

    //synchronized
    public int getQueueSize() {
        return queueSize;
    }

    public Set<SemanticEvent> getEventSet() {
        return new HashSet<>(eventQueue);
    }
    @Override
    public Iterator<SemanticEvent> iterator() {
        return new Iterator<SemanticEvent>() {

            private int currentIndex = 0;

            @Override
            public boolean hasNext() {
                return currentIndex < getQueueSize();
            }

            @Override
            public SemanticEvent next() {
                return eventQueue.get(currentIndex++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public ListIterator<SemanticEvent> iteratorAtTail() {
        return eventQueue.listIterator(eventQueue.size());
    }

    public Iterator<SemanticEvent> iterator(Context context) {
        List<EventList<SemanticEvent>> lstOfSubQueue = new ArrayList<>();
        int totalSize = 0;
        if (context.left != null) {
            EventList lst = eventMap.get(context.left.getMapKey());
            if (lst != null) {
                lstOfSubQueue.add(lst);
                totalSize += lst.size();
            }
        }
        if (context.right != null) {
            EventList lst = eventMap.get(context.right.getMapKey());
            if (lst != null) {
                lstOfSubQueue.add(lst);
                totalSize += lst.size();
            }
        }
        if (context.secondright != null) {
            EventList lst = eventMap.get(context.secondright.getMapKey());
            if (lst != null) {
                lstOfSubQueue.add(lst);
                totalSize += lst.size();
            }
        }

        List<Integer> indexLst = new ArrayList<>();
        for (int i = 0; i < lstOfSubQueue.size(); ++i)
            indexLst.add(0);

        //List<Iterator<SemanticEvent>> itLst = new ArrayList<>();
        //for (TimeToLiveList semanticEvents : lstOfSubQueue) itLst.add(semanticEvents.iterator());
        int finalTotalSize = totalSize;
        return new Iterator<SemanticEvent>() {
            @Override
            public boolean hasNext() {
                int count = 0;
                for (Integer integer : indexLst) count += integer;
                //for(TimeToLiveList lst: lstOfSubQueue)
                //    if(lst.size()>0)
                //        return true;
                return count < finalTotalSize;
                //return false;
            }

            @Override
            public SemanticEvent next() {
                SemanticEvent minEvent = null;
                long minVal = Long.MAX_VALUE - 1;
                int cursor = 0;
                for (int i = 0; i < lstOfSubQueue.size(); ++i) {
                    int index = indexLst.get(i);
                    EventList<SemanticEvent> lst = lstOfSubQueue.get(i);
                    //if(lst.size()==0)
                    //    continue;
                    if (index >= lst.size())
                        continue;

                    //synchronized (lst)
                    {
                        SemanticEvent event = lst.get(index);
                        //SemanticEvent event = lst.peek();
                        if (event.logical_timestamp < minVal) {
                            minEvent = event;
                            //minLst = lst;
                            minVal = event.logical_timestamp;
                            cursor = i;
                        }
                    }
                }

                //if (minLst != null)
                //    return minLst.poll();

                indexLst.set(cursor, indexLst.get(cursor) + 1);
                if (minEvent == null) {
                    //new RuntimeException("IMPOSSIBLE").printStackTrace();
                    //System.exit(-1);
                    return null;
                }
                return minEvent;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    //for testing use
    public void assignSerialTimestamp(boolean logical, boolean system) {
        long count = 1;
        for (SemanticEvent event : eventQueue) {
            if(logical)
                event.logical_timestamp = count;
            if(system)
                event.system_timestamp = count;
            count++;
        }
    }

    private void loadMapFromQueue() throws Throwable{
        for (SemanticEvent event : eventQueue) {
            enqueueMap(event);
        }
    }

    //make this threadlocal so we don't need to sync on this
    static ThreadLocal<OpTriggerEvent> opTriggerEventContainer = ThreadLocal.withInitial(() -> new OpTriggerEvent(""));
    //TODO: add sync between this and oathkeeper.runtime.EventTracer.serialize
    //synchronized
    public static void registerOpEvent(String opName) {
        //StaticInstrumentEngine.trackSwitchOff();
        //okHelper.logInfo("registerOpEvent "+opName);

        //OpTriggerEvent opTriggerEvent = new OpTriggerEvent(opName);
        OpTriggerEvent opTriggerEvent = opTriggerEventContainer.get();
        opTriggerEvent.opName = opName;
        opTriggerEvent.system_timestamp = System.currentTimeMillis();
        opTriggerEvent.logical_timestamp = eventCounter.incrementAndGet();
        instance.enqueue(opTriggerEvent);

        //StaticInstrumentEngine.trackSwitchOn();
        //InvariantStore.invokeInvariantIfProd(event);
    }

    static ThreadLocal<StateUpdateEvent> stateUpdateEventContainer = ThreadLocal.withInitial(() -> new StateUpdateEvent("","",0));
    //synchronized
    public static void registerStateEvent(String stateName, String sourceName, long value) {
        //StaticInstrumentEngine.trackSwitchOff();
        //okHelper.logInfo("registerStateEvent "+stateName);

        //StateUpdateEvent stateUpdateEvent = new StateUpdateEvent(stateName, sourceName, value);
        StateUpdateEvent stateUpdateEvent = stateUpdateEventContainer.get();
        stateUpdateEvent.stateName = stateName;
        stateUpdateEvent.sourceMethodName = sourceName;
        stateUpdateEvent.updatedValue = value;
        stateUpdateEvent.system_timestamp = System.currentTimeMillis();
        stateUpdateEvent.logical_timestamp = eventCounter.incrementAndGet();
        instance.enqueue(stateUpdateEvent);
        //StaticInstrumentEngine.trackSwitchOn();

        //InvariantStore.invokeInvariantIfProd(event);
    }

    //synchronized
    public static void registerMarkerEvent(int marker) {
        MarkerEvent event = new MarkerEvent(marker);
        instance.enqueue(event);
    }

    //this would only be called in infer phase, so it's fine directly operating on the eventqueue
    public void filterAfterMarkerEvents(int marker) {
        //we may falsely insert some marker events at the end of some methods, so only use the last one here
        int index = this.eventQueue.lastIndexOf(new MarkerEvent(marker));
        if (index != -1)
            this.eventQueue = this.eventQueue.subList(0, index);
    }

    public String getStatefulName() {
        return tracerName
                + (mode.equals(TracerMode.TEST_PATCHED) ? PATCHED_SUFFIX : UNPATCHED_SUFFIX);
    }

    public static synchronized EventTracer deserialize(String json) {
        return GsonUtils.gsonPrettyPrinter.fromJson(json, EventTracer.class);
    }

    public static String serialize(EventTracer tracer) {
        synchronized (tracer)
        {
            try {
                return GsonUtils.gsonPrettyPrinter.toJson(tracer);
            } catch (java.lang.OutOfMemoryError ex)
            {
                System.err.println("OOM ERROR: potential solution is, checking if in the rules we instrument too many," +
                        "we should customize to instrument the most important ones");
                throw ex;
            }
        }
    }

    public static EventTracer loadFromFile(String fileName) {
        EventTracer tracer = null;

        try {
            File traceFile = new File(fileName);
            if (!traceFile.exists())
            {
                System.err.println("FILE " + fileName + " not exists!");
                return null;
            }

            byte[] encoded = Files.readAllBytes(traceFile.toPath());
            tracer = deserialize(new String(encoded, StandardCharsets.US_ASCII));

            if(tracer==null)
            {
                System.err.println("WARN: loading fails for trace file:" + fileName);
                return null;
            }

            tracer.filterAfterMarkerEvents(MarkerEvent.Marker.EndOfTest.ordinal());
            tracer.queueSize = tracer.eventQueue.size();
            tracer.loadMapFromQueue();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        System.out.println("Successfully loaded trace file:" + fileName + " with " + tracer.getQueueSize() + " events");
        return tracer;
    }

    public static void dumpToFile(String dirPath, String fileName, EventTracer tracer) {
        File dir = new File(dirPath);
        if (!dir.exists()) dir.mkdirs();

        File traceFile = new File(dirPath + "/" + fileName);

        try {
            //cleanup old one
            Files.deleteIfExists(traceFile.toPath());

            Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(traceFile)));
            writer.write(serialize(tracer));

            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
