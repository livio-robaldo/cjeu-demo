//Copyright 2014 Livio Robaldo
//This file is part of CJEUprocessorDEMO. The demo uses the SDFTagger library, whose source code can be obtained by contacting Livio Robaldo (http://www.liviorobaldo.com)
//CJEUprocessorDEMO is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software 
//Foundation, either version 3 of the License, or (at your option) any later version.
//CJEUprocessorDEMO is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
//PARTICULAR PURPOSE. See the GNU General Public License for more details.
//You should have received a copy of the GNU General Public License along with CJEUprocessorDEMO.  If not, see <http://www.gnu.org/licenses/>.
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