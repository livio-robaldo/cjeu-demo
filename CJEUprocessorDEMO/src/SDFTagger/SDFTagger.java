package SDFTagger;

import SDFTagger.SDFItems.SDFDependencyTree;
import java.util.*;
import SDFTagger.SDFItems.*;

public class SDFTagger
{    
    protected SDFTaggerConfig SDFTaggerConfig = null;
    public SDFTagger(SDFTaggerConfig SDFTaggerConfig)throws Exception{this.SDFTaggerConfig=SDFTaggerConfig;}
    public SDFTaggerConfig getSDFTaggerConfig(){return SDFTaggerConfig;}    
    public ArrayList<SDFTag> tagTrees(ArrayList<SDFDependencyTree> trees) throws Exception
    {
        SDFNode firstSDFNode = new SDFNode(null, 0, null, null, false);
        buildSDFNodesChain(trees, firstSDFNode);
        long idInstance = 1;
        ArrayList<SDFTag> ret = new ArrayList<SDFTag>();
        SDFNode scanSDFNode = firstSDFNode.nextSDFNode;
        while(scanSDFNode!=null)
        {
            ArrayList<String> SDFCodes = SDFTaggerConfig.KBManager.retrieveSDFCodes(scanSDFNode.SDFHead);
            for(int i=0;i<SDFCodes.size();i++)
            {
                SDFRule SDFRule = new SDFRule(SDFCodes.get(i), SDFTaggerConfig.SDFNodeConstraintsFactory, this);
                ArrayList<SDFTag> newTags = SDFRule.executeSDFRule(scanSDFNode, idInstance++);
                addTags(ret, newTags);
            }   
            
            scanSDFNode = scanSDFNode.nextSDFNode;
        }
        
        for(int i=0;i<ret.size()-2;i++)
        {
            if(!(ret.get(i).priority>=ret.get(i+1).priority))
            {
                System.out.println("Alt! Sbagliato!!!");System.exit(0);
            }
        }
        
        SDFTaggerConfig.SDFLogger.writeInLogFile(firstSDFNode, ret);
        return ret;
    }

    protected Hashtable<SDFHead,SDFNode> SDFHead2SDFNode = new Hashtable<SDFHead,SDFNode>();
    private void buildSDFNodesChain(ArrayList<SDFDependencyTree> trees, SDFNode firstSDFNode) throws Exception
    {
        int index = 1;
        SDFNode lastSDFNode = firstSDFNode;
        
        for(int i=0;i<trees.size();i++)
        {
            for(int j=0;j<trees.get(i).getHeads().length;j++)
            {
                if(trees.get(i).getHeads()[j].getForm().compareToIgnoreCase("rossa")==0)
                    i=i;

                boolean endSentence=false;
                if(j==trees.get(i).getHeads().length-1)endSentence=true;
                
                SDFNode newSDFNode = new SDFNode(trees.get(i).getHeads()[j], index, new ArrayList<String>(), new ArrayList<String>(), endSentence);
                SDFTaggerConfig.KBManager.fillBagsOfSDFNode(newSDFNode.SDFHead.getForm(), newSDFNode.SDFHead.getLemma(), newSDFNode.bagsOnForm, newSDFNode.bagsOnLemma);
                SDFHead2SDFNode.put(newSDFNode.SDFHead, newSDFNode);
                
                lastSDFNode.nextSDFNode = newSDFNode;
                newSDFNode.nextSDFNode = null;
                newSDFNode.prevSDFNode = lastSDFNode;
                firstSDFNode.prevSDFNode = newSDFNode;
                lastSDFNode = newSDFNode;
                
                index++;            
            }
        }
    }
    
    private synchronized void addTags(ArrayList<SDFTag> array, ArrayList<SDFTag> tagsToAdd)
    {
        if(tagsToAdd.isEmpty())return;
        int a = 0;
        int m = 0;
        int b = array.size()-1;
        while(b>=a)
        {
            m = (a+b)/2;
            if(array.get(m).priority==tagsToAdd.get(0).priority)
            {
                for(int i=0;i<tagsToAdd.size();i++)array.add(m, tagsToAdd.get(i));
                return;
            }
            
            if((m==a)&&(m==b))
            {
                if(array.get(m).priority>tagsToAdd.get(0).priority)m++;
                for(int i=0;i<tagsToAdd.size();i++)array.add(m, tagsToAdd.get(i));
                return;
            }
            
            if(array.get(m).priority>tagsToAdd.get(0).priority)a=m+1;
            else if(m==a)b=a;else b=m-1;
        }
        
        for(int i=0;i<tagsToAdd.size();i++)array.add(m, tagsToAdd.get(i));
    }
}