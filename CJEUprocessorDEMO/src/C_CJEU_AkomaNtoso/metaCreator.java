package C_CJEU_AkomaNtoso;

import org.jdom2.*;
import java.util.*;

public class metaCreator extends buildAkomaNtosoCJEUs
{    
    public static Element createMeta(ArrayList<String> allOrganizations)
    {
        Element meta = new Element("meta", xmlns);
        Element identification = new Element("identification", xmlns);
        meta.getContent().add(identification);
        identification.setAttribute("source", "#source");
        
        Element FRBRWork = new Element("FRBRWork", xmlns);
        Element FRBRthis = new Element("FRBRthis", xmlns);
        Element FRBRuri =  new Element("FRBRuri", xmlns);
        Element FRBRdate =  new Element("FRBRdate", xmlns);
        Element FRBRauthor =  new Element("FRBRauthor", xmlns);
        Element FRBRcountry =  new Element("FRBRcountry", xmlns);
        FRBRthis.setAttribute("value", "");
        FRBRuri.setAttribute("value", "");
        FRBRdate.setAttribute("date", "1111-11-11");
        FRBRdate.setAttribute("name", "");
        FRBRauthor.setAttribute("href", "");
        FRBRcountry.setAttribute("value", "");
        FRBRWork.getContent().add(FRBRthis);
        FRBRWork.getContent().add(FRBRuri);
        FRBRWork.getContent().add(FRBRdate);
        FRBRWork.getContent().add(FRBRauthor);
        FRBRWork.getContent().add(FRBRcountry);
        
        Element FRBRExpression = new Element("FRBRExpression", xmlns);
        FRBRthis = new Element("FRBRthis", xmlns);
        FRBRuri =  new Element("FRBRuri", xmlns);
        FRBRdate =  new Element("FRBRdate", xmlns);
        FRBRauthor =  new Element("FRBRauthor", xmlns);
        Element FRBRlanguage =  new Element("FRBRlanguage", xmlns);
        FRBRthis.setAttribute("value", "");
        FRBRuri.setAttribute("value", "");
        FRBRdate.setAttribute("date", "1111-11-11");
        FRBRdate.setAttribute("name", "");
        FRBRauthor.setAttribute("href", "");
        FRBRlanguage.setAttribute("language", "");
        FRBRExpression.getContent().add(FRBRthis);
        FRBRExpression.getContent().add(FRBRuri);
        FRBRExpression.getContent().add(FRBRdate);
        FRBRExpression.getContent().add(FRBRauthor);
        FRBRExpression.getContent().add(FRBRlanguage);
        
        Element FRBRManifestation = new Element("FRBRManifestation", xmlns);
        FRBRthis = new Element("FRBRthis", xmlns);
        FRBRuri =  new Element("FRBRuri", xmlns);
        FRBRdate =  new Element("FRBRdate", xmlns);
        FRBRauthor =  new Element("FRBRauthor", xmlns);
        Element FRBRformat =  new Element("FRBRformat", xmlns);
        FRBRthis.setAttribute("value", "");
        FRBRuri.setAttribute("value", "");
        FRBRdate.setAttribute("date", "1111-11-11");
        FRBRdate.setAttribute("name", "");
        FRBRauthor.setAttribute("href", "");
        FRBRformat.setAttribute("value", "");
        FRBRManifestation.getContent().add(FRBRthis);
        FRBRManifestation.getContent().add(FRBRuri);
        FRBRManifestation.getContent().add(FRBRdate);
        FRBRManifestation.getContent().add(FRBRauthor);
        FRBRManifestation.getContent().add(FRBRformat);
        
        identification.getContent().add(FRBRWork);
        identification.getContent().add(FRBRExpression);
        identification.getContent().add(FRBRManifestation);
        
        if(allOrganizations.size()>0)
        {
            Element references = new Element("references", xmlns);
            meta.getContent().add(references);
            references.setAttribute("source", "#source");
            
            for(int i=0;i<allOrganizations.size();i++)
            {
                String name = allOrganizations.get(i);
                Element TLCOrganization = new Element("TLCOrganization", xmlns);
                references.getContent().add(TLCOrganization);
                TLCOrganization.setAttribute("eId", name);
                TLCOrganization.setAttribute("showAs", "");
                if(name.indexOf("company")!=-1)TLCOrganization.setAttribute("href", "/akn/ontology/Organization/Company/"+name);
                else if(name.indexOf("institution")!=-1)TLCOrganization.setAttribute("href", "/akn/ontology/Organization/Institution/"+name);
            }
        }
        
        return meta;
    }
}
