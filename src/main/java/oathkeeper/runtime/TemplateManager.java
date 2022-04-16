package oathkeeper.runtime;

import oathkeeper.runtime.template.*;
import oathkeeper.runtime.template_v1.EventHappenBeforeEventTemplateV1;
import oathkeeper.runtime.template_v1.EventImplyEventTemplateV1;
import oathkeeper.runtime.template_v1.EventProtectedByEventTemplateV1;
import oathkeeper.runtime.template_v1.TemplateV1;

import java.util.ArrayList;
import java.util.List;

public class TemplateManager {

    public static List<Template> templatePool = new ArrayList<Template>() {{
        //unary
        //add(new OpPeriodicTemplate());

        //binary-op
        add(new EventHappenBeforeEventTemplate());
        add(new EventImplyEventTemplate());
        add(new EventProtectedByEventTemplate());

        //binary-timing
        add(new OpAddTimeInvokeOpTemplate());
        //add(new OpAddTimeDenyOpTemplate());

        //binary-deny
        add(new StateEqualsDenyOpTemplate());

        //triple
        add(new AfterOpAtomicStateUpdateTemplate());

        //omitted, such templates should not be directly inferred from patched traces, instead, their opposite version should
        //be inferred in unpatched traces and invert
        //add(new OpMutualExclusiveTemplate());
    }};

    public static List<TemplateV1> templatePool_v1 = new ArrayList<TemplateV1>() {{
        //binary-op
        add(new EventHappenBeforeEventTemplateV1());
        add(new EventImplyEventTemplateV1());
        add(new EventProtectedByEventTemplateV1());
    }};

    //some templates cannot be inferred from patched version, we should infer from unpatched version and invert
    public static List<Template> invertableTemplatePool = new ArrayList<Template>() {{
        //binary-op
        //for OpMutualExclusiveTemplate
        //add(new OpAtomicTemplate());

        //binary-timing
    }};

}
