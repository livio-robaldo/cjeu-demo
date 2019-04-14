//Copyright 2014 Livio Robaldo
//This file is part of CJEUprocessorDEMO. The demo uses the SDFTagger library, whose source code can be obtained by contacting Livio Robaldo (http://www.liviorobaldo.com)
//CJEUprocessorDEMO is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software 
//Foundation, either version 3 of the License, or (at your option) any later version.
//CJEUprocessorDEMO is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
//PARTICULAR PURPOSE. See the GNU General Public License for more details.
//You should have received a copy of the GNU General Public License along with CJEUprocessorDEMO.  If not, see <http://www.gnu.org/licenses/>.

package B_CJEU_Parsing;
import StanfordConLL.DependencyLabel;
import StanfordConLL.ParseSentences;
import java.io.*;
import java.util.*;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
We use the the STANFORD CORE NLP 3.8.0 to parse the crawled CJEU case law. With an exception: I've added the POS "Punctuation" for commas, dots, etc. I remind
that the Stanford parser assign these symbols of puntuation the same class as the form, e.g. "FORM: ., LEMMA: ., POS: ., GOV: 3, LABEL: punct"

In the output document we have a single set of dependency trees. So we obtain an XML document with the following structure:

<texts>
    <DependencyTrees>
        <DependencyTree>
              ...
        </DependencyTree>
        <DependencyTree>
              ...
        </DependencyTree>
              ...
    </DependencyTrees>
</texts>

To obtain it, we proceed in TWO STEPS.

----------------------------------------
STEP (1): we polish the input XML document, which is an HTML crawled from the CJEU portal. Specifically, STEP (1) returns a *flat* sequence of Element(s) <p>.
Some of these <p> are marked, because, for instance, we want to keep the titles, the bold font, etc. Specifically, at the end of STEP1, we'll have, for instance:

<p class="title">Findings of the Court</p>
<p class="bold">European Commission</p>

Together with normal (unmarked) paragraphs, e.g.:

<p>, represented by A. Rubio González and L. Banciella Rodríguez-Miñón, acting as Agents,</p>

The list of available marks on the <p> Element(s) is listed below in the method:

**********************************************************************
private static void markP(Element pNew, Element pOld)
**********************************************************************

IF YOU WANT TO ADD MORE, GO TO THAT METHOD!
----------------------------------------
STEP (2): we parse the text in the <p> Element(s) via STANFORD CORE NLP 3.8.0 and we build the output <DependencyTrees>.

We keep the marking on the font (title, bold, etc.) as optional features of the <line>(s).
----------------------------------------
/**/

/**
If the sentence is too long, the Stanford Parser prints on System.out the following message:

"[main] WARN edu.stanford.nlp.pipeline.ParserAnnotator - Parsing of sentence failed, possibly because of out of memory.  Will ignore and continue:"

BUT! An exception is not thrown: the parser returns a tree with all nodes connected to the same root (the root is taken as the last word that is not punctuation:
all nodes are set as its dependent).

So, the computation can continue: we just have (some) dummy trees in the output.

In order to avoid it, we should detect the "[main] WARN" above. One way to do it is to redirect System.out in order to catch the messages of the Stanford Parser:

https://www.geeksforgeeks.org/redirecting-system-out-println-output-to-a-file-in-java/
/**/


public class convertXMLfiles 
{
    private static File inputFolder = new File("./CORPUS/0 - INPUT/");
    private static File outputFolder = new File("./CORPUS/1 - PARSED INPUT");
    
//----------------------------------------------------------------------------------------------------------------------------------------------------------------
// These are parameters of the Stanford parser. Further notes about the Stanford parser are at the end of the file. 
// Note that I added the POS "Punctuation".
    private static String annotators = "tokenize, ssplit, pos, lemma, ner, parse";                        
    private static DependencyLabel label = DependencyLabel.BASIC;
    //private static DependencyLabel label = DependencyLabel.ENACHED;
    //private static DependencyLabel label = DependencyLabel.ENACHED_PLUS;
    
        //if this boolean is true, "nmod:poss" -> "nmod"
    private static boolean nosublabels = false;
//----------------------------------------------------------------------------------------------------------------------------------------------------------------
    
        //If this is set to true, it deletes all files in outputFolder and recreates them. 
        //If this is set to false, it only creates the ones that are in inputFolder but not in outputFolder
    private static boolean deleteOldFiles = false;
    public static void main(String[] args)
    {
        try 
        {
                //We remove all files from the output directory, if the boolean deleteOldFiles is set to true.
            if(deleteOldFiles==true)
            {
                ArrayList<File> tempFiles = new ArrayList<File>();
                for(int i=0;i<outputFolder.listFiles().length;i++)
                    tempFiles.add(outputFolder.listFiles()[i]);
                for(int i=0;i<tempFiles.size();i++)if(tempFiles.get(i).isDirectory()==false)while(tempFiles.get(i).exists())tempFiles.get(i).delete();
            }
            
                //We convert each case law with the converter 
            for(int i=0;i<inputFolder.listFiles().length;i++)
            {
                File outputFile = new File(outputFolder.getAbsolutePath()+"/"+inputFolder.listFiles()[i].getName());
                if(outputFile.exists())continue;
                
                try
                {
                    if(inputFolder.listFiles()[i].isDirectory()==true)continue;
                    String ECLI = inputFolder.listFiles()[i].getName().replaceAll("_", ":");
                    System.out.println("Converting CJEU case law "+ECLI+" in Akoma Ntoso");
                    
                        //We collect all Element(s) below <body> in the ArrayList<Element> "elements". Then, we pass it
                        //to the methods defined below to extract the Element that need to be inserted in a section.
                    Document inputDoc = (Document) new SAXBuilder().build(inputFolder.listFiles()[i]);
                    Element body = inputDoc.getRootElement();
                    ArrayList<Element> elements = getAllDirectChildElements(body);

                        //STEP (1): we clean all these Elements, with the method defined below in this class.
                    ArrayList<Element> paragraphs = polishElements(elements);
                            
                        //STEP (2) We instantiate the Stanford Parser and we use it to create the Element <DependencyTrees>.
                    ParseSentences StanfordParser = new ParseSentences(annotators);
                    Element DependencyTrees = createDependencyTreesWithStanfordParser(paragraphs, StanfordParser);
                    
                        //FINAL STEP: We write everything in the XML output file
                    Element texts = new Element("texts");
                    //for(int j=0;j<paragraphs.size();j++)texts.getContent().add(paragraphs.get(j));
                    texts.getContent().add(DependencyTrees);
                    Document outputDoc = new Document();
                    outputDoc.setRootElement(texts);
                    XMLOutputter outputter = new XMLOutputter();
                    outputter.setFormat(Format.getPrettyFormat().setEncoding("UTF-8"));
                    FileOutputStream fos = new FileOutputStream(outputFile);
                    OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
                    outputter.output(outputDoc, osw);
                    osw.close();
                    fos.close();
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
    
    
        //This method returns all the child elements nested directly (one level deep, i.e. direct child).
        //Therefore, it is equivalent to "element.getChildren()", with the difference that *WE DO NOT ACCEPT Text(s) AS CHILDREN!
        //With "element.getChildren()" we can indeed have Text(s) (the method does not simply return them), here instead we 
        //don't want them: if we found one, we raise an exception.
    
        //This method is used from the main (to extract all Element(s) of <body> and from the "UTILITIES TO POLISH THE INPUT ELEMENT(S)",
        //for instance to extract all Element(s) from a <table> cell. 
    private static ArrayList<Element> getAllDirectChildElements(Element element)throws Exception
    {
        ArrayList<Element> ret = new ArrayList<Element>();
        
        
        for(int i=0;i<element.getContent().size();i++)
        {
            if(element.getContent().get(i)instanceof Element)ret.add((Element)element.getContent().get(i));
                
                //if we find Text, this must be empty
            else if(element.getContent().get(i)instanceof Text)
            {
                if(((Text)element.getContent().get(i)).getText().trim().replaceAll(" ", "").isEmpty()==true)continue;
                throw new Exception("The text \""+((Text)element.getContent().get(i)).getText().trim()+"\" is not expected in "+element.getName());
            }
            
            else throw new Exception("The object "+element.getContent().get(i).getClass().getName()+" is not expected in "+element.getName());
        }
        
        return ret;
    }

/******************************************************************************************************************************************/
/*****************************************                                                        *****************************************/
/*****************************************        UTILITIES TO POLISH THE INPUT ELEMENT(S)        *****************************************/
/*****************************************                                                        *****************************************/
/******************************************************************************************************************************************/
    
    /*
    The input Elements are expected to be:
        - Paragraphs <p>
        - Tables <table>
        - <hr class="note" />
    
    The methods below transform each of them in Element(s) <p> that are compliant with the XSD of Akoma Ntoso. 
    
    - Some input text is in *BOLD* font. We want to preserve this in the output file, as we assume that bold font mark named entities, so that's an information we need
      to keep for the next module. Bold text is inserted in <p> Element(s) where we set the attribute class="ne", i.e., we insert this text in "<p class="ne">" 
      (the attribute class is compliant with the Akoma Ntoso XSD, although maybe it is used for other purposes...).
    
    - Some input text mark roles, e.g., <p class="pstatus">applicant,</p>, <p class="pstatus">defendant,</p>, ...
    
    /**/
    
    private static ArrayList<Element> polishElements(ArrayList<Element> elements)throws Exception
    {
        ArrayList<Element> ret = new ArrayList<Element>();
        
        for(int i=0;i<elements.size();i++)
        {
            Element element = elements.get(i);
            
                //paragraph(s)
            if(element.getName().compareToIgnoreCase("p")==0)
            {
                ArrayList<Element> temp = polishParagraph(element);
                for(int j=0;j<temp.size();j++)ret.add(temp.get(j));
            }
            
                //table(s)
            else if(element.getName().compareToIgnoreCase("table")==0)
            {
                ArrayList<Element> temp = polishTable(element);
                for(int j=0;j<temp.size();j++)ret.add(temp.get(j));
            }            
            
                //For hr, we simply check it is empty
            else if(element.getName().compareToIgnoreCase("hr")==0)
            {
                if(extractTextFromElement(element).isEmpty()==false)
                    throw new Exception("The Element "+element.getName()+" is not empty (as we expected it to be)");
            }
            
            else throw new Exception("No rules defined to handle Element "+element.getName()+" within Element <body> while building the <header>");
        }
            
        return ret;
    }
    
        /*
            Paragraphs may contain <span class="bold"> that mark text in bold as well as other Element(s), e.g. links (<a>). We convert the <span class="bold"> 
            in <p class="bold">, i.e., we keep the attribute class="bold" for the next module. Note that we can return more than one <p>. For instance:
        
            <p class="normal">
                <span class="bold">Kingdom of Spain</span>
                , represented by A. Rubio González and L. Banciella Rodríguez-Miñón, acting as Agents,
            </p>
        
            is converted in two <p>(s):

            <p class="bold">Kingdom of Spain</p>
            <p>, represented by A. Rubio González and L. Banciella Rodríguez-Miñón, acting as Agents,</p>
  
            Note that not all bold are named entities, e.g. <p class="bold">Judgment</p>. This will be converted within headerCreator into:
    
                <p>
                    <docType>Judgment</docType>
                </p>
    
            Other markers we are interested to keep are:

            - <p class="title-grseq-2"> or <p class="sum-title-1">     --->       all converted in class="title"
            - <p class="pstatus">defendant,</p>     ---->     where class="pstatus" marks a role

            NOTE THAT IN MANY CASES THERE IS A COMMA AT THE END OF THE MARKER, E.G. "<p class="pstatus">defendant,</p>", WHICH SHOULD CORRESPOND TO TWO <p>(s):
            <p class="pstatus">defendant</p><p>,</p>. In this phase we keep these commas, they will be processed, i.e., removed from <p> in the next module.

        /**/
    
        //</p>
    private static ArrayList<Element> polishParagraph(Element pOld)throws Exception 
    {
        ArrayList<Element> ret = new ArrayList<Element>();
        
        ArrayList<Content> contents = new ArrayList<Content>();
        for(int i=0;i<pOld.getContent().size();i++)
        {
            if((pOld.getContent().get(i)instanceof Text)||(pOld.getContent().get(i)instanceof Element))contents.add((Content)pOld.getContent().get(i));
            else throw new Exception("Unrecognized JDOM Content: only Text and Element are recognized");
        }
        
            //This is slighty different from the methods below, because here we consider also Text(s)
        ArrayList<String> buffer = new ArrayList<String>();
        for(int i=0;i<contents.size();i++)
        {
            Content content = contents.get(i);
            
                //If we find Text, we collect its textual content in the buffer
            if(content instanceof Text)
            {
                String text = ((Text)content).getText().trim();
                    //unbelievable ... with this kind of blank, trim() does not work ...
                while((text.length()>0)&&(text.charAt(0)==' '))text=text.substring(1, text.length());
                while((text.length()>0)&&(text.charAt(text.length()-1)==' '))text=text.substring(0, text.length()-1);
                if(((Text)content).getText().trim().isEmpty()==false)buffer.add(text);
            }
                //If we find an Element, we first drop the buffer if it contains Strings, then we further process the Element
            else if(content instanceof Element)
            {
                    //we empty the buffer
                if(buffer.isEmpty()==false)
                {
                    Element pNew = new Element("p");
                    String text = "";
                    while(buffer.isEmpty()==false)text=text+" "+buffer.remove(0);
                    pNew.getContent().add(new Text(text));
                    markP(pNew, pOld);//we set the attributes wrt the main <p>
                    ret.add(pNew);
                }
                
                    //Now we can have either <span> or <a>. Both are considered leaves, so that we extract their text.
                String newText = extractTextFromElement((Element)content);
                if(newText.isEmpty()==false)
                {
                    Element pNew = new Element("p");
                    pNew.getContent().add(new Text(newText));
                    ret.add(pNew);
                    
                    if(newText.compareToIgnoreCase("IV.")==0)
                        ret=ret;

                        //<span>
                    if(((Element)content).getName().compareToIgnoreCase("span")==0)
                    {
                            //Possiamo avere un "bold" nello <span> ed un "title" nel <p> che contiene lo <span>.
                            //Il secondo sovrascriverà il primo, ovviamente ...
                        markP(pNew, (Element)content);
                        markP(pNew, pOld);
                    }
                    
                        //<a>
                    if(((Element)content).getName().compareToIgnoreCase("a")==0)
                    {
                        //if it's a link ... we don't do anything (in the current version, but maybe in the future we'll change...)
                        //NOTE!!! Also within <span> there could be <a>, e.g. <span class="note"><a id="c-ECR_62..." href="#t-ECR_620...">*1</a></span>
                    }
                }

                    //This instruction needs to stay here! As we ask "(buffer.isEmpty())" within the branch of the Element(s)
                buffer.clear();
            }
            else 
                throw new Exception("Unrecognized JDOM Content: only Text and Element are recognized");
        }
        
            //At the end, we could have a non-empty buffer...
        if(buffer.isEmpty()==false)
        {
            Element pNew = new Element("p");
            String text = "";
            while(buffer.isEmpty()==false)text=text+" "+buffer.remove(0);
            pNew.getContent().add(new Text(text));
            markP(pNew, pOld);//we set the attributes wrt the main <p>
            ret.add(pNew);
            buffer.clear();
        }
        
        return ret;
    }

        //Small UTILITY that checks the existence of certain attributes (only THREE for now).
        //If these attributes are found, the new <p> is marked.
        //Note that there is "else if": they are ordered. For instance, if a <p> is both "title" and "bold", we only leave "title"
    private static void markP(Element pNew, Element pOld)
    {
        if((pOld.getAttributeValue("class")!=null)&&(pOld.getAttributeValue("class").indexOf("title")!=-1)) 
            pNew.setAttribute("class", "title");
        else if((pOld.getAttributeValue("class")!=null)&&(pOld.getAttributeValue("class").compareToIgnoreCase("pstatus")==0)) 
            pNew.setAttribute("class", "pstatus");
        else if((pOld.getAttributeValue("class")!=null)&&(pOld.getAttributeValue("class").indexOf("signature")!=-1)) 
            pNew.setAttribute("class", "signature");
        else if((pOld.getAttributeValue("class")!=null)&&(pOld.getAttributeValue("class").compareToIgnoreCase("bold")==0))
            pNew.setAttribute("class", "bold");
    }
    
        //<table>
    private static ArrayList<Element> polishTable(Element table)throws Exception 
    {
        ArrayList<Element> ret = new ArrayList<Element>();
        
            //We expect Element(s) within a <table> to be:
            // - <col>        ---->        This is expected to be empty
            // - <tr>         ---->        This is expected to contain <td>
            // - <tbody>      ---->        This is the real content of the table ... we just apply recursion.
        ArrayList<Element> elements = getAllDirectChildElements(table);
        for(int i=0;i<elements.size();i++)
        {
            Element element = elements.get(i);
            
                //if we find a <col>, this has to be empty
            if(element.getName().compareToIgnoreCase("col")==0)
            {
                if(extractTextFromElement(element).isEmpty()==false)
                    throw new Exception("The Element "+element.getName()+" is not empty (as we expected it to be)");
            }
            
                //<tr>
            else if(element.getName().compareToIgnoreCase("tr")==0)
            {
                ArrayList<Element> temp = polishTableRow(element);
                for(int j=0;j<temp.size();j++)ret.add(temp.get(j));
            }
            
                //<tbody>, we just apply recursion
            else if(element.getName().compareToIgnoreCase("tbody")==0)
            {
                ArrayList<Element> temp = polishTable(element);
                for(int j=0;j<temp.size();j++)ret.add(temp.get(j));
            }
            
            else throw new Exception("No rules defined to handle Element "+element.getName()+" within Element <table> while building the <header>");
        }
        
        return ret;
    }

        //<tr>
    private static ArrayList<Element> polishTableRow(Element tableRow)throws Exception 
    {
        ArrayList<Element> ret = new ArrayList<Element>();

        ArrayList<Element> elements = getAllDirectChildElements(tableRow);
        for(int i=0;i<elements.size();i++)
        {
            Element element = elements.get(i);
            
                //Within table rows (<tr>), we can only find table cells <td>(s) ...
            if(element.getName().compareToIgnoreCase("td")==0)
            {
                ArrayList<Element> temp = polishTableCell(element);
                for(int j=0;j<temp.size();j++)ret.add(temp.get(j));
            }
            
            else throw new Exception("No rules defined to handle Element "+element.getName()+" within Element <tr> while building the <header>");
        }
        
        return ret;
    }
    
        //<td>
    private static ArrayList<Element> polishTableCell(Element tableCell)throws Exception 
    {
        ArrayList<Element> ret = new ArrayList<Element>();
        
            //We expect Element(s) within a table cell to be paragraphs (<p>), or tables (<table>), or <div>, or <img>
            //To deal with them, we recursively apply the methods above.
        ArrayList<Element> elements = getAllDirectChildElements(tableCell);
        for(int i=0;i<elements.size();i++)
        {
            Element element = elements.get(i);
            
                //p
            if(element.getName().compareToIgnoreCase("p")==0)
            {
                ArrayList<Element> temp = polishParagraph(element);
                for(int j=0;j<temp.size();j++)ret.add(temp.get(j));
            }
            
                //table
            else if(element.getName().compareToIgnoreCase("table")==0)
            {
                ArrayList<Element> temp = polishTable(element);
                for(int j=0;j<temp.size();j++)ret.add(temp.get(j));
            }
            
                //if there is a <div> (usually only <div class="signaturecase"> at the bottom), we simply put all its Element children in elements.
                //But! We add the class "signature" on the first of it, if there is on the <div> (i.e., we migrate it from the <div> to the first <p>
            else if(element.getName().compareToIgnoreCase("div")==0)
            {
                if((element.getAttributeValue("class")!=null)&&(element.getAttributeValue("class").toLowerCase().indexOf("signature")!=-1))
                    element=element;
                
                for(int j=element.getContent().size()-1;j>=0;j--)
                {
                    if(element.getContent().get(j)instanceof Element)
                    {
                        if
                        (
                            (element.getAttributeValue("class")!=null)&&
                            (element.getAttributeValue("class").toLowerCase().indexOf("signature")!=-1)
                        )((Element)element.getContent().get(j)).setAttribute("class", "signature");
                        
                        elements.add(i+1, (Element)element.getContent().get(j));
                    }
                }
            }
            
                //<img> is simply ignored.
            else if(element.getName().compareToIgnoreCase("img")==0)
            {
                continue;
            }
            
            else throw new Exception("No rules defined to handle Element "+element.getName()+" within Element <td> while building the <header>");
        }
        
        return ret;
    }
    
    private static String extractTextFromElement(Element e)throws Exception 
    {
        String ret = "";
        
        for(int i=0;i<e.getContent().size();i++)
        {
            if(e.getContent().get(i)instanceof Text)ret=ret.trim()+((Text)e.getContent().get(i)).getText().trim();
            else if(e.getContent().get(i)instanceof Element)ret=ret.trim()+extractTextFromElement((Element)e.getContent().get(i));
            else throw new Exception("Unrecognized JDOM Content: only Text and Element are recognized");
        }
        
            //unbelievable ... with this kind of blank, trim() does not work ...
        while((ret.length()>0)&&(ret.charAt(0)==' '))ret=ret.substring(1, ret.length());
        while((ret.length()>0)&&(ret.charAt(ret.length()-1)==' '))ret=ret.substring(0, ret.length()-1);
        
        return ret.trim();
    }
    
/******************************************************************************************************************************************/
/*****************************************                                                        *****************************************/
/*****************************************      END UTILITIES TO POLISH THE INPUT ELEMENT(S)      *****************************************/
/*****************************************                                                        *****************************************/
/******************************************************************************************************************************************/

/******************************************************************************************************************************************/
/*****************************************                                                        *****************************************/
/*****************************************           UTILITIES TO PARSE THE PARAGRAPH(S)          *****************************************/
/*****************************************                                                        *****************************************/
/******************************************************************************************************************************************/
    private static Element createDependencyTreesWithStanfordParser(ArrayList<Element> paragraphs, ParseSentences StanfordParser) throws Exception
    {
        Element DependencyTrees = new Element("DependencyTrees");
        
        for(int i=0;i<paragraphs.size();i++)
        {
            String font = "normal";
            if(paragraphs.get(i).getAttributeValue("class")!=null)font = paragraphs.get(i).getAttributeValue("class");
            
            ArrayList<Element> newDependencyTrees = parseText(extractTextFromElement(paragraphs.get(i)).trim(), font, StanfordParser);
            for(int j=0;j<newDependencyTrees.size();j++)DependencyTrees.getContent().add(newDependencyTrees.get(j));
        }
        
        return DependencyTrees;
    }
            
    
    
        //This method takes a Text and returns a set of Element(s) <DependencyTree>. The <line>(s) of the <DependencyTree>(s) contains two optional features:
        // - <blanksBefore>: specify the number of blanks before the word (usually, this number is either 0 or 1)
        // - <font>: specify whether the Text has some special font (bold, title, etc. see method "markP" in the previous set of utilities). The font is a parameter
        //   of the method; it could be "normal", so that we specify <font>normal</font> for each <line>.
    private static ArrayList<Element> parseText(String text, String font, ParseSentences StanfordParser) throws Exception
    {
        text = text.trim().replaceAll("’", "'");
        
        System.out.println("Parsing: \""+text+"\"");
        
        ArrayList<Element> ret = new ArrayList<Element>();
        
        if(text.isEmpty())return ret;
        
        ArrayList<ArrayList<ArrayList<String[]>>> analyses = StanfordParser.parseSentenceAndGetDependencies(text, label);
        for(int j=0;j<analyses.size();j++)
        {
            ArrayList<ArrayList<String[]>> analysis = analyses.get(j);
            if(analysis.size()==0)continue;
            
            Element DependencyTree = new Element("DependencyTree");
            for(int k=0;k<analysis.size();k++)
            {
                Element line = new Element("line");
                ArrayList<String[]> slots = analysis.get(k);
                for(int z=0;z<slots.size();z++)
                {
                    String[] slot = slots.get(z);

                    String attribute = slot[0];
                    String value = slot[1];

                    String blanksBefore = null;

                    Element temp = null;

                        //These attributes don't require any further process on the value: we simply create the Element
                    if(attribute.compareToIgnoreCase("SENTENCE")==0){continue;}
                    else if(attribute.compareToIgnoreCase("SURFPOS")==0){line.setAttribute("eId", slot[1]);continue;}
                    else if(attribute.compareToIgnoreCase("LEMMA")==0)temp=new Element("Lemma");
                    else if(attribute.compareToIgnoreCase("FORM")==0)
                    {
                        String[] bbTemp = getBlankBefore(value, text);

                        blanksBefore = bbTemp[0];
                        text = bbTemp[1];

                        temp=new Element("Form");
                    }
                    else if(attribute.compareToIgnoreCase("POS")==0)
                    {
                        temp=new Element("POS");

                            //Qui sotto le POS del Penn Treebank (http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html).
                            //Se non è nessuna di esse, è un segno di punteggiatura (punti, virgole, etc.), che mettiamo nella POS "Punctuation"
                            //Leggi al fondo di questo file, se vuoi saperne di più ...
                        if
                        (
                            (value.compareToIgnoreCase("CC")!=0)&&
                            (value.compareToIgnoreCase("CD")!=0)&&
                            (value.compareToIgnoreCase("DT")!=0)&&
                            (value.compareToIgnoreCase("EX")!=0)&&
                            (value.compareToIgnoreCase("FW")!=0)&&
                            (value.compareToIgnoreCase("IN")!=0)&&
                            (value.compareToIgnoreCase("JJ")!=0)&&
                            (value.compareToIgnoreCase("JJR")!=0)&&
                            (value.compareToIgnoreCase("JJS")!=0)&&
                            (value.compareToIgnoreCase("LS")!=0)&&
                            (value.compareToIgnoreCase("MD")!=0)&&
                            (value.compareToIgnoreCase("NN")!=0)&&
                            (value.compareToIgnoreCase("NNP")!=0)&&
                            (value.compareToIgnoreCase("NNPS")!=0)&&
                            (value.compareToIgnoreCase("NNS")!=0)&&
                            (value.compareToIgnoreCase("PDT")!=0)&&
                            (value.compareToIgnoreCase("POS")!=0)&&
                            (value.compareToIgnoreCase("PRP$")!=0)&&
                            (value.compareToIgnoreCase("PRP")!=0)&&
                            (value.compareToIgnoreCase("RB")!=0)&&
                            (value.compareToIgnoreCase("RBR")!=0)&&
                            (value.compareToIgnoreCase("RBS")!=0)&&
                            (value.compareToIgnoreCase("RP")!=0)&&
                            (value.compareToIgnoreCase("SYM")!=0)&&
                            (value.compareToIgnoreCase("TO")!=0)&&
                            (value.compareToIgnoreCase("UH")!=0)&&
                            (value.compareToIgnoreCase("VB")!=0)&&
                            (value.compareToIgnoreCase("VBD")!=0)&&
                            (value.compareToIgnoreCase("VBG")!=0)&&
                            (value.compareToIgnoreCase("VBN")!=0)&&
                            (value.compareToIgnoreCase("VBP")!=0)&&
                            (value.compareToIgnoreCase("VBZ")!=0)&&
                            (value.compareToIgnoreCase("WDT")!=0)&&
                            (value.compareToIgnoreCase("WP$")!=0)&&
                            (value.compareToIgnoreCase("WP")!=0)&&
                            (value.compareToIgnoreCase("WRB")!=0)
                        )value="Punctuation";
                    }
                    else if(attribute.compareToIgnoreCase("GOV")==0)
                    {
                        temp=new Element("Governor");
                    }
                    else if(attribute.compareToIgnoreCase("LABEL")==0)
                    {
                        temp=new Element("Label");

                            //togliamo i subtypes dalle labels se nosublabels è true!
                        if((value.indexOf(":")!=-1)&&(nosublabels==true))value=value.substring(0, value.indexOf(":"));
                    }
                    else 
                        throw new Exception("Irreversibilissimus errur!");

                    line.getContent().add(temp);
                    temp.getContent().add(new Text(value));

                        //If this is true, it's because we just inserted a Form. So, we just calculated the blanksBefore, which need to be inserted as well.
                    if(blanksBefore!=null)
                    {
                        Element tempE = new Element("blanksBefore");
                        tempE.getContent().add(new Text(blanksBefore));
                        line.getContent().add(tempE);
                    }

                    if(slot[1].indexOf("nsubjpass")!=-1)
                        slot=slot;
                }

                if(line.getContent().size()==0)continue;
                
                    //We add the font
                Element Font = new Element("Font");
                Font.getContent().add(new Text(font));
                line.getContent().add(Font);
                
                DependencyTree.getContent().add(line);
            }
            
            if(DependencyTree.getContent().size()==0)
                continue;
            
            ret.add(DependencyTree);
        }
        
        return ret;
    }
    
        //Returns the blanks before, by searching the first occurrence of Form within text.
    private static String[] getBlankBefore(String Form, String text)throws Exception
    {
        if(text.indexOf("Acqua")!=-1)
            text=text;
                            
        String[] ret = new String[2];
        
            //defeault is 1 (if there's some error below, we add a blank. Better "hello hello , hello" than "hellohello, hello" or "hellohello,hello"
        int blanksBefore = 1;
        
        int index = text.indexOf(Form);
        if(index!=-1)
        {
            blanksBefore = 0;
            while
            (
                (index>0)&&
                (
                        //Ci sono vari tipi di blank. Quando ne becchiamo uno, lo elenchiamo qui.
                    (text.charAt(index-1)==' ')||//questo ha unicode=32
                    (text.charAt(index-1)==' ')//questo ha unicode=160
                )
            )
            {
                index--;
                blanksBefore++;
            }
            text = text.substring(text.indexOf(Form)+Form.length(), text.length());
        }
        
        ret[0] = ""+blanksBefore;
        ret[1] = text;
        return ret;
    }
}

/*
NOTES (in Italian):

La classi basi del Stanford CORE sono quelle del Penn Treebank:

http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html

Almeno questa è la versione dello Stanford CORE che ho io ...

Inoltre, mettiamo la POS "Punctuation" per i segni di punteggiatura. Come si vede, ad esempio qui:

( (S 
    (NP-SBJ 
      (NP (NNP Pierre) (NNP Vinken) )
      (, ,) 
      (ADJP 
        (NP (CD 61) (NNS years) )
        (JJ old) )
      (, ,) )
    (VP (MD will) 
      (VP (VB join) 
        (NP (DT the) (NN board) )
        (PP-CLR (IN as) 
          (NP (DT a) (JJ nonexecutive) (NN director) ))
        (NP-TMP (NNP Nov.) (CD 29) )))
    (. .) ))

i segni di punteggiatura (punti, virgole, etc.) non hanno una POS propria ... ci mette il punto e la virgola stessa come POS. 
E così fa lo Stanford CORE:

SURFPOS: 8, FORM: ,, LEMMA: ,, POS: ,, GOV: 4, LABEL: punct, 
SURFPOS: 12, FORM: ., LEMMA: ., POS: ., GOV: 2, LABEL: punct, 
*/