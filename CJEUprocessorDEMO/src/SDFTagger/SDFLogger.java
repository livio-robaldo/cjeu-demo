package SDFTagger;

import java.io.*;
import java.util.*;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.*;
import SDFTagger.SDFItems.*;

public class SDFLogger 
{
    private File logFile = null;
    public SDFLogger(File logFile)throws Exception
    {
        this.logFile=logFile;
        if(logFile!=null)while(logFile.exists())logFile.delete();
    }
    
    private Hashtable<String, SDFRule> id2SDFRules = new Hashtable<String, SDFRule>();
    private Hashtable<SDFRule, Element> htSDFRuleSteps2Elements = new Hashtable<SDFRule, Element>();
    private ArrayList<Element> StepsToWriteInSDFDebug = new ArrayList<Element>();
    protected void startTracingOnSDFNode(SDFRule SDFRuleStep, long idInstance, SDFNode SDFNode)
    {
        if(logFile==null)return;
        
        Element Step = new Element("Step");
        htSDFRuleSteps2Elements.put(SDFRuleStep, Step);        
        Step.setAttribute("onSDFNode", ""+SDFNode.index);
        if(stepsIntoStack.isEmpty()==false)stepsIntoStack.get(stepsIntoStack.size()-1).getContent().add(Step);
        String className = SDFRuleStep.getClass().getName();
        if(className.lastIndexOf(".")!=-1)className=className.substring(className.lastIndexOf(".")+1, className.length());
        if(className.lastIndexOf("$")!=-1)className=className.substring(className.lastIndexOf("$")+1, className.length());
        if(className.compareToIgnoreCase("SDFRule")==0)
        {
            Step.setAttribute("id", ""+SDFRuleStep.id);
            Step.setAttribute("idInstance", ""+idInstance);
            Step.setAttribute("priority", ""+SDFRuleStep.priority);
            if(id2SDFRules.get(""+SDFRuleStep.id)==null)id2SDFRules.put(""+SDFRuleStep.id, SDFRuleStep);
        }
    }
    
    protected void startTracingOnNoSDFNodes(SDFRule SDFRuleStep)
    {
        if(logFile==null)return;
        Element Step = new Element("Step");
        htSDFRuleSteps2Elements.put(SDFRuleStep, Step);
        Step.setAttribute("onSDFNode", "none");
        if(stepsIntoStack.isEmpty()==false)stepsIntoStack.get(stepsIntoStack.size()-1).getContent().add(Step);
    }
    
    private ArrayList<Element> stepsIntoStack = new ArrayList<Element>();
    protected void stepInto(SDFRule SDFRuleStep, String stepIntoWhat)
    {
        if(logFile==null)return;
        Element stepIntoWhatXML = new Element(stepIntoWhat);
        stepsIntoStack.add(stepIntoWhatXML);
        htSDFRuleSteps2Elements.get(SDFRuleStep).getContent().add(stepIntoWhatXML);
    }
    
    protected void stepOut()
    {
        if(logFile==null)return;
        stepsIntoStack.remove(stepsIntoStack.size()-1);
    }
    
    protected void endTracing(SDFRule SDFRuleStep, boolean satisfied)
    {
        if(logFile==null)return;    
        Element Step = htSDFRuleSteps2Elements.get(SDFRuleStep);
        if(satisfied==true)Step.setAttribute("result", "OK");
        else Step.setAttribute("result", "FAILED");
        String className = SDFRuleStep.getClass().getName();
        if(className.lastIndexOf(".")!=-1)className=className.substring(className.lastIndexOf(".")+1, className.length());
        if(className.lastIndexOf("$")!=-1)className=className.substring(className.lastIndexOf("$")+1, className.length());
        if(className.compareToIgnoreCase("SDFRule")==0)StepsToWriteInSDFDebug.add(Step);
    }
    
    protected synchronized void writeInLogFile(SDFNode firstSDFNode, ArrayList<SDFTag> tags)throws Exception
    {
        if(logFile==null)return;
        if((id2SDFRules==null)||(id2SDFRules.keys().hasMoreElements()==false))return;            
        try
        {
            Document SDFDebugDoc = new Document();
            SDFDebugDoc.setRootElement(new Element("SDFDebugs"));
            if(logFile.exists())SDFDebugDoc = (Document)new SAXBuilder().build(logFile);
            Element SDFDebugs = SDFDebugDoc.getRootElement();
            if(SDFDebugs.getName().compareToIgnoreCase("SDFDebugs")!=0)throw new Exception();
            SDFDebugs.getContent().add(generateNewSDFDebug(firstSDFNode, tags));
            XMLOutputter outputter = new XMLOutputter();
            outputter.setFormat(Format.getPrettyFormat().setEncoding("UTF-8"));
            FileOutputStream fos = new FileOutputStream(logFile);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
            outputter.output(SDFDebugDoc, osw);
            osw.close();
            fos.close();
        }catch(Exception e)
        {
            throw new Exception("Unknown format in SDFDebug file "+logFile.getName());
        }
        finally{id2SDFRules.clear();htSDFRuleSteps2Elements.clear();StepsToWriteInSDFDebug.clear();}
    }
    
    private Element generateNewSDFDebug(SDFNode firstSDFNode, ArrayList<SDFTag> tags)
    {
        Element SDFDebug = new Element("SDFDebug");
        Element SDFNodes = new Element("SDFNodes");
        SDFDebug.getContent().add(SDFNodes);
        Element SDFRules = new Element("SDFRules");
        SDFDebug.getContent().add(SDFRules);
        Element SDFTraces = new Element("SDFTraces");
        SDFDebug.getContent().add(SDFTraces);
        Element SDFTags = new Element("SDFTags");
        SDFDebug.getContent().add(SDFTags);
        Hashtable<SDFHead, SDFNode> SDFHeads2SDFNodes = new Hashtable<SDFHead, SDFNode>();
        SDFNode tempSDFNode = firstSDFNode.nextSDFNode;
        while(tempSDFNode!=null){SDFHeads2SDFNodes.put(tempSDFNode.SDFHead, tempSDFNode);tempSDFNode=tempSDFNode.nextSDFNode;}
        tempSDFNode = firstSDFNode.nextSDFNode;
        while(tempSDFNode!=null)
        {
            Element SDFNode = new Element("SDFNode");
            SDFNodes.getContent().add(SDFNode);
            Element index = new Element("index");
            SDFNode.getContent().add(index);
            index.getContent().add(new Text(""+tempSDFNode.index));
            Element Form = new Element("Form");
            SDFNode.getContent().add(Form);
            Form.getContent().add(new Text(tempSDFNode.SDFHead.getForm()));
            Element Lemma = new Element("Lemma");
            SDFNode.getContent().add(Lemma);
            Lemma.getContent().add(new Text(tempSDFNode.SDFHead.getLemma()));
            Element POS = new Element("POS");
            SDFNode.getContent().add(POS);
            POS.getContent().add(new Text(tempSDFNode.SDFHead.getPOS()));
            if((tempSDFNode.SDFHead.getEndOfSentence()!=null)&&(tempSDFNode.SDFHead.getEndOfSentence().length()>0))
            {
                Element endOfSentence = new Element("endOfSentence");
                SDFNode.getContent().add(endOfSentence);
                endOfSentence.getContent().add(new Text(tempSDFNode.SDFHead.getEndOfSentence()));
            }
              
            ArrayList<String> optionalFeatures = tempSDFNode.SDFHead.listOptionalFeatures();
            for(int i=0; i<optionalFeatures.size(); i++)
            {
                Element optionalFeature = new Element(optionalFeatures.get(i));
                SDFNode.getContent().add(optionalFeature);
                optionalFeature.getContent().add(new Text(tempSDFNode.SDFHead.getOptionalFeaturesValue(optionalFeatures.get(i))));
            }
            
            Element Governor = new Element("Governor");
            SDFNode.getContent().add(Governor);
            SDFHead gov = tempSDFNode.SDFHead.getGovernor();
            if(gov==null)Governor.getContent().add(new Text("ROOT"));
            else Governor.getContent().add(new Text(""+SDFHeads2SDFNodes.get(gov).index));
            
            Element Label = new Element("Label");
            SDFNode.getContent().add(Label);
            Label.getContent().add(new Text(tempSDFNode.SDFHead.getLabel()));
            
            tempSDFNode = tempSDFNode.nextSDFNode;
        }
        for(int i=0;i<tags.size();i++)
        {
            Element SDFTag = new Element("SDFTag");
            SDFTags.getContent().add(SDFTag);
            SDFTag.setAttribute("tag", tags.get(i).tag);
            SDFTag.setAttribute("id", ""+tags.get(i).idSDFRule);
            SDFTag.setAttribute("idInstance", ""+tags.get(i).idInstance);
            SDFTag.setAttribute("priority", ""+tags.get(i).priority);
            SDFTag.setAttribute("onSDFNode", ""+SDFHeads2SDFNodes.get(tags.get(i).taggedHead).index);
        }
        Enumeration keys = id2SDFRules.keys();
        while(keys.hasMoreElements())
        {
            String key = (String)keys.nextElement();
            
            SDFRule SDFRule =  id2SDFRules.get(key);
            SDFRules.getContent().add(SDFRule.buildSDFRuleXML());
        }
        
        for(int i=0;i<StepsToWriteInSDFDebug.size();i++)
            SDFTraces.getContent().add(StepsToWriteInSDFDebug.get(i));
        
        return SDFDebug;
    }
}
