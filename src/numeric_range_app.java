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
// Created Date : 2017/06/26
// Created for Project : GRAVITATE
//
/////////////////////////////////////////////////////////////////////////
//
// Dependencies : NONE 
//
/////////////////////////////////////////////////////////////////////////
package ITinnov.semantic_matching;

import java.io.*;
import java.util.Properties;

import org.apache.jena.atlas.json.JsonParseException;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.tdb.TDBFactory;
/*
 * This class queries Blazegraph looking for the objects of the predicates in the list listPredicatesForPreprocessing in config.properties.
 * listPredicatesForPreprocessing is a \t separated list of predicates.
 * The class writes a distinct file with such objects (as strings) for each predicate.
 */
public class numeric_range_app {
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
	Creates a directory where the files with literal to process/cluster are written
	A file for each predicate is created, e.g., for predicate "<http://www.cidoc-crm.org/cidoc-crm/P82a_begin_of_the_begin>"
	Input: the name of the directory
	Return: the directory created
	 */
	private static File create_directory_for_preprocessing(String directory_name) {
		File theDir = new File(directory_name);

		// if the directory does not exist, create it
		if (!theDir.exists()) {
			System.out.println("creating directory: " + theDir.getName());
			boolean result = false;

			try{
				theDir.mkdir();
				result = true;
			}
			catch(SecurityException se){
				//handle it
				System.out.println("error in the creation of directory for preprocessing"+directory_name);
			}
			if(result) {
				System.out.println("directory "+directory_name+" created");
			}
		}else{
			System.out.println("directory "+directory_name+" for preprocessing alredy exists. Delete this folder to continue");
			System.exit(0);}
		return theDir;
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
	Executes the query against Blazegraph to extract values for a predicate.
	ex: predicate "<http://www.cidoc-crm.org/cidoc-crm/P82a_begin_of_the_begin>"
	Input: a string representing the predicate, BufferedWriter to write predicate values.
	 */
	private static void execute_query(String predicate, BufferedWriter bw) throws IOException {
		// load numeric query template from disk
		// String q = "SELECT distinct (str(?o) as ?label) where{ ?subject "+predicate+" ?o. }";
		String q = read_query( properties.getProperty( "strPathToPreprocessingDateQuery" ) );
		q = q.replace( "PREDICATE", predicate );

		// execute query
		QueryEngineHTTP httpQuery = null;
		long startTime = System.currentTimeMillis();
		long pastTime = startTime;
		while (true) {
			try {
				httpQuery = new QueryEngineHTTP(properties.getProperty("strServiceURI"), q);
				Dataset dataset = TDBFactory.createDataset();
				ResultSet results = httpQuery.execSelect();
				while(results.hasNext()){
					String strResult = results.next().get("label").toString();
					bw.write(strResult+"\n");
				}
				break;
			}catch (JsonParseException e0) {
				System.out.println("strMessageJsonParseException");
				if (System.currentTimeMillis() - startTime > Integer.valueOf(properties.getProperty("timeLimitforImportQuery"))) {
					if (!httpQuery.isClosed()) httpQuery.close(); // it guarantees connection will be closed anyway
					throw e0;
				}
			} catch (com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP e) {  // the URI for import query is not well written
				if (e.getMessage().contains("MalformedURLException")) {
					System.out.println("Check the URI for http query");
					throw (e);
				} else if (e.getCause() instanceof org.apache.http.conn.HttpHostConnectException) {  // connection refused; trying to connect again
					if ((System.currentTimeMillis() - pastTime) / intTimePrintingException == 1) {
						pastTime = System.currentTimeMillis();
						System.out.println("Check connectivity to " + properties.getProperty("strServiceURI").replace("sparql", ""));
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
	}

	/*
	Executes the query against Blazegraph to extract values for a predicate.
	ex: predicate "<http://www.cidoc-crm.org/cidoc-crm/P82a_begin_of_the_begin>"
	Input: a string representing the predicate, BufferedWriter to write predicate values.
	 */
	private static void execute_query_unit(String type_dimension, BufferedWriter bw) throws IOException {
		// TODO Auto-generated method stub
		String q = "PREFIX crm: <http://www.cidoc-crm.org/cidoc-crm/> SELECT distinct (str(?value_with_unit) as ?label) where{?dimension crm:P2_has_type "+type_dimension+" . \n" +
				"                                                ?dimension crm:P91_has_unit ?dim_unit .\n" +
				"                                                ?dimension crm:P90_has_value ?dim_value .\n" +
				"                                                BIND(concat(str(?dim_value),str(?dim_unit)) as ?value_with_unit).}";
		QueryEngineHTTP httpQuery = null;
		long startTime = System.currentTimeMillis();
		long pastTime = startTime;
		while (true) {
			try {
				httpQuery = new QueryEngineHTTP(properties.getProperty("strServiceURI"), q);
				Dataset dataset = TDBFactory.createDataset();
				ResultSet results = httpQuery.execSelect();
				while(results.hasNext()){
					String strResult = results.next().get("label").toString();
					//System.out.println(strResult);
					bw.write(strResult+"\n");
				}
				break;
			}catch (JsonParseException e0) {
				System.out.println("strMessageJsonParseException");
				if (System.currentTimeMillis() - startTime > Integer.valueOf(properties.getProperty("timeLimitforImportQuery"))) {
					if (!httpQuery.isClosed()) httpQuery.close(); // it guarantees connection will be closed anyway
					throw e0;
				}
			} catch (com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP e) {  // the URI for import query is not well written
				if (e.getMessage().contains("MalformedURLException")) {
					System.out.println("Check the URI for http query");
					throw (e);
				} else if (e.getCause() instanceof org.apache.http.conn.HttpHostConnectException) {  // connection refused; trying to connect again
					if ((System.currentTimeMillis() - pastTime) / intTimePrintingException == 1) {
						pastTime = System.currentTimeMillis();
						System.out.println("Check connectivity to " + properties.getProperty("strServiceURI").replace("sparql", ""));
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
	}

	/*
	For each predicate to process (date or unit), it writes a file with its values.
	It iterates over the list of predicates and calls the execute_query method for each predicate, passing a BufferedWriter as parameter.
	 */
	public static void main(String[] args) throws IOException {
		getParamatersValues("config.properties");
		create_directory_for_preprocessing("preprocess_literal");
		FileWriter fw_CORRESPONDENCE_PREDICATE_LABEL = new FileWriter("CORRESPONDENCE_PREDICATE_LABEL");
		BufferedWriter bw_CORRESPONDENCE_PREDICATE_LABEL = new BufferedWriter(fw_CORRESPONDENCE_PREDICATE_LABEL);
		String typeOfPredicate, listOfPredicates;
		String[] listOfPredicates_splitted;

		java.sql.Timestamp timestamp = new java.sql.Timestamp(System.currentTimeMillis());
		System.out.println("Timestamp (numeric range bin start) " + timestamp.toString() );

		if(properties.containsKey("listPredicatesForPreprocessingDate")){
			typeOfPredicate = "date";
			listOfPredicates = properties.getProperty("listPredicatesForPreprocessingDate");
			listOfPredicates_splitted = listOfPredicates.split("\t");
			int cont_predicate = 0;
			for (int i = 0; i < listOfPredicates_splitted.length; i++) {

				System.out.println( "processing date " + listOfPredicates_splitted[i] );

				String [] listPredPair = listOfPredicates_splitted[i].split(" ");
				if (listPredPair.length != 2) {
					throw new IOException("bad date spec");
				}

				cont_predicate++;
				// filling the file with predicate and relative label (with the construct's date predicate)
				bw_CORRESPONDENCE_PREDICATE_LABEL.write(typeOfPredicate+cont_predicate+"\t"+listPredPair[1]+"\n");
				// filling the file with literals to cluster (with original RDF date predicate)
				FileWriter fw = new FileWriter("preprocess_literal/"+typeOfPredicate+cont_predicate);
				BufferedWriter bw = new BufferedWriter(fw);
				execute_query( listPredPair[0], bw );
				bw.close();
			}
		} else {System.out.println("Literals (dates) will be treated as strings. No predicate for clustering found in the configuration file.");}
		if(properties.containsKey("listPredicatesForPreprocessingUnit")){
			typeOfPredicate = "unit";
			listOfPredicates = properties.getProperty("listPredicatesForPreprocessingUnit");
			listOfPredicates_splitted = listOfPredicates.split("\t");
			int cont_predicate = 0;
			for (int i = 0; i < listOfPredicates_splitted.length; i++) {

				System.out.println( "processing unit " + listOfPredicates_splitted[i] );

				String [] listPredPair = listOfPredicates_splitted[i].split(" ");
				if (listPredPair.length != 2) {
					throw new IOException("bad unit spec");
				}

				cont_predicate++;
				// filling the file with predicate and relative label (with the construct's unit predicate)
				bw_CORRESPONDENCE_PREDICATE_LABEL.write(typeOfPredicate+cont_predicate+"\t"+listPredPair[1]+"\n");
				// filling the file with literals to cluster (with original RDF unit predicate)
				FileWriter fw = new FileWriter("preprocess_literal/"+typeOfPredicate+cont_predicate);
				BufferedWriter bw = new BufferedWriter(fw);
				execute_query_unit( listPredPair[0], bw );
				bw.close();
			}
		} else {System.out.println("Literals (dimensions) will be treated as strings. No predicate for clustering found in the configuration file.");}
		bw_CORRESPONDENCE_PREDICATE_LABEL.close();

		timestamp = new java.sql.Timestamp(System.currentTimeMillis());
		System.out.println("Timestamp (numeric range bin end) " + timestamp.toString() );

	}
}
