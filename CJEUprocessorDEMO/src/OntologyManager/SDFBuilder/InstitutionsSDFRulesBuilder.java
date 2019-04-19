package OntologyManager.SDFBuilder;

import SDFTagger.SDFItems.SDFHead;
import SDFTagger.SDFItems.SDFDependencyTree;
import org.jdom2.*;
import java.util.*;
import java.io.*;
import java.net.*;
import org.jdom2.input.SAXBuilder;

public class InstitutionsSDFRulesBuilder extends SDFRulesBuilderUtilities
{
    protected static int priorityStrict = 1000;
    
    public static ArrayList<Element> createSDFRulesForInstitutionFromSDFDependencyTrees(ArrayList<SDFDependencyTree> SDFDependencyTrees, String newIndividualName)
    {
        return createSDFRulesForInstitutionFromSDFHeads(getAllSDFHeads(SDFDependencyTrees), newIndividualName);
    }
    
    public static ArrayList<Element> createSDFRulesForInstitutionFromSDFHeads(ArrayList<SDFHead> SDFHeads, String newIndividualName)
    {
        ArrayList<Element> ret = new ArrayList<Element>();
        
            //(1) We create the strict rule (from left to right, only on Form(s), maxDistance=1)
        Element strictSDFRule = createSDFRuleOnSDFHeadsLeft2Right(SDFHeads, priorityStrict, 1, newIndividualName, new String[]{"Form"});
        if(strictSDFRule!=null)ret.add(strictSDFRule);
            
            //We take (maybe temporarily) the institution name as the comment of the strict SDFRule. We need it for the SDFRule(s) generated from Wikipedia (see below)
        String institutionName = getStringRepresentation(strictSDFRule).replaceAll("\"", "").trim();
        
            //(2) We search for the acronym at the end of the name; if there is, we add other two SDFRule(s): one that recognize the institution 
            //without the acronym, and one that recognize the acronym. If there is an acronym, we generate two SDFRules: one that recognize the
            //name without the acronym and one that recognizes the acronym.
            //The two new SDFRule have a priority which is 10 lower than the priority of the strictSDFRule in input.
        try
        {
            ArrayList<ArrayList<SDFHead>> temp = searchForAcronym(SDFHeads);
            if(temp!=null)
            {
                SDFHeads = temp.get(0);
                ArrayList<SDFHead> SDFHeadsAcronym = temp.get(1);
                strictSDFRule = createSDFRuleOnSDFHeadsLeft2Right(SDFHeads, priorityStrict-100, 1, newIndividualName, new String[]{"Form"});
                if(strictSDFRule!=null){ret.add(strictSDFRule);institutionName=getStringRepresentation(strictSDFRule).replaceAll("\"", "").trim();}
                Element SDFRuleAcronym = createSDFRuleOnSDFHeadsLeft2Right(SDFHeadsAcronym, priorityStrict-10, 1, newIndividualName, new String[]{"Form"});
                if(SDFRuleAcronym!=null)ret.add(SDFRuleAcronym);
            }
        }
        catch(Exception e){}//exceptions are ignored...
        
            //(3) we try to create a "relaxed" SDFRule only on the NN POS, with maxDistance=3 between one node and the next one.
            //Only with the SDFHead(s) that belong to certain POSs, and only if the first and last SDFHead in the main array belong to these POSs.
            //The relaxed rule has half priority with respect to the strict rule, and maxDistance=3 between its Element(s)
        try
        {
            String[] POSs = new String[]{"NN"};//this are the accepted POSs
            ArrayList<SDFHead> SDFHeadsOnPOSs = new ArrayList<SDFHead>();
            for(int i=0;i<SDFHeads.size();i++)for(int j=0;j<POSs.length;j++)if(POSs[j].compareToIgnoreCase(SDFHeads.get(i).getPOS())==0){SDFHeadsOnPOSs.add(SDFHeads.get(i));break;}
            if((SDFHeadsOnPOSs.indexOf(SDFHeads.get(0))!=-1)&&(SDFHeadsOnPOSs.indexOf(SDFHeads.get(SDFHeads.size()-1))!=-1))
            {
                Element relaxedSDFRule = createSDFRuleOnSDFHeadsLeft2Right(SDFHeadsOnPOSs, priorityStrict/2, 3, newIndividualName, new String[]{"Lemma", "POS"});
                if(relaxedSDFRule!=null)ret.add(relaxedSDFRule);
            }
        }
        catch(Exception e){}//exceptions are ignored...
        
            //(4) We look for prefixes and suffixes, and we create an SDFRule for each possible combination.
        ArrayList<Element> SDFRulesOnPrefixesAndSuffixes = buildSDFRulesOnPrefixesAndSuffixes(SDFHeads, priorityStrict, newIndividualName);
        for(int i=0;i<SDFRulesOnPrefixesAndSuffixes.size();i++)ret.add(SDFRulesOnPrefixesAndSuffixes.get(i));
        
            //(5) If the institution's name (we took it from the previous steps) is Italian, French, etc., we should find it in Wikipedia.
            //We search for its English named and we create a rule for it (if we find it)./**
        if(institutionName!=null)
        {
            try
            {
                Element newSDFRule = createSDFRuleOnEnglishNameFromWikipedia(institutionName, priorityStrict, newIndividualName);
                if(newSDFRule!=null)ret.add(newSDFRule);
            }
            catch(Exception e){}//exceptions are ignored...
        }/**/
        
        
        return ret;
    }
    
        //This method look for acronym in the ArrayList of SDFHead(s); if it finds it, it separate the SDFHead(s) of the name from the ones of the acronym.
        //For instance, from the SDFRule that recognizes "Autorità nazionale anticorruzione (ANAC)", we generate the SDFRule(s) that
        //recognize "Autorità nazionale anticorruzione" and "ANAC".
        //The method assumes that if there is an acronym, this is found at the end, enclosed between "(...)".
    private static ArrayList<ArrayList<SDFHead>> searchForAcronym(ArrayList<SDFHead> SDFHeads)throws Exception
    {
        ArrayList<SDFHead> SDFHeadsName = new ArrayList<SDFHead>();
        ArrayList<SDFHead> SDFHeadsAcronym = new ArrayList<SDFHead>();
        int i=0;
        for(;i<SDFHeads.size();i++)
            if(SDFHeads.get(i).getForm().compareToIgnoreCase("(")==0)break;
            else SDFHeadsName.add(SDFHeads.get(i));
        i++;
        for(;i<SDFHeads.size();i++)
            if(SDFHeads.get(i).getForm().compareToIgnoreCase(")")==0)break;
            else SDFHeadsAcronym.add(SDFHeads.get(i));
        i++;
                
            //if we have effectively separated the SDFHead(s) into name and acronym, we return the two ArrayList(s). Otherwise we return null.
        if
        (
            (i==SDFHeads.size())&&
            (SDFHeadsName.size()>0)&&(SDFHeadsName.size()<SDFHeads.size())&&
            (SDFHeadsAcronym.size()>0)&&(SDFHeadsAcronym.size()<SDFHeads.size())
        )
        {
            ArrayList<ArrayList<SDFHead>> ret = new ArrayList<ArrayList<SDFHead>>();
            ret.add(SDFHeadsName);
            ret.add(SDFHeadsAcronym);
            return ret;
        }
                
            //if we are here, we couldn't find either the "(" or the ")"
        return null;
    }

        //The method look for prefixes and suffixes in the Form(s), out of a list of possible values. If it find them, it create new ArrayList(s) of Forms.
        //At the end, all the new ArrayList(s) of Forms, obtained from all possible combinations, are transformed in SDFRule(s).
    private static ArrayList<Element> buildSDFRulesOnPrefixesAndSuffixes(ArrayList<SDFHead> SDFHeads, int priority, String tag)
    {
        try
        {
            String[] availablePrefixes = new String[]{"anti"};
            String[] availableSuffixes = new String[]{};
            
                //We identify all Form(s) and which of them is associated with either a prefix or a suffix (but not to both!)
                //For each Form having a prefix or a suffix, we create and ArrayList<String> of Form(s) that are supposed to substitute the original one.
            ArrayList<String> Forms = new ArrayList<String>();
            Hashtable<String, ArrayList<String>> Forms2decomposedForms = new Hashtable<String, ArrayList<String>>();
            for(int i=0;i<SDFHeads.size();i++)
            {
                String Form = SDFHeads.get(i).getForm().trim();
                Forms.add(Form);
                
                    //The first prefix or suffix we find .. is the right one, we don't search for multiple prefixes or suffixes
                String prefix = null;
                for(int j=0;(j<availablePrefixes.length)&&(prefix==null);j++)
                    if(Form.indexOf(availablePrefixes[j])==0)
                        prefix=availablePrefixes[j];
                String suffix = null;
                for(int j=0;(j<availableSuffixes.length)&&(suffix==null)&&(prefix==null);j++)
                    if(Form.indexOf(availableSuffixes[j])==Form.length()-availableSuffixes[j].length())
                        suffix=availableSuffixes[j];
                
                if(prefix!=null)
                {
                    String temp = Form.substring(prefix.length(), Form.length()).trim();
                    ArrayList<String> newForms = new ArrayList<String>();
                    newForms.add(prefix);
                    newForms.add("-");
                    newForms.add(temp);
                    Forms2decomposedForms.put(Form, newForms);
                }
                
                if(suffix!=null)
                {
                    String temp = Form.substring(0, Form.length()-suffix.length()).trim();
                    ArrayList<String> newForms = new ArrayList<String>();
                    newForms.add(suffix);
                    newForms.add("-");
                    newForms.add(temp);
                    Forms2decomposedForms.put(Form, newForms);
                }
            }
            
                //if the Hashtable is empty, we couldn't find any prefix or suffix: we return the empty set.
            if(Forms2decomposedForms.keys().hasMoreElements()==false)return new ArrayList<Element>();
                
                //Now we generate all possible combinations.
            ArrayList<ArrayList<String>> sequencesOfForms = new ArrayList<ArrayList<String>>();
            
                //We start from Forms. When we manage to find a prefix or suffix, we add to sequencesOfForms all possible combinations.
                //Until there are no more prefixes or suffixes, i.e., until the Hashtable is empty
            sequencesOfForms.add(Forms);
            while(Forms2decomposedForms.keys().hasMoreElements()==true)
            {
                String form2decompose = Forms2decomposedForms.keys().nextElement();
                for(int i=0;i<sequencesOfForms.size();i++)
                {
                    for(int j=0;j<sequencesOfForms.get(i).size();j++)
                    {
                        if(sequencesOfForms.get(i).get(j).compareToIgnoreCase(form2decompose)==0)
                        {
                                //One with the prefix splitted (one for each word)
                            ArrayList<String> newSequence = new ArrayList<String>();
                            for(int k=0;k<j;k++)newSequence.add(sequencesOfForms.get(i).get(k));
                            ArrayList<String> temp = Forms2decomposedForms.get(form2decompose);
                            for(int k=0;k<temp.size();k++)newSequence.add(temp.get(k));
                            for(int k=j+1;k<sequencesOfForms.get(i).size();k++)newSequence.add(sequencesOfForms.get(i).get(k));
                            sequencesOfForms.add(newSequence);
                            
                                //One with the concatenation, e.g. {"anti","+","corruzione"} -> "anticorruzione" 
                            newSequence = new ArrayList<String>();
                            for(int k=0;k<j;k++)newSequence.add(sequencesOfForms.get(i).get(k));
                            temp = Forms2decomposedForms.get(form2decompose);
                            String newWord = "";
                            for(int k=0;k<temp.size();k++)newWord=(newWord+temp.get(k)).trim();
                            newSequence.add(newWord);
                            for(int k=j+1;k<sequencesOfForms.get(i).size();k++)newSequence.add(sequencesOfForms.get(i).get(k));
                            sequencesOfForms.add(newSequence);
                            
                            break;
                        }
                    }
                }
                Forms2decomposedForms.remove(form2decompose);
            }
                //At the end I remove the Forms, i.e., the one at index=0, that I only used to generate the others.
            sequencesOfForms.remove(0);
            
                //Now I create fake SDFHead(s) in order to use the method createSDFRuleOnSDFHeadsLeft2Right for creating the SDFRule(s)
            ArrayList<Element> ret = new ArrayList<Element>();
            for(int i=0;i<sequencesOfForms.size();i++)
            {
                ArrayList<SDFHead> fakeSDFHeads = new ArrayList<SDFHead>();
                for(int j=0;j<sequencesOfForms.get(i).size();j++)fakeSDFHeads.add(new SDFHead(sequencesOfForms.get(i).get(j), "Lemma", "POS", "endOfSentence", null, "Label"));
                Element newSDFRule = createSDFRuleOnSDFHeadsLeft2Right(fakeSDFHeads, priority, 1, tag, new String[]{"Form"});
                if(newSDFRule!=null)ret.add(newSDFRule);
            }
            return ret;
        }
        catch(Exception e){return new ArrayList<Element>();}
    }
        
    private static Element createSDFRuleOnEnglishNameFromWikipedia(String URI, int priority, String tag)throws Exception
    {
            //We try Wikipedia in different languages (add more in the list below)
        URL[] urls = 
        {
            new URL("https://it.wikipedia.org/wiki/"+URI),
            new URL("https://fr.wikipedia.org/wiki/"+URI),
            new URL("https://de.wikipedia.org/wiki/"+URI),
            new URL("https://es.wikipedia.org/wiki/"+URI)
        };
        
        for(int i=0;i<urls.length;i++)
        {
            try
            {
                HttpURLConnection conn = (HttpURLConnection) urls[i].openConnection();
                conn.setRequestMethod("GET");
                if(conn.getResponseCode()!=200)continue;
                BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream()), "UTF8"));
                String crawledText="";
                String buffer;
                while((buffer=br.readLine())!=null)crawledText=crawledText+buffer;
                    //To make is simple, we only take the <body>
                crawledText = crawledText.substring(crawledText.indexOf("<body"), crawledText.indexOf("</body>")+"</body>".length());
                crawledText = crawledText.replaceAll("&", "&amp;");
                Document doc = new SAXBuilder().build(new StringReader(crawledText));
                
                    //We search for a link with written "English" in the text.
                ArrayList<Element> elements = new ArrayList<Element>();
                elements.add(doc.getRootElement());
                while(elements.isEmpty()==false)
                {
                    Element e = elements.remove(0);
                    if(e.getName().compareToIgnoreCase("a")==0)
                    {
                        String text = "";
                        for(int j=0;j<e.getContent().size();j++)
                            if(e.getContent().get(j)instanceof Element)continue;
                            else text = (text + " " +((Text)e.getContent().get(j)).getText()).trim();
                        if(text.compareToIgnoreCase("English")==0)
                        {
                            text = e.getAttributeValue("href");
                            text = text.substring(text.lastIndexOf("/")+1, text.length());
                            if(text.indexOf("_(")!=-1)text=text.substring(0, text.lastIndexOf("_(")).trim();
                            ArrayList<String> Forms = new ArrayList<String>();
                            while(text.indexOf("_")!=-1)
                            {
                                Forms.add(text.substring(0, text.indexOf("_")));
                                text = text.substring(text.indexOf("_")+1, text.length());
                            }
                            Forms.add(text);
                            
                                //We create a fake ArrayList of SDFHead(s) on these Form(s) in order to use the method createSDFRuleOnSDFHeadsLeft2Right
                            ArrayList<SDFHead> fakeSDFHeads = new ArrayList<SDFHead>();
                            for(int j=0;j<Forms.size();j++)fakeSDFHeads.add(new SDFHead(Forms.get(j), "Lemma", "POS", "endOfSentence", null, "Label"));
                            
                            return createSDFRuleOnSDFHeadsLeft2Right(fakeSDFHeads, priority, 1, tag, new String[]{"Form"});
                        }
                    }
                    else for(int j=0;j<e.getContent().size();j++)
                            if(!(e.getContent().get(j)instanceof Element))continue;
                            else elements.add((Element)e.getContent().get(j));
                }
            }catch(Exception e){continue;}
        }       
        
        return null;
    }
}
