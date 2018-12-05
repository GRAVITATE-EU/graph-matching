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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.TDBFactory;
/*
 * This class defines the files necessary to implement the GED approximation through stars.
 * INPUT FILE: This class uses the file with triples (TTL format) built by Graph_construct_app.java to define a TDB repository. 
 * It then queries the TDB repository and defines a (single-level, rooted tree) for each (subject) RDF node,
 * OUTPUT FILES: The class writes the following files:
 * - dictionary_for_starsDefinition.txt: a dictionary of all entities in the TDB repo, i.e.,  <entity, counter_dic>, where entity is a string representation of an the entity and counter_dic is an integer
 * - starsDefinition.txt: each line is a star rooted in root, i.e., <root, leaf, leaf,...> (tab separated) 
 * - artefacts_as_sets_of_nodes.txt: each line is the set of nodes linked to a particular artefact (up to a certain level), i.e., <entity, node1, node2,...> 
 * where entity is the URI of the artefact and node is the integer (value) corresponding to the RDF node (key) linked to the artefact, according to dictionary_for_starsDefinition
 */
import com.hp.hpl.jena.util.FileManager;

public class Star_distance_app {
	static Properties properties;
	static HashMap<String, Integer> dictionary = new HashMap<String, Integer>();
	static int counter_dic = 1;
	static FileOutputStream fosDic;;
	static OutputStreamWriter writerDic;
	static Dataset dataset;
	static Model tdb;

	//mlistPersonNames

	/*
	Gets the parameters values specified in the config.properties file
	Input: a string representing the path of config.properties file with parameters 
	 */
	static private void getParamatersValues(String strPath) {
		ParameterValues gettingParameterValues = new ParameterValues();
		properties = gettingParameterValues.loadParamsFromFile(strPath);
	}

	/*
	 Execution of a query to define a star (single-level, rooted tree) for each (subject) RDF node.
	 Writes "starsDefinition.txt" file.
	 Writes the dictionary for star definition (RDF node <-> unique integer).
	 */
	private static void execute_query_to_define_stars() {
		FileOutputStream fos_stars = null;
		// each row in "starsDefinition.txt" is a star <root, leaf, leaf,...> (tab separated)
		// root is the int representing the root node in the dictionary "dictionary_for_starsDefinition.txt"
		// leaf is the int representing the leaf node in the dictionary "dictionary_for_starsDefinition.txt"
		OutputStreamWriter w_stars = null;
		try {
			fos_stars = new FileOutputStream("starsDefinition.txt", false);
			w_stars = new OutputStreamWriter(fos_stars, java.nio.charset.StandardCharsets.UTF_8 );
		} catch (FileNotFoundException e1) {
			System.out.println("output file for stars not found");
			e1.printStackTrace();
		};

		// the query extracts all RDF nodes within single-level and concatenates them, using "->" as separator 
		String q = "SELECT ?s (group_concat(?o ; separator=\"->\") as ?star) WHERE {?s ?p ?o} GROUP BY ?s";
		Query query = QueryFactory.create(q);
		QueryExecution qexec = QueryExecutionFactory.create(query, tdb);
		ResultSet results = qexec.execSelect();
		while (results.hasNext()) {
			QuerySolution result = results.next();
			String s = null ;
			try {s = result.get("s").toString();}
			catch (java.lang.NullPointerException e) {
				System.out.println("Error: no results (construct query created empty set of artifacts?)");
				e.printStackTrace();
				throw e;}
			// dictionary update with the subject
			update_dictionary(s);
			try {
				w_stars.write(dictionary.get(s)+ "\t");
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("IOException while writing stars");
			}
			String star_feature = result.get("star").toString().replace("\t", "").replace("\r\r\n", "").replace("\r\n", "");
			String[] star_feature_splitted = star_feature.split("->");
			for (int i = 0; i < star_feature_splitted.length-1; i++){
				// dictionary update with an object
				update_dictionary(star_feature_splitted[i]);
				try {
					w_stars.write(dictionary.get(star_feature_splitted[i])+ "\t");
				} catch (IOException e) {
					System.out.println("IOException while writing stars");
					e.printStackTrace();
				}
			}
			// dictionary update with an object
			update_dictionary(star_feature_splitted[star_feature_splitted.length-1]);
			try {
				w_stars.write(dictionary.get(star_feature_splitted[star_feature_splitted.length-1])+ "\n");
			} catch (IOException e) {
				System.out.println("IOException while writing stars");
				e.printStackTrace();
			}
		}
		qexec.close();
		try {
			w_stars.close();
			writerDic.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	Updating the dictionary with entries <entity, counter_dic>, where entity is a string and counter_dic is an integer
	Input: String representing an entity (RDF resource)
	 */
	private static void update_dictionary(String entity) {
		if (!dictionary.containsKey(entity)){
			dictionary.put(entity, counter_dic);
			try {
				writerDic.write(counter_dic+"\t"+ entity +"\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("error in writing dictionary for entity" + entity);
				e.printStackTrace();
			}
			counter_dic++;
		}
	}

	/*
	Reading the artefacts URIs.
	Returns the ArrayList<String> with entities' URIs.
	 */
	private static List<String> selectUsefulSeedEntities() {
		ArrayList<String> allEntities = new ArrayList<String>();
		BufferedReader br;
		try {
			//br = new BufferedReader(new FileReader(properties.getProperty("strPathArtefactsURIs")));
			FileInputStream fis = new FileInputStream(properties.getProperty("strPathArtefactsURIs"));
			br  = new BufferedReader( new InputStreamReader(fis, java.nio.charset.StandardCharsets.UTF_8) );
			String strCurrentLine;
			String URI_item;
			while ((strCurrentLine = br.readLine()) != null ) {
				String[] splitted = strCurrentLine.split("\t");
				URI_item = splitted[0];
				allEntities.add(URI_item);
			}
			br.close();
		} catch (FileNotFoundException e) {
			System.out.println("file with artefacts' URIs not found");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IOException reading file with artefacts' URIs");
			e.printStackTrace();
		}
		return allEntities;
	}

	/*
	Execution of a query to define a set of nodes for each artefact (nodes linked up to a certain level)
	File "artefacts_as_sets_of_nodes.txt" is written.
	Each row in the file corresponds to an artefact and is made of the ID of the artfact and the IDs of all nodes linked up to a certain level (\t separated).
	 */
	private static void execute_query_to_define_artefacts_as_sets_of_nodes() {
		FileOutputStream fos_setNodes;
		OutputStreamWriter w_setNodes = null;
		try {
			fos_setNodes = new  FileOutputStream(("artefacts_as_sets_of_nodes.txt"));
			w_setNodes = new OutputStreamWriter(fos_setNodes, java.nio.charset.StandardCharsets.UTF_8);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		List<String> entities = selectUsefulSeedEntities();
		Iterator<String> it_entities = entities.iterator();
		String entity;
		HashSet<Integer> already_processed;// this hashset is used to avoid any duplication of nodes in the description of an artifact.
		while(it_entities.hasNext()) {
			entity = it_entities.next();
			String stringToWrite;
			if (Integer.valueOf(properties.getProperty("depth_for_artefacts_as_sets_of_nodes"))>0) {
				String q = SPARQLQueryBuilder.calcSurroundingGraphQuery(Integer.valueOf(properties.getProperty("depth_for_artefacts_as_sets_of_nodes")), false).replace("$ENTITY$",  entity );
				Query query = QueryFactory.create(q);
				QueryExecution qexec = QueryExecutionFactory.create(query, tdb);
				ResultSet results = qexec.execSelect();
				stringToWrite = entity + "\t" + dictionary.get(entity) + "\t";
				already_processed = new HashSet<Integer>();
				while (results.hasNext()) {
					QuerySolution result = results.next();
					for (String var : results.getResultVars()) {
						if(result.get(var)!=null) {
							Integer value_from_dictionary = dictionary.get(result.get(var).toString().replace("\t", "").replace("\r\r\n", "").replace("\r\n", ""));
							if (!already_processed.contains(value_from_dictionary)) {
								stringToWrite = stringToWrite + value_from_dictionary + "\t";
								already_processed.add(value_from_dictionary);
							}
						}
					}
				}
				qexec.close();
			}else stringToWrite = entity + "\t" + dictionary.get(entity) + "\t";
			try {
				// remove the last '\t' before writing into file
				w_setNodes.write(stringToWrite.substring(0,stringToWrite.lastIndexOf('\t'))+"\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			w_setNodes.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
    * Creates a jena Model from the file with triples (original or updated version).
    * Defines stars and artefacts as sets of nodes and write both into files.
    * */
	public static void main(String[] args) {
		getParamatersValues("config.properties");
		dataset = TDBFactory.createDataset(properties.getProperty("strPathTDBrepoConstruct"));
		tdb = dataset.getDefaultModel();

		java.sql.Timestamp timestamp = new java.sql.Timestamp(System.currentTimeMillis());
		System.out.println("Timestamp (star definition start) " + timestamp.toString() );

		try {
			// File f = new File(properties.getProperty("strPathOfOutputFileForConstructQueryUPDATED"));
			File f = new File(properties.getProperty("strPathOfOutputFileForConstructQueryUPDATED"));
			if (!f.exists()) {
				// use the original version of the triples file, because the updated version (resulting from the clustering of literal) of the triples file has not been found
				FileManager.get().readModel(tdb, properties.getProperty("strPathOfOutputFileForConstructQuery"),"N-TRIPLES");
			} else {
				// use the updated version of the triples file
				// FileManager.get().readModel(tdb, properties.getProperty("strPathOfOutputFileForConstructQueryUPDATED"),"N-TRIPLES");
				FileManager.get().readModel(tdb, properties.getProperty("strPathOfOutputFileForConstructQueryUPDATED"),"N-TRIPLES");
			}
		} catch (NullPointerException e) {
			System.out.println("NullPointerException: Check file for strPathOfOutputFileForConstructQuery in config.properties");
			e.printStackTrace();
			throw(e);
		} catch (Exception e) {
			System.out.println("Error reading file "+properties.getProperty("strPathOfOutputFileForConstructQuery"));
			e.printStackTrace();
		}
		try {
			fosDic = new FileOutputStream("dictionary_for_starsDefinition.txt", false);
			writerDic = new OutputStreamWriter(fosDic, java.nio.charset.StandardCharsets.UTF_8 );
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		execute_query_to_define_stars();
		execute_query_to_define_artefacts_as_sets_of_nodes();
		dataset.close();

		timestamp = new java.sql.Timestamp(System.currentTimeMillis());
		System.out.println("Timestamp (star definition end) " + timestamp.toString() );

	}
}
