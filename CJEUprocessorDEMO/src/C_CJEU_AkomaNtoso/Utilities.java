package C_CJEU_AkomaNtoso;

import SDFTagger.SDFItems.SDFHead;
import SDFTagger.SDFItems.SDFDependencyTree;
import SDFTagger.SDFItems.SDFTag;
import java.util.*;
import org.jdom2.*;

public class Utilities 
{
        //This procedure looks for the Tags assigned by the same (instance of) SDFRule. It assumes the Tag.tag with which we mark every SDFHead is the *SAME*,
        //i.e. we cannot have, within the same SDFRule, an SDFHead associated with "XXX" and another one with "YYY".
        //- Then, the procedures take the leftmost and the rightmost SDFHead(s) to which this SDFRule has assigned the (same) Tag; if the SDFRule assigns a single 
        //  Tag, the corresponding SDFHead is both the leftmost and the rightmost.
        //- Then, it builds the ArrayList of *CONSECUTIVE* SDFHead(s) between the leftmost and the rightmost ones.
        //- Then, it associates this ArrayList to the Tag (one of them, it's the same, they all assign the same label, they all have the same id and idInstance).
        //It returns the associations for all Tags 
        //NB. The input ArrayList<SDFTag> SDFTagsTemp is ordered according to the priority!
    public static Hashtable<SDFTag, ArrayList<SDFHead>> createSequencesOfConsecutiveSDFHeads(ArrayList<SDFTag> SDFTagsTemp, ArrayList<SDFDependencyTree> SDFDependencyTrees)
    {
        Hashtable<SDFTag, ArrayList<SDFHead>> ret = new Hashtable<SDFTag, ArrayList<SDFHead>>();
        
            //We extract the SDFHead(s) from the SDFDependencyTrees and we put all of them in a line.
        ArrayList<SDFHead> allSDFHeads = new ArrayList<SDFHead>();
        for(int i=0;i<SDFDependencyTrees.size();i++)
            for(int j=0;j<SDFDependencyTrees.get(i).getHeads().length;j++)
                allSDFHeads.add(SDFDependencyTrees.get(i).getHeads()[j]);
        
            //We make a copy of the input ArrayList<SDFTag> because in the for cycle below I need to remove elements from the ArrayList,
            //but I don't want to ruin the input ArrayList, which is used also from the calling procedure.
        ArrayList<SDFTag> SDFTags = new ArrayList<SDFTag>();
        for(int i=0;i<SDFTagsTemp.size();i++)SDFTags.add(SDFTagsTemp.get(i));
        
        for(int i=0;i<SDFTags.size();i++)
        {
            SDFTag SDFTag = SDFTags.get(i);
            
            SDFHead leftmostSDFHead = SDFTag.taggedHead;
            SDFHead rightmostSDFHead = SDFTag.taggedHead;
            for(int j=i+1;j<SDFTags.size();j++)
            {
                if((SDFTags.get(j).tag.compareToIgnoreCase(SDFTag.tag)==0)&&(SDFTags.get(j).idSDFRule==SDFTag.idSDFRule)&&(SDFTags.get(j).idInstance==SDFTag.idInstance))
                {
                    SDFTag tempTag = SDFTags.remove(j);//I remove it because otherwise it will be processed again, when the "i" will reach this cell of the ArrayList.
                    j--;
                    
                    if(allSDFHeads.indexOf(tempTag.taggedHead)<allSDFHeads.indexOf(leftmostSDFHead))leftmostSDFHead=tempTag.taggedHead;
                    else if(allSDFHeads.indexOf(tempTag.taggedHead)>allSDFHeads.indexOf(rightmostSDFHead))rightmostSDFHead=tempTag.taggedHead;
                }
            }
            
            ArrayList<SDFHead> allContiguousSDFHeads = new ArrayList<SDFHead>();
            for(int j=allSDFHeads.indexOf(leftmostSDFHead);j<=allSDFHeads.indexOf(rightmostSDFHead);j++)
                allContiguousSDFHeads.add(allSDFHeads.get(j));
            
            ret.put(SDFTag, allContiguousSDFHeads);
        }
        
        return ret;
    }
    
        //This method takes the Hashtable returned from the previous method and the set of SDFTags (which is ordered according to the priority) and remove
        //from the Hashtable the sequences of consecutive SDFHead(s), stored within an ArrayList<SDFHead>, having low priority, in case they are overridden
        //by sequences with higher priority. They are overridden if at least one SDFHead belong to both, i.e., if the two sequences *intersect*.
        //There are then special tags, with label "not-XXX". These are *always* removed. But, before removing them, they are used to remove all lower priority
        //sequences with tag "XXX" that intersect with the ones associated with these "not-XXX" tags.
    public static void removeIntersectingSequencesOfConsecutiveSDFHeads(Hashtable<SDFTag, ArrayList<SDFHead>> sequencesOfConsecutiveSDFHeads, ArrayList<SDFTag> tags)
    {
        for(int i=0;i<tags.size();i++)
        {
            ArrayList<SDFHead> highPrioritySDFHeads = sequencesOfConsecutiveSDFHeads.get(tags.get(i));
            if(highPrioritySDFHeads==null)continue;
            
            for(int j=i+1;j<tags.size();j++)
            {
                    //Tags in the form "not-XXX" only apply to tags in the form "XXX". 
                    //Thus, if there is a (high-priority) "not-XXX" but a (low-priority) "YYY", with "YYY" different from "XXX", we skip.
                if((tags.get(i).tag.indexOf("not-")!=-1)&&(("not-"+tags.get(j).tag).compareToIgnoreCase(tags.get(i).tag)!=0))continue;
                
                ArrayList<SDFHead> lowPrioritySDFHeads = sequencesOfConsecutiveSDFHeads.get(tags.get(j));
                if(lowPrioritySDFHeads==null)continue;
                
                for(int k=0;k<lowPrioritySDFHeads.size();k++)
                {
                        //If this is true, for any node in lowPriorityConcept, there is overlapping/intersection! We remove the tag associated with the 
                        //lowPriorityConcept (and also the corresponding ArrayList<SDFHead> in sequencesOfConsecutiveSDFHeads, of course).
                    if(highPrioritySDFHeads.indexOf(lowPrioritySDFHeads.get(k))!=-1)
                    {
                        sequencesOfConsecutiveSDFHeads.remove(tags.get(j));
                        break;
                    }
                }
            }
            
                //When we are here, all ArrayList<SDFHead> that overlap with highPriorityConcept have been removed. If highPriorityConcept was a "not-XXX",
                //we also remove that. Otherwise, we simply go to the next i.
            if(tags.get(i).tag.indexOf("not-")==0)sequencesOfConsecutiveSDFHeads.remove(tags.get(i));
        }
    }
    
        //This method simply convert the Hashtable processed in the two methods above (which associate each SDFTag with the set of consecutive SDFHead(s))
        //into an Hashtable that can be given to the SDFDependencyTrees2XML, where SDFHead(s) are associated with SDFTags (this facilitate their tagging)
        //Of course, the convertion could be avoided by adding in SDFDependencyTrees2XML a method that implement the same by using an Hashtable in the
        //format used by the two methods above.
    public static Hashtable<SDFHead, SDFTag> convertHashtable(Hashtable<SDFTag, ArrayList<SDFHead>> sequencesOfConsecutiveSDFHeads)
    {
        Hashtable<SDFHead, SDFTag> ret = new Hashtable<SDFHead, SDFTag>();
        
        Enumeration en = sequencesOfConsecutiveSDFHeads.keys();
        while(en.hasMoreElements())
        {
            SDFTag SDFTag = (SDFTag)en.nextElement();
            ArrayList<SDFHead> SDFHeads = sequencesOfConsecutiveSDFHeads.get(SDFTag);
            
            for(int i=0;i<SDFHeads.size();i++)ret.put(SDFHeads.get(i), SDFTag);
        }
        
        return ret;
    }

        //This method takes an ArrayList of Content(s) (i.e., a sequence of Text(s) and Element(s)) and return another ArrayList of Content(s) where some of them have
        //been transformed into <blockList>. To do so, it looks at the <index> tags. The method uses some private utilities defined below. At the end of the method,
        //the remained Text(s) are enclosed within a <p>, so that an ArrayList<Element> is always returned (and not an ArrayList<Content>!).
        //IMPORTANT FEATURES OF THE METHOD!!!!
        //- At each call of the method, any Element in the parameter "contents" is a <blockList> or an <index>.
        //- The method is recursive; at each recursion, at most a <blockList> is created (the innest). Then, we apply recursion on the Content(s), in order to possibly 
        //  identify other <blockList>(s).
        //- In the previous point I wrote "at most a <blockList>". I wrote this because actually if there are no <index>(s), no <blockList> is created.
        //  In such a case, the parameter is simply given in output.
    public static ArrayList<Element> createBlockLists(ArrayList<Content> contents)
    {
            //We extract the innest sequence of consecutive indexes; if there is not, no <blockList> need to be created, we just enclose all Text
            //within a <p> and we leave the other Element as they are.
        ArrayList<Element> innestConsecutiveIndexes = identifyInnestConsecutiveIndexes(contents);
        if(innestConsecutiveIndexes.size()==0)return encloseAllTextsWithinP(contents);
        
            //Now we create a new ArrayList<Content>; the innestConsecutiveIndexes (and all Elements in the middle) are substituted by a single <blockList>
        contents = createBlockList(contents, innestConsecutiveIndexes);
        
            //Now we apply the recursion on the new contents, to possibly identify other <blockList>(s).
        return createBlockLists(contents);
    }
    
/************************************************************************************************************************************************/
/************************************************************************************************************************************************/
/****                                           private utilities used by createBlockLists                                                      */
/************************************************************************************************************************************************/
/************************************************************************************************************************************************/
    
        //This procedure identify the innest (better: ONE OF THE innests ... we could have more than one) sequences of <index>(s).
        //A sequence of <index>(s) is "innest" if it does not contain any other enumeration, i.e., any other <index>. 
    private static ArrayList<Element> identifyInnestConsecutiveIndexes(ArrayList<Content> contents)
    {
        ArrayList<Element> innestConsecutiveIndexes = new ArrayList<Element>();
        
            //For each <index>, we proceed we find out consecutive <index>(s) (at least one more, i.e., at least two <index>(s)).
            //If there is not, we return all <index>(s) as a single innest sequence of <index>(s).
        for(int i=0; i<contents.size(); i++)
        {
            if(contents.get(i)instanceof Text)continue;
            if(((Element)contents.get(i)).getName().compareToIgnoreCase("index")!=0)continue;
                    
            innestConsecutiveIndexes.add((Element)contents.get(i));
            
            int j=i+1;
            for(;j<contents.size();j++)
            {
                if(contents.get(j)instanceof Text)continue;
                if(((Element)contents.get(j)).getName().compareToIgnoreCase("index")!=0)continue;
                if(areIndexesConsecutiveIndexes(innestConsecutiveIndexes.get(innestConsecutiveIndexes.size()-1), (Element)contents.get(j))==false)break;
                innestConsecutiveIndexes.add((Element)contents.get(j));
            }
            
                //if innestConsecutiveIndexes contains more than one Element, it is a candidate. We must check if there is not any other
                //subsequent <index> that follows it. If so, there are other innest sequences of <index>(s) that need to be processed before.
            if(innestConsecutiveIndexes.size()>1)
            {
                for(j=j+1;(j<contents.size())&&(innestConsecutiveIndexes.size()>1);j++)
                {
                    if(contents.get(j)instanceof Text)continue;
                    if(((Element)contents.get(j)).getName().compareToIgnoreCase("index")!=0)continue;
                    
                        //We found a subsequent index! We clear the candidate!
                    if(areIndexesConsecutiveIndexes(innestConsecutiveIndexes.get(innestConsecutiveIndexes.size()-1), (Element)contents.get(j))==true)
                        innestConsecutiveIndexes.clear();
                }
                
                    //If the list is unchanged, we return it. Otherwise, we search for another innest sequence of <index>(s).
                if(innestConsecutiveIndexes.isEmpty())continue;
                return innestConsecutiveIndexes;
            }
            innestConsecutiveIndexes.clear();
        }
        
            //No sequence of consecutive <index>(s) has found. We consider all <index>(s) in a single sequence (if there is at least one <index>, of course...)
        for(int i=0; i<contents.size(); i++)
        {
            if(contents.get(i)instanceof Text)continue;
            if(((Element)contents.get(i)).getName().compareToIgnoreCase("index")!=0)continue;
            innestConsecutiveIndexes.add((Element)contents.get(i));
        }
        
            //If there were no <index>, this list is empty... but that's exactly what needs to be returned.
        return innestConsecutiveIndexes;
    }
    
        //It takes two <index>(s) in input. It returns true if the second is consecutive to the first. Example of consecutive indexes:
        //- <index>1.</index>, <index>2.</index>, <index>3.</index>, ...
        //- <index>(a)</index>, <index>(b)</index>, <index>(c)</index>, ...
        //- <index>-</index>, <index>-</index>, <index>-</index>, ...
    private static boolean areIndexesConsecutiveIndexes(Element firstIndex, Element secondIndex)
    {
        String firstText = null;
        String secondText = null;
        for(int j=0;(j<firstIndex.getContent().size())&&(firstText==null);j++)
            if(firstIndex.getContent().get(j)instanceof Text)
                firstText=((Text)firstIndex.getContent().get(j)).getText();
        for(int j=0;(j<secondIndex.getContent().size())&&(secondText==null);j++)
            if(secondIndex.getContent().get(j)instanceof Text)
                secondText=((Text)secondIndex.getContent().get(j)).getText();
        
        //System.out.println(firstText+", "+secondText);
        //if(firstText.compareToIgnoreCase("34)")==0)
        //    firstText=firstText;
        
            //absurd conditions... we check just in case...
        if((firstText==null)||(secondText==null))return false;
        if((firstText.length()==0)||(secondText.length()==0))return false;
        
            //Here we check the format of the two indexes is the same; if it's not, it is already false.
        if((firstText.charAt(0)=='(')&&(secondText.charAt(0)!='('))return false;
        if((firstText.charAt(firstText.length()-1)=='.')&&(secondText.charAt(secondText.length()-1)!='.'))return false;
        if((firstText.charAt(firstText.length()-1)==')')&&(secondText.charAt(secondText.length()-1)!=')'))return false;
        if((secondText.charAt(0)=='(')&&(firstText.charAt(0)!='('))return false;
        if((secondText.charAt(secondText.length()-1)=='.')&&(firstText.charAt(firstText.length()-1)!='.'))return false;
        if((secondText.charAt(secondText.length()-1)==')')&&(firstText.charAt(firstText.length()-1)!=')'))return false;
        
            //We polish the text from useless characters (those we've just checked)
        while(firstText.indexOf(".")!=-1)firstText=firstText.substring(0, firstText.indexOf("."))+firstText.substring(firstText.indexOf(".")+1, firstText.length());
        while(firstText.indexOf("(")!=-1)firstText=firstText.substring(0, firstText.indexOf("("))+firstText.substring(firstText.indexOf("(")+1, firstText.length());
        while(firstText.indexOf(")")!=-1)firstText=firstText.substring(0, firstText.indexOf(")"))+firstText.substring(firstText.indexOf(")")+1, firstText.length());
        while(secondText.indexOf(".")!=-1)secondText=secondText.substring(0, secondText.indexOf("."))+secondText.substring(secondText.indexOf(".")+1, secondText.length());
        while(secondText.indexOf("(")!=-1)secondText=secondText.substring(0, secondText.indexOf("("))+secondText.substring(secondText.indexOf("(")+1, secondText.length());
        while(secondText.indexOf(")")!=-1)secondText=secondText.substring(0, secondText.indexOf(")"))+secondText.substring(secondText.indexOf(")")+1, secondText.length());
        
            //Now we can have letters, numbers, roman numbers, or hyphen. All but roman numbers are easy to check.
            
            //Let's try numbers!
        boolean firstTextIsNumber = false;
        try
        {
            int a = Integer.parseInt(firstText);
            int b = Integer.parseInt(secondText);
            if(b==(a+1))return true;
            firstTextIsNumber = true;//non è successivo, ma comunque è un numero. Lo marchiamo perchè ci serve sotto.
        }catch(Exception e){}
        
            //Let's try letters!
        if((firstText.length()==1)&&(secondText.length()==1))
        {
            int a = (int)firstText.charAt(0);
            int b = (int)secondText.charAt(0);
            if(b==(a+1))return true;
        }
        
            //Roman numbers ...
        if((firstText.compareToIgnoreCase("i")==0)&&(secondText.compareToIgnoreCase("ii")==0))return true;
        if((firstText.compareToIgnoreCase("ii")==0)&&(secondText.compareToIgnoreCase("iii")==0))return true;
        if((firstText.compareToIgnoreCase("iii")==0)&&(secondText.compareToIgnoreCase("iv")==0))return true;
        if((firstText.compareToIgnoreCase("iv")==0)&&(secondText.compareToIgnoreCase("v")==0))return true;
        if((firstText.compareToIgnoreCase("v")==0)&&(secondText.compareToIgnoreCase("vi")==0))return true;
        if((firstText.compareToIgnoreCase("vi")==0)&&(secondText.compareToIgnoreCase("vii")==0))return true;
        if((firstText.compareToIgnoreCase("vii")==0)&&(secondText.compareToIgnoreCase("viii")==0))return true;
        if((firstText.compareToIgnoreCase("viii")==0)&&(secondText.compareToIgnoreCase("ix")==0))return true;
        if((firstText.compareToIgnoreCase("ix")==0)&&(secondText.compareToIgnoreCase("x")==0))return true;
        if((firstText.compareToIgnoreCase("x")==0)&&(secondText.compareToIgnoreCase("xi")==0))return true;
        if((firstText.compareToIgnoreCase("xi")==0)&&(secondText.compareToIgnoreCase("xii")==0))return true;
        if((firstText.compareToIgnoreCase("xii")==0)&&(secondText.compareToIgnoreCase("xiii")==0))return true;
        if((firstText.compareToIgnoreCase("xiii")==0)&&(secondText.compareToIgnoreCase("xiv")==0))return true;
        if((firstText.compareToIgnoreCase("xiv")==0)&&(secondText.compareToIgnoreCase("xv")==0))return true;
        if((firstText.compareToIgnoreCase("xv")==0)&&(secondText.compareToIgnoreCase("xvi")==0))return true;
        if((firstText.compareToIgnoreCase("xvi")==0)&&(secondText.compareToIgnoreCase("xvii")==0))return true;
        if((firstText.compareToIgnoreCase("xvii")==0)&&(secondText.compareToIgnoreCase("xviii")==0))return true;
        if((firstText.compareToIgnoreCase("xviii")==0)&&(secondText.compareToIgnoreCase("xix")==0))return true;
        if((firstText.compareToIgnoreCase("xix")==0)&&(secondText.compareToIgnoreCase("xx")==0))return true;
        //add here more pairs of roman numbers...
        
            //The final possibility is that the two index are the same, e.g., they are two hyphens: <index>-</index>, <index>-</index>
            //Of course, just in case they are not letters of numbers.
        if((firstText.length()==1)&&(Character.isLetter(firstText.charAt(0))==true))return false;
        if(firstTextIsNumber==true)return false;
                
        if(firstText.compareToIgnoreCase(secondText)==0)return true;
                
        return false;
    }
    
        //We copy all contents until we find the first <index>. Then we create the <blockList>, we add it to the content, and we create all items 
        //until I found the last <index> in longestConsecutiveIndexes. Finally, we copy all Content(s) left in the input array.
        //NB. longestConsecutiveIndexes is consumed during the execution of the method. In other words: at the end of the method it is empty.
    private static ArrayList<Content> createBlockList(ArrayList<Content> contents, ArrayList<Element> longestConsecutiveIndexes)
    {
        ArrayList<Content> ret = new ArrayList<Content>();
        
        for(int i=0;i<contents.size();i++)
        {
            if((longestConsecutiveIndexes.size()==0)||(contents.get(i)!=longestConsecutiveIndexes.get(0))){ret.add(contents.get(i));continue;}
            
            Element blockList = new Element("blockList");
            ret.add(blockList);
            
            while(longestConsecutiveIndexes.isEmpty()==false)
            {
                Element item = new Element("item");
                blockList.getContent().add(item);
                Element num = new Element("num");
                item.getContent().add(num);
                Element index = longestConsecutiveIndexes.remove(0);
                for(int j=0;j<index.getContent().size();j++)
                    if(index.getContent().get(j)instanceof Element)num.getContent().add(((Element)index.getContent().get(j)).clone());
                    else if(index.getContent().get(j)instanceof Text)num.getContent().add(((Text)index.getContent().get(j)).clone());
                
                    //We search for the Element(s) to be inserted in the <item>
                ArrayList<Content> contentsOfItem = new ArrayList<Content>();
                for(i=i+1;i<contents.size();i++)
                {
                        //If we find the next index, the contentsOfItem is complete.
                    if((longestConsecutiveIndexes.size()>0)&&(contents.get(i)==longestConsecutiveIndexes.get(0)))break;
                    
                        //If this is the last item, it could be the case we find an <index>! In such a case, the contentsOfItem is complete.
                        //The <index> will be processed in the next recursions of createBlockLists. To allow that, before "break;" we have 
                        //to insert it in ret.
                    if((longestConsecutiveIndexes.size()==0)&&(contents.get(i)instanceof Element)&&(((Element)contents.get(i)).getName().compareToIgnoreCase("index")==0))
                    {
                        ret.add(contents.get(i));
                        break;
                    }
                    
                    contentsOfItem.add(contents.get(i));
                }
                
                    //If the content is empty, we add an empty <p> just to respect the XSD of Akoma Ntoso.
                if(contentsOfItem.size()==0)item.getContent().add(new Element("p"));
                
                for(int j=0;j<contentsOfItem.size();j++)
                    if(contentsOfItem.get(j)instanceof Element)item.getContent().add(contentsOfItem.get(j));
                    else{Element p=new Element("p");item.getContent().add(p);p.getContent().add(contentsOfItem.get(j));}
            }
        }
        
        return ret;
    }
        
        //This method return a ArrayList<Element> of the same size of the ArrayList<Content> in input.
        //- If contents.get(i) is an instance of Element, this is copied in the returning ArrayList
        //- If contents.get(i) is an instance of Text, an Element <p> is created, the Text is put among its children, and <p> is added to the returning ArrayList
    private static ArrayList<Element> encloseAllTextsWithinP(ArrayList<Content> contents)
    {
        ArrayList<Element> ret = new ArrayList<Element>();
        
            //Now, if there are Text(s), we insert them within a <p>.
            //On the other hand, we insert Element(s) as they are.
        for(int i=0;i<contents.size();i++)
        {
            if((contents.get(i)instanceof Text))
            {
                if(((Text)contents.get(i)).getText().isEmpty()==false)
                {
                    Element p = new Element("p");
                    p.getContent().add(contents.get(i));
                    ret.add(p);
                }
            }
            else ret.add((Element)contents.get(i));
        }
        
        return ret;
    }

        //This procedure enclose the second <blockList> within the latest <item> of the first <blockList>.
        //It is used when we have two consecutive <blockList>(s): we nest the second within the latest <item> of the first 
        //rather than putting them one consecutive to the other (I mean: we have this preference criteria in case of conflicts,
        //but sometimes it could be correct to leave the two <blockList>(s) as sequential <blockList>(s)).
        //The method assume that in input it has two Element with name "blockList".
        //The method returns "false" if it does not manage to enclose the second <blockList> within the former.
    private static boolean encloseSecondConsecutiveBlockListWithinTheFirst(Element blockListWideScope, Element blockListNarrowScope)
    {
        Element latestItem = null;
        for(int i=blockListWideScope.getContent().size()-1; (i>=0)&&(latestItem==null); i--)
            if((blockListWideScope.getContent().get(i)instanceof Element)&&( ((Element)blockListWideScope.getContent().get(i)).getName().compareToIgnoreCase("item")==0))
                latestItem = (Element)blockListWideScope.getContent().get(i);
        if(latestItem==null)return false;
        
        latestItem.getContent().add(blockListNarrowScope);
        return true;
    }
    
/************************************************************************************************************************************************/
/************************************************************************************************************************************************/
/****                                     end of private utilities used by createBlockLists                                 et                    */
/************************************************************************************************************************************************/
/************************************************************************************************************************************************/
}
