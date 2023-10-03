package oathkeeper.runtime;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import oathkeeper.runtime.event.MarkerEvent;
import oathkeeper.runtime.event.OpTriggerEvent;
import oathkeeper.runtime.event.StateUpdateEvent;
import oathkeeper.runtime.eventlist.EventListBuilder;
import oathkeeper.runtime.gson.GsonUtils;
import oathkeeper.runtime.eventlist.CircularBuffer;
import oathkeeper.runtime.utils.BashUtil;
import org.reflections8.Reflections;
import org.reflections8.scanners.MemberUsageScanner;
import org.reflections8.scanners.SubTypesScanner;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A helper class to instrument
 */
public class DynamicClassModifier {
    Map<String, String> stateFields = new HashMap<String, String>() {{

    }};
    Set<String> opInstClasses = new HashSet<String>() {{

    }};

    //since we only have one shot for each class, should group and do together
    Map<String, CtClass> toDumpClasses = new HashMap<>();

    private boolean ifClassMatchTargetTestClass(String cName)
    {
        String testName = System.getProperty("ok.testname");
        return testName != null && !testName.equals("") && cName.contains(testName);
    }

    private List<String> getClassesFromDiffFiles(String diffFiles)
    {
        List<String> lst = new ArrayList<>();

        String[] lines = diffFiles.split("\\r?\\n");

        for(String line: lines)
        {
            int index = line.lastIndexOf(".java");
            if(index==-1)
                continue;
            String result = line.substring(0,index)
                    .replace('/','.');
            int index2 = result.indexOf(ConfigManager.config.getString(ConfigManager.SYSTEM_PACKAGE_PREFIX_KEY));
            if(index2==-1)
                continue;
            String result2 = result.substring(index2);

            //we filter testing classes here
            if(result2.contains(".test."))
                continue;

            lst.add(result2);
        }
        return lst;
    }

    private String getPackageNameFromClassName(String className) {
        int iend = className.lastIndexOf(".");
        if (iend != -1) {
            String packageName = className.substring(0, iend);
            return packageName;
        }

        return null;
    }

    private void initFromDiffFileFromCommit(boolean appendUsage)
    {
        String diffFiles = System.getProperty("ok.filediff");
        System.out.println("diffFiles: "+diffFiles);
        if(diffFiles == null || diffFiles.equals(""))
        {
            return;
        }

        for(String clazz: getClassesFromDiffFiles(diffFiles))
        {
            String prefix = getPackageNameFromClassName(clazz);
            if(prefix==null)
                continue;

            Reflections reflections = new Reflections(prefix, new SubTypesScanner(false));

            Set<String> allClasses;
            try {
                allClasses = reflections.getAllTypes();
            } catch (org.reflections8.ReflectionsException e) {
                System.out.println("Reflection cannot find class: " + clazz);
                e.printStackTrace();
                continue;
            }
            System.out.print("Reflection successfully get classes: ");
            System.out.println(allClasses);

            for(String clazz2:allClasses)
            {
                opInstClasses.add(clazz2);
                System.out.println("Append to-instrument classes "+clazz2+" from git diff");
            }

            //used in relaxed mode, we add usage as well
            if(appendUsage)
            {
                for(String clazz2: scanImportedClassesFromDiffFile(clazz))
                {
                    String clazz2_processed = clazz2.replace("import ","").replace(";","");
                    opInstClasses.add(clazz2_processed);
                    System.out.println("Append to-instrument classes "+clazz2_processed+" from usage analysis");
                }
            }
        }
    }

    private Set<String> scanImportedClassesFromDiffFile(String clazz)
    {
        return BashUtil.executeBashCommand("find "+ConfigManager.config.getString(ConfigManager.SYSTEM_DIR_PATH_KEY)
                +" -name \""+clazz.substring(clazz.lastIndexOf('.') + 1)+".java\" -exec cat {} + | sed -n '/import "
                +ConfigManager.config.getString(ConfigManager.SYSTEM_PACKAGE_PREFIX_KEY)+"/p'");
    }

    private void initFromConfigFile()
    {
        for(String str: ConfigManager.config.getStringArray(ConfigManager.INSTRUMENT_STATE_FIELDS_KEY))
        {
            if(str.equals(""))
                continue;

            String fieldName = str.split("\\^")[0];
            String valMethodName = str.split("\\^")[1];
            stateFields.put(fieldName, valMethodName);
        }
        opInstClasses.addAll(Arrays.asList(ConfigManager.config.getStringArray(ConfigManager.INSTRUMENT_CLASS_ALLMETHODS_KEY)));
    }

    private void initFromAllClasses()
    {
        String prefix = ConfigManager.config.getString(ConfigManager.SYSTEM_PACKAGE_PREFIX_KEY);
        Reflections reflections = new Reflections(prefix, new SubTypesScanner(false));
        Set<String> allClasses  = reflections.getAllTypes();
        opInstClasses.addAll(allClasses);
    }

    class OpInstClassesWrapper
    {
        Set<String> opInstClasses = new HashSet<String>() {{}};
        OpInstClassesWrapper(Set<String> opInstClasses)
        {
            this.opInstClasses = opInstClasses;
        }
    }

    private void initFromDumpedFiles()
    {
        File pointFileDir = new File(FileLayoutManager.getPathForPreloadInstrumentInputDir() + "/" + FileLayoutManager.INSTRUMENT_POINTS_FILE_NAME);
        if (!pointFileDir.exists())
        {
            System.err.println("FILE " + pointFileDir.getAbsolutePath() + " not exists!");
            System.exit(-1);
        }

        for (final File pointFile : pointFileDir.listFiles()) {
            if (!pointFile.isDirectory()) {
                try{
                    byte[] encoded = Files.readAllBytes(pointFile.toPath());
                    OpInstClassesWrapper opInstClassesWrapper = GsonUtils.gsonPrettyPrinter.fromJson(new String(encoded, StandardCharsets.US_ASCII), OpInstClassesWrapper.class);
                    opInstClasses.addAll(opInstClassesWrapper.opInstClasses);
                }catch (Exception ex)
                {
                    ex.printStackTrace();
                    System.exit(-1);
                }
            }
        }
    }

    private void dumpInstrumentPoints()
    {
        try {
            File pointFile = new File(FileLayoutManager.getPathForInstrumentPointFile());
            //cleanup old one
            Files.deleteIfExists(pointFile.toPath());

            Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(pointFile)));
            writer.write(GsonUtils.gsonPrettyPrinter.toJson(new OpInstClassesWrapper(opInstClasses)));
            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void excludeSpecifiedClasses()
    {
        String[] exclude_class_list = ConfigManager.config.getStringArray(ConfigManager.EXCLUDE_CLASS_LIST_KEY);
        for(String clazz:exclude_class_list)
        {
            List<String> result = new ArrayList<>();
            for(String s : opInstClasses)
                if(s.contains(clazz))
                    result.add(s);

            for(String toremove: result)
            {
                if (opInstClasses.remove(toremove))
                    System.out.println("Remove to-instrument classes "+toremove+" due to excluding list");
            }
        }
    }

    private void appendTrackedStates() {
        int total = 0;
        for (String clazz : opInstClasses) {
            try {
                ClassPool pool = ClassPool.getDefault();
                CtClass cc = pool.get(clazz);
                for (CtField field : cc.getDeclaredFields()) {
                    //TODO: fix this
                    //if(Collection.class.isAssignableFrom(field.getClass())) {
                    if (field.getType().getName().equals("java.util.Map") ||
                            field.getType().getName().equals("java.util.HashMap") ||
                            field.getType().getName().equals("java.util.List")) {
                        String fullName = clazz + "." + field.getName();
                        stateFields.put(fullName, ".size()");
                    }

//                    if (field.getType().getName().equals("java.lang.Long") ||
//                            field.getType().getName().equals("java.lang.Byte") ||
//                            field.getType().getName().equals("java.lang.Integer") ||
//                            field.getType().getName().equals("java.lang.Boolean") ||
//                            field.getType().getName().equals("java.lang.Short") ||
//                            field.getType().getName().equals("long") ||
//                            field.getType().getName().equals("byte") ||
//                            field.getType().getName().equals("int") ||
//                            field.getType().getName().equals("boolean") ||
//                            field.getType().getName().equals("short")) {
//                        String fullName = clazz + "." + field.getName();
//                        stateFields.put(fullName, "");
//                    }
                }
                total++;
                cc.defrost();
            } catch (Exception e) {
                System.out.println("appendTrackedStates failed on class: " + clazz);
                e.printStackTrace();
            }
        }
        System.out.println("Instrument "+total+" fields");
        for(String fieldName: stateFields.keySet())
        {
            System.out.println("Instrument field "+fieldName);
        }
    }


    public DynamicClassModifier()
    {
        if(ConfigManager.config.getBoolean(ConfigManager.FORCE_INSTRUMENT_NOTHING_KEY))
            return;

        //in two cases we init from all classes 1)gen mode with full mode 2)in verify mode, then we filter
        if(ConfigManager.getGentraceInstrumentMode().equals(ConfigManager.InstrumentMode.FULL) || System.getProperty("ok.invmode").equals("verify")
        ||System.getProperty("ok.invmode").equals("prod"))
        {
            initFromAllClasses();
            excludeSpecifiedClasses();
            appendTrackedStates();
            //dumpInstrumentPoints();
        }
        else if(ConfigManager.getGentraceInstrumentMode().equals(ConfigManager.InstrumentMode.STRICT_SELECTIVE)) {
            initFromConfigFile();
            initFromDiffFileFromCommit(false);
            excludeSpecifiedClasses();
            appendTrackedStates();
            //dumpInstrumentPoints();
        }
        else if(ConfigManager.getGentraceInstrumentMode().equals(ConfigManager.InstrumentMode.RELAXED_SELECTIVE)) {
            initFromConfigFile();
            initFromDiffFileFromCommit(true);
            excludeSpecifiedClasses();
            appendTrackedStates();
            //dumpInstrumentPoints();
        }
        else if(ConfigManager.getGentraceInstrumentMode().equals(ConfigManager.InstrumentMode.SPECIFIED_SELECTIVE)) {
            initFromDumpedFiles();
            excludeSpecifiedClasses();
            appendTrackedStates();
        }
        else {
            System.err.println("Unexpected path, abort");
            System.exit(-1);
        }
    }

    static class StateAccessPoint {
        String className;
        String methodName;
        String fieldName;
        int lineNum;

        public StateAccessPoint(String className, String methodName, String fieldName, int lineNum) {
            this.className = className;
            this.methodName = methodName;
            this.fieldName = fieldName;
            this.lineNum = lineNum;
        }
    }

    //from descriptor to hookpoint, this is modeled after reflection package internal logic
    private static StateAccessPoint parse(String descriptor, String fieldName) {
        int p0 = descriptor.lastIndexOf('(');
        String memberKey = p0 != -1 ? descriptor.substring(0, p0) : descriptor;
        String methodParameters = p0 != -1 ? descriptor.substring(p0 + 1, descriptor.lastIndexOf(')')) : "";

        int p1 = memberKey.lastIndexOf('.');
        String className = memberKey.substring(memberKey.lastIndexOf(' ') + 1, p1);
        String memberName = memberKey.substring(p1 + 1);

        int p2 = descriptor.lastIndexOf('#');
        String lineNumStr = descriptor.substring(p2 + 1);

        return new StateAccessPoint(className, memberName, fieldName, Integer.parseInt(lineNumStr));
    }

    private static String getStateShortName(String longName) {
        int p1 = longName.lastIndexOf('.');
        return longName.substring(p1 + 1);
    }

    private Map<String, List<StateAccessPoint>> scan() {
        List<StateAccessPoint> stateAccessPoints = new ArrayList<>();

        String prefix = ConfigManager.config.getString(ConfigManager.SYSTEM_PACKAGE_PREFIX_KEY);
        Reflections reflections = new Reflections(prefix,
                new MemberUsageScanner());
        try {
            for (String fieldKey : stateFields.keySet()) {
                for (String str : reflections.getStore().get(MemberUsageScanner.class.getSimpleName(), fieldKey)) {
                    System.out.println(str);
                    stateAccessPoints.add(parse(str, fieldKey));
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            //exceptions here will be critical
            System.err.println("[ERROR] Abort!");
            System.exit(-1);
        }

        //we need to group in classes so it can be modified one by one
        Map<String, List<StateAccessPoint>> map = stateAccessPoints.stream().collect(Collectors.groupingBy(w -> w.className));
        return map;
    }

    public void modifyOperationEntry(Set<String> allowedSet) {

        ClassPool pool = ClassPool.getDefault();

        int succMethodCounter = 0;
        int failClassCounter = 0;
        System.out.println("opInstClasses.size() "+opInstClasses.size());

        List<String> disabledList = Arrays.asList(ConfigManager.config.getStringArray(ConfigManager.EXCLUDE_METHOD_LIST_KEY));
        System.out.println("disabledList"+disabledList);

        OpTriggerEvent event = new OpTriggerEvent();

        for (String cName : opInstClasses) {
            //skip subclass
            //if (cName.contains("$"))
            //    continue;

            if(ifClassMatchTargetTestClass(cName))
            {
                //we don't want to inject in test class and result a lot of traces we are not interested!
                continue;
            }

            int localCounter = 0;
            try {
                //only get the part before @
                //e.g. org.apache.hadoop.hbase.regionserver.MemStore@heapSizeChange -> org.apache.hadoop.hbase.regionserver.MemStore
                String cNameNoMethod = cName.split("\\@")[0];
                String methodName = cName.split("\\@").length>1?cName.split("\\@")[1]:null;
                CtClass cc = pool.get(cNameNoMethod);
                for (CtMethod m : cc.getDeclaredMethods()) {
                    if (allowedSet != null && !allowedSet.contains(m.getLongName()))
                        continue;

                    if(methodName!=null && !m.getName().equals(methodName))
                        continue;

                    if(disabledList.contains(cNameNoMethod+"@"+methodName))
                    {
                        System.out.println("Skip unwanted method:"+ cNameNoMethod+"@"+methodName);
                        continue;
                    }

                    m.insertBefore(
                            "{"+EventTracer.class.getName()+".registerOpEvent(\"" + m.getLongName() + "\");}");
                            // "{"+EventTracer.class.getName()+".registerOpEvent(\"" + m.getName() + "\");}");
                    localCounter++;

                    //pre-init for event map
                    event.opName = m.getLongName();
                    if (!EventTracer.instance.eventMap.containsKey(event.getMapKey())) {
                        //fixme: add sync back
                        //eventMap.put(event, Collections.synchronizedList(new ArrayList<>()));
                        //eventMap.put(event, (new ArrayList<>()));
                        //eventMap.put(event, (new TimeToLiveList(time_window_length_in_millis)));
                        EventTracer.instance.eventMap.put(event.getMapKey(), EventListBuilder.buildEventList(event.getClass()));
                    }
                }

                //lazy dump
                //cc.toClass();
                toDumpClasses.put(cc.getName(),cc);
            } catch (Exception ex) {
                ex.printStackTrace();
                failClassCounter++;

                continue;
            }
            succMethodCounter += localCounter;
            // System.out.println("prepare for " + cName);
        }

        System.out.println("succMethodCounter" + succMethodCounter + " failClassCounter" + failClassCounter);
    }

    public void modifyStateAccess(Set<String> allowedSet) {
        if(ConfigManager.config.getBoolean(ConfigManager.FORCE_TRACK_NO_STATES_KEY))
            return;

        ClassPool pool = ClassPool.getDefault();

        int succMethodCounter = 0;
        int failClassCounter = 0;
        Map<String, List<StateAccessPoint>> map = scan();

        List<String> disabledList = Arrays.asList(ConfigManager.config.getStringArray(ConfigManager.EXCLUDE_METHOD_LIST_KEY));
        System.out.println("disabledList"+disabledList);

        StateUpdateEvent event = new StateUpdateEvent();

        for (String cName : map.keySet()) {
            int localCounter = 0;
            try {
                if(ifClassMatchTargetTestClass(cName))
                    //we don't want to inject in test class and result a lot of traces we are not interested!
                    continue;

                CtClass cc = pool.get(cName);
                cc.defrost();


                String lastMethodName = "";
                int lastLineNum = -1;

                //important, we found that if a method contains several inject points, it's very likely to cause problems like
                // 1) testCreateAfterCloseShouldFail(org.apache.zookeeper.test.SessionInvalidationTest)
                // java.lang.VerifyError: (class: org/apache/zookeeper/common/PathTrie$TrieNode, method: getChild signature: (Ljava/lang/String;)Lorg/apache/zookeeper/common/PathTrie$TrieNode;) Stack size too large
                //         at org.apache.zookeeper.common.PathTrie.<init>(PathTrie.java:189)
                //         at org.apache.zookeeper.server.DataTree.<init>(DataTree.java:112)
                //         at org.apache.zookeeper.server.ZKDatabase.<init>(ZKDatabase.java:84)
                //         at org.apache.zookeeper.server.ZooKeeperServer.<init>(ZooKeeperServer.java:158)
                //         at org.apache.zookeeper.server.ZooKeeperServer.<init>(ZooKeeperServer.java:192)
                //         at org.apache.zookeeper.test.ClientBase.createNewServerInstance(ClientBase.java:343)
                //         at org.apache.zookeeper.test.ClientBase.startServer(ClientBase.java:429)
                //         at org.apache.zookeeper.test.ClientBase.setUp(ClientBase.java:422)
                //         at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
                //         at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)

                //thus we would discard injecting for multiple hook points in one method
                //(even just allow single one would still be problematic)
                List<StateAccessPoint> points = map.get(cName);
                Map<String, List<StateAccessPoint>> pByMethodName = new HashMap<>();
                for(StateAccessPoint p: points)
                {
                    pByMethodName.putIfAbsent(p.methodName,new ArrayList<>());
                    pByMethodName.get(p.methodName).add(p);
                }
                pByMethodName.entrySet().removeIf(entry -> entry.getValue().size() > 1);

                for (StateAccessPoint stateAccessPoint : pByMethodName.values().stream()
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList())) {
                    if (stateAccessPoint.methodName.contains("<init>"))
                        continue;
                    if (stateAccessPoint.methodName.contains("$"))
                        continue;

                    if (allowedSet != null && !allowedSet.contains(stateAccessPoint.fieldName))
                        continue;

                    String fullName = stateAccessPoint.className+"@"+stateAccessPoint.methodName;
                    if(disabledList.contains(fullName))
                    {
                        System.out.println("Skip unwanted method:"+ fullName);
                        continue;
                    }

                    CtMethod m = cc.getDeclaredMethod(stateAccessPoint.methodName);
                    System.out.println("instrument now for " + m.getLongName() + " at " + (stateAccessPoint.lineNum + 1));
                    try {
                        //should insert AFTER the op
                        int attmeptedLoc = stateAccessPoint.lineNum + 1;
                        String stmt = "{"+EventTracer.class.getName()+".registerStateEvent(\"" + stateAccessPoint.fieldName + "\",\""
                                + stateAccessPoint.methodName + "\", (long) "
                                + getStateShortName(stateAccessPoint.fieldName)
                                + stateFields.get(stateAccessPoint.fieldName) + ");}";
                        //why takes two phase? because some stmts report stack error
                        int realLoc = m.insertAt(attmeptedLoc, false, stmt);
                        //it seems only when moving to different location seems to be safe
                        //and another pattern is like:
                        //instrument now for org.apache.hadoop.hdfs.server.namenode.DirectoryWithQuotaFeature.namespaceString() at 241
                        //instrument at 241
                        //instrument now for org.apache.hadoop.hdfs.server.namenode.DirectoryWithQuotaFeature.namespaceString() at 242
                        //instrument at 241
                        //javassist.CannotCompileException: by javassist.bytecode.BadBytecode: namespaceString ()Ljava/lang/String; in org.apache.hadoop.hdfs.server.namenode.DirectoryWithQuotaFeature: inconsistent stack height 5
                        //	at javassist.CtBehavior.insertAt(CtBehavior.java:1309)
                        //	at ok.runtime.DyInster.modifyStateAccess(DyInster.java:181)
                        //	at ok.runtime.DyInster.modifyAll(DyInster.java:220)
                        //	at org.apache.hadoop.hdfs.ok.TestEngine.main(TestEngine.java:114)
                        //Caused by: javassist.bytecode.BadBytecode: namespaceString ()Ljava/lang/String; in org.apache.hadoop.hdfs.server.namenode.DirectoryWithQuotaFeature: inconsistent stack height 5
                        //	at javassist.bytecode.stackmap.MapMaker.make(MapMaker.java:119)
                        //	at javassist.bytecode.MethodInfo.rebuildStackMap(MethodInfo.java:458)
                        //	at javassist.bytecode.MethodInfo.rebuildStackMapIf6(MethodInfo.java:440)
                        //	at javassist.CtBehavior.insertAt(CtBehavior.java:1299)
                        //	... 3 more

                        //case2
                        if (!((lastMethodName.equals(m.getLongName()) && lastLineNum == realLoc)))
                            //case1
                            //if (realLoc != attmeptedLoc) {
                            //first try but not really insert
                                m.insertAt(attmeptedLoc, false, stmt);
                        //TODO: revert after failing here
                        m.insertAt(attmeptedLoc, true, stmt);
                        System.out.println("instrument at " + realLoc);
                            //}

                        lastMethodName = m.getLongName();
                        lastLineNum = realLoc;

                        //pre-init for event map
                        event.stateName = stateAccessPoint.fieldName;
                        event.sourceMethodName = stateAccessPoint.methodName;
                        if (!EventTracer.instance.eventMap.containsKey(event.getMapKey())) {
                            EventTracer.instance.eventMap.put(event.getMapKey(), EventListBuilder.buildEventList(event.getClass()));
                        }


                    } catch (Exception ex) {
                        ex.printStackTrace();
                        continue;
                    }
                    localCounter++;
                }

                //drafts
                //cc.toBytecode().;
                //Bytecode bc = new Bytecode();
                //bc.toCodeAttribute().computeMaxStack();

                //lazy dump
                //cc.toClass();
                toDumpClasses.put(cc.getName(),cc);
            } catch (Exception ex) {
                ex.printStackTrace();
                failClassCounter++;

                continue;
            }
            succMethodCounter += localCounter;
            // System.out.println("prepare for " + cName);
        }

        System.out.println("succMethodCounter" + succMethodCounter + " failClassCounter" + failClassCounter);
    }

    public void writeToClasses() {
        for (CtClass ctClass : toDumpClasses.values()) {
            try {
                ctClass.toClass();
            } catch (javassist.CannotCompileException ex) {
                // ex.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("Instrument classes finished");
    }

    //we want to cut at the end of test methods so our invs wouldn't include some boring events like "shutdown"
    public void markEndOfTestMethods()
    {
        ClassPool pool = ClassPool.getDefault();
        String testName = System.getProperty("ok.testname");
        int errCounters = 0;
        {
            try {
                CtClass cc = pool.get(testName);
                cc.defrost();

                for(CtMethod m: cc.getMethods()){
                    if (!m.hasAnnotation("org.junit.Test")) {
                        continue;
                    }
                    System.out.println("insert marker at "+m.getLongName());

                    try {
                        String stmt = "{"+EventTracer.class.getName()+".registerMarkerEvent("+ MarkerEvent.Marker.EndOfTest.ordinal() +");}";
                        m.insertAfter(stmt);
                    } catch (Exception ex) {
                        //suppress known errors
                        if(!ex.getMessage().contains("no method body"))
                        {
                            ex.printStackTrace();
                        }
                        {
                            errCounters++;
                        }
                    }
                }

                //lazy dump
                //cc.toClass();
                toDumpClasses.put(cc.getName(),cc);
            } catch (Throwable ex) {
                //we somehow encounter Exception in thread "main" java.lang.NoSuchMethodError: javassist.CtMethod.hasAnnotation(Ljava/lang/String;)Z
                ex.printStackTrace();
                if(ex instanceof NoSuchMethodError)
                {
                    System.err.println("If the error is Exception in thread \"main\" java.lang.NoSuchMethodError: javassist.CtMethod.hasAnnotation(Ljava/lang/String;)Z");
                    System.err.println("potential hints: reorder the class loading to bring forward oathkeeper javassist, this might be a conflict between oathkeeper and target system dependency");
                }
            }
            // System.out.println("prepare for test " + testName);
        }

        System.out.println("suppress " + errCounters+" no method body errors in markEndOfTestMethods()");

    }

    public void modifyAll() {
        modifyOperationEntry(null);
        modifyStateAccess(null);
        writeToClasses();
    }

    public void modifySelectively(Set<String> opSet, Set<String> stateSet) {
        long startTime = System.currentTimeMillis();

        modifyOperationEntry(opSet);
        modifyStateAccess(stateSet);
        writeToClasses();

        long endTime = System.currentTimeMillis();
        System.out.println("modifySelectively took" + (endTime - startTime) + " milliseconds");
    }
}
