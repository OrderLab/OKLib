package oathkeeper.engine;

import oathkeeper.runtime.*;
import oathkeeper.runtime.event.MarkerEvent;
import oathkeeper.runtime.event.OpTriggerEvent;
import oathkeeper.runtime.event.SemanticEvent;
import oathkeeper.runtime.event.StateUpdateEvent;
import oathkeeper.runtime.invariant.Invariant;
import oathkeeper.runtime.template.Template;
import oathkeeper.runtime.template_v1.TemplateV1;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static oathkeeper.runtime.CalcUtils.sortByValue;
import static oathkeeper.runtime.invariant.Invariant.ifStateNotFail;

/**
 * Main class for infer phase
 */
public class InferEngine {

    //warning: these options would decrease performance, use for testing only!
    public static boolean ENABLE_CHECK_REDUNDANT_INVARIANTS = false;
    public static boolean ENABLE_CHECK_INFERRED_INVS_PASS_PATCHED_TRACES = false;

    static class InferWorkload {
        EventTracer patchedTracer;
        EventTracer unpatchedTracer;

        //to keep inferred invariants
        InvariantStore invariantStore;

        public List<Invariant> infer() {

            List<Invariant> genInvs = new ArrayList<>();
            for (Template template : TemplateManager.templatePool) {
                List<Invariant> patchedInvs = template.infer(patchedTracer);

                System.out.println("patchedInvs size:" + patchedInvs.size());

                int count = 0;
                //we should highlight rules that hold in patched version but not hold in unpatched version
                for (Invariant inv : patchedInvs) {
                    if (!ifStateNotFail(inv.verify(unpatchedTracer))) {
                        count++;
                        //if inferred rule does not pass unpatched traces, add it to output
                        genInvs.add(inv);
                    }
                }

                //careful! this statement is related to the tooltest script format check!
                System.out.println("inferred " + count + " " + template.getTemplateName());
            }

            if(ENABLE_CHECK_REDUNDANT_INVARIANTS)
            {
                Set<Invariant> set = new HashSet<>(genInvs);
                if(set.size() < genInvs.size()){
                    System.err.println("[ERROR] redundant detected!");
                    System.err.println("set size: "+set.size() + ",list size:" + genInvs.size());
                }
            }

            if(ENABLE_CHECK_INFERRED_INVS_PASS_PATCHED_TRACES) {

                int failedCount = 0;
                int inactiveCount = 0;
                for (Invariant inv : genInvs) {
                    if (inv.verify(patchedTracer).equals(Invariant.InvState.FAIL)) {
                        failedCount++;
                    }
                    if (inv.verify(patchedTracer).equals(Invariant.InvState.INACTIVE)) {
                        inactiveCount++;
                    }
                }
                if (failedCount + inactiveCount > 0) {
                    //don't change the format used in test
                    System.err.println("[ERROR] critical errors detected in algorithm! "
                            + "In " + genInvs.size() + " inferred invariants, " + failedCount + " fail on patched version " +
                            "and " + inactiveCount + " are inactive!");
                }
            }

            return genInvs;
        }

        public List<Invariant> infer_v1() {

            List<Invariant> genInvs = new ArrayList<>();
            Set<SemanticEvent> candidateEvents = new HashSet<>(patchedTracer.eventQueue);
            //previously we only leave diff events to form potentially interesting context, but this may not hold for some semantics
            //candidateEvents.removeAll(unpatchedTracer.eventQueue);
            System.out.println("candidateEvents size:" + candidateEvents.size());

            for (TemplateV1 template : TemplateManager.templatePool_v1) {
                List<Invariant> patchedInvs = template.infer(patchedTracer.eventQueue, candidateEvents);

                System.out.println("patchedInvs size:" + patchedInvs.size());

                int count = 0;
                //we should highlight rules that hold in patched version but not hold in unpatched version
                for (Invariant inv : patchedInvs) {
                    if (!ifStateNotFail(inv.verify_v1(unpatchedTracer.eventQueue))) {
                        count++;
                        //if inferred rule does not pass unpatched traces, add it to output
                        genInvs.add(inv);
                    }
                }

                //careful! this statement is related to the tooltest script format check!
                System.out.println("inferred " + count + " " + template.getTemplateName());
            }

            if(ENABLE_CHECK_REDUNDANT_INVARIANTS)
            {
                Set<Invariant> set = new HashSet<>(genInvs);
                if(set.size() < genInvs.size()){
                    //don't change the format used in test
                    System.err.println("[ERROR] redundant detected!");
                    System.err.println("set size: "+set.size() + ",list size:" + genInvs.size());
                }
            }

            if(ENABLE_CHECK_INFERRED_INVS_PASS_PATCHED_TRACES)
            {
                int failedCount = 0;
                int inactiveCount = 0;
                for (Invariant inv : genInvs) {
                    if (inv.verify_v1(patchedTracer.eventQueue).equals(Invariant.InvState.FAIL)) {
                        failedCount++;
                    }
                    if (inv.verify_v1(patchedTracer.eventQueue).equals(Invariant.InvState.INACTIVE)) {
                        inactiveCount++;
                    }
                }
                if(failedCount+inactiveCount>0)
                {
                    //don't change the format used in test
                    System.err.println("[ERROR] critical errors detected in algorithm! "
                            +"In "+genInvs.size()+" inferred invariants, "+failedCount+" fail on patched version" +
                            "and "+inactiveCount+" are inactive!");
                }
            }

            return genInvs;
        }





        public void dumpInvs() {
            try {
                File dir = new File(FileLayoutManager.getPathForInvOutputDir());
                if (!dir.exists()) dir.mkdirs();

                File logFile = new File(FileLayoutManager.getPathForInvOutputDir() + "/" + patchedTracer.tracerName);
                //cleanup old one
                Files.deleteIfExists(logFile.toPath());

                Writer writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(logFile)));
                writer.write(invariantStore.serialize());

                writer.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            System.out.println("Generate " + invariantStore.invariantList.size() + " invs");
        }
    }

    public static List<Invariant> processTrace(String prefix, boolean ifDump) {
        System.out.println("infer for " + prefix + " now");

        InferWorkload inferWorkload = new InferWorkload();
        inferWorkload.patchedTracer = EventTracer.loadFromFile(prefix + EventTracer.PATCHED_SUFFIX);
        inferWorkload.unpatchedTracer = EventTracer.loadFromFile(prefix + EventTracer.UNPATCHED_SUFFIX);

        if(inferWorkload.patchedTracer==null || inferWorkload.unpatchedTracer==null)
        {
            System.err.println("infer abort for " + prefix);
            return new ArrayList<>();
        }

        long startTime = System.currentTimeMillis();
        List<Invariant> invs = inferWorkload.infer();
        inferWorkload.invariantStore = new InvariantStore(invs);
        inferWorkload.invariantStore.classSummary();
        long usedTime = System.currentTimeMillis() - startTime;
        System.out.println("Infer invs used " + usedTime / 1000 + " secs.");

        //TODO: use more smart way to truncate list when exceeding
        //inferWorkload.invariantStore.truncate(100000,inferWorkload.invariantStore.invariantList.size()/10);

        if(ifDump)
            inferWorkload.dumpInvs();
        return invs;
    }

    public static List<Invariant> processTrace_v1(String prefix, boolean ifDump) {
        System.out.println("infer for " + prefix + " now");

        InferWorkload inferWorkload = new InferWorkload();
        inferWorkload.patchedTracer = EventTracer.loadFromFile(prefix + EventTracer.PATCHED_SUFFIX);
        inferWorkload.unpatchedTracer = EventTracer.loadFromFile(prefix + EventTracer.UNPATCHED_SUFFIX);

        if(inferWorkload.patchedTracer==null || inferWorkload.unpatchedTracer==null)
        {
            System.err.println("infer abort for " + prefix);
            return new ArrayList<>();
        }

        long startTime = System.currentTimeMillis();
        List<Invariant> invs = inferWorkload.infer_v1();
        inferWorkload.invariantStore = new InvariantStore(invs);
        inferWorkload.invariantStore.classSummary();
        long usedTime = System.currentTimeMillis() - startTime;
        System.out.println("Infer invs used " + usedTime / 1000 + " secs.");

        if(ifDump)
            inferWorkload.dumpInvs();
        return invs;
    }

    private static Set<String> getAllFilesWithPrefix(String dir, String prefix) {
        File folder = new File(dir);
        File[] listOfFiles = folder.listFiles();

        Set<String> legalFileNames = new HashSet<>();
        for (int i = 0; i < Objects.requireNonNull(listOfFiles).length; i++) {
            if (listOfFiles[i].isFile()) {
                String name = listOfFiles[i].getName();
                if (name.contains(prefix)) {
                    if (name.endsWith(EventTracer.PATCHED_SUFFIX))
                        legalFileNames.add(name.replace(EventTracer.PATCHED_SUFFIX, ""));
                    else if (name.endsWith(EventTracer.UNPATCHED_SUFFIX))
                        legalFileNames.add(name.replace(EventTracer.UNPATCHED_SUFFIX, ""));
                }
            }
        }

        //todo: check if patched and unpatched version both exist

        return legalFileNames;
    }

    public static void main(String[] args) {
        ConfigManager configManager = new ConfigManager();
        configManager.initConfig();

        if (args.length < 1) {
            System.err.println("args not enough!");
            System.exit(-1);
        }

        Set<String> set = getAllFilesWithPrefix(FileLayoutManager.getPathForTracesOutputDir(), args[0]);
        if(set.size()==0)
        {
            System.err.println("Did not find any trace files for given prefix "+ args[0]);
            System.err.println("Did you forget to generate trace first?");
            return;
        }

        FileLayoutManager.cleanDir(FileLayoutManager.getPathForInvOutputDir());

        int totalCount = 0;
        for (String prefix : set)
        {
            String version = System.getProperty("ok.template_version");
            if(version !=null && version.equals("v1"))
            {
                System.out.println("[WARN] use v1 version of templates now");
                totalCount += processTrace_v1(FileLayoutManager.getPathForTracesOutputDir() + "/" + prefix, true).size();
            }
            else
                totalCount += processTrace(FileLayoutManager.getPathForTracesOutputDir() + "/" + prefix, true).size();
        }
        System.out.println("Total inferred "+totalCount+" invariants from "+set.size()+" tests");
    }
}
