package oathkeeper.runtime;

import java.util.Arrays;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

public class ConfigManager {

    public enum ExecuteMode
    {
        GENTRACE,
        VERIFY,
        VERIFY_BAREMETAL,
        PROD,
        OTHERS
    }
    enum InstrumentMode
    {
        STRICT_SELECTIVE, //only instrument git diff
        RELAXED_SELECTIVE, //append usage from git diff
        SPECIFIED_SELECTIVE, //use dumped instrument points, support aggregated, used for crosschecking
        FULL // instrument everything specified by package prefix
    }

    static String CONFIG_FILE_NAME = "okconfig.properties";
    public static PropertiesConfiguration config;

    public static String SYSTEM_DIR_PATH_KEY = "system_dir_path";
    public static String SYSTEM_PACKAGE_PREFIX_KEY = "system_package_prefix";
    public static String TEST_CLASS_NAME_REGEX_KEY = "test_class_name_regex";
    public static String JVM_ARGS_FOR_TESTS_KEY = "jvm_args_for_tests";
    public static String SPECIFIED_TEST_CLASS_LIST_KEY = "specified_test_class_list";
    public static String EXCLUDED_TEST_METHOD_LIST_KEY = "excluded_test_method_list";
    public static String OP_INTERFACE_CLASS_LIST_KEY = "op_interface_class_list";

    public static String INSTRUMENT_STATE_FIELDS_KEY = "instrument_state_fields";
    public static String INSTRUMENT_CLASS_ALLMETHODS_KEY = "instrument_class_allmethods";
    public static String EXCLUDE_CLASS_LIST_KEY = "exclude_class_list";
    public static String EXCLUDE_METHOD_LIST_KEY = "exclude_method_list";

    public static String TIME_WINDOW_LENGTH_IN_MILLIS_KEY = "time_window_length_in_millis";
    //WARN: this is deprecated, we still check it to avoid config errors
    public static String ENABLE_FULL_INSTRUMENT_MODE_KEY = "enable_full_instrument_mode";
    public static String GENTRACE_INSTRUMENT_MODE_KEY = "gentrace_instrument_mode";

    public static String VERIFY_ABORT_AFTER_THREE_TEST_KEY = "verify_abort_after_three_test";
    public static String VERIFY_SURVIVOR_MODE_KEY = "verify_survivor_mode";

    public static String FORCE_INSTRUMENT_NOTHING_KEY = "force_instrument_nothing";
    public static String FORCE_TRACK_NO_STATES_KEY = "force_track_no_states";
    public static String FORCE_DISABLE_PROD_CHECKING_KEY = "force_disable_prod_checking";
    public static String FORCE_DISABLE_ENQUEUE_EVENTS_KEY = "force_disable_enqueue_events";
    public static String DUMP_SUPPRESS_INV_LIST_WHEN_CHECKING_KEY = "dump_suppress_inv_list_when_checking";

    public void initConfig() {
        //init config
        String confname = System.getProperty("ok.conf");
        if (confname != null)
            CONFIG_FILE_NAME = confname;

        try {
            config = new PropertiesConfiguration(CONFIG_FILE_NAME);
            config.setListDelimiter(',');
            String system_dir_path = config.getString(SYSTEM_DIR_PATH_KEY);
            String prefix = config.getString(SYSTEM_PACKAGE_PREFIX_KEY);
            String regex = config.getString(TEST_CLASS_NAME_REGEX_KEY);
            // trim double quote
            config.setProperty(JVM_ARGS_FOR_TESTS_KEY,
                config.getString(JVM_ARGS_FOR_TESTS_KEY).replaceAll("^\"|\"$", ""));
            String jvm_args = config.getString(JVM_ARGS_FOR_TESTS_KEY);
            String[] specifiedClassList = config.getStringArray(SPECIFIED_TEST_CLASS_LIST_KEY);
            String[] excludedMethodList = config.getStringArray(EXCLUDED_TEST_METHOD_LIST_KEY);
            String[] interfaceClassList = config.getStringArray(OP_INTERFACE_CLASS_LIST_KEY);
            String[] instrument_state_fields = config.getStringArray(INSTRUMENT_STATE_FIELDS_KEY);
            String[] instrument_class_allmethods = config.getStringArray(INSTRUMENT_CLASS_ALLMETHODS_KEY);
            String[] exclude_class_list = config.getStringArray(EXCLUDE_CLASS_LIST_KEY);
            String[] exclude_method_list = config.getStringArray(EXCLUDE_METHOD_LIST_KEY);
            long time_window_length_in_millis = config.getLong(TIME_WINDOW_LENGTH_IN_MILLIS_KEY);
            if(config.containsKey(ENABLE_FULL_INSTRUMENT_MODE_KEY))
            {
                System.err.println("ERROR: should not contain "+ENABLE_FULL_INSTRUMENT_MODE_KEY+" ,exits");
                System.exit(-1);
            }
            String gentrace_instrument_mode = config.getString(GENTRACE_INSTRUMENT_MODE_KEY);
            boolean verify_abort_after_three_test = config.getBoolean(VERIFY_ABORT_AFTER_THREE_TEST_KEY);
            boolean verify_survivor_mode = config.getBoolean(VERIFY_SURVIVOR_MODE_KEY);
            boolean force_instrument_nothing = config.getBoolean(FORCE_INSTRUMENT_NOTHING_KEY);
            boolean force_track_no_states = config.getBoolean(FORCE_TRACK_NO_STATES_KEY);
            boolean force_disable_prod_checking = config.getBoolean(FORCE_DISABLE_PROD_CHECKING_KEY);
            boolean force_disable_enqueue_events = config.getBoolean(FORCE_DISABLE_ENQUEUE_EVENTS_KEY);
            boolean dump_suppress_inv_list_when_checking = config.getBoolean(DUMP_SUPPRESS_INV_LIST_WHEN_CHECKING_KEY);
            System.out.println(SYSTEM_DIR_PATH_KEY + ":" + system_dir_path);
            System.out.println(SYSTEM_PACKAGE_PREFIX_KEY + ":" + prefix);
            System.out.println(TEST_CLASS_NAME_REGEX_KEY + ":" + regex);
            System.out.println(JVM_ARGS_FOR_TESTS_KEY + ":" + jvm_args);
            System.out.println(SPECIFIED_TEST_CLASS_LIST_KEY + ":" + Arrays.toString(specifiedClassList));
            System.out.println(EXCLUDED_TEST_METHOD_LIST_KEY + ":" + Arrays.toString(excludedMethodList));
            System.out.println(OP_INTERFACE_CLASS_LIST_KEY + ":" + Arrays.toString(interfaceClassList));
            System.out.println(INSTRUMENT_STATE_FIELDS_KEY + ":" + Arrays.toString(instrument_state_fields));
            System.out.println(INSTRUMENT_CLASS_ALLMETHODS_KEY + ":" + Arrays.toString(instrument_class_allmethods));
            System.out.println(EXCLUDE_CLASS_LIST_KEY + ":" + Arrays.toString(exclude_class_list));
            System.out.println(EXCLUDE_METHOD_LIST_KEY + ":" + Arrays.toString(exclude_method_list));
            System.out.println(TIME_WINDOW_LENGTH_IN_MILLIS_KEY + ":" + time_window_length_in_millis);
            System.out.println(GENTRACE_INSTRUMENT_MODE_KEY + ":" + gentrace_instrument_mode);
            System.out.println(VERIFY_ABORT_AFTER_THREE_TEST_KEY + ":" + verify_abort_after_three_test);
            System.out.println(VERIFY_SURVIVOR_MODE_KEY + ":" + verify_survivor_mode);
            System.out.println(FORCE_INSTRUMENT_NOTHING_KEY + ":" + force_instrument_nothing);
            System.out.println(FORCE_INSTRUMENT_NOTHING_KEY + ":" + force_instrument_nothing);
            System.out.println(FORCE_DISABLE_PROD_CHECKING_KEY + ":" + force_disable_prod_checking);
            System.out.println(FORCE_DISABLE_ENQUEUE_EVENTS_KEY + ":" + force_disable_enqueue_events);
            System.out.println(DUMP_SUPPRESS_INV_LIST_WHEN_CHECKING_KEY + ":" + dump_suppress_inv_list_when_checking);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            System.err.println("Cannot find config file:" + CONFIG_FILE_NAME);
            System.exit(-1);
        }
    }
    public static InstrumentMode getGentraceInstrumentMode()
    {
        String gentrace_instrument_mode = config.getString(GENTRACE_INSTRUMENT_MODE_KEY);
        if(gentrace_instrument_mode.equals("strict-selective"))
            return InstrumentMode.STRICT_SELECTIVE;
        else if(gentrace_instrument_mode.equals("relaxed-selective"))
            return InstrumentMode.RELAXED_SELECTIVE;
        else if(gentrace_instrument_mode.equals("full"))
            return InstrumentMode.FULL;
        else if(gentrace_instrument_mode.equals("specified_selective"))
            return InstrumentMode.SPECIFIED_SELECTIVE;
        else
        {
            System.err.println("ERROR: unrecognized option:" +GENTRACE_INSTRUMENT_MODE_KEY+" "+gentrace_instrument_mode);
            System.exit(-1);
        }

        return null;
    }

    //WARN: this might potentially slow down the perf as it does not cache
    public static ExecuteMode getExecuteMode()
    {
        String executeMode = System.getProperty("ok.invmode");
        if(executeMode ==null)
            return ExecuteMode.OTHERS;

        if(executeMode.equals("dump"))
            return ExecuteMode.GENTRACE;
        else if(executeMode.equals("verify"))
            return ExecuteMode.VERIFY;
        else if(executeMode.equals("verify_baremetal"))
            return ExecuteMode.VERIFY_BAREMETAL;
        else if(executeMode.equals("prod"))
            return ExecuteMode.PROD;
        else
            return ExecuteMode.OTHERS;
    }
}
