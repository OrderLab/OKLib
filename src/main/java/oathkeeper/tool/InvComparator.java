package oathkeeper.tool;

import oathkeeper.runtime.InvariantStore;
import oathkeeper.runtime.invariant.Invariant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
    Tool class to compare two files of invariants (diff)
    See usage in scripts.
 */
public class InvComparator {
    static InvariantStore store1;
    static InvariantStore store2;

    static void findCommon()
    {
        Map<String, List<Invariant>> commonInvs = new HashMap<>();
        for(Invariant inv:store1.invariantList)
        {
            if(store2.invariantList.contains(inv))
            {
                if(commonInvs.containsKey(inv.template.getTemplateName()))
                {
                    commonInvs.get(inv.template.getTemplateName()).add(inv);
                }
                else
                {
                    commonInvs.put(inv.template.getTemplateName(), new ArrayList<Invariant>(){{add(inv);}});
                }
                System.out.println(inv.toStringWithStats());
            }
        }

        for(String type:commonInvs.keySet())
        {
            System.out.println(commonInvs.get(type).size()+" invs in common as "+type);
        }
    }

    public static void main(String[] args) {
        if(args.length<2)
        {
            System.err.println("No enough args.");
            System.exit(-1);
        }

        store1 = InvariantStore.loadFromFile(args[0]);
        store2 = InvariantStore.loadFromFile(args[1]);

        findCommon();
    }
}
