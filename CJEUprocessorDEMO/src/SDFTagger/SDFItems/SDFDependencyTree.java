package SDFTagger.SDFItems;

import java.util.*;

public class SDFDependencyTree 
{
    protected SDFHead[] heads = null;
    protected SDFHead root = null;
    
    public SDFDependencyTree(SDFHead[] heads)throws Exception
    {
        Hashtable<SDFHead,String> safeSDFHead = new Hashtable<SDFHead,String>();
                
        this.heads = new SDFHead[heads.length];
        for(int i=0;i<heads.length;i++)
        {
            this.heads[i]=heads[i];
            if(heads[i].getGovernor()==null)
                if(root==null)root=heads[i];
                else throw new Exception("Ill-formed dependency tree: multiple roots");
            else checkSDFHead(heads[i], heads, safeSDFHead);
        }
    }
    
    public SDFHead getRoot(){return root;}
    
    public SDFHead[] getHeads()
    {
        SDFHead[] ret = new SDFHead[heads.length];
        for(int i=0; i<heads.length; i++)ret[i]=heads[i];
        return ret;
    }
    
    private boolean checkSDFHead(SDFHead newSDFHead, SDFHead[] heads, Hashtable<SDFHead,String> safeSDFHead)throws Exception
    {
        if(safeSDFHead.get(newSDFHead)!=null)return true;
        if(newSDFHead.Governor==null)return true;
        if(safeSDFHead.get(newSDFHead.Governor)==null)
        {
            boolean found=false;
            for(int i=0;i<heads.length&&found==false;i++)if(heads[i]==newSDFHead.Governor)found=true;
            if(found==false)throw new Exception("Ill-formed dependency tree: at least one head has an external governor"); 
            checkSDFHead(newSDFHead.Governor, heads, safeSDFHead);
        }
            
        safeSDFHead.put(newSDFHead, "");
        return true;
    }
}
