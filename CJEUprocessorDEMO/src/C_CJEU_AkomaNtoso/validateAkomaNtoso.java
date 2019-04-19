package C_CJEU_AkomaNtoso;

import java.io.File;
import java.util.ArrayList;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class validateAkomaNtoso 
{
    private static File xsdFile = new File("./lib/XSDAkomaNtoso/akomantoso30.xsd");
    
    public static void validateAllFilesInFolder(File inputFolder)throws Exception
    {
        for(int i=0;i<inputFolder.listFiles().length;i++)
        {
            String filename=inputFolder.listFiles()[i].getName();
            if(inputFolder.listFiles()[i].isDirectory())validateAllFilesInFolder(inputFolder.listFiles()[i]);
            else if((filename.lastIndexOf(".xml")!=-1)&&(filename.lastIndexOf(".xml")==filename.length()-4))
            {
                ArrayList<String> messages = validateASingleFile(inputFolder.listFiles()[i]);

                if(messages.isEmpty()==false)
                {
                    System.out.println("The file: "+inputFolder.listFiles()[i]+" is not valid with respect to the AkomaNtoso XSD");
                    for(int j=0;j<messages.size();j++)System.out.println("\t"+messages.get(j));
                    System.exit(0);
                }
            }
        }
    }
    
    public static ArrayList<String> validateASingleFile(File f) throws Exception
    {
        try
        {
            MyErrorHandler eh = new MyErrorHandler();

            SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
            sf.setErrorHandler(eh);
            Schema s = sf.newSchema(xsdFile);
            
            Validator v = s.newValidator();
            v.setErrorHandler(eh);
            v.validate(new StreamSource(f));
            
            ArrayList<String> ret = new ArrayList<String>();
            for(int i=0;i<eh.errors.size();i++)ret.add(eh.errors.get(i));
            return ret;
        }
        catch(Exception e){throw e;}
    }
    
    protected static class MyErrorHandler implements org.xml.sax.ErrorHandler
    {
        public ArrayList<String> errors = new ArrayList<String>();

        public MyErrorHandler(){reset();}
        public void reset(){errors.clear();}

        public void warning(SAXParseException exception) throws SAXException{}
        public void error(SAXParseException exception) throws SAXException{errors.add(exception.getMessage());}
        public void fatalError(SAXParseException exception) throws SAXException{throw exception;}
    }
}
