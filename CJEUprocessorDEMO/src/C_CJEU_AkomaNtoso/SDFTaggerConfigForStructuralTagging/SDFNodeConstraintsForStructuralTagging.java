//Copyright 2014 Livio Robaldo
//This file is part of CJEUprocessorDEMO. The demo uses the SDFTagger library, whose source code can be obtained by contacting Livio Robaldo (http://www.liviorobaldo.com)
//CJEUprocessorDEMO is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software 
//Foundation, either version 3 of the License, or (at your option) any later version.
//CJEUprocessorDEMO is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
//PARTICULAR PURPOSE. See the GNU General Public License for more details.
//You should have received a copy of the GNU General Public License along with CJEUprocessorDEMO.  If not, see <http://www.gnu.org/licenses/>.

package C_CJEU_AkomaNtoso.SDFTaggerConfigForStructuralTagging;

import SDFTagger.SDFNodeConstraints;
import SDFTagger.SDFItems.SDFHead;
import java.util.*;

    //As explained in SDFNodeConstraints, when we extend this class, we need to override FactorySDFNodeConstraints and checkOptionalFeatures.
public class SDFNodeConstraintsForStructuralTagging extends SDFNodeConstraints
{
        //This is overridden so that now it instantiates an object of this class (and not of the superclass)
    protected SDFNodeConstraints FactorySDFNodeConstraints(){return new SDFNodeConstraintsForStructuralTagging();}
    
        //This is overridden so that the optional features are checked (in the superclass they are not, the method always returns true)
    protected boolean checkOptionalFeatures(Hashtable<String, String> optionalFeatures, SDFHead SDFHead)
    {
        Enumeration en = optionalFeatures.keys();
        
        while(en.hasMoreElements())
        {
            String key = (String)en.nextElement();
            
            if(key.compareToIgnoreCase("blanksBefore")==0)
            {
                String blanksBeforeSDFHead = SDFHead.getOptionalFeaturesValue(key);
                if(blanksBeforeSDFHead==null)return false;
                if(optionalFeatures.get(key).compareToIgnoreCase(blanksBeforeSDFHead)!=0) return false;
            }
            else if(key.compareToIgnoreCase("Font")==0)
            {
                String fontSDFHead = SDFHead.getOptionalFeaturesValue(key);
                if(fontSDFHead==null)return false;
                if(optionalFeatures.get(key).compareToIgnoreCase(fontSDFHead)!=0) return false;
            }
            
                //The Stanford parser has two POSs for numbers: CD and LS. Worst of all, sometimes it even classifies numbers as NN!
                //E.g. in "No 2445" -> it classifies "2445" as NN. We use then this constraint in SDFHead to check when there is a number.
                //This constraints checks when all the Characters of the Form are digits.
            else if(key.compareToIgnoreCase("isNumber")==0)
            {
                String isNumber = optionalFeatures.get(key);
                if(isNumber.compareToIgnoreCase("true")==0)
                {
                    for(int i=0;i<SDFHead.getForm().length();i++)
                        if(Character.isDigit(SDFHead.getForm().charAt(i))==false)
                            return false;
                }
                else if(isNumber.compareToIgnoreCase("false")==0)
                {
                    for(int i=0;i<SDFHead.getForm().length();i++)
                        if(Character.isDigit(SDFHead.getForm().charAt(i))==true)
                            return false;
                }   
                else return false;
            }
            
                //If <isCapital>true</isCapital>, then this is true if the Form is capital, i.e., if its first letter is capital. False otherwise.
            else if(key.compareToIgnoreCase("isCapital")==0)
            {
                String isCapital = optionalFeatures.get(key);
                if(isCapital.compareToIgnoreCase("true")==0){if(Character.isUpperCase(SDFHead.getForm().charAt(0))==false)return false;}
                else if(isCapital.compareToIgnoreCase("false")==0){if(Character.isUpperCase(SDFHead.getForm().charAt(0))==true)return false;}
                else return false;
            }
            
                //This is true if the Form contains both letters and numbers. 
                //It is used in "Article 25k (1)(e)" to jump over "25k" and avoid tagging "(1)" as index.
            else if(key.compareToIgnoreCase("isAlphanumerical")==0)
            {
                boolean areThereNumbers = false;
                boolean areThereLetters = false;
                for(int i=0;(i<SDFHead.getForm().length())&&((areThereLetters==false)||(areThereNumbers==false));i++)
                    if(Character.isDigit(SDFHead.getForm().charAt(i))==true)
                        areThereNumbers=true;
                    else if(Character.isLetter(SDFHead.getForm().charAt(i))==true)
                        areThereLetters=true;
                
                if(!((areThereLetters==true)&&(areThereNumbers==true)))return false;
            }
            
                //This is true if the Form express a number that is too big, i.e., equal or superior to four integers.
                //We used to avoid indexes on years. E.g. "2016.", which is taken as index without this feature.
            else if(key.compareToIgnoreCase("isTooBigNumber")==0)
            {
                String isTooBigNumber = optionalFeatures.get(key);
                if(isTooBigNumber.compareToIgnoreCase("true")==0){if(SDFHead.getForm().length()<4)return false;}
                else if(isTooBigNumber.compareToIgnoreCase("false")==0){if(SDFHead.getForm().length()>=4)return false;}
                else return false;
            }
        }
                
        return true;
    }
}
