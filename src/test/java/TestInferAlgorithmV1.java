import oathkeeper.runtime.FileLayoutManager;
import oathkeeper.runtime.event.OpTriggerEvent;
import oathkeeper.runtime.event.StateUpdateEvent;
import oathkeeper.runtime.invariant.Context;
import oathkeeper.runtime.invariant.Invariant;
import oathkeeper.runtime.template.*;
import oathkeeper.runtime.template_v1.EventHappenBeforeEventTemplateV1;
import oathkeeper.runtime.template_v1.EventImplyEventTemplateV1;
import oathkeeper.runtime.template_v1.EventProtectedByEventTemplateV1;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class TestInferAlgorithmV1 {

    @BeforeClass
    static public void generateAll()
    {
        TestUtils.generateAll();
    }

    @Test
    public void testEventImplyEventTemplate() {
        List<Invariant> invs = TestUtils.loadAndInfer_v1(FileLayoutManager.getPathForTestTraceDir()
                +"/"+"testEventImplyEventTemplate");
        //TestUtils.debugInvariant(invs);
        //actually this dumps multiple same invariants here as it would be go through all xxImplyxxTemplates
        Assert.assertTrue(TestUtils.containsInvariant_v1(invs,
                new Invariant(new EventImplyEventTemplateV1(),
                        new Context(new OpTriggerEvent("1"),new OpTriggerEvent("2")))));
    }

    @Test
    public void testEventHappenBeforeEventTemplate() {
        List<Invariant> invs = TestUtils.loadAndInfer_v1(FileLayoutManager.getPathForTestTraceDir()
                +"/"+"testEventHappenBeforeEventTemplate");
        //TestUtils.debugInvariant(invs);
        Assert.assertTrue(TestUtils.containsInvariant_v1(invs,
                new Invariant(new EventHappenBeforeEventTemplateV1(),
                        new Context(new OpTriggerEvent("1"),new OpTriggerEvent("2")))));
    }

    @Test
    public void testEventProtectedByEventTemplate() {
        List<Invariant> invs = TestUtils.loadAndInfer_v1(FileLayoutManager.getPathForTestTraceDir()
                +"/"+"testEventProtectedByEventTemplate");
        //TestUtils.debugInvariant(invs);
        Assert.assertTrue(TestUtils.containsInvariant_v1(invs,
                new Invariant(new EventProtectedByEventTemplateV1(),
                        new Context(new OpTriggerEvent("2"),new OpTriggerEvent("1")))));
        Assert.assertFalse(TestUtils.containsInvariant_v1(invs,
                new Invariant(new EventProtectedByEventTemplateV1(),
                        new Context(new OpTriggerEvent("1"),new OpTriggerEvent("2")))));
    }

}
