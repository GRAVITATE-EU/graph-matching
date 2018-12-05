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
/*
    These functions define the queries used in the classes Star_distance_app.java Astar_distance_app.java and Graph_embedding_app.java
	 */  
package ITinnov.semantic_matching;

public class SPARQLQueryBuilder {

	/*
    Builds a query to get random walks with a certain level or "depth"
    INPUT: "depth" integer for the exploration of the RDF graph
    INPUT: "literals" boolean to consider literals (or not)
    INPUT: "numberWalks" int for the number of walks we want
    OUTPUT: string query
	 */  
	static String calcRandomWalkQuery(int depth, int numberWalks, boolean literals){
		String selectPart = "SELECT ?p ?o1";
		String bodyPart = "{ <$ENTITY$> ?p ?o1  ";
		String query = "";
		int lastO = 1;
		for (int i = 1; i < depth; i++) {
			bodyPart += ". ?o" + i + " ?p" + i + "?o" + (i + 1);
			selectPart += " ?p" + i + "?o" + (i + 1);
			lastO = i + 1;
		}
		String lastOS = "?o" + lastO;
		String filter_part="";//Filter(contains(str(?p),"PX_object_type")).
		//        if(literals==false){filter_part= "FILTER(!isLiteral(" + lastOS + ")).  Filter(!contains(str(?o1),\".jpg\")). Filter(!contains(str(?o1),\"" + "$ENTITY$" + "\"))";}
		if(literals==false){
			filter_part= "FILTER(!isLiteral(" + lastOS + ")).";
		}
		query = selectPart + " WHERE " + bodyPart + " . " + filter_part + " BIND(RAND() AS ?orderKey) } ORDER BY ?orderKey LIMIT "
				+ numberWalks;
		System.out.println(query);
		return query;
	}
	
	/*
    Builds a query to get random walks with a certain level or "depth"
    INPUT: "depth" integer for the exploration of the RDF graph
    INPUT: "literals" boolean to consider literals (or not)
    INPUT: "numberWalks" int for the number of walks we want
    OUTPUT: string query
	 */  
	static String calcRandomWalkQueryUpToDepth(int depth, int numberWalks, boolean literals){
		String selectPart = "SELECT ?p ?o1";
		String bodyPart = "{ <$ENTITY$> ?p ?o1  ";
		String query = "";
		int lastO = 1;
		String optionalPart = "";
		for (int i = 1; i < depth; i++) {
			bodyPart += " OPTIONAL{"+optionalPart+"?o" + i + " ?p" + i + " ?o" + (i + 1)+".}";
			optionalPart+= " ?o" + i + " ?p" + i + " ?o" + (i + 1)+". ";
			selectPart += " ?p" + i + "?o" + (i + 1);
			lastO = i + 1;
		}
		String lastOS = "?o" + lastO;
		String filter_part="";//Filter(contains(str(?p),"PX_object_type")).
		//        if(literals==false){filter_part= "FILTER(!isLiteral(" + lastOS + ")).  Filter(!contains(str(?o1),\".jpg\")). Filter(!contains(str(?o1),\"" + "$ENTITY$" + "\"))";}
		if(literals==false){
			filter_part= "FILTER(!isLiteral(" + lastOS + ")).";
		}
		query = selectPart + " WHERE " + bodyPart + " . " + filter_part + " BIND(RAND() AS ?orderKey) } ORDER BY ?orderKey LIMIT "
				+ numberWalks;
		System.out.println(query);
		return query;
	}

	/*
	 * TODO: modify the way weights are defined
    Builds a query to get weighted walks with a certain level or "depth"
    INPUT: "depth" integer for the exploration of the RDF graph
    INPUT: "literals" boolean to consider literals (or not)
    INPUT: "numberWalks" int for the number of walks we want
    OUTPUT: string query
	 */   
	static String calcWeightedWalkQuery(int depth, int numberWalks, boolean literals){
		String selectPart = "SELECT ?p ?o1";
		String bodyPart = "{ <$ENTITY$> ?p ?o1  ";
		String query = "";
		int lastO = 1;
		for (int i = 1; i < depth; i++) {
			bodyPart += ". ?o" + i + " ?p" + i + "?o" + (i + 1);
			selectPart += " ?p" + i + "?o" + (i + 1);
			lastO = i + 1;
		}
		String lastOS = "?o" + lastO;
		String filter_part="";//Filter(contains(str(?p),"PX_object_type")).
		String bind_part="BIND(IF(?p=<http://www.cidoc-crm.org/cidoc-crm/P45_consists_of>, 0.9,0.1) as ?w1).";
		//        if(literals==false){filter_part= "FILTER(!isLiteral(" + lastOS + ")).  Filter(!contains(str(?o1),\".jpg\")). Filter(!contains(str(?o1),\"" + "$ENTITY$" + "\"))";}
		if(literals==false){
			filter_part= "FILTER(!isLiteral(" + lastOS + ")).";
		}
		query = selectPart + " WHERE " + bodyPart + " . " + bind_part + filter_part + " } ORDER BY DESC(?w1) LIMIT "
				+ numberWalks;
		System.out.println(query);
		return query;
	}

	/*
    Builds a query to get surrounding resources within a certain level or "depth"
    OPTIONAL allows to consider all resources within a certain level (not just resources AT a certain level)
    INPUT: "depth" integer for the exploration of the RDF graph
    INPUT: "booleanForO0" boolean to consider the subject, i.e., o0, as a query result (or not)
    OUTPUT: string query
	 */   		
	static String calcSurroundingGraphQuery(int depth, boolean booleanForO0){
		String strO0 = (booleanForO0) ? "(str(<$ENTITY$>) as ?strO0)" :  "";
		String selectPart = "SELECT distinct"+ strO0 +"  (str(?o1) as ?strO1) ";
		String bodyPart = "{ <$ENTITY$> ?p ?o1  ";
		String query = "";
		String optionalPart = "";
		for (int i = 1; i < depth; i++) {
			bodyPart += " OPTIONAL{"+optionalPart+"?o" + i + " ?p" + i + " ?o" + (i + 1)+".}";
			optionalPart+= " ?o" + i + " ?p" + i + " ?o" + (i + 1)+". ";
			selectPart += "(str(?o" + (i + 1) + ") as ?strO" + (i + 1) + ") ";
		}
		query = selectPart + " WHERE " + bodyPart + " .   }  ";
		return query;
	}
	
/*
 *  * calcDataImportQueryFILTER uses a regex function to filter out non useful predicates
 */
	//static String calcDataImportQueryFILTER(){
	//  String query = " SELECT ?s ?p ?o WHERE{ ?s ?p ?o . "+build_filter_part()+ "}";
	//  return query;
	//}
	//
	//private static String build_filter_part() {
	//  String filter_part = "";
	//  filter_part +="FILTER( regex( STR(?p), \"^(http://www.cidoc-crm.org/cidoc-crm/*.|http://www.w3.org/2004/02/skos/core#*."
	//          + "|http://purl.org/dc/*.|http://www.w3c.org/2002/07/owl#*.|http://www.researchspace.org/ontology/PX_*.)\", \"i\" ) )." ;
	//  return filter_part;
	//}
}
