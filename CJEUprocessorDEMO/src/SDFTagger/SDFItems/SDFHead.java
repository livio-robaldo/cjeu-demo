package SDFTagger.SDFItems;
import java.util.*;

public class SDFHead
{
    protected String Form = null;
    protected String Lemma = null;
    protected String POS = null;
    protected String endOfSentence = null;
    protected SDFHead Governor = null;
    protected String Label = null;
    protected ArrayList<SDFHead> dependents = new ArrayList<SDFHead>();
    protected Hashtable<String,String> optionalFeatures = new Hashtable<String,String>();

    public SDFHead(String Form, String Lemma, String POS, String endOfSentence, SDFHead Governor, String Label)
    {
        this.Form = Form;
        this.Lemma = Lemma;
        this.POS = POS;
        this.endOfSentence = endOfSentence;
        this.Governor = Governor;
        this.Label = Label;
        if((Governor!=null)&&(Governor.dependents.indexOf(this)==-1))Governor.dependents.add(this);
    }
    
    public void setOptionalFeatures(String feature, String value)
    {
        if(optionalFeatures.get(feature)!=null)optionalFeatures.remove(feature);
        optionalFeatures.put(feature, value);
    }
    
    public String getForm()
    {
        return Form;
    }

    public String getLemma()
    {
        return Lemma;
    }
    
    public String getPOS()
    {
        return POS;
    }
    
    public String getEndOfSentence()
    {
        return endOfSentence;
    }
    
    public SDFHead getGovernor()
    {
        return Governor;
    }
   
    public SDFHead[] getDependents()
    {
        SDFHead[] ret = new SDFHead[dependents.size()];
        for(int i=0; i<dependents.size(); i++) ret[i] = dependents.get(i);
        return ret;
    }
    
    public String getLabel()
    {
        return Label;
    }
    
    public ArrayList<String> listOptionalFeatures()
    {
        ArrayList<String> ret = new ArrayList<String>();
        Enumeration en = optionalFeatures.keys();
        while(en.hasMoreElements())ret.add((String)en.nextElement());
        return ret;
    }
    
    public String getOptionalFeaturesValue(String feature){return optionalFeatures.get(feature);}
}

