package SDFTagger.KBInterface.XMLFilesInterface;

import java.util.*;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import java.io.*;
import SDFTagger.SDFItems.*;
import SDFTagger.KBInterface.*;
import SDFTagger.KBInterface.XMLFilesInterface.XML2SDFCodesCompiler.*;

public class XMLFilesManager implements KBInterface
{
    protected Hashtable<String, ArrayList<String>> allFormsToBags = new Hashtable<String,ArrayList<String>>();
    protected Hashtable<String, ArrayList<String>> allLemmasToBags = new Hashtable<String,ArrayList<String>>();
    protected ArrayList<String> rulesFormIndex = new ArrayList<String>();
    protected ArrayList<ArrayList<String>> rulesForm = new ArrayList<ArrayList<String>>();
    protected ArrayList<String> rulesLemmaIndex = new ArrayList<String>();
    protected ArrayList<ArrayList<String>> rulesLemma = new ArrayList<ArrayList<String>>();
    protected ArrayList<String> rulesPOSIndex = new ArrayList<String>();
    protected ArrayList<ArrayList<String>> rulesPOS = new ArrayList<ArrayList<String>>();
    protected ArrayList<String> rulesNotAssociatedWithFormsLemmasAndPOSs = new ArrayList<String>();
    
    public XMLFilesManager
    (
        File rootDirectoryBags, String[] localPathsBags,
        File rootDirectoryXmlSDFRules, File rootDirectoryCompiledSDFRules, String[] localPathsSDFRules
    ) throws Exception
    {
        loadBags(rootDirectoryBags, localPathsBags);
        checkUpdatesXML.updateCompiledKB(rootDirectoryXmlSDFRules, rootDirectoryCompiledSDFRules, localPathsSDFRules);
        loadCompiledSDFRules(rootDirectoryCompiledSDFRules, localPathsSDFRules);
    }
    
    public void fillBagsOfSDFNode(String Form, String Lemma, ArrayList<String> bagsOnForm, ArrayList<String> bagsOnLemma)throws Exception
    {
        ArrayList<String> temp = allFormsToBags.get(Form.toLowerCase());
        for(int i=0;(temp!=null)&&(i<temp.size());i++)bagsOnForm.add(temp.get(i));
        temp = allLemmasToBags.get(Lemma.toLowerCase());
        for(int i=0;(temp!=null)&&(i<temp.size());i++)bagsOnLemma.add(temp.get(i));
    }
    
    public ArrayList<String> retrieveSDFCodes(SDFHead SDFHead)throws Exception
    {
        String Form = SDFHead.getForm().toLowerCase();
        String Lemma = SDFHead.getLemma().toLowerCase();
        String POS = SDFHead.getPOS().toLowerCase();            
        ArrayList<String> ret = new ArrayList<String>();
        int a = 0;
        int b = rulesFormIndex.size()-1;
        while(b>=a)
        {
            int m = (a+b)/2;
            if(rulesFormIndex.get(m).compareToIgnoreCase(Form)==0)
            {
                ArrayList<String> rules = rulesForm.get(m);
                for(int i=0;i<rules.size();i++)ret.add(rules.get(i));
                break;
            }
            if((m==a)&&(m==b))break;
            if(rulesFormIndex.get(m).compareToIgnoreCase(Form)<0)a=m+1;
            else if(m==a)b=a;else b=m-1;
        }
        
        a = 0;
        b = rulesLemmaIndex.size()-1;
        while(b>=a)
        {
            int m = (a+b)/2;
            if(rulesLemmaIndex.get(m).compareToIgnoreCase(Lemma)==0)
            {
                ArrayList<String> rules = rulesLemma.get(m);
                for(int i=0;i<rules.size();i++)ret.add(rules.get(i));
                break;
            }
            if((m==a)&&(m==b))break;
            if(rulesLemmaIndex.get(m).compareToIgnoreCase(Lemma)<0)a=m+1;
            else if(m==a)b=a;else b=m-1;
        }
        
        a = 0;
        b = rulesPOSIndex.size()-1;
        while(b>=a)
        {
            int m = (a+b)/2;
            if(rulesPOSIndex.get(m).compareToIgnoreCase(POS)==0)
            {
                ArrayList<String> rules = rulesPOS.get(m);
                for(int i=0;i<rules.size();i++)ret.add(rules.get(i));
                break;
            }
            if((m==a)&&(m==b))break;
            if(rulesPOSIndex.get(m).compareToIgnoreCase(POS)<0)a=m+1;
            else if(m==a)b=a;else b=m-1;
        }
        
        for(int i=0;i<rulesNotAssociatedWithFormsLemmasAndPOSs.size();i++)ret.add(rulesNotAssociatedWithFormsLemmasAndPOSs.get(i));
        return ret;
    }
        
    private void loadBags(File rootDirectoryBags, String[] localPathsBags) throws Exception
    {
        for(int i=0;i<localPathsBags.length;i++)
        {
            File file = new File(rootDirectoryBags.getAbsolutePath()+"/"+localPathsBags[i]);            
            if(file.exists()==false)throw new Exception("The file "+file.getAbsolutePath()+" does not exist");
            try
            {
                Document document = (Document) new SAXBuilder().build(file);
                Element Bags = document.getRootElement();
                for(int j=0; j<Bags.getChildren().size(); j++)
                {
                    if(!(Bags.getChildren().get(j) instanceof Element)) continue;
                    
                    Element Bag = (Element)Bags.getChildren().get(j);
                    String name = Bag.getAttributeValue("name");
                    String tempType = Bag.getAttributeValue("type");
                    
                    for(int k=0; k<Bag.getChildren().size(); k++)
                    {
                        if(!(Bag.getChildren().get(k) instanceof Element)) continue;
                        if(((Element)Bag.getChildren().get(k)).getName().compareToIgnoreCase("instance")!=0) continue;
                        String instance = ((Text)((Element)Bag.getChildren().get(k)).getContent().get(0)).getText();
                        ArrayList<String> bags=null;
                        if(tempType.compareToIgnoreCase("Form")==0)
                        {
                            bags = allFormsToBags.get(instance);
                            if(bags==null){bags=new ArrayList<String>();allFormsToBags.put(instance, bags);}
                        }
                        else if(tempType.compareToIgnoreCase("Lemma")==0)
                        {
                            bags = allLemmasToBags.get(instance);
                            if(bags==null){bags=new ArrayList<String>();allLemmasToBags.put(instance, bags);}
                        }
                        
                        bags.add(name);
                    }
                }
            }
            catch(Exception e)
            {
                throw new Exception("The file "+file.getAbsolutePath()+" is not in the correct format");
            }
        }
    }
    
    private void loadCompiledSDFRules(File rootDirectoryCompiledSDFRules, String[] localPathsSDFRules) throws Exception
    {
        for(int j=0; j<localPathsSDFRules.length; j++)
        {
            String path = rootDirectoryCompiledSDFRules.getAbsolutePath()+"/"+localPathsSDFRules[j];
            if(path.lastIndexOf(".xml")!=path.length()-4)continue;
            path=path.substring(0, path.length()-4)+".sdf";            
            if(new File(path).exists()==false)continue;
            InputStream is = new FileInputStream(path);
            InputStreamReader isr = new InputStreamReader(is, "UTF8");
            BufferedReader bf = new BufferedReader(isr);
            String line = bf.readLine();
            while((line=bf.readLine())!=null)
            {
                String headAlternativesCode = line.substring(line.indexOf("$")+1, line.indexOf("$", line.indexOf("$")+1));
                ArrayList<String> keys = new ArrayList<String>();
                while(headAlternativesCode.isEmpty()==false)
                {
                    String headCode = headAlternativesCode.substring(headAlternativesCode.indexOf("#")+1, headAlternativesCode.indexOf("#", headAlternativesCode.indexOf("#")+1));
                    headAlternativesCode = headAlternativesCode.substring(headAlternativesCode.indexOf("#", headAlternativesCode.indexOf("#")+1)+1, headAlternativesCode.length());
                    String key = "OTHER";
                    if(headCode.indexOf("£c")==0)
                        key="form+++"+headCode.substring(headCode.indexOf("£c")+2, headCode.indexOf("£",headCode.indexOf("£c")+2)).toLowerCase();
                    else if(headCode.indexOf("£d")==0)
                        key="lemma+++"+headCode.substring(headCode.indexOf("£d")+2, headCode.indexOf("£",headCode.indexOf("£d")+2)).toLowerCase();
                    else if(headCode.indexOf("£e")==0)
                        key="POS+++"+headCode.substring(headCode.indexOf("£e")+2, headCode.indexOf("£",headCode.indexOf("£e")+2)).toLowerCase();

                    if(key.compareToIgnoreCase("OTHER")==0)
                    {
                        keys.clear();
                        keys.add(key);
                        break;
                    }
                    
                    boolean t=false;
                    for(int z=0;z<keys.size();z++){if(keys.get(z).compareToIgnoreCase(key)==0){t=true;break;}}
                    if(t==false)keys.add(key);
                } 

                for(int z=0;z<keys.size();z++)
                {
                    String key=keys.get(z);
                    if(key.compareToIgnoreCase("OTHER")==0)rulesNotAssociatedWithFormsLemmasAndPOSs.add(line);
                    else
                    {
                        ArrayList<String> keysOfrules = new ArrayList<String>();
                        ArrayList<ArrayList<String>> rules = new ArrayList<ArrayList<String>>();
                            
                        if(key.indexOf("form+++")==0)
                        {
                            keysOfrules = rulesFormIndex;
                            rules = rulesForm;
                            key = key.substring(7, key.length());
                        }
                        else if(key.indexOf("lemma+++")==0)
                        {
                            keysOfrules = rulesLemmaIndex;
                            rules = rulesLemma;
                            key = key.substring(8, key.length());
                        }
                        else if(key.indexOf("POS+++")==0)
                        {
                            keysOfrules = rulesPOSIndex;
                            rules = rulesPOS;
                            key = key.substring(6, key.length());
                        }
                        
                        addKeyAndLineToTheArray(keysOfrules, key, rules, line);
                    }
                }
            }
            
            is.close();
            isr.close();
            bf.close();
        }
    }
    
    private void addKeyAndLineToTheArray(ArrayList<String> array, String key, ArrayList<ArrayList<String>> rules, String line)throws Exception
    {
        int a = 0;
        int m = 0;
        int b = array.size()-1;

        while(b>=a)
        {
            m = (a+b)/2;
            if(array.get(m).compareToIgnoreCase(key)==0){rules.get(m).add(line);return;}
            if((m==a)&&(m==b))
            {
                if(array.get(m).compareToIgnoreCase(key)<0)m++;
                array.add(m, key);
                rules.add(m, new ArrayList<String>());
                rules.get(m).add(line);
                return;
            }

            if(array.get(m).compareToIgnoreCase(key)<0)a=m+1;
            else if(m==a)b=a;else b=m-1;
        }
        
        array.add(key);
        rules.add(new ArrayList<String>());
        rules.get(0).add(line);
    }
}
