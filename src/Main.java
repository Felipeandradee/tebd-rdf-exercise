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
import java.sql.*;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {

        String log4jConfPath = System.getProperty("user.dir")+"\\log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);

        if(CreateRDFFile()){
            System.out.println("created RDF file.");
        }

        ReadRDFFile("file.rdf");

    }

    public static java.sql.ResultSet SearchMySQL() throws SQLException {
        DriverManager.registerDriver(new com.mysql.jdbc.Driver());
        String url = "jdbc:mysql://127.0.0.1:3306/congress";
        Connection conn = DriverManager.getConnection(url,"root","");
        Statement stmt = conn.createStatement();

        return stmt.executeQuery(
                "SELECT distinct * " +
                        "FROM paper as p " +
                        "JOIN autor as a on a.idPaper = p.paperId " +
                        "JOIN participant as par on par.registrationId = a.idParticipant " +
                        "LIMIT 25;"
        );
    }

    public static boolean CreateRDFFile() throws FileNotFoundException {

        // create an empty model for result
        Model model_result = ModelFactory.createDefaultModel();

        try {

            // Getting information in MySQL for create RDF Models
            java.sql.ResultSet rs = SearchMySQL();

            while ( rs.next() ) {

                //Paper
                String title = rs.getString("title");
                String _abstract = rs.getString("abstract");
                String paperURI    = "http://paper/"+title.replace(' ', '_');

                //Autor
                String name = rs.getString("name");
                String email = rs.getString("mail");
                String autorURI    = "http://autor/"+name.replace(' ', '_');

                // create an empty model for each
                Model model = ModelFactory.createDefaultModel();
                Property ABSTRACT = model.createProperty("http://www.w3.org/2001/vcard-rdf/3.0#", "ABSTRACT");
                Property AUTOR = model.createProperty("http://www.w3.org/2001/vcard-rdf/3.0#","AUTOR");

                Resource paper
                        = model.createResource(paperURI)
                        .addProperty(VCARD.TITLE, title)
                        .addProperty(ABSTRACT, _abstract)
                        .addProperty(AUTOR, model.createResource(autorURI)
                                .addProperty(VCARD.NAME, name)
                                .addProperty(VCARD.EMAIL, email));

                model_result.add(model);
            }

            // Saving generated models in a RDF file
            OutputStream out = new FileOutputStream("file.rdf");
            model_result.write(out, "RDF/XML-ABBREV");
            return true;

        } catch (Exception e) {
            System.err.println("Got an exception! ");
            System.err.println(e.getMessage());
            return false;
        }

    }

    public static org.apache.jena.query.ResultSet SelectAutor(Model model){

        String queryString =
                "PREFIX p: <" + VCARD.getURI() + "> " +
                        "SELECT * "+
                        "WHERE {?autor p:AUTOR ?name} ";

        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model) ;
        return qexec.execSelect();
    }

    public static org.apache.jena.query.ResultSet SelectDistinctAutorName(Model model){

        String queryString =
                "PREFIX p: <" + VCARD.getURI() + "> " +
                        "SELECT DISTINCT ?name "+
                        "WHERE {?autor p:AUTOR ?name} ";

        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model) ;
        return qexec.execSelect();
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

        System.out.println("\n\nSelect example in Autor");
        org.apache.jena.query.ResultSet results = SelectAutor(model);
        while (results.hasNext()){
            System.out.println(results.nextSolution().toString());
        }

        System.out.println("\n\nDistinct example in Autor");
        results = SelectDistinctAutorName(model);
        while (results.hasNext()){
            System.out.println(results.nextSolution().toString());
        }

    }
}
