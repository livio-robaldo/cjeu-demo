package SDFTagger;

import SDFTagger.SDFItems.SDFHead;
import java.util.*;

public class SDFNode
{
    protected SDFHead SDFHead = null;
    protected int index = -1;
    protected SDFNode nextSDFNode = null;
    protected SDFNode prevSDFNode = null;
    protected ArrayList<String> bagsOnForm = new ArrayList<String>();
    protected ArrayList<String> bagsOnLemma = new ArrayList<String>();
    protected boolean endOfSentence = false;
    public SDFNode(SDFHead SDFHead, int index, ArrayList<String> bagsOnForms, ArrayList<String> bagsOnLemmas, boolean endOfSentence)
    {
        this.index=index;
        this.SDFHead=SDFHead;
        this.endOfSentence=endOfSentence;
        if(bagsOnForms!=null)
            for(int i=0;i<bagsOnForms.size();i++)
                this.bagsOnForm.add(bagsOnForms.get(i));
        if(bagsOnLemmas!=null)
            for(int i=0;i<bagsOnLemmas.size();i++)
                this.bagsOnLemma.add(bagsOnLemmas.get(i));
    }
}