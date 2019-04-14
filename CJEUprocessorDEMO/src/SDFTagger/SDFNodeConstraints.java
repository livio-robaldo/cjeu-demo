package SDFTagger;

import java.util.*;
import org.jdom2.*;
import SDFTagger.SDFItems.*;

public class SDFNodeConstraints 
{
    protected SDFNodeConstraints FactorySDFNodeConstraints(){return new SDFNodeConstraints();}
    protected boolean checkOptionalFeatures(Hashtable<String, String> optionalFeatures, SDFHead SDFHead){return true;}
    protected ArrayList<String> tags = new ArrayList<String>();    
    private SDFRule owner = null;
    private ArrayList<SDFHeadConstraints> headAlternatives = new ArrayList<SDFHeadConstraints>();
    private class SDFHeadConstraints
    {
        private String Form = null;
        private String Lemma = null;
        private String POS = null;
        private String endOfSentence = null;
        private ArrayList<String> notForm = new ArrayList<String>();
        private ArrayList<String> notLemma = new ArrayList<String>();
        private ArrayList<String> notPOS = new ArrayList<String>();
        private String Bag = null;
        private ArrayList<String> notInBag = new ArrayList<String>();
        private Hashtable<String, String> optionalFeatures = new Hashtable<String, String>();        
        private Element buildSDFHeadXML()
        {
            Element head = new Element("head");
            if(Form!=null)
            {
                Element Form = new Element("Form");
                head.getContent().add(Form);
                Form.getContent().add(new Text(this.Form));
            }
            
            if(Lemma!=null)
            {
                Element Lemma = new Element("Lemma");
                head.getContent().add(Lemma);
                Lemma.getContent().add(new Text(this.Lemma));
            }
            
            if(POS!=null)
            {
                Element POS = new Element("POS");
                head.getContent().add(POS);
                POS.getContent().add(new Text(this.POS));
            }

            if(endOfSentence!=null)
            {
                Element endOfSentence = new Element("endOfSentence");
                head.getContent().add(endOfSentence);
                endOfSentence.getContent().add(new Text(this.endOfSentence));
            }
            
            for(int i=0;i<notForm.size();i++)
            {
                Element notForm = new Element("notForm");
                head.getContent().add(notForm);
                notForm.getContent().add(new Text(this.notForm.get(i)));
            }
            
            for(int i=0;i<notLemma.size();i++)
            {
                Element notLemma = new Element("notLemma");
                head.getContent().add(notLemma);
                notLemma.getContent().add(new Text(this.notLemma.get(i)));
            }
            
            for(int i=0;i<notPOS.size();i++)
            {
                Element notPOS = new Element("notPOS");
                head.getContent().add(notPOS);
                notPOS.getContent().add(new Text(this.notPOS.get(i)));
            }
            
            if(Bag!=null)
            {
                Element Bag = new Element("Bag");
                head.getContent().add(Bag);
                Bag.setAttribute("name", this.Bag);
            }
            
            for(int i=0;i<notInBag.size();i++)
            {
                Element notInBag = new Element("notInBag");
                head.getContent().add(notInBag);
                notInBag.setAttribute("name", this.notInBag.get(i));
            }
            
            Enumeration en = optionalFeatures.keys();
            while(en.hasMoreElements())
            {
                String key = (String)en.nextElement();
                Element optionalFeature = new Element(key);
                head.getContent().add(optionalFeature);
                optionalFeature.getContent().add(new Text(optionalFeatures.get(key)));
            }
            
            return head;
        }
    }
    
    protected boolean doesSDFNodeMatch(SDFNode SDFNode)
    {
        for(int i=0; i<headAlternatives.size(); i++)
        {
            if(checkMandatoryFeatures(headAlternatives.get(i), SDFNode)==true)
            {
                if(checkOptionalFeatures(headAlternatives.get(i).optionalFeatures, SDFNode.SDFHead)==true)
                {
                    return true;
                }
            }
        }
        
        return false;
    }
   
    protected void loadAttributes(String SDFCodeOfSDFNode, SDFRule owner)
    {
        this.owner = owner;
        
        if(SDFCodeOfSDFNode.indexOf("@")==0)
        {
            while((SDFCodeOfSDFNode.indexOf("@")==0)&&(SDFCodeOfSDFNode.indexOf("@$")!=0))
            {
                tags.add(SDFCodeOfSDFNode.substring(1, SDFCodeOfSDFNode.indexOf("@",1)));
                SDFCodeOfSDFNode=SDFCodeOfSDFNode.substring(SDFCodeOfSDFNode.indexOf("@",1),SDFCodeOfSDFNode.length());
            }
            SDFCodeOfSDFNode = SDFCodeOfSDFNode.substring(1,SDFCodeOfSDFNode.length());
        }
        
        if(SDFCodeOfSDFNode.indexOf("$")==0)
        {
            String headAlternativesCode = SDFCodeOfSDFNode.substring(1, SDFCodeOfSDFNode.length()-1);
            while(headAlternativesCode.isEmpty()==false)
            {
                String headCode = headAlternativesCode.substring(1, headAlternativesCode.indexOf("#",1));
                headAlternativesCode = headAlternativesCode.substring(headAlternativesCode.indexOf("#",1)+1, headAlternativesCode.length());
                SDFHeadConstraints hc = new SDFHeadConstraints();
                if(headCode.indexOf("£a")==0)
                {
                    hc.Bag = headCode.substring(2, headCode.indexOf("£",1));
                    headCode = headCode.substring(headCode.indexOf("£",1)+1, headCode.length());
                }
                while(headCode.indexOf("£b")==0)
                {
                    hc.notInBag.add(headCode.substring(2, headCode.indexOf("£",1)));
                    headCode = headCode.substring(headCode.indexOf("£",1)+1, headCode.length());
                }
                if(headCode.indexOf("£c")==0)
                {
                    hc.Form = headCode.substring(2, headCode.indexOf("£",1));
                    headCode = headCode.substring(headCode.indexOf("£",1)+1, headCode.length());
                }
                if(headCode.indexOf("£d")==0)
                {
                    hc.Lemma = headCode.substring(2, headCode.indexOf("£",1));
                    headCode = headCode.substring(headCode.indexOf("£",1)+1, headCode.length());
                }
                if(headCode.indexOf("£e")==0)
                {
                    hc.POS = headCode.substring(2, headCode.indexOf("£",1));
                    headCode = headCode.substring(headCode.indexOf("£",1)+1, headCode.length());
                }
                if(headCode.indexOf("£f")==0)
                {
                    hc.endOfSentence = headCode.substring(2, headCode.indexOf("£",1));
                    headCode = headCode.substring(headCode.indexOf("£",1)+1, headCode.length());
                }
                while(headCode.indexOf("£g")==0)
                {
                    hc.notForm.add(headCode.substring(2, headCode.indexOf("£",1)));
                    headCode = headCode.substring(headCode.indexOf("£",1)+1, headCode.length());
                }
                while(headCode.indexOf("£h")==0)
                {
                    hc.notLemma.add(headCode.substring(2, headCode.indexOf("£",1)));
                    headCode = headCode.substring(headCode.indexOf("£",1)+1, headCode.length());
                }
                while(headCode.indexOf("£i")==0)
                {
                    hc.notPOS.add(headCode.substring(2, headCode.indexOf("£",1)));
                    headCode = headCode.substring(headCode.indexOf("£",1)+1, headCode.length());
                }
                while(headCode.indexOf("£j")==0)
                {
                    String key = headCode.substring(2, headCode.indexOf("£k"));
                    headCode = headCode.substring(headCode.indexOf("£k")+2, headCode.length());
                    String value = headCode.substring(0, headCode.indexOf("£"));
                    headCode = headCode.substring(headCode.indexOf("£")+1, headCode.length());
                    hc.optionalFeatures.put(key, value);
                }
                
                headAlternatives.add(hc);
            }
        }
        
        if(headAlternatives.isEmpty())headAlternatives.add(new SDFHeadConstraints());
    }

    private boolean checkMandatoryFeatures(SDFHeadConstraints SDFHeadConstraints, SDFNode SDFNode)
    {
        if(SDFHeadConstraints.Form!=null)
        {
            if(SDFHeadConstraints.Form.compareToIgnoreCase(SDFNode.SDFHead.getForm()) != 0) return false;
        }

        for(int i=0; i<SDFHeadConstraints.notForm.size(); i++)
            if(SDFHeadConstraints.notForm.get(i).compareToIgnoreCase(SDFNode.SDFHead.getForm())==0) 
                return false;

        if(SDFHeadConstraints.Lemma!=null)
        {
            if(SDFHeadConstraints.Lemma.compareToIgnoreCase(SDFNode.SDFHead.getLemma()) != 0) return false;
        }

        for(int i=0; i<SDFHeadConstraints.notLemma.size(); i++)
            if(SDFHeadConstraints.notLemma.get(i).compareToIgnoreCase(SDFNode.SDFHead.getLemma())==0) 
                return false;

        if(SDFHeadConstraints.POS!=null)
        {
            if(SDFHeadConstraints.POS.compareToIgnoreCase(SDFNode.SDFHead.getPOS())!=0) return false;
        }

        for(int i=0; i<SDFHeadConstraints.notPOS.size(); i++)
            if(SDFHeadConstraints.notPOS.get(i).compareToIgnoreCase(SDFNode.SDFHead.getPOS())==0)
                return false;

        if(SDFHeadConstraints.endOfSentence!=null)
        {
            if((SDFHeadConstraints.endOfSentence.compareToIgnoreCase("true")==0)&&(SDFNode.endOfSentence==false))return false;
            if((SDFHeadConstraints.endOfSentence.compareToIgnoreCase("false")==0)&&(SDFNode.endOfSentence==true))return false;
        }

        if(SDFHeadConstraints.Bag!=null)
        {
            boolean found = false;
            for(int i=0;(SDFNode.bagsOnForm!=null)&&(found==false)&&(i<SDFNode.bagsOnForm.size());i++)
                if(SDFNode.bagsOnForm.get(i).compareToIgnoreCase(SDFHeadConstraints.Bag)==0)
                    found=true;
            for(int i=0;(SDFNode.bagsOnLemma!=null)&&(found==false)&&(i<SDFNode.bagsOnLemma.size());i++)
                if(SDFNode.bagsOnLemma.get(i).compareToIgnoreCase(SDFHeadConstraints.Bag)==0)
                    found=true;
            if(found==false)return false;
        }
        
        if(SDFHeadConstraints.notInBag!=null)
        {
            boolean found = false;
            for(int j=0; (found==false)&&(j<SDFHeadConstraints.notInBag.size()); j++)
            {
                for(int i=0;(SDFNode.bagsOnForm!=null)&&(found==false)&&(i<SDFNode.bagsOnForm.size());i++)
                    if(SDFNode.bagsOnForm.get(i).compareToIgnoreCase(SDFHeadConstraints.notInBag.get(j))==0)
                        found=true;
                for(int i=0;(SDFNode.bagsOnLemma!=null)&&(found==false)&&(i<SDFNode.bagsOnLemma.size());i++)
                    if(SDFNode.bagsOnLemma.get(i).compareToIgnoreCase(SDFHeadConstraints.notInBag.get(j))==0)
                        found=true;
            }
            if(found==true)return false;
        }

        return true;
    }
    
    protected Element[] buildTagsAndHeadAlternativesXML()
    {
        Element[] ret = new Element[1+tags.size()];
        for(int i=0;i<tags.size();i++)
        {
            ret[i] = new Element("tag");
            ret[i].getContent().add(new Text(tags.get(i)));
        }
                
        ret[ret.length-1] = new Element("headAlternatives");
        for(int i=0;i<headAlternatives.size();i++)ret[ret.length-1].getContent().add(headAlternatives.get(i).buildSDFHeadXML());

        return ret;
    }
}
