import java.util.ArrayList;
import java.util.List;

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

public class TestInferAlgorithm {

    @BeforeClass
    static public void generateAll()
    {
        TestUtils.generateAll();
    }

    //@Test
//    @Deprecated
//    public void testOpPeriodicTemplate() {
//        Assert.assertTrue(containsInvariantType(loadAndInfer("./test/traces/testOpPeriodicTemplate"), new OpPeriodicTemplate().getTemplateName()));
//    }
//
//    //@Test
//    @Deprecated
//    public void testOpAddTimeDenyOpTemplate() {
//        Assert.assertTrue(containsInvariantType(loadAndInfer("./test/traces/testOpAddTimeDenyOpTemplate"), new OpAddTimeDenyOpTemplate().getTemplateName()));
//    }

    @Test
    public void testEventImplyEventTemplate() {
        List<Invariant> invs = TestUtils.loadAndInfer(FileLayoutManager.getPathForTestTraceDir()
                +"/"+"testEventImplyEventTemplate");
        //TestUtils.debugInvariant(invs);
        //actually this dumps multiple same invariants here as it would be go through all xxImplyxxTemplates
        Assert.assertTrue(TestUtils.containsInvariant(invs,
                new Invariant(new OpImplyOpTemplate(),
                        new Context(new OpTriggerEvent("1"),new OpTriggerEvent("2")))));
    }

    @Test
    public void testEventHappenBeforeEventTemplate() {
        List<Invariant> invs = TestUtils.loadAndInfer(FileLayoutManager.getPathForTestTraceDir()
                +"/"+"testEventHappenBeforeEventTemplate");
        //TestUtils.debugInvariant(invs);
        Assert.assertTrue(TestUtils.containsInvariant(invs,
                new Invariant(new OpHappenBeforeOpTemplate(),
                        new Context(new OpTriggerEvent("1"),new OpTriggerEvent("2")))));
    }

    @Test
    public void testEventProtectedByEventTemplate() {
        List<Invariant> invs = TestUtils.loadAndInfer(FileLayoutManager.getPathForTestTraceDir()
                +"/"+"testEventProtectedByEventTemplate");
        //TestUtils.debugInvariant(invs);
        Assert.assertTrue(TestUtils.containsInvariant(invs,
                new Invariant(new OpProtectedByOpTemplate(),
                        new Context(new OpTriggerEvent("2"),new OpTriggerEvent("1")))));
        Assert.assertFalse(TestUtils.containsInvariant(invs,
                new Invariant(new OpProtectedByOpTemplate(),
                        new Context(new OpTriggerEvent("1"),new OpTriggerEvent("2")))));
    }

    @Test
    public void testOpAddTimeInvokeOpTemplate() {
        List<Invariant> invs = TestUtils.loadAndInfer(FileLayoutManager.getPathForTestTraceDir()
                +"/"+"testOpAddTimeInvokeOpTemplate");
        //TestUtils.debugInvariant(invs);
        Context context =  new Context(new OpTriggerEvent("1"),new OpTriggerEvent("2"));
        context.timeInterval=1000;
        Invariant inv = new Invariant(new OpAddTimeInvokeOpTemplate(),context);
        Assert.assertTrue(TestUtils.containsInvariant(invs, inv));
    }

    @Test
    public void testStateEqualsDenyOpTemplate() {
        List<Invariant> invs = TestUtils.loadAndInfer(FileLayoutManager.getPathForTestTraceDir()
                +"/"+"testStateEqualsDenyOpTemplate");
        //TestUtils.debugInvariant(invs);
        Context context =  new Context(new StateUpdateEvent("1","nouse",0),
                new OpTriggerEvent("2"));
        context.constant = 5;
        Invariant inv = new Invariant(new StateEqualsDenyOpTemplate(), context);
        Assert.assertTrue(TestUtils.containsInvariant(invs, inv));
    }

    @Test
    public void testAfterOpAtomicStateUpdateTemplate() {
        List<Invariant> invs = TestUtils.loadAndInfer(FileLayoutManager.getPathForTestTraceDir()
                +"/"+"testAfterOpAtomicStateUpdateTemplate");
        //TestUtils.debugInvariant(invs);
        Invariant inv = new Invariant(new AfterOpAtomicStateUpdateTemplate(),
                new Context(new OpTriggerEvent("1"),
                        new StateUpdateEvent("2","nouse",0),
                        new StateUpdateEvent("3","nouse",0)));
        Assert.assertTrue(TestUtils.containsInvariant(invs, inv));
    }
}
