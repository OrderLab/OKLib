package oathkeeper.engine.tester;

import oathkeeper.runtime.EventTracer;
import oathkeeper.runtime.FileLayoutManager;
import oathkeeper.runtime.OKHelper;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class TestListener extends RunListener {
    TestEngine testEngine;

    TestListener(TestEngine engine)
    {
        this.testEngine = engine;
    }

    public void testStarted(Description description) {
        System.out.println("Test " + description.getClassName() + ":" + description.getMethodName() + " started");
        String testName = description.getClassName() + "@" + description.getMethodName();
        if(System.getProperty("ok.invmode").equals("dump") && !testName.contains(System.getProperty("ok.test_trace_prefix")))
            throw new RuntimeException("Skip unintended tests.");
        OKHelper.getInstance().setLastTestName(testName);

        EventTracer.instance = new EventTracer();
        EventTracer.instance.tracerName = testName;
        if(System.getProperty("ok.patchstate").equals("patched"))
            EventTracer.instance.mode = EventTracer.TracerMode.TEST_PATCHED;
        else if(System.getProperty("ok.patchstate").equals("unpatched"))
            EventTracer.instance.mode = EventTracer.TracerMode.TEST_UNPATCHED;
        else{
            throw new RuntimeException("PATCHED state not set!");
        }

        failThrowable = null;

    }

    private static final Description FAILED = Description.createTestDescription("failed", "failed");
    private static Throwable failThrowable = null;

    public void testFailure(Failure failure) throws Exception {
        failure.getDescription().addChild(FAILED);
        failThrowable = failure.getException();
    }

    public void testFinished(Description description) {

        String testName = description.getClassName() + "@" + description.getMethodName();
        System.out.println("Test " + description.getClassName() + ":" + description.getMethodName() + " ended");
        if(System.getProperty("ok.invmode").equals("dump") && !testName.contains(System.getProperty("ok.test_trace_prefix")))
            throw new RuntimeException("Skip unintended tests.");

        if(System.getProperty("ok.invmode").equals("dump"))
        {
            String testTracePrefix = System.getProperty("ok.test_trace_prefix");

            //check the output, ideally buggy version
            // should: fail with assertion or customized exception
            // should not: pass, fail with  java.lang.VerifyError, java.lang.ClassNotFoundException, java.lang.NoSuchMethodError
            //              java.lang.IllegalAccessError and etc
            // patched version
            // should always pass
            if(testName.startsWith(testTracePrefix))
            {
                boolean ifBuggyVersion = System.getProperty("ok.patchstate").equals("unpatched");
                if(ifBuggyVersion)
                {
                    if (!description.getChildren().contains(FAILED))
                    {
                        //this should not happen, buggy version should fail not pass
                        testEngine.criticalErrors.add(testName+" should not pass!");
                    }
                    else {
                        String throwableName = failThrowable.getClass().getName();
                        if(!throwableName.equals("java.lang.AssertionError"))
                        {
                            if(throwableName.startsWith("java.lang."))
                            {
                                //should not return general exceptions
                                testEngine.criticalErrors.add(testName+" should not throw "+throwableName);
                            }
                        }
                    }
                }
                else
                {
                    if (description.getChildren().contains(FAILED))
                    {
                        testEngine.criticalErrors.add(testName+" should pass!");
                    }
                }
            }

            EventTracer.dumpToFile(FileLayoutManager.getPathForTracesOutputDir(), EventTracer.instance.getStatefulName(),
                    EventTracer.instance);
        }
        else if(System.getProperty("ok.invmode").equals("verify"))
        {
            if (description.getChildren().contains(FAILED))
            {
                //this test failed in the verifying phase, so we need to count that as well
                testEngine.checker.markTestFailed(testName);
            }
            else
                testEngine.checker.verifyFromVerifyPhase(testName);

            //need to dump survivors
            testEngine.checker.dumpSurvivors();
        }
    }
}