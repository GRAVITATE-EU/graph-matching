# !/usr/bin/env python
# -*- coding: utf-8 -*-

"""
..
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
	// Created By : jessica Rosati
	// Created Date : 2017/06/27
	// Created for Project : GRAVITATE
	//
	/////////////////////////////////////////////////////////////////////////
	//
	// Dependencies : NONE
	//
	/////////////////////////////////////////////////////////////////////////
	'''

This module uses the file with walks on RDF graph to train a gensim word2vec model.
The model trained is then used to define the distances between artefacts and write them into a file.
"""
def ConfigSectionReader(Config, section):
	"""Defines a dictionary whose entries are the fields of a section in the configuration file

	:param Config: the ConfigParser instance
	:type Config: ConfigParser
	:param section: the name of a section in the configuration file
	:type section: String
	:return: the dictionary whose entries are the fields of a section in the configuration file
	:rtype: dict
	"""
	dictSec = {}
	options = Config.options(section)
	for option in options:
		dictSec[option] = Config.get(section, option)
	return dictSec

def read_artefacts(fname_artefacts):
	"""Reads the file with the artefacts 

	This function reads the file with the artefacts and builds the list of all artefacts, i.e., artefacts_all.
	:param fname_artefacts: the path of the file with artafacts (each line in the file corresponds to an artefact)
	:type fname_artefacts: String
	"""
	readHandle = codecs.open( fname_artefacts, 'r', 'utf-8', errors = 'replace' )
	content = readHandle.readlines()	
	for line in content:
		line = line.rstrip('\n\r')
		#line = line.replace("http://collection.britishmuseum.org/id/object/","")
		#line = line.replace("http://o/","")
		artefacts_all.append(line)

	'''
	with open(fname_artefacts) as f:
		content = f.readlines()	
		for line in content:
			line = line.rstrip('\n')
			line = line.replace("http://collection.britishmuseum.org/id/object/","")
			line = line.replace("http://o/","")
			artefacts_all.append(line)	
	'''

def train_rdf_walks2vec(file_with_walks, min_count_input, window_input, iter_input, size_input):
	"""Trains a gensim word2vec model and saves it.

	This function uses the file with walks on RDF graph to train a gensim word2vec model and saves it.
	:param file_with_walks: the name of the file with walks on RDF graph
	:type file_with_walks: string
	:param min_count_input: the minimum number of occurrences for a word to be considered
	:type min_count_input: int
	:param window_input: the size of the window (context around central word)
	:type window_input: int
	:param iter_input: the number of iterations for training
	:type iter_input: int
	:param size_input: the size of the embedding (vectors)
	:type size_input: int
	"""
	sentences = word2vec.LineSentence( file_with_walks+'.txt' )
	model = gensim.models.Word2Vec( sentences, min_count=min_count_input,window=window_input, iter= iter_input, size=size_input ,sg=1, seed=1, workers=1 )
	model.save( 'prova'+str(min_count_input)+file_with_walks )

def getDistances(strURIArtefactI, arrayOfstrURIArtefact, f_distances_between_artefacts, model,logger, nThresholdSimilarity,nTopN):
	"""Builds the distances between an artefact and a list of artefacts and writes them

	This function builds the distances between an artefact (1st param) and a list of artefacts (2nd param) and writes them in a file (3rd param).
	The distance is given by gensim word2vec model previously trained with walks
	:param strURIArtefactI: the first artefact to compare
	:type strURIArtefactI: string
	:param arrayOfstrURIArtefact: the list of other artefacts with which strURIArtefactI must be compared
	:type arrayOfstrURIArtefact: list of string
	:param f_distances_between_artefacts: the path of the file for writing distances between_artefacts
	:type f_distances_between_artefacts: string
	:param model: the word2vec model trained
	:type model: gensim.models.Word2Vec
	:param logger
	"""

	# make a full set of matches for this artifact
	listMatches = []
	for artefactJ in arrayOfstrURIArtefact :
		if artefactJ != strURIArtefactI :
			try :
				# cosine_distance = 1 - model.wv.similarity("bmo:"+strURIArtefactI, "bmo:"+artefactJ)
				#cosine_distance = 1 - model.wv.similarity("http://o/"+strURIArtefactI, "http://o/"+artefactJ)
				cosine_distance = 1 - model.wv.similarity( strURIArtefactI, artefactJ )

				# avoid reporting low confidence pairs as 9k artifacts will create 81M pairs
				if cosine_distance <= nThresholdSimilarity :
					listMatches.append( [cosine_distance,artefactJ] )
			except:
				# TODO work out why word missing
				# logger.exception( 'word2vec distance similarity fail (ignoring)' )
				pass

	# sort matches by distance (smallest distance first)
	listMatches = sorted( listMatches, key=lambda entry: entry[0], reverse=False )

	# write top N to disk
	nMax = nTopN
	if (nTopN == -1) or (nTopN > len(listMatches)) :
		nMax = len(listMatches)
	for nMatch in range(nMax) :
		strEntry = strURIArtefactI + "\t" + listMatches[nMatch][1] + "\t" + str(listMatches[nMatch][0])
		f_distances_between_artefacts.write( strEntry + "\n" )


def loadModel_and_DefineDistances(logger,nThresholdSimilarity,nTopN):
	"""Loads the gensim word2vec model previously trained and uses it to define distances.
	It writes distances into a file.
	"""
	model = gensim.models.Word2Vec.load( 'prova'+str(min_count_input)+file_with_walks )

	f_distances_between_artefacts = codecs.open( ConfigSectionReader(Config,"rdf_walks2vec_app")['distances_rdf_walks2vec_app_file'], 'wb', 'utf-8', errors = 'replace' )
	#f_distances_between_artefacts = open( ConfigSectionReader(Config,"rdf_walks2vec_app")['distances_rdf_walks2vec_app_file'], 'wb' )
	artefacts_all_copy = []
	for art in artefacts_all:
	    artefacts_all_copy.append(art)
	for artefact in artefacts_all_copy:
		getDistances(artefact, artefacts_all, f_distances_between_artefacts, model,logger,nThresholdSimilarity,nTopN)
		artefacts_all.remove(artefact)
	f_distances_between_artefacts.close()

if __name__ == "__main__":
	import logging,warnings,codecs
	warnings.filterwarnings(action='ignore', category=UserWarning, module='gensim')
	import gensim
	from gensim.models import word2vec
	import ConfigParser

	# make logger (global to STDOUT)
	LOG_FORMAT = ('%(levelname) -s %(asctime)s %(message)s')
	logger = logging.getLogger( __name__ )
	logging.basicConfig( level=logging.INFO, format=LOG_FORMAT )
	logger.info('logging started')

	Config = ConfigParser.ConfigParser()
	Config.read( "config.ini" )

	file_with_walks = ConfigSectionReader(Config,"rdf_walks2vec_app")['file_with_walks']
	min_count_input = int( ConfigSectionReader(Config,"rdf_walks2vec_app")['min_count_input'] )
	window_input = int( ConfigSectionReader(Config,"rdf_walks2vec_app")['window_input'] )
	iter_input = int( ConfigSectionReader(Config,"rdf_walks2vec_app")['iter_input'] )
	size_input = int( ConfigSectionReader(Config,"rdf_walks2vec_app")['size_input'] )
	nThresholdSimilarity = float( ConfigSectionReader(Config,"rdf_walks2vec_app")['threshold_similarity_rdf2vec'] )
	nTopN = int( ConfigSectionReader(Config,"rdf_walks2vec_app")['similarity_top_n'] )

	logger.info('training rdf2vec model (train)')
	train_rdf_walks2vec(file_with_walks, min_count_input, window_input, iter_input, size_input)

	logger.info('reading artifacts (test)')
	artefacts_all=[]
	read_artefacts( ConfigSectionReader(Config,"all_app")['artefacts_file'] )
	#artefacts_all = artefacts_reduced

	logger.info('running rdf2vec model (test)')
	loadModel_and_DefineDistances(logger,nThresholdSimilarity,nTopN)

	logger.info('finished')