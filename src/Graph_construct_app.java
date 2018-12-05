/////////////////////////////////////////////////////////////////////////
//
// (c) Copyright University of Southampton IT Innovation, 2015
//
// Copyright in this software belongs to IT Innovation Centre of
// Gamma House, Enterprise Road, Southampton SO16 7NS, UK.
//
// This software may not be used, sold, licensed, transferred, copied
// or reproduced in whole or in part in any manner or form or in or
// on any media by any person other than in accordance with the terms
// of the Licence Agreement supplied with the software, or otherwise
// without the prior written consent of the copyright owners.
//
// This software is distributed WITHOUT ANY WARRANTY, without even the
// implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
// PURPOSE, except where stated in the Licence Agreement supplied with
// the software.
//
// Created By : Jessica Rosati
// Created Date : 2017/06/23
// Created for Project : GRAVITATE
//
/////////////////////////////////////////////////////////////////////////
//
// Dependencies : NONE
//
/////////////////////////////////////////////////////////////////////////
package ITinnov.semantic_matching;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.tdb.TDBFactory;
import org.apache.jena.atlas.json.JsonParseException;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
/*
 * This class queries Blazegraph according to the query written in the file specified by the key strPathToQueryConstruct in config.properties
 * The class defines a jena.rdf.model.Model with extracted triples and writes it into a file (see properties.getProperty("strPathOfOutputFileForConstructQuery")) with utf-8 encoding, TTL format
 * If there are predicates that have been clustered, the file with triples is updated.
 */

public class Graph_construct_app {
	static Model model;
	static Properties properties;
	static final int intTimePrintingException = 2000;

	/*
	Gets the parameters values specified in the config.properties file
	Input: a string representing the path of config.properties file with parameters 
	 */
	static private void getParamatersValues(String strPath) {
		ParameterValues gettingParameterValues = new ParameterValues();
		properties = gettingParameterValues.loadParamsFromFile(strPath);
	}

	/*
	Reads the file with the query to run against Blazegraph (construct query or query to select entities)
	Input: a string representing the path of the file with the query
	 */
	private static String read_query(String strPathToQuery) {
		String strQueryInput = "";
		try {
			FileInputStream fis = new FileInputStream(strPathToQuery);
			BufferedReader br  = new BufferedReader( new InputStreamReader(fis, java.nio.charset.StandardCharsets.UTF_8) );
			String strCurrentLine;
			while ((strCurrentLine = br.readLine()) != null ) {
				strQueryInput += strCurrentLine + ' ';
			}
			br.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return strQueryInput;
	}

	/*
	Executes the construct query against Blazegraph to extract the initial knowledge
	Writes the extracted triple into a file ("strPathOfOutputFileForConstructQuery" in config.properties) with utf-8 encoding and TTL format
	If there are any predicates that have been clustered, the file with triples is updated (calling update_file method).
	 */
	public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException {
		getParamatersValues("config.properties");
		try {
			new URI(properties.getProperty("strServiceURI"));
		} catch (URISyntaxException e) {
			System.out.println("URISyntaxException for import query");
			e.printStackTrace();
		}

		java.sql.Timestamp timestamp = new java.sql.Timestamp(System.currentTimeMillis());
		System.out.println("Timestamp (construct start) " + timestamp.toString() );

		// the construct query
		Query construct_query = null;
		// the query for entities
		Query query_entities = null;
		try {
			// special case DBpedia simple which needs BlazeGraph WITH that JENA cannot process. Simple dont run this construct
			if( !properties.getProperty("strPathOfOutputFileForConstructQuery").equals( "dbpedia-simple.nt" ) ) {
				construct_query = QueryFactory.create(read_query(properties.getProperty("strPathToQueryConstruct")), Syntax.syntaxARQ);
			}
			if( !properties.getProperty("strPathToQueryGraphID").equals( "query_dbpedia_id_simple.txt" ) ) {
				query_entities = QueryFactory.create(read_query(properties.getProperty("strPathToQueryGraphID")), Syntax.syntaxARQ);
			}
		} catch (com.hp.hpl.jena.query.QueryParseException e) {
			System.out.println(properties.getProperty("strMessageQueryParseException"));
			throw (e);
		}
		QueryEngineHTTP httpQuery = null;
		if( construct_query != null ) {
			httpQuery = new QueryEngineHTTP(properties.getProperty("strServiceURI"), construct_query);
		}
		QueryEngineHTTP httpQuery_entities = null;
		if( query_entities != null ) {
			httpQuery_entities = new QueryEngineHTTP(properties.getProperty("strServiceURI"), query_entities);
		}
		Dataset dataset = TDBFactory.createDataset();
		model = dataset.getDefaultModel();
		dataset.begin(ReadWrite.WRITE);

		long startTime = System.currentTimeMillis();
		long pastTime = startTime;
		while (true) {
			try {
				// executes the query for entities
				if( query_entities != null ) {
					ResultSet resultSet = httpQuery_entities.execSelect();
					if ( resultSet == null ) {
						throw new RuntimeException( "SPARQL query failed (check " + properties.getProperty("strPathToQueryGraphID") + ")" );
					}
					write_entities(resultSet);
					httpQuery_entities.close();
				}

				// Executes the construct query.
				if( construct_query != null ) {
					model = httpQuery.execConstruct();
					if ( model == null ) {
						throw new RuntimeException( "SPARQL query failed (check " + properties.getProperty("strPathToQueryConstruct") + ")" );
					}
					httpQuery.close();

					// write the model (resulting from the construct query) into a file (utf-8 encoding, TTL format)
					try {
						model.write(new OutputStreamWriter(new FileOutputStream(properties.getProperty("strPathOfOutputFileForConstructQuery"), false), java.nio.charset.StandardCharsets.UTF_8), "N-TRIPLES") ;
					}
					catch (NullPointerException e) {
						System.out.println("Check strPathOfOutputFileForConstructQuery in config.properties file");
						e.printStackTrace();
						throw e;
					} catch (FileNotFoundException e) {
						System.out.println("file for writing output not found");
						e.printStackTrace();
						throw e;
					}
				}
				break;
			} catch (JsonParseException e0) {
				System.out.println("strMessageJsonParseException");
				if (System.currentTimeMillis() - startTime > Integer.valueOf(properties.getProperty("timeLimitforImportQuery"))) {
					if (!httpQuery.isClosed()) httpQuery.close(); // it guarantees connection will be closed anyway
					throw e0;
				}
			} catch (com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP e) {  // the URI for import query is not well written
				if (e.getMessage().contains("MalformedURLException")) {
					System.out.println("Check the URI for import query");
					throw (e);
				} else if (e.getCause() instanceof org.apache.http.conn.HttpHostConnectException) {  // connection refused; trying to connect again
					if ((System.currentTimeMillis() - pastTime) / intTimePrintingException == 1) {
						pastTime = System.currentTimeMillis();
						System.out.println(properties.getProperty("strMessageHttpHostConnectExceptionException")+". Check connectivity to " + properties.getProperty("strServiceURI").replace("sparql", ""));
					}
					if (System.currentTimeMillis() - startTime > Integer.valueOf(properties.getProperty("timeLimitforImportQuery"))) {
						if (!httpQuery.isClosed()) httpQuery.close(); // it guarantees connection will be closed anyway
						throw e;
					}
				}
			} catch (RuntimeException eRuntime) {
				System.out.println("runtime exception in import query");
				throw eRuntime;
			}
		}
		dataset.close();
		try {
			if(properties.containsKey("listPredicatesForPreprocessingDate") || properties.containsKey("listPredicatesForPreprocessingUnit")) {
				// if any predicate to process/cluster is specified in the config.properties file
				HashMap<String, String> predicate_label = read_CORRESPONDENCE_PREDICATE_LABEL("CORRESPONDENCE_PREDICATE_LABEL");
				update_file( properties.getProperty("strPathOfOutputFileForConstructQuery"), predicate_label, "preprocess_literal/file_with_literal_cluster_entries.txt" );
			}
			else
			{
				// do nothing but make the updated file anyway as we will process it later
				System.out.println("Literals (dimensions and dates) treated as strings and just exact matching will be verified.\nUse numeric_range_app.java to cluster literals.");
				HashMap<String, String> hm_predicate_label = new HashMap<String, String>();
				update_file( properties.getProperty("strPathOfOutputFileForConstructQuery"), hm_predicate_label, "" );
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		timestamp = new java.sql.Timestamp(System.currentTimeMillis());
		System.out.println("Timestamp (construct end) " + timestamp.toString() );

		/*
		// DISABLED AS NOT NEEDED AND STOPS DBPEDIA WORKING (its BM specific)
		// STUART CODE
		// function to load RDF output and replace BM namespace with a much shorter version. this is to reduce the BM URI length and thus RAM needed to store it.
		// this makes in-memory SED work use much less memory later (which was exceeding 64Gbytes RAM for graphs with 9000 artifacts)
		try {
			String[] arraySourceURI ={ "http://collection.britishmuseum.org/id/object/", "http://collection.britishmuseum.org/id/thesauri/", "http://collection.britishmuseum.org/id/person-institution/", "http://collection.britishmuseum.org/id/place/", "http://www.cidoc-crm.org/cidoc-crm/", "http://qudt.org/vocab/" };
			String[] arrayReplaceURI ={ "http://o/", "http://t/", "http://pe/", "http://pl/", "http://c/", "http://u/" };
			// replace URI from artifact graph query construct
			rdf_replacement(
					properties.getProperty("strPathOfOutputFileForConstructQueryUPDATED"),
					properties.getProperty("strPathOfOutputFileForConstructQuerySHORTENED"),
					arraySourceURI,
					arrayReplaceURI );
			// replace URI from artifact ID query (so they match)
			rdf_replacement(
					properties.getProperty("strPathArtefactsURIs"),
					properties.getProperty("strPathArtefactsURIsSHORTENED"),
					arraySourceURI,
					arrayReplaceURI );
		} catch (IOException e) {
			e.printStackTrace();
		}
		// END STUART CODE
		*/
	}

	/*
	Builds an hashmap with (KEY) the URI of a processed predicate and (VALUE) the label.
	Input: a string representing the path of the file to write the hashmap in.
 	*/
	private static HashMap<String, String> read_CORRESPONDENCE_PREDICATE_LABEL(String file_CORRESPONDENCE_PREDICATE_LABEL)throws IOException {
		HashMap<String, String> hm_predicate_label = new HashMap<String, String>();
		FileInputStream fis = new FileInputStream(file_CORRESPONDENCE_PREDICATE_LABEL);
		BufferedReader br  = new BufferedReader(new InputStreamReader(fis, java.nio.charset.StandardCharsets.UTF_8));
		String line = br.readLine();
		while(line != null){
			String[] line_splitted = line.split("\t");
			hm_predicate_label.put(line_splitted[1], line_splitted[0]);
			line = br.readLine();
		}
		return hm_predicate_label;
	}

	/*
        Updates the triples file (specified by "strPathOfOutputFileForConstructQuery" in config.properties) with clustered values for literal.
        It removes a literal if it founds a correspondence with a cluster value.
        It writes a new file with updated triples (the file name is specified by "strPathOfOutputFileForConstructQueryUPDATED" in config.properties).
        Input: the path of original file with RDF triples,
        the list (tab separated string) of processed predicates,
        the hashmap with predicates and relative labels,
        the path of the file with clusters.
    */
	private static void update_file(String file_with_RDFtriples, HashMap<String, String> hm_predicate_label, String file_clusters) throws IOException {
		OutputStreamWriter output = new OutputStreamWriter(new FileOutputStream(properties.getProperty("strPathOfOutputFileForConstructQueryUPDATED")), java.nio.charset.StandardCharsets.UTF_8);
		HashMap<String, HashMap<String, String>> clusters = new HashMap<String, HashMap<String, String>>();
		FileInputStream fis_clusters = null, fis_RDFtriples = null ;
		BufferedReader br_clusters = null, br_RDFtriples = null ;

		if( file_clusters != "" )
		{
			try {
				// filling the HashMap clusters: its keys are the labels associated to processed predicates
				// its values are HashMap<String, String>, with the value clustered and the correpsonding cluster
				fis_clusters = new FileInputStream(file_clusters);
				br_clusters  = new BufferedReader(new InputStreamReader(fis_clusters, java.nio.charset.StandardCharsets.UTF_8));
				String line = br_clusters.readLine();
				while(line != null){
					String[] splitted = line.split("\t");
					String key_hm = splitted[1].substring(0,splitted[1].indexOf("_"));
					if(!clusters.containsKey(key_hm)){
						clusters.put(key_hm, new HashMap<String, String>());
						clusters.get(key_hm).put(splitted[0], splitted[1]);
					}
					else clusters.get(key_hm).put(splitted[0], splitted[1]);
					line = br_clusters.readLine();
				}
			}catch (FileNotFoundException e) {
				System.out.println("File with clusters not found.");
			}
		}

		String[] RDFtriple_splitted;

		try {
			fis_RDFtriples = new FileInputStream(new File(file_with_RDFtriples));
			br_RDFtriples  = new BufferedReader(new InputStreamReader(fis_RDFtriples, java.nio.charset.StandardCharsets.UTF_8));
			String RDFtriple = br_RDFtriples.readLine();
			while(RDFtriple != null){
				// iterating over RDF triples
				RDFtriple_splitted = RDFtriple.split("> ");
				if(hm_predicate_label.containsKey(RDFtriple_splitted[1]+">")) {
					// only if the triple predicate is among processed predicates
					String label = hm_predicate_label.get(RDFtriple_splitted[1]+">");
					// note: predicate might have no values in which case it will not appear in cluster
					if( clusters.containsKey( label ) ) {
						Iterator<String> it_on_predicate_cluster = clusters.get(label).keySet().iterator();
						while (it_on_predicate_cluster.hasNext()) {
							// iterating over the cluster of the current predicate
							String literal_clustered = it_on_predicate_cluster.next();
							// in the triple, the literal object is replaced with the belonging cluster (if any)
							RDFtriple = RDFtriple.replace("\"" + literal_clustered + "\"", "\"" + clusters.get(label).get(literal_clustered) + "\"");
						}
					}
				}
				output.write(RDFtriple+"\n");
				RDFtriple = br_RDFtriples.readLine();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		if (br_clusters != null)
		{
			br_clusters.close();
			fis_clusters.close();
		}
		br_RDFtriples.close();
		fis_RDFtriples.close();
		output.close();
	}

	/*
        Writes the entities of interest (ex. artefacts), that is the set of resources returned by query query_entities of the main method.
        The parameter strPathArtefactsURIs in config.properties file is the name of the file to write.
        Input: the ResultSet of query_entities
    */
	private static void write_entities(ResultSet resultSet) {
		OutputStreamWriter output_artefacts;
		QuerySolution obj;
		Resource res;
		try {
			output_artefacts = new OutputStreamWriter(
				new FileOutputStream (properties.getProperty("strPathArtefactsURIs")),
				java.nio.charset.StandardCharsets.UTF_8 );
			while(resultSet.hasNext()){
				obj = resultSet.next();
				if (obj != null) {
					res =  obj.getResource("s");
					if (res != null) {
						output_artefacts.write( res.toString()+"\n" );
					}
					else {
						throw new IOException( "SPARQL query does not have a bound variable 's'" );
					}
				}
			}
			output_artefacts.close();
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
        Replace BM namespace with a shorter version to save RAM later
    */
	private static void rdf_replacement( String strFileIn, String strFileOut, String [] arraySource, String [] arrayReplacement ) throws IOException {
		FileInputStream fis_RDF = null;
		BufferedReader br_RDF = null;
		OutputStreamWriter ow_RDF = null;
		FileOutputStream fos_RDF = null;

		fis_RDF = new FileInputStream( new File(strFileIn) );
		br_RDF  = new BufferedReader( new InputStreamReader(fis_RDF, java.nio.charset.StandardCharsets.UTF_8) );
		fos_RDF = new FileOutputStream( new File(strFileOut) );
		ow_RDF = new OutputStreamWriter( fos_RDF, java.nio.charset.StandardCharsets.UTF_8 );

		String strTriple = br_RDF.readLine();
		while(strTriple != null) {
			for( int i=0;i<arraySource.length;i++ ) {
				strTriple = strTriple.replace( arraySource[i], arrayReplacement[i] );
			}
			ow_RDF.write( strTriple+"\n" );
			strTriple = br_RDF.readLine();
		}

		ow_RDF.close();
		br_RDF.close();
	}

}

