package OntologyManager;

import java.io.FileWriter;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.iterator.ExtendedIterator;

public class OntologyManager 
{
    private static OntModel OntModel = null;
    private static OntClass Person = null;
    private static OntClass Company = null;
    private static OntClass Institution = null;
    private static long maxIndexPerson = 1;
    private static long maxIndexCompany = 1;
    private static long maxIndexInstitution = 1;
    
        //If the boolean deleteAllIndividuals is true, after we have loaded the ontology, we delete from it all individuals. 
    public static void loadOntology(String ontologyFilePath, boolean deleteAllIndividuals) throws Exception
    {
        OntModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        OntModel.read(ontologyFilePath);
        
        ExtendedIterator ei = OntModel.listClasses();
        while(ei.hasNext())
        {
            OntClass temp = (OntClass)ei.next();
            if(temp.getLocalName().compareToIgnoreCase("Person")==0)Person=temp;
            else if(temp.getLocalName().compareToIgnoreCase("Company")==0)Company=temp;
            else if(temp.getLocalName().compareToIgnoreCase("Institution")==0)Institution=temp;
        }
        
        if(Person==null)throw new Exception("Ill-formed reference ontology: the class Person does not exist.");
        if(Company==null)throw new Exception("Ill-formed reference ontology: the class Company does not exist.");
        if(Institution==null)throw new Exception("Ill-formed reference ontology: the class Institution does not exist.");
        
        ExtendedIterator iteratorPerson = Person.listInstances();
        while(iteratorPerson.hasNext())
        {
            Individual ind = (Individual)iteratorPerson.next();
            String name = ind.getLocalName();
            if(name.indexOf("person_")!=0)
                throw new Exception("Ill-formed reference ontology: the class Person includes the individual \""+name+"\"; "
                    + "the names of individuals in this class must be in the form \"person_X\", where X is a positive integer.");
            name = name.substring("person_".length(), name.length()).trim();
            int index = -1;
            try{index=Integer.parseInt(name);}catch(Exception e)
            {
                throw new Exception("Ill-formed reference ontology: the class Person includes the individual \""+name+"\"; "
                    + "the names of individuals in this class must be in the form \"person_X\", where X is a positive integer.");
            }
            
            if(index>maxIndexPerson)maxIndexPerson=index;
        }
        
        ExtendedIterator iteratorCompany = Company.listInstances();
        while(iteratorCompany.hasNext())
        {
            Individual ind = (Individual)iteratorCompany.next();
            String name = ind.getLocalName();
            if(name.indexOf("company_")!=0)
                throw new Exception("Ill-formed reference ontology: the class Company includes the individual \""+name+"\"; "
                    + "the names of individuals in this class must be in the form \"company_X\", where X is a positive integer.");
            name = name.substring("company_".length(), name.length()).trim();
            int index = -1;
            try{index=Integer.parseInt(name);}catch(Exception e)
            {
                throw new Exception("Ill-formed reference ontology: the class Company includes the individual \""+name+"\"; "
                    + "the names of individuals in this class must be in the form \"company_X\", where X is a positive integer.");
            }
            
            if(index>maxIndexCompany)maxIndexCompany=index;
        }
        
        ExtendedIterator iteratorInstitution = Institution.listInstances();
        while(iteratorInstitution.hasNext())
        {
            Individual ind = (Individual)iteratorInstitution.next();
            String name = ind.getLocalName();
            if(name.indexOf("institution_")!=0)
                throw new Exception("Ill-formed reference ontology: the class Institution includes the individual \""+name+"\"; "
                    + "the names of individuals in this class must be in the form \"institution_X\", where X is a positive integer.");
            name = name.substring("institution_".length(), name.length()).trim();
            int index = -1;
            try{index=Integer.parseInt(name);}catch(Exception e)
            {
                throw new Exception("Ill-formed reference ontology: the class Institution includes the individual \""+name+"\"; "
                    + "the names of individuals in this class must be in the form \"institution_X\", where X is a positive integer.");
            }
            
            if(index>maxIndexInstitution)maxIndexInstitution=index;
        }
        
        if(deleteAllIndividuals==true)
        {
            ExtendedIterator individualsItereator = OntModel.listIndividuals();
            while(individualsItereator.hasNext())
            {
                Individual ind = (Individual)individualsItereator.next();
                
                ExtendedIterator classesItereator = ind.listOntClasses(true);
                while(classesItereator.hasNext())
                {
                    OntClass OntClass = (OntClass)classesItereator.next();
                    ind.removeOntClass(OntClass);
                }
            }
            
            maxIndexPerson = 1;
            maxIndexCompany = 1;
            maxIndexInstitution = 1;
            saveOntology(ontologyFilePath);
        }
    }
    
    public static void saveOntology(String ontologyFilePath)throws Exception
    {
        FileWriter fw = new FileWriter(ontologyFilePath);
        OntModel.write( fw, "RDF/XML-ABBREV" );
        fw.close();
    }
    
    public static String createCompany()
    {
        Individual ind = Company.createIndividual(Company.getNameSpace()+"company_"+maxIndexCompany);
        maxIndexCompany++;
        return ind.getLocalName();
    }
    
    public static String createInstitution()
    {
        Individual ind = Institution.createIndividual(Institution.getNameSpace()+"institution_"+maxIndexInstitution);
        maxIndexInstitution++;
        return ind.getLocalName();
    }
}
