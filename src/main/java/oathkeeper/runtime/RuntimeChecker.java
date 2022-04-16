package oathkeeper.runtime;

import oathkeeper.runtime.event.OpTriggerEvent;
import oathkeeper.runtime.event.SemanticEvent;
import oathkeeper.runtime.event.StateUpdateEvent;
import oathkeeper.runtime.invariant.Invariant;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static oathkeeper.runtime.invariant.Invariant.ifStateNotFail;

public class RuntimeChecker implements Runnable {

    public InvariantStore store = new InvariantStore();

    private List<Integer> survivorInvs = new ArrayList<>();
    //sometimes we want to suppress certain known invs that are alerting even in normal workloads
    private List<Integer> suppressedInvs = new ArrayList<>();

    int roundCounter = 0;

    //cache some configs
    private boolean force_disable_prod_checking = false;
    private boolean verify_survivor_mode = false;

    public RuntimeChecker()
    {
        force_disable_prod_checking = ConfigManager.config.getBoolean(ConfigManager.FORCE_DISABLE_PROD_CHECKING_KEY);
        verify_survivor_mode = ConfigManager.config.getBoolean(ConfigManager.VERIFY_SURVIVOR_MODE_KEY);
    }

    public void listAllSubfiles(String directoryName, List<File> files) {
        File directory = new File(directoryName);

        // Get all files from a directory.
        File[] fList = directory.listFiles();
        if (fList != null)
            for (File file : fList) {
                if (file.isFile()) {
                    files.add(file);
                } else if (file.isDirectory()) {
                    listAllSubfiles(file.getAbsolutePath(), files);
                }
            }
    }

    public void loadSuppressedInvs()
    {
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(FileLayoutManager.getPathForProdSuppressableInvInputFile()));
            while(scanner.hasNextInt()){
                suppressedInvs.add(scanner.nextInt());
            }
            System.out.println("Loaded "+suppressedInvs.size()+" suppressable invs from file: "+FileLayoutManager.getPathForProdSuppressableInvInputFile());
        } catch (FileNotFoundException e) {
            System.out.println("No suppress file found.");
        }
    }

    public void dumpSuppressedInvs(List<Integer> invs)
    {
        if(invs.isEmpty())
        {
            //our intuition is to avoid namenode and datanode all write to same file
            System.out.println("Skip dumpSuppressedInv due to empty list.");
            return;
        }

        FileWriter writer = null;
        try {
            File file = new File(FileLayoutManager.getPathForProdSuppressableInvOutputFile());
            boolean result = Files.deleteIfExists(file.toPath());

            writer = new FileWriter(FileLayoutManager.getPathForProdSuppressableInvOutputFile());
            for (Integer inv:invs)
            {
                writer.write(inv + " ");
            }
            writer.close();
            System.out.println("Dumped "+invs.size()+" invs to suppress later.");
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * this method can be called from verifying phase or production, we handle both scenarios
     * if from verifying, only load needed invariant files
     * if from production, by default load all
     */
    public boolean loadInvs(String dirPath, String nameFilter, String sourceName) {
        long startTime = System.currentTimeMillis();

        List<File> listOfFiles = new ArrayList<>();
        //traverse all sub directories
        listAllSubfiles(dirPath,listOfFiles);

        if (listOfFiles.isEmpty()) {
            System.out.println(System.getProperty("user.dir"));
            System.out.println("WARN: cannot find list of invariants!");
            return false;
        }

        for (File listOfFile : listOfFiles) {
            if (listOfFile.isFile()) {
                if(nameFilter != null && !listOfFile.getName().contains(nameFilter))
                {
                    continue;
                }

                store.invariantList.addAll(InvariantStore.loadFromFile(listOfFile.getAbsolutePath()).invariantList);
            }
        }
        if(sourceName !=null)
            store.source = sourceName;

        System.out.println("Total loaded invariants number: " + store.invariantList.size());

        long endTime = System.currentTimeMillis();
        System.out.println("loadInvs took" + (endTime - startTime) + " milliseconds");
        return store.invariantList.size()>0;
    }

    private void check() {
        System.out.println("Start to check for round " + roundCounter);
        System.out.println("Current eventtracer list size:" + EventTracer.instance.getQueueSize());

        if(force_disable_prod_checking)
        {
            System.out.println("Skip checking due to "+ConfigManager.FORCE_DISABLE_PROD_CHECKING_KEY+" set true");
            return;
        }

        Map<String, Integer> failStats = new HashMap<>();
        Map<String, Integer> totalStats = new HashMap<>();

        int succCount = 0;
        int failCount = 0;
        int inactiveCount = 0;

        int succCountAfterSuppress = 0;
        int failCountAfterSuppress = 0;
        int inactiveCountAfterSuppress = 0;
        List<Integer> failedInvs = new ArrayList<>();
        for (int i = 0; i < store.invariantList.size(); ++i) {
            Invariant inv = store.invariantList.get(i);

            String templateName = inv.template.getTemplateName();
            Invariant.InvState state = inv.verify(EventTracer.instance);
            if (state.equals(Invariant.InvState.FAIL)) {
                if(!suppressedInvs.contains(i))
                {
                    System.err.print("["+LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))+"]");
                    System.err.println("ASSERT FAIL! #" + i);
                    System.err.println(inv.toString());
                    failCountAfterSuppress++;
                }

                failCount++;
                failedInvs.add(i);

                failStats.put(templateName, failStats.getOrDefault(templateName, 0)+1);
                totalStats.put(templateName, totalStats.getOrDefault(templateName, 0)+1);

            } else if(state.equals(Invariant.InvState.INACTIVE))
            {
                if(!suppressedInvs.contains(i))
                {
                    inactiveCountAfterSuppress++;
                }
                inactiveCount++;
                totalStats.put(templateName, totalStats.getOrDefault(templateName, 0)+1);
            } else if(state.equals(Invariant.InvState.PASS))
            {
                if(!suppressedInvs.contains(i))
                {
                    succCountAfterSuppress++;
                }
                succCount++;
                totalStats.put(templateName, totalStats.getOrDefault(templateName, 0)+1);
            }
        }
        for(Map.Entry<String, Integer> entry: totalStats.entrySet())
        {
            String tName = entry.getKey();
            System.out.println(tName+" fails "+failStats.get(tName)+"/"+entry.getValue());
        }

        System.out.println("Checking finished, succCount:" + succCount + " failCount: " + failCount
        +  " inactiveCount: " + inactiveCount);
        System.out.println("After suppressing, succCount:" + succCountAfterSuppress + " failCount: " + failCountAfterSuppress
                +  " inactiveCount: " + inactiveCountAfterSuppress);

        roundCounter++;

        if(ConfigManager.config.getBoolean(ConfigManager.DUMP_SUPPRESS_INV_LIST_WHEN_CHECKING_KEY))
            dumpSuppressedInvs(failedInvs);
    }

    public void verifyFromVerifyPhase(String testName)
    {
        File dir = new File(FileLayoutManager.getPathForVerifiedInvOutputDir());
        if (!dir.exists()) dir.mkdirs();

        verify(EventTracer.instance, testName, dir.getAbsolutePath() +
                "/" + FileLayoutManager.EXCHANGE_RESULT_FILE_NAME, true);
    }

    public void runThroughTraces(EventTracer tracer, List<Integer> passInvs, List<Integer> inactiveInvs,List<Integer> failedInvs,
                                 boolean ignoreSurvivor) {
        long startTime = System.currentTimeMillis();

        int actualCheckedInvCount = 0;

        for (int i = 0; i < store.invariantList.size(); ++i) {
            //ignoreSurvivor is set true when using checking inv utility
            if (!ignoreSurvivor)
                if (verify_survivor_mode) {
                    if (!survivorInvs.contains(i))
                        continue;
                }

            Invariant inv = store.invariantList.get(i);
            OKHelper.debug("Start to verify for inv "+i);
            OKHelper.debug(inv.toString());
            Invariant.InvState state = inv.verify(tracer);
            if (state.equals(Invariant.InvState.PASS)) {
                passInvs.add(i);
            }
            else if (state.equals(Invariant.InvState.INACTIVE)) {
                inactiveInvs.add(i);
            }
            else if (state.equals(Invariant.InvState.FAIL)) {
                failedInvs.add(i);
            }

            actualCheckedInvCount++;
        }

        long endTime = System.currentTimeMillis();
        System.out.println("runThroughTraces "+actualCheckedInvCount+ " invs took" + (endTime - startTime) + " milliseconds");
    }

    public void output(String testName, String outputPath, boolean ifAppend, List<Integer> passInvs, List<Integer> inactiveInvs,List<Integer> failedInvs) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath, ifAppend))) {
            bw.write("[TEST] "+testName+"\n");
            bw.write("pass ");
            for (int i : passInvs) {
                bw.write(i + " ");
            }
            bw.newLine();
            bw.write("inac ");
            for (int i : inactiveInvs) {
                bw.write(i + " ");
            }
            bw.newLine();
            bw.write("fail ");
            for (int i : failedInvs) {
                bw.write(i + " ");
            }
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadSurvivors()
    {
        if(!verify_survivor_mode)
        {
            return;
        }

        String path = FileLayoutManager.getPathForVerifiedInvOutputDir()
                +"/"+FileLayoutManager.SURVIVOR_INV_FILE_NAME;

        BufferedReader reader = null;

        //already have data
        if((new File(path)).exists())
        {
            List<Integer> lst = new ArrayList<>();
            try {
                reader = new BufferedReader(new FileReader(path));
                String lastLine = null;
                String text = null;

                //find last line
                while ((text = reader.readLine()) != null) {
                    lastLine = text;
                }

                for (String intStr : lastLine.split("\\s+")) {
                    try {
                        int id = Integer.parseInt(intStr);
                        lst.add(id);

                    } catch (NumberFormatException ex) {
                        //just continue
                        //this is empty line
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            survivorInvs = lst;
            System.out.println("Loaded survivorInvs "+ survivorInvs.size());
        }
        else {

            //we are first, init with full invariant list
            for(int i=0;i<store.invariantList.size();++i)
            {
                survivorInvs.add(i);
            }

            System.out.println("survivorInvs file not created, inited with "+ survivorInvs.size()+" invs");
        }
    }

    public void dumpSurvivors()
    {
        if(!verify_survivor_mode)
        {
            return;
        }

        String path = FileLayoutManager.getPathForVerifiedInvOutputDir()
                +"/"+FileLayoutManager.SURVIVOR_INV_FILE_NAME;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path, true))) {
            for (int i : survivorInvs) {
                bw.write(i + " ");
            }
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void verify(EventTracer tracer, String testName, String outputPath, boolean ifAppend) {
        List<Integer> passInvs = new ArrayList<>();
        List<Integer> inactiveInvs = new ArrayList<>();
        List<Integer> failedInvs = new ArrayList<>();
        runThroughTraces(tracer,passInvs,inactiveInvs,failedInvs,false);
        output(testName, outputPath, ifAppend, passInvs, inactiveInvs,failedInvs);

        //update survivor list and dump
        survivorInvs.removeAll(failedInvs);
    }

    public void markTestFailed(String testName)
    {
        File dir = new File(FileLayoutManager.getPathForVerifiedInvOutputDir());
        if (!dir.exists()) dir.mkdirs();

        String outputPath = dir.getAbsolutePath() +
                "/" + FileLayoutManager.EXCHANGE_RESULT_FILE_NAME;
        boolean ifAppend = true;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath, ifAppend))) {
            bw.write("[TEST] "+testName+"\n");
            //we write magical number to mark the test failed
            bw.write("FAILED");
            bw.newLine();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void parseEventToMetaSet(SemanticEvent event, Set<String> opList, Set<String> stateList) {
        if (event != null) {
            if (event instanceof OpTriggerEvent) {
                OpTriggerEvent event1 = (OpTriggerEvent) event;
                opList.add(event1.opName);
            } else if (event instanceof StateUpdateEvent) {
                StateUpdateEvent event1 = (StateUpdateEvent) event;
                stateList.add(event1.stateName);
            }
        }
    }

    private void instrumenSelectively() {
        Set<String> opSet = new HashSet<>();
        Set<String> stateSet = new HashSet<>();

        long startTime = System.currentTimeMillis();

        for (Invariant inv : store.invariantList) {
            parseEventToMetaSet(inv.context.left, opSet, stateSet);
            parseEventToMetaSet(inv.context.right, opSet, stateSet);
            parseEventToMetaSet(inv.context.secondright, opSet, stateSet);
        }

        long endTime = System.currentTimeMillis();
        System.out.println("parseEventToMetaList took" + (endTime - startTime) + " milliseconds");

        DynamicClassModifier modifier = new DynamicClassModifier();
        modifier.modifySelectively(opSet, stateSet);
    }

    public void prepare() {
        long startTime = System.currentTimeMillis();

        if(System.getProperty("ok.invmode")!=null &&
                System.getProperty("ok.invmode").equals("verify"))
        {
            String testname = System.getProperty("ok.invfile");
            System.out.println("invfile:" + testname);
            loadInvs(FileLayoutManager.getPathForInvOutputDir(),testname,testname);
        }
        else if(System.getProperty("ok.invmode")!=null &&
                System.getProperty("ok.invmode").equals("prod"))
        {
            loadInvs(FileLayoutManager.getPathForProdInvInputDir(),
                    FileLayoutManager.VERIFIED_FILE_NAME,"prodInput");
            loadSuppressedInvs();
        }
        else
        {
            throw new RuntimeException("IMPOSSIBLE!");
        }


        //only load survivors in verify mode
        if(System.getProperty("ok.invmode")!=null &&
                System.getProperty("ok.invmode").equals("verify"))
            loadSurvivors();

        instrumenSelectively();

        long endTime = System.currentTimeMillis();

        System.out.println("prepare checker for verify took" + (endTime - startTime) + " milliseconds");
    }

    public void start() {
        prepare();
        new Thread(this).start();
    }

    @Override
    public void run() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

        try {

            //wait for the system
            Thread.sleep(5000);

            while (true) {
                Thread.sleep(1000);

                check();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
