package OntologyManager.SDFBuilder;

import SDFTagger.SDFItems.SDFHead;
import SDFTagger.SDFItems.SDFDependencyTree;
import java.io.*;
import java.util.*;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.*;

//This is a class of Utilities to create SDFRule(s)
public class SDFRulesBuilderUtilities 
{
/************************************************* UTILITIES TO CREATE NEW SDFRule(s) *************************************************/

    protected static Element createSDFRuleOnSDFHeadsLeft2Right(ArrayList<SDFHead> SDFHeads, int priority, int maxDistance, String tag, String[] features)
    {
        if(SDFHeads.isEmpty())return null;
        
        Element ret = new Element("SDFRule");
        ret.setAttribute("id", "");
        ret.setAttribute("priority", ""+priority);
        
        Element e = ret;
        for(int i=0;i<SDFHeads.size();i++)
        {
            Element SDFTag = new Element("tag");
            SDFTag.getContent().add(new Text(tag));
            e.getContent().add(SDFTag);
            //Element headAlternatives = createHeadAlternatives(SDFHeads.get(i), new String[]{"Form", "Lemma", "POS"});
            Element headAlternatives = createHeadAlternatives(SDFHeads.get(i), features);
            e.getContent().add(headAlternatives);
            
            if(i==SDFHeads.size()-1)continue;
            
            Element nextAlternatives = new Element("nextAlternatives");
            e.getContent().add(nextAlternatives);
            Element next = new Element("next");
            next.setAttribute("maxDistance", ""+maxDistance);
            nextAlternatives.getContent().add(next);
            e = next;
        }
        
        return ret;
    }
    
    private static Element createHeadAlternatives(SDFHead SDFHead, String[] features)
    {
        Element headAlternatives = new Element("headAlternatives");
        
        Element head = new Element("head");
        headAlternatives.getContent().add(head);
                
        for(int i=0;i<features.length;i++)
        {
            Element temp = null;
            if(features[i].compareToIgnoreCase("Form")==0){temp=new Element("Form");temp.getContent().add(new Text(SDFHead.getForm()));}
            else if(features[i].compareToIgnoreCase("Lemma")==0){temp=new Element("Lemma");temp.getContent().add(new Text(SDFHead.getLemma()));}
            else if(features[i].compareToIgnoreCase("POS")==0){temp=new Element("POS");temp.getContent().add(new Text(SDFHead.getPOS()));}
            else if(SDFHead.getOptionalFeaturesValue(features[i])!=null){temp=new Element(features[i]);temp.getContent().add(new Text(SDFHead.getOptionalFeaturesValue(features[i])));}
            head.getContent().add(temp);
        }
        
        return headAlternatives;
    }
    
/************************************************* UTILITIES TO QUICKLY FIND NEEDED INFO *************************************************/
    
        //We collect and return all SDFHead(s) from left to right
    protected static ArrayList<SDFHead> getAllSDFHeads(ArrayList<SDFDependencyTree> SDFDependencyTrees)
    {
        ArrayList<SDFHead> SDFHeads = new ArrayList<SDFHead>();
        for(int i=0;i<SDFDependencyTrees.size();i++)
            for(int j=0;j<SDFDependencyTrees.get(i).getHeads().length;j++)
                SDFHeads.add(SDFDependencyTrees.get(i).getHeads()[j]);
        
        return SDFHeads;
    }
    
        //XMLconstraint is either <SDFRule> or <next>, or <prev> or <governor> or <dependent>
        //The method returns an Array of AT LEAST SEVEN Element(s), each of which corresponds a subcondition, as listed below.
        //The Element(s) from Element[7] until the last one are optional as they the SDFTag(s) (possibly) defined on the XMLconstraint.
        //Of course, if some conditions are missing, in its place there will be null.
        //Element[0] -> <headAlternatives>
        //Element[1] -> <prevStarAlternatives>
        //Element[2] -> <prevAlternatives>
        //Element[3] -> <nextStarAlternatives>
        //Element[4] -> <nextAlternatives>
        //Element[5] -> <governorAlternatives>
        //Element[6] -> <dependentsAlternatives>
        //Element[7] -> SDFTag1
        //Element[8] -> SDFTag2
        // ...
        //Element[n] -> SDFTag(n+7)
        //The method assumes the input is well-formed (no multiple <headAlternative>(s), for instance).
    protected static Element[] identifyConditions(Element XMLconstraint)
    {
        Element headAlternatives = null;
        Element prevStarAlternatives = null;
        Element prevAlternatives = null;
        Element nextStarAlternatives = null;
        Element nextAlternatives = null;
        Element governorAlternatives = null;
        Element dependentsAlternatives = null;
        ArrayList<Element> SDFTags = new ArrayList<Element>();
        
        for(int i=0;i<XMLconstraint.getContent().size();i++)
            if(!(XMLconstraint.getContent().get(i)instanceof Element))continue;
            else if(((Element)XMLconstraint.getContent().get(i)).getName().compareToIgnoreCase("headAlternatives")==0)
                headAlternatives=(Element)XMLconstraint.getContent().get(i);
            else if(((Element)XMLconstraint.getContent().get(i)).getName().compareToIgnoreCase("prevStarAlternatives")==0)
                prevStarAlternatives=(Element)XMLconstraint.getContent().get(i);
            else if(((Element)XMLconstraint.getContent().get(i)).getName().compareToIgnoreCase("prevAlternatives")==0)
                prevAlternatives=(Element)XMLconstraint.getContent().get(i);
            else if(((Element)XMLconstraint.getContent().get(i)).getName().compareToIgnoreCase("nextStarAlternatives")==0)
                nextStarAlternatives=(Element)XMLconstraint.getContent().get(i);
            else if(((Element)XMLconstraint.getContent().get(i)).getName().compareToIgnoreCase("nextAlternatives")==0)
                nextAlternatives=(Element)XMLconstraint.getContent().get(i);
            else if(((Element)XMLconstraint.getContent().get(i)).getName().compareToIgnoreCase("governorAlternatives")==0)
                governorAlternatives=(Element)XMLconstraint.getContent().get(i);
            else if(((Element)XMLconstraint.getContent().get(i)).getName().compareToIgnoreCase("dependentsAlternatives")==0)
                dependentsAlternatives=(Element)XMLconstraint.getContent().get(i);
            else if(((Element)XMLconstraint.getContent().get(i)).getName().compareToIgnoreCase("tag")==0)
                SDFTags.add((Element)XMLconstraint.getContent().get(i));
        
        Element[] ret = new Element[7+SDFTags.size()];
        
        ret[0] = headAlternatives;
        ret[1] = prevStarAlternatives;
        ret[2] = prevAlternatives;
        ret[3] = nextStarAlternatives;
        ret[4] = nextAlternatives;
        ret[5] = governorAlternatives;
        ret[6] = dependentsAlternatives;
        for(int i=0;i<SDFTags.size();i++)ret[i+7]=SDFTags.get(i);
        return ret;
    }
    
        //Given an <head> it builds and returns an Hashtable<String,String> with the associations field->value,
        //e.g., "Form"->"XXX", "Lemma"->"XXX", "POS"->"ZZZ", etc. (also all the optional features of course)
    protected static Hashtable<String,String> identifyFeatures(Element head)
    {
        Hashtable<String,String> ret = new Hashtable<String,String>();
        
        for(int i=0;i<head.getContent().size();i++)
        {
            if(!(head.getContent().get(i)instanceof Element))continue;
            else
            {
                Element feature = (Element)head.getContent().get(i);
                String value = "";
                for(int j=0;j<feature.getContent().size();j++)
                    if(feature.getContent().get(j)instanceof Element)continue;
                    else value = (value+" "+((Text)feature.getContent().get(j)).getText().trim()).trim();
                ret.put(feature.getName(), value);
            }
        }
        
        return ret;
    }
    
/************************************************* UTILITIES TO SAVE SDFRule(s) IN A File *************************************************/
        //This method assumes that the input document and the SDFRule are well-formed.
        //If the SDFRule is not well-formed, it won't compile.
    public static void saveSDFRulesOnFile(ArrayList<Element> SDFRulesToAdd, File file)throws Exception
    {
        if(file.isDirectory()==true)return;
        
        Document doc = null;
        Element SDFRules = null;
        
        if(file.exists()==false)
        {
            SDFRules = new Element("SDFRules");
            doc = new Document();
            doc.setRootElement(SDFRules);
        }
        else
        {
            try{doc=(Document)new SAXBuilder().build(file);}catch(Exception e){throw new Exception("The file "+file.getName()+" does not contain a well-formed XML document.");}
            SDFRules = doc.getRootElement();
            if(SDFRules.getName().compareToIgnoreCase("SDFRules")!=0)throw new Exception("The root of "+file.getName()+" is not <SDFRules>.");
        }
        
        for(int i=0;i<SDFRulesToAdd.size();i++)
        {
            SDFRules.getContent().add(new Comment(getStringRepresentation(SDFRulesToAdd.get(i))));
            SDFRules.getContent().add(SDFRulesToAdd.get(i));
        }
        
        try
        {
            XMLOutputter outputter = new XMLOutputter();
            outputter.setFormat(Format.getPrettyFormat().setEncoding("UTF-8"));
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
            outputter.output(doc, osw);
            osw.close();
            fos.close();
        }catch(Exception e)
        {
            throw new Exception("I cannot save the new SDFRule in the file "+file.getName());
        }
    }
    
        //It returns a string representation of the SDFRule, to increase readability;
        //The procedure starts from the XMLconstraint of a node (<SDFRule> or <next>, or <prev> or <governor> or <dependent>) and uses
        //the methods identifyConditions and identifyFeatures defined above to build the String representation of the SDFRule, which
        //is convenient to be read by a human.
        //The method is recursive, and assumes the XMLconstraint is well-formed (no multiple <headAlternative>(s), for instance).
    protected static String getStringRepresentation(Element XMLconstraint)
    {
        try
        {
            String ret = "";

            Element[] conditions = identifyConditions(XMLconstraint);

            Element headAlternatives = conditions[0];
            for(int i=0;i<headAlternatives.getContent().size();i++)
            {
                if(!(headAlternatives.getContent().get(i)instanceof Element))continue;
                else if(((Element)headAlternatives.getContent().get(i)).getName().compareToIgnoreCase("head")==0)
                {
                    Hashtable<String,String> features = identifyFeatures((Element)headAlternatives.getContent().get(i));
                    String nodeText = features.get("Form");
                    if(nodeText==null)nodeText = "|"+features.get("Lemma").toUpperCase()+"|";

                    ret = ret + nodeText;
                }
            }

            Element nextAlternatives = conditions[4];
            if(nextAlternatives!=null)
            {
                for(int i=0;i<nextAlternatives.getContent().size();i++)
                {
                    if(!(nextAlternatives.getContent().get(i)instanceof Element))continue;
                    else if(((Element)nextAlternatives.getContent().get(i)).getName().compareToIgnoreCase("next")==0)
                    {
                        ret = (ret + " " + getStringRepresentation((Element)nextAlternatives.getContent().get(i))).trim();
                    }
                }
            }

                //NB. "--" cannot be inserted in a XML comment: we convert it to "-"
            if(XMLconstraint.getName().compareToIgnoreCase("SDFRule")==0)return " \""+ret.replaceAll("--", "-")+"\" ";
            return ret;
        }
        catch(Exception e)
        {
            return " FAILURE WHILE GENERATING THE STRING REPRESENTATION OF THE SDFRule ";
        }
    }
}
