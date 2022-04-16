import oathkeeper.engine.InferEngine;
import oathkeeper.runtime.EventTracer;
import oathkeeper.runtime.FileLayoutManager;
import oathkeeper.runtime.event.OpTriggerEvent;
import oathkeeper.runtime.event.StateUpdateEvent;
import oathkeeper.runtime.invariant.Context;
import oathkeeper.runtime.invariant.Invariant;
import oathkeeper.runtime.template.*;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import traces.*;

import java.util.List;

public class TestVerifyAlgorithm {

    @BeforeClass
    static public void generateAll()
    {
        TestUtils.generateAll();
    }

    @Test
    public void testEventImplyEventTemplate() {
        String prefix = FileLayoutManager.getPathForTestTraceDir()
                +"/"+"testEventImplyEventTemplate";
        Invariant inv = new Invariant(new EventImplyEventTemplate(),
                new Context(new OpTriggerEvent("1"),new OpTriggerEvent("2")));
        Assert.assertEquals(Invariant.InvState.PASS, TestUtils.loadAndVerifyPatched(prefix, inv));
        Assert.assertEquals(Invariant.InvState.FAIL, TestUtils.loadAndVerifyUnpatched(prefix, inv));
    }

    @Test
    public void testEventHappenBeforeEventTemplate() {
        String prefix = FileLayoutManager.getPathForTestTraceDir()
                +"/"+"testEventHappenBeforeEventTemplate";
        Invariant inv = new Invariant(new EventHappenBeforeEventTemplate(),
                new Context(new OpTriggerEvent("1"),new OpTriggerEvent("2")));
        Assert.assertEquals(Invariant.InvState.PASS, TestUtils.loadAndVerifyPatched(prefix, inv));
        Assert.assertEquals(Invariant.InvState.FAIL, TestUtils.loadAndVerifyUnpatched(prefix, inv));
    }

    @Test
    public void testEventHappenBeforeEventTemplate2() {
        String prefix = FileLayoutManager.getPathForTestTraceDir()
                +"/"+"testEventHappenBeforeEventTemplate2";
        Invariant inv = new Invariant(new EventHappenBeforeEventTemplate(),
                new Context(new OpTriggerEvent("1"),new OpTriggerEvent("2")));
        Invariant inv2 = new Invariant(new EventProtectedByEventTemplate(),
                new Context(new OpTriggerEvent("2"),new OpTriggerEvent("1")));
        Assert.assertEquals(Invariant.InvState.INACTIVE, TestUtils.loadAndVerifyPatched(prefix, inv));
        Assert.assertEquals(Invariant.InvState.FAIL, TestUtils.loadAndVerifyPatched(prefix, inv2));
        Assert.assertEquals(Invariant.InvState.FAIL, TestUtils.loadAndVerifyUnpatched(prefix, inv));
    }


    @Test
    public void testEventProtectedByEventTemplate() {
        String prefix = FileLayoutManager.getPathForTestTraceDir()
                +"/"+"testEventProtectedByEventTemplate";
        Invariant inv = new Invariant(new EventProtectedByEventTemplate(),
                new Context(new OpTriggerEvent("2"),new OpTriggerEvent("1")));
        Assert.assertEquals(Invariant.InvState.PASS, TestUtils.loadAndVerifyPatched(prefix, inv));
        Assert.assertEquals(Invariant.InvState.FAIL, TestUtils.loadAndVerifyUnpatched(prefix, inv));
    }

    @Test
    public void testOpAddTimeInvokeOpTemplate() {
        String prefix = FileLayoutManager.getPathForTestTraceDir()
                +"/"+"testOpAddTimeInvokeOpTemplate";
        Context context =  new Context(new OpTriggerEvent("1"),new OpTriggerEvent("2"));
        context.timeInterval=1000;
        Invariant inv = new Invariant(new OpAddTimeInvokeOpTemplate(),context);
        Assert.assertEquals(Invariant.InvState.PASS, TestUtils.loadAndVerifyPatched(prefix, inv));
        Assert.assertEquals(Invariant.InvState.FAIL, TestUtils.loadAndVerifyUnpatched(prefix, inv));
    }

    @Test
    public void testStateEqualsDenyOpTemplate() {
        String prefix = FileLayoutManager.getPathForTestTraceDir()
                +"/"+"testStateEqualsDenyOpTemplate";
        Context context =  new Context(new StateUpdateEvent("1","nouse",0),
                new OpTriggerEvent("2"));
        context.constant = 5;
        Invariant inv = new Invariant(new StateEqualsDenyOpTemplate(), context);
        Assert.assertEquals(Invariant.InvState.PASS, TestUtils.loadAndVerifyPatched(prefix, inv));
        Assert.assertEquals(Invariant.InvState.FAIL, TestUtils.loadAndVerifyUnpatched(prefix, inv));
    }

    @Test
    public void testAfterOpAtomicStateUpdateTemplate() {
        String prefix = FileLayoutManager.getPathForTestTraceDir()
                +"/"+"testAfterOpAtomicStateUpdateTemplate";
        Invariant inv = new Invariant(new AfterOpAtomicStateUpdateTemplate(),
                new Context(new OpTriggerEvent("1"),
                        new StateUpdateEvent("2","nouse",0),
                        new StateUpdateEvent("3","nouse",0)));
        Assert.assertEquals(Invariant.InvState.PASS, TestUtils.loadAndVerifyPatched(prefix, inv));
        Assert.assertEquals(Invariant.InvState.FAIL, TestUtils.loadAndVerifyUnpatched(prefix, inv));
    }
}
