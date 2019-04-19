package OntologyManager.SDFBuilder;

import SDFTagger.SDFItems.SDFHead;
import SDFTagger.SDFItems.SDFDependencyTree;
import org.jdom2.*;
import java.util.*;

public class CompaniesSDFRulesBuilder extends SDFRulesBuilderUtilities
{
    protected static int priorityStrict = 1000;
           
    public static ArrayList<Element> createSDFRulesForCompaniesFromSDFDependencyTrees(ArrayList<SDFDependencyTree> SDFDependencyTrees, String newIndividualName)
    {
        ArrayList<Element> ret = new ArrayList<Element>();
        
        ArrayList<SDFHead> SDFHeads = getAllSDFHeads(SDFDependencyTrees);
        
            //(1) We create the strict rule (from left to right, only on Form(s), maxDistance=1)
        Element strictSDFRule = createSDFRuleOnSDFHeadsLeft2Right(SDFHeads, priorityStrict, 1, newIndividualName, new String[]{"Form"});
        if(strictSDFRule!=null)ret.add(strictSDFRule);
        
        return ret;
    }
    
    public static ArrayList<Element> createSDFRulesForCompaniesFromSDFHeads(ArrayList<SDFHead> SDFHeads, String newIndividualName) 
    {
        ArrayList<Element> ret = new ArrayList<Element>();
        
            //(1) We create the strict rule (from left to right, only on Form(s), maxDistance=1)
        Element strictSDFRule = createSDFRuleOnSDFHeadsLeft2Right(SDFHeads, priorityStrict, 1, newIndividualName, new String[]{"Form"});
        if(strictSDFRule!=null)ret.add(strictSDFRule);
        
        return ret;
    }
}
