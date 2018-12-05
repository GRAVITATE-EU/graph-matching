# GRAVITATE Graph Matching Tool

Tool to query the GRAVITATE RDF artifact database and compute a pair-wise artifact similarity matrix suitable for ingestion into the GRAVITATE platform.

# Install

Copy the GRAVITATE graph matching release files to [install dir]

Install Java 1.8

Install Python 2.7 and Pip

Install Python lib Numpy 1.13.1+mk1

Install Python lib Munkres 1.0.12

Install Python lib Gensim 3.0.1

Install Python lib Sklearn 0.0

Note: The blazegraph database endpoint is assumed to be http://localhost:9999/blazegraph/sparql if this is not correct edit config.properties

# Usage

Edit the configuration file config.properties
+ strPathToQueryGraphID = file containing SPARQL query to find artifact ID’s
+ strPathToQueryConstruct = file containing SPARQL query to create artifact RDF raph

Run the numeric range app

cd [install dir]

java -Xmx32g -cp “graph-construct;third-party/*” ITinnov.semantic_matching.numeric_range_app

python numeric_range_app.py

Run the graph construct app

java -Xmx32g -cp “graph-construct;third-party/*” ITinnov.semantic_matching.Graph_construct_app

Run the rdf2vec

java -Xmx32g -cp “rdf2vec;third-party/*” ITinnov.semantic_matching.Star_distance_app

java -Xmx32g -cp “rdf2vec;third-party/*” ITinnov.semantic_matching.Graph_embedding_app

python rdf_walks2vec_app.py

python evaluation_app.py

The result file file_distances_between_artefacts contains the pair-wise artifact similarity scores ready for ingest via the GRAVITATE ETL process.

# Contact

Admin: Stuart E. Middleton sem03[at]soton.ac.uk
