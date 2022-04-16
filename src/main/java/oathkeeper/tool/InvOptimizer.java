package oathkeeper.tool;

import oathkeeper.runtime.ConfigManager;
import oathkeeper.runtime.FileLayoutManager;
import oathkeeper.runtime.RuntimeChecker;

public class InvOptimizer {

    public void analyze(String dirName, String nameFilter)
    {
        RuntimeChecker checker = new RuntimeChecker();
        checker.loadInvs(dirName, nameFilter, "InvOptimizer");
        checker.store.optimize();

        System.out.println("After optimize, remains "+ checker.store.invariantList.size()+" invs!");
    }

    public static void main(String[] args)
    {
        ConfigManager manager = new ConfigManager();
        manager.initConfig();

        InvOptimizer printer = new InvOptimizer();
        printer.analyze(args[0], FileLayoutManager.VERIFIED_FILE_NAME);
    }
}
