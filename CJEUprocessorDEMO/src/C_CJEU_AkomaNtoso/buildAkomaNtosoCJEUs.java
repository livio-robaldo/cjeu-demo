//Copyright 2014 Livio Robaldo
//This file is part of CJEUprocessorDEMO. The demo uses the SDFTagger library, whose source code can be obtained by contacting Livio Robaldo (http://www.liviorobaldo.com)
//CJEUprocessorDEMO is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software 
//Foundation, either version 3 of the License, or (at your option) any later version.
//CJEUprocessorDEMO is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
//PARTICULAR PURPOSE. See the GNU General Public License for more details.
//You should have received a copy of the GNU General Public License along with CJEUprocessorDEMO.  If not, see <http://www.gnu.org/licenses/>.

package C_CJEU_AkomaNtoso;

import C_CJEU_AkomaNtoso.SDFTaggerConfigForStructuralTagging.SDFTaggerConfigForStructuralTagging;
import C_CJEU_AkomaNtoso.SDFTaggerConfigForPartyClassificationOnKeywords.SDFTaggerConfigForPartyClassificationOnKeywords;
import SDFTagger.SDFItems.SDFHead;
import SDFTagger.SDFItems.SDFDependencyTree;
import SDFTagger.SDFTagger;
import SDFTagger.SDFItems.SDFTag;
import java.io.*;
import java.util.*;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.*;
    //SDFTagger
import XMLUtilities.*;
    //SDFTagger configurations
import C_CJEU_AkomaNtoso.SDFTaggerConfigForEntityLinking.*;
import OntologyManager.*;
import OntologyManager.SDFBuilder.*;


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

public class buildAkomaNtosoCJEUs 
{
    private static boolean resetAll = true;
    
    private static File inputFolder = new File("./CORPUS/1 - PARSED INPUT");
    private static File outputFolder = new File("./CORPUS/2 - AKOMA NTOSO");
    private static String ontologyFilePath = "./CORPUS/OWL_Ontology/CJEUontology.owl";
    
    protected static Namespace xmlns = Namespace.getNamespace("http://docs.oasis-open.org/legaldocml/ns/akn/3.0/CSD11");
    protected static Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    protected static Namespace xsd = Namespace.getNamespace("xsd", "http://www.w3.org/2001/XMLSchema");
    
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
            SDFTagger SDFTaggerForStructuralTagging = new SDFTagger(new SDFTaggerConfigForStructuralTagging(null));
            SDFTagger SDFTaggerForPartyClassificationOnKeywords = new SDFTagger(new SDFTaggerConfigForPartyClassificationOnKeywords(null));
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
            

                //We convert each case law. In the for cycle below, there are two steps: "(1) STRUCTURAL TAGGING" and "(2) ENTITY LINKING".  
                //The former create the structure of the Akoma Ntoso, also identifying titles, parties, itemizations, paragraphs, etc.
                //The latter tries to classify all identified parties as either "company" or "institution". Whenever it manages to do it, 
                //it creates a new individual in the ontology, then it generates and executes the SDFRule that recognize the same pattern.
                //For institutions, it also generates and executes several variant of the SDFRule associated with the institution. 
            for(int i=0;i<inputFolder.listFiles().length;i++)
            {
                if(inputFolder.listFiles()[i].getName().lastIndexOf(".xml")!=inputFolder.listFiles()[i].getName().length()-4)continue;
                File outputFile = new File(outputFolder.getAbsolutePath()+"/"+inputFolder.listFiles()[i].getName());
                if(outputFile.exists()==true)continue;
                
                try
                {
                    System.out.println("Building Akoma Ntoso document of: "+inputFolder.listFiles()[i].getAbsolutePath());
                    
                    Document inputDoc = (Document) new SAXBuilder().build(inputFolder.listFiles()[i]);
                    
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
                    ArrayList<SDFTag> sectionsSDFTags = extractSectionsSDFTags(SDFTags);
                    Hashtable<SDFTag, ArrayList<SDFHead>> tags2sequencesSectionsSDFTags = Utilities.createSequencesOfConsecutiveSDFHeads(sectionsSDFTags, SDFDependencyTrees);
                    Hashtable<SDFHead, SDFTag> SDFHeads2SectionsSDFTags = Utilities.convertHashtable(tags2sequencesSectionsSDFTags);
                    
                        //SDFHeads2SectionsSDFTags will be used later. 
                        //Now we continue to process the remaining SDFTags; we must obtain a Hashtable<SDFHead, SDFTag> also from them.
                        //We recall this method that continue to fill SDFTags with info about the parties (for the parties that are properly classified via 
                        //SDFTaggerForPartyClassificationOnKeywords... read below the comments on the method addSDFTagsForParties).
                    addSDFTagsForParties(SDFTags, SDFDependencyTrees, SDFTaggerForPartyClassificationOnKeywords, SDFTaggersForEntityLinking);
                                  
                        //Now we obtain an Hashtable<SDFHead, SDFTag> also from SDFTags.
                        //For the SDFTag(s) identifying the titles of the sections we identify consecutive sequences of SDFHeads, and we convert the hashtable 
                        //in a format more suitable to build the AkomaNtoso file.
                        //Same for the others, but before the conversion, we remove the tags having low priority that intersect with the ones having higher priority.
                    Hashtable<SDFTag, ArrayList<SDFHead>> tags2sequencesSDFTags = Utilities.createSequencesOfConsecutiveSDFHeads(SDFTags, SDFDependencyTrees);
                    Utilities.removeIntersectingSequencesOfConsecutiveSDFHeads(tags2sequencesSDFTags, SDFTags);
                    Hashtable<SDFHead, SDFTag> SDFHeads2SDFTags = Utilities.convertHashtable(tags2sequencesSDFTags);
                    
                        //(3) We use the tags collected above (SDFHeads2SectionsSDFTags and SDFHeads2SDFTags) to build *A FIRST DRAFT* of the Akoma Ntoso.
                    Element judgment = buildJudgment(SDFDependencyTrees, SDFHeads2SectionsSDFTags, SDFHeads2SDFTags);

                        //(4) We adjust the *FIRST DRAFT*.
                        //Specifically, in the method adjustElements, we polish the "leaf" Element(s) in the DRAFT OF the judgment,
                        //in order to make them AkomaNtoso-compliant.
                    adjustElements(judgment);
                    judgment.getContent().add(0, metaCreator.createMeta(allOrganizations));
                    
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
                        //If there is an exception, we simply jump to the next file
                    System.out.println("Exception on the file: "+inputFolder.listFiles()[i].getName()+": "+e.getMessage());
                    continue;
                }
            }
        } 
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
    
        //There are SDFTag(s) referring to the borders of the sections, e.g. "end-of-header", "beginning-of-conclusions", "end-of-motivation", etc.
        //This is a simple private utility that extract all these tags and gives them in output. 
        //IMPORTANT!!!!!!! These tags are removed from the parameter ArrayList<SDFTag> SDFTags!
    protected static ArrayList<SDFTag> extractSectionsSDFTags(ArrayList<SDFTag> SDFTags)throws Exception
    {
        ArrayList<SDFTag> sectionsSDFTags = new ArrayList<SDFTag>();
     
        for(int i=0;i<SDFTags.size();i++)
        {
            SDFTag SDFTag = SDFTags.get(i);
            
            if
            (
                (SDFTag.tag.compareToIgnoreCase("end-of-header")==0)||
                (SDFTag.tag.compareToIgnoreCase("beginning-of-judgmentBody")==0)||
                (SDFTag.tag.compareToIgnoreCase("end-of-judgmentBody")==0)||
                (SDFTag.tag.compareToIgnoreCase("beginning-of-conclusions")==0)||
                    
                    //within judgmentBody
                (SDFTag.tag.compareToIgnoreCase("end-of-introduction")==0)||
                (SDFTag.tag.compareToIgnoreCase("beginning-of-background")==0)||
                (SDFTag.tag.compareToIgnoreCase("end-of-background")==0)||
                (SDFTag.tag.compareToIgnoreCase("beginning-of-motivation")==0)||
                (SDFTag.tag.compareToIgnoreCase("end-of-motivation")==0)||
                (SDFTag.tag.compareToIgnoreCase("beginning-of-decision")==0)
            )
            {
                sectionsSDFTags.add(SDFTags.remove(i));
                i--;
            }
        }
        
        return sectionsSDFTags;
    }
    
        //This and the procedures below build a single AkomaNtoso file:
    protected static Element buildJudgment(ArrayList<SDFDependencyTree> SDFDependencyTrees, Hashtable<SDFHead, SDFTag> SDFHeads2SDFTagsSections, Hashtable<SDFHead, SDFTag> SDFHeads2SDFTags)throws Exception
    {
            //We create a <judgment> and we add an empty <meta> (it is needed only to allow the validation via the XSD)
        Element judgment = new Element("judgment", xmlns);
        judgment.setAttribute("name", "JUDGMENT OF THE CJEU");
        
            //We create the first partition between <header>, <judgmentBody>, and <conclusions>
        ArrayList<ArrayList<SDFDependencyTree>> HeaderJudgementBodyConclusionsSDFDependencyTrees = partitionIntoHeaderJudgementBodyAndConclusions(SDFDependencyTrees, SDFHeads2SDFTagsSections);
        ArrayList<SDFDependencyTree> headerSDFDependencyTrees = HeaderJudgementBodyConclusionsSDFDependencyTrees.get(0);
        ArrayList<SDFDependencyTree> judgmentBodySDFDependencyTrees = HeaderJudgementBodyConclusionsSDFDependencyTrees.get(1);
        ArrayList<SDFDependencyTree> conclusionsSDFDependencyTrees = HeaderJudgementBodyConclusionsSDFDependencyTrees.get(2);
        
        Element header = new Element("header", xmlns);
        Element judgmentBody = new Element("judgmentBody", xmlns);
        Element conclusions = new Element("conclusions", xmlns);
        judgment.getContent().add(header);
        judgment.getContent().add(judgmentBody);
        judgment.getContent().add(conclusions);
        
            //The judgmentBody is structured into introduction, background, motivation, decision
        ArrayList<ArrayList<SDFDependencyTree>> IntroductionBackgroundMotivationDecisionSDFDependencyTrees = partitionIntoIntroductionBackgroundMotivationAndDecision(judgmentBodySDFDependencyTrees, SDFHeads2SDFTagsSections);
        ArrayList<SDFDependencyTree> introductionSDFDependencyTrees = IntroductionBackgroundMotivationDecisionSDFDependencyTrees.get(0);
        ArrayList<SDFDependencyTree> backgroundSDFDependencyTrees = IntroductionBackgroundMotivationDecisionSDFDependencyTrees.get(1);
        ArrayList<SDFDependencyTree> motivationSDFDependencyTrees = IntroductionBackgroundMotivationDecisionSDFDependencyTrees.get(2);
        ArrayList<SDFDependencyTree> decisionSDFDependencyTrees = IntroductionBackgroundMotivationDecisionSDFDependencyTrees.get(3);
        
            //... and we populate with the Element|Text obtained out of the three-fold partition above and the SDFTags left in the Hashtable tags2sequences
        ArrayList<Content> contentsHeader = SDFDependencyTrees2XML.SDFDependencyTrees2XML(headerSDFDependencyTrees, SDFHeads2SDFTags);
            ArrayList<Content> contentsIntroduction = SDFDependencyTrees2XML.SDFDependencyTrees2XML(introductionSDFDependencyTrees, SDFHeads2SDFTags);
            ArrayList<Content> contentsBackground = SDFDependencyTrees2XML.SDFDependencyTrees2XML(backgroundSDFDependencyTrees, SDFHeads2SDFTags);
            ArrayList<Content> contentsMotivation = SDFDependencyTrees2XML.SDFDependencyTrees2XML(motivationSDFDependencyTrees, SDFHeads2SDFTags);
            ArrayList<Content> contentsDecision = SDFDependencyTrees2XML.SDFDependencyTrees2XML(decisionSDFDependencyTrees, SDFHeads2SDFTags);
        ArrayList<Content> contentsConclusions = SDFDependencyTrees2XML.SDFDependencyTrees2XML(conclusionsSDFDependencyTrees, SDFHeads2SDFTags);
        
            //Now we identify <paragraph>(s) and <blockList> and we put the Text(s) into <p>.
            //Introduction, Background, Motivation, and Decision have <paragraph>(s), while Header and Conclusions can only have <blockList>(s)
        ArrayList<Element> elementsHeader = Utilities.createBlockLists(contentsHeader);        
            ArrayList<Element> elementsIntroduction = createParagraphs(contentsIntroduction);
            ArrayList<Element> elementsBackground = createParagraphs(contentsBackground);
            ArrayList<Element> elementsMotivation = createParagraphs(contentsMotivation);
            ArrayList<Element> elementsDecision = createParagraphs(contentsDecision);
        ArrayList<Element> elementsConclusions = Utilities.createBlockLists(contentsConclusions);

            //We add the subsection of <judgmentBody>
        Element introduction = new Element("introduction", xmlns);
        Element background = new Element("background", xmlns);
        Element motivation = new Element("motivation", xmlns);
        Element decision = new Element("decision", xmlns);
        judgmentBody.getContent().add(introduction);
        judgmentBody.getContent().add(background);
        judgmentBody.getContent().add(motivation);
        judgmentBody.getContent().add(decision);
        
            //We fill all the sections and subsections
        for(int j=0;j<elementsHeader.size();j++)header.getContent().add(elementsHeader.get(j));
            for(int j=0;j<elementsIntroduction.size();j++)introduction.getContent().add(elementsIntroduction.get(j));
            for(int j=0;j<elementsBackground.size();j++)background.getContent().add(elementsBackground.get(j));
            for(int j=0;j<elementsMotivation.size();j++)motivation.getContent().add(elementsMotivation.get(j));
            for(int j=0;j<elementsDecision.size();j++)decision.getContent().add(elementsDecision.get(j));
        for(int j=0;j<elementsConclusions.size();j++)conclusions.getContent().add(elementsConclusions.get(j));
                    
        return judgment;
    }
    
/**************************************************************************************************************************************************************/    
/**************************************************************************************************************************************************************/
/*                                                                                                                                                            */
/*     METHODS FOR FINDING THE PARTIES, CLASSIFYING THE PARTIES, GENERATING THE SDFRule(s) FOR ENTITY LINKING ON THE PARTIES, EXECUTING THESE RULES TO GENERATE NEW SDFTag(s) */
/*                                                                                                                                                            */
/**************************************************************************************************************************************************************/    
/**************************************************************************************************************************************************************/
    
    /**
    This method addSDFTagsForParties (and the following):
        (1) Extract from SDFTags all SDFHead(s) associated with a <party>.
        (2) We classify each party as "company" or "institution" via SDFTaggerConfigForPartyClassificationOnKeywords.
        (3) For each company or institution recognized, we create an individual in the ontology and we add the SDFRule(s) 
        (4) We create a new SDFTagger for the new SDFRule(s) and we execute it on all other SDFDependencyTrees(s), and 
            we add the new SDFTag(s) to the ArrayList<SDFTag> SDFTags.
    */
    
    protected static void addSDFTagsForParties
    (
        ArrayList<SDFTag> SDFTags, 
        ArrayList<SDFDependencyTree> SDFDependencyTrees, 
        SDFTagger SDFTaggerForPartyClassificationOnKeywords,
        ArrayList<SDFTagger> SDFTaggersForEntityLinking
    )throws Exception
    {
            //(1) Extract from SDFTags all SDFHead(s) associated with a <party>. NB. Those are *REMOVED* from SDFTags and put in a new ArrayList SDFTagsParty.
            //We extract the SDFTags corresponding to a party and we remove the intersections.
        ArrayList<SDFTag> SDFTagsParty = new ArrayList<SDFTag>();
        for(int i=0;i<SDFTags.size();i++)if(SDFTags.get(i).tag.indexOf("party")==0){SDFTagsParty.add(SDFTags.remove(i));i--;}
        Hashtable<SDFTag, ArrayList<SDFHead>> tags2sequencesSDFTagsParty = Utilities.createSequencesOfConsecutiveSDFHeads(SDFTagsParty, SDFDependencyTrees);
        Utilities.removeIntersectingSequencesOfConsecutiveSDFHeads(tags2sequencesSDFTagsParty, SDFTagsParty);
        
            //(2) We try to classify each party as "company" or "institution" via SDFTaggerConfigForPartyClassificationOnKeywords.
            //Each ArrayList<SDFHead> in tags2sequencesSDFTagsParty is a party. We retrieve the corresponding SDFDependencyTree and we classify them 
            //as either "company" or "institution". If we manage, we add new SDFTag(s) in the ArrayList<SDFTag> SDFTags (details below).
            //Inside this cycle: (3) For each company or institution recognized, we create an individual in the ontology and we add the SDFRule(s).
            //The point (3) is done within the procedure 
        Enumeration en = tags2sequencesSDFTagsParty.keys();
            //This array collects the local paths of the SDFRule(s) recognizing the parties. We'll allocate a new SDFTagger out of these paths,
            //and we will add it to the ArrayList<SDFTagger> SDFTaggersForEntityLinking.
        ArrayList<String> localPathsSDFRules = new ArrayList<String>();
        while(en.hasMoreElements()==true)
        {
            SDFTag SDFTag = (SDFTag)en.nextElement();
            ArrayList<SDFHead> SDFHeads = tags2sequencesSDFTagsParty.get(SDFTag);
            
                //We search on the roots, for efficiency reasons.
            ArrayList<SDFHead> roots = new ArrayList<SDFHead>();
            for(int i=0;i<SDFHeads.size();i++)
            {
                SDFHead root = SDFHeads.get(i);
                while(root.getGovernor()!=null)root=root.getGovernor();
                if(roots.indexOf(root)==-1)roots.add(root);
            }
            ArrayList<SDFDependencyTree> SDFDependencyTreesToClassify = new ArrayList<SDFDependencyTree>();
            for(int i=0;(i<SDFDependencyTrees.size())&&(roots.isEmpty()==false);i++)
            {
                int index = roots.indexOf(SDFDependencyTrees.get(i).getRoot());
                if(index==-1)continue;
                SDFDependencyTreesToClassify.add(SDFDependencyTrees.get(i));
                roots.remove(index);
            }
            
                //We try to recognize the party with the SDFTagger(s) in the ArrayList SDFTaggersForEntityLinking. If at least one of this SDFTagger
                //Recognize the party... it is because the same company or institution appears in two case law! Therefore, we don't have to classify it again.
                //Rather, we have to link it to the same individual in the ontology. So, we ignore the steps below, for this party.
            String recognizedParty = "";
            for(int i=0; (i<SDFTaggersForEntityLinking.size())&&(recognizedParty.isEmpty());i++)
            {
                ArrayList<SDFTag> tagsOfTheParty = SDFTaggersForEntityLinking.get(i).tagTrees(SDFDependencyTreesToClassify);
                if(tagsOfTheParty.isEmpty()==false)recognizedParty=tagsOfTheParty.get(0).tag;
            }
            
                //If we did not recognize any party, we try to classify the <party>. And, if we manage to do it:
                //(3) For each company or institution recognized, we create an individual in the ontology and we add the SDFRule(s)
                //The point (3) is done within the method createOntologyIndividualAnSDFRules; more details below.
            if(recognizedParty.isEmpty())
            {
                String[] individualNameAndSDFRulesPath = null;
                ArrayList<SDFTag> tagsOfTheParty = SDFTaggerForPartyClassificationOnKeywords.tagTrees(SDFDependencyTreesToClassify);
                if(tagsOfTheParty.isEmpty()==false)
                    individualNameAndSDFRulesPath=createOntologyIndividualAnSDFRules(tagsOfTheParty.get(0).tag, SDFHeads, SDFTaggersForEntityLinking);
                if(individualNameAndSDFRulesPath!=null)
                {
                    recognizedParty = individualNameAndSDFRulesPath[0];
                    localPathsSDFRules.add(individualNameAndSDFRulesPath[1]);
                }
            }
                
                //If recognizedParty is still empty, nothing to do: the party will remain unclassified. We recreate the same SDFTag(s) in the ArrayList<SDFTag> SDFTags.
                //Otherwise, we create and add new SDFTag(s) by adding the recognizedParty name at the end.
                //To obtain either results, it is sufficient to concatenate recognizedParty; in case no party has been recognized, recognizedParty is empty, and 
                //the tag will be the one of before PLUS! the "-" concatenated at the end.
                //NB. Note that the SDFRule(s) in party.xml have priority="10000", while the ones in the SDFRule(s) generated in createOntologyIndividualAnSDFRules
                //start with priority="10000". That's why the SDFTag(s) in this for cycle will override the SDFTag(s) obtained in step (4) below.
            for(int i=0;i<SDFHeads.size();i++)
                SDFTags.add(new SDFTag(SDFTag.tag+"-"+recognizedParty, SDFHeads.get(i), SDFTag.priority, SDFTag.idSDFRule, SDFTag.idInstance));
        }
        
            //(4) We create a new SDFTagger for the new SDFRule(s) and we execute it on all other SDFDependencyTrees(s), and 
            //    we add the new SDFTag(s) to the ArrayList<SDFTag> SDFTags.
        String[] localPathsSDFRulesArray = new String[localPathsSDFRules.size()];
        for(int i=0;i<localPathsSDFRules.size();i++)localPathsSDFRulesArray[i]=localPathsSDFRules.get(i);
                
            //We generate a new SDFTagger out of the localPathsSDFRules collected in the while cycle above. 
            //And, we add this SDFTagger to SDFTaggersForEntityLinking.
        SDFTagger newSDFTaggerForEntityLinking = new SDFTagger(new SDFTaggerConfigForEntityLinking(null, localPathsSDFRulesArray));
        SDFTaggersForEntityLinking.add(newSDFTaggerForEntityLinking);
            
            //We generate all SDFTag(s) in the other SDFDependencyTrees and we add them to the ArrayList<SDFTag> SDFTags.
            //Note that also the parties will be re-annotated as "company_1", "institution_3", etc. But, as I wrote you just above, the one associating the
            //tags "party-appellant-company_1", "party-respondent-institution_3", etc. have higher priority and will override the SDFTags generated now.
        ArrayList<SDFTag> newSDFTags = newSDFTaggerForEntityLinking.tagTrees(SDFDependencyTrees);
        for(int i=0;i<newSDFTags.size();i++)SDFTags.add(newSDFTags.get(i));
    } 
    
        //(3) For each company or institution recognized, we create an individual in the ontology and we add the SDFRule(s).
        //Specifically:
        //(3.1) We create an individual in the ontology under the OWL class partyClass (either "company" or "institution")
        //(3.2) We generate the new SDFRule(s) for the ArrayList<SDFHead> SDFHeads (NB: in case of Institution, SDFRule(s) are expanded within InstitutionsSDFRulesBuilder)
        //(3.3) We return a String[] of two elements: the name of the new individual and the local path of the SDFRule(s) recognizing it (the global path are the ones
        //      specified in all SDFTagger(s) belonging to SDFTaggersForEntityLinking (we retrieve this global path from any of them, e.g., the first one).
    private static String[] createOntologyIndividualAnSDFRules(String partyClass, ArrayList<SDFHead> SDFHeads, ArrayList<SDFTagger> SDFTaggersForEntityLinking)throws Exception
    {
        String rootDirectoryXmlSDFRulesPath = ((SDFTaggerConfigForEntityLinking)SDFTaggersForEntityLinking.get(0).getSDFTaggerConfig()).getRootDirectoryXmlSDFRulesPath();
        
        String newIndividualName = null;
        ArrayList<Element> newSDFRules = null;
        if(partyClass.compareToIgnoreCase("Company")==0)
        {
            newIndividualName = OntologyManager.createCompany();
            newSDFRules = CompaniesSDFRulesBuilder.createSDFRulesForCompaniesFromSDFHeads(SDFHeads, newIndividualName);
        }
        else if(partyClass.compareToIgnoreCase("Institution")==0)
        {
            newIndividualName = OntologyManager.createInstitution();
            newSDFRules = InstitutionsSDFRulesBuilder.createSDFRulesForInstitutionFromSDFHeads(SDFHeads, newIndividualName);
        }
        else throw new Exception("Unrecognized class for the party! Only \"Company\" and \"Institution\" are allowed");
        
        File outputFolder = new File(rootDirectoryXmlSDFRulesPath+"/"+partyClass);
        if(outputFolder.exists()==false)outputFolder.mkdir();
        File outputFile = new File(outputFolder.getCanonicalPath()+"/"+newIndividualName+".xml");
        SDFRulesBuilderUtilities.saveSDFRulesOnFile(newSDFRules, outputFile);
        
        String[] ret = new String[2];
        ret[0] = newIndividualName;
        ret[1] = partyClass+"/"+newIndividualName+".xml";
        return ret;
    }
    
/**************************************************************************************************************************************************************/    
/**************************************************************************************************************************************************************/
/*                                                                                                                                                            */
/*                                            METHODS FOR PARTITIONING/IDENTIFYING THE SECTIONS                                                               */
/*                                 (header, judgmentBody, introduction, background, motivation, decision, conclusions)                                        */
/*                                                                                                                                                            */
/**************************************************************************************************************************************************************/    
/**************************************************************************************************************************************************************/    
        //This method extract all SDFTag(s) that allow to identify the <header>, the <judgmentBody>, and the <conclusions>.
        //We identify the three sections by populating three ArrayList<SDFDependencyTree> that will be given in return. 
        //The tags in taggedSDFHeads can be:
        //  - end-of-header
        //  - beginning-of-judgmentBody
        //  - end-of-judgmentBody
        //  - beginning-of-conclusions
        //These tags *DO NOT OVERLAP* because we used the procedure "removeIntersectingSequencesOfConsecutiveSDFHeads" to remove overlappings. However, there could
        //be multiple "end-of-header"(s), "beginning-of-conclusions"(s), etc. 
        //Therefore, we must first look for the ones with highest priority, then we will split the three sections according to these highest priority ones.
    
        //The method returns then an ArrayList containing *THREE* ArrayList<SDFDependencyTree> that partition the one in input: one for each of the three sections 
        //<header>, <judgmentBody>, and <conclusions>. If one of the three is not identified, the method throws an Exception
    private static ArrayList<ArrayList<SDFDependencyTree>> partitionIntoHeaderJudgementBodyAndConclusions
    (
        ArrayList<SDFDependencyTree> SDFDependencyTrees, 
        Hashtable<SDFHead, SDFTag> taggedSDFHeads
    )throws Exception
    {
            //FIRST CYCLE: we first look for the four possible tags (in case of multiple instances we only keep the one with highest priority)
        SDFTag endOfHeader = null;
        SDFTag beginningOfJudgementBody = null;
        SDFTag endOfJudgementBody = null;
        SDFTag beginningOfConclusions = null;
        ArrayList<SDFHead> endOfHeaderSDFHeads = new ArrayList<SDFHead>();
        ArrayList<SDFHead> beginningOfJudgementBodySDFHeads = new ArrayList<SDFHead>();
        ArrayList<SDFHead> endOfJudgementBodySDFHeads = new ArrayList<SDFHead>();
        ArrayList<SDFHead> beginningOfConclusionsSDFHeads = new ArrayList<SDFHead>();
                
        SDFTag tempSDFTag = null;
        ArrayList<SDFHead> tempSDFHeads = null;
        for(int i=0;i<SDFDependencyTrees.size();i++)
        {
            for(int j=0;j<SDFDependencyTrees.get(i).getHeads().length;j++)
            {
                SDFTag SDFTag = taggedSDFHeads.get(SDFDependencyTrees.get(i).getHeads()[j]);
                if(SDFTag==null)continue;
                
                    //until I keep finding the same tag, I populate the array of SDFHead selected in the previous cycle
                if((tempSDFTag!=null)&&(tempSDFTag.tag.compareToIgnoreCase(SDFTag.tag)==0)&&(tempSDFTag.idSDFRule==SDFTag.idSDFRule)&&(tempSDFTag.idInstance==SDFTag.idInstance))
                {
                    tempSDFHeads.add(SDFDependencyTrees.get(i).getHeads()[j]);
                    continue;
                }
                tempSDFTag = null;
                tempSDFHeads = null;
                
                
                if
                (
                    (SDFTag.tag.compareToIgnoreCase("end-of-header")==0)&&
                    ((endOfHeader==null)||(endOfHeader.priority<SDFTag.priority))
                )
                {
                    endOfHeader = SDFTag;
                    endOfHeaderSDFHeads.clear();
                    endOfHeaderSDFHeads.add(SDFDependencyTrees.get(i).getHeads()[j]);
                    tempSDFTag = SDFTag;
                    tempSDFHeads = endOfHeaderSDFHeads;
                }
                
                if
                (
                    (SDFTag.tag.compareToIgnoreCase("beginning-of-judgmentBody")==0)&&
                    ((beginningOfJudgementBody==null)||(beginningOfJudgementBody.priority<SDFTag.priority))
                )
                {
                    beginningOfJudgementBody = SDFTag;
                    beginningOfJudgementBodySDFHeads.clear();
                    beginningOfJudgementBodySDFHeads.add(SDFDependencyTrees.get(i).getHeads()[j]);
                    tempSDFTag = SDFTag;
                    tempSDFHeads = beginningOfJudgementBodySDFHeads;
                }
                    
                if
                (
                    (SDFTag.tag.compareToIgnoreCase("end-of-judgmentBody")==0)&&
                    ((endOfJudgementBody==null)||(endOfJudgementBody.priority<SDFTag.priority))
                )
                {
                    endOfJudgementBody = SDFTag;
                    endOfJudgementBodySDFHeads.clear();
                    endOfJudgementBodySDFHeads.add(SDFDependencyTrees.get(i).getHeads()[j]);
                    tempSDFTag = SDFTag;
                    tempSDFHeads = endOfJudgementBodySDFHeads;
                }
                
                if
                (
                    (SDFTag.tag.compareToIgnoreCase("beginning-of-conclusions")==0)&&
                    ((beginningOfConclusions==null)||(beginningOfConclusions.priority<SDFTag.priority))
                )
                {
                    beginningOfConclusions = SDFTag;
                    beginningOfConclusionsSDFHeads.clear();
                    beginningOfConclusionsSDFHeads.add(SDFDependencyTrees.get(i).getHeads()[j]);
                    tempSDFTag = SDFTag;
                    tempSDFHeads = beginningOfConclusionsSDFHeads;
                }
            }
        }
        
            //Now, we have identified: 
            //  - SDFTag endOfHeader
            //  - SDFTag beginningOfJudgementBody
            //  - SDFTag endOfJudgementBody
            //  - SDFTag beginningOfConclusions
            //However, in case there is both endOfHeader and beginningOfJudgementBody we only keep the one with higher priority. 
            //Same for endOfJudgementBody and beginningOfConclusions
        if((endOfHeader!=null)&&(beginningOfJudgementBody!=null))
            if(endOfHeader.priority>beginningOfJudgementBody.priority)beginningOfJudgementBody=null;
            else endOfHeader=null;
        if((endOfJudgementBody!=null)&&(beginningOfConclusions!=null))
            if(endOfJudgementBody.priority>beginningOfConclusions.priority)beginningOfConclusions=null;
            else endOfJudgementBody=null;
        
            //Now, in the three cycles below, we identify the SDFDependencyTree(s) belonging to <header>, <judgmentBody>, <conclusions> on the basis of the SDFTag(s):
            //  - SDFTag endOfHeader
            //  - SDFTag beginningOfJudgementBody
            //  - SDFTag endOfJudgementBody
            //  - SDFTag beginningOfConclusions
            //that we have (i.e., that are not null)
        ArrayList<SDFDependencyTree> headerSDFDependencyTrees = new ArrayList<SDFDependencyTree>();
        ArrayList<SDFDependencyTree> judgmentBodySDFDependencyTrees = new ArrayList<SDFDependencyTree>();
        ArrayList<SDFDependencyTree> conclusionsSDFDependencyTrees = new ArrayList<SDFDependencyTree>();
        
            //1) FIRST CYCLE: we identify the SDFDependencyTree(s) which belong to the <header>
        int i=0;
        for(;i<SDFDependencyTrees.size();i++)
        {
                //We take all SDFHeads of the SDFDependencyTrees. If at least one of them is in endOfHeaderSDFHeads or beginningOfJudgementBodySDFHeads,
                //i.e., if SDFHeads *intersect* with one of the array, we put *THE WHOLE* SDFDependencyTree as the last of the <header> or the first 
                //of the <judgmentBody> (depending on which of the two, of course).
            SDFHead[] SDFHeads = SDFDependencyTrees.get(i).getHeads();
            
            if(endOfHeader!=null)
            {
                boolean intersect=false;
                for(int j=0;(j<SDFHeads.length)&&(intersect==false);j++)if(endOfHeaderSDFHeads.indexOf(SDFHeads[j])!=-1)intersect=true;

                    //We found the intersection! We add this SDFDependencyTree to headerSDFDependencyTrees and we proceed until we find SDFDependencyTree(s) 
                    //whose SDFHead(s) are also marked by same SDFTag. All those SDFDependencyTree(s) are *THE LAST ONES* in <header>.
                    //Once we've collected them all, we break.
                if(intersect==true)
                {
                    headerSDFDependencyTrees.add(SDFDependencyTrees.get(i));
                    for(i=i+1;(i<SDFDependencyTrees.size())&&(intersect==true);i++)
                    {
                        SDFHeads = SDFDependencyTrees.get(i).getHeads();
                        intersect=false;
                        for(int j=0;(j<SDFHeads.length)&&(intersect==false);j++)if(endOfHeaderSDFHeads.indexOf(SDFHeads[j])!=-1)intersect=true;
                        if(intersect==true)headerSDFDependencyTrees.add(SDFDependencyTrees.get(i));
                        else i--;
                    }
                    break;
                }
            }
            else if(beginningOfJudgementBody!=null)
            {
                boolean intersect=false;
                for(int j=0;(j<SDFHeads.length)&&(intersect==false);j++)if(beginningOfJudgementBodySDFHeads.indexOf(SDFHeads[j])!=-1)intersect=true;

                    //We found the intersection! This SDFDependencyTree is *THE FIRST ONE* of the <judgmentBody>.
                    //We add it to judgmentBodySDFDependencyTrees and we exit.
                if(intersect==true){judgmentBodySDFDependencyTrees.add(SDFDependencyTrees.get(i));i++;break;}
            }
            
                //If we're here, we didn't execute any of the two "break;" instructions in the "if(...){...}else if(...){...}" above.
                //We add the SDFDependencyTree to headerSDFDependencyTrees and we'll check the next one.
            headerSDFDependencyTrees.add(SDFDependencyTrees.get(i));
        }
        
            //2) SECOND CYCLE: we identify the SDFDependencyTree(s) which belong to the <judgmentBody>
        for(;i<SDFDependencyTrees.size();i++)
        {
            SDFHead[] SDFHeads = SDFDependencyTrees.get(i).getHeads();
            
            if(endOfJudgementBody!=null)
            {
                boolean intersect=false;
                for(int j=0;(j<SDFHeads.length)&&(intersect==false);j++)if(endOfJudgementBodySDFHeads.indexOf(SDFHeads[j])!=-1)intersect=true;

                    //We found the intersection! We add this SDFDependencyTree to judgmentBodySDFDependencyTrees and we proceed until we find SDFDependencyTree(s) 
                    //whose SDFHead(s) are also marked by same SDFTag. All those SDFDependencyTree(s) are *THE LAST ONES* in <judgmentBody>.
                    //Once we've collected them all, we break.
                if(intersect==true)
                {
                    judgmentBodySDFDependencyTrees.add(SDFDependencyTrees.get(i));
                    for(i=i+1;(i<SDFDependencyTrees.size())&&(intersect==true);i++)
                    {
                        SDFHeads = SDFDependencyTrees.get(i).getHeads();
                        intersect=false;
                        for(int j=0;(j<SDFHeads.length)&&(intersect==false);j++)if(endOfJudgementBodySDFHeads.indexOf(SDFHeads[j])!=-1)intersect=true;
                        if(intersect==true)judgmentBodySDFDependencyTrees.add(SDFDependencyTrees.get(i));
                        else i--;
                    }
                    break;
                }
                
            }
            else if(beginningOfConclusions!=null)
            {
                boolean intersect=false;
                for(int j=0;(j<SDFHeads.length)&&(intersect==false);j++)if(beginningOfConclusionsSDFHeads.indexOf(SDFHeads[j])!=-1)intersect=true;
                
                    //We found the intersection! This SDFDependencyTree is *THE FIRST ONE* of the <conclusions>.
                    //We add it to conclusionsSDFDependencyTrees and we exit.
                if(intersect==true){conclusionsSDFDependencyTrees.add(SDFDependencyTrees.get(i));i++;break;}
            }
            
                //If we're here, we didn't execute any of the two "break;" instructions in the "if(...){...}else if(...){...}" above.
                //We add the SDFDependencyTree to judgmentBodySDFDependencyTrees and we'll check the next one.
            judgmentBodySDFDependencyTrees.add(SDFDependencyTrees.get(i));
        }
        
            //3) SECOND CYCLE: we add all remaining SDFDependencyTree(s) to conclusionsSDFDependencyTrees
        for(;i<SDFDependencyTrees.size();i++)conclusionsSDFDependencyTrees.add(SDFDependencyTrees.get(i));
        
            //Now, if one of the three sets is empty: Exception!
        if(headerSDFDependencyTrees.isEmpty()==true)throw new Exception("I cannot find the <header>");
        if(judgmentBodySDFDependencyTrees.isEmpty()==true)throw new Exception("I cannot find the <judgmentBody>");
        if(conclusionsSDFDependencyTrees.isEmpty()==true)throw new Exception("I cannot find the <conclusions>");
        
            //Otherwise, the three sets are returned.
        ArrayList<ArrayList<SDFDependencyTree>> ret = new ArrayList<ArrayList<SDFDependencyTree>>();
        ret.add(headerSDFDependencyTrees);
        ret.add(judgmentBodySDFDependencyTrees);
        ret.add(conclusionsSDFDependencyTrees);
        return ret;
    }
    
        //Like the above one, but it splits the judgmentBody into the four subsection Introduction, Backcground, Motivation, and Decision
    private static ArrayList<ArrayList<SDFDependencyTree>> partitionIntoIntroductionBackgroundMotivationAndDecision
    (
        ArrayList<SDFDependencyTree> SDFDependencyTrees, 
        Hashtable<SDFHead, SDFTag> taggedSDFHeads
    )throws Exception
    {
            //FIRST CYCLE: we first look for the four possible tags (in case of multiple instances we only keep the one with highest priority)
        SDFTag endOfIntroduction = null;
        SDFTag beginningOfBackground = null;
        SDFTag endOfBackground = null;
        SDFTag beginningOfMotivation = null;
        SDFTag endOfMotivation = null;
        SDFTag beginningOfDecision = null;
        
        ArrayList<SDFHead> endOfIntroductionSDFHeads = new ArrayList<SDFHead>();
        ArrayList<SDFHead> beginningOfBackgroundSDFHeads = new ArrayList<SDFHead>();
        ArrayList<SDFHead> endOfBackgroundSDFHeads = new ArrayList<SDFHead>();
        ArrayList<SDFHead> beginningOfMotivationSDFHeads = new ArrayList<SDFHead>();
        ArrayList<SDFHead> endOfMotivationSDFHeads = new ArrayList<SDFHead>();
        ArrayList<SDFHead> beginningOfDecisionSDFHeads = new ArrayList<SDFHead>();
                
        SDFTag tempSDFTag = null;
        ArrayList<SDFHead> tempSDFHeads = null;
        for(int i=0;i<SDFDependencyTrees.size();i++)
        {
            for(int j=0;j<SDFDependencyTrees.get(i).getHeads().length;j++)
            {
                SDFTag SDFTag = taggedSDFHeads.get(SDFDependencyTrees.get(i).getHeads()[j]);
                if(SDFTag==null)continue;
                
                    //until I keep finding the same tag, I populate the array of SDFHead selected in the previous cycle
                if((tempSDFTag!=null)&&(tempSDFTag.tag.compareToIgnoreCase(SDFTag.tag)==0)&&(tempSDFTag.idSDFRule==SDFTag.idSDFRule)&&(tempSDFTag.idInstance==SDFTag.idInstance))
                {
                    tempSDFHeads.add(SDFDependencyTrees.get(i).getHeads()[j]);
                    continue;
                }
                tempSDFTag = null;
                tempSDFHeads = null;
                
                
                if
                (
                    (SDFTag.tag.compareToIgnoreCase("end-of-introduction")==0)&&
                    ((endOfIntroduction==null)||(endOfIntroduction.priority<SDFTag.priority))
                )
                {
                    endOfIntroduction = SDFTag;
                    endOfIntroductionSDFHeads.clear();
                    endOfIntroductionSDFHeads.add(SDFDependencyTrees.get(i).getHeads()[j]);
                    tempSDFTag = SDFTag;
                    tempSDFHeads = endOfIntroductionSDFHeads;
                }
                
                if
                (
                    (SDFTag.tag.compareToIgnoreCase("beginning-of-background")==0)&&
                    ((beginningOfBackground==null)||(beginningOfBackground.priority<SDFTag.priority))
                )
                {
                    beginningOfBackground = SDFTag;
                    beginningOfBackgroundSDFHeads.clear();
                    beginningOfBackgroundSDFHeads.add(SDFDependencyTrees.get(i).getHeads()[j]);
                    tempSDFTag = SDFTag;
                    tempSDFHeads = beginningOfBackgroundSDFHeads;
                }
                
                if
                (
                    (SDFTag.tag.compareToIgnoreCase("end-of-background")==0)&&
                    ((endOfBackground==null)||(endOfBackground.priority<SDFTag.priority))
                )
                {
                    endOfBackground = SDFTag;
                    endOfBackgroundSDFHeads.clear();
                    endOfBackgroundSDFHeads.add(SDFDependencyTrees.get(i).getHeads()[j]);
                    tempSDFTag = SDFTag;
                    tempSDFHeads = endOfBackgroundSDFHeads;
                }
                
                if
                (
                    (SDFTag.tag.compareToIgnoreCase("beginning-of-motivation")==0)&&
                    ((beginningOfMotivation==null)||(beginningOfMotivation.priority<SDFTag.priority))
                )
                {
                    beginningOfMotivation = SDFTag;
                    beginningOfMotivationSDFHeads.clear();
                    beginningOfMotivationSDFHeads.add(SDFDependencyTrees.get(i).getHeads()[j]);
                    tempSDFTag = SDFTag;
                    tempSDFHeads = beginningOfMotivationSDFHeads;
                }
                
                if
                (
                    (SDFTag.tag.compareToIgnoreCase("end-of-motivation")==0)&&
                    ((endOfMotivation==null)||(endOfMotivation.priority<SDFTag.priority))
                )
                {
                    endOfMotivation = SDFTag;
                    endOfMotivationSDFHeads.clear();
                    endOfMotivationSDFHeads.add(SDFDependencyTrees.get(i).getHeads()[j]);
                    tempSDFTag = SDFTag;
                    tempSDFHeads = endOfMotivationSDFHeads;
                }

                if
                (
                    (SDFTag.tag.compareToIgnoreCase("beginning-of-decision")==0)&&
                    ((beginningOfDecision==null)||(beginningOfDecision.priority<SDFTag.priority))
                )
                {
                    beginningOfDecision = SDFTag;
                    beginningOfDecisionSDFHeads.clear();
                    beginningOfDecisionSDFHeads.add(SDFDependencyTrees.get(i).getHeads()[j]);
                    tempSDFTag = SDFTag;
                    tempSDFHeads = beginningOfDecisionSDFHeads;
                }
            }
        }
        
            //Now, we have identified: 
            //  - SDFTag endOfIntroduction
            //  - SDFTag beginningOfBackground
            //  - SDFTag endOfBackground
            //  - SDFTag beginningOfMotivation
            //  - SDFTag endOfMotivation
            //  - SDFTag beginningOfDecision
            //However, in case there is both endOfIntroduction and beginningOfBackground we only keep the one with higher priority. 
            //Same for the other similar pairs.
        if((endOfIntroduction!=null)&&(beginningOfBackground!=null))
            if(endOfIntroduction.priority>beginningOfBackground.priority)beginningOfBackground=null;
            else endOfIntroduction=null;
        if((endOfBackground!=null)&&(beginningOfMotivation!=null))
            if(endOfBackground.priority>beginningOfMotivation.priority)beginningOfMotivation=null;
            else endOfBackground=null;
        if((endOfMotivation!=null)&&(beginningOfDecision!=null))
            if(endOfMotivation.priority>beginningOfDecision.priority)beginningOfDecision=null;
            else endOfMotivation=null;
        
            //Now, in the three cycles below, we identify the SDFDependencyTree(s) belonging to <introduction>, <background>, <motivation> and <decision>
            //on the basis of the SDFTag(s):
            //  - SDFTag endOfIntroduction
            //  - SDFTag beginningOfBackground
            //  - SDFTag endOfBackground
            //  - SDFTag beginningOfMotivation
            //  - SDFTag endOfMotivation
            //  - SDFTag beginningOfDecision
            //that we have (i.e., that are not null)
        ArrayList<SDFDependencyTree> introductionSDFDependencyTrees = new ArrayList<SDFDependencyTree>();
        ArrayList<SDFDependencyTree> backgroundSDFDependencyTrees = new ArrayList<SDFDependencyTree>();
        ArrayList<SDFDependencyTree> motivationSDFDependencyTrees = new ArrayList<SDFDependencyTree>();
        ArrayList<SDFDependencyTree> decisionSDFDependencyTrees = new ArrayList<SDFDependencyTree>();
        
            //1) FIRST CYCLE: we identify the SDFDependencyTree(s) which belong to the <introduction>
        int i=0;
        for(;i<SDFDependencyTrees.size();i++)
        {
            SDFHead[] SDFHeads = SDFDependencyTrees.get(i).getHeads();
            
            if(endOfIntroduction!=null)
            {
                boolean intersect=false;
                for(int j=0;(j<SDFHeads.length)&&(intersect==false);j++)if(endOfIntroductionSDFHeads.indexOf(SDFHeads[j])!=-1)intersect=true;

                if(intersect==true)
                {
                    introductionSDFDependencyTrees.add(SDFDependencyTrees.get(i));
                    for(i=i+1;(i<SDFDependencyTrees.size())&&(intersect==true);i++)
                    {
                        SDFHeads = SDFDependencyTrees.get(i).getHeads();
                        intersect=false;
                        for(int j=0;(j<SDFHeads.length)&&(intersect==false);j++)if(endOfIntroductionSDFHeads.indexOf(SDFHeads[j])!=-1)intersect=true;
                        if(intersect==true)introductionSDFDependencyTrees.add(SDFDependencyTrees.get(i));
                        else i--;
                    }
                    break;
                }
            }
            else if(beginningOfBackground!=null)
            {
                boolean intersect=false;
                for(int j=0;(j<SDFHeads.length)&&(intersect==false);j++)if(beginningOfBackgroundSDFHeads.indexOf(SDFHeads[j])!=-1)intersect=true;
                if(intersect==true){backgroundSDFDependencyTrees.add(SDFDependencyTrees.get(i));i++;break;}
            }
            
            introductionSDFDependencyTrees.add(SDFDependencyTrees.get(i));
        }
        
            //2) SECOND CYCLE: we identify the SDFDependencyTree(s) which belong to the <background>
        for(;i<SDFDependencyTrees.size();i++)
        {
            SDFHead[] SDFHeads = SDFDependencyTrees.get(i).getHeads();
            
            if(endOfBackground!=null)
            {
                boolean intersect=false;
                for(int j=0;(j<SDFHeads.length)&&(intersect==false);j++)if(endOfBackgroundSDFHeads.indexOf(SDFHeads[j])!=-1)intersect=true;

                if(intersect==true)
                {
                    backgroundSDFDependencyTrees.add(SDFDependencyTrees.get(i));
                    for(i=i+1;(i<SDFDependencyTrees.size())&&(intersect==true);i++)
                    {
                        SDFHeads = SDFDependencyTrees.get(i).getHeads();
                        intersect=false;
                        for(int j=0;(j<SDFHeads.length)&&(intersect==false);j++)if(endOfBackgroundSDFHeads.indexOf(SDFHeads[j])!=-1)intersect=true;
                        if(intersect==true)backgroundSDFDependencyTrees.add(SDFDependencyTrees.get(i));
                        else i--;
                    }
                    break;
                }
                
            }
            else if(beginningOfMotivation!=null)
            {
                boolean intersect=false;
                for(int j=0;(j<SDFHeads.length)&&(intersect==false);j++)if(beginningOfMotivationSDFHeads.indexOf(SDFHeads[j])!=-1)intersect=true;
                if(intersect==true){motivationSDFDependencyTrees.add(SDFDependencyTrees.get(i));i++;break;}
            }
            
            backgroundSDFDependencyTrees.add(SDFDependencyTrees.get(i));
        }
        
        
            //3) THIRD CYCLE: we identify the SDFDependencyTree(s) which belong to the <motivation>
        for(;i<SDFDependencyTrees.size();i++)
        {
            SDFHead[] SDFHeads = SDFDependencyTrees.get(i).getHeads();
            
            if(endOfMotivation!=null)
            {
                boolean intersect=false;
                for(int j=0;(j<SDFHeads.length)&&(intersect==false);j++)if(endOfMotivationSDFHeads.indexOf(SDFHeads[j])!=-1)intersect=true;

                if(intersect==true)
                {
                    motivationSDFDependencyTrees.add(SDFDependencyTrees.get(i));
                    for(i=i+1;(i<SDFDependencyTrees.size())&&(intersect==true);i++)
                    {
                        SDFHeads = SDFDependencyTrees.get(i).getHeads();
                        intersect=false;
                        for(int j=0;(j<SDFHeads.length)&&(intersect==false);j++)if(endOfMotivationSDFHeads.indexOf(SDFHeads[j])!=-1)intersect=true;
                        if(intersect==true)motivationSDFDependencyTrees.add(SDFDependencyTrees.get(i));
                        else i--;
                    }
                    break;
                }
                
            }
            else if(beginningOfDecision!=null)
            {
                boolean intersect=false;
                for(int j=0;(j<SDFHeads.length)&&(intersect==false);j++)if(beginningOfDecisionSDFHeads.indexOf(SDFHeads[j])!=-1)intersect=true;
                if(intersect==true){decisionSDFDependencyTrees.add(SDFDependencyTrees.get(i));i++;break;}
            }
            
            motivationSDFDependencyTrees.add(SDFDependencyTrees.get(i));
        }
        
            //4) SECOND CYCLE: we add all remaining SDFDependencyTree(s) to conclusionsSDFDependencyTrees
        for(;i<SDFDependencyTrees.size();i++)decisionSDFDependencyTrees.add(SDFDependencyTrees.get(i));
        
            //Now, if one of the three sets is empty: Exception!
        if(introductionSDFDependencyTrees.isEmpty()==true)throw new Exception("I cannot find the <introduction>");
        if(backgroundSDFDependencyTrees.isEmpty()==true)throw new Exception("I cannot find the <background>");
        if(motivationSDFDependencyTrees.isEmpty()==true)throw new Exception("I cannot find the <motivation>");
        if(decisionSDFDependencyTrees.isEmpty()==true)throw new Exception("I cannot find the <decision>");

            //Otherwise, the three sets are returned.
        ArrayList<ArrayList<SDFDependencyTree>> ret = new ArrayList<ArrayList<SDFDependencyTree>>();
        ret.add(introductionSDFDependencyTrees);
        ret.add(backgroundSDFDependencyTrees);
        ret.add(motivationSDFDependencyTrees);
        ret.add(decisionSDFDependencyTrees);
        return ret;
    }
    
/**************************************************************************************************************************************************************/    
/**************************************************************************************************************************************************************/
/*                                                                                                                                                            */
/*************************************              METHODS FOR CREATING/IDENTIFYING THE PARAGRAPHS                ********************************************/
/*                                                                                                                                                            */
/**************************************************************************************************************************************************************/    
/**************************************************************************************************************************************************************/   
        //Case law of CJEU are structured into paragraphs; those are identified by incremental numbers at the left of the screen.
        //This method transform them into:
        //<paragraph eId="para_1">
        //  <num>1.</num>
        //    <content eId="para_1__content">
        //        ...
        //    </content>
        //</paragraph>
    private static ArrayList<Element> createParagraphs(ArrayList<Content> contents)
    {
            //We extract the paragraph indexes, which has been tagged with the tag "paragraph-index".
        ArrayList<Element> paragraphIndexes = new ArrayList<Element>();
        for(int i=0;i<contents.size();i++)
        {
            if(contents.get(i)instanceof Text)continue;
            if(((Element)contents.get(i)).getName().compareToIgnoreCase("paragraph-index")!=0)continue;
            paragraphIndexes.add((Element)contents.get(i));
        }
            //if there are not, we try to identify <blockList>(s)
        if(paragraphIndexes.size()==0)return Utilities.createBlockLists(contents);
        
            //Now we create a new ArrayList<Content>; we create <paragraph>(s) whenever we find a "paragraph-index"
        contents = createParagraphs(contents, paragraphIndexes);
        
            //Now, we isolate the content before the first paragraph and the one after the last paragraph (actually, the latter should be always empty... but maybe
            //in the future I will find out it is not always, i.e., there are exceptions).
        ArrayList<Content> contentsBeforeParagraphs = new ArrayList<Content>();
        ArrayList<Element> paragraphs = new ArrayList<Element>();
        ArrayList<Content> contentsAfterParagraphs = new ArrayList<Content>();
        for(int i=0;i<contents.size();i++)
        {
            if((contents.get(i)instanceof Element)&&(((Element)contents.get(i)).getName().compareToIgnoreCase("paragraph")==0))
            {
                paragraphs.add((Element)contents.get(i));
                continue;
            }
            
            if(paragraphs.size()==0)contentsBeforeParagraphs.add(contents.get(i));
            else contentsAfterParagraphs.add(contents.get(i));
        }
        
        ArrayList<Element> ret = new ArrayList<Element>();
        
            //We try to identify <blockList>(s) in the contentsBeforeParagraphs and the contentsAfterParagraphs
        ArrayList<Element> elementsBeforeParagraphs = Utilities.createBlockLists(contentsBeforeParagraphs);
        for(int i=0;i<elementsBeforeParagraphs.size();i++)ret.add(elementsBeforeParagraphs.get(i));
        for(int i=0;i<paragraphs.size();i++)ret.add(paragraphs.get(i));
        ArrayList<Element> elementsAfterParagraphs = Utilities.createBlockLists(contentsAfterParagraphs);
        for(int i=0;i<elementsAfterParagraphs.size();i++)ret.add(elementsAfterParagraphs.get(i));
        
        return ret;
    }
    
        //We copy all contents until we find the first <paragraph-index>. Then we create the <paragraph>, we add it to the ArrayList<Content> that will be 
        //given in return, and we fill the <content> of the paragraph until I find the last <paragraph-index>.
        //Finally, we copy all Content(s) left in the input array (but this should be always empty, as I said above).
        //NB. paragraphIndexes is consumed during the execution of the method. In other words: at the end of the method it is empty.
    private static int paragraphCounter = 1;
    private static ArrayList<Content> createParagraphs(ArrayList<Content> contents, ArrayList<Element> paragraphIndexes)
    {
        ArrayList<Content> ret = new ArrayList<Content>();
        
        for(int i=0;i<contents.size();i++)
        {
            if((paragraphIndexes.size()==0)||(contents.get(i)!=paragraphIndexes.get(0))){ret.add(contents.get(i));continue;}
            
                //When we are here, we found a <paragraph-index>
            while(paragraphIndexes.isEmpty()==false)
            {
                Element paragraph = new Element("paragraph");
                ret.add(paragraph);
                paragraph.setAttribute("eId", "para_"+paragraphCounter);
                
                if(paragraphCounter==2)
                    paragraphCounter=paragraphCounter;
                
                Element num = new Element("num");
                paragraph.getContent().add(num);
                Element paragraphIndex = paragraphIndexes.remove(0);
                for(int j=0;j<paragraphIndex.getContent().size();j++)num.getContent().add(((Content)paragraphIndex.getContent().get(j)).clone());
                
                Element content = new Element("content");
                content.setAttribute("eId", "para_"+paragraphCounter+"__content");
                paragraph.getContent().add(content);
                
                ArrayList<Content> contentsOfContent = new ArrayList<Content>();
                for(i=i+1;(i<contents.size())&&((paragraphIndexes.size()==0)||(contents.get(i)!=paragraphIndexes.get(0)));i++)
                    contentsOfContent.add(contents.get(i));
                
                    //We try to identify <blockList>(s) within the <content> of the <paragraph>
                ArrayList<Element> elementsOfContent = Utilities.createBlockLists(contentsOfContent);
                
                    //Now, if there are Text(s), we insert them within a <p>.
                    //On the other hand, we insert Element(s) as they are.
                for(int j=0;j<elementsOfContent.size();j++)content.getContent().add(elementsOfContent.get(j));
                
                paragraphCounter++;
            }
        }
        
        return ret;
    }
    
/***************************************************************************************************************************************************************/    
/***************************************************************************************************************************************************************/
/*                                                                                                                                                             */
/*************************                   METHODS FOR ADJUSTING STRUCTURAL SDFTag(s) TO AKOMA NTOSO TAGS                   **********************************/
/*                                                                                                                                                             */
/***************************************************************************************************************************************************************/
/***************************************************************************************************************************************************************/
/*                                                                                                                                                             */
/*  The methods below change the tags assigned by the SDFTagger for the structural tagging into Akoma Ntoso tags. The one assigned by the SDFTagger  */
/*  includes all info; one of such SDFTag is, for instance "party-appellant", which marks all words that are both a party and an appellant. The SDFTag needs   */
/*  to be converted into <party eId="" as="appellant" refersTo="">...</party>, which is an Akoma Ntoso tag.                                                    */
/*                                                                                                                                                             */
/***************************************************************************************************************************************************************/    
    
        //This is the main procedures, which recalls the ones below. Note that the ones below take in input the ArrayList<Element>, and not the Element. This because 
        //it could be the case that some Element(s) can be adjusted multiple times; in order to make Element(s) to be adjusted more time... we simply add them to the 
        //ArrayList<Element> again. That's why it is given in input.
    protected static void adjustElements(Element judgment)
    {
        allOrganizations.clear();
        
        ArrayList<Element> elements = new ArrayList<Element>();
        elements.add(judgment);
        
        while(elements.isEmpty()==false)
        {
                //If the Element is spurious (this is checked in the method below), we simply ignore and proceed to the next.
            if(removeSpuriousElements(elements.get(0))==true){elements.remove(0);continue;}
            
                //Let's make sure each Element has the namespace required by Akoma Ntoso.
            elements.get(0).setNamespace(xmlns);
            
                //The following procedures all act on elements.get(0), but we pass the whole ArrayList for the reason explained at the beginning of the method.
            adjustTblockTags(elements);
            adjustDocType(elements);
            adjustParties(elements);
            adjustCompaniesAndInstitutions(elements);
            
                //Now we remove this element and we add all its children.
            Element element = elements.remove(0);
            for(int i=0;i<element.getContent().size();i++)
                if(element.getContent().get(i)instanceof Element)
                    elements.add((Element)element.getContent().get(i));
        }
    }
    
        //It could be the case that some Element(s) assigned by the SDFTagger... are still there! I.e., they have not been processed. For instance, in some cases 
        //the SDFTagger adds <paragraph-index> in the <conclusions>. That Element remains there, as the conclusions are not processed on the paragraphs, i.e., 
        //we don't call "createParagraphs" on the conclusions, but only "Utilities.createBlockListsAndP(contentsConclusions);". The method below removes these Element(s) 
        //and returns true when it does, so that from the calling method we know we can "continue;" to the next Element.
    private static boolean removeSpuriousElements(Element element)
    {
        if
        (
            (element.getName().compareToIgnoreCase("paragraph-index")!=0)&&
            (element.getName().compareToIgnoreCase("paragraph-index")!=0)//ADD HERE ALL OTHER SPURIOUS ELEMENT(S) WHEN YOU FIND THEM!
        )return false;
        
        Element parentElement = element.getParentElement();
        int index = parentElement.getContent().indexOf(element);
        parentElement.getContent().remove(index);
        
        for(int i=element.getContent().size()-1;i>=0;i--)
        {
            if(element.getContent().get(i)instanceof Element)parentElement.getContent().add(index, ((Element)element.getContent().get(i)).clone());
            else
            {
                Element p = new Element("p", xmlns);
                parentElement.getContent().add(index, p);
                p.getContent().add(((Text)element.getContent().get(i)).clone());
            }
        }
        
        return true;
    }
            
        //<tblock> (titled blocks) mark the titles of the several sections, subsections, paragraphs, etc. of the case law. 
        //They must include paragraphs <p>, not free text (they are not inline Element(s) of Akoma Ntoso!).
        //Furthermore, it could be possible that a <tblock> has been inserted as last element of the last <item> of a <blockList> or a <paragraph>; 
        //in such a case, we remove it from there: it must be inserted AFTER the <blockList> or <paragraph>.
    private static void adjustTblockTags(ArrayList<Element> elements)
    {
        Element element = elements.get(0);
            
        if(element.getName().compareToIgnoreCase("tblock")==0)
        {
            for(int i=0;i<element.getContent().size();i++)
            {
                Content content = (Content)element.getContent().get(i);
                if(content instanceof Text)
                {
                    int index = element.getContent().indexOf(content);
                    element.getContent().remove(index);
                    Element p = new Element("p", xmlns);
                    p.getContent().add(content.clone());
                    element.getContent().add(index, p);
                }
            }

                //Furthermore, it could be possible that a <tblock> has been inserted as last element of the last <item> of a <blockList> or a <paragraph>; 
                //in such a case, we remove it from there: it must be inserted AFTER the <blockList> or <paragraph>.
            Element parent = element.getParentElement();
            Element parentOfParent = parent.getParentElement();
            boolean isTheLastElementOfParent = false;
            if(parent.getContent().indexOf(element)==(parent.getContent().size()-1))isTheLastElementOfParent=true;
            boolean isParentTheLastElementOfTheParentOfParent = false;
            if(parentOfParent.getContent().indexOf(parent)==(parentOfParent.getContent().size()-1))isParentTheLastElementOfTheParentOfParent=true;

                //we set up a cycle, because we can have a <blockList> within a <paragraph>. The <tblock> must be moved out of both.
            while
            (
                (
                    (parent.getName().compareToIgnoreCase("item")==0)&&
                    (parentOfParent.getName().compareToIgnoreCase("blockList")==0)&&
                    (isTheLastElementOfParent==true)&&(isParentTheLastElementOfTheParentOfParent==true)
                )||
                (
                    (parent.getName().compareToIgnoreCase("content")==0)&&
                    (parentOfParent.getName().compareToIgnoreCase("paragraph")==0)&&
                    (isTheLastElementOfParent==true)&&(isParentTheLastElementOfTheParentOfParent==true)
                )
            )
            {
                parent.getContent().remove(element);
                
                    //PATCH! If we're removing the last Element in an <item>, we add a fake empty <p>, otherwise it won't be compliant with Akoma Ntoso XSD.
                    //Of course, if this if is true, the <item> had not to be built in the first place. But this is an error in the SDFRule(s) of the SDFTagger,
                    //now we simply repair on the fly the syntactic mistake, in order to have an Akoma Ntoso well-formed document. In the future, we'll have to 
                    //change also the SDFRule(s) in the SDFTagger, in order to avoid the whole construction of the item.
                if
                (
                    (parent.getName().compareToIgnoreCase("item")==0)&&
                    (parent.getContent().size()==1)&&
                    (parent.getContent().get(0)instanceof Element)&&
                    (((Element)parent.getContent().get(0)).getName().compareToIgnoreCase("num")==0)
                )parent.getContent().add(new Element("p"));
                
                Element parentOfParentOfParent = parentOfParent.getParentElement();
                
                    //We clone the Element, and we put it in the array (in the top)
                while(elements.remove(element)==true){}//we remove ALL occurences! It could be the case there are more than one...
                element = (Element)element.clone();
                elements.add(0, element);
                
                parentOfParentOfParent.getContent().add(parentOfParentOfParent.getContent().indexOf(parentOfParent)+1, element);

                    //IMPORTANT! There could be more <tblock>(s) at the end of the <paragraph>. For instance:
                    //<paragraph eId="para_16">
                    //    <num>16</num>
                    //    <content eId="para_16__content">
                    //    <p>...</p>
                    //    <tblock>
                    //      <p>Consideration of the question referred</p>
                    //    </tblock>
                    //    <tblock>
                    //      <p>Admissibility</p>
                    //    </tblock>
                    //  </content>
                    //</paragraph>
                    //With the above code, only <tblock><p>Admissibility</p></tblock> has been removed; to remove also <tblock><p>Consideration of ...</p></tblock>,
                    //we simply put the <content>, i.e., the variable "parent" in elements, so that it will be processed again.
                elements.add(parent);

                    //We set up the variables for a new cycle. Note that element has changed! Now there is its clone.
                parent = element.getParentElement();
                parentOfParent = parent.getParentElement();
                isTheLastElementOfParent = false;
                if(parent.getContent().indexOf(element)==(parent.getContent().size()-1))isTheLastElementOfParent=true;
                isParentTheLastElementOfTheParentOfParent = false;
                if(parentOfParent.getContent().indexOf(parent)==(parentOfParent.getContent().size()-1))isParentTheLastElementOfTheParentOfParent=true;
            }
        }
    }
    
        //We could have tagged some words as <tag>docType</tag> and <tag>pBeforeDocType</tag>.
        //Both need to be brought outside a <blockList> and <paragraph> if they occur therein as last Element.
        //Furthermore, <pBeforeDocType> needs to be transformed in a <p>, while <docType> needs to be enclosed in a <p>.
    private static void adjustDocType(ArrayList<Element> elements)
    {
        Element element = elements.get(0);
            
        if((element.getName().compareToIgnoreCase("docType")==0)||(element.getName().compareToIgnoreCase("pBeforeDocType")==0))
        {
                //It could be possible that a <docType> or the <pBeforeDocType> has been inserted as last element of the last <item> of a 
                //<blockList> or a <paragraph>; in such a case, we remove it from there: it must be inserted AFTER the <blockList> or <paragraph>.
                //This is of course the same code we used above for <tblock> (with the different that we can have <docType> and <pBeforeDocType>
                //only within a <blockList>, not within a <paragraph>.
            Element parent = element.getParentElement();
            Element parentOfParent = parent.getParentElement();
            boolean isTheLastElementOfParent = false;
            if(parent.getContent().indexOf(element)==(parent.getContent().size()-1))isTheLastElementOfParent=true;
            boolean isParentTheLastElementOfTheParentOfParent = false;
            if(parentOfParent.getContent().indexOf(parent)==(parentOfParent.getContent().size()-1))isParentTheLastElementOfTheParentOfParent=true;
            while
            (
                (
                    (parent.getName().compareToIgnoreCase("item")==0)&&
                    (parentOfParent.getName().compareToIgnoreCase("blockList")==0)&&
                    (isTheLastElementOfParent==true)&&(isParentTheLastElementOfTheParentOfParent==true)
                )
            )
            {
                parent.getContent().remove(element);                    
                Element parentOfParentOfParent = parentOfParent.getParentElement();
                element = (Element)element.clone();
                parentOfParentOfParent.getContent().add(parentOfParentOfParent.getContent().indexOf(parentOfParent)+1, element);
                elements.add(parent);
                parent = element.getParentElement();
                parentOfParent = parent.getParentElement();
                isTheLastElementOfParent = false;
                if(parent.getContent().indexOf(element)==(parent.getContent().size()-1))isTheLastElementOfParent=true;
                isParentTheLastElementOfTheParentOfParent = false;
                if(parentOfParent.getContent().indexOf(parent)==(parentOfParent.getContent().size()-1))isParentTheLastElementOfTheParentOfParent=true;
            }

                //Now, if it is a <pBeforeDocType>, it must be converted in a <p>. Of course, unless they are still within an <item> and they are not already within a <p>
            parent = element.getParentElement();
            if
            (
                (element.getName().compareToIgnoreCase("pBeforeDocType")==0)&&
                ((parent.getName().compareToIgnoreCase("item")!=0)&&(parent.getName().compareToIgnoreCase("p")!=0))||
                    //this is allowed: we're within an <item> but this is not the last Element of the item
                ((parent.getName().compareToIgnoreCase("item")==0)&&(isTheLastElementOfParent==false))
            )
            {      
                int index = parent.getContent().indexOf(element);
                parent.getContent().remove(index);
                Element p = new Element("p", xmlns);
                parent.getContent().add(index, p);
                for(int i=0;i<element.getContent().size();i++)
                    p.getContent().add(((Content)element.getContent().get(i)).clone());
            }
            
                //If it is a <docType> it must be enclosed in a <p>.
            else if(element.getName().compareToIgnoreCase("docType")==0)
            {
                int index = parent.getContent().indexOf(element);
                parent.getContent().remove(index);
                Element p = new Element("p", xmlns);
                parent.getContent().add(index, p);
                p.getContent().add(element.clone());
            }
        }
    }
    
        //This method changes the SDFTags: <tag>party-appellant</tag>, <tag>party-respondent</tag>, <tag>party-intervening</tag>.
        //It transforms them into the Akoma Ntoso tags <party eId="" as="appellant" refersTo="">...</party>, 
        //<party eId="" as="respondent" refersTo="">...</party> and <party eId="" as="intervening" refersTo="">...</party>.
    private static void adjustParties(ArrayList<Element> elements)
    {
        Element element = elements.get(0);
            
        if(element.getName().indexOf("party-")==0)
        {
            String as = element.getName().substring(element.getName().indexOf("-")+1, element.getName().length());
            String refersTo = as.substring(as.indexOf("-")+1, as.length());
            as = as.substring(0, as.indexOf("-"));
                    
            Element parent = element.getParentElement();
            int index = parent.getContent().indexOf(element);
            parent.getContent().remove(index);
         
            Element party = new Element("party", xmlns);
            party.setAttribute("as", as);
            party.setAttribute("refersTo", "#"+refersTo);
            for(int i=0;i<element.getContent().size();i++)party.getContent().add(((Content)element.getContent().get(i)).clone());
         
            if(refersTo.trim().isEmpty()==false)
            {
                boolean isThere = false;
                for(int i=0;(i<allOrganizations.size())&&(isThere==false);i++)if(allOrganizations.get(i).compareToIgnoreCase(refersTo)==0)isThere=true;
                if(isThere==false)allOrganizations.add(refersTo);
            }
            
                //<party> is an in-line tag: it must be enclosed in a <p>
            Element p = new Element("p", xmlns);
            p.getContent().add(party);
            parent.getContent().add(index, p);
        }
    }
    
        //This method convert all "company_*" and "institution_*" which are recognized within the text outside the <party>(s) into <organization>.
        //It then sets the attribute refersTo to the specific individual of the ontology.
        //Finally, we populate the allOrganizations with all companies and institutions (they are needed to create the meta).
    protected static ArrayList<String> allOrganizations = new ArrayList<String>();
    private static void adjustCompaniesAndInstitutions(ArrayList<Element> elements)
    {
        Element element = elements.get(0);
            
        if((element.getName().indexOf("company_")==0)||(element.getName().indexOf("institution_")==0))
        {
            String refersTo = element.getName();
            boolean isThere = false;
            for(int i=0;(i<allOrganizations.size())&&(isThere==false);i++)if(allOrganizations.get(i).compareToIgnoreCase(refersTo)==0)isThere=true;
            if(isThere==false)allOrganizations.add(refersTo);
            
            Element parent = element.getParentElement();
            int index = parent.getContent().indexOf(element);
            parent.getContent().remove(index);
            
            Element organization = new Element("organization", xmlns);
            organization.setAttribute("refersTo", "#"+refersTo);
            for(int k=0;k<element.getContent().size();k++)organization.getContent().add(((Content)element.getContent().get(k)).clone());
            Element p = new Element("p", xmlns);
            p.getContent().add(organization);
            parent.getContent().add(index, p);
        }
    }
}