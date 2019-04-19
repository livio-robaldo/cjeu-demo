package C_CJEU_AkomaNtoso.SDFTaggerConfigForPartyClassificationOnKeywords;

import SDFTagger.SDFItems.SDFHead;
import SDFTagger.SDFNodeConstraints;
import java.util.Enumeration;
import java.util.Hashtable;


    //we need <isCapital>
public class SDFNodeConstraintsForClassificationOnKeywords extends SDFNodeConstraints
{
        //This is overridden so that now it instantiates an object of this class (and not of the superclass)
    protected SDFNodeConstraints FactorySDFNodeConstraints(){return new SDFNodeConstraintsForClassificationOnKeywords();}

        //This is overridden so that the optional features are checked (in the superclass they are not, the method always returns true)
    protected boolean checkOptionalFeatures(Hashtable<String, String> optionalFeatures, SDFHead SDFHead)
    {
        Enumeration en = optionalFeatures.keys();

        while(en.hasMoreElements())
        {
            String key = (String)en.nextElement();

                //If <isCapital>true</isCapital>, then this is true if the Form is capital, i.e., if its first letter is capital. False otherwise.
            if(key.compareToIgnoreCase("isCapital")==0)
            {
                String isCapital = optionalFeatures.get(key);
                if(isCapital.compareToIgnoreCase("true")==0)
                {
                    if(Character.isUpperCase(SDFHead.getForm().charAt(0))==false)
                        return false;
                    return true;
                }
                else if(isCapital.compareToIgnoreCase("false")==0)
                {
                    if(Character.isUpperCase(SDFHead.getForm().charAt(0))==false)
                        return true;
                    return false;
                }  
                else return false;
            }
        }

        return true;
    }
}
