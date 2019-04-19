package A_CJEU_Crawler;

import java.io.*;
import java.net.*;
import org.jdom2.*;
import org.jdom2.input.*;
import org.jdom2.output.*;

//We crawl and download all case law having an ECLI identifiers listed in the files within the subfolder "LISTS OF CJEU CASE LAW". 

//The files in this subfolders have been built with the Linked data query wizard of CELLAR (https://data.europa.eu/euodp/data/dataset/sparql-cellar-of-the-publications-office/resource/22428a2a-e230-47f3-829e-1fc57532a2b7)
//We extract the ECLI and we put it at the end of the URL: "https://eur-lex.europa.eu/legal-content/EN/TXT/HTML/?uri=".
//For instance:
//https://eur-lex.europa.eu/legal-content/EN/TXT/HTML/?uri=ecli:ECLI:EU:C:2017:1000

public class CJEUcrawler 
{
    public static void main(String[] args)
    {
        try 
        {
//NOTE!!!!!! WE CRAWL ONLY THE CASE LAW OF THE CJEU THAT HAS THE ECLI AND ONLY THOSE WHO DO NOT RISE AN EXCEPTION WHEN WE TRY TO OPEN THEM WITH JDOM (SOME OF 
//THEM CONTAIN ERRORS, E.G. THEY OPEN A TAG AND THEY DO NOT CLOSE IT -> WE SKIP ALL THESE PROBLEMATIC CASE LAW).
            
                /*
                The list here is created via the Wizard of the CELLAR system. More info at the bottom of the file.
                We crawl only the documents that have an ECLI associated; of course, we should download *ALL* docs, but here
                we are just building a sample CORPUS, so ...
            
                    <row>
                        <col>Judgment of the Court (Eighth Chamber) of 28 February 2018.#MA.T.I. SUD SpA v Centostazioni SpA and Duemme SGR SpA v Associazione Cassa Nazionale di Previdenza e Assistenza in favore dei Ragionieri e Periti Commerciali (CNPR).#Requests for a preliminary ruling from the Tribunale Amministrativo Regionale per il Lazio.#Reference for a preliminary ruling — Public procurement — Directive 2004/18/EC — Article 51 — Rectification of procedural shortfalls in tenders — Directive 2004/17/EC — Clarification of tenders — National legislation making the rectification by tenderers of the documentation submitted subject to the payment of a financial penalty — Principles relating to the award of public works contracts — Principle of equal treatment — Principle of proportionality.#Joined Cases C-523/16 and C-536/16.</col>
                        <col />
                        <col>2018-02-28</col>
                        <col>celex:62016CJ0523,ecli:ECLI:EU:C:2018:122</col>
                        <col>ENG</col>
                        <col>html</col>
                    </row>
                /**/
            
            
                //The CELLAR system does allow to save all results of a query within a single XML: the Download button at the end of the Wizard only saves the results
                //of a page. Therefore, we set the results shown per page to 500, and we download the files one by one.
            
                //We can set the results displayed per page in the Wizard menu "Customize results" on the slot "Number of lines"
                     
                //Yes, I've tried to put 10000000 in "Number of lines", or even 3000 ... but it doesn't work, when I try to download, it blocks.
                //With some patience, we download the page results 500 by 500 ...
            File[] inputList = 
                {
                    new File("./CORPUS/0 - INPUT/LISTS OF CJEU CASE LAW/CaseLawCJEUsince1.1.2017-PAGE1.xml"),
                    new File("./CORPUS/0 - INPUT/LISTS OF CJEU CASE LAW/CaseLawCJEUsince1.1.2017-PAGE2.xml"),
                    new File("./CORPUS/0 - INPUT/LISTS OF CJEU CASE LAW/CaseLawCJEUsince1.1.2017-PAGE3.xml"),
                    new File("./CORPUS/0 - INPUT/LISTS OF CJEU CASE LAW/CaseLawCJEUsince1.1.2017-PAGE4.xml"),
                    new File("./CORPUS/0 - INPUT/LISTS OF CJEU CASE LAW/CaseLawCJEUsince1.1.2017-PAGE5.xml"),
                    new File("./CORPUS/0 - INPUT/LISTS OF CJEU CASE LAW/CaseLawCJEUsince1.1.2017-PAGE6.xml")
                };
                
            File outputFolder = new File("./CORPUS/0 - INPUT/");
            
                //We remove all files from the output directory
            boolean allDir=false;
            while(allDir==false)
            {
                allDir=true;
                for(int i=0;i<outputFolder.listFiles().length;i++)
                {
                    if(outputFolder.listFiles()[i].isDirectory()==false)
                    {
                        outputFolder.listFiles()[i].delete();
                        allDir=false;
                    }
                }
            }

                //We crawl the case law whose ECLIs is in the files of the input list
            for(int l=0;l<inputList.length;l++)
            {
                Document doc = (Document) new SAXBuilder().build(inputList[l]);
                Element root = doc.getRootElement();
                for(int i=0;i<root.getContent().size();i++)
                {
                    if(!(root.getContent().get(i) instanceof Element))continue;
                    Element row = (Element)root.getContent().get(i);
                    for(int j=0;j<row.getContent().size();j++)
                    {
                        if(!(row.getContent().get(j) instanceof Element))continue;
                        Element col = (Element)row.getContent().get(j);
                        for(int k=0;k<col.getContent().size();k++)
                        {
                            if(!(col.getContent().get(k) instanceof Text))continue;
                            Text Text = (Text)col.getContent().get(k);
                            if(Text.getText().indexOf("ECLI:EU:C")==-1)continue;

                            String ECLI = Text.getText().substring(Text.getText().indexOf("ECLI:EU:C"), Text.getText().length()).trim();
                            if(ECLI.indexOf(",")!=-1)ECLI=ECLI.substring(0, ECLI.indexOf(","));

                            System.out.println("Crawling CJEU case law "+ECLI);
                            
                            crawlCaseLaw(ECLI, outputFolder);
                        }
                    }
                }
            }
        } 
        catch(Exception e) 
        {
            e.printStackTrace();
        }
    }
    
    private static void crawlCaseLaw(String ECLI, File outputFolder)throws Exception
    {
        Document doc = null;
        try
        {
            URL url = new URL("https://eur-lex.europa.eu/legal-content/EN/TXT/HTML/?uri=ecli:"+ECLI);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if(conn.getResponseCode()!=200)throw new RuntimeException("Failed: HTTP error code: "+ conn.getResponseCode());
            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream()), "UTF8"));
            String crawledText="";
            String buffer;
            while((buffer=br.readLine())!=null)crawledText=crawledText+buffer;

                //To make is simple, we only take the <body>
            crawledText = crawledText.substring(crawledText.indexOf("<body"), crawledText.indexOf("</body>")+"</body>".length());
            crawledText = crawledText.replaceAll("&", "&amp;");
            doc = new SAXBuilder().build(new StringReader(crawledText));

            ECLI = ECLI.replaceAll(":", "_");
        }
        catch(Exception e){return;}//if we get any exception, we simply skip this case law
        
        XMLOutputter outputter = new XMLOutputter();
        outputter.setFormat(Format.getPrettyFormat().setEncoding("UTF-8"));
        FileOutputStream fos = new FileOutputStream(outputFolder.getAbsolutePath()+"/"+ECLI+".xml");
        OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
        outputter.output(doc, osw);
        osw.close();
        fos.close();
    }
}

/*
USING THE CELLAR SYSTEM:

Per avere tutti i case law della Court of Justice dal 1 gennaio 2018, si guarda qui:

https://data.europa.eu/euodp/data/dataset/sparql-cellar-of-the-publications-office

Si può usare il Wizard, che è molto comodo (ma non riesco ad usare i type ... poco male, l'annotazione dei documenti è comunque incoerente e approssimativa in molti casi).
I problemi di incoerente/approssimata annotazione si superano mettendo nel titolo:

Title like "Judgment of the Court"

Poi:

Autore: Court of Justice
Data: dopo il 1 gennaio 2017, ad esempio
Language: English

Invece che usare il wizard, si può usare lo sparql endpoint all'url:

https://publications.europa.eu/en/advanced-sparql-query-editor

La query di sopra tradotta in sparql è riportata qui sotto; eseguendola, si ottengono tutti i docs prodotti dalla corte di giustizia dopo il 1 gennaio 2018 che hanno la 
string "Judgment of the Court" nel titolo. 

Con il wizard, i documenti si possono scaricare in XML. Vengono solo visualizzati con lo sparql-query-editor. Quindi: meglio usare il query wizard, che si può fare il 
download in XML. La query è visualizzata nel file wizardCELLAR.jpg. 

QUERY SPARQL:

PREFIX cdm:<http://publications.europa.eu/ontology/cdm#>
PREFIX skos:<http://www.w3.org/2004/02/skos/core#>
PREFIX dc:<http://purl.org/dc/elements/1.1/>
PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>
PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX owl:<http://www.w3.org/2002/07/owl#>
SELECT 
DISTINCT (group_concat(distinct ?work;separator=",") as ?cellarURIs)
(group_concat(distinct ?title_;separator=",") as ?title)
?langIdentifier


(group_concat(distinct ?resType;separator=",") as ?workTypes) 
(group_concat(distinct ?agentName;separator=",") as ?authors)
?date

(group_concat(distinct ?workId_;separator=",") as ?workIds)
WHERE 
{
graph ?g {
    ?work rdf:type ?resType .
    
    ?work cdm:work_date_document ?date .
    FILTER( ?date > "2018-01-01"^^xsd:date)
    ?work cdm:work_id_document ?workId_.

    
    
    OPTIONAL {?work cdm:work_created_by_agent ?agent. graph ?ga { ?agent skos:prefLabel ?agentName  
    filter (lang(?agentName)="en") FILTER(  str( ?agentName)="CJ").}}.
}
graph ?ge { 
    ?exp cdm:expression_belongs_to_work ?work .
     ?exp cdm:expression_title ?title_ 
filter(lang(?title_)="en" or lang(?title_)="eng" or lang(?title_)='' ).

     ?exp cdm:expression_uses_language ?lg. 
graph ?lgc { ?lg dc:identifier ?langIdentifier .}
    FILTER(  str( ?langIdentifier)="ENG")
    FILTER( regex( str(?title_), "Judgment of the Court", "i"))
}


}
GROUP BY ?work  ?date ?langIdentifier
LIMIT 100
OFFSET 0



*/
