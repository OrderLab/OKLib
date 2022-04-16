package oathkeeper.engine;

import oathkeeper.runtime.ConfigManager;
import oathkeeper.runtime.RuntimeChecker;

import java.lang.reflect.Method;
import java.util.Arrays;


/**
 *  Main entry for wrapped system instance
    we need to do dynamic instrumentation before everything else, so the production system must be started using this entry
    systems usually have their own startup scripts, for example,
    zookeeper: modify zkServer.sh,
          ZOOMAIN="org.apache.zookeeper.server.quorum.QuorumPeerMain"
              ->
          ZOOMAIN="oathkeeper.engine.MainWrapper org.apache.zookeeper.server.quorum.QuorumPeerMain"
    hdfs: modify libexec/hadoop-functions.sh,
          in function hadoop_start_daemon
          exec "${JAVA}" "-Dproc_${command}" ${HADOOP_OPTS} "${class}" "$@"
          ->
          exec "${JAVA}" "-Dproc_${command}" ${HADOOP_OPTS} "oathkeeper.engine.MainWrapper" "${class}" "$@"
 */

public class MainWrapper {

    RuntimeChecker checker;
    ConfigManager configManager;

    private static void invokeMainClass(String[] args) {
        //invoke original main class
        try {
            Class<?> cls = Class.forName(args[0]);
            Method meth = cls.getMethod("main", String[].class);
            String[] params = Arrays.copyOfRange(args, 1, args.length);
            meth.invoke(null, (Object) params);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Check the main class arg!");
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Cannot find main method in target class" + args[0]);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Exception when trying to invoke");
        }
    }

    void start()
    {
        configManager = new ConfigManager();
        configManager.initConfig();

        //start runtime checker here, instrument inside checker
        checker = new RuntimeChecker();
        checker.start();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            throw new RuntimeException("No enough args!");
        }

        MainWrapper mainWrapper = new MainWrapper();
        mainWrapper.start();

        invokeMainClass(args);
    }
}
