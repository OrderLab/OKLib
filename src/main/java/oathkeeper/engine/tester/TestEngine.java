package oathkeeper.engine.tester;


import oathkeeper.runtime.*;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import oathkeeper.tool.InvMerger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Main class for offline test case scheduling engine
 * the purpose of this part is to track each test and do two things
 * 1) divide execution of each test so we are when we have some outputs what are the current running test, by
 * printing out the test class and method name
 * 2) dump traces for each test
 */

//how to run:
//java -cp [sys_lib_path:sys_test_path] oathkeeper.engine.tester.TestEngine

/*
order here is extremely important!
previously we met a bug that some methods cannot be found, but they are indeed in the package, later
we found out somehow in the build/lib/jars/ there are some same packages but older versions, cause cannot find methods
*/
public class TestEngine {
    static final String OUPUTSTREAM_FILE_NAME = "ok.out";

    static PrintStream stream;
    static{
        try {
            stream = new PrintStream(new FileOutputStream(OUPUTSTREAM_FILE_NAME,true),true);
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    TestClassPool pool = new TestClassPool();
    //in the verify mode we need to run the checker at the end of test
    public RuntimeChecker checker;
    ConfigManager configManager = new ConfigManager();

    //save some critical errors
    public List<String> criticalErrors = new ArrayList<>();

    public int spawnProcess(String klass) throws IOException,
            InterruptedException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome +
                File.separator + "bin" +
                File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String className = TestEngine.class.getName();

        String rootPath = System.getProperty("user.dir");

        List<String> cmds = new ArrayList<>(Arrays.asList(
                javaBin, "-cp", classpath,
                "-Dok.testname="+klass,
                "-Dok.invmode="+System.getProperty("ok.invmode"),
                //set the mode is patched or unpatched
                "-Dok.patchstate="+System.getProperty("ok.patchstate"),
                "-Dok.patchid="+System.getProperty("ok.patchid"),
                "-Dok.conf="+System.getProperty("ok.conf"),
                "-Dok.filediff="+System.getProperty("ok.filediff"),
                "-Dok.invfile="+System.getProperty("ok.invfile"),
                "-Dok.ticket_id="+System.getProperty("ok.ticket_id"), //e.g. ZK-1208, this is specified implicitly by property file name
                "-Dok.ok_root_abs_path="+System.getProperty("ok.ok_root_abs_path"),
                "-Dok.target_system_abs_path="+System.getProperty("ok.target_system_abs_path"),
                "-Dok.test_trace_prefix="+System.getProperty("ok.test_trace_prefix"),
                "-Dok.verify_test_package="+System.getProperty("ok.verify_test_package"),
        ));

        String jvm_args = ConfigManager.config.getString(ConfigManager.JVM_ARGS_FOR_TESTS_KEY);
        if (!jvm_args.equals(""))
            cmds.addAll(Arrays.asList(jvm_args.split("\\s+")));
        cmds.add(className);
        ProcessBuilder builder = new ProcessBuilder(cmds);

        Process process = builder.inheritIO().start();
        //set the timeout threshold to be
        //relax a little bit, loading massive invariants for initial cases could take a lot of time
        //update: set aggressive timeout
        //TODO: make it configurable
        int TIMEOUT_THRESHOLD = 5;
        if(ConfigManager.getExecuteMode().equals(ConfigManager.ExecuteMode.GENTRACE))
            TIMEOUT_THRESHOLD = 20;
        else if(ConfigManager.getExecuteMode().equals(ConfigManager.ExecuteMode.VERIFY) ||
                        ConfigManager.getExecuteMode().equals(ConfigManager.ExecuteMode.VERIFY_BAREMETAL))
            TIMEOUT_THRESHOLD = 5;

        boolean exited = process.waitFor(TIMEOUT_THRESHOLD, TimeUnit.MINUTES);
        if(!exited) {
            System.out.println("Test "+klass+" timeout, terminates it forcibly by OathKeeper.");
            System.out.println("Threshold is "+TIMEOUT_THRESHOLD+".");
            process.destroyForcibly();
        }
        return 0;
    }

    public void execSingleTest(String className) {
        JUnitCore core = new JUnitCore();
        core.addListener(new TestListener(this));
        core.addListener(new TextListener(System.out));

        System.out.println("execSingleTest for "+className);

        try {
            //iterate
            Result result = core.run(pool.getClass(className));
            OKHelper.globalLogInfo(className + " failures: " + result.getFailureCount() + "/" + result.getRunCount());

        } catch (Exception ex) {
            System.err.println("Exception when executing test class:" + className);
            ex.printStackTrace();
        }

        //print some important checking results for genTrace phase
        if(System.getProperty("ok.invmode").equals("dump"))
        {
            if(!criticalErrors.isEmpty())
            {
                System.err.println("Detected possible issues when generate traces");
                for(String log:criticalErrors)
                    System.err.println("[WARN] "+log);
            }
        }



        //do clean exit
        System.exit(0);
    }

    public static void main(String... args) {
        TestEngine testEngine = new TestEngine();
        testEngine.configManager.initConfig();

        DynamicClassModifier modifier = new DynamicClassModifier();

        //spawned process
        if (System.getProperty("ok.testname")!=null){
            //dump traces need to instrument for all potential hooks
            if(System.getProperty("ok.invmode").equals("dump"))
            {
                modifier.markEndOfTestMethods();
                modifier.modifyAll();
            }
            else if(System.getProperty("ok.invmode").equals("verify"))
            {
                testEngine.checker = new RuntimeChecker();
                testEngine.checker.prepare();
            }
            testEngine.execSingleTest(System.getProperty("ok.testname"));
        }
        else {
            //main entry
            try {
                if(System.getProperty("ok.invmode").equals("dump"))
                {
                    testEngine.pool.registerSpecificClasses();
                    FileLayoutManager.cleanDir(FileLayoutManager.getPathForTracesOutputDir());
                }
                else if(System.getProperty("ok.invmode").equals("verify"))
                {
                    //pre-check if the inv file exists (this checker is just for testing-only)
                    RuntimeChecker checker = new RuntimeChecker();
                    String testname = System.getProperty("ok.invfile");
                    if (!checker.loadInvs(FileLayoutManager.getPathForInvOutputDir(),testname,testname))
                    {
                        System.err.println("ERROR: no inv found for "+FileLayoutManager.getPathForInvOutputDir());
                        System.exit(-1);
                    }

                    //register all test classes by default
                    testEngine.pool.registerAllClass();

                    FileLayoutManager.cleanDir(FileLayoutManager.getPathForVerifiedInvOutputDir());
                }
                else if(System.getProperty("ok.invmode").equals("verify_baremetal"))
                {
                    testEngine.pool.registerAllClass();
                }
                else
                {
                    System.err.println("Bad args! Exit..");
                    System.exit(-1);
                }

                int count = 0;
                for (String clazz : testEngine.pool.getClasses()) {
                    System.out.println("Spawn test for "+(count+1)+"/"+testEngine.pool.getClasses().size());
                    testEngine.spawnProcess(clazz);
                    count++;
                }

                //if in the verify mode, aggregate the verify result
                if(System.getProperty("ok.invmode").equals("verify"))
                {
                    InvMerger merger = new InvMerger();
                    merger.output_dir = FileLayoutManager.getPathForVerifiedInvOutputDir();
                    merger.aggregate();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
