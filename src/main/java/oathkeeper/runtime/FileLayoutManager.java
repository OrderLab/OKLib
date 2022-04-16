package oathkeeper.runtime;

/**
 * This class is to manage the layout of files that we read and generate during the workflow
 * Previously we handle this issue in an adhoc way
 * After we allow the working dir to the target system dir, we can no longer assume that we always
 * execute under the OathKeeper root dir (but we still want input and output aggregated under tool dir),
 * thus we need to make this part more rigorous
 *
 * Here the principle is, all file IO in this project should go through this interface to ensure path correctness
 * This class would also makes the design of layout more formal
 */


import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * layout:
 * OathKeeper/
 *      /trace_output
 *      /inv_output
 *      /inv_verified
 *      /oklogs
 */
public class FileLayoutManager {

    //logs
    public final static String LOG_DIR = "oklogs";

    //gentrace phase
    public final static String TRACES_OUTPUT_DIR_NAME = "trace_output";

    //infer phase
    public final static String INV_OUTPUT_DIR_NAME = "inv_infer_output";

    //verify phase
    public final static String VERIFIED_INV_DIR_NAME = "inv_verify_output";
    public final static String EXCHANGE_RESULT_FILE_NAME = "inv.id";
    public final static String SURVIVOR_INV_FILE_NAME = "inv.survived";
    public final static String TOTAL_FILE_NAME = "all_invs";
    public final static String VERIFIED_FILE_NAME = "verified_invs";
    public final static String DISCARDED_FILE_NAME = "discarded_invs";
    public final static String RANK_FILE_NAME = "rank_invs_by_succ";
    public final static String DIST_FILE_NAME = "invs_passed_test_dist.csv";

    //production phase
    public final static String PROD_LOADED_INVS_DIR_NAME = "inv_prod_input";
    public final static String PROD_SUPPRESSABLE_INVS_INPUT_NAME = "inv_suppressable_input";
    public final static String PROD_SUPPRESSABLE_INVS_OUTPUT_NAME = "inv_suppressable_output";

    //crosscheck phase
    public final static String INSTRUMENT_POINTS_FILE_NAME = "instrument_points";
    public final static String PRELOAD_INSTRUMENT_POINTS_DIR_NAME = "instrument_points_input";

    //check tool
    public final static String INV_CHECK_TRACE_DIR_NAME = "inv_checktrace_output";

    //junit test
    public final static String TEST_TRACE_OUTPUT_DIR_NAME = "test_traces";
    public final static String TEST_REAL_TRACES_DIR_NAME = "src/test/java/realtraces/";

    public static String target_system_abs_path = null;
    public static String ok_root_abs_path = null;

    static
    {
        //this is optional, can be null or empty
        target_system_abs_path = System.getProperty("ok.target_system_abs_path");
        //this must be assigned
        ok_root_abs_path = System.getProperty("ok.ok_root_abs_path");
        if(ok_root_abs_path == null || ok_root_abs_path.equals(""))
        {
            System.err.println("[ERROR] ok.ok_root_abs_path not specified, abort");
            System.exit(-1);
        }
    }

    public static String getPathForLogDir()
    {
        return Paths.get(ok_root_abs_path,LOG_DIR).toString();
    }

    public static String getPathForTracesOutputDir()
    {
        if(ConfigManager.getExecuteMode().equals(ConfigManager.ExecuteMode.GENTRACE)
                && ConfigManager.getGentraceInstrumentMode().equals(ConfigManager.InstrumentMode.SPECIFIED_SELECTIVE))
            return Paths.get(ok_root_abs_path,TRACES_OUTPUT_DIR_NAME+"_extended", System.getProperty("ok.ticket_id")).toString();
        else
            return Paths.get(ok_root_abs_path,TRACES_OUTPUT_DIR_NAME, System.getProperty("ok.ticket_id")).toString();
    }

    public static String getPathForInvOutputDir()
    {
        return Paths.get(ok_root_abs_path,INV_OUTPUT_DIR_NAME, System.getProperty("ok.ticket_id")).toString();
    }

    public static String getPathForVerifiedInvOutputDir()
    {
        return Paths.get(ok_root_abs_path,VERIFIED_INV_DIR_NAME, System.getProperty("ok.ticket_id")).toString();
    }

    public static String getPathForProdInvInputDir() {
        return Paths.get(ok_root_abs_path, PROD_LOADED_INVS_DIR_NAME).toString();
    }

    public static String getPathForProdSuppressableInvInputFile() {
        return Paths.get(ok_root_abs_path, PROD_SUPPRESSABLE_INVS_INPUT_NAME).toString();
    }

    public static String getPathForProdSuppressableInvOutputFile() {
        return Paths.get(ok_root_abs_path, PROD_SUPPRESSABLE_INVS_OUTPUT_NAME).toString();
    }

    public static String getPathForInstrumentPointFile() {
        return Paths.get(ok_root_abs_path, getPathForTracesOutputDir(),
                FileLayoutManager.INSTRUMENT_POINTS_FILE_NAME+"/"+System.getProperty("ok.ticket_id")).toString();
    }

    public static String getPathForPreloadInstrumentInputDir() {
        return Paths.get(ok_root_abs_path, PRELOAD_INSTRUMENT_POINTS_DIR_NAME).toString();
    }

    //public static String getPathForInvCheckerOutputFile()
    //{
    //    return Paths.get(ok_root_abs_path,TRACE_CHECK_OUTPUT_FILE_NAME).toString();
    //}

    public static String getPathForInvCheckerOutputDir()
    {
        return Paths.get(ok_root_abs_path,INV_CHECK_TRACE_DIR_NAME).toString();
    }

    public static String getPathForTestTraceDir()
    {
        return Paths.get(ok_root_abs_path,TEST_TRACE_OUTPUT_DIR_NAME).toString();
    }

    public static String getPathForTestRealTracesDir()
    {
        return Paths.get(ok_root_abs_path,TEST_REAL_TRACES_DIR_NAME).toString();
    }


    public static void cleanDir(String dirPath)
    {
        try{
            Files.walkFileTree(Paths.get(dirPath), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (NoSuchFileException ex)
        {
            //if not exist, it is already clean, we init it
            File dir = new File(dirPath);
            if (!dir.exists()) dir.mkdirs();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            System.exit(-1);
        }


    }
}
