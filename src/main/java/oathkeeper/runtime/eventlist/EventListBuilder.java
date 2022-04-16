package oathkeeper.runtime.eventlist;

import oathkeeper.runtime.ConfigManager;
import oathkeeper.runtime.event.SemanticEvent;

import java.util.ArrayList;
import java.util.Collections;

public class EventListBuilder {

    public static EventList buildEventList(Class clazz) throws InstantiationException, IllegalAccessException
    {
        if(ConfigManager.getExecuteMode().equals(ConfigManager.ExecuteMode.PROD))
            return new CircularBuffer(clazz,100);
        else
            return new SynchronizedArrayList();
    }
}
