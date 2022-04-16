package oathkeeper.tool;

import oathkeeper.runtime.ConfigManager;
import oathkeeper.runtime.FileLayoutManager;
import oathkeeper.runtime.InvariantStore;
import oathkeeper.runtime.RuntimeChecker;
import oathkeeper.runtime.invariant.Invariant;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/*
    Tool class to randomly generate subset of invariants (for perf testing use)
    See usage in scripts.
 */
public class InvSplitter {

    final static String SPLIT_FILE_NAME_PREFIX = "SplittedInvs";

    //todo: make it formal
    // this is to improve perf
    void filter(List<Invariant> invs)
    {
        System.out.println("WARN!! we filter some for perf issue");
        ListIterator<Invariant> iter = invs.listIterator();
        while(iter.hasNext()){
            Invariant inv = iter.next();
            if(inv.toString().contains("Header") ||
                    inv.toString().contains("QuorumPacket")){
                iter.remove();
            }
        }
    }

    //this is to reduce number
    void filterWithMustIncludeKeywords(List<Invariant> invs,List<String> keywords)
    {
        System.out.println("WARN!! we filter some to reduce number");
        ListIterator<Invariant> iter = invs.listIterator();
        while(iter.hasNext()){
            Invariant inv = iter.next();

            List<String> contexts = new ArrayList<>();
            if(inv.context.left!=null) contexts.add(inv.context.left.toString());
            if(inv.context.right!=null) contexts.add(inv.context.right.toString());
            if(inv.context.secondright!=null) contexts.add(inv.context.secondright.toString());
            for(String context:contexts)
            {
                boolean keywordMatch = false;
                if(keywords==null || keywords.isEmpty())
                    keywordMatch= true;
                else
                for(String keyword:keywords)
                {
                    if(context.contains(keyword))
                    {
                        keywordMatch = true;
                        break;
                    }
                }
                if(!keywordMatch)
                {
                    iter.remove();
                    break;
                }
            }

        }
    }

    public void split(String dirName, String nameFilter,List<String> keywords)
    {
        RuntimeChecker checker = new RuntimeChecker();
        checker.loadInvs(dirName, nameFilter, "splitInput");
        List<Invariant> totalInvs = checker.store.invariantList;

        System.out.println("Read inv size:"+totalInvs.size());

        System.out.println("Keywords:");
        if(keywords!=null)
        for(String keyword: keywords)
            System.out.println(keyword);

        filter(totalInvs);
        filterWithMustIncludeKeywords(totalInvs, keywords);

        double ratio_list[] = {0.25,0.5,0.75,1.00};
        for(double ratio:ratio_list)
        {
            Collections.shuffle(totalInvs);

            InvariantStore store = new InvariantStore();
            store.invariantList = totalInvs.subList(0,(int)(totalInvs.size()*ratio));

            try {
                FileLayoutManager.cleanDir(SPLIT_FILE_NAME_PREFIX+"_"+ratio);
                File logFile = new File(SPLIT_FILE_NAME_PREFIX+"_"+ratio+"/"+ FileLayoutManager.VERIFIED_FILE_NAME);
                //cleanup old one
                Files.deleteIfExists(logFile.toPath());

                Writer writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(logFile)));
                writer.write(store.serialize());

                writer.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        System.out.println("Total inv size:"+totalInvs.size());
        System.out.println("Finished split.");
    }

    public static void main(String[] args)
    {
        ConfigManager manager = new ConfigManager();
        manager.initConfig();

        InvSplitter splitter = new InvSplitter();
        splitter.split(args[0], FileLayoutManager.VERIFIED_FILE_NAME,
                args.length>1?Arrays.asList(Arrays.copyOfRange(args, 1,args.length)):null);
    }
}
