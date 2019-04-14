package SDFTagger.KBInterface.XMLFilesInterface.XML2SDFCodesCompiler;
        
import java.io.*;
import java.util.*;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.*;

public class compileKB
{
    protected static void compile(File rootDirectoryXmlSDFRules, File rootDirectoryCompiledSDFRules, String[] localPathsSDFRules)throws Exception 
    {
        deleteAndRecreateFolders(rootDirectoryCompiledSDFRules, localPathsSDFRules);
        long startingId = 1;
        for(int i=0;i<localPathsSDFRules.length;i++)
        {
            File file = new File(rootDirectoryXmlSDFRules.getAbsolutePath()+"/"+localPathsSDFRules[i]);
            if(file.exists()==false)throw new Exception("The file "+file.getName()+" does not exist");
            startingId = setIdsOnFilesRecursively(file, startingId);
        }
        for(int i=0;i<localPathsSDFRules.length;i++)
        {
            String localXMLFile = localPathsSDFRules[i];
            String localSDFFile = localXMLFile.substring(0, localXMLFile.length()-4)+".sdf";
            File inputFile = new File(rootDirectoryXmlSDFRules+"/"+localXMLFile);
            File outputFile = new File(rootDirectoryCompiledSDFRules+"/"+localSDFFile);
            if(outputFile.getParentFile().exists()==false)throw new Exception("The directory "+outputFile.getParentFile().getName()+" does not exist, please create it");
            compile(inputFile, outputFile);
        }
    }
 
    private static void deleteAndRecreateFolders(File rootDirectoryCompiledSDFRules, String[] localPathsSDFRules)throws Exception
    {        
        if(rootDirectoryCompiledSDFRules.exists()==false)
        {
            ArrayList<String> subpaths = new ArrayList<String>();
            File tempDir = rootDirectoryCompiledSDFRules;
            while(tempDir.exists()==false)
            {
                subpaths.add(0, tempDir.getName());
                tempDir = tempDir.getParentFile();
            }
            File newDir = tempDir;
            for(int i=0;i<subpaths.size();i++)
            {
                newDir = new File(newDir+"/"+subpaths.get(i));
                newDir.mkdir();
            }
        }
        ArrayList<File> filesToDelete = new ArrayList<File>();
        filesToDelete.add(rootDirectoryCompiledSDFRules);
        while(filesToDelete.isEmpty()==false)
        {
            File outputFile = filesToDelete.remove(0);
            if(outputFile.isDirectory()==true)
            {
                for(int i=0;i<outputFile.listFiles().length;i++)filesToDelete.add(outputFile.listFiles()[i]);
                if(outputFile==rootDirectoryCompiledSDFRules)continue;
                if(outputFile.listFiles().length==0)outputFile.delete();
                else filesToDelete.add(outputFile);
            }
            else
            {
                for(int z=0;z<1000;z++)if(outputFile.exists()==true)outputFile.delete();
                if(outputFile.exists()==true)throw new Exception("I don't manage to delete the file "+outputFile.getAbsolutePath());
            }
        }
        
        if(localPathsSDFRules!=null)for(int i=0;i<localPathsSDFRules.length;i++)createSubdir(rootDirectoryCompiledSDFRules, localPathsSDFRules[i]);
    }
    
    private static void compile(File input, File output)throws Exception 
    {
        SAXBuilder saxbuilder = new SAXBuilder();
        InputStream is = new FileInputStream(input);
        Document doc = saxbuilder.build(is);
        Element SDFRules = doc.getRootElement();
        is.close();
        if(moveNotDependentsAtTheBottom(SDFRules)==true)
        {
            doc = new Document();
            doc.setRootElement(SDFRules.clone());
            XMLOutputter outputter = new XMLOutputter();
            outputter.setFormat(Format.getPrettyFormat().setEncoding("UTF-8"));
            FileOutputStream fos = new FileOutputStream(input);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
            outputter.output(doc, osw);
            osw.close();
            fos.close();
        }
        if(SDFRules.getName().compareToIgnoreCase("SDFRules")!=0)return;
        for(int z=0;z<1000;z++)if(output.exists()==true)output.delete();
        if(output.exists()==true)throw new Exception("I don't manage to delete the file "+output.getAbsolutePath());
        FileOutputStream fos = new FileOutputStream(output);
        PrintStream psFos = new PrintStream(fos, true, "UTF8");
        psFos.println(input.length());
        List l = SDFRules.getChildren();
        for(int i=0; i<l.size(); i++) 
        {
            Element e = (Element)l.get(i);
            if(e.getName().compareToIgnoreCase("SDFRule")!=0)continue;
            convert2SDFCode temp = new convert2SDFCode(e);
            psFos.println(temp.getSdfCode());
        }
        psFos.close();
        fos.close();
    }
    
    private static long setIdsOnFilesRecursively(File f, long startingId) throws Exception
    {
        if(f.isDirectory()==false)
        {
            long salvaStartingId=startingId;
            try
            {
                startingId = setIdsOnSDFRules(f, startingId);
            }
            catch(Exception e)
            {
                startingId=salvaStartingId;
            }
        }
        else
        {
            for(int i=0;i<f.listFiles().length;i++)
            {
                startingId = setIdsOnFilesRecursively(f.listFiles()[i], startingId);
            }
        }
        
        return startingId;
    }
    
    private static long setIdsOnSDFRules(File xmlFile, long startingId) throws Exception
    {
        SAXBuilder saxbuilder = new SAXBuilder();
        InputStream is = new FileInputStream(xmlFile);
        Document doc = saxbuilder.build(is);
        Element SDFRules = doc.getRootElement();
        
        if(SDFRules.getName().compareToIgnoreCase("SDFRules")!=0)
            throw new Exception("Invalid element! <SDFRules> expected!");
                
        List l = SDFRules.getChildren();
        for(int i=0; i<l.size(); i++) 
        {
            Element e = (Element)l.get(i);
            if(e.getName().compareToIgnoreCase("SDFRule")!=0)continue;
            e.setAttribute("id", ""+startingId);
            startingId++;
        }
            
        XMLOutputter outputter = new XMLOutputter();
        outputter.setFormat(Format.getPrettyFormat());
        File outputF = xmlFile;
        if(outputF.exists()==true) outputF.delete();
        FileWriter writer = new FileWriter(outputF);
        outputter.output(doc, writer);
        writer.close();
        is.close();

        return startingId;
    }
    private static boolean moveNotDependentsAtTheBottom(Element element) throws Exception
    {
        boolean ret = false;
        if(element.getName().compareToIgnoreCase("dependents")==0)
        {
            int i=element.getContent().size()-1;
            for(;i>=0;i--)
                if(!(element.getContent().get(i)instanceof Element))continue;
                else if(((Element)element.getContent().get(i)).getName().compareToIgnoreCase("dependent")==0)
                    break;
            ArrayList<Element> notDependents = new ArrayList<Element>();
            for(i=i-1;i>=0;i--)
                if(!(element.getContent().get(i)instanceof Element))continue;
                else if(((Element)element.getContent().get(i)).getName().compareToIgnoreCase("notDependent")==0)
                    notDependents.add((Element)element.getContent().get(i));
            if(notDependents.isEmpty()==false)
            {
                ret=true;
                for(i=0;i<notDependents.size();i++)
                {
                    element.getContent().remove(notDependents.get(i));
                    element.getContent().add(notDependents.get(i).clone());
                }
            }
        }
        
        for(int i=0;i<element.getContent().size();i++)
        {
            if(!(element.getContent().get(i)instanceof Element))continue;
            ret = ret | moveNotDependentsAtTheBottom((Element)element.getContent().get(i));
        }
        
        return ret;
    }
    
    private static void createSubdir(File rootDirectoryCompiledSDFRules, String localPath)
    {
        ArrayList<String> chainOfSubdirs = new ArrayList<String>();
        String name = "";
        for(int i=0;i<localPath.length();i++)
        {
            if((localPath.charAt(i)=='/')||(localPath.charAt(i)=='\\'))
            {
                if(name.isEmpty()==false)chainOfSubdirs.add(name);
                name = "";
            }
            else name = name + localPath.charAt(i);
        }
        
        File subdir = rootDirectoryCompiledSDFRules;
        for(int i=0; i<chainOfSubdirs.size(); i++)
        {
            subdir = new File(subdir+"/"+chainOfSubdirs.get(i));
            if(subdir.exists()==false)subdir.mkdir();
        }
    }
}