import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.VCARD;
import org.apache.log4j.PropertyConfigurator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {

        String log4jConfPath = System.getProperty("user.dir")+"\\log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);

        if(CreateRDFFile()){
            System.out.println("created RDF file.");
        }
        ReadRDFFile("file.rdf");

    }

    public static boolean CreateRDFFile() throws FileNotFoundException {

        // create an empty model
        Model model_result = ModelFactory.createDefaultModel();

        try {
            DriverManager.registerDriver(new com.mysql.jdbc.Driver());
            String url = "jdbc:mysql://127.0.0.1:3306/congress";
            Connection conn = DriverManager.getConnection(url,"root","");
            Statement stmt = conn.createStatement();
            ResultSet rs;

            rs = stmt.executeQuery("SELECT * FROM paper LIMIT 10");
            while ( rs.next() ) {

                String title = rs.getString("title");
                String _abstract = rs.getString("abstract");
                String paperURI    = "http://paper/"+title.replace(' ', '_').substring(0,15);

                // create an empty model
                Model model = ModelFactory.createDefaultModel();
                Property ABSTRACT = model.createProperty("http://www.w3.org/2001/vcard-rdf/3.0#", "ABSTRACT");

                Resource paper
                        = model.createResource(paperURI)
                        .addProperty(VCARD.TITLE, title)
                        .addProperty(ABSTRACT, _abstract);

                model_result.add(model);
            }
            conn.close();

            OutputStream out = new FileOutputStream("file.rdf");
            model_result.write(out, "RDF/XML-ABBREV");
            return true;

        } catch (Exception e) {
            System.err.println("Got an exception! ");
            System.err.println(e.getMessage());
            return false;
        }

    }

    public static void ReadRDFFile(String inputFileName){
        // create an empty model
        Model model = ModelFactory.createDefaultModel();

        // use the FileManager to find the input file
        InputStream in = FileManager.get().open( inputFileName );
        if (in == null) {
            throw new IllegalArgumentException(
                    "File: " + inputFileName + " not found");
        }

        // read the RDF/XML file
        model.read(in, null);

        // List all the resources with the property "vcard:FN"
        String queryString =
                "PREFIX vcard: <" + VCARD.getURI() + "> " +
                        "SELECT ?title "+
                        "WHERE { ?paper vcard:TITLE ?title.} ";
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model) ;
        org.apache.jena.query.ResultSet results = qexec.execSelect();

        while (results.hasNext())
        {
            System.out.println(results.nextSolution().toString());

        }

    }
}
