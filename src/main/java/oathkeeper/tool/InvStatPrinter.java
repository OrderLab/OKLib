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
    Tool class to show stats of inv list such as type distributions for debugging use
    See usage in scripts.
 */
public class InvStatPrinter {

    public void analyze(String dirName, String nameFilter)
    {
        RuntimeChecker checker = new RuntimeChecker();
        checker.loadInvs(dirName, nameFilter, "InvStatPrinter");
        checker.store.stat();
    }

    public static void main(String[] args)
    {
        ConfigManager manager = new ConfigManager();
        manager.initConfig();

        InvStatPrinter printer = new InvStatPrinter();
        printer.analyze(args[0], FileLayoutManager.VERIFIED_FILE_NAME);
    }
}
