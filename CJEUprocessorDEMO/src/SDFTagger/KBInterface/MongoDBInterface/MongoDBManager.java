package SDFTagger.KBInterface.MongoDBInterface;

import SDFTagger.SDFItems.*;
import SDFTagger.KBInterface.XMLFilesInterface.*;
import com.mongodb.*;
import java.util.*;
import java.io.File;

public class MongoDBManager extends XMLFilesManager
{
    private DB mongoDBdatabase = null;
    public MongoDBManager
    (
        File rootDirectoryBags, String[] localPathsBags,
        File rootDirectoryXmlSDFRules, File rootDirectoryCompiledSDFRules, String[] localPathsSDFRules,
        String SDFTaggerKBname, boolean reloadKB
    )throws Exception
    {
        super(rootDirectoryBags, localPathsBags, rootDirectoryXmlSDFRules, rootDirectoryCompiledSDFRules, localPathsSDFRules);
        Mongo mdb = new Mongo("localhost", 27017);
        mongoDBdatabase = mdb.getDB(SDFTaggerKBname);
        if(reloadKB==true)
        {
            DBCollection allFormsToBags = mongoDBdatabase.getCollection("allFormsToBags");
            DBCollection allLemmasToBags = mongoDBdatabase.getCollection("allLemmasToBags");
            DBCollection rulesForm = mongoDBdatabase.getCollection("rulesForm");
            DBCollection rulesLemma = mongoDBdatabase.getCollection("rulesLemma");
            DBCollection rulesPOS = mongoDBdatabase.getCollection("rulesPOS");
            DBCollection rulesNotAssociatedWithFormsLemmasAndPOSs = mongoDBdatabase.getCollection("rulesNotAssociatedWithFormsLemmasAndPOSs");
            allFormsToBags.drop();
            allLemmasToBags.drop();
            rulesForm.drop();
            rulesLemma.drop();
            rulesPOS.drop();
            rulesNotAssociatedWithFormsLemmasAndPOSs.drop();
            allFormsToBags.createIndex(new BasicDBObject("Form", 1));
            allLemmasToBags.createIndex(new BasicDBObject("Lemma", 1));
            rulesForm.createIndex(new BasicDBObject("Form", 1));
            rulesLemma.createIndex(new BasicDBObject("Lemma", 1));
            rulesPOS.createIndex(new BasicDBObject("POS", 1));
            System.out.println("\tUploading Bag(s) indexed on Form(s)");
            Enumeration en = super.allFormsToBags.keys();
            while(en.hasMoreElements())
            {
                String key = (String)en.nextElement();
                ArrayList<String> bags = super.allFormsToBags.get(key);
                for(int i=0;i<bags.size();i++)
                {
                    BasicDBObject obj = new BasicDBObject();
                    obj.put("Form", key);
                    obj.put("bag", bags.get(i));
                    allFormsToBags.insert(obj);
                }
            }

            System.out.println("\tUploading Bag(s) indexed on Lemma(s)");
            en = super.allLemmasToBags.keys();
            while(en.hasMoreElements())
            {
                String key = (String)en.nextElement();
                ArrayList<String> bags = super.allLemmasToBags.get(key);
                for(int i=0;i<bags.size();i++)
                {
                    BasicDBObject obj = new BasicDBObject();
                    obj.put("Lemma", key);
                    obj.put("bag", bags.get(i));
                    allFormsToBags.insert(obj);
                }
            }

            System.out.println("\tUploading SDFRule(s) indexed on Form(s)");
            for(int i=0;i<super.rulesFormIndex.size();i++)
            {
                String key = super.rulesFormIndex.get(i);
                ArrayList<String> rules = super.rulesForm.get(i);
                for(int j=0; j<rules.size(); j++)
                {
                    BasicDBObject obj = new BasicDBObject();
                    obj.put("Form", key);
                    obj.put("SDFCode", rules.get(j));
                    rulesForm.insert(obj);
                }
            }

            System.out.println("\tUploading SDFRule(s) indexed on Lemma(s)");
            for(int i=0;i<super.rulesLemmaIndex.size();i++)
            {
                String key = super.rulesLemmaIndex.get(i);
                ArrayList<String> rules = super.rulesLemma.get(i);
                for(int j=0; j<rules.size(); j++)
                {
                    BasicDBObject obj = new BasicDBObject();
                    obj.put("Lemma", key);
                    obj.put("SDFCode", rules.get(j));
                    rulesLemma.insert(obj);
                }
            }

            System.out.println("\tUploading SDFRule(s) indexed on POS(s)");
            for(int i=0;i<super.rulesPOSIndex.size();i++)
            {
                String key = super.rulesPOSIndex.get(i);
                ArrayList<String> rules = super.rulesPOS.get(i);
                for(int j=0; j<rules.size(); j++)
                {
                    BasicDBObject obj = new BasicDBObject();
                    obj.put("POS", key);
                    obj.put("SDFCode", rules.get(j));
                    rulesPOS.insert(obj);
                }
            }

            System.out.println("\tUploading SDFRule(s) not indexed neither on Form(s) nor on Lemma(s) nor on POS(s)");
            for(int i=0;i<super.rulesNotAssociatedWithFormsLemmasAndPOSs.size();i++)
            {
                BasicDBObject obj = new BasicDBObject();
                obj.put("SDFCode", super.rulesNotAssociatedWithFormsLemmasAndPOSs.get(i));
                rulesNotAssociatedWithFormsLemmasAndPOSs.insert(obj);
            }
        }
        super.allFormsToBags = null;
        super.allLemmasToBags = null;
        super.rulesFormIndex = null;
        super.rulesForm = null;
        super.rulesLemmaIndex = null;
        super.rulesLemma = null;
        super.rulesPOSIndex = null;
        super.rulesPOS = null;
        super.rulesNotAssociatedWithFormsLemmasAndPOSs = null;
        Runtime.getRuntime().gc();
    }
    
    public void fillBagsOfSDFNode(String Form, String Lemma, ArrayList<String> bagsOnForm, ArrayList<String> bagsOnLemma)throws Exception
    {
        DBCollection allFormsToBags = mongoDBdatabase.getCollection("allFormsToBags");
        DBCollection allLemmasToBags = mongoDBdatabase.getCollection("allLemmasToBags");
        
        BasicDBObject query = new BasicDBObject();
        query.put("Form", Form);
        DBCursor cur = allFormsToBags.find(query);
        if(cur.hasNext()==true)
        {
            DBObject dbobject = cur.next();
            bagsOnForm.add((String)dbobject.get("bag"));
        }
        cur.close();
        
        query = new BasicDBObject();
        query.put("Lemma", Lemma);
        cur = allLemmasToBags.find(query);
        if(cur.hasNext()==true)
        {
            DBObject dbobject = cur.next();
            bagsOnLemma.add((String)dbobject.get("bag"));
        }
        cur.close();
    }
   
    public ArrayList<String> retrieveSDFCodes(SDFHead SDFHead)throws Exception
    {
        String Form = SDFHead.getForm().toLowerCase();
        String Lemma = SDFHead.getLemma().toLowerCase();
        String POS = SDFHead.getPOS().toLowerCase();
        
        ArrayList<String> ret = new ArrayList<String>();
        
        DBCollection rulesForm = mongoDBdatabase.getCollection("rulesForm");
        DBCollection rulesLemma = mongoDBdatabase.getCollection("rulesLemma");
        DBCollection rulesPOS = mongoDBdatabase.getCollection("rulesPOS");
        DBCollection rulesNotAssociatedWithFormsLemmasAndPOSs = mongoDBdatabase.getCollection("rulesNotAssociatedWithFormsLemmasAndPOSs");
        
        BasicDBObject query = new BasicDBObject();
        query.put("Form", Form);
        DBCursor cur = rulesForm.find(query);
        while(cur.hasNext()==true)
        {
            DBObject dbobject = cur.next();
            ret.add((String)dbobject.get("SDFCode"));
        }
        cur.close();
        
        query = new BasicDBObject();
        query.put("Lemma", Lemma);
        cur = rulesLemma.find(query);
        while(cur.hasNext()==true)
        {
            DBObject dbobject = cur.next();
            ret.add((String)dbobject.get("SDFCode"));
        }
        cur.close();
        
        query = new BasicDBObject();
        query.put("POS", POS);
        cur = rulesPOS.find(query);
        while(cur.hasNext()==true)
        {
            DBObject dbobject = cur.next();
            ret.add((String)dbobject.get("SDFCode"));
        }
        cur.close();
        
        cur = rulesNotAssociatedWithFormsLemmasAndPOSs.find();
        while(cur.hasNext()==true)
        {
            DBObject dbobject = cur.next();
            ret.add((String)dbobject.get("SDFCode"));
        }
        cur.close();
        
        return ret;
    }
}