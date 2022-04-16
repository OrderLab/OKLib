package oathkeeper.tool;

import oathkeeper.runtime.*;
import oathkeeper.runtime.invariant.Invariant;

import java.io.*;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/* A utility class, can be used in two ways
    * 1) check a list of invariants on arbitrary trace files and show results
    * 2) apply a list of invariants to a pair or execution traces (patched and unpatched), and dump those
    *   invs that **detect** that differences, which means pass in patched version and fail in unpatched version

 */
public class InvChecker {

    RuntimeChecker runtimeChecker;

    public static void main(String[] args)
    {
        if(args.length<=2)
        {
            throw new RuntimeException("No enough args!");
        }

        System.out.println("Args:");
        for(String arg: args)
        {
            System.out.println(arg);
        }

        InvChecker invChecker = new InvChecker();

        ConfigManager configManager = new ConfigManager();
        configManager.initConfig();

        String invFile = args[1];
        String traceFiles = args[2];
        boolean verifyOrDetectMode = true;
        String outputFile = null;

        if(args[0].equals("verify"))
            verifyOrDetectMode = true;
        else if(args[0].equals("detect"))
            verifyOrDetectMode = false;
        else
            throw new RuntimeException("Incorrect args, should be either verify or detect!");

        if(args.length>=4)
            outputFile = args[3];
        else
            outputFile = "invChecker.output";

        if(args.length>=5)
            OKHelper.ifDebugEnabled = !args[4].equals("ifDebugEnabled=false");

        invChecker.runtimeChecker = new RuntimeChecker();
        invChecker.runtimeChecker.store.invariantList.addAll(
                InvariantStore.loadFromFile(invFile).invariantList);

        File dir = new File(FileLayoutManager.getPathForInvCheckerOutputDir());
        if (!dir.exists()) dir.mkdirs();

        Path outputFilePath = Paths.get(FileLayoutManager.getPathForInvCheckerOutputDir(),outputFile);
        try {
            Files.delete(outputFilePath);
        }
        catch (NoSuchFileException ex)
        {
            System.out.println("Output file not found, no need to delete");
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }

        if(verifyOrDetectMode)
            invChecker.verify(traceFiles, outputFilePath.toString());
        else
            invChecker.detect(traceFiles, outputFilePath.toString());
    }

    static List<String> parse(String traceFilesStr)
    {
        return Arrays.asList(traceFilesStr.split(";"));
    }

    //usage 1)
    //verify is the simpler usage, just apply inv on traces and output the result, pass or not
    void verify(String traceFiles, String outputPath) {
        for(String traceFile: parse(traceFiles))
        {
            if(traceFile.isEmpty())
                continue;

            EventTracer tracer = EventTracer.loadFromFile(traceFile);
            if(tracer==null)
                continue;
            List<Integer> passInvs = new ArrayList<>();
            List<Integer> inactiveInvs = new ArrayList<>();
            List<Integer> failedInvs = new ArrayList<>();
            runtimeChecker.runThroughTraces(tracer,passInvs,inactiveInvs,failedInvs,true);
            runtimeChecker.output(traceFile, outputPath, true, passInvs,inactiveInvs,failedInvs);
        }
    }

    // usage 2)
    //detect is actually wrapper on top of basic verify functions, it would check on both patched and unpatched traces,
    //only pass on patched traces and fail on unpatched traces, we would call it detect successfully
    void detect(String traceFiles, String outputPath) {
        boolean ifDetected = false;
        List<Integer> out_lst = new ArrayList<>();

        for(String traceFile: parse(traceFiles))
        {
            if(traceFile.isEmpty())
                continue;

            EventTracer patchedTracer = EventTracer.loadFromFile(traceFile);
            if(patchedTracer==null)
            {
                System.err.println("Cannot find "+traceFile);
                continue;
            }
            List<Integer> p_passInvs = new ArrayList<>();
            List<Integer> p_inactiveInvs = new ArrayList<>();
            List<Integer> p_failedInvs = new ArrayList<>();
            runtimeChecker.runThroughTraces(patchedTracer,p_passInvs,p_inactiveInvs,p_failedInvs,true);
            runtimeChecker.output(traceFile, outputPath+"_p", true, p_passInvs,p_inactiveInvs,p_failedInvs);

            EventTracer unpatchedTracer = EventTracer.loadFromFile(traceFile.replace(EventTracer.PATCHED_SUFFIX,EventTracer.UNPATCHED_SUFFIX));
            if(unpatchedTracer==null)
            {
                System.err.println("Cannot find "+traceFile.replace(EventTracer.PATCHED_SUFFIX,EventTracer.UNPATCHED_SUFFIX));
                continue;
            }
            List<Integer> up_passInvs = new ArrayList<>();
            List<Integer> up_inactiveInvs = new ArrayList<>();
            List<Integer> up_failedInvs = new ArrayList<>();
            runtimeChecker.runThroughTraces(unpatchedTracer,up_passInvs,up_inactiveInvs,up_failedInvs,true);
            runtimeChecker.output(traceFile, outputPath+"_up", true, up_passInvs,up_inactiveInvs,up_failedInvs);

            p_passInvs.retainAll(up_failedInvs);
            if(!p_passInvs.isEmpty())
            {
                ifDetected = true;
                out_lst = p_passInvs;
                break;
            }
        }

        try{
            if(ifDetected)
            {
                FileWriter writer = new FileWriter(outputPath+"_detected");
                for(Integer i: out_lst)
                {
                    writer.write(String.valueOf(i)+" "+runtimeChecker.store.invariantList.get(i).toString()+"\n");
                }
                writer.close();
            }
            else {
                new FileOutputStream(outputPath+"_undetected").close();
            }

        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

}
