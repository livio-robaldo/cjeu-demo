//Copyright 2014 Livio Robaldo
//This file is part of CJEUprocessorDEMO. The demo uses the SDFTagger library, whose source code can be obtained by contacting Livio Robaldo (http://www.liviorobaldo.com)
//CJEUprocessorDEMO is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software 
//Foundation, either version 3 of the License, or (at your option) any later version.
//CJEUprocessorDEMO is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
//PARTICULAR PURPOSE. See the GNU General Public License for more details.
//You should have received a copy of the GNU General Public License along with CJEUprocessorDEMO.  If not, see <http://www.gnu.org/licenses/>.
package C_CJEU_AkomaNtoso.SDFTaggerConfigForPartyClassificationOnKeywords;

import SDFTagger.SDFTaggerConfig;
import SDFTagger.SDFLogger;
import SDFTagger.KBInterface.XMLFilesInterface.XMLFilesManager;
import java.io.File;

public class SDFTaggerConfigForPartyClassificationOnKeywords extends SDFTaggerConfig
{
    public SDFTaggerConfigForPartyClassificationOnKeywords(File logFile)throws Exception
    {
            //Bags
        rootDirectoryBags = new File("./src/C_CJEU_AkomaNtoso/SDFTaggerConfigForPartyClassificationOnKeywords/SDFBags");
        localPathsBags = new String[]
        {
            //"NONE FOR NOW.xml",
        };

            //SDFRule(s)
        rootDirectoryXmlSDFRules = new File("./src/C_CJEU_AkomaNtoso/SDFTaggerConfigForPartyClassificationOnKeywords/SDFRulesXML");
        rootDirectoryCompiledSDFRules = new File("./src/C_CJEU_AkomaNtoso/SDFTaggerConfigForPartyClassificationOnKeywords/SDFRulesCompiled");
        localPathsSDFRules = new String[]
        {
            "companies.xml",
            "institutions.xml"
        };

        KBManager = new XMLFilesManager(rootDirectoryBags, localPathsBags, rootDirectoryXmlSDFRules, rootDirectoryCompiledSDFRules, localPathsSDFRules);
        
            //For the SDFTagger
        SDFNodeConstraintsFactory = new SDFNodeConstraintsForClassificationOnKeywords();
        if(logFile!=null)SDFLogger = new SDFLogger(logFile);
    }
}
