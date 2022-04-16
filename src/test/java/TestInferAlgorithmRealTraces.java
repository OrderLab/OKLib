import oathkeeper.engine.InferEngine;
import oathkeeper.runtime.FileLayoutManager;
import oathkeeper.runtime.event.OpTriggerEvent;
import oathkeeper.runtime.event.StateUpdateEvent;
import oathkeeper.runtime.invariant.Context;
import oathkeeper.runtime.invariant.Invariant;
import oathkeeper.runtime.template.AfterOpAtomicStateUpdateTemplate;
import oathkeeper.runtime.template.EventImplyEventTemplate;
import org.junit.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

public class TestInferAlgorithmRealTraces {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @Before
    public void generateAll()
    {
        System.setProperty("ok.ok_root_abs_path", System.getProperty("user.dir"));

        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));

        InferEngine.ENABLE_CHECK_INFERRED_INVS_PASS_PATCHED_TRACES = true;
        InferEngine.ENABLE_CHECK_REDUNDANT_INVARIANTS = true;
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void testInferredInvsCannotPassPatchedTracesBug() {
        //from HBASE-4797
        List<Invariant> invs = TestUtils.loadAndInfer(FileLayoutManager.getPathForTestRealTracesDir()
                      +"/"+"org.apache.hadoop.hbase.regionserver.TestHRegion@testSkipRecoveredEditsReplayAllIgnored");

        Assert.assertFalse("output should not give errors when checking result sanity",outContent.toString().contains("[ERROR]"));
        Assert.assertFalse("output should not give errors when checking result sanity",errContent.toString().contains("[ERROR]"));

        List<Invariant> invs_v1 = TestUtils.loadAndInfer_v1(FileLayoutManager.getPathForTestRealTracesDir()
                +"/"+"org.apache.hadoop.hbase.regionserver.TestHRegion@testSkipRecoveredEditsReplayAllIgnored");
        int implySize=0, protectedSize=0, happenbeforeSize = 0;
        int implySize_v1=0, protectedSize_v1=0, happenbeforeSize_v1 = 0;
        for(Invariant inv:invs)
        {
            if(inv.template.getTemplateName().contains("Imply"))
                implySize++;
            if(inv.template.getTemplateName().contains("ProtectedBy"))
                protectedSize++;
            if(inv.template.getTemplateName().contains("HappenBefore"))
                happenbeforeSize++;
        }
        for(Invariant inv:invs_v1)
        {
            if(inv.template_v1.getTemplateName().contains("Imply"))
                implySize_v1++;
            if(inv.template_v1.getTemplateName().contains("ProtectedBy"))
                protectedSize_v1++;
            if(inv.template_v1.getTemplateName().contains("HappenBefore"))
                happenbeforeSize_v1++;
        }
        Assert.assertEquals("v1 and v2 result should size match",implySize,implySize_v1);
        Assert.assertEquals("v1 and v2 result should size match",protectedSize,protectedSize_v1);
        Assert.assertEquals("v1 and v2 result should size match",happenbeforeSize,happenbeforeSize_v1);
    }

}
