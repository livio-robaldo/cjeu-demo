//Copyright 2014 Livio Robaldo
//This file is part of CJEUprocessorDEMO. The demo uses the SDFTagger library, whose source code can be obtained by contacting Livio Robaldo (http://www.liviorobaldo.com)
//CJEUprocessorDEMO is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software 
//Foundation, either version 3 of the License, or (at your option) any later version.
//CJEUprocessorDEMO is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
//PARTICULAR PURPOSE. See the GNU General Public License for more details.
//You should have received a copy of the GNU General Public License along with CJEUprocessorDEMO.  If not, see <http://www.gnu.org/licenses/>.

package C_CJEU_AkomaNtoso.SDFTaggerConfigForStructuralTagging;

import SDFTagger.SDFTaggerConfig;
import SDFTagger.SDFLogger;
import SDFTagger.KBInterface.XMLFilesInterface.XMLFilesManager;
import java.io.File;

/**
This class contains the config of an SDFTagger to identify the several Akoma Ntoso sections of a case law on the basis of the titles, the text in bold, etc.
This rules also identify Index, a new POS for SDFHead, which contain an optional feature (value), with the value of the index.
/**/
public class SDFTaggerConfigForStructuralTagging extends SDFTaggerConfig
{
    public SDFTaggerConfigForStructuralTagging(File logFile)throws Exception
    {
            //Bags
        rootDirectoryBags = new File("./src/C_CJEU_AkomaNtoso/SDFTaggerConfigForStructuralTagging/SDFBags");
        localPathsBags = new String[]
        {
            "LegalTextStructItems.xml",
            "Letters.xml",
            "Months.xml",
            "RomanNumbers.xml"
        };

            //SDFRule(s)
        rootDirectoryXmlSDFRules = new File("./src/C_CJEU_AkomaNtoso/SDFTaggerConfigForStructuralTagging/SDFRulesXML");
        rootDirectoryCompiledSDFRules = new File("./src/C_CJEU_AkomaNtoso/SDFTaggerConfigForStructuralTagging/SDFRulesCompiled");
        localPathsSDFRules = new String[]
        {
            "indexes.xml",
            "titles.xml",
            "docType.xml",
            "parties.xml",

            "header.xml",
            "judgmentBody.xml",
                "introduction.xml",
                "background.xml",
                "motivation.xml",
                "decision.xml",
            "conclusions.xml"
        };

        KBManager = new XMLFilesManager(rootDirectoryBags, localPathsBags, rootDirectoryXmlSDFRules, rootDirectoryCompiledSDFRules, localPathsSDFRules);
        
            //We need optional features for, for instance, asserting conditions on the different <font>(s)
        SDFNodeConstraintsFactory = new SDFNodeConstraintsForStructuralTagging();
        
            //Log file        
        SDFLogger = new SDFLogger(logFile);
    }
}
