//Copyright 2014 Livio Robaldo
//This file is part of CJEUprocessorDEMO. The demo uses the SDFTagger library, whose source code can be obtained by contacting Livio Robaldo (http://www.liviorobaldo.com)
//CJEUprocessorDEMO is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software 
//Foundation, either version 3 of the License, or (at your option) any later version.
//CJEUprocessorDEMO is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
//PARTICULAR PURPOSE. See the GNU General Public License for more details.
//You should have received a copy of the GNU General Public License along with CJEUprocessorDEMO.  If not, see <http://www.gnu.org/licenses/>.
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
