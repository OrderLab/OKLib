package oathkeeper.tool;

import oathkeeper.runtime.InvariantStore;
import oathkeeper.runtime.invariant.Invariant;

import java.util.*;

/*
    Tool class to search a list of keyword in invariant lists
 */
public class InvSearcher {
    static InvariantStore store;
    static String[] keywords ;

    static List<String> extractText(Invariant inv)
    {
        List<String> lst =  new ArrayList<>();
        if(inv.template!=null)
        {
            lst.add(inv.template.getTemplateName());
        }
        if(inv.template_v1!=null)
        {
            lst.add(inv.template_v1.getTemplateName());
        }
        lst.add(inv.context.toString());
        return lst;
    }

    static void search()
    {
        List<Invariant> foundInvs = new ArrayList<>();

        for(Invariant inv: store.invariantList)
        {
            boolean invHasKeyword = true;

            List<String> lst = extractText(inv);
            for(String keyword: keywords)
            {
                boolean textHasKeyword = false;
                for(String text: lst)
                {
                    if(text.contains(keyword))
                    {
                        textHasKeyword = true;
                        break;
                    }
                }

                if (!textHasKeyword) {
                    invHasKeyword = false;
                    break;
                }
            }

            if(invHasKeyword)
            {
                foundInvs.add(inv);
            }
        }

        for(Invariant inv: foundInvs)
        {
            System.out.println(inv.toString());
        }
        System.out.println("Found "+foundInvs.size()+" invs.");
    }

    public static void main(String[] args) {
        if(args.length<2)
        {
            System.err.println("No enough args.");
            System.exit(-1);
        }

        store = InvariantStore.loadFromFile(args[0]);
        keywords = Arrays.copyOfRange(args, 1, args.length);

        System.out.println("Args: ");
        for(String keyword: keywords)
        {
            System.out.println(keyword);
        }

        search();
    }
}
