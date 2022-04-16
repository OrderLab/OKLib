package oathkeeper.runtime;

import java.io.*;

/**
 * A helper class to be used in the hooks in the systems to do certain common func, e.g. logging, set states
 */
public class OKHelper {
    private static OKHelper helper = new OKHelper();

    private static final String LOGFILEPREFIX = "oklog_";
    private String lastTestName = "";

    //log for each test case
    private Writer logwriter;
    private String logFileName;

    //shared log for all test cases
    private Writer globalLogwriter;
    private String globalLogFileName = "oktests.summary";

    //runtime log in production
    private Writer productionLogwriter;
    private String productionLogFileName = "ok.prod.log";

    public static boolean ifDebugEnabled = false;

    private void init() {
        // init in lazyway because we need to wait until test case to feed us log name
        try {

            File directory = new File(FileLayoutManager.getPathForLogDir());
            if (!directory.exists()) {
                directory.mkdir();
            }

            //in cassandra we didn't get per-test name, we have to run one-by-one manually
            logFileName = LOGFILEPREFIX + lastTestName;
            logwriter = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(FileLayoutManager.getPathForLogDir() + "/" + logFileName, true)));

            globalLogwriter = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(FileLayoutManager.getPathForLogDir() + "/" + globalLogFileName, true)));

            productionLogwriter = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(FileLayoutManager.getPathForLogDir() + "/" + productionLogFileName)));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static OKHelper getInstance() {
        return helper;
    }

    void logInfoInternal(String str) {
        //if we move to another test, re-init
        if (logFileName == null || !logFileName.equals(LOGFILEPREFIX + lastTestName))
            init();

        try {
            logwriter.write(str + "\n");
            logwriter.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void logInfo(String str) {
        getInstance().logInfoInternal(str);

    }

    void globallogInfoInternal(String str) {
        if (globalLogwriter == null)
            init();

        try {
            globalLogwriter.write(str + "\n");
            globalLogwriter.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public PrintWriter getGlobalLogWriter() {
        return new PrintWriter(globalLogwriter);
    }

    public static void globalLogInfo(String str) {
        getInstance().globallogInfoInternal(str);

    }

    void prodlogInfoInternal(String str) {
        if (productionLogwriter == null)
            init();

        try {
            productionLogwriter.write(str + "\n");
            productionLogwriter.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void prodLogInfo(String str) {
        getInstance().prodlogInfoInternal(str);

    }

    public static void debug(String str) {
        if(ifDebugEnabled)
            System.out.println(str);

    }

    public void setLastTestName(String name) {
        lastTestName = name;
    }

    public static boolean ifFaultInjected(String faultID) {
        boolean result = new File("./fault." + faultID).isFile();
        if (result)
            System.out.println("CHANG: fault " + faultID + "triggered!");
        return result;
    }
}