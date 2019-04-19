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
