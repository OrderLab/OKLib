package oathkeeper.runtime;

import oathkeeper.runtime.event.OpTriggerEvent;
import oathkeeper.runtime.event.SemanticEvent;
import oathkeeper.runtime.event.StateUpdateEvent;
import oathkeeper.runtime.gson.GsonUtils;
import oathkeeper.runtime.invariant.Invariant;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static oathkeeper.runtime.CalcUtils.sortByValue;

/**
 * Management unit for invariant lists
 */
public class InvariantStore {

    public List<Invariant> invariantList = new ArrayList<>();
    //source file name if provided, optional
    public String source = null;

    public InvariantStore() {
    }

    public InvariantStore(List<Invariant> invariantList) {
        this.invariantList = invariantList;
    }

    public static InvariantStore deserialize(String json) {
        return GsonUtils.gsonPrettyPrinter.fromJson(json, InvariantStore.class);
    }

    public String serialize() {
        return GsonUtils.gsonPrettyPrinter.toJson(this);
    }

    //we dump too many invs a lot of times, we may only want to reserve those have relations in the diff files
    public void truncate(List<String> diffClazz)
    {
        List<Invariant> retainedInvs = new ArrayList<>();
        //for()
        //TODO
    }

    public void truncate(int uplimit, int retainedSize)
    {
        if(invariantList.size()<uplimit)
            return;

        int originSize = invariantList.size();
        Collections.shuffle(invariantList);
        invariantList = invariantList.subList(0, retainedSize);

        System.out.println("[WARN]: retain only " + retainedSize+" from "+originSize+" invs due to exceeding limit");
    }

    private String getClassNameFromEvent(SemanticEvent event)
    {
        if (event == null)
        {
            return null;
        }

        String separator = ".";
        String str = null;
        if(event instanceof OpTriggerEvent)
        {
            str = ((OpTriggerEvent) event).opName;
        }
        else if(event instanceof StateUpdateEvent)
        {
            str = ((StateUpdateEvent) event).stateName;
        }
        else {
            return null;
        }
        //deal with cases like org.apache.zookeeper.server.PrepRequestProcessor.pRequest2Txn(int,long,org.apache.zookeeper.server.Request,org.apache.jute
        int firstBracketPos = str.lastIndexOf("(");
        if(firstBracketPos!=-1)
            str = str.substring(0,firstBracketPos);

        int sepPos = str.lastIndexOf(separator);
        if (sepPos == -1) {
            return str;
        }
        return str.substring(0,sepPos);
    }

    public void classSummary()
    {
        Map<String, Integer> clazzMap = new HashMap<>();
        for (Invariant inv : invariantList) {
            String cName;
            cName = getClassNameFromEvent(inv.context.left);
            if (cName != null) {
                int count = clazzMap.getOrDefault(cName, 0);
                clazzMap.put(cName, count + 1);
            }
            cName = getClassNameFromEvent(inv.context.right);
            if (cName != null) {
                int count = clazzMap.getOrDefault(cName, 0);
                clazzMap.put(cName, count + 1);
            }
            cName = getClassNameFromEvent(inv.context.secondright);
            if (cName != null) {
                int count = clazzMap.getOrDefault(cName, 0);
                clazzMap.put(cName, count + 1);
            }
        }

        Map<String, Integer> sortedMap = sortByValue(clazzMap);
        int count =0 ;
        System.out.println("Summary of involved classes:");
        for(Map.Entry<String, Integer> entry:sortedMap.entrySet())
        {
            if(count>=20)
                continue;

            System.out.println(entry.getKey()+" "+entry.getValue());
            count++;
        }

    }
    public void templateSummary()
    {
        Map<String, Integer> templateMap = new HashMap<>();
        for (Invariant inv : invariantList) {
            String cName;
            cName = inv.template.getTemplateName();
            templateMap.putIfAbsent(cName,new Integer(0));
            templateMap.put(cName,templateMap.get(cName)+1);
        }

        System.out.println("Summary of template types:");
        for(Map.Entry<String, Integer> entry:templateMap.entrySet())
        {
            System.out.println(entry.getKey()+" "+entry.getValue());
        }
    }

    public void stat()
    {
        templateSummary();
        classSummary();
    }

    private boolean ifOptimize(Invariant inv)
    {
        if((double) inv.stats.passNum / (inv.stats.passNum + inv.stats.inactiveNum) < 0.2)
        {
            System.out.println("WARN: remove invariant due to inactive threshold");
            return true;
        }

        if(inv.toString().contains("shutdown"))
        {
            System.out.println("WARN: remove invariant due to contains shutdown");
            return true;
        }

        if(inv.template.getTemplateName().equals("AfterOpAtomicStateUpdateTemplate"))
        {
            System.out.println("WARN: remove invariant due to AfterOpAtomicStateUpdateTemplate ");
            return true;
        }

        if(inv.template.getTemplateName().equals("OpAddTimeInvokeOpTemplate"))
        {
            System.out.println("WARN: remove invariant due to OpAddTimeInvokeOpTemplate ");
            return true;
        }

        return false;
    }

    public void optimize()
    {
        // must be called before you can call i.remove()
        invariantList.removeIf(this::ifOptimize);
    }

    public static InvariantStore loadFromFile(String fileName) {
        InvariantStore store = null;

        try {
            File storeFile = new File(fileName);
            if(!storeFile.exists())
                throw new RuntimeException("FILE "+fileName+" not exists!");

            byte[] encoded = Files.readAllBytes(storeFile.toPath());
            store = InvariantStore.deserialize(new String(encoded, StandardCharsets.US_ASCII));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        System.out.println("Successfully loaded inv file:" + fileName + " with " + store.invariantList.size() + " invs");
        return store;
    }


}
