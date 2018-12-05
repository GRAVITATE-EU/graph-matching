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

This module approximates Graph Edit Distance, aka GED, with star edit distance.
It first computes SED for every pair of stars.
Then it approximates GED for every pair of artefacts, considering the stars that are part of the artefact graph (regarded as a set of stars): 
given two artefacts, the Hungarian algorithm is used to define the cost of "transforming" an artefact graph into another artefact graph.
"""
from __future__ import division
import io, os, logging, copy, math, multiprocessing, ConfigParser, time, sys, codecs
import numpy, psutil
from munkres import Munkres
from datetime import datetime

def star_edit_distance( logger, sed_file, dict_stars_size, dict_pairs_of_stars_SED ):
	"""Builds the dictionary dict_pairs_of_stars_SED, i.e.,  <pair of stars, SED> 

	This function computes the SED for each pair of stars in the starsDefinition.txt file
	Each line in such file is a star, made of a root and a list of leaves (linked resources in RDF)
	:param logger
	:param sed_file: the path of the file where SED must be written
	:type sed_file: String
	"""

	logger.info( 'reading star definition file and computing numpy array index' )

	# read star definition file into memory (set of star_id's for each artifact graph node)
	# and create a lookup index for numpy array (leaves)
	indexLeaves = {}
	readHandle = codecs.open( ConfigSectionReader("sed_app")['stars_definition_file'], 'r', 'utf-8', errors = 'replace' )
	for line in readHandle :
		line = line.rstrip('\n\r')
		line_splitted = line.split('\t')
		dict_stars_size[ int(line_splitted[0]) ] = len(line_splitted) - 1
		# tupleStar = star_id, ... leave_star_ids ...
		tupleStar = tuple( line_splitted )

		indexLeaves[ tupleStar[0] ] = set( tupleStar[1:] )
	readHandle.close()

	'''
	for line in io.open( ConfigSectionReader("sed_app")['stars_definition_file'] , 'r' ) :
		line = line.rstrip('\n')
		line_splitted = line.split('\t')
		dict_stars_size[ int(line_splitted[0]) ] = len(line_splitted) - 1
		# tupleStar = star_id, ... leave_star_ids ...
		tupleStar = tuple( line_splitted )

		indexLeaves[ tupleStar[0] ] = set( tupleStar[1:] )
	'''

	logger.info( 'number of stars in graph = ' + str(len(indexLeaves)) )

	# get a set of all possible tupleStar pairs
	# note this gets big - 9000 artifacts, 46000 stars, sqr(46000) pairs - so we cannot naively store all combinations due to RAM constraints
	# therefore ONLY record SED where the stars have something in common in the first place
	dict_pairs_of_stars_SED = {}
	listStarIDs = indexLeaves.keys()
	for nStar1 in range( len(listStarIDs) ) :
		for nStar2 in range( nStar1, len(listStarIDs) ) :

			root1 = listStarIDs[nStar1]
			root2 = listStarIDs[nStar2]
			if root1 == root2:
				# same root means same star in a RDF graph
				nSED = 0
			else:
				setLeaves1 = indexLeaves[ root1 ]
				setLeaves2 = indexLeaves[ root2 ]

				setIntersection = setLeaves1.intersection( setLeaves2 )
				nCommonElements = len(setIntersection)
				if nCommonElements > 0 :

					# calc star edit distance accoring to lemma 4.1 of paper "Comparing stars: on approximating graph edit distance"
					max_length = max( len(setLeaves1), len(setLeaves2) )
					nSED = 1 + abs(len(setLeaves1) - len(setLeaves2)) + max_length - nCommonElements
				else :
					nSED = -1

			if nSED != -1 :
				dict_pairs_of_stars_SED[ ( int(root1), int(root2) ) ] = nSED
				sed_file.write( root1+'\t'+root2+'\t'+str(nSED)+'\n' )

		if nStar1/1000 == math.floor(nStar1/1000) :
			logger.info( 'star count (pair calculation) = ' + str(nStar1) )
		
		# unit test
		'''
		if nStar1 == 200 :
			break
		'''

	'''
	# the star ID is the 1st number in each line
	pairs_of_stars = list(itertools.combinations(listLines, 2))

	logger.info( 'T1' )

	# calculate the star edit distance between each pair of stars
	for pair_of_stars in pairs_of_stars:
		root1= pair_of_stars[0][0]
		root2= pair_of_stars[1][0]
		if root1 == root2:
			# same root means same star in a RDF graph
			SED = 0
		else:
			leaves1 = indexLeaves[ root1 ]
			leaves2 = indexLeaves[ root2 ]

			max_length = max( len(leaves1), len(leaves2) )

			# star edit distance accoring to lemma 4.1 of paper "Comparing stars: on approximating graph edit distance"
			nSED = 1 + abs(len(leaves2) - len(leaves1)) + max_length - len( numpy.intersect1d(leaves1, leaves2, assume_unique = True ))

		dict_pairs_of_stars_SED[ ( int(root1), int(root2) ) ] = nSED
		sed_file.write(root1+'\t'+root2+'\t'+str(nSED)+'\n')
	'''

def read_star_edit_distance( dict_pairs_of_stars_SED ):
	# if the file with SED is already available, then we read it
	readHandle = codecs.open( 'sed_file', 'r', 'utf-8', errors = 'replace' )
	for line in readHandle :
		line = line.rstrip('\n\r')
		line_splitted = line.split('\t')
		dict_pairs_of_stars_SED[(int(line_splitted[0]), int(line_splitted[1]))]=int(line_splitted[2]) #using numpy.uint32 instead of int does not reduce time
	readHandle.close()

	'''
	for line in io.open( 'sed_file' , 'r' ) :
		line = line.rstrip('\n')
		line_splitted = line.split('\t')
		dict_pairs_of_stars_SED[(int(line_splitted[0]), int(line_splitted[1]))]=int(line_splitted[2]) #using numpy.uint32 instead of int does not reduce time
	'''
		
def read_artefacts( fname_artefacts, artefacts_all ):
	"""Reads the file with the artefacts 

	This function reads the file with the artefacts and builds the list of all artefacts, i.e., artefacts_all
	:param fname: the path of the file with artafacts (each line in the file corresponds to an artefact)
	:type fname: String
	"""

	readHandle = codecs.open( fname_artefacts, 'r', 'utf-8', errors = 'replace' )
	content = readHandle.readlines()	
	for line in content:
		line = line.rstrip('\n\r')

		'''
		# DISABLED AS WONT WORK FOR DBPEDIA
		# remove the namespace so we are working with just the ID not the URI
		line = line.replace("http://collection.britishmuseum.org/id/object/","")
		line = line.replace("http://o/","")
		'''

		artefacts_all.append(line)
	readHandle.close()

	'''
	with open(fname_artefacts) as f:
		content = f.readlines()	
		for line in content:
			line = line.rstrip('\n')
			# remove the BM namespace so we are working with just the ID not the URI
			line = line.replace("http://collection.britishmuseum.org/id/object/","")
			line = line.replace("http://o/","")
			artefacts_all.append(line)
	'''

def read_artefacts_as_set_of_nodes( strPath_artefacts_as_sets_of_nodes ):
	"""Reads the file with the artefacts as set of nodes

	This function reads the file with the artefacts as set of nodes written by Star_distance_app.java
	:param strPath_artefacts_as_sets_of_nodes: the path of the file with artafacts set of nodes (each line in the file corresponds to an artefact)
	:type strPath_artefacts_as_sets_of_nodes: String
	:return: a dictionary with an artefact (KEY) and a list of nodes (VALUE) <artefact name, list of nodes>
	:rtype: dict
	"""
	artefacts_as_set_of_nodes = {}
	readHandle = codecs.open( strPath_artefacts_as_sets_of_nodes, 'r', 'utf-8', errors = 'replace' )
	for line in readHandle :
		line = line.rstrip('\n\r')

		'''
		# DISABLED AS WONT WORK FOR DBPEDIA
		# remove the BM namespace so we are working with just the ID not the URI
		line = line.replace("http://collection.britishmuseum.org/id/object/","")
		line = line.replace("http://o/","")
		'''

		line_splitted = line.split('\t')
		list_of_int = []
		for element in line_splitted[1:] :
			# STUART: SPARQL query to create this list can generate 'null' for nothing so test for this
			if element != 'null' :
				list_of_int.append(int(element))
		artefacts_as_set_of_nodes[ line_splitted[0] ] = list_of_int
	return artefacts_as_set_of_nodes

def readDistanceBetweenStars( i, j, dict_stars_size, dict_pairs_of_stars_SED ):
	"""Looks for the distance between two stars.

	This function looks for the distance between two stars in dict_pairs_of_stars_SED
	:param i: the first star
	:type i: int
	:param j: the second star
	:type j: int
	:return: float(value) for SED, i.e., the distance between the input stars
	:rtype: float
	.. note:: see dictionary_for_starsDefinition.txt for correspondence star(RDF node) <-> int
	.. note:: null stars or single-node stars are also taken into account
	"""

	# note: star ID starts from 1 so i or j == 0 will cause no match

	if (i,j) in dict_pairs_of_stars_SED:
		value = dict_pairs_of_stars_SED[(i,j)]
	elif (j,i) in dict_pairs_of_stars_SED:
		value = dict_pairs_of_stars_SED[(j,i)]
	else : 
		# note: STUART
		# if the pair SED is missing then the pair have no common stars so SED is simply worse case value
		# lemma 4.1 of paper "Comparing stars: on approximating graph edit distance"
		# if i or j (or both) is not a codified star, i.e. no subject of RDF triples, then SED corresponds to 1+2*(maximum number of leaves between star i and star j)

		if j in dict_stars_size :
			nSizeJ = dict_stars_size[j]
		else :
			nSizeJ = 0

		if i in dict_stars_size :
			nSizeI = dict_stars_size[i]
		else :
			nSizeI = 0
		
		if (nSizeJ == 0) and (nSizeJ == 0) :
			# note: STUART if both stars not defined then we have no basis to make a SED so give it a large value (e.g. 50)
			value = 1 + 2 * 50
		else :
			value = 1 + 2 * max( nSizeJ, nSizeI )

	return(float(value))

def hungarian_algorithm(matrix):
	"""Implements the hungarian algorithm.

	This function implements the hungarian optimization algorithm to estimate the distance between two artefacts.
	One artefact is represented on rows, one artefact is represented on columns.
	Each entry (i, j) of the input matrix is a SED.
	:param matrix: the matrix with SEDs between stars composing the two artefacts to compare
	:type matrix: array[][]
	:return: total is the hungarian cost to "transform" an artefact into the other one
	:rtype: float
	"""
	m = Munkres()
	indexes = m.compute(matrix)
	total = 0
	for row, column in indexes:
		value = matrix[row][column]
		total += value
	return total

#def define_matrix_hungarian_and_solve( artefactI, artefactJ, dictIndexNodes, logger, dict_stars_size, dict_pairs_of_stars_SED ) :
def define_matrix_hungarian_and_solve( tupleQueue, dictIndexNodes, dict_stars_size, dict_pairs_of_stars_SED, parent_pid ) :
	"""Builds the matrix for the hungarian algorithm and calls hungarian_algorithm

	This function builds the matrix for hungarian algorithm, i.e., a nXn matrix where n is the max size of the artefacts under comparison.
	Null nodes are added for graph normalization, that is to compare two graphs of the same size n.
	:param artefactI: the first artefact to compare
	:type artefactI: string
	:param artefactJ: the second artefact to compare
	:type artefactJ: string
	:param logger
	:param graphArtefactI: the graph of artefactI
	:type graphArtefactI: numpy.array
	:return: the approximated GED between artefactI and artefactJ
	:rtype: float
	"""
	#graphArtefactJ = numpy.array( artefacts_as_set_of_nodes[artefactJ], dtype=numpy.int16 )

	# note: we dont use logger as it will not pickle for multiprocessing lib

	try :

		bFinished = False
		while bFinished == False :

			# check parent process still exists. if not abort
			if psutil.pid_exists( parent_pid ) == False :
				raise Exception( 'parent process dead' )

			# process the input queue
			while tupleQueue[0].empty() == False :
				# pause if the result queue is too large to allow main process time to process the results
				while tupleQueue[1].qsize() > 100 :
					time.sleep(0.001)

				# get next pair
				( artefactI, artefactJ ) = tupleQueue[0].get()

				# end marker?
				if (artefactI == None) and (artefactJ == None) :
					bFinished = True
					break

				# lookup index for node graph data 
				( graphArtefactJ, max_degree_of_a_vertex_in_graphArtefactJ ) = dictIndexNodes[artefactJ]
				( graphArtefactI, max_degree_of_a_vertex_in_graphArtefactI ) = dictIndexNodes[artefactI]

				# computing the maximum degree of a vertex both in artefactI and in artefactJ
				max_degree_of_a_vertex = max( max_degree_of_a_vertex_in_graphArtefactI, max_degree_of_a_vertex_in_graphArtefactJ )

				# compute a numpy matrix for SED between each star in artifact I and each star in artifact J
				# note: we need a square matrix for hungarian_algorithm
				# if the two graphs have different number of nodes, then an additional "null" node is added to the smaller graph as many times as the difference (see "Comparing stars: on approximating graph edit distance")
				# analogously here, if(lenI>lenJ), we add lenI-lenJ columns to matrix C and fill it opportunely
				# if(lenJ>lenI), we add lenJ-lenI rows to matrix C and fill it opportunely

				lenI = len(graphArtefactI)
				lenJ = len(graphArtefactJ)
				# C=numpy.zeros( (lenI, lenJ) )

				# abort if artifact has no stars (e.g. node with no sub-nodes in graph that was downloaded)
				# otherwise we will later get a square array of zeros (which is a perfect match)!
				if (lenI == 0) or (lenJ == 0) :
					tupleQueue[1].put( ( artefactI, artefactJ, None ) )
					continue

				nDim = max( [ lenI, lenJ ] )

				C = numpy.zeros( (nDim, nDim) )

				'''
				# OLD
				for pair in list( itertools.product(graphArtefactI, graphArtefactJ) ):
					i, = numpy.where(graphArtefactI == pair[0])
					j, = numpy.where(graphArtefactJ == pair[1])
					if pair[0] == pair[1]:
						#if the two nodes coincide
						C[i[0]][j[0]] = 0
					else:
						C[i[0]][j[0]] = readDistanceBetweenStars( pair[0], pair[1], dict_stars_size, dict_pairs_of_stars_SED )
				'''

				for nStarIndex1 in range( lenI ) :
					for nStarIndex2 in range( lenJ ) :
						if graphArtefactI[nStarIndex1] == graphArtefactJ[nStarIndex2] :
							# same star? SED = 0
							nSED = 0
						else :
							# lookup star pair SED
							nSED = readDistanceBetweenStars( graphArtefactI[nStarIndex1], graphArtefactJ[nStarIndex2], dict_stars_size, dict_pairs_of_stars_SED )
						C[nStarIndex1][nStarIndex2] = nSED

						# add extra column padding to make square
						if lenI > lenJ :
							# get SED for pair (0,star_2) with 0 == no star
							nSED = readDistanceBetweenStars( 0, graphArtefactJ[nStarIndex2], dict_stars_size, dict_pairs_of_stars_SED )
							for nExtraIndex in range( lenJ, lenI ) :
								C[nStarIndex1][nExtraIndex] = nSED

					# add extra row padding to make square
					if lenJ > lenI :
						# get SED for pair (star_1,0) with 0 == no star
						nSED = readDistanceBetweenStars( graphArtefactI[nStarIndex1], 0, dict_stars_size, dict_pairs_of_stars_SED )
						for nExtraIndex in range( lenI, lenJ ) :
							C[nExtraIndex][nStarIndex2] = nSED

				# computing the maximum degree of a vertex both in artefactI and in artefactJ
				'''
				max_degree_of_a_vertex_in_graphArtefactI = 1
				for v in graphArtefactI:
					if v in dict_stars_size:
						max_degree_of_a_vertex_in_graphArtefactI = max(max_degree_of_a_vertex_in_graphArtefactI,dict_stars_size[v])
				max_degree_of_a_vertex_in_graphArtefactJ = 1
				for v in graphArtefactJ:
					if v in dict_stars_size:
						max_degree_of_a_vertex_in_graphArtefactJ = max(max_degree_of_a_vertex_in_graphArtefactJ,dict_stars_size[v])
				max_degree_of_a_vertex = max( max_degree_of_a_vertex_in_graphArtefactI, max_degree_of_a_vertex_in_graphArtefactJ )
				'''
				
				# if the two graphs have different number of nodes, then an additional "null" node is added to the smaller graph as many times as the difference (see "Comparing stars: on approximating graph edit distance")
				# analogously here, if(lenI>lenJ), we add lenI-lenJ columns to matrix C and fill it opportunely
				# if(lenJ>lenI), we add lenJ-lenI rows to matrix C and fill it opportunely

				'''
				# OLD
				if(lenI>lenJ):
					v=numpy.zeros((lenI,1)) # a vector of the same size of graphArtefactI
					for el in graphArtefactI:
						i, = numpy.where(graphArtefactI == el)
						v[i,0] = 1 + 2 * (dict_stars_size[el] if el in dict_stars_size else 0)
					for k in range(0,lenI-lenJ):
						C = numpy.c_[ C, v ] # adding columns  
				elif(lenJ>lenI):
					v=numpy.zeros((1,lenJ)) # a vector of the same size of graphArtefactJ
					for el in graphArtefactJ:
						i, = numpy.where(graphArtefactJ == el)
						v[0,i] = 1 + 2 * (dict_stars_size[el] if el in dict_stars_size else 0)
					for k in range(0,lenJ-lenI):
						C = numpy.r_[ C, v ] # adding rows
				'''

				hungarian_cost = hungarian_algorithm( C.tolist() )

				setCommonNodes = set().union( graphArtefactI,graphArtefactJ )

				# calling code will write to disk
				# retuns the lower bound on graph edit distance, normalized by the number of distinct source entities in both graphs (to eliminate the effect of different graph sizes)
				# see (see "Comparing stars: on approximating graph edit distance") lemma 4.2
				#return (hungarian_cost/max(4, float(max_degree_of_a_vertex) +1))/len(setCommonNodes)

				# retuns the lower bound on graph edit distance, normalized by the number of distinct source entities in both graphs (to eliminate the effect of different graph sizes)
				# see (see "Comparing stars: on approximating graph edit distance") lemma 4.2
				nCost = hungarian_cost/max(4, float(max_degree_of_a_vertex) +1)

				# STUART below is a version normalized by graph size (DISABLED TO PREFER PAPER unnormalized version as SPARQL query will create graphs of a similar size anyway)
				#nCost = (hungarian_cost/max(4, float(max_degree_of_a_vertex) +1)) / len(setCommonNodes)

				# TODO think about only recording similarity only if the graph size is the same [for higher precision > will hit recall]
				# TODO think about a configurable graph size filter (and run process several times) -> distances_graph_size_1, distances_graph_size_2 ...
				tupleQueue[1].put( ( artefactI, artefactJ, nCost ) )

			# pause for more input
			time.sleep(0.001)

	except :
		# error result
		tupleQueue[2].put( 'hungarian algorithm worker thread error : ' + repr( sys.exc_info()[:2] ) )


'''
def getDistances( strURIArtefactI, arrayOfstrURIArtefact, dictIndexNodes, f_distances_between_artefacts, dict_stars_size, dict_pairs_of_stars_SED, logger, nProcessMax = 4, nThresholdSimilarity = 0.0 ):
	"""Builds the distances between an artefact and a list of artefacts and writes them

	This function builds the distances between an artefact (1st param) and a list of artefacts (2nd param) and writes them in a file (3rd param)
	:param strURIArtefactI: the first artefact to compare
	:type strURIArtefactI: string
	:param arrayOfstrURIArtefact: the list of other artefacts with which strURIArtefactI must be compared
	:type arrayOfstrURIArtefact: list of string
	:param f_distances_between_artefacts: the path of the file for writing distances between_artefacts
	:type f_distances_between_artefacts: string
	:param logger
	"""

	# create (queueIn, queueOut, queueError) for artifacts to check
	listQueue = []
	for nProcess in range(nProcessMax) :
		listQueue.append( ( multiprocessing.Queue(), multiprocessing.Queue(), multiprocessing.Queue(), multiprocessing.Queue()  ) )

	# chunk up the artifacts and put in queues to be processed by the thread pool
	listChunks = []
	nProcess = 0
	for artefactJ in arrayOfstrURIArtefact:
		if artefactJ != strURIArtefactI:
			# add pair to check on queue
			listQueue[nProcess][0].put( ( strURIArtefactI, artefactJ ) )

			# round robin assignment to queues
			nProcess = nProcess + 1
			if nProcess >= nProcessMax :
				nProcess = 0

	# setup a process pool as the hungarian algorithm is very CPU intensive so we need to use all cores on a machine to speed it up
	# note: use multiprocessing not threading so we get access to multiple CPU cores which is the whole point
	listProcesses = []
	for nProcess in range(nProcessMax) :
		#logger.info( 'starting thread ' + str(nProcess) + ' with ' + str( listQueue[nProcess][0].qsize() ) + ' pairs to check' )
		processHungarian = multiprocessing.Process( target = define_matrix_hungarian_and_solve, args = ( listQueue[nProcess], dictIndexNodes, dict_stars_size, dict_pairs_of_stars_SED ) )
		processHungarian.start()
		listProcesses.append( processHungarian )

	# do a blocking get of the finished flag queue
	# note: do not just join on thread as it will stay alive until its data is empties from queue object
	for nProcess in range(nProcessMax) :
		listQueue[nProcess][3].get( True, None )

	# compile the output
	listOutputResults = []
	for nProcess in range(nProcessMax) :
		#logger.info( 'processing results from thread ' + str(nProcess) + ' with ' + str( listQueue[nProcess][1].qsize() ) + ' pairs' )
		# handle errors
		while listQueue[nProcess][2].empty() == False :
			strErrorMsg = listQueue[nProcess][2].get()
			logger.info( '\tprocess failure > ' + strErrorMsg )
		# handle results
		while listQueue[nProcess][1].empty() == False :
			( artefactI, artefactJ, nCost ) = listQueue[nProcess][1].get()
			if nCost != None :
				edit_cost = float( nCost )

				# avoid reporting low confidence pairs as 9k artifacts will create 81M pairs
				if edit_cost <= nThresholdSimilarity :
					listOutputResults.append(
						artefactI + "\t" +
						artefactJ + "\t" +
						str(edit_cost) )

	
	# write output var in one go to disk
	if len(listOutputResults) > 0 :
		f_distances_between_artefacts.write( '\n'.join( listOutputResults ) +'\n' )

'''

def getDistancesAll( artefacts_all, dictIndexNodes, f_distances_between_artefacts, dict_stars_size, dict_pairs_of_stars_SED, logger, nProcessMax = 4, nThresholdSimilarity = 0.0 ):
	"""Builds the distances between all artefacts
	"""

	# no work to do?
	if len(artefacts_all) < 2 :
		return

	# create (queueIn, queueOut, queueError) for artifacts to check
	listQueue = []
	listExpectedResult = []
	for nProcess in range(nProcessMax) :
		listQueue.append( ( multiprocessing.Queue(), multiprocessing.Queue(), multiprocessing.Queue(), multiprocessing.Queue()  ) )
		listExpectedResult.append( 0 )

	logger.info( 'process queues created' )

	try :

		# setup a process pool as the hungarian algorithm is very CPU intensive so we need to use all cores on a machine to speed it up
		# note: use multiprocessing not threading so we get access to multiple CPU cores which is the whole point
		listProcesses = []
		for nProcess in range(nProcessMax) :
			#logger.info( 'starting thread ' + str(nProcess) + ' with ' + str( listQueue[nProcess][0].qsize() ) + ' pairs to check' )
			processHungarian = multiprocessing.Process( target = define_matrix_hungarian_and_solve, args = ( listQueue[nProcess], dictIndexNodes, dict_stars_size, dict_pairs_of_stars_SED, os.getpid() ) )
			listProcesses.append( processHungarian )

		logger.info( 'processes created' )

		for nProcess in range(nProcessMax) :
			listProcesses[nProcess].start()

		logger.info( 'processes started' )

		# loop adding pairs to be processed and reading the results
		# making sure queue size always < 100
		# last entry is ( None, None ) to indicate process should terminate
		nProcessInput = 0
		nIndexArtifactA = 0
		nIndexArtifactB = 1
		bFinishedInput = False
		bFinished = False
		bWriteEnd = False
		bAbort = False
		listOutputResults = []

		while (bFinished == False) and (bAbort == False) :

			# get any errors
			for nProcess in range(nProcessMax) :
				while listQueue[nProcess][2].empty() == False :
					strErrorMsg = listQueue[nProcess][2].get()
					logger.info( '\tprocess failure > ' + strErrorMsg )
					bAbort = True

			if bAbort == True :
				break

			# get any results
			for nProcess in range(nProcessMax) :
				if listExpectedResult[nProcess] > 0 :

					while listQueue[nProcess][1].empty() == False :
						( artefactI, artefactJ, nCost ) = listQueue[nProcess][1].get()
						listExpectedResult[nProcess] = listExpectedResult[nProcess] - 1

						if nCost != None :
							edit_cost = float( nCost )

							# avoid reporting low confidence pairs as 9k artifacts will create 81M pairs
							if edit_cost <= nThresholdSimilarity :
								f_distances_between_artefacts.write(
									artefactI + "\t" +
									artefactJ + "\t" +
									str(edit_cost) + "\n"
									)


			# have we finished?
			bFinished = True
			for nProcess in range(nProcessMax) :
				#logger.info( 'T3 process ' + str(nProcess) + ' has left ' + str(listExpectedResult[nProcess]) )
				if listExpectedResult[nProcess] > 0 :
					bFinished = False
			if bFinishedInput == False :
				bFinished = False
			if bFinished == True :
				break

			# add stuff on the input queue of each process
			for nProcess in range(nProcessMax) :
				while (bFinishedInput == False) and (listQueue[nProcess][0].qsize() < 100) :

					# add pair to check on queue (do not do symetric pair as the result will be same)
					listQueue[nProcess][0].put( ( artefacts_all[nIndexArtifactA], artefacts_all[nIndexArtifactB] ) )
					listExpectedResult[nProcess] = listExpectedResult[nProcess] + 1
					#logger.info( 'submitted pair : ' + repr( ( artefacts_all[nIndexArtifactA], artefacts_all[nIndexArtifactB] ) ) )

					# increment artifact index
					nIndexArtifactB = nIndexArtifactB + 1
					if nIndexArtifactB >= len(artefacts_all) :
						logger.info( 'submitted all pairs for artifact : ' + str(nIndexArtifactA) )
						nIndexArtifactA = nIndexArtifactA + 1
						nIndexArtifactB = nIndexArtifactA + 1

					if (nIndexArtifactA >= len(artefacts_all)) or (nIndexArtifactB >= len(artefacts_all)) :
						logger.info( 'finished input' )
						# finished the input
						bFinishedInput = True
						bWriteEnd = True
						break

			if bWriteEnd == True :
				for nProcess in range(nProcessMax) :
					listQueue[nProcess][0].put( ( None, None ) )
				bWriteEnd = False

			# pause to allow results to be processed
			time.sleep(0.001)

		if bAbort == True :
			Exception( 'aborted (see log)' )

	except :
		logger.exception( 'getDistancesAll failed' )

	finally :
		# empty queues so main process will terminate ok
		for nProcess in range(nProcessMax) :
			for nIndexQueue in range( len(listQueue[nProcess]) ) :
				while listQueue[nProcess][nIndexQueue].empty() == False :
					listQueue[nProcess][nIndexQueue].get()



def indexGraphNodes( artefacts_as_set_of_nodes, dict_stars_size ):
	"""
		Index the graphs as a numpy array of nodes (for later use in getDistances())
	"""
	dictIndex = {}
	for strURI in artefacts_as_set_of_nodes :
		# make a numpy array of nodes
		graphArtefact = numpy.array( artefacts_as_set_of_nodes[strURI], dtype=numpy.uint32 )

		# computing the maximum degree of a vertex
		# Stuart: start at 0 as we can get artifacts with no nodes (e.g. no part descriptions)
		max_degree_of_a_vertex = 0
		for nStarID in graphArtefact:
			if nStarID in dict_stars_size:
				max_degree_of_a_vertex = max( max_degree_of_a_vertex,dict_stars_size[nStarID] )

		dictIndex[strURI] = ( graphArtefact, max_degree_of_a_vertex )

	return dictIndex

def ConfigSectionReader(section):
	"""Defines a dictionary whose entries are the fields of a section in the configuration file

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

if __name__ == "__main__": 
	
	# make logger (global to STDOUT)
	LOG_FORMAT = ('%(levelname) -s %(asctime)s %(message)s')
	logger = logging.getLogger( __name__ )
	logging.basicConfig( level=logging.INFO, format=LOG_FORMAT )
	logger.info('logging started')

	Config = ConfigParser.ConfigParser()
	Config.read( ".\config.ini")

	dict_stars_size={}
	dict_pairs_of_stars_SED={}
	if(os.path.exists('sed_file')):
		read_star_edit_distance( dict_pairs_of_stars_SED )
	else:
		sed_file = codecs.open( 'sed_file', 'wb', 'utf-8', errors = 'replace' )
		# sed_file = open( 'sed_file', 'wb' )
		star_edit_distance( logger, sed_file, dict_stars_size,dict_pairs_of_stars_SED )
		sed_file.close()

	logger.info( 'sed computation finished now:'+ str(datetime.now()) )
	artefacts_all=[]
	read_artefacts( ConfigSectionReader("all_app")['artefacts_file'], artefacts_all )
	logger.info( 'artifacts file read OK')

	nProcessMax = int( ConfigSectionReader("sed_app")['process_count']  )
	nThresholdSimilarity = float( ConfigSectionReader("sed_app")['threshold_similarity_sed']  )
	logger.info( 'SED threshold = ' + str(nThresholdSimilarity) )

	artefacts_as_set_of_nodes = read_artefacts_as_set_of_nodes( ConfigSectionReader("sed_app")['artefacts_as_sets_of_nodes_file'] )
	dictIndexNodes = indexGraphNodes( artefacts_as_set_of_nodes, dict_stars_size )
	logger.info( 'artifact graph node index built : ' + str(len(dictIndexNodes)) )

	f_distances_between_artefacts = codecs.open( ConfigSectionReader("sed_app")['distances_sed_app_file'], 'wb', 'utf-8', errors = 'replace' )
	#f_distances_between_artefacts = open( ConfigSectionReader("sed_app")['distances_sed_app_file'], 'wb' )
	logger.info( 'number of artefacts :'+ str(len(artefacts_all)))

	artefacts_all_copy = copy.deepcopy( artefacts_all )
	logger.info( 'computing artifact graph similarity')

	getDistancesAll( artefacts_all_copy, dictIndexNodes, f_distances_between_artefacts, dict_stars_size, dict_pairs_of_stars_SED, logger, nProcessMax, nThresholdSimilarity )

	'''
	for nIndexArtifact in range(len(artefacts_all_copy)) :
		artefact = artefacts_all_copy[nIndexArtifact]
		#logger.info( 'artifact URI = ' + str(artefact) )

		time_before=time.time()
		getDistances( artefact, artefacts_all, dictIndexNodes, f_distances_between_artefacts, dict_stars_size, dict_pairs_of_stars_SED, logger, nProcessMax, nThresholdSimilarity )
		artefacts_all.remove(artefact)# distance is symmetrical
		time_spent=time.time() - time_before
		#logger.info('time_spent_by getDistances %0.2f' % (time_spent,))

		#if nIndexArtifact/100 == math.floor(nIndexArtifact/100) :
		logger.info( 'artifact count = ' + str(nIndexArtifact) )
	'''

	f_distances_between_artefacts.close()
	
	logger.info( 'finished' )