package SDFTagger.KBInterface.XMLFilesInterface.XML2SDFCodesCompiler;

import java.util.*;
import org.jdom2.*;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class convert2SDFCode
{
    private String SDFCode = "";
    public String getSdfCode(){return SDFCode;}

    protected long id=-1;
    protected long priority=-1;
    protected ArrayList<String> tags = new ArrayList<String>();
    protected ArrayList<HeadConstraint> headAlternatives = new ArrayList<HeadConstraint>();
    protected ArrayList<Prev> prevAlternatives = new ArrayList<Prev>();
    protected ArrayList<Next> nextAlternatives = new ArrayList<Next>();
    protected ArrayList<Next> nextStarAlternatives = new ArrayList<Next>();
    protected ArrayList<Prev> prevStarAlternatives = new ArrayList<Prev>();
    protected ArrayList<ArrayList<Dependent>> dependentsAlternatives = new ArrayList<ArrayList<Dependent>>();
    protected ArrayList<Governor> governorAlternatives = new ArrayList<Governor>();
    
    public convert2SDFCode(Element e) throws Exception
    {
        if(this.getClass().getName().indexOf("$")==-1)
        {
            prevStarAlternativesCounter = 1;
            prevAlternativesCounter = 1;
            nextStarAlternativesCounter = 1;
            nextAlternativesCounter = 1;
            prevCounter = 1;
            nextCounter = 1;
            dependentsAlternativesCounter = 1;
            dependentsCounter = 1;
            dependentCounter = 1;
            governorAlternativesCounter = 1;
            governorCounter = 1;
            
            List attributes = e.getAttributes();
            for(int i=0;i<attributes.size();i++)
            {
                if(((Attribute)attributes.get(i)).getName().compareToIgnoreCase("id")!=0) 
                {
                    try{id = Long.parseLong(e.getAttributeValue("id").trim() );}
                    catch(Exception ex){throw new Exception("Error #1: id must be an integer greater than zero! "+getExceptionSuffixMessage(e));}
                }
                else if(((Attribute)attributes.get(i)).getName().compareToIgnoreCase("priority")!=0) 
                {
                    try{priority = Integer.parseInt( e.getAttributeValue("priority").trim() );}
                    catch(Exception ex){throw new Exception("Error #1: priority must be an integer greater than zero! "+getExceptionSuffixMessage(e));}
                }
                else throw new Exception("Error #7: Attribute \""+((Attribute)attributes.get(i)).getName()+"\" not allowed on Element "+e.getName()+". "+getExceptionSuffixMessage(e));
            }
            if((id==-1)||(priority==-1))throw new Exception("Error #2: id and Priority are mandatory Attribute(s) of SDFRule. "+getExceptionSuffixMessage(e));
        }
        
        try
        {
            List children = e.getChildren();
            for(int i=0; i<children.size(); i++)
            {
                Element child = (Element)children.get(i);
                if(child.getName().compareToIgnoreCase("tag")==0)tags.add(((Text)child.getContent().get(0)).getText());
                else if(child.getName().compareToIgnoreCase("headAlternatives")==0)
                {
                    if(child.getAttributes().size()>0)
                        throw new Exception("Error #7: Attribute \""+((Attribute)child.getAttributes().get(0)).getName()+"\" not allowed on Element headAlternatives. "+getExceptionSuffixMessage(e));
                    List childrenOfChild = child.getChildren();
                    for(int j=0; j<childrenOfChild.size(); j++)
                    {
                        Element childOfChild = (Element)childrenOfChild.get(j);
                        if(childOfChild.getName().compareToIgnoreCase("head")==0)headAlternatives.add(new HeadConstraint(childOfChild));
                        else throw new Exception("Error #3: Element <"+childOfChild.getName()+"> not allowed within <headAlternatives>. "+getExceptionSuffixMessage(e));
                    }
                    if(headAlternatives.isEmpty())throw new Exception("Error #5: <headAlternatives> must contain at least one <head>. "+getExceptionSuffixMessage(e));
                }
                else if(child.getName().compareToIgnoreCase("prevAlternatives")==0)
                {
                    List childrenOfChild = child.getChildren();
                    for(int j=0; j<childrenOfChild.size(); j++)
                    {
                        Element childOfChild = (Element)childrenOfChild.get(j);
                        if((childOfChild.getName().compareToIgnoreCase("Prev")==0)||(childOfChild.getName().compareToIgnoreCase("notPrev")==0))
                            prevAlternatives.add(new Prev(childOfChild));
                        else throw new Exception("Error #3: Element <"+childOfChild.getName()+"> not allowed within <prevAlternatives>. "+getExceptionSuffixMessage(e));
                    }
                    if(prevAlternatives.isEmpty())throw new Exception("Error #6: <prevAlternatives> must contain at least one <Prev>. "+getExceptionSuffixMessage(e));
                }
                else if(child.getName().compareToIgnoreCase("prevStarAlternatives")==0)
                {
                    List childrenOfChild = child.getChildren();
                    for(int j=0; j<childrenOfChild.size(); j++)
                    {
                        Element childOfChild = (Element)childrenOfChild.get(j);
                        if(childOfChild.getName().compareToIgnoreCase("Prev")==0)prevStarAlternatives.add(new Prev(childOfChild));
                        else throw new Exception("Error #3: Element <"+childOfChild.getName()+"> not allowed within <prevStarAlternatives>. "+getExceptionSuffixMessage(e));
                    }
                    if(prevStarAlternatives.isEmpty())throw new Exception("Error #6: <prevStarAlternatives> must contain at least one <Prev>. "+getExceptionSuffixMessage(e));
                }
                else if(child.getName().compareToIgnoreCase("nextAlternatives")==0)
                {
                    List childrenOfChild = child.getChildren();
                    for(int j=0; j<childrenOfChild.size(); j++)
                    {
                        Element childOfChild = (Element)childrenOfChild.get(j);
                        if((childOfChild.getName().compareToIgnoreCase("Next")==0)||(childOfChild.getName().compareToIgnoreCase("notNext")==0))
                            nextAlternatives.add( new Next(childOfChild) );
                        else throw new Exception("Error #3: Element <"+childOfChild.getName()+"> not allowed within <nextAlternatives>. "+getExceptionSuffixMessage(e));
                    }
                    if(nextAlternatives.isEmpty())throw new Exception("Error #6: <nextAlternatives> must contain at least one <Next>. "+getExceptionSuffixMessage(e));
                }
                else if(child.getName().compareToIgnoreCase("nextStarAlternatives")==0)
                {
                    List childrenOfChild = child.getChildren();
                    for(int j=0; j<childrenOfChild.size(); j++)
                    {
                        Element childOfChild = (Element)childrenOfChild.get(j);
                        if(childOfChild.getName().compareToIgnoreCase("Next")==0)nextStarAlternatives.add(new Next(childOfChild));
                        else throw new Exception("Error #3: Element <"+childOfChild.getName()+"> not allowed within <nextStarAlternatives>. "+getExceptionSuffixMessage(e));
                    }
                    if(nextStarAlternatives.isEmpty())throw new Exception("Error #6: <nextStarAlternatives> must contain at least one <Next>. "+getExceptionSuffixMessage(e));
                }
                else if(child.getName().compareToIgnoreCase("governorAlternatives")==0)
                {
                    List childrenOfChild = child.getChildren();
                    for(int j=0; j<childrenOfChild.size(); j++)
                    {
                        Element childOfChild = (Element)childrenOfChild.get(j);
                        if((childOfChild.getName().compareToIgnoreCase("Governor")==0)||(childOfChild.getName().compareToIgnoreCase("notGovernor")==0))
                            governorAlternatives.add(new Governor(childOfChild));
                        else throw new Exception("Error #3: Element <"+childOfChild.getName()+"> not allowed within <governorAlternatives>. "+getExceptionSuffixMessage(e));
                    }
                    if(governorAlternatives.isEmpty())throw new Exception("Error #6: <governorAlternatives> must contain at least one <Governor>. "+getExceptionSuffixMessage(e));
                }
                else if(child.getName().compareToIgnoreCase("dependentsAlternatives")==0)
                {
                    List childrenOfChild = child.getChildren();
                    for(int j=0; j<childrenOfChild.size(); j++)
                    {
                        Element childOfChild = (Element)childrenOfChild.get(j);
                        if(childOfChild.getName().compareToIgnoreCase("Dependents")==0)
                        {
                            ArrayList<Dependent> dependents = new ArrayList<Dependent>();
                            dependentsAlternatives.add(dependents);
                            
                            List childrenOfChildOfChild = childOfChild.getChildren();
                            for(int k=0; k<childrenOfChildOfChild.size(); k++)
                            {
                                Element childOfChildOfChild = (Element)childrenOfChildOfChild.get(k);
                                if((childOfChildOfChild.getName().compareToIgnoreCase("Dependent")==0)||(childOfChildOfChild.getName().compareToIgnoreCase("notDependent")==0))
                                    dependents.add(new Dependent(childOfChildOfChild));
                                else throw new Exception("Error #3: Element <"+childOfChild.getName()+"> not allowed within <Dependents>. "+getExceptionSuffixMessage(e));
                            }
                            if(dependentsAlternatives.isEmpty())throw new Exception("Error #6: <Dependents> must contain at least one <Dependent>. "+getExceptionSuffixMessage(e));
                        }
                        else throw new Exception("Error #3: Element <"+childOfChild.getName()+"> not allowed within <dependentsAlternatives>. "+getExceptionSuffixMessage(e));
                    }
                    if(dependentsAlternatives.isEmpty())throw new Exception("Error #6: <dependentsAlternatives> must contain at least one <Dependents>. "+getExceptionSuffixMessage(e));
                }
            }
            
            if(headAlternatives.isEmpty())throw new Exception("Error #4: Element "+e.getName()+" must contain one (and only one) headAlternatives. "+getExceptionSuffixMessage(e));
        }
        catch(Exception ex)
        {
            if(ex.getMessage().indexOf("Error #")!=-1)throw ex;
            throw new Exception("Error #9: unforseen exception: \""+ex.getMessage()+"\". "+getExceptionSuffixMessage(e));
        }
        
        SDFCode = buildSdfCode();
    }

    private class HeadConstraint
    {
        protected String Form = null;
        protected String Lemma = null;
        protected String POS = null;
        protected String endOfSentence = null;
        public ArrayList<String> notForm = new ArrayList<String>();
        public ArrayList<String> notLemma = new ArrayList<String>();
        public ArrayList<String> notPOS = new ArrayList<String>();
        protected Hashtable<String, String> optionalFeaturesString = new Hashtable<String, String>();
        protected Hashtable<String, String> optionalFeaturesSDFHead = new Hashtable<String, String>();
        public String Bag = null;
        public ArrayList<String> notInBag = new ArrayList<String>();
        protected HeadConstraint(Element headXMLNode) throws Exception
        {
            try
            {
                List children = headXMLNode.getChildren();
                for(int i=0; i<children.size(); i++)
                {
                    if( ((Element)children.get(i)).getName().compareToIgnoreCase("Form") == 0 )
                    {
                        Form = getValue( ((Element)children.get(i)) );
                    }
                    
                    else if( ((Element)children.get(i)).getName().compareToIgnoreCase("notForm") == 0 )
                    {
                        notForm.add( getValue( ((Element)children.get(i)) ) );
                    }
                    
                    else if( ((Element)children.get(i)).getName().compareToIgnoreCase("Lemma") == 0 )
                    {
                        Lemma = getValue( ((Element)children.get(i)) );
                    }
                    
                    else if( ((Element)children.get(i)).getName().compareToIgnoreCase("notLemma") == 0 )
                    {
                        notLemma.add( getValue( ((Element)children.get(i)) ) );
                    }
                    
                    else if( ((Element)children.get(i)).getName().compareToIgnoreCase("POS") == 0 )
                    {
                        POS = getValue( ((Element)children.get(i)) );
                    }
                    
                    else if( ((Element)children.get(i)).getName().compareToIgnoreCase("notPOS") == 0 )
                    {
                        notPOS.add( getValue( ((Element)children.get(i)) ) );
                    }
                    
                    else if( ((Element)children.get(i)).getName().compareToIgnoreCase("endOfSentence") == 0 )
                    {
                        endOfSentence = getValue( ((Element)children.get(i)) );
                        
                        if((endOfSentence.compareToIgnoreCase("true")!=0)&&(endOfSentence.compareToIgnoreCase("false")!=0))
                            throw new Exception("Error #8: Value \""+endOfSentence+"\" not allowed on Attribute endOfSentence. "+getExceptionSuffixMessage(headXMLNode));
                    }
                    
                    else if( ((Element)children.get(i)).getName().compareToIgnoreCase("Bag") == 0 )
                    {
                        Bag = ((Element)children.get(i)).getAttributeValue("name");
                    }
                    
                    else if( ((Element)children.get(i)).getName().compareToIgnoreCase("notInBag") == 0 )
                    {
                        notInBag.add(((Element)children.get(i)).getAttributeValue("id"));
                    }
                    else
                    {
                        String featureName = ((Element)children.get(i)).getName();
                        String value = getValue( ((Element)children.get(i)) );
                        boolean isOptionalFeaturesSDFHead=false;
                        String temp = value;
                        if(temp.indexOf("headId=")==0)
                        {
                            temp = temp.substring("headId=".length(), temp.length()).trim();
                            try{Integer.parseInt(temp);isOptionalFeaturesSDFHead=true;}
                            catch(Exception e){}
                        }
                        if(isOptionalFeaturesSDFHead==false)optionalFeaturesString.put(featureName, value);
                        else optionalFeaturesSDFHead.put(featureName, temp);
                    }
                }
            }
            catch(Exception e)
            {
                throw new Exception("Wrong constraint, rule id="+id);
            }
        }
        
        protected String getValue(Element e) throws Exception
        {
            try
            {
                List children = e.getContent();
                for(int i=0; i<children.size(); i++)
                {
                    if( !(children.get(i) instanceof Text) ) continue;

                    String text = ((Text)children.get(i)).getText().trim();
                    if(text.isEmpty() == true) continue;

                    return text;
                }
            }
            catch(Exception ex)
            {
                throw new Exception("Wrong constraint, rule id="+id);
            }

            throw new Exception("Wrong constraint, rule id="+id);
        }
    }
                    
    private class Prev extends convert2SDFCode
    {
        protected int maxDistance = -1;
        protected boolean not = false;

        public Prev(Element e) throws Exception
        {
            super(e);
            
            if(e.getName().compareToIgnoreCase("notPrev")==0)not=true;

            boolean maxDistanceSpecified = false;
            List attributes = e.getAttributes();
            for(int j=0;j<attributes.size();j++)
            {
                if(((Attribute)attributes.get(j)).getName().compareToIgnoreCase("maxDistance")==0)
                    try{maxDistance = Integer.parseInt(e.getAttributeValue("maxDistance"));maxDistanceSpecified=true;}
                    catch(Exception ex){throw new Exception("Error #1: maxDistance must be an integer greater than zero! "+getExceptionSuffixMessage(e));}
                else throw new Exception("Error #7: Attribute \""+((Attribute)attributes.get(j)).getName()+"\" not allowed on Element "+e.getName()+". "+getExceptionSuffixMessage(e));
            }
            if(maxDistanceSpecified==false)throw new Exception("Error #2: maxDistance is a mandatory Attribute of Prev. "+getExceptionSuffixMessage(e));
            else if(maxDistance<=0)throw new Exception("Error #1: maxDistance must be an integer greater than zero! "+getExceptionSuffixMessage(e));
        }
        
        public String getSdfCode()
        {
            String ret = "";
            if(not==true)ret="£D";
            return ret + "£C" + maxDistance + "£C" + super.getSdfCode();
        }
    }

    private class Next extends convert2SDFCode
    {
        protected int maxDistance = -1;
        protected boolean not = false;
        
        public Next(Element e) throws Exception
        {
            super(e);

            if(e.getName().compareToIgnoreCase("notNext")==0)not=true;
            
            boolean maxDistanceSpecified = false;
            List attributes = e.getAttributes();
            for(int j=0;j<attributes.size();j++)
            {
                if(((Attribute)attributes.get(j)).getName().compareToIgnoreCase("maxDistance")==0)
                    try{maxDistance = Integer.parseInt(e.getAttributeValue("maxDistance"));maxDistanceSpecified=true;}
                    catch(Exception ex){throw new Exception("Error #1: maxDistance must be an integer greater than zero! "+getExceptionSuffixMessage(e));}
                else throw new Exception("Error #7: Attribute \""+((Attribute)attributes.get(j)).getName()+"\" not allowed on Element Next. "+getExceptionSuffixMessage(e));
            }
            if(maxDistanceSpecified==false)throw new Exception("Error #2: maxDistance is a mandatory Attribute of Next. "+getExceptionSuffixMessage(e));
            else if(maxDistance<=0)throw new Exception("Error #1: maxDistance must be an integer greater than zero! "+getExceptionSuffixMessage(e));
        }

        public String getSdfCode()
        {
            String ret = "";
            if(not==true)ret="£D";
            return ret + "£C" + maxDistance + "£C" + super.getSdfCode();
        }
    }
    
    private class Governor extends convert2SDFCode
    {
        protected int maxHeight = -1;
        protected boolean not = false;
        protected ArrayList<String> labelAlternatives = new ArrayList<String>();

        public Governor(Element e) throws Exception
        {
            super(e);
            
            if(e.getName().compareToIgnoreCase("notGovernor")==0)not=true;
          
            boolean maxDistanceSpecified = false;
            List attributes = e.getAttributes();
            for(int j=0;j<attributes.size();j++)
            {
                if(((Attribute)attributes.get(j)).getName().compareToIgnoreCase("maxHeight")==0)
                    try{maxHeight = Integer.parseInt(e.getAttributeValue("maxHeight"));maxDistanceSpecified=true;}
                    catch(Exception ex){throw new Exception("Error #1: maxHeight must be an integer greater than zero! "+getExceptionSuffixMessage(e));}
                else throw new Exception("Error #7: Attribute \""+((Attribute)attributes.get(j)).getName()+"\" not allowed on Element Governor. "+getExceptionSuffixMessage(e));
            }
            if(maxDistanceSpecified==false)throw new Exception("Error #2: maxHeight is a mandatory Attribute of Governor. "+getExceptionSuffixMessage(e));
            else if(maxHeight<=0)throw new Exception("Error #1: maxHeight must be an integer greater than zero! "+getExceptionSuffixMessage(e));
            
            try
            {
                List children = e.getChildren();
                for(int i=0; i<children.size(); i++)
                {
                    Element child = (Element)children.get(i);

                    if(child.getName().compareToIgnoreCase("labelAlternatives")==0)
                    {
                        List childrenOfChild = child.getContent();
                        for(int j=0; j<childrenOfChild.size(); j++)
                        {
                            if(!(childrenOfChild.get(j) instanceof Element)) continue;
                            Element childOfChild = (Element)childrenOfChild.get(j);
                            String s = ((Text)childOfChild.getContent().get(0)).getText();
                            labelAlternatives.add(s);
                        }
                    }
                }
            }
            catch(Exception ex)
            {
                throw new Exception("it was not possible to build a <dependent>");
            }
        }
        
        public String getSdfCode()
        {
            String ret = "";
            if(not==true)ret="£D";
            if(labelAlternatives.size()>0)ret=ret+"£E";
            for(int i=0;i<labelAlternatives.size();i++)ret=ret+labelAlternatives.get(i)+"£E";
            return ret + "£C" + maxHeight + "£C" + super.getSdfCode();
        }
    }
    
    private class Dependent extends convert2SDFCode
    {
        protected int maxDepth = -1;
        protected boolean not = false;
        protected ArrayList<String> labelAlternatives = new ArrayList<String>();

        public Dependent(Element e) throws Exception
        {
            super(e);
            
            if(e.getName().compareToIgnoreCase("notDependent")==0)not=true;
 
            boolean maxDepthSpecified = false;
            List attributes = e.getAttributes();
            for(int j=0;j<attributes.size();j++)
            {
                if(((Attribute)attributes.get(j)).getName().compareToIgnoreCase("maxDepth")==0)
                    try{maxDepth = Integer.parseInt(e.getAttributeValue("maxDepth"));maxDepthSpecified=true;}
                    catch(Exception ex){throw new Exception("Error #1: maxDepth must be an integer greater than zero! "+getExceptionSuffixMessage(e));}
                else throw new Exception("Error #7: Attribute \""+((Attribute)attributes.get(j)).getName()+"\" not allowed on Element Dependent. "+getExceptionSuffixMessage(e));
            }
            if(maxDepthSpecified==false)throw new Exception("Error #2: maxDepth is a mandatory Attribute of Dependent. "+getExceptionSuffixMessage(e));
            else if(maxDepth<=0)throw new Exception("Error #1: maxDepth must be an integer greater than zero! "+getExceptionSuffixMessage(e));
            
            try
            {
                List children = e.getChildren();
                for(int i=0; i<children.size(); i++)
                {
                    Element child = (Element)children.get(i);

                    if(child.getName().compareToIgnoreCase("labelAlternatives")==0)
                    {
                        List childrenOfChild = child.getContent();
                        for(int j=0; j<childrenOfChild.size(); j++)
                        {
                            if(!(childrenOfChild.get(j) instanceof Element)) continue;
                            Element childOfChild = (Element)childrenOfChild.get(j);
                            String s = ((Text)childOfChild.getContent().get(0)).getText();
                            labelAlternatives.add(s);
                        }
                    }
                }
            }
            catch(Exception ex)
            {
                throw new Exception("it was not possible to build a <dependent>");
            }
        }
        
        public String getSdfCode()
        {
            String ret = "";
            if(not==true)ret="£D";
            if(labelAlternatives.size()>0)ret=ret+"£E";
            for(int i=0;i<labelAlternatives.size();i++)ret=ret+labelAlternatives.get(i)+"£E";
            return ret + "£C" + maxDepth + "£C" + super.getSdfCode();
        }
    }

    private static String getExceptionSuffixMessage(Element e)
    {
        String ret = " Check the user manual, subsection \"Well-formed SDFRule(s)\".\n\n\n";
        XMLOutputter outputter = new XMLOutputter();
        outputter.setFormat(Format.getPrettyFormat());
        Document doc = new Document();
        doc.setRootElement((Element)e.clone());
        return ret+outputter.outputString(doc);
    }
    
    private static int prevStarAlternativesCounter = 1;
    private static int prevAlternativesCounter = 1;
    private static int nextStarAlternativesCounter = 1;
    private static int nextAlternativesCounter = 1;
    private static int prevCounter = 1;
    private static int nextCounter = 1;
    private static int dependentsAlternativesCounter = 1;
    private static int dependentsCounter = 1;
    private static int dependentCounter = 1;
    private static int governorAlternativesCounter = 1;
    private static int governorCounter = 1;
    private String buildSdfCode() throws Exception
    {
        if(id==1)
            id=id;
        
        String ret = "";
        if(this.getClass().getName().indexOf("$")==-1)ret=id+"£"+priority+"£";
        if(tags.size()>0)ret=ret+"@";
        for(int i=0;i<tags.size();i++)ret=ret+tags.get(i)+"@";
        
        if(headAlternatives.size()>0)
        {
            ret = ret + "$";
            for(int i=0; i<headAlternatives.size(); i++)
            {
                ret = ret + "#";
                HeadConstraint hc = (HeadConstraint)headAlternatives.get(i);
                if(hc.Bag!=null)ret = ret + "£a" + hc.Bag + "£";
                if(hc.notInBag.isEmpty()==false)for(int j=0;j<hc.notInBag.size();j++)ret=ret+"£b"+hc.notInBag.get(j)+"£";
                if((hc.Form!=null)&&(hc.Form.isEmpty()==false)) ret = ret + "£c" + hc.Form + "£";
                if((hc.Lemma!=null)&&(hc.Lemma.isEmpty()==false)) ret = ret + "£d" + hc.Lemma + "£";
                if((hc.POS!=null)&&(hc.POS.isEmpty()==false)) ret = ret + "£e" + hc.POS + "£";
                if((hc.endOfSentence!=null)&&(hc.endOfSentence.isEmpty()==false)) ret = ret + "£f" + hc.endOfSentence + "£";
                if(hc.notForm.isEmpty()==false)for(int j=0; j<hc.notForm.size(); j++) ret = ret + "£g" + hc.notForm.get(j) + "£";
                if(hc.notLemma.isEmpty()==false)for(int j=0; j<hc.notLemma.size(); j++) ret = ret + "£h" + hc.notLemma.get(j) + "£";
                if(hc.notPOS.isEmpty()==false)for(int j=0; j<hc.notPOS.size(); j++) ret = ret + "£i" + hc.notPOS.get(j) + "£";
                Enumeration enOFS = hc.optionalFeaturesString.keys();
                while(enOFS.hasMoreElements())
                {
                    String key = (String)enOFS.nextElement();
                    ret = ret + "£j" + key + "£k" + hc.optionalFeaturesString.get(key) + "£";
                }
                Enumeration enOFSDFHead = hc.optionalFeaturesSDFHead.keys();
                while(enOFSDFHead.hasMoreElements())
                {
                    String key = (String)enOFSDFHead.nextElement();
                    ret = ret + "£l" + key + "£m" + hc.optionalFeaturesSDFHead.get(key) + "£";
                }
                ret = ret + "#";
            }
            ret = ret + "$";
        }

        if(prevStarAlternatives.isEmpty()==false)
        {
            ret = ret + "ç"+prevStarAlternativesCounter + ",";
            for(int i=0; i<prevStarAlternatives.size(); i++)
            {
                ret = ret + "+" + prevCounter + ",";
                ret = ret + ((Prev)prevStarAlternatives.get(i)).getSdfCode() + "+" + prevCounter + ",";
                prevCounter++;
            }
            ret = ret + "ç"+prevStarAlternativesCounter + ",";
            prevStarAlternativesCounter++;
        }

        if(prevAlternatives.isEmpty()==false)
        {
            ret = ret + "%"+prevAlternativesCounter + ",";
            for(int i=0; i<prevAlternatives.size(); i++)
            {
                ret = ret + "+" + prevCounter + ",";
                ret = ret + ((Prev)prevAlternatives.get(i)).getSdfCode() + "+" + prevCounter + ",";
                prevCounter++;
            }
            ret = ret + "%"+prevAlternativesCounter + ",";
            prevAlternativesCounter++;
        }
        
        if(nextStarAlternatives.isEmpty()==false)
        {
            ret = ret + "?"+nextStarAlternativesCounter + ",";
            for(int i=0; i<nextStarAlternatives.size(); i++)
            {
                ret = ret + "-" + nextCounter + ",";
                ret = ret + ((Next)nextStarAlternatives.get(i)).getSdfCode() + "-" + nextCounter + ",";
                nextCounter++;
            }
            ret = ret + "?"+nextStarAlternativesCounter + ",";
            nextStarAlternativesCounter++;
        }
        
        if(nextAlternatives.isEmpty()==false)
        {
            ret = ret + "!"+nextAlternativesCounter + ",";
            for(int i=0; i<nextAlternatives.size(); i++)
            {
                ret = ret + "-" + nextCounter + ",";
                ret = ret + ((Next)nextAlternatives.get(i)).getSdfCode() + "-" + nextCounter + ",";
                nextCounter++;
            }
            ret = ret + "!"+nextAlternativesCounter + ",";
            nextAlternativesCounter++;
        }
        
        if(governorAlternatives.isEmpty()==false)
        {
            ret = ret + "&"+governorAlternativesCounter + ",";
            for(int i=0; i<governorAlternatives.size(); i++)
            {
                ret = ret + "=" + governorCounter + ",";
                ret = ret + ((Governor)governorAlternatives.get(i)).getSdfCode() + "=" + governorCounter + ",";
                governorCounter++;
            }
            ret = ret + "&"+governorAlternativesCounter + ",";
            governorAlternativesCounter++;
        }
        
        if(dependentsAlternatives.isEmpty()==false)
        {
            ret = ret + ";"+dependentsAlternativesCounter + ",";
            for(int i=0; i<dependentsAlternatives.size(); i++)
            {
                ArrayList<Dependent> dependents = dependentsAlternatives.get(i);
                
                ret = ret + "."+dependentsCounter + ",";
                for(int j=0; j<dependents.size(); j++)
                {
                    ret = ret + "*"+dependentCounter + ",";
                    Dependent dep = (Dependent)dependents.get(j);
                    ret = ret + dep.getSdfCode();
                    ret = ret + "*"+dependentCounter + ",";
                    dependentCounter++;
                }
                ret = ret + "."+dependentsCounter + ",";
                dependentsCounter++;
            }
            ret = ret + ";"+dependentsAlternativesCounter + ",";
            dependentsAlternativesCounter++;
        }
        
        return ret;
    }
}