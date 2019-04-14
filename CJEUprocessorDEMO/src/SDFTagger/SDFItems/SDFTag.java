package SDFTagger.SDFItems;

public class SDFTag 
{
    public String tag = null;
    public SDFHead taggedHead = null;
    public long priority;
    public long idSDFRule;
    public long idInstance;
    
    public SDFTag(String tag, SDFHead taggedHead, long priority, long idSDFRule, long idInstance)
    {
        this.tag = tag; 
        this.taggedHead = taggedHead;
        this.priority = priority;
        this.idSDFRule = idSDFRule;
        this.idInstance = idInstance;
    }
}
