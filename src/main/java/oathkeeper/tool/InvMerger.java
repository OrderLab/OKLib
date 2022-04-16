package oathkeeper.tool;

import oathkeeper.runtime.FileLayoutManager;
import oathkeeper.runtime.InvariantStore;
import oathkeeper.runtime.RuntimeChecker;
import oathkeeper.runtime.gson.GsonUtils;
import oathkeeper.runtime.invariant.Invariant;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static oathkeeper.runtime.CalcUtils.sortByValue;

/*
    Tool class to merge, which is part of verifying
    note this is not an isolated tool class
 */
public class InvMerger {

    public String output_dir = "invalid";

    private void dumpInvs(List<Invariant> invs, String fileName)
    {
        InvariantStore store = new InvariantStore(invs);
        try {
            File logFile = new File(output_dir+"/"+fileName);
            //cleanup old one
            Files.deleteIfExists(logFile.toPath());

            Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(logFile)));

            writer.write(GsonUtils.gsonPrettyPrinter.toJson(store));

            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //dump the distribution of passed test to inspect
    private void dumpDist(Map<Integer, Integer> occuranceMap, String fileName)
    {
        //phase 1
        //transform occuranceMap to distMap
        //key: how many test passed
        //val: list of invs
        TreeMap<Integer, List<Integer>> distMap = new TreeMap<>();

        for(Map.Entry<Integer, Integer> entry: occuranceMap.entrySet())
        {
            //id of inv
            Integer key = entry.getKey();
            //how many test passed
            Integer val = entry.getValue();
            if(distMap.containsKey(val))
            {
                distMap.get(val).add(key);
            }
            else {
                List<Integer> lst = new ArrayList<>();
                lst.add(key);
                distMap.put(val, lst);
            }
        }

        //phase 2
        //dump the distMap
        try {
            File logFile = new File(output_dir+"/"+fileName);
            //cleanup old one
            Files.deleteIfExists(logFile.toPath());

            Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(logFile)));

            for(Map.Entry<Integer, List<Integer>> entry:distMap.entrySet())
            {
                writer.write(entry.getKey()+","+entry.getValue().size()+","+entry.getValue());
                writer.write("\n");
            }
            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void summarizeVerifiedInvs(Map<Integer, Integer> passInvMap, Map<Integer, Integer> inactiveInvMap,
                                       Map<Integer, Integer> failInvMap, List<Invariant> totalInvs, List<Invariant> outputVerifiedInvs,
                                       Map<Integer, Invariant.VerifyStats> statsMap)
    {
        //phase 1: sort

        Map<Integer, Integer> sortedMap = sortByValue(passInvMap);

        //phase 2: dump
        for(Integer key:sortedMap.keySet())
        {
            Integer passNum = (passNum = passInvMap.get(key)) != null ? passNum : 0;
            Integer inactiveNum = (inactiveNum = inactiveInvMap.get(key)) != null ? inactiveNum : 0;
            Integer failNum = (failNum = failInvMap.get(key)) != null ? failNum : 0;
            //don't print useless invs
            Invariant.VerifyStats stats = new Invariant.VerifyStats(passNum,inactiveNum,failNum);
            //RULE: ALLOWS SOME VIOLATIONS (<=5)
            if(!(failNum>5 || passNum==0)){
                Invariant inv = totalInvs.get(key);
                inv.stats = stats;
                outputVerifiedInvs.add(inv);
            }

            statsMap.put(key, stats);
        }
    }

    private void dumpRank(Map<Integer, Invariant.VerifyStats> statsMap, String fileName)
    {

        try {
            File logFile = new File(output_dir+"/"+fileName);
            //cleanup old one
            Files.deleteIfExists(logFile.toPath());

            Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(logFile)));

            //traverse twice

            //first
            for(Map.Entry<Integer, Invariant.VerifyStats> entry: statsMap.entrySet())
            {
                if(entry.getValue().failNum==0)
                {
                    writer.write( entry.getKey()+" "+entry.getValue().passNum +
                            " " + entry.getValue().inactiveNum + " " + entry.getValue().failNum);
                    writer.write("\n");
                }
            }

            //second
            for(Map.Entry<Integer, Invariant.VerifyStats> entry: statsMap.entrySet())
            {
                if(entry.getValue().failNum>0)
                {
                    writer.write( entry.getKey()+" "+entry.getValue().passNum +
                            " " + entry.getValue().inactiveNum + " " + entry.getValue().failNum);
                    writer.write("\n");
                }
            }
            //leave a EOF in case we thought nothing useful get printed
            writer.write("EOF.\n");
            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void aggregate()
    {
        long startTime = System.currentTimeMillis();

        RuntimeChecker checker = new RuntimeChecker();
        String testname = System.getProperty("ok.invfile");
        System.out.println("invfile:" + testname);
        checker.loadInvs(FileLayoutManager.getPathForInvOutputDir(),testname,testname);
        List<Invariant> totalInvs = checker.store.invariantList;

        //<invid, count>
        Map<Integer, Integer> passInvMap = new HashMap<>();
        Map<Integer, Integer> inactiveInvMap = new HashMap<>();
        Map<Integer, Integer> passOrInactiveInvMap = new HashMap<>();
        Map<Integer, Integer> failInvMap = new HashMap<>();
        List<Invariant> verifiedInvs = new ArrayList<>();
        Map<Integer, Invariant.VerifyStats> statsMap = new LinkedHashMap<>();

        //init passInvMap
        for(int i=0;i<totalInvs.size();++i)
        {
            passInvMap.put(i,0);
        }

        File file = new File(output_dir+"/"+ FileLayoutManager.EXCHANGE_RESULT_FILE_NAME);
        BufferedReader reader = null;

        int headerCount = 0;
        int failedCount = 0;

        //the format of exchange file looks like (suppose you have 10 invs):
        //[TEST] test1
        //FAILED
        //[TEST] test2
        //pass 1 2 3 4 5                                  (passed invs)
        //inac 6 7                                        (inactive invs in this test)
        //fail 8 9 10                                     (failed invs)
        try {
            reader = new BufferedReader(new FileReader(file));
            String text = null;

            while ((text = reader.readLine()) != null) {
                if (text.startsWith("[TEST]"))
                {
                    //just start of a new block
                    headerCount++;
                }
                else if (text.startsWith("FAILED"))
                    failedCount++;
                else
                {
                    Map<Integer, Integer> invMapRef = null;
                    if(text.startsWith("pass"))
                        invMapRef = passInvMap;
                    else if(text.startsWith("inac"))
                        invMapRef = inactiveInvMap;
                    else if(text.startsWith("fail"))
                        invMapRef = failInvMap;
                    else
                    {
                        throw new RuntimeException("[ERROR] Incorrect format detected with content: "+text);
                    }

                    for(String intStr: text.substring(4).split("\\s+"))
                    {
                        try {
                            int id = Integer.parseInt(intStr);
                            invMapRef.putIfAbsent(id,0);
                            invMapRef.put(id,invMapRef.get(id)+1);

                        } catch (NumberFormatException ex)
                        {
                            //just continue
                            //this is empty line
                        }
                    }
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

        passOrInactiveInvMap = sumMap(passInvMap,inactiveInvMap);

        //System.out.println("Result: ");

        int maxPass = passInvMap.isEmpty()?0:Collections.max(passInvMap.values());
        int maxSucc = passOrInactiveInvMap.isEmpty()?0:Collections.max(passOrInactiveInvMap.values());
//        for(Map.Entry<Integer, Integer> entry:passOrInactiveInvMap.entrySet())
//        {
//            if(entry.getValue()>=maxSucc)
//            {
//                //System.out.print(entry.getKey()+":"+entry.getValue()+" ");
//                verifiedInvs.add(totalInvs.get(entry.getKey()));
//            }
//        }

        summarizeVerifiedInvs(passInvMap, inactiveInvMap, failInvMap, totalInvs, verifiedInvs, statsMap);

        System.out.println("[[SUMMARY]]");
        System.out.println("Total finished tests:"+(headerCount-failedCount));
        System.out.println("Total aborted tests:"+failedCount);
        System.out.println("Most successful invs pass or inactive in "+maxSucc+" tests");
        System.out.println("Most valuable invs pass in "+maxPass+" tests");
        System.out.println("Dumping output now...");
        dumpInvs(verifiedInvs, FileLayoutManager.VERIFIED_FILE_NAME);
        System.out.println("Dumped "+verifiedInvs.size()+" invs");
        dumpInvs(totalInvs, FileLayoutManager.TOTAL_FILE_NAME);
        totalInvs.removeAll(verifiedInvs);
        dumpInvs(totalInvs, FileLayoutManager.DISCARDED_FILE_NAME);
        dumpDist(passOrInactiveInvMap, FileLayoutManager.DIST_FILE_NAME);
        dumpRank(statsMap, FileLayoutManager.RANK_FILE_NAME);
        System.out.println("Verify finished.");

        long endTime = System.currentTimeMillis();

        System.out.println("Merge took " + (endTime - startTime) + " milliseconds");
    }

    private Map<Integer, Integer> sumMap(Map<Integer, Integer>... maps) {
        return Stream.of(maps)    // Stream<Map<..>>
                .map(Map::entrySet)  // Stream<Set<Map.Entry<..>>
                .flatMap(Collection::stream) // Stream<Map.Entry<..>>
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue,
                        Integer::sum));
    }

    public static void main(String[] args)
    {

        InvMerger merger = new InvMerger();
        merger.output_dir = FileLayoutManager.getPathForVerifiedInvOutputDir();
        merger.aggregate();
    }
}