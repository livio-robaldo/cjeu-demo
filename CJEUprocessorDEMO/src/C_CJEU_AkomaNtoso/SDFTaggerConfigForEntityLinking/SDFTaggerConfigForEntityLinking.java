//Copyright 2014 Livio Robaldo
//This file is part of CJEUprocessorDEMO. The demo uses the SDFTagger library, whose source code can be obtained by contacting Livio Robaldo (http://www.liviorobaldo.com)
//CJEUprocessorDEMO is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software 
//Foundation, either version 3 of the License, or (at your option) any later version.
//CJEUprocessorDEMO is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
//PARTICULAR PURPOSE. See the GNU General Public License for more details.
//You should have received a copy of the GNU General Public License along with CJEUprocessorDEMO.  If not, see <http://www.gnu.org/licenses/>.

package C_CJEU_AkomaNtoso.SDFTaggerConfigForEntityLinking;

import SDFTagger.SDFNodeConstraints;
import SDFTagger.SDFTaggerConfig;
import SDFTagger.SDFLogger;
import SDFTagger.KBInterface.XMLFilesInterface.XMLFilesManager;
import java.io.File;
import java.util.*;

public class SDFTaggerConfigForEntityLinking extends SDFTaggerConfig
{
    public SDFTaggerConfigForEntityLinking(File logFile, boolean resetAll)throws Exception
    {
            //Bags        
        rootDirectoryBags = new File("./src/C_CJEU_AkomaNtoso/SDFTaggerConfigForEntityLinking/SDFBags");
        localPathsBags = new String[]
        {
            //"NONE FOR NOW.xml",
        };

        rootDirectoryXmlSDFRules = new File("./src/C_CJEU_AkomaNtoso/SDFTaggerConfigForEntityLinking/SDFRulesXML");
        rootDirectoryCompiledSDFRules = new File("./src/C_CJEU_AkomaNtoso/SDFTaggerConfigForEntityLinking/SDFRulesCompiled");
        
        if(resetAll==true)deleteAllSDFRules();
        fillLocalPathsSDFRules();
        
            //For the SDFTagger
        SDFNodeConstraintsFactory = new SDFNodeConstraints();//no subclass of SDFNodeConstraints is defined
        KBManager = new XMLFilesManager(rootDirectoryBags, localPathsBags, rootDirectoryXmlSDFRules, rootDirectoryCompiledSDFRules, localPathsSDFRules);
        
        if(logFile!=null)SDFLogger = new SDFLogger(logFile);
    }
    
    public String getRootDirectoryXmlSDFRulesPath(){return rootDirectoryXmlSDFRules.getAbsolutePath();}
            
        //This method fill localPathsSDFRules with all XML files we find in rootDirectoryXmlSDFRules. We create subdirectories, if needed.
        //This method is called from the constructor and whenever we update the directory with new XML files.
    private void fillLocalPathsSDFRules()throws Exception
    {
                    //In the constructor, we fill localPathsSDFRules with all XML files we find in rootDirectoryXmlSDFRules and we compile it
        ArrayList<File> files = new ArrayList<File>();
        ArrayList<File> temp = new ArrayList<File>();
        temp.add(rootDirectoryXmlSDFRules);
        while(temp.isEmpty()==false)
        {
            File file = temp.remove(0);
            if(file.isDirectory()==true)for(int i=0;i<file.listFiles().length;i++)temp.add(file.listFiles()[i]);
            else files.add(file);
        }
        
        localPathsSDFRules = new String[files.size()];
        for(int i=0;i<files.size();i++)
        {
            String path = files.get(i).getAbsolutePath();
            path = path.substring(path.indexOf(rootDirectoryXmlSDFRules.getAbsolutePath())+rootDirectoryXmlSDFRules.getAbsolutePath().length(), path.length());
            while(path.charAt(0)=='/')path=path.substring(1, path.length());
            while(path.charAt(0)=='\\')path=path.substring(1, path.length());
            localPathsSDFRules[i] = path;
        }
    }
    
        //Second constructor: the String[] localPathsSDFRules is given in input, no need to call the fillLocalPathsSDFRules method.
        //This constructor is used when we create the new SDFRule(s) from the parties that we found in the case law and that we managed to classify.
    public SDFTaggerConfigForEntityLinking(File logFile, String[] localPathsSDFRules)throws Exception
    {
            //Bags        
        rootDirectoryBags = new File("./src/C_CJEU_AkomaNtoso/SDFTaggerConfigForEntityLinking/SDFBags");
        localPathsBags = new String[]
        {
            //"NONE FOR NOW.xml",
        };

        rootDirectoryXmlSDFRules = new File("./src/C_CJEU_AkomaNtoso/SDFTaggerConfigForEntityLinking/SDFRulesXML");
        rootDirectoryCompiledSDFRules = new File("./src/C_CJEU_AkomaNtoso/SDFTaggerConfigForEntityLinking/SDFRulesCompiled");
        
            //We populate the localPathsSDFRules of this object with the one in input (we copy the latter into the former...)
        this.localPathsSDFRules = new String[localPathsSDFRules.length];
        for(int i=0;i<localPathsSDFRules.length;i++)this.localPathsSDFRules[i]=localPathsSDFRules[i];
        
            //For the SDFTagger
        SDFNodeConstraintsFactory = new SDFNodeConstraints();//no subclass of SDFNodeConstraints is defined
        KBManager = new XMLFilesManager(rootDirectoryBags, localPathsBags, rootDirectoryXmlSDFRules, rootDirectoryCompiledSDFRules, localPathsSDFRules);
        
        if(logFile!=null)SDFLogger = new SDFLogger(logFile);
    }
    
        //We delete all files and all subdirectories from rootDirectoryXmlSDFRules and rootDirectoryCompiledSDFRules. 
    private void deleteAllSDFRules()
    {
        ArrayList<File> filesToDelete = new ArrayList<File>();
        for(int i=0;i<rootDirectoryXmlSDFRules.listFiles().length;i++)filesToDelete.add(rootDirectoryXmlSDFRules.listFiles()[i]);
        for(int i=0;i<rootDirectoryCompiledSDFRules.listFiles().length;i++)filesToDelete.add(rootDirectoryCompiledSDFRules.listFiles()[i]);
        deleteFileOrDirectory(filesToDelete);
    }
    
        //private recursive procedure used by the one above.
    private void deleteFileOrDirectory(ArrayList<File> filesToDelete)
    {
        if(filesToDelete.isEmpty())return;
        
        File file = filesToDelete.remove(0);
        if(file.isDirectory())
        {
            for(int i=0;i<file.listFiles().length;i++)filesToDelete.add(file.listFiles()[i]);
            deleteFileOrDirectory(filesToDelete);
        }
        
        for(int i=0;(i<1000)&&(file.exists());i++)file.delete();
        if(file.exists()==true){System.out.println("I don't manage to delete "+file.getAbsolutePath());System.exit(0);}
        deleteFileOrDirectory(filesToDelete);
    }
}