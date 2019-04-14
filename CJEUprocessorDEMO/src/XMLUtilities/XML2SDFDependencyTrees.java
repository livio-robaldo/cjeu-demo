package XMLUtilities;

import SDFTagger.SDFItems.SDFHead;
import SDFTagger.SDFItems.SDFDependencyTree;
import java.util.ArrayList;
import org.jdom2.*;
import java.util.*;

public class XML2SDFDependencyTrees 
{
    public static ArrayList<SDFDependencyTree> XML2SDFDependencyTrees(Element DependencyTrees) throws Exception 
    {
        if(DependencyTrees.getName().compareToIgnoreCase("DependencyTrees")!=0)throw new Exception("not DependencyTrees!");
        
        ArrayList<SDFDependencyTree> ret = new ArrayList<SDFDependencyTree>();
        
        Hashtable<String,SDFHead> eId2SDFHeads = new Hashtable<String,SDFHead>();
        Hashtable<SDFHead,Hashtable<String,String>> SDFHeads2optionalFeaturesSDFHead = new Hashtable<SDFHead,Hashtable<String,String>>();
        
        for(int i=0;i<DependencyTrees.getContent().size();i++)
        {
            if(!(DependencyTrees.getContent().get(i)instanceof Element))continue;
            Element DependencyTree = (Element)DependencyTrees.getContent().get(i);    
            ret.add(XMLDependencyTree2SDFDependencyTree(DependencyTree, eId2SDFHeads, SDFHeads2optionalFeaturesSDFHead));
        }

        return ret;
    }
    
    private static SDFDependencyTree XMLDependencyTree2SDFDependencyTree
    (
        Element DependencyTree, 
        Hashtable<String,SDFHead> eId2SDFHeads,
        Hashtable<SDFHead,Hashtable<String,String>> SDFHeads2optionalFeaturesSDFHead
    ) throws Exception 
    {
        if(DependencyTree.getName().compareToIgnoreCase("DependencyTree")!=0)throw new Exception("<DependencyTrees> must only contain <DependencyTree>!");
        Hashtable<String, ArrayList<Element>> governorEids2linesOfDependents = new Hashtable<String,ArrayList<Element>>();
        int lineCounter=0;
        
        for(int i=0;i<DependencyTree.getContent().size();i++)
        {
            if(!(DependencyTree.getContent().get(i)instanceof Element))continue;
            Element line = (Element)DependencyTree.getContent().get(i);
            if(line.getName().compareToIgnoreCase("line")!=0)throw new Exception("<DependencyTree> must only contain <line>!");
            
            lineCounter++;
            String eIdLine = line.getAttributeValue("eId");
            if(eIdLine==null)throw new Exception("Ill-formed line: eId missing");
            
            String eIdLineGovernor = getGovernor(line);
            ArrayList<Element> linesOfDependents = governorEids2linesOfDependents.get(eIdLineGovernor);
            if(linesOfDependents==null)
            {
                linesOfDependents = new ArrayList<Element>();
                governorEids2linesOfDependents.put(eIdLineGovernor, linesOfDependents);
            }
            linesOfDependents.add(line);
        }
        
        getHeadsFromLines(governorEids2linesOfDependents, null, "0", eId2SDFHeads, SDFHeads2optionalFeaturesSDFHead);
        SDFHead[] heads = new SDFHead[lineCounter];
        lineCounter=0;
        for(int i=0;i<DependencyTree.getContent().size();i++)
        {
            if(!(DependencyTree.getContent().get(i)instanceof Element))continue;
            Element line = (Element)DependencyTree.getContent().get(i);
            if(line.getName().compareToIgnoreCase("line")!=0)throw new Exception("<DependencyTree> must only contain <line>!");
            String eIdLine = line.getAttributeValue("eId");
            if(eIdLine==null)throw new Exception("Ill-formed line: eId missing");
            
            heads[lineCounter] = eId2SDFHeads.get(eIdLine);
            Hashtable<String,String> optionalFeaturesSDFHead = SDFHeads2optionalFeaturesSDFHead.get(heads[lineCounter]);            
            lineCounter++;
        }
        
        return new SDFDependencyTree(heads);
    }
    
    private static void getHeadsFromLines
    (
        Hashtable<String, ArrayList<Element>> governor2linesOfDependents,
        SDFHead Governor, String governorEid, 
        Hashtable<String, SDFHead> eId2SDFHeads,
        Hashtable<SDFHead,Hashtable<String,String>> SDFHeads2optionalFeaturesSDFHead
    )throws Exception
    {
        ArrayList<Element> linesOfDependents = governor2linesOfDependents.get(governorEid);
        if(linesOfDependents==null)return;
        
        for(int i=0;i<linesOfDependents.size();i++)
        {
            Element line = linesOfDependents.get(i);
            String eIdLine = line.getAttributeValue("eId");
            if(eIdLine==null)throw new Exception("Ill-formed line: eId missing");
            
            Hashtable<String,String> optionalFeaturesString = new Hashtable<String,String>();
            String[] features = getFeatures(line, optionalFeaturesString);
            SDFHead newSDFHead = new SDFHead(features[0], features[1], features[2], features[3], Governor, features[4]);
            Enumeration en = optionalFeaturesString.keys();
            while(en.hasMoreElements())
            {
                String feature = (String)en.nextElement();
                newSDFHead.setOptionalFeatures(feature, optionalFeaturesString.get(feature));
            }
            
            eId2SDFHeads.put(eIdLine, newSDFHead);
            getHeadsFromLines(governor2linesOfDependents, newSDFHead, eIdLine, eId2SDFHeads, SDFHeads2optionalFeaturesSDFHead);
        }
    }
    
    private static String getGovernor(Element line)throws Exception
    {
        for(int i=0;i<line.getContent().size();i++)
        {
            if(!(line.getContent().get(i)instanceof Element))continue;
            Element Governor = (Element)line.getContent().get(i);
            if(Governor.getName().compareToIgnoreCase("Governor")!=0)continue;
         
            for(int j=0;j<Governor.getContent().size();j++)
            {
                if(!(Governor.getContent().get(j)instanceof Text))continue; 
                String eIdLine = ((Text)Governor.getContent().get(j)).getText().trim();
                
                try
                {
                    int temp=Integer.parseInt(eIdLine);
                    if(temp<0)throw new Exception("Ill-formed <Governor> tag value: the argument must be a positive integer\"");
                }
                catch(Exception e)
                {
                    throw new Exception("Ill-formed <Governor> tag value: the argument must be a positive integer\"");
                }
                
                return eIdLine;
            }
        }
        
        throw new Exception("I cannot find the <Governor> within the <line> with eId="+line.getAttributeValue("eId")+"!");
    }
    
    private static String[] getFeatures(Element line, Hashtable<String,String> optionalFeaturesString)throws Exception
    {
        String[] ret = new String[]{null,null,null,null,null};
        
        for(int i=0;i<line.getContent().size();i++)
        {
            if(!(line.getContent().get(i)instanceof Element))continue;
            Element e = (Element)line.getContent().get(i);
            
            String value = null;
            for(int j=0;(j<e.getContent().size()&&(value==null));j++)
            {
                if(!(e.getContent().get(j)instanceof Text))continue;
                value=((Text)e.getContent().get(j)).getText();
            }
            
            if(e.getName().compareToIgnoreCase("Form")==0)ret[0]=value;
            else if(e.getName().compareToIgnoreCase("Lemma")==0)ret[1]=value;
            else if(e.getName().compareToIgnoreCase("POS")==0)ret[2]=value;
            else if(e.getName().compareToIgnoreCase("endOfSentence")==0)ret[3]=value;
            else if(e.getName().compareToIgnoreCase("Governor")==0)continue;
            else if(e.getName().compareToIgnoreCase("Label")==0)ret[4]=value;
            else optionalFeaturesString.put(e.getName(), value.trim());
        }
        
        if(ret[0]==null)throw new Exception("Ill-formed <line> with eId=\""+line.getAttributeValue("eId")+"\": missing Form.");
        if(ret[1]==null)throw new Exception("Ill-formed <line> with eId=\""+line.getAttributeValue("eId")+"\": missing Lemma.");
        if(ret[2]==null)throw new Exception("Ill-formed <line> with eId=\""+line.getAttributeValue("eId")+"\": missing POS.");
        if(ret[4]==null)throw new Exception("Ill-formed <line> with eId=\""+line.getAttributeValue("eId")+"\": missing Label.");
        
        return ret;
    }
    
    private static String getLemma(Element line)throws Exception
    {
        for(int i=0;i<line.getContent().size();i++)
        {
            if(!(line.getContent().get(i)instanceof Element))continue;
            Element Lemma = (Element)line.getContent().get(i);
            if(Lemma.getName().compareToIgnoreCase("Lemma")!=0)continue;
         
            for(int j=0;j<Lemma.getContent().size();j++)
            {
                if(!(Lemma.getContent().get(j)instanceof Text))continue;
                
                return ((Text)Lemma.getContent().get(j)).getText();
            }
        }
        
        throw new Exception("I cannot find the <Lemma> within a <line> with eId="+line.getAttributeValue("eId")+"!");
    }
    
    private static String getPOS(Element line)throws Exception
    {
        for(int i=0;i<line.getContent().size();i++)
        {
            if(!(line.getContent().get(i)instanceof Element))continue;
            Element POS = (Element)line.getContent().get(i);
            if(POS.getName().compareToIgnoreCase("POS")!=0)continue;
         
            for(int j=0;j<POS.getContent().size();j++)
            {
                if(!(POS.getContent().get(j)instanceof Text))continue;
                
                return ((Text)POS.getContent().get(j)).getText();
            }
        }
        
        throw new Exception("I cannot find the <POS> within a <line> with eId="+line.getAttributeValue("eId")+"!");
    }
    
    private static String getLabel(Element line)throws Exception
    {
        for(int i=0;i<line.getContent().size();i++)
        {
            if(!(line.getContent().get(i)instanceof Element))continue;
            Element Label = (Element)line.getContent().get(i);
            if(Label.getName().compareToIgnoreCase("Label")!=0)continue;
         
            for(int j=0;j<Label.getContent().size();j++)
            {
                if(!(Label.getContent().get(j)instanceof Text))continue;
                
                return ((Text)Label.getContent().get(j)).getText();
            }
        }
        
        throw new Exception("I cannot find the <Label> within a <line> with eId="+line.getAttributeValue("eId")+"!");
    }
}