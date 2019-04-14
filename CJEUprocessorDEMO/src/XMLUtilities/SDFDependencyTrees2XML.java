package XMLUtilities;

import SDFTagger.SDFItems.SDFHead;
import SDFTagger.SDFItems.SDFDependencyTree;
import SDFTagger.SDFItems.SDFTag;
import java.util.*;
import org.jdom2.*;

public class SDFDependencyTrees2XML 
{
    public static ArrayList<Content> SDFDependencyTrees2XML(ArrayList<SDFDependencyTree> SDFDependencyTrees, Hashtable<SDFHead, SDFTag> taggedSDFHeads)throws Exception
    {
        addBlanksBefore(SDFDependencyTrees);
        
        ArrayList<Content> ret = new ArrayList<Content>();
        
        String text = "";
        String prevtag = "";
        long prevId = -1;
        for(int i=0;i<SDFDependencyTrees.size();i++)
        {
            SDFDependencyTree SDFDependencyTree = SDFDependencyTrees.get(i);
            
            for(int j=0;j<SDFDependencyTree.getHeads().length;j++)
            {
                SDFTag Tag = taggedSDFHeads.get(SDFDependencyTree.getHeads()[j]);
                
                if(Tag==null)
                {
                    if(prevtag.isEmpty()==true)text=updateText(text, SDFDependencyTree.getHeads()[j]);
                    else if(text.isEmpty()==false)
                    {
                        Element element = new Element(prevtag);
                        element.getContent().add(new Text(text));
                        ret.add(element);
                        prevtag = "";
                        text = SDFDependencyTree.getHeads()[j].getForm().trim();
                    }
                }
                else 
                {
                    if((Tag.tag.compareToIgnoreCase(prevtag)==0)&&(prevId==Tag.idInstance))text=updateText(text, SDFDependencyTree.getHeads()[j]);
                    else
                    {
                        if(prevtag.isEmpty()==true)ret.add(new Text(text));
                        else
                        {
                            Element element = new Element(prevtag);
                            element.getContent().add(new Text(text));
                            ret.add(element);
                        }
                        
                        text = SDFDependencyTree.getHeads()[j].getForm().trim();
                        prevtag = Tag.tag;
                        prevId = Tag.idInstance;
                    }
                }
            }
        }
        
        if(text.isEmpty()==false)
        {
            if(prevtag.isEmpty()==true) ret.add(new Text(text));
            else
            {
                Element element = new Element(prevtag);
                element.getContent().add(new Text(text));
                ret.add(element);
            }
        }
        
        return ret;
    }
    
    private static void addBlanksBefore(ArrayList<SDFDependencyTree> SDFDependencyTrees)
    {
        for(int i=0;i<SDFDependencyTrees.size()-1; i++)
        {
            if
            (
                (SDFDependencyTrees.get(i+1).getHeads().length>0)&&
                (SDFDependencyTrees.get(i+1).getHeads()[0].getPOS().compareToIgnoreCase("Punctuation")!=0)&&
                (SDFDependencyTrees.get(i+1).getHeads()[0].getOptionalFeaturesValue("blanksBefore").compareToIgnoreCase("0")==0)
            ) SDFDependencyTrees.get(i+1).getHeads()[0].setOptionalFeatures("blanksBefore", "1");
        }
    }
    
    
    private static String updateText(String text, SDFHead SDFHead)
    {
        text=text.trim();
        
        int blanksBefore = 1;
        
        String blanksBeforeString = SDFHead.getOptionalFeaturesValue("blanksBefore");
        
        if(blanksBeforeString!=null)blanksBefore = Integer.parseInt(blanksBeforeString);
        
        for(int i=0;i<blanksBefore;i++)text=text+" ";
        
        return (text+SDFHead.getForm().trim()).trim();
    }    
}