package SDFTagger.KBInterface.XMLFilesInterface.XML2SDFCodesCompiler;

import java.io.*;
import java.util.*;

public class checkUpdatesXML
{
    public static void updateCompiledKB(File rootDirectoryXmlSDFRules, File rootDirectoryCompiledSDFRules, String[] localPathsSDFRules)throws Exception 
    {
        if(detectChanges(rootDirectoryXmlSDFRules, rootDirectoryCompiledSDFRules, localPathsSDFRules)==false)return;
        compileKB.compile(rootDirectoryXmlSDFRules, rootDirectoryCompiledSDFRules, localPathsSDFRules);
    }
    
    private static boolean detectChanges(File rootDirectoryXmlSDFRules, File rootDirectoryCompiledSDFRules, String[] localPathsSDFRules)throws Exception 
    {
        Hashtable<String, ArrayList<String>> subfolderFiles = new Hashtable<String, ArrayList<String>>();
        for(int i=0;i<localPathsSDFRules.length;i++)
        {
            if((localPathsSDFRules[i].indexOf("/")==-1)&&(localPathsSDFRules[i].indexOf("\\")==-1))
            {
                File XMLFile = new File(rootDirectoryXmlSDFRules.getAbsolutePath()+"/"+localPathsSDFRules[i]);
                File compiledSDFFile = new File(rootDirectoryCompiledSDFRules.getAbsolutePath()+"/"+localPathsSDFRules[i].substring(0, localPathsSDFRules[i].length()-4)+".sdf");
                if(XMLFile.exists()==false){System.out.println("The file "+XMLFile.getAbsolutePath()+" does not exist!");System.exit(0);}
                if(compiledSDFFile.exists()==false)return true;
                InputStream is = new FileInputStream(compiledSDFFile);
                BufferedReader bf = new BufferedReader(new InputStreamReader(is, "UTF8"));
                long oldsize = Long.parseLong(bf.readLine());
                bf.close();
                is.close();
                if(oldsize!=XMLFile.length())return true;
                continue;
            }
            else
            {
                String localPath = localPathsSDFRules[i];
                String subFolder = "";
                if((localPath.indexOf("/")==-1)||((localPath.indexOf("\\")!=-1)&&(localPath.indexOf("\\")<localPath.indexOf("/"))))
                {
                    subFolder = localPath.substring(0, localPath.indexOf("\\"));
                    localPath  = localPath.substring(localPath.indexOf("\\")+1, localPath.length());
                }
                else if((localPath.indexOf("\\")==-1)||((localPath.indexOf("/")!=-1)&&(localPath.indexOf("/")<localPath.indexOf("\\"))))
                {
                    subFolder = localPath.substring(0, localPath.indexOf("/"));
                    localPath  = localPath.substring(localPath.indexOf("/")+1, localPath.length());
                }
                ArrayList<String> temp = subfolderFiles.get(subFolder);
                if(temp==null)
                {
                    temp = new ArrayList<String>();
                    subfolderFiles.put(subFolder, temp);
                }
                temp.add(localPath);
            }
        }
        
        Enumeration en = subfolderFiles.keys();
        while(en.hasMoreElements())
        {
            String subFolder = (String)en.nextElement();
            ArrayList<String> subPaths = subfolderFiles.get(subFolder);
            String[] subPathsSDFRules = new String[subPaths.size()];
            for(int i=0;i<subPaths.size();i++)subPathsSDFRules[i]=subPaths.get(i);
            
            if(detectChanges(
                new File(rootDirectoryXmlSDFRules.getAbsolutePath()+"/"+subFolder), 
                new File(rootDirectoryCompiledSDFRules.getAbsolutePath()+"/"+subFolder), 
                subPathsSDFRules)==true)return true;
        }
        
        return false;
    }
}

