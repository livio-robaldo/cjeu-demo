//Copyright 2014 Livio Robaldo
//This file is part of CJEUprocessorDEMO. The demo uses the SDFTagger library, whose source code can be obtained by contacting Livio Robaldo (http://www.liviorobaldo.com)
//CJEUprocessorDEMO is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software 
//Foundation, either version 3 of the License, or (at your option) any later version.
//CJEUprocessorDEMO is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
//PARTICULAR PURPOSE. See the GNU General Public License for more details.
//You should have received a copy of the GNU General Public License along with CJEUprocessorDEMO.  If not, see <http://www.gnu.org/licenses/>.

package C_CJEU_AkomaNtoso;

import C_CJEU_AkomaNtoso.SDFTaggerConfigForStructuralTagging.SDFTaggerConfigForStructuralTagging;
import SDFTagger.SDFItems.SDFHead;
import SDFTagger.SDFItems.SDFDependencyTree;
import SDFTagger.SDFTagger;
import SDFTagger.SDFItems.SDFTag;
import C_CJEU_AkomaNtoso.SDFTaggerConfigForEntityLinking.SDFTaggerConfigForEntityLinking;
import C_CJEU_AkomaNtoso.SDFTaggerConfigForPartyClassificationOnKeywords.SDFTaggerConfigForPartyClassificationOnKeywords;
import java.io.*;
import java.util.*;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.*;
    //SDFTagger
import XMLUtilities.*;
    //SDFTagger configurations
import OntologyManager.OntologyManager;

/*
We build the <judgment> in the following way:
(1) We use SDFTagger to tags the words; some of the tags identify the sections (<header>, <judgmentBody>, <motivation>, <decision>, etc.) while the others identify
    some "low-level" Element(s) such as <tblock>(s), <party>(s), etc. as well as *indexes* needed to build <blockIndex>(s) or <paragraph>(s)
(2) We separate the tags into the one identifying the sections from the other ones. We identify the sections with the first ones.
(3) We use the other tags to build *A FIRST DRAFT* of the Element(s) within the sections. 
(4) We adjust the *FIRST DRAFT* of the Element to comply with AkomaNtoso. For instance, <tblock>(s) always require a <p> between his children. Therefore, we 
    transform every <tblock>bla bla bla</tblock> into <tblock><p>bla bla bla</p></tblock>. Furthermore, it is possible that a <tblock> has been inserted as last 
    element of the last <item> of a <blockList> or a <paragraph>; in such a case, we remove it from there: it must be inserted AFTER the <blockList> or <paragraph>.
*/


public class buildAkomaNtosoCJEUsDEBUG 
{
    private static boolean resetAll = true;
    
    private static String fileName = "ECLI_EU_C_2017_1000.xml";
    private static File inputFolder = new File("./CORPUS/1 - PARSED INPUT");
    private static File outputFolder = new File("./CORPUS/2 - AKOMA NTOSO");
    private static String ontologyFilePath = "./CORPUS/OWL_Ontology/CJEUontology.owl";
    
    protected static Namespace xmlns = Namespace.getNamespace("http://docs.oasis-open.org/legaldocml/ns/akn/3.0/CSD11");
    protected static Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    protected static Namespace xsd = Namespace.getNamespace("xsd", "http://www.w3.org/2001/XMLSchema");
    
    private static File logFileForStructuralTagging = new File("./CORPUS/LogFiles/SDFDebugForStructuralTagging.xml");
    private static File logFileForPartyClassificationOnKeywords = new File("./CORPUS/LogFiles/SDFDebugForPartyClassificationOnKeywords.xml");
    
    public static void main(String[] args)
    {
        try 
        {
                //We load the SDFTagger(s) for: (1) structural tagging, (2) classification of the parties, (3) entity linking.
                //Note that we have a ArrayList<SDFTagger> for (3). The reason is that now we load the XML SDFRule(s) that we have in 
                //C_CJEU_AkomaNtoso/SDFTaggerConfigForEntityLinking/SDFRulesXML, because we look if the party is perhaps already mentioned
                //in a previous case law. If so, we associate the party in the new case law with the same individual. If not, we'll create new
                //individuals for the document, but then we'll create a new SDFTagger (that we'll add in the ArrayList) with which we'll 
                //process the next case law. See below the addSDFTagsForParties method, for further details.
                       
                //NB. Note that resetAll is passed as second argument of SDFTaggerConfigForEntityLinking. If resetAll=true, all SDFRule(s)
                //generated in past executions of buildAkomaNtosoCJEUs will be deleted.
            SDFTagger SDFTaggerForStructuralTagging = new SDFTagger(new SDFTaggerConfigForStructuralTagging(logFileForStructuralTagging));
            SDFTagger SDFTaggerForPartyClassificationOnKeywords = new SDFTagger(new SDFTaggerConfigForPartyClassificationOnKeywords(logFileForPartyClassificationOnKeywords));
            SDFTagger SDFTaggerForEntityLinking = new SDFTagger(new SDFTaggerConfigForEntityLinking(null, resetAll));
            ArrayList<SDFTagger> SDFTaggersForEntityLinking = new ArrayList<SDFTagger>();
            SDFTaggersForEntityLinking.add(SDFTaggerForEntityLinking);
            
                //We load the ontology (and we remove all individuals, if resetAll=true)
            OntologyManager.loadOntology(ontologyFilePath, resetAll);
            
                //If resetAll (the second parameter of the method just above) is true, we also delete all files from the output directory. 
                //And we delete all SDFRule with the sepcial method 
                //Otherwise, we leave the files that have been already generated.
            if(resetAll==true)
            {
                ArrayList<File> tempFiles = new ArrayList<File>();
                for(int i=0;i<outputFolder.listFiles().length;i++)tempFiles.add(outputFolder.listFiles()[i]);
                for(int i=0;i<tempFiles.size();i++)if(tempFiles.get(i).isDirectory()==false)while(tempFiles.get(i).exists())tempFiles.get(i).delete();
            }
            
            File inputFile = new File(inputFolder.getAbsolutePath()+"/"+fileName);
            File outputFile = new File(outputFolder.getAbsolutePath()+"/"+fileName);
            
            System.out.println("Building Akoma Ntoso document of: "+inputFile.getAbsolutePath());
            Document inputDoc = (Document) new SAXBuilder().build(inputFile);

            Element texts = inputDoc.getRootElement();
            Element XMLDependencyTrees = null;
            for(int j=0;(j<texts.getContent().size())&&(XMLDependencyTrees==null);j++)
                if((texts.getContent().get(j)instanceof Element)&&(((Element)texts.getContent().get(j)).getName().compareToIgnoreCase("DependencyTrees")==0))
                    XMLDependencyTrees=(Element)texts.getContent().get(j);
            if(XMLDependencyTrees==null)throw new Exception("Cannot find the <DependencyTrees> Element");

                //(1) We use SDFTagger to tags the words; some of the tags identify the sections (<header>, <judgmentBody>, <motivation>,
                //<decision>, etc.) while the others identify some "low-level" Element(s) such as <tblock>(s), <party>(s), etc. as well as 
                //*indexes* needed to build <blockIndex>(s) or <paragraph>(s).
            ArrayList<SDFDependencyTree> SDFDependencyTrees = XML2SDFDependencyTrees.XML2SDFDependencyTrees(XMLDependencyTrees);
            ArrayList<SDFTag> SDFTags = SDFTaggerForStructuralTagging.tagTrees(SDFDependencyTrees);

                //(2) We extract and separately deal with the tags that identify the sections. We continue to process the others below.
                //We create the Hashtable SDFHeads2SectionsSDFTags via the utilities. Note that we do not use "removeIntersectingSequencesOfConsecutiveSDFHeads"!
                //In other words, we do not remove the low-priority tags on the basis of the high priority tags. We don't remove the intersecting low-priority 
                //ones with the titles of the sections because, for instance, we could have an SDFHead that ends both the judgmentBody and the decision.
            ArrayList<SDFTag> sectionsSDFTags = buildAkomaNtosoCJEUs.extractSectionsSDFTags(SDFTags);
            Hashtable<SDFTag, ArrayList<SDFHead>> tags2sequencesSectionsSDFTags = Utilities.createSequencesOfConsecutiveSDFHeads(sectionsSDFTags, SDFDependencyTrees);
            Hashtable<SDFHead, SDFTag> SDFHeads2SectionsSDFTags = Utilities.convertHashtable(tags2sequencesSectionsSDFTags);

                //SDFHeads2SectionsSDFTags will be used later. 
                //Now we continue to process the remaining SDFTags; we must obtain a Hashtable<SDFHead, SDFTag> also from them.
                //We recall this method that continue to fill SDFTags with info about the parties (for the parties that are properly classified via 
                //SDFTaggerForPartyClassificationOnKeywords... read below the comments on the method addSDFTagsForParties).
            buildAkomaNtosoCJEUs.addSDFTagsForParties(SDFTags, SDFDependencyTrees, SDFTaggerForPartyClassificationOnKeywords, SDFTaggersForEntityLinking);

                //Now we obtain an Hashtable<SDFHead, SDFTag> also from SDFTags.
                //For the SDFTag(s) identifying the titles of the sections we identify consecutive sequences of SDFHeads, and we convert the hashtable 
                //in a format more suitable to build the AkomaNtoso file.
                //Same for the others, but before the conversion, we remove the tags having low priority that intersect with the ones having higher priority.
            Hashtable<SDFTag, ArrayList<SDFHead>> tags2sequencesSDFTags = Utilities.createSequencesOfConsecutiveSDFHeads(SDFTags, SDFDependencyTrees);
            Utilities.removeIntersectingSequencesOfConsecutiveSDFHeads(tags2sequencesSDFTags, SDFTags);
            Hashtable<SDFHead, SDFTag> SDFHeads2SDFTags = Utilities.convertHashtable(tags2sequencesSDFTags);

                //(3) We use the tags collected above (SDFHeads2SectionsSDFTags and SDFHeads2SDFTags) to build *A FIRST DRAFT* of the Akoma Ntoso.
            Element judgment = buildAkomaNtosoCJEUs.buildJudgment(SDFDependencyTrees, SDFHeads2SectionsSDFTags, SDFHeads2SDFTags);

                //(4) We adjust the *FIRST DRAFT*.
                //Specifically, in the method adjustElements, we polish the "leaf" Element(s) in the DRAFT OF the judgment,
                //in order to make them AkomaNtoso-compliant.
            buildAkomaNtosoCJEUs.adjustElements(judgment);
            judgment.getContent().add(0, metaCreator.createMeta(buildAkomaNtosoCJEUs.allOrganizations));

                //------------------------------------------------------------------------------------------------------
                //FINALLY...
                //We build the final <akomaNtoso> and we write it in the output file
            Element akomaNtoso = new Element("akomaNtoso", xmlns);
            akomaNtoso.addNamespaceDeclaration(xsi);
            akomaNtoso.addNamespaceDeclaration(xsd);
            akomaNtoso.getContent().add(judgment);
            Document outputDoc = new Document();
            outputDoc.setRootElement(akomaNtoso);
            XMLOutputter outputter = new XMLOutputter();
            outputter.setFormat(Format.getPrettyFormat().setEncoding("UTF-8"));
            FileOutputStream fos = new FileOutputStream(outputFile);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
            outputter.output(outputDoc, osw);
            osw.close();
            fos.close();

            /**/
                //We check if the Akoma Ntoso is valid. If it's not, there is something really wrong in the Java code... we exit.
            ArrayList<String> validationMessages = validateAkomaNtoso.validateASingleFile(outputFile);
            if(validationMessages.isEmpty()==false)
            {
                System.out.println("The file: "+outputFile.getName()+" is not valid with respect to the AkomaNtoso XSD");
                for(int j=0;j<validationMessages.size();j++)System.out.println("\t"+validationMessages.get(j));
                System.exit(0);
            }
            /**/

                //And, at the end of *EACH* case law, we save the ontology model in the file, with the new individuals we've harvested from the case law.
            OntologyManager.saveOntology(ontologyFilePath);
        }
        catch(StackOverflowError e)
        {
            System.out.println("\n\n\n Set \"private static boolean resetAll = false;\" and run again: when this boolean is true, it reset the ontology and "
                    + "the folder of the output files; when it is true, it process *ONLY* the files that are not generated."
                    + "\n\n\n"
                    + "... and don't look at me, it's JDOM that saturates the memory, I don't know how to solve it...");
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
}