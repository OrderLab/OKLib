package oathkeeper.engine;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*  Main class for pre-processing phase: instrument system classes so we can track state changes
    this step cannot be done dynamically since system classes are loaded before all others

    But this class is now deprecated since instrumenting system class may cause many unwanted side effects
 */
@Deprecated
public class StaticInstrumentEngine {

    static String TRACKING_SWITCH_KEY = "StateAccessTracking";

    static Map<String, List<String>> getInstrumentedClassesForState() {
        Map<String, List<String>> instrumentedClasses = new HashMap<>();
        instrumentedClasses.put("java.util.ArrayList", Arrays.asList("add","remove"));

        return instrumentedClasses;
    }

    static void modifyStateAccess() {

        Map<String, List<String>> map = getInstrumentedClassesForState();

        ClassPool pool = ClassPool.getDefault();

        int succMethodCounter = 0;
        int failClassCounter = 0;
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String cName = entry.getKey();

            int localCounter = 0;
            try {
                CtClass cc = pool.get(cName);

                for (String mName : entry.getValue()) {
                    CtMethod m = cc.getDeclaredMethod(mName);
                    m.insertBefore(
                            "{if(System.getProperty(\""+TRACKING_SWITCH_KEY+"\")!=null"
                                    + "&&System.getProperty(\""+TRACKING_SWITCH_KEY+"\").equals(\"on\"))"
                                    + "{ok.runtime.EventTracer.registerStateEvent(\""
                                    + m.getLongName() + "\", size());}}");
                    //System.out.println("inject for "+m.getLongName());
                    localCounter++;
                }
                cc.writeFile(".");
            } catch (Exception ex) {
                ex.printStackTrace();
                failClassCounter++;

                continue;
            }
            succMethodCounter += localCounter;
            System.out.println("inject for " + cName);
        }
        System.out.println("succMethodCounter" + succMethodCounter + " failClassCounter" + failClassCounter);
    }

    public static void trackSwitchOn() {
        //load ok.runtime.EventTracer first
        //EventTracer dummyEventTracer = new EventTracer();
        //dummyEventTracer.forceLoad();

        System.setProperty(TRACKING_SWITCH_KEY,"on");
    }

    public static void trackSwitchOff() {
        System.setProperty(TRACKING_SWITCH_KEY,"off");
    }

    public static void main(String[] args) {
        modifyStateAccess();
    }
}
