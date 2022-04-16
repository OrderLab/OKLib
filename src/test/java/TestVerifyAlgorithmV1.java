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

public class TestVerifyAlgorithmV1 {

    @BeforeClass
    static public void generateAll()
    {
        TestUtils.generateAll();
    }

    @Test
    public void testEventImplyEventTemplate() {
        String prefix = FileLayoutManager.getPathForTestTraceDir()
                +"/"+"testEventImplyEventTemplate";
        Invariant inv = new Invariant(new EventImplyEventTemplateV1(),
                new Context(new OpTriggerEvent("1"),new OpTriggerEvent("2")));
        Assert.assertEquals(Invariant.InvState.PASS, TestUtils.loadAndVerifyPatched_v1(prefix, inv));
        Assert.assertEquals(Invariant.InvState.FAIL, TestUtils.loadAndVerifyUnpatched_v1(prefix, inv));
    }

    @Test
    public void testEventHappenBeforeEventTemplate() {
        String prefix = FileLayoutManager.getPathForTestTraceDir()
                +"/"+"testEventHappenBeforeEventTemplate";
        Invariant inv = new Invariant(new EventHappenBeforeEventTemplateV1(),
                new Context(new OpTriggerEvent("1"),new OpTriggerEvent("2")));
        Assert.assertEquals(Invariant.InvState.PASS, TestUtils.loadAndVerifyPatched_v1(prefix, inv));
        Assert.assertEquals(Invariant.InvState.FAIL, TestUtils.loadAndVerifyUnpatched_v1(prefix, inv));
    }

    @Test
    public void testEventHappenBeforeEventTemplate2() {
        String prefix = FileLayoutManager.getPathForTestTraceDir()
                +"/"+"testEventHappenBeforeEventTemplate2";
        Invariant inv = new Invariant(new EventHappenBeforeEventTemplateV1(),
                new Context(new OpTriggerEvent("1"),new OpTriggerEvent("2")));
        Invariant inv2 = new Invariant(new EventProtectedByEventTemplateV1(),
                new Context(new OpTriggerEvent("2"),new OpTriggerEvent("1")));
        Assert.assertEquals(Invariant.InvState.INACTIVE, TestUtils.loadAndVerifyPatched_v1(prefix, inv));
        Assert.assertEquals(Invariant.InvState.FAIL, TestUtils.loadAndVerifyPatched_v1(prefix, inv2));
        Assert.assertEquals(Invariant.InvState.FAIL, TestUtils.loadAndVerifyUnpatched_v1(prefix, inv));
    }

    @Test
    public void testEventProtectedByEventTemplate() {
        String prefix = FileLayoutManager.getPathForTestTraceDir()
                +"/"+"testEventProtectedByEventTemplate";
        Invariant inv = new Invariant(new EventProtectedByEventTemplateV1(),
                new Context(new OpTriggerEvent("2"),new OpTriggerEvent("1")));
        Assert.assertEquals(Invariant.InvState.PASS, TestUtils.loadAndVerifyPatched_v1(prefix, inv));
        Assert.assertEquals(Invariant.InvState.FAIL, TestUtils.loadAndVerifyUnpatched_v1(prefix, inv));
    }

}
